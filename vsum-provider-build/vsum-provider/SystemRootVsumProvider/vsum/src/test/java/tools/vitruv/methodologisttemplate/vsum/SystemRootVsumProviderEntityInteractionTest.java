package tools.vitruv.methodologisttemplate.vsum;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.Server;
import tools.vitruv.methodologisttemplate.model.model.impl.ModelFactoryImpl;
import tools.vitruv.methodologisttemplate.model.model2.Model2Factory;
import tools.vitruv.methodologisttemplate.model.model2.Root;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Uses the real {@link SystemRootVsumProvider} initializer (including Root URI setup) and simulates
 * a server JVM whose working directory differs from vsum-storage.
 */
class SystemRootVsumProviderEntityInteractionTest {

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void entityInsertPromptsAndRootIsStoredInVsumStorage(@TempDir Path vsumStorage, @TempDir Path serverCwd)
      throws Exception {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextMultipleChoiceSingleSelection().respondWithChoiceAt(1);

    Path previousCwd = Path.of("").toAbsolutePath();
    try {
      java.lang.System.setProperty("user.dir", serverCwd.toString());

      InternalVirtualModel vsum = (InternalVirtualModel) new SystemRootVsumProvider()
          .getInitializer(vsumStorage, new TestUserInteraction.ResultProvider(interactions))
          .init();

      Root root = getDefaultView(vsum, List.of(Root.class)).getRootObjects(Root.class).iterator().next();
      URI rootUri = root.eResource().getURI();
      String expectedBase = vsumStorage.toAbsolutePath().toString().replace('\\', '/');
      Assertions.assertEquals(
          normalizeUri(URI.createFileURI(expectedBase + "/example.model2")),
          normalizeUri(rootUri),
          () -> "Root should live in vsum-storage, but was at " + rootUri);
      Assertions.assertTrue(
          Files.exists(vsumStorage.resolve("example.model2")),
          "example.model2 should exist in vsum-storage");

      modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
        var entity = Model2Factory.eINSTANCE.createEntity();
        entity.setName("myEntity");
        view.getRootObjects(Root.class).iterator().next().getEntities().add(entity);
      });

      tools.vitruv.methodologisttemplate.model.model.System system =
          getDefaultView(vsum, List.of(tools.vitruv.methodologisttemplate.model.model.System.class))
              .getRootObjects(tools.vitruv.methodologisttemplate.model.model.System.class)
              .iterator()
              .next();
      Assertions.assertEquals(1, system.getComponents().size());
      Assertions.assertInstanceOf(Server.class, system.getComponents().get(0));
      Assertions.assertEquals("myEntity", system.getComponents().get(0).getName());
      interactions.assertAllInteractionsOccurred();
    } finally {
      java.lang.System.setProperty("user.dir", previousCwd.toString());
    }
  }

  private static String normalizeUri(URI uri) {
    return uri.toString().replace('\\', '/');
  }

  private static View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType(
        "default", ModelFactoryImpl.init().getModelPackage()));
    selector.getSelectableElements().stream()
        .filter(element -> rootTypes.stream().anyMatch(it -> it.isInstance(element)))
        .forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private static void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }
}
