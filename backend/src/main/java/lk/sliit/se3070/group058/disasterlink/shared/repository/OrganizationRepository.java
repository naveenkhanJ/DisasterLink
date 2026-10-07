package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for organisations (model dictionary section 6, OrganizationRepository). {@code
 * findAllById} is inherited.
 */
public interface OrganizationRepository extends JpaRepository<Organization, String> {

  /**
   * Finds the organisations whose service area includes any of some districts (MD section 6, {@code
   * findByDistrictIn}). It is written by hand, because {@code districts} is a collection attribute;
   * each organisation is returned once.
   *
   * @param districts the districts
   * @return the organisations serving at least one of them
   */
  @Query("select distinct o from Organization o join o.districts d where d in :districts")
  List<Organization> findByDistrictIn(@Param("districts") Set<District> districts);
}
