package tools.vitruv.framework.remote.modules.users.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUser;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUserRepo;
import tools.vitruv.framework.remote.modules.users.model.entities.UserMetamodel;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpdateMetamodelsRequest;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpsertUserRequest;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UserProfileResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileUseCases {

  private final AppUserRepo appUserRepo;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;

  @Transactional
  public UserProfileResponse signIn(UpsertUserRequest request) {
    String username = requireUsername(request.username());
    AppUser user = appUserRepo.findByUsernameIgnoreCase(username).orElseGet(AppUser::new);
    Instant now = Instant.now();
    if (user.getCreatedAt() == null) {
      user.setUsername(username);
      user.setCreatedAt(now);
    }
    user.setDisplayName(displayName(request.displayName(), username));
    user.setEmail(blankToNull(request.email()));
    user.setLastLoginAt(now);
    return toResponse(appUserRepo.save(user));
  }

  @Transactional(readOnly = true)
  public List<UserProfileResponse> list() {
    return appUserRepo.findAllByOrderByUsernameAsc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public UserProfileResponse get(String username) {
    return toResponse(getOrThrow(username));
  }

  @Transactional
  public UserProfileResponse updateMetamodels(String username, UpdateMetamodelsRequest request) {
    AppUser user = getOrThrow(username);
    List<String> canonical = new ArrayList<>();
    LinkedHashSet<String> seen = new LinkedHashSet<>();
    List<String> requested = request.metamodels() == null ? List.of() : request.metamodels();
    for (String name : requested) {
      String canonicalName = knowledgeMetamodelCatalog.canonicalName(name);
      if (canonicalName == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown metamodel: " + name);
      }
      if (seen.add(canonicalName)) {
        canonical.add(canonicalName);
      }
    }

    // Keep rows that are still selected. Clearing and reinserting the same name
    // inserts before the old row is deleted and hits the unique constraint.
    List<UserMetamodel> current = user.getMetamodels();
    current.removeIf(row -> !seen.contains(row.getMetamodelName()));
    LinkedHashSet<String> kept = new LinkedHashSet<>();
    for (UserMetamodel row : current) {
      kept.add(row.getMetamodelName());
    }
    for (String name : canonical) {
      if (kept.contains(name)) {
        continue;
      }
      UserMetamodel row = new UserMetamodel();
      row.setUser(user);
      row.setMetamodelName(name);
      current.add(row);
    }
    return toResponse(appUserRepo.save(user));
  }

  private AppUser getOrThrow(String username) {
    return appUserRepo.findByUsernameIgnoreCase(requireUsername(username))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
  }

  private static String requireUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
    }
    return username.trim();
  }

  private static String displayName(String displayName, String username) {
    if (displayName == null || displayName.isBlank()) {
      return username;
    }
    return displayName.trim();
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private UserProfileResponse toResponse(AppUser user) {
    List<String> metamodels = user.getMetamodels().stream()
        .map(UserMetamodel::getMetamodelName)
        .sorted()
        .toList();
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getEmail(),
        user.getCreatedAt(),
        user.getLastLoginAt(),
        metamodels
    );
  }
}
