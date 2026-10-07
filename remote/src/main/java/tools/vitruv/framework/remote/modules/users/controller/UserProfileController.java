package tools.vitruv.framework.remote.modules.users.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;
import tools.vitruv.framework.remote.modules.users.usecases.UserProfileUseCases;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpdateMetamodelsRequest;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpsertUserRequest;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UserProfileResponse;

import java.util.List;

/**
 * User profiles and the metamodels each user knows.
 * The client sends the username. This server does not verify login tokens.
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserProfileUseCases userProfileUseCases;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;

  @PostMapping("/session")
  public UserProfileResponse signIn(@RequestBody UpsertUserRequest request) {
    return userProfileUseCases.signIn(request);
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
  public UserProfileResponse get(@PathVariable String username) {
    return userProfileUseCases.get(username);
  }

  @PutMapping("/{username}/metamodels")
  public UserProfileResponse updateMetamodels(
      @PathVariable String username,
      @RequestBody UpdateMetamodelsRequest request
  ) {
    return userProfileUseCases.updateMetamodels(username, request);
  }
}
