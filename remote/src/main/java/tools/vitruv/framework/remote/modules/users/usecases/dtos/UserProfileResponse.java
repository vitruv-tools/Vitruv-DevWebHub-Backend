package tools.vitruv.framework.remote.modules.users.usecases.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A user profile as returned by the API.
 *
 * @param id the user ID
 * @param username the username
 * @param displayName the display name
 * @param email the email address, or null if hidden or unset
 * @param createdAt the creation time
 * @param lastLoginAt the time of the last sign-in
 * @param metamodels the names of the known metamodels
 * @param profileToken the newly issued profile token, or null if none was issued
 */
public record UserProfileResponse(
    UUID id,
    String username,
    String displayName,
    String email,
    Instant createdAt,
    Instant lastLoginAt,
    List<String> metamodels,
    String profileToken) {}
