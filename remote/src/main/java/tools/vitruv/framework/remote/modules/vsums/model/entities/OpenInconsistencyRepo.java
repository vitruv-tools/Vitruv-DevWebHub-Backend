package tools.vitruv.framework.remote.modules.vsums.model.entities;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tools.vitruv.framework.remote.common.entities.BaseRepo;

/** Repository for {@link OpenInconsistency} entities. */
public interface OpenInconsistencyRepo extends BaseRepo<OpenInconsistency> {

  /** Returns all inconsistencies in the given state, newest first. */
  List<OpenInconsistency> findByStateOrderByCreatedAtDesc(OpenInconsistencyState state);

  /** Returns all inconsistencies, newest first. */
  List<OpenInconsistency> findAllByOrderByCreatedAtDesc();

  /** Returns the inconsistency of the given VSUM in the given state, if any. */
  Optional<OpenInconsistency> findByVsumIdAndState(UUID vsumId, OpenInconsistencyState state);

  /** Returns the inconsistency of the given propagation task in the given state, if any. */
  Optional<OpenInconsistency> findByTaskIdAndState(UUID taskId, OpenInconsistencyState state);

  /** Returns the inconsistency of the given propagation task, if any. */
  Optional<OpenInconsistency> findByTaskId(UUID taskId);
}
