package tools.vitruv.framework.remote.modules.users.usecases.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String username,
    String displayName,
    String email,
    Instant createdAt,
    Instant lastLoginAt,
    List<String> metamodels,
    String profileToken
) {
}
