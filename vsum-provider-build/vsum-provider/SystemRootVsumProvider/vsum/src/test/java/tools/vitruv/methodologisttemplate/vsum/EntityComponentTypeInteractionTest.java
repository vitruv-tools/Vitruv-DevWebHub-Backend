package tools.vitruv.methodologisttemplate.vsum;

import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import mir.reactions.model2ToModel.Model2ToModelChangePropagationSpecification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.Device;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.Server;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model.impl.ModelFactoryImpl;
import tools.vitruv.methodologisttemplate.model.model2.Model2Factory;
import tools.vitruv.methodologisttemplate.model.model2.Root;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Direct Vitruv tests for {@code model2ToModelReactions.reactions} (no VitruviusServer).
 * For full provider init + storage URI checks, see {@link SystemRootVsumProviderEntityInteractionTest}.
 */
class EntityComponentTypeInteractionTest {

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void entityInsertCreatesServerWhenUserSelectsServer(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextMultipleChoiceSingleSelection().respondWithChoiceAt(1);

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      var entity = Model2Factory.eINSTANCE.createEntity();
      entity.setName("dbServer");
      view.getRootObjects(Root.class).iterator().next().getEntities().add(entity);
    });

    var system = getDefaultView(vsum, List.of(System.class)).getRootObjects(System.class).iterator().next();
    Assertions.assertEquals(1, system.getComponents().size());
    Assertions.assertInstanceOf(Server.class, system.getComponents().get(0));
    Assertions.assertEquals("dbServer", system.getComponents().get(0).getName());
    interactions.assertAllInteractionsOccurred();
  }

  @Test
  void entityInsertCreatesDeviceWhenUserSelectsDevice(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextMultipleChoiceSingleSelection().respondWithChoiceAt(2);

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      var entity = Model2Factory.eINSTANCE.createEntity();
      entity.setName("sensor");
      view.getRootObjects(Root.class).iterator().next().getEntities().add(entity);
    });

    var system = getDefaultView(vsum, List.of(System.class)).getRootObjects(System.class).iterator().next();
    Assertions.assertInstanceOf(Device.class, system.getComponents().get(0));
    interactions.assertAllInteractionsOccurred();
  }

  private static InternalVirtualModel createVirtualModel(Path projectPath, TestUserInteraction interactions) {
    try {
      InternalVirtualModel model = new VirtualModelBuilder()
          .withStorageFolder(projectPath)
          .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(interactions))
          .withChangePropagationSpecifications(
              new Model2Model2ChangePropagationSpecification(),
              new Model2ToModelChangePropagationSpecification())
          .withViewType(ViewTypeFactory.createIdentityMappingViewType(
              "default", ModelFactoryImpl.init().getModelPackage()))
          .buildAndInitialize();
      model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
      return model;
    } catch (IOException e) {
      throw new RuntimeException("Failed to prepare VSUM storage folder: " + projectPath, e);
    }
  }

  private static void addSystem(VirtualModel vsum, Path projectPath) {
    String base = projectPath.toAbsolutePath().toString().replace('\\', '/');
    URI systemUri = URI.createFileURI(base + "/example.model");

    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(view, v -> v.registerRoot(ModelFactory.eINSTANCE.createSystem(), systemUri));
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
