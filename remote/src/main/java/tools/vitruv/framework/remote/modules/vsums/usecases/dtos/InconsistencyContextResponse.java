package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.List;
import java.util.UUID;

/** Model/correspondence snapshot for an inconsistency (Epic 2 visualization). */
public record InconsistencyContextResponse(
    UUID inconsistencyId,
    UUID vsumId,
    String vsumName,
    String metamodelName,
    String viewTypeName,
    String note,
    List<ModelElementNode> elements,
    List<CorrespondenceEdge> correspondences) {
  /** A model element in the visualization, optionally nested under a parent element. */
  public record ModelElementNode(
      String id,
      String displayName,
      String eClassName,
      String metamodelName,
      String uri,
      String role,
      String parentId) {}

  /** A correspondence between two model elements, referenced by their ids. */
  public record CorrespondenceEdge(
      String sourceId, String targetId, String sourceLabel, String targetLabel) {}
}
