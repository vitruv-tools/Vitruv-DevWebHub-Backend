package tools.vitruv.methodologisttemplate.vsum;

import vitruv.tools.framework.remote.vsumprovider.VirtualModelInitializer;
import vitruv.tools.framework.remote.vsumprovider.VsumProvider;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import mir.reactions.model2ToModel.Model2ToModelChangePropagationSpecification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model.impl.ModelFactoryImpl;
import tools.vitruv.methodologisttemplate.model.model2.Model2Factory;
import tools.vitruv.methodologisttemplate.model.model2.Root;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * VSUM provider for metamodel {@code SystemRootVsum} (Methodologist Template {@code model} + {@code model2}).
 *
 * <ul>
 *   <li>{@code Entity} in {@code Root} (model2): component type dialog</li>
 *   <li>{@code Link} in {@code Root} (model2): protocol selection or new protocol text input</li>
 *   <li>{@code Link} in {@code System} (model): speed text input in MBits/s</li>
 * </ul>
 *
 * <p>{@link #fixRootInStorage} keeps Root under the VSUM storage folder when reactions persist
 * with a project-relative path.
 */
public class SystemRootVsumProvider implements VsumProvider {
  @Override
  public String getMetamodelName() {
    return "SystemRootVsum";
  }

  @Override
  public VirtualModelInitializer getInitializer(Path storagePath) {
    return getInitializer(storagePath, new TestUserInteraction.ResultProvider(new TestUserInteraction()));
  }

  public VirtualModelInitializer getInitializer(Path storagePath, InteractionResultProvider interactionResultProvider) {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

    return () -> {
      repairIncompleteModelPair(storagePath);
      InternalVirtualModel vsum = createDefaultVirtualModel(storagePath, interactionResultProvider);
      if (!Files.exists(storagePath.resolve("example.model"))) {
        addSystem(vsum, storagePath);
      } else {
        ensureRootExists(vsum, storagePath);
      }
      fixRootInStorage(vsum, storagePath);
      ensureSystemRootCorrespondence(vsum);
      return vsum;
    };
  }

  /**
   * SystemRoot needs both {@code example.model} (System) and {@code example.model2} (Root).
   * If only one remains, correspondence metadata still points at the missing file and load fails
   * with {@code no EObject could be found for ID: .../example.model(2)#/...}. Wipe the incomplete
   * pair + consistency metadata so {@link #addSystem} can re-seed cleanly.
   */
  private static void repairIncompleteModelPair(Path projectPath) {
    Path model = projectPath.resolve("example.model");
    Path model2 = projectPath.resolve("example.model2");
    boolean hasModel = Files.exists(model);
    boolean hasModel2 = Files.exists(model2);
    if (hasModel == hasModel2) {
      return;
    }

    deleteQuietly(projectPath.resolve("vsum").resolve("correspondences.correspondence"));
    deleteQuietly(projectPath.resolve("vsum").resolve("models.models"));
    deleteRecursivelyQuietly(projectPath.resolve("consistencymetadata"));
    deleteQuietly(model);
    deleteQuietly(model2);
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // best-effort repair before load
    }
  }

  private static void deleteRecursivelyQuietly(Path dir) {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (var walk = Files.walk(dir)) {
      walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException ignored) {
          // best-effort
        }
      });
    } catch (IOException ignored) {
      // best-effort
    }
  }

  /**
   * Creates model2 {@link Root} at {@code example.model2} when System exists but Root does not.
   */
  private static void ensureRootExists(InternalVirtualModel vsum, Path projectPath) {
    System system = null;
    Root root = null;

    var selector = vsum.createSelector(
        ViewTypeFactory.createIdentityMappingViewType("default", ModelFactoryImpl.init().getModelPackage()));
    for (EObject element : selector.getSelectableElements()) {
      if (element instanceof System s) {
        system = s;
      } else if (element instanceof Root r) {
        root = r;
      }
    }

    if (system == null || root != null) {
      return;
    }

    String base = projectPath.toAbsolutePath().toString().replace('\\', '/');
    URI expectedRootUri = URI.createFileURI(base + "/example.model2");
    Root newRoot = Model2Factory.eINSTANCE.createRoot();

    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeRecordingTrait();
    modifyView(view, v -> v.registerRoot(newRoot, expectedRootUri));

    var correspondenceModel = vsum.getCorrespondenceModel();
    correspondenceModel.addCorrespondenceBetween(system, newRoot, "");
    correspondenceModel.addCorrespondenceBetween(newRoot, system, "");
  }

  private static InternalVirtualModel createDefaultVirtualModel(
      Path projectPath, InteractionResultProvider interactionResultProvider) {
    try {
      Files.createDirectories(projectPath);
      // Recovered / reopened VSUMs already have the marker; createFile would fail.
      if (!Files.exists(projectPath.resolve("test_project.marker_vitruv"))) {
        ProjectMarker.markAsProjectRootFolder(projectPath);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to prepare VSUM storage folder: " + projectPath, e);
    }

    // Ensure reactions correspondence EPackage is registered before loading existing
    // correspondences.correspondence from recovered / reopened storage.
    tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondencePackage.eINSTANCE.eClass();
    tools.vitruv.change.correspondence.CorrespondencePackage.eINSTANCE.eClass();

    try {
      InternalVirtualModel model = new VirtualModelBuilder()
          .withStorageFolder(projectPath)
          .withUserInteractorForResultProvider(interactionResultProvider)
          .withChangePropagationSpecifications(
              new Model2Model2ChangePropagationSpecification(),
              new Model2ToModelChangePropagationSpecification())
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
    String base = projectPath.toAbsolutePath().toString().replace('\\', '/');
    URI systemUri = URI.createFileURI(base + "/example.model");

    CommittableView view = getDefaultView(vsum, List.of(System.class)).withChangeDerivingTrait();
    modifyView(view, v -> v.registerRoot(ModelFactory.eINSTANCE.createSystem(), systemUri));
  }

  /**
   * Relocates model2 Root into {@code projectPath/example.model2} when needed,
   * preserving entities, links, and System↔Root correspondences.
   */
  private static void fixRootInStorage(InternalVirtualModel vsum, Path projectPath) {
    System system = null;
    Root root = null;

    var selector = vsum.createSelector(
        ViewTypeFactory.createIdentityMappingViewType("default", ModelFactoryImpl.init().getModelPackage()));
    for (EObject element : selector.getSelectableElements()) {
      if (element instanceof System s) {
        system = s;
      } else if (element instanceof Root r) {
        root = r;
      }
    }

    if (system == null || root == null) {
      return;
    }

    String base = projectPath.toAbsolutePath().toString().replace('\\', '/');
    URI expectedRootUri = URI.createFileURI(base + "/example.model2");
    URI actualRootUri = root.eResource().getURI();

    if (normalizeUri(actualRootUri).equals(normalizeUri(expectedRootUri))) {
      return;
    }

    Root relocatedRoot = Model2Factory.eINSTANCE.createRoot();
    relocatedRoot.getEntities().addAll(EcoreUtil.copyAll(root.getEntities()));
    relocatedRoot.getLinks().addAll(EcoreUtil.copyAll(root.getLinks()));

    var correspondenceModel = vsum.getCorrespondenceModel();
    correspondenceModel.removeCorrespondencesBetween(system, root, "");
    correspondenceModel.removeCorrespondencesBetween(root, system, "");

    var misplacedModel = vsum.getModelInstance(actualRootUri);
    if (misplacedModel != null) {
      misplacedModel.delete();
    }

    CommittableView view = getDefaultView(vsum, List.of(Root.class)).withChangeRecordingTrait();
    modifyView(view, v -> v.registerRoot(relocatedRoot, expectedRootUri));

    correspondenceModel.addCorrespondenceBetween(system, relocatedRoot, "");
    correspondenceModel.addCorrespondenceBetween(relocatedRoot, system, "");
  }

  private static String normalizeUri(URI uri) {
    return uri.toString().replace('\\', '/');
  }

  /**
   * {@code model2ToModel} routines retrieve {@link System} via {@link Root}. Re-create the link if
   * relocation or legacy storage left Root without a corresponding System (causes silent reaction
   * failure and {@code EntityImpl} errors in the UI).
   */
  private static void ensureSystemRootCorrespondence(InternalVirtualModel vsum) {
    System system = null;
    Root root = null;

    var selector = vsum.createSelector(
        ViewTypeFactory.createIdentityMappingViewType("default", ModelFactoryImpl.init().getModelPackage()));
    for (EObject element : selector.getSelectableElements()) {
      if (element instanceof System s) {
        system = s;
      } else if (element instanceof Root r) {
        root = r;
      }
    }

    if (system == null || root == null) {
      return;
    }

    var correspondenceModel = vsum.getCorrespondenceModel();
    boolean rootHasSystem = correspondenceModel.getCorrespondingEObjects(root).stream()
        .anyMatch(System.class::isInstance);
    if (!rootHasSystem) {
      correspondenceModel.addCorrespondenceBetween(system, root, "");
      correspondenceModel.addCorrespondenceBetween(root, system, "");
    }
  }
}
