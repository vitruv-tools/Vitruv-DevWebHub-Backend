package tools.vitruv.framework.remote.modules.users.usecases.dtos;

/**
 * Request to sign in or create a user.
 *
 * @param username the username
 * @param displayName the display name, defaults to the username if blank
 * @param email the email address, may be blank
 */
public record UpsertUserRequest(String username, String displayName, String email) {}
