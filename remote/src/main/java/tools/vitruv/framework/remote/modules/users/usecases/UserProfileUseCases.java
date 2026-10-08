package tools.vitruv.framework.remote.modules.users.usecases;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
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

@Service
@RequiredArgsConstructor
public class UserProfileUseCases {

  public static final String PROFILE_TOKEN_HEADER = "X-Profile-Token";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final AppUserRepo appUserRepo;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;

  /**
   * Creates or updates a profile and returns a secret token.
   * Sign-in always succeeds: this server does not verify the Methodologist login,
   * so a returning user such as demo must be able to sign in again after the browser
   * lost the previous token. Reading or changing the profile still requires the
   * latest token, which sign-in replaces when the presented one is missing or wrong.
   */
  @Transactional
  public UserProfileResponse signIn(UpsertUserRequest request, String presentedToken) {
    String username = requireUsername(request.username());
    AppUser user = appUserRepo.findByUsernameIgnoreCase(username).orElseGet(AppUser::new);
    Instant now = Instant.now();
    if (user.getCreatedAt() == null) {
      user.setUsername(username);
      user.setCreatedAt(now);
    }
    String issued = null;
    if (user.getProfileTokenHash() == null || !tokenMatches(user, presentedToken)) {
      issued = newToken();
      user.setProfileTokenHash(hash(issued));
    }
    user.setDisplayName(displayName(request.displayName(), username));
    user.setEmail(blankToNull(request.email()));
    user.setLastLoginAt(now);
    String tokenToReturn = issued != null ? issued : presentedToken;
    return toResponse(appUserRepo.save(user), tokenToReturn);
  }

  /**
   * Directory of display names. Email and metamodel selections stay off this list.
   */
  @Transactional(readOnly = true)
  public List<UserProfileResponse> list() {
    return appUserRepo.findAllByOrderByUsernameAsc().stream().map(this::toDirectoryResponse).toList();
  }

  @Transactional(readOnly = true)
  public UserProfileResponse get(String username, String presentedToken) {
    AppUser user = getOrThrow(username);
    requireToken(user, presentedToken);
    return toResponse(user, null);
  }

  @Transactional
  public UserProfileResponse updateMetamodels(
      String username,
      UpdateMetamodelsRequest request,
      String presentedToken
  ) {
    AppUser user = getOrThrow(username);
    requireToken(user, presentedToken);
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
    return toResponse(appUserRepo.save(user), null);
  }

  private AppUser getOrThrow(String username) {
    return appUserRepo.findByUsernameIgnoreCase(requireUsername(username))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
  }

  private static void requireToken(AppUser user, String presentedToken) {
    if (!tokenMatches(user, presentedToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Profile token is required");
    }
  }

  private static boolean tokenMatches(AppUser user, String presentedToken) {
    if (user.getProfileTokenHash() == null || presentedToken == null || presentedToken.isBlank()) {
      return false;
    }
    byte[] expected = user.getProfileTokenHash().getBytes(StandardCharsets.UTF_8);
    byte[] actual = hash(presentedToken).getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, actual);
  }

  private static String newToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
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

  private UserProfileResponse toDirectoryResponse(AppUser user) {
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        null,
        user.getCreatedAt(),
        user.getLastLoginAt(),
        List.of(),
        null
    );
  }

  private UserProfileResponse toResponse(AppUser user, String profileToken) {
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
        metamodels,
        profileToken
    );
  }
}
