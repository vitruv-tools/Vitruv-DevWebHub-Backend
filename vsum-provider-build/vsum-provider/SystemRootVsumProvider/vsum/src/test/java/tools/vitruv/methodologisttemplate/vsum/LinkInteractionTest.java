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
import tools.vitruv.methodologisttemplate.model.model.Link;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
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
 * Point 2 (Root Link → protocol multi-select / new protocol text) and
 * Point 3 (System Link → speed text input in MBits/s).
 */
class LinkInteractionTest {

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void systemLinkInsertSetsSpeedFromUserInput(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextTextInput().respondWith("1000");

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait(), view -> {
      var system = view.getRootObjects(System.class).iterator().next();
      var link = ModelFactory.eINSTANCE.createLink();
      system.getLinks().add(link);
    });

    Link link = getDefaultView(vsum, List.of(System.class))
        .getRootObjects(System.class).iterator().next().getLinks().get(0);
    Assertions.assertEquals(1000, link.getSpeedMBitsPerSecond());
    Assertions.assertEquals(1, getDefaultView(vsum, List.of(Root.class))
        .getRootObjects(Root.class).iterator().next().getLinks().size());
    interactions.assertAllInteractionsOccurred();
  }

  @Test
  void rootLinkInsertMultiSelectsExistingProtocols(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextTextInput().respondWith("HTTP");
    interactions.onNextMultipleChoiceSingleSelection().respondWithChoiceAt(1);
    interactions.onNextTextInput().respondWith("TCP");
    interactions.onNextMultipleChoiceSingleSelection().respondWithChoiceAt(0);
    interactions.onNextMultipleChoiceMultiSelection().respondWithChoicesAt(0, 1);

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      view.getRootObjects(Root.class).iterator().next().getLinks().add(Model2Factory.eINSTANCE.createLink());
    });
    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      view.getRootObjects(Root.class).iterator().next().getLinks().add(Model2Factory.eINSTANCE.createLink());
    });
    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      view.getRootObjects(Root.class).iterator().next().getLinks().add(Model2Factory.eINSTANCE.createLink());
    });

    System system = getDefaultView(vsum, List.of(System.class)).getRootObjects(System.class).iterator().next();
    Assertions.assertEquals(3, system.getLinks().size());
    Link link = system.getLinks().get(2);
    Assertions.assertEquals(2, link.getSupportedProtocols().size());
    Assertions.assertEquals("HTTP", link.getSupportedProtocols().get(0).getName());
    Assertions.assertEquals("TCP", link.getSupportedProtocols().get(1).getName());
    Assertions.assertEquals("HTTP", link.getProtocol().getName());
    interactions.assertAllInteractionsOccurred();
  }

  @Test
  void rootLinkInsertCreatesNewProtocolFromTextInput(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextTextInput().respondWith("Existing");
    interactions.onNextMultipleChoiceSingleSelection().respondWithChoiceAt(1);
    interactions.onNextTextInput().respondWith("CustomProtocol");

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      view.getRootObjects(Root.class).iterator().next().getLinks().add(Model2Factory.eINSTANCE.createLink());
    });
    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      view.getRootObjects(Root.class).iterator().next().getLinks().add(Model2Factory.eINSTANCE.createLink());
    });

    System system = getDefaultView(vsum, List.of(System.class)).getRootObjects(System.class).iterator().next();
    Link link = system.getLinks().get(1);
    Assertions.assertEquals(1, link.getSupportedProtocols().size());
    Assertions.assertEquals("CustomProtocol", link.getProtocol().getName());
    Assertions.assertTrue(system.getProtocols().stream().anyMatch(p -> "CustomProtocol".equals(p.getName())));
    interactions.assertAllInteractionsOccurred();
  }

  @Test
  void rootLinkInsertWithNoProtocolsPromptsForNewProtocolName(@TempDir Path tempDir) {
    TestUserInteraction interactions = new TestUserInteraction();
    interactions.onNextTextInput().respondWith("FirstProtocol");

    InternalVirtualModel vsum = createVirtualModel(tempDir, interactions);
    addSystem(vsum, tempDir);

    modifyView(getDefaultView(vsum, List.of(Root.class)).withChangeDerivingTrait(), view -> {
      view.getRootObjects(Root.class).iterator().next().getLinks().add(Model2Factory.eINSTANCE.createLink());
    });

    System system = getDefaultView(vsum, List.of(System.class)).getRootObjects(System.class).iterator().next();
    Assertions.assertEquals(1, system.getProtocols().size());
    Assertions.assertEquals("FirstProtocol", system.getLinks().get(0).getProtocol().getName());
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
