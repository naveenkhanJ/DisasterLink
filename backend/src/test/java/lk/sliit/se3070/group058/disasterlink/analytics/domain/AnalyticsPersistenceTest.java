package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AlertTimelineEntry;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.TimelineEvent;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Round trips of {@link DisasterReport} and {@link CriteriaTemplate}, including the JSON columns.
 */
@DataJpaTest
class AnalyticsPersistenceTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 25, 10, 0, 30);

  private static final ReportCriteria CRITERIA =
      new ReportCriteria(
          Set.of(District.RATNAPURA, District.KALUTARA),
          HazardType.FLOOD,
          LocalDate.of(2026, 5, 20),
          LocalDate.of(2026, 5, 25),
          Set.of(ReportMetric.ALERT_TIMELINE, ReportMetric.CITIZENS_REACHED),
          "HZ-2026-0142");

  private static final ReportResults RESULTS =
      new ReportResults(
          7,
          List.of(
              new AlertTimelineEntry(
                  "WRN-1",
                  NOW,
                  WarningLevel.LEVEL_2_AMBER,
                  Set.of(District.RATNAPURA),
                  TimelineEvent.ISSUED)),
          Map.of(NotificationChannel.SMS, 12),
          12,
          List.of(),
          0,
          List.of(),
          Set.of(ReportMetric.SHELTER_OCCUPANCY_OVER_TIME));

  @Autowired private TestEntityManager entityManager;

  @Test
  @DisplayName("A new report keeps its criteria and results as JSON, its hazards and its officer")
  void newReportRoundTrip() {
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    Hazard hazard = SampleEntities.hazard("HZ-2026-0142");
    entityManager.persist(officer);
    entityManager.persist(hazard);
    entityManager.persistAndFlush(
        new DisasterReport(
            "DR-2026-0200", "Kalu Ganga flood", NOW, CRITERIA, RESULTS, officer, List.of(hazard)));
    entityManager.clear();

    DisasterReport loaded = entityManager.find(DisasterReport.class, "DR-2026-0200");

    assertThat(loaded.getCriteria()).isEqualTo(CRITERIA);
    assertThat(loaded.getResults()).isEqualTo(RESULTS);
    assertThat(loaded.getGeneratedDate()).isEqualTo(NOW);
    assertThat(loaded.getOfficer().getUserId()).isEqualTo("DMC-1");
    assertThat(loaded.getHazards()).extracting(Hazard::getHazardId).containsExactly("HZ-2026-0142");
    assertThat(loaded.getFindings()).isEmpty();
    assertThat(loaded.getProvisionalSince()).isEmpty();
    assertThat(loaded.getSharedWith()).isEmpty();
  }

  @Test
  @DisplayName("A seeded report keeps findings, provisional districts and shares")
  void seededReportRoundTrip() {
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    Organization organization = SampleEntities.organization("ORG-0001");
    entityManager.persist(officer);
    entityManager.persist(organization);
    entityManager.persistAndFlush(
        new DisasterReport(
            "DR-2026-0201",
            "Second",
            NOW,
            CRITERIA,
            RESULTS,
            officer,
            List.of(),
            "Evacuations worked",
            Map.of(District.KALUTARA, NOW.minusDays(1)),
            List.of(organization)));
    entityManager.clear();

    DisasterReport loaded = entityManager.find(DisasterReport.class, "DR-2026-0201");

    assertThat(loaded.getFindings()).contains("Evacuations worked");
    assertThat(loaded.getProvisionalSince())
        .containsOnly(Map.entry(District.KALUTARA, NOW.minusDays(1)));
    assertThat(loaded.getSharedWith())
        .extracting(Organization::getOrganizationId)
        .containsExactly("ORG-0001");
  }

  @Test
  @DisplayName("A criteria template keeps its criteria as JSON and its owner")
  void templateRoundTrip() {
    DMCOfficer owner = SampleEntities.officer("DMC-2", Set.of());
    entityManager.persist(owner);
    entityManager.persistAndFlush(new CriteriaTemplate("TPL-1", "Monsoon", CRITERIA, owner));
    entityManager.clear();

    CriteriaTemplate loaded = entityManager.find(CriteriaTemplate.class, "TPL-1");

    assertThat(loaded.getCriteria()).isEqualTo(CRITERIA);
    assertThat(loaded.getName()).isEqualTo("Monsoon");
    assertThat(loaded.getOwner().getUserId()).isEqualTo("DMC-2");
  }
}
