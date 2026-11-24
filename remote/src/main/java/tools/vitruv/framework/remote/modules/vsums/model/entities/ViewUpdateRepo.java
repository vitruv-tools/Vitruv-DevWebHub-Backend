package tools.vitruv.framework.remote.modules.vsums.model.entities;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for persisted {@link ViewUpdate} entities. */
@Repository
public interface ViewUpdateRepo extends JpaRepository<ViewUpdate, UUID> {
  /** Returns the latest view update of the given VSUM, or {@code null} if there is none. */
  ViewUpdate findFirstByVsumInfoIdOrderByTimestampDesc(UUID vsumId);

  /** Returns all view updates of the given VSUM, newest first. */
  List<ViewUpdate> findByVsumInfoIdOrderByTimestampDesc(UUID vsumId);

  /** Deletes all view updates of the given VSUM. */
  // Derived delete queries need a transaction; callers (VsumManager repair paths) have none.
  @Transactional
  void deleteByVsumInfoId(UUID vsumId);
}
