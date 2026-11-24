package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

/** Request body for adding a comment to an inconsistency. */
public record CreateInconsistencyCommentRequest(String author, String body) {}
