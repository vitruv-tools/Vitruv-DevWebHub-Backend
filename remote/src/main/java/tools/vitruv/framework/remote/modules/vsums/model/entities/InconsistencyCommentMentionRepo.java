package tools.vitruv.framework.remote.modules.vsums.model.entities;

import tools.vitruv.framework.remote.common.entities.BaseRepo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InconsistencyCommentMentionRepo extends BaseRepo<InconsistencyCommentMention> {

  List<InconsistencyCommentMention> findByCommentIdInOrderByCommentIdAscPositionAsc(Collection<UUID> commentIds);
}
