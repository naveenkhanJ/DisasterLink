package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for client sync statuses (model dictionary section 6, ClientSyncStatusRepository).
 */
public interface ClientSyncStatusRepository extends JpaRepository<ClientSyncStatus, String> {

  /**
   * Finds the statuses of the clients of any of some districts (MD section 6, {@code
   * findByDistrictIn}; derived from the name, because {@code district} is a single-valued
   * attribute).
   *
   * @param districts the districts
   * @return the statuses of the clients in those districts
   */
  List<ClientSyncStatus> findByDistrictIn(Set<District> districts);
}
