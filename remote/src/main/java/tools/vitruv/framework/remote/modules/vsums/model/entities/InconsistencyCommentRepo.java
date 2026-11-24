package tools.vitruv.framework.remote.modules.vsums.model.entities;

import java.util.List;
import java.util.UUID;
import tools.vitruv.framework.remote.common.entities.BaseRepo;

/** Repository for {@link InconsistencyComment} entities. */
public interface InconsistencyCommentRepo extends BaseRepo<InconsistencyComment> {

  /** Returns all comments on the given inconsistency, oldest first. */
  List<InconsistencyComment> findByInconsistencyIdOrderByCreatedAtAsc(UUID inconsistencyId);
}
