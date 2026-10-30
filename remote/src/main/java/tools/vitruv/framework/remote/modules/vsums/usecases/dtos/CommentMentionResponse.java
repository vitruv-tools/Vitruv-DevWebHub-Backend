package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.List;
import tools.vitruv.framework.remote.modules.users.model.services.MentionKind;

/**
 * A tag in a comment. {@code name} is what follows the {@code @}. {@code users} lists
 * who the tag reaches: the one user, or everyone who knows the tagged metamodel.
 */
public record CommentMentionResponse(
    MentionKind kind,
    String name,
    String label,
    List<String> users
) {
}
