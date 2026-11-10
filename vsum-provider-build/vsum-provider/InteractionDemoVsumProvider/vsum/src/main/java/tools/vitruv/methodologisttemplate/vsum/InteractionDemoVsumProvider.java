package tools.vitruv.methodologisttemplate.vsum;

import vitruv.tools.framework.remote.vsumprovider.VirtualModelInitializer;
import vitruv.tools.framework.remote.vsumprovider.VsumProvider;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model.impl.ModelFactoryImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Demo VSUM for manual async user-interaction testing.
 * Same metamodels as SystemRootVsum, but asks for confirmation before mirroring a new component to model2.
 */
public class InteractionDemoVsumProvider implements VsumProvider {
  @Override
  public String getMetamodelName() {
    return "InteractionDemoVsum";
  }

  @Override
  public VirtualModelInitializer getInitializer(Path storagePath) {
    return getInitializer(storagePath, new TestUserInteraction.ResultProvider(new TestUserInteraction()));
  }

  @Override
  public VirtualModelInitializer getInitializer(Path storagePath, InteractionResultProvider interactionResultProvider) {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

    return () -> {
      VirtualModel vsum = createDefaultVirtualModel(storagePath, interactionResultProvider);
      addSystem(vsum, storagePath);
      return vsum;
    };
  }

  private static InternalVirtualModel createDefaultVirtualModel(
      Path projectPath, InteractionResultProvider interactionResultProvider) {
    try {
      InternalVirtualModel model = new VirtualModelBuilder()
          .withStorageFolder(projectPath)
          .withUserInteractorForResultProvider(interactionResultProvider)
          .withChangePropagationSpecifications(new Model2Model2ChangePropagationSpecification())
          .withViewType(ViewTypeFactory.createIdentityMappingViewType("default", ModelFactoryImpl.init().getModelPackage()))
          .buildAndInitialize();
      model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
      return model;
    } catch (IOException e) {
      throw new RuntimeException("Failed to prepare VSUM storage folder: " + projectPath, e);
    }
  }

  private static View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default", ModelFactoryImpl.init().getModelPackage()));
    selector.getSelectableElements().stream()
        .filter(element -> rootTypes.stream().anyMatch(it -> it.isInstance(element)))
        .forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private static void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }

  private static void addSystem(VirtualModel vsum, Path projectPath) {
    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
          ModelFactory.eINSTANCE.createSystem(),
          URI.createFileURI(projectPath.toString() + "/example.model"));
    });
  }
}
