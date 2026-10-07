package tools.vitruv.framework.remote.modules.users.usecases.dtos;

public record UpsertUserRequest(
    String username,
    String displayName,
    String email
) {
}
