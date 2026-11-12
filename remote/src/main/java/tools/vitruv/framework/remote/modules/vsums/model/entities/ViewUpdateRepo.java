package tools.vitruv.framework.remote.modules.vsums.model.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ViewUpdateRepo extends JpaRepository<ViewUpdate, UUID> {
  ViewUpdate findFirstByVsumInfoIdOrderByTimestampDesc(UUID vsumId);

  List<ViewUpdate> findByVsumInfoIdOrderByTimestampDesc(UUID vsumId);

  // Derived delete queries need a transaction; callers (VsumManager repair paths) have none.
  @Transactional
  void deleteByVsumInfoId(UUID vsumId);
}
