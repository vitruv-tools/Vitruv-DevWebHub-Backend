package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A comment on an inconsistency. */
public record InconsistencyCommentResponse(
    UUID id,
    UUID inconsistencyId,
    String author,
    String body,
    Instant createdAt,
    List<CommentMentionResponse> mentions) {}
