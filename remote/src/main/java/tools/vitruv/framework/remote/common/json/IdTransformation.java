package tools.vitruv.framework.remote.common.json;

import org.eclipse.emf.common.util.URI;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.root.RootEChange;
import tools.vitruv.change.utils.ProjectMarker;

import java.nio.file.Path;
import java.util.List;

/**
 * Contains functions to transform IDs used by the Vitruvius framework to identify {@link
 * org.eclipse.emf.ecore.EObject EObjects}.
 */
public class IdTransformation {
  private URI root;

  /**
   * Creates a new IdTransformation.
   *
   * @param vsumPath the path to the .vsum file of the project
   */
  public IdTransformation(Path vsumPath) {
    root =
        URI.createFileURI(
            ProjectMarker.getProjectRootFolder(vsumPath)
                .orElseThrow(
                    () -> new IllegalStateException("No project root found for " + vsumPath))
                .toString());

    var nextToCheck = vsumPath;
    while ((nextToCheck = nextToCheck.getParent()) != null) {
      var parentRoot = ProjectMarker.getProjectRootFolder(nextToCheck);
      if (parentRoot.isEmpty()) {
        break;
      }
      root = URI.createFileURI(parentRoot.get().toString());
    }
  }

  /**
   * Transforms the given global (absolute path) ID to a local ID (relative path).
   *
   * @param global The ID to transform.
   * @return The local ID.
   */
  public URI toLocal(URI global) {
    if (global == null
        || global.toString().contains("cache")
        || global.toString().equals(JsonFieldName.TEMP_VALUE)
        || !global.isFile()) {
      return global;
    }

    return URI.createURI(global.toString().replace(root.toString(), ""));
  }

  /**
   * Transforms the given local ID (relative path) to a global ID (absolute path).
   *
   * @param local The ID to transform.
   * @return The global ID.
   */
  public URI toGlobal(URI local) {
    if (local == null
        || local.toString().contains("cache")
        || local.toString().equals(JsonFieldName.TEMP_VALUE)) {
      return local;
    }

    if (!local.isRelative()) {
      return local;
    }

    return URI.createURI(root.toString() + local.toString());
  }

  /**
   * Transforms all root change URIs in the given list of changes to global IDs.
   *
   * @param eChanges the list of changes
   */
  public void allToGlobal(List<? extends EChange<HierarchicalId>> eChanges) {
    for (var eChange : eChanges) {
      if (eChange instanceof RootEChange<?> change) {
        change.setUri(toGlobal(URI.createURI(change.getUri())).toString());
      }
    }
  }

  /**
   * Transforms all root change URIs in the given list of changes to local IDs.
   *
   * @param eChanges the list of changes
   */
  public void allToLocal(List<? extends EChange<HierarchicalId>> eChanges) {
    for (var eChange : eChanges) {
      if (eChange instanceof RootEChange<?> change) {
        change.setUri(toLocal(URI.createURI(change.getUri())).toString());
      }
    }
  }
}
