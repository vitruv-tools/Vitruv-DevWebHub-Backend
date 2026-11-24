package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.UUID;

/** Response containing the id of a started asynchronous update task. */
public record StartAsyncUpdateResponse(UUID taskId) {}
