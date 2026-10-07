package lk.sliit.se3070.group058.disasterlink.hazardreport.repository;

import java.time.LocalDateTime;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoBox;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoDistance;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Data access for hazard reports (model dictionary section 6, HazardReportRepository). */
public interface HazardReportRepository extends JpaRepository<HazardReport, String> {

  /**
   * Finds the reports waiting for a decision: status PENDING_VERIFICATION, oldest received first
   * (MD section 6, {@code findPending}).
   *
   * @return the pending reports
   */
  @Query("select r from HazardReport r where r.status = PENDING_VERIFICATION order by r.reportTime")
  List<HazardReport> findPending();

  /**
   * Finds the reports captured at or after a time within a radius of a location (MD section 6,
   * {@code findNearby}; implementation-only). A helper query selects the candidates inside a
   * bounding box, and the radius is then checked with {@link GeoDistance#haversineKm}.
   *
   * <p>The dictionary's signature carries no report, so the result may contain the selected report
   * and reports of any type and status. The caller, {@code
   * ReportVerificationController.findNearbyReports}, removes the selected report and applies the
   * type and status rule.
   *
   * @param location the centre of the search
   * @param radiusKm the radius in kilometres
   * @param since the earliest capture time, inclusive
   * @return the reports within the radius
   */
  @Transactional(readOnly = true)
  default List<HazardReport> findNearby(GeoPoint location, double radiusKm, LocalDateTime since) {
    GeoBox box = GeoBox.around(location, radiusKm);
    return findCapturedInBox(
            since, box.minLatitude(), box.maxLatitude(), box.minLongitude(), box.maxLongitude())
        .stream()
        .filter(report -> GeoDistance.haversineKm(location, report.getLocation()) <= radiusKm)
        .toList();
  }

  /**
   * Helper of {@code findNearby}; controls never call it. Finds the reports captured at or after a
   * time whose location lies inside a box of latitudes and longitudes.
   *
   * @param since the earliest capture time, inclusive
   * @param minLatitude the southern edge
   * @param maxLatitude the northern edge
   * @param minLongitude the western edge
   * @param maxLongitude the eastern edge
   * @return the candidate reports
   */
  @Query(
      "select r from HazardReport r where r.capturedAt >= :since"
          + " and r.location.latitude between :minLatitude and :maxLatitude"
          + " and r.location.longitude between :minLongitude and :maxLongitude"
          + " order by r.capturedAt")
  List<HazardReport> findCapturedInBox(
      @Param("since") LocalDateTime since,
      @Param("minLatitude") double minLatitude,
      @Param("maxLatitude") double maxLatitude,
      @Param("minLongitude") double minLongitude,
      @Param("maxLongitude") double maxLongitude);

  /**
   * Finds the reports a citizen submitted, newest first (MD section 6, {@code findByReporter}).
   *
   * @param citizenId the identifier of the citizen
   * @return the citizen's reports
   */
  @Query(
      "select r from HazardReport r where r.reporter.userId = :citizenId"
          + " order by r.reportTime desc")
  List<HazardReport> findByReporter(@Param("citizenId") String citizenId);
}
