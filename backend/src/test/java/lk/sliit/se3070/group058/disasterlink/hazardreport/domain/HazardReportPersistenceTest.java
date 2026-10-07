package lk.sliit.se3070.group058.disasterlink.hazardreport.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDraft;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Round trips of {@link HazardReport} through H2, including its optimistic-lock version. */
@DataJpaTest
class HazardReportPersistenceTest {

  private static final LocalDateTime CAPTURED = LocalDateTime.of(2026, 5, 21, 8, 0);
  private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 5, 21, 8, 5);

  @Autowired private TestEntityManager entityManager;

  private Citizen reporter() {
    Citizen reporter = SampleEntities.citizen("CIT-KLT-0001", "token");
    entityManager.persist(reporter);
    return reporter;
  }

  @Test
  @DisplayName("A report from the drawn constructor is stored with location, photo and no decision")
  void drawnReportRoundTrip() {
    ReportDraft draft =
        new ReportDraft(
            "draft-1",
            HazardType.FLOOD,
            "Water is rising",
            new GeoPoint(6.5888, 79.9640, 9.5),
            true,
            District.KALUTARA,
            CAPTURED);
    HazardReport report =
        new HazardReport("RPT-2026-KLT-1000", reporter(), draft, "p.jpg", RECEIVED);
    entityManager.persistAndFlush(report);
    entityManager.clear();

    HazardReport loaded = entityManager.find(HazardReport.class, "RPT-2026-KLT-1000");

    assertThat(loaded.getLocation()).isEqualTo(new GeoPoint(6.5888, 79.9640, 9.5));
    assertThat(loaded.isLocationFromGps()).isTrue();
    assertThat(loaded.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(loaded.getPhoto()).isEqualTo("p.jpg");
    assertThat(loaded.getReporter().getUserId()).isEqualTo("CIT-KLT-0001");
    assertThat(loaded.getReviewer()).isEmpty();
    assertThat(loaded.getHazard()).isEmpty();
    assertThat(loaded.getVerificationTime()).isEmpty();
    assertThat(loaded.getRejectionReason()).isEmpty();
    assertThat(loaded).isEqualTo(report);
  }

  @Test
  @DisplayName("A decided report from the seed constructor keeps reviewer, hazard and reason")
  void decidedReportRoundTrip() {
    DMCDutyOfficer reviewer = SampleEntities.dutyOfficer("DMC-9");
    Hazard hazard = SampleEntities.hazard("HZ-2026-0142");
    entityManager.persist(reviewer);
    entityManager.persist(hazard);
    HazardReport report =
        new HazardReport(
            "RPT-1",
            reporter(),
            HazardType.FLOOD,
            "d",
            "p.jpg",
            SampleEntities.RATNAPURA_POINT,
            false,
            District.RATNAPURA,
            CAPTURED,
            RECEIVED,
            ReportStatus.REJECTED,
            RECEIVED.plusHours(2),
            "Not a flood",
            reviewer,
            hazard);
    entityManager.persistAndFlush(report);
    entityManager.clear();

    HazardReport loaded = entityManager.find(HazardReport.class, "RPT-1");

    assertThat(loaded.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(loaded.getVerificationTime()).contains(RECEIVED.plusHours(2));
    assertThat(loaded.getRejectionReason()).contains("Not a flood");
    assertThat(loaded.getReviewer().orElseThrow().getUserId()).isEqualTo("DMC-9");
    assertThat(loaded.getHazard().orElseThrow().getHazardId()).isEqualTo("HZ-2026-0142");
    assertThat(loaded.isLocationFromGps()).isFalse();
  }

  @Test
  @DisplayName("The version is 0 on insert and rises by one for each versioned update")
  void versionIncreasesWithEachVersionedUpdate() {
    entityManager.persistAndFlush(
        new HazardReport(
            "RPT-2",
            reporter(),
            HazardType.FLOOD,
            "d",
            "p.jpg",
            SampleEntities.RATNAPURA_POINT,
            true,
            District.GALLE,
            CAPTURED,
            RECEIVED,
            ReportStatus.PENDING_VERIFICATION,
            null,
            null,
            null,
            null));
    entityManager.clear();
    assertThat(versionOf("RPT-2")).isZero();

    // The entity has no setter for the version (and none for the status), so the update goes
    // through a Hibernate "update versioned" statement, which raises the version itself.
    entityManager
        .getEntityManager()
        .createQuery(
            "update versioned HazardReport r set r.status = :status where r.reportId = :id")
        .setParameter("status", ReportStatus.VERIFIED)
        .setParameter("id", "RPT-2")
        .executeUpdate();
    entityManager.clear();

    assertThat(versionOf("RPT-2")).isEqualTo(1);
    assertThat(entityManager.find(HazardReport.class, "RPT-2").getStatus())
        .isEqualTo(ReportStatus.VERIFIED);
  }

  private long versionOf(String reportId) {
    return entityManager
        .getEntityManager()
        .createQuery("select r.version from HazardReport r where r.reportId = :id", Long.class)
        .setParameter("id", reportId)
        .getSingleResult();
  }
}
