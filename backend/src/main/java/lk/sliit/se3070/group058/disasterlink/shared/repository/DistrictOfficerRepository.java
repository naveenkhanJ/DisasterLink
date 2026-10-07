package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for district officers (model dictionary section 6, DistrictOfficerRepository). */
public interface DistrictOfficerRepository extends JpaRepository<DistrictOfficer, String> {

  /**
   * Finds the district officers of any of some districts (MD section 6, {@code findByDistrictIn};
   * derived from the name, because {@code district} is a single-valued attribute).
   *
   * @param districts the districts
   * @return the officers of those districts
   */
  List<DistrictOfficer> findByDistrictIn(Set<District> districts);
}
