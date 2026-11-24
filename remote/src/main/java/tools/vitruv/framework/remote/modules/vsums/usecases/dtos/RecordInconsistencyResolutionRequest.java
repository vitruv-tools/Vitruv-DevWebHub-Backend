package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

/** Request body for recording who resolved an inconsistency and with which choice. */
public record RecordInconsistencyResolutionRequest(
    String resolvedBy, String choice, String comment) {}
