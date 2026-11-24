package tools.vitruv.framework.remote.modules.users.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;
import tools.vitruv.framework.remote.modules.users.usecases.UserProfileUseCases;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpdateMetamodelsRequest;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpsertUserRequest;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UserProfileResponse;

/**
 * User profiles and the metamodels each user knows. First sign-in issues an {@code
 * X-Profile-Token}. Later sign-ins, reads, and updates must present that token. The username in the
 * path is not accepted as identity on its own.
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserProfileUseCases userProfileUseCases;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;

  /** Signs in or creates the user and returns the profile, with a token when one is issued. */
  @PostMapping("/session")
  public UserProfileResponse signIn(
      @RequestBody UpsertUserRequest request,
      @RequestHeader(value = UserProfileUseCases.PROFILE_TOKEN_HEADER, required = false)
          String profileToken) {
    return userProfileUseCases.signIn(request, profileToken);
  }

  /** Lists all user profiles as a directory without emails or metamodels. */
  @GetMapping
  public List<UserProfileResponse> list() {
    return userProfileUseCases.list();
  }

  /** Lists the metamodels a user can claim knowledge of. */
  @GetMapping("/knowledge-metamodels")
  public List<KnowledgeMetamodel> listKnowledgeMetamodels() {
    return knowledgeMetamodelCatalog.listAvailable();
  }

  /** Returns the profile of the given user if the profile token matches. */
  @GetMapping("/{username}")
  public UserProfileResponse get(
      @PathVariable String username,
      @RequestHeader(value = UserProfileUseCases.PROFILE_TOKEN_HEADER, required = false)
          String profileToken) {
    return userProfileUseCases.get(username, profileToken);
  }

  /** Replaces the metamodels the given user knows if the profile token matches. */
  @PutMapping("/{username}/metamodels")
  public UserProfileResponse updateMetamodels(
      @PathVariable String username,
      @RequestHeader(value = UserProfileUseCases.PROFILE_TOKEN_HEADER, required = false)
          String profileToken,
      @RequestBody UpdateMetamodelsRequest request) {
    return userProfileUseCases.updateMetamodels(username, request, profileToken);
  }
}
