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
import tools.vitruv.methodologisttemplate.model.model.Component;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model.impl.ModelFactoryImpl;
import tools.vitruv.methodologisttemplate.model.model2.Entity;
import tools.vitruv.methodologisttemplate.model.model2.Root;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

class ComponentInsertedIntoSystemTest {

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void componentInsertWithSystemAndRootInSameViewCreatesEntity(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(System.class, Root.class)).withChangeDerivingTrait(), view -> {
      Component component = ModelFactory.eINSTANCE.createComponent();
      component.setName("TestEntity");
      view.getRootObjects(System.class).iterator().next().getComponents().add(component);
    });

    Root root = getDefaultView(vsum, List.of(Root.class)).getRootObjects(Root.class).iterator().next();
    Assertions.assertEquals(1, root.getEntities().size());
    Assertions.assertEquals("TestEntity", root.getEntities().get(0).getName());
    interactions.assertAllInteractionsOccurred();
  }

  @Test
  void componentInsertCreatesEntityWithoutUserInteraction(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(), view -> {
      Component component = ModelFactory.eINSTANCE.createComponent();
      component.setName("TestEntity");
      view.getRootObjects(System.class).iterator().next().getComponents().add(component);
    });

    Root root = getDefaultView(vsum, List.of(Root.class)).getRootObjects(Root.class).iterator().next();
    Assertions.assertEquals(1, root.getEntities().size());
    Assertions.assertEquals("TestEntity", root.getEntities().get(0).getName());
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
