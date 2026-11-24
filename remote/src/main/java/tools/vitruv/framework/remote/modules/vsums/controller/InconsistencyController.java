package tools.vitruv.framework.remote.modules.vsums.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyVisualizationUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateInconsistencyCommentRequest;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyCommentResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyContextResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyModelSnapshotResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenInconsistencyResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.RecordInconsistencyResolutionRequest;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.ViewUpdateSummaryResponse;

/** REST endpoints for inspecting, commenting on and resolving open inconsistencies. */
@RestController
@RequestMapping("/v1/inconsistencies")
@RequiredArgsConstructor
public class InconsistencyController {

  private final InconsistencyUseCases inconsistencyUseCases;
  private final InconsistencyVisualizationUseCases inconsistencyVisualizationUseCases;

  /**
   * Lists inconsistencies, optionally filtered by state.
   *
   * @param state the state to filter by; all inconsistencies if absent
   */
  @GetMapping
  public List<OpenInconsistencyResponse> list(@RequestParam(required = false) String state) {
    return inconsistencyUseCases.list(state);
  }

  /** Returns the inconsistency with the given ID. */
  @GetMapping("/{id}")
  public OpenInconsistencyResponse get(@PathVariable UUID id) {
    return inconsistencyUseCases.get(id);
  }

  /** Returns the visualization context of the given inconsistency. */
  @GetMapping("/{id}/context")
  public InconsistencyContextResponse getContext(@PathVariable UUID id) {
    return inconsistencyVisualizationUseCases.getContext(id);
  }

  /** Returns the model snapshot of the given inconsistency. */
  @GetMapping("/{id}/model")
  public InconsistencyModelSnapshotResponse getModel(@PathVariable UUID id) {
    return inconsistencyUseCases.getModelSnapshot(id);
  }

  /** Lists the comments on the given inconsistency. */
  @GetMapping("/{id}/comments")
  public List<InconsistencyCommentResponse> listComments(@PathVariable UUID id) {
    return inconsistencyUseCases.listComments(id);
  }

  /**
   * Adds a comment to the given inconsistency.
   *
   * @return the created comment
   */
  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public InconsistencyCommentResponse addComment(
      @PathVariable UUID id, @RequestBody CreateInconsistencyCommentRequest request) {
    return inconsistencyUseCases.addComment(id, request);
  }

  /**
   * Records a resolution for the given inconsistency.
   *
   * @return the updated inconsistency
   */
  @PostMapping("/{id}/resolution")
  public OpenInconsistencyResponse recordResolution(
      @PathVariable UUID id, @RequestBody RecordInconsistencyResolutionRequest request) {
    return inconsistencyUseCases.recordResolution(id, request);
  }

  /** Lists the summaries of view updates related to the given inconsistency. */
  @GetMapping("/{id}/view-updates")
  public List<ViewUpdateSummaryResponse> listViewUpdates(@PathVariable UUID id) {
    return inconsistencyUseCases.listViewUpdates(id);
  }
}
