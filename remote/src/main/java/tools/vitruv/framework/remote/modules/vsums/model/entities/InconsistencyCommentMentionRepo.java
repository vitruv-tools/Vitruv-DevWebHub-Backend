package tools.vitruv.framework.remote.modules.vsums.model.entities;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import tools.vitruv.framework.remote.common.entities.BaseRepo;

/**
 * Stores the {@code @tags} found in inconsistency comments.
 */
public interface InconsistencyCommentMentionRepo extends BaseRepo<InconsistencyCommentMention> {

  /**
   * Tags of the given comments, grouped by comment and in order of appearance.
   *
   * @param commentIds ids of the comments to load tags for
   * @return the matching tags
   */
  List<InconsistencyCommentMention> findByCommentIdInOrderByCommentIdAscPositionAsc(
      Collection<UUID> commentIds);
}
