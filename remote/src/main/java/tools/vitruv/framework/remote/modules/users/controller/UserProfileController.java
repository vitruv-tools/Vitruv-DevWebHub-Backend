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
 * User profiles and the metamodels each user knows.
 * Reading or changing a profile requires the {@code X-Profile-Token} issued by sign-in.
 * The username in the path is not accepted as identity on its own.
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserProfileUseCases userProfileUseCases;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;

  @PostMapping("/session")
  public UserProfileResponse signIn(
      @RequestBody UpsertUserRequest request,
      @RequestHeader(value = UserProfileUseCases.PROFILE_TOKEN_HEADER, required = false) String profileToken
  ) {
    return userProfileUseCases.signIn(request, profileToken);
  }

  @GetMapping
  public List<UserProfileResponse> list() {
    return userProfileUseCases.list();
  }

  @GetMapping("/knowledge-metamodels")
  public List<KnowledgeMetamodel> listKnowledgeMetamodels() {
    return knowledgeMetamodelCatalog.listAvailable();
  }

  @GetMapping("/{username}")
  public UserProfileResponse get(
      @PathVariable String username,
      @RequestHeader(value = UserProfileUseCases.PROFILE_TOKEN_HEADER, required = false) String profileToken
  ) {
    return userProfileUseCases.get(username, profileToken);
  }

  @PutMapping("/{username}/metamodels")
  public UserProfileResponse updateMetamodels(
      @PathVariable String username,
      @RequestHeader(value = UserProfileUseCases.PROFILE_TOKEN_HEADER, required = false) String profileToken,
      @RequestBody UpdateMetamodelsRequest request
  ) {
    return userProfileUseCases.updateMetamodels(username, request, profileToken);
  }
}
