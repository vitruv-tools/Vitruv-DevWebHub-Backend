package tools.vitruv.framework.remote.modules.users.model.entities;

import tools.vitruv.framework.remote.common.entities.BaseRepo;

import java.util.List;
import java.util.Optional;

public interface AppUserRepo extends BaseRepo<AppUser> {
  Optional<AppUser> findByUsernameIgnoreCase(String username);

  List<AppUser> findAllByOrderByUsernameAsc();
}
