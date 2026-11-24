package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;

/** Status of an asynchronous propagation task, with pending interaction or result if any. */
public record PropagationTaskStatusResponse(
    UUID taskId,
    UUID viewId,
    AsyncTaskState state,
    Instant createdAt,
    Instant completedAt,
    String error,
    Map<String, Object> interaction,
    String result) {}
