package tools.vitruv.framework.remote.modules.vsums.model.entities;

import java.util.List;
import java.util.UUID;
import tools.vitruv.framework.remote.common.entities.BaseRepo;

public interface InconsistencyCommentRepo extends BaseRepo<InconsistencyComment> {

  List<InconsistencyComment> findByInconsistencyIdOrderByCreatedAtAsc(UUID inconsistencyId);
}
