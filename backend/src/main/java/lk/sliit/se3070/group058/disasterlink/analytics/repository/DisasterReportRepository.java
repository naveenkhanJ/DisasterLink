package lk.sliit.se3070.group058.disasterlink.analytics.repository;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for disaster reports (model dictionary section 6, DisasterReportRepository). */
public interface DisasterReportRepository extends JpaRepository<DisasterReport, String> {

  /**
   * Finds the reports shared with an organisation, oldest first (MD section 6, {@code
   * findSharedWith}; A23).
   *
   * @param organizationId the identifier of the organisation
   * @return the reports shared with it
   */
  @Query(
      "select r from DisasterReport r join r.sharedWith o"
          + " where o.organizationId = :organizationId order by r.generatedDate")
  List<DisasterReport> findSharedWith(@Param("organizationId") String organizationId);
}
