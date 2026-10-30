package tools.vitruv.framework.remote.modules.users.model.entities;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.vitruv.framework.remote.common.entities.BaseRepo;

public interface AppUserRepo extends BaseRepo<AppUser> {
  Optional<AppUser> findByUsernameIgnoreCase(String username);

  List<AppUser> findAllByOrderByUsernameAsc();

  /** Everyone who selected the given metamodel in their settings, sorted by username. */
  @Query("select distinct u from AppUser u join u.metamodels m "
      + "where m.metamodelName = :metamodelName order by u.username asc")
  List<AppUser> findAllKnowingMetamodel(@Param("metamodelName") String metamodelName);
}
