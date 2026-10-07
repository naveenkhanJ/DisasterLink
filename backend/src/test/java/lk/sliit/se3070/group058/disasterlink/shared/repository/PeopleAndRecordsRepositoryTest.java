package lk.sliit.se3070.group058.disasterlink.shared.repository;

import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RecipientKind;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReliefSupply;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyCategory;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Tests of the other repositories of {@code shared.repository}. */
@DataJpaTest
class PeopleAndRecordsRepositoryTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);
  private static final LocalDate PERIOD_START = LocalDate.of(2026, 5, 20);
  private static final LocalDate PERIOD_END = LocalDate.of(2026, 5, 25);

  @Autowired private TestEntityManager entityManager;
  @Autowired private NotificationRepository notifications;
  @Autowired private CitizenRepository citizens;
  @Autowired private DistrictOfficerRepository districtOfficers;
  @Autowired private ClientSyncStatusRepository clientStatuses;
  @Autowired private OrganizationRepository organizations;
  @Autowired private DMCOfficerRepository dmcOfficers;
  @Autowired private DMCDutyOfficerRepository dutyOfficers;
  @Autowired private ProcessedSubmissionRepository processedSubmissions;
  @Autowired private ResourceDistributionRepository distributions;
  @Autowired private AuditRecordRepository auditRecords;

  private void save(Object... entities) {
    for (Object entity : entities) {
      entityManager.persist(entity);
    }
    entityManager.flush();
    entityManager.clear();
  }

  private static Notification delivered(
      String recipientId, RecipientKind kind, NotificationChannel channel, int round) {
    return new Notification(
        kind, recipientId, channel, "text", round, DeliveryStatus.DELIVERED, NOW);
  }

  // ---------------------------------------------------------------- NotificationRepository

  private void saveWarningsWithNotifications() {
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    Hazard hazard =
        FeatureFixtures.hazard(
            "H-1",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            NOW.minusDays(1),
            null,
            ORIGIN,
            District.KALUTARA);
    Warning inScope =
        FeatureFixtures.warning(
            "W1",
            hazard,
            officer,
            NOW.minusHours(5),
            NOW.plusDays(1),
            List.of(District.KALUTARA),
            List.of());
    Warning outOfScope =
        FeatureFixtures.warning(
            "W2",
            hazard,
            officer,
            NOW.minusHours(4),
            NOW.plusDays(1),
            List.of(District.KALUTARA),
            List.of());
    FeatureFixtures.addNotifications(
        inScope,
        delivered("C1", RecipientKind.CITIZEN, NotificationChannel.SMS, 1),
        delivered("C1", RecipientKind.CITIZEN, NotificationChannel.SMS, 2),
        delivered("C1", RecipientKind.CITIZEN, NotificationChannel.PUSH, 1),
        delivered("C2", RecipientKind.CITIZEN, NotificationChannel.SMS, 1),
        delivered("C3", RecipientKind.CITIZEN, NotificationChannel.SMS, 1),
        new Notification(
            RecipientKind.CITIZEN,
            "C4",
            NotificationChannel.SMS,
            "text",
            1,
            DeliveryStatus.FAILED,
            NOW),
        new Notification(
            RecipientKind.CITIZEN,
            "C5",
            NotificationChannel.SMS,
            "text",
            1,
            DeliveryStatus.PENDING,
            null),
        delivered("ORG-1", RecipientKind.ORGANIZATION, NotificationChannel.SMS, 1));
    FeatureFixtures.addNotifications(
        outOfScope, delivered("C6", RecipientKind.CITIZEN, NotificationChannel.AUDIBLE_ALERT, 1));
    save(
        officer,
        hazard,
        FeatureFixtures.citizenIn("C1", District.KALUTARA),
        FeatureFixtures.citizenIn("C2", District.KALUTARA),
        FeatureFixtures.citizenIn("C3", District.RATNAPURA),
        FeatureFixtures.citizenIn("C4", District.KALUTARA),
        FeatureFixtures.citizenIn("C5", District.KALUTARA),
        FeatureFixtures.citizenIn("C6", District.KALUTARA),
        inScope,
        outOfScope);
  }

  @Test
  @DisplayName("countCitizensReachedByChannel counts distinct delivered citizens per channel")
  void countCitizensReachedByChannel() {
    saveWarningsWithNotifications();

    Map<NotificationChannel, Integer> counts =
        notifications.countCitizensReachedByChannel(Set.of("W1"), Set.of(District.KALUTARA));

    // C1 and C2 on SMS (C1's two SMS count once); C1 on PUSH; C3 is in Ratnapura; C4 failed;
    // C5 is pending; the organisation is not a citizen; C6 belongs to the warning outside the ids.
    assertThat(counts)
        .containsOnly(
            Map.entry(NotificationChannel.SMS, 2), Map.entry(NotificationChannel.PUSH, 1));
    assertThat(counts).doesNotContainKey(NotificationChannel.AUDIBLE_ALERT);
  }

  @Test
  @DisplayName("countCitizensReached counts each reached citizen once across channels")
  void countCitizensReached() {
    saveWarningsWithNotifications();

    assertThat(notifications.countCitizensReached(Set.of("W1"), Set.of(District.KALUTARA)))
        .isEqualTo(2);
    assertThat(notifications.countCitizensReached(Set.of("W1", "W2"), Set.of(District.KALUTARA)))
        .isEqualTo(3);
    assertThat(
            notifications.countCitizensReached(
                Set.of("W1"), Set.of(District.KALUTARA, District.RATNAPURA)))
        .isEqualTo(3);
    assertThat(notifications.countCitizensReached(Set.of("W1"), Set.of(District.GALLE))).isZero();
  }

  @Test
  @DisplayName(
      "countByWarnings counts every notification of the warnings, whatever its recipient or status")
  void countByWarnings() {
    saveWarningsWithNotifications();

    assertThat(notifications.countByWarnings(Set.of("W1"))).isEqualTo(8);
    assertThat(notifications.countByWarnings(Set.of("W1", "W2"))).isEqualTo(9);
    assertThat(notifications.countByWarnings(Set.of("W2"))).isEqualTo(1);
    assertThat(notifications.countByWarnings(Set.of("NO-SUCH"))).isZero();
  }

  @Test
  @DisplayName("The counts are zero for empty sets")
  void countsForEmptySets() {
    saveWarningsWithNotifications();

    assertThat(notifications.countByWarnings(Set.of())).isZero();
    assertThat(notifications.countCitizensReached(Set.of(), Set.of(District.KALUTARA))).isZero();
    assertThat(notifications.countCitizensReached(Set.of("W1"), Set.of())).isZero();
    assertThat(notifications.countCitizensReachedByChannel(Set.of(), Set.of())).isEmpty();
  }

  // ---------------------------------------------------------------- finders by district

  @Test
  @DisplayName("findByDistrictIn on citizens, district officers and client statuses")
  void findByDistrictIn() {
    LocalDateTime now = NOW;
    save(
        FeatureFixtures.citizenIn("C1", District.KALUTARA),
        FeatureFixtures.citizenIn("C2", District.GALLE),
        FeatureFixtures.citizenIn("C3", District.JAFFNA),
        SampleEntities.districtOfficer("DO-COL-01"),
        new ClientSyncStatus("client-1", District.KALUTARA, now, 0),
        new ClientSyncStatus("client-2", District.JAFFNA, now, 1));

    assertThat(citizens.findByDistrictIn(Set.of(District.KALUTARA, District.GALLE)))
        .extracting(Citizen::getUserId)
        .containsExactlyInAnyOrder("C1", "C2");
    assertThat(citizens.findByDistrictIn(Set.of(District.MATARA))).isEmpty();
    assertThat(districtOfficers.findByDistrictIn(Set.of(District.COLOMBO, District.GALLE)))
        .extracting(DistrictOfficer::getUserId)
        .containsExactly("DO-COL-01");
    assertThat(districtOfficers.findByDistrictIn(Set.of(District.GALLE))).isEmpty();
    assertThat(clientStatuses.findByDistrictIn(Set.of(District.JAFFNA)))
        .extracting(ClientSyncStatus::getClientId)
        .containsExactly("client-2");
  }

  @Test
  @DisplayName(
      "Organization findByDistrictIn finds each organisation once, and findAllById is inherited")
  void organizationFindByDistrictIn() {
    save(SampleEntities.organization("ORG-1"));

    assertThat(organizations.findByDistrictIn(Set.of(District.COLOMBO, District.GAMPAHA)))
        .extracting(Organization::getOrganizationId)
        .containsExactly("ORG-1");
    assertThat(organizations.findByDistrictIn(Set.of(District.JAFFNA))).isEmpty();
    assertThat(organizations.findAllById(List.of("ORG-1", "ORG-2"))).hasSize(1);
  }

  // ---------------------------------------------------------------- officers and submissions

  @Test
  @DisplayName("The typed officer repositories find their own kind only")
  void typedOfficerRepositories() {
    save(SampleEntities.dutyOfficer("DUTY-1"), SampleEntities.officer("DMC-2", Set.of()));

    assertThat(dmcOfficers.findById("DUTY-1"))
        .get()
        .isInstanceOf(lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer.class);
    assertThat(dmcOfficers.findById("DMC-2")).isPresent();
    assertThat(dutyOfficers.findById("DUTY-1")).isPresent();
    assertThat(dutyOfficers.findById("DMC-2")).isEmpty();
  }

  @Test
  @DisplayName("findBySubmissionId finds an accepted submission and nothing else")
  void findBySubmissionId() {
    save(new ProcessedSubmission("sub-1", "client-1", NOW, NOW.plusMinutes(1), "RPT-1"));

    assertThat(processedSubmissions.findBySubmissionId("sub-1"))
        .get()
        .extracting(ProcessedSubmission::getClientId)
        .isEqualTo("client-1");
    assertThat(processedSubmissions.findBySubmissionId("sub-2")).isEmpty();
  }

  // ---------------------------------------------------------------- ResourceDistributionRepository

  @Test
  @DisplayName("Distribution findMatching applies district, period and the hazard-type rule")
  void distributionFindMatching() {
    Hazard flood =
        FeatureFixtures.hazard(
            "H-F",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            NOW.minusDays(9),
            null,
            ORIGIN,
            District.RATNAPURA);
    Hazard slide =
        FeatureFixtures.hazard(
            "H-L",
            HazardType.LANDSLIDE,
            HazardStatus.ACTIVE,
            NOW.minusDays(9),
            null,
            ORIGIN,
            District.RATNAPURA);
    DistrictOfficer coordinator = SampleEntities.districtOfficer("DO-1");
    ReliefSupply supply =
        new ReliefSupply(
            "SUP-1",
            "Rice",
            SupplyCategory.FOOD,
            100,
            "kg",
            "Store",
            SupplyStatus.AVAILABLE,
            null,
            null);
    LocalDateTime start = PERIOD_START.atStartOfDay();
    LocalDateTime endExclusive = PERIOD_END.plusDays(1).atStartOfDay();
    save(
        flood,
        slide,
        coordinator,
        supply,
        distribution("D1", start, District.RATNAPURA, supply, coordinator, flood),
        distribution("D2", endExclusive, District.RATNAPURA, supply, coordinator, flood),
        distribution(
            "D3", endExclusive.minusMinutes(1), District.RATNAPURA, supply, coordinator, null),
        distribution("D4", start.plusDays(2), District.RATNAPURA, supply, coordinator, slide),
        distribution("D5", start.plusDays(1), District.GALLE, supply, coordinator, flood),
        distribution("D6", start.minusMinutes(1), District.RATNAPURA, supply, coordinator, flood));

    ReportCriteria flood20 =
        FeatureFixtures.criteria(
            Set.of(District.RATNAPURA), HazardType.FLOOD, PERIOD_START, PERIOD_END);
    ReportCriteria anyType =
        FeatureFixtures.criteria(Set.of(District.RATNAPURA), null, PERIOD_START, PERIOD_END);

    // D1 at the start (counts); D2 at the end (does not); D3 has no hazard (only without a type);
    // D4 has another hazard type; D5 is in another district; D6 is a minute before the start.
    assertThat(distributions.findMatching(flood20))
        .extracting(ResourceDistribution::getDistributionId)
        .containsExactly("D1");
    assertThat(distributions.findMatching(anyType))
        .extracting(ResourceDistribution::getDistributionId)
        .containsExactly("D1", "D4", "D3");
  }

  private static ResourceDistribution distribution(
      String id,
      LocalDateTime date,
      District district,
      ReliefSupply supply,
      DistrictOfficer coordinator,
      Hazard hazard) {
    return new ResourceDistribution(
        id, 10, date, district, "Camp", supply, null, coordinator, hazard);
  }

  // ---------------------------------------------------------------- AuditRecordRepository

  @Test
  @DisplayName("findEscalations gives the escalation records of the warnings, oldest first")
  void findEscalations() {
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    AuditRecord late = new AuditRecord(AuditAction.WARNING_ESCALATED, officer, "W1", "A→B", NOW);
    AuditRecord early =
        new AuditRecord(AuditAction.WARNING_ESCALATED, officer, "W1", "B→C", NOW.minusHours(1));
    AuditRecord other = new AuditRecord(AuditAction.WARNING_ESCALATED, officer, "W9", null, NOW);
    AuditRecord notEscalation =
        new AuditRecord(AuditAction.REPORT_GENERATED, officer, "W1", null, NOW);
    save(officer, late, early, other, notEscalation);

    assertThat(auditRecords.findEscalations(Set.of("W1")))
        .extracting(AuditRecord::getAuditId)
        .containsExactly(early.getAuditId(), late.getAuditId());
    assertThat(auditRecords.findEscalations(Set.of("W1", "W9"))).hasSize(3);
    assertThat(auditRecords.findEscalations(Set.of("W5"))).isEmpty();
    assertThat(auditRecords.findEscalations(Set.of())).isEmpty();
  }
}
