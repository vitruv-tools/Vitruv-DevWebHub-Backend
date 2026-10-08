package tools.vitruv.framework.remote.modules.vsums.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog;
import tools.vitruv.framework.remote.modules.vsums.model.entities.InconsistencyCommentRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistency;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyState;
import tools.vitruv.framework.remote.modules.vsums.model.entities.ViewUpdateRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfoRepo;
import tools.vitruv.framework.remote.modules.vsums.model.manager.ViewManager;
import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.VsumWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyModelSnapshotEnricher;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyModelSnapshotResponse;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The hub snapshot reads the committed resource set while commit is still blocked on a prompt.
 * Moving that assignment to after commit would make this test fail.
 */
@ExtendWith(MockitoExtension.class)
class AsyncPropagationCommitSnapshotTest {

    @Mock
    private ViewManager viewManager;
    @Mock
    private ViewService viewService;
    @Mock
    private InconsistencyUseCases parkedUseCases;
    @Mock
    private VsumManager vsumManager;

    @Test
    void hubSnapshotSeesSubmittedModelWhileCommitIsBlocked() throws Exception {
        UUID viewId = UUID.randomUUID();
        UUID vsumId = UUID.randomUUID();
        UUID inconsistencyId = UUID.randomUUID();
        String submitted = "{\"resources\":[{\"name\":\"submitted-task\"}]}";

        VsumInfo info = new VsumInfo();
        ReflectionTestUtils.setField(info, "id", vsumId);
        ViewWrapper viewWrapper = new ViewWrapper(viewId, null, null, new VsumWrapper(info, null, null));
        when(viewManager.getView(viewId)).thenReturn(viewWrapper);
        when(parkedUseCases.hasOpenForVsum(vsumId)).thenReturn(false);

        CountDownLatch commitEntered = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        when(viewService.commitResourceSet(any(), anyString())).thenAnswer(invocation -> {
            commitEntered.countDown();
            assertThat(releaseCommit.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        });
        when(viewService.update(any())).thenReturn("[]");

        PropagationTaskRegistry registry = new PropagationTaskRegistry();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AsyncPropagationService service = new AsyncPropagationService(
                registry,
                viewManager,
                viewService,
                parkedUseCases,
                vsumManager,
                executor);
        try {
            UUID taskId = service.startUpdate(viewId, submitted);
            assertThat(commitEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(registry.getTaskStatus(taskId).getCommittedResourceSet()).isEqualTo(submitted);

            OpenInconsistency entity = new OpenInconsistency();
            ReflectionTestUtils.setField(entity, "id", inconsistencyId);
            entity.setState(OpenInconsistencyState.OPEN);
            entity.setTaskId(taskId);
            entity.setVsumId(vsumId);
            entity.setMessage("A task was created");

            OpenInconsistencyRepo openRepo = mock(OpenInconsistencyRepo.class);
            when(openRepo.findById(inconsistencyId)).thenReturn(Optional.of(entity));
            VsumInfoRepo vsumInfoRepo = mock(VsumInfoRepo.class);
            when(vsumInfoRepo.findById(vsumId)).thenReturn(Optional.empty());
            InconsistencyModelSnapshotEnricher enricher = mock(InconsistencyModelSnapshotEnricher.class);
            when(enricher.enrich(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));

            InconsistencyUseCases hub = new InconsistencyUseCases(
                    openRepo,
                    mock(InconsistencyCommentRepo.class),
                    mock(ViewUpdateRepo.class),
                    vsumInfoRepo,
                    viewManager,
                    viewService,
                    registry,
                    enricher,
                    mock(KnowledgeMetamodelCatalog.class),
                    new ObjectMapper());

            InconsistencyModelSnapshotResponse snapshot = hub.getModelSnapshot(inconsistencyId);
            assertThat(snapshot.encodedResourceSet()).isEqualTo(submitted);
            assertThat(snapshot.note()).contains("committed when propagation started");
        } finally {
            releaseCommit.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
