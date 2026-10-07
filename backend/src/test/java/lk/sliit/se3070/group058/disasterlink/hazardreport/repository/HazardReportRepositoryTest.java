package lk.sliit.se3070.group058.disasterlink.hazardreport.repository;

import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.DIAGONAL;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.NORTH_3_KM;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.NORTH_7_KM;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Tests of {@link HazardReportRepository}. */
@DataJpaTest
class HazardReportRepositoryTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

  @Autowired private TestEntityManager entityManager;
  @Autowired private HazardReportRepository reports;

  private void save(Object... entities) {
    for (Object entity : entities) {
      entityManager.persist(entity);
    }
    entityManager.flush();
    entityManager.clear();
  }

  private static List<String> ids(List<HazardReport> found) {
    return found.stream().map(HazardReport::getReportId).toList();
  }

  @Test
  @DisplayName("findPending gives the pending reports, oldest received first")
  void findPending() {
    Citizen citizen = FeatureFixtures.citizenIn("C1", District.COLOMBO);
    save(
        citizen,
        FeatureFixtures.report(
            "R-LATE", citizen, ORIGIN, NOW, NOW.plusHours(2), ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report(
            "R-EARLY", citizen, ORIGIN, NOW, NOW.plusHours(1), ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report("R-DONE", citizen, ORIGIN, NOW, NOW, ReportStatus.VERIFIED),
        FeatureFixtures.report("R-REJECTED", citizen, ORIGIN, NOW, NOW, ReportStatus.REJECTED));

    assertThat(ids(reports.findPending())).containsExactly("R-EARLY", "R-LATE");
  }

  @Test
  @DisplayName(
      "findNearby keeps reports within the radius (3 km yes, 7 km no) and captured since the time")
  void findNearby() {
    Citizen citizen = FeatureFixtures.citizenIn("C1", District.COLOMBO);
    save(
        citizen,
        FeatureFixtures.report(
            "R-ORIGIN", citizen, ORIGIN, NOW, NOW, ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report("R-3KM", citizen, NORTH_3_KM, NOW, NOW, ReportStatus.VERIFIED),
        FeatureFixtures.report(
            "R-7KM", citizen, NORTH_7_KM, NOW, NOW, ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report(
            "R-DIAGONAL", citizen, DIAGONAL, NOW, NOW, ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report(
            "R-OLD", citizen, NORTH_3_KM, NOW.minusDays(2), NOW, ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report(
            "R-EDGE",
            citizen,
            NORTH_3_KM,
            NOW.minusDays(1),
            NOW,
            ReportStatus.PENDING_VERIFICATION));

    List<HazardReport> found = reports.findNearby(ORIGIN, 5.0, NOW.minusDays(1));

    // R-7KM is outside the radius; R-DIAGONAL is inside the box but 6 km away; R-OLD was captured
    // before the cut-off; R-EDGE was captured exactly at the cut-off (counts). The selected report
    // (R-ORIGIN here) is not removed: that is the caller's job.
    assertThat(ids(found)).containsExactlyInAnyOrder("R-ORIGIN", "R-3KM", "R-EDGE");
    assertThat(ids(reports.findNearby(ORIGIN, 8.0, NOW.minusDays(1))))
        .containsExactlyInAnyOrder("R-ORIGIN", "R-3KM", "R-EDGE", "R-7KM", "R-DIAGONAL");
    assertThat(reports.findNearby(ORIGIN, 1.0, NOW.minusDays(1)))
        .extracting(HazardReport::getReportId)
        .containsExactly("R-ORIGIN");
  }

  @Test
  @DisplayName("findByReporter gives one citizen's reports, newest first")
  void findByReporter() {
    Citizen first = FeatureFixtures.citizenIn("C1", District.COLOMBO);
    Citizen second = FeatureFixtures.citizenIn("C2", District.COLOMBO);
    save(
        first,
        second,
        FeatureFixtures.report("R-OLD", first, ORIGIN, NOW, NOW, ReportStatus.VERIFIED),
        FeatureFixtures.report(
            "R-NEW", first, ORIGIN, NOW, NOW.plusHours(1), ReportStatus.PENDING_VERIFICATION),
        FeatureFixtures.report(
            "R-OTHER", second, ORIGIN, NOW, NOW, ReportStatus.PENDING_VERIFICATION));

    assertThat(ids(reports.findByReporter("C1"))).containsExactly("R-NEW", "R-OLD");
    assertThat(reports.findByReporter("C3")).isEmpty();
  }
}
