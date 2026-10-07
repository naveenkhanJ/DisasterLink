package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for citizens (model dictionary section 6, CitizenRepository). */
public interface CitizenRepository extends JpaRepository<Citizen, String> {

  /**
   * Finds the citizens registered in any of some districts (MD section 6, {@code findByDistrictIn};
   * derived from the name, because {@code district} is a single-valued attribute).
   *
   * @param districts the districts
   * @return the citizens registered there
   */
  List<Citizen> findByDistrictIn(Set<District> districts);
}
