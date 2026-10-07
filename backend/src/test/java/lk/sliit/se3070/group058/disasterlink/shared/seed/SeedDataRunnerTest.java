package lk.sliit.se3070.group058.disasterlink.shared.seed;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.analytics.repository.CriteriaTemplateRepository;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.config.DemoUserProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RecipientKind;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.NotificationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ResourceDistributionRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.repository.ShelterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the seed of stage S4: every fixed identifier and fact of the seed plan, the finders the
 * scenarios rely on, the sequences' start values, the demo users and the pending client. The test
 * calls the runner directly (see {@link #runnerOf}); the {@code test} profile keeps the runner out
 * of normal contexts, and this class runs under the {@code dev} profile, which the {@code !test}
 * condition of the seed classes accepts.
 */
@DataJpaTest
@ActiveProfiles("dev")
@Import({
  PeopleSeed.class,
  CompletedStorySeed.class,
  LiveStorySeed.class,
  ShelterSeed.class,
  ReportingSeed.class,
  IdGenerator.class,
  CurrentUserService.class,
  SeedDataRunnerTest.FixedClockConfiguration.class
})
@EnableConfigurationProperties(DemoUserProperties.class)
class SeedDataRunnerTest {

  /** The moment the seed runs: 12 October 2026, 10:30 in Colombo. */
  static final Instant INSTANT = Instant.parse("2026-10-12T05:00:00Z");

  /** Supplies a clock fixed at {@link #INSTANT}. */
  @TestConfiguration
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(INSTANT, ZoneId.of("Asia/Colombo"));
    }
  }

  private static final GeoPoint KADUWELA = new GeoPoint(6.9356, 79.9843, null);

  @Autowired private AutowireCapableBeanFactory beans;

  @Autowired private Clock clock;

  @Autowired private HazardRepository hazards;

  @Autowired private WarningRepository warnings;

  @Autowired private HazardReportRepository reports;

  @Autowired private NotificationRepository notifications;

  @Autowired private ResourceDistributionRepository distributions;

  @Autowired private OrganizationRepository organizations;

  @Autowired private CitizenRepository citizens;

  @Autowired private ShelterRepository shelters;

  @Autowired private AuditRecordRepository auditRecords;

  @Autowired private CriteriaTemplateRepository templates;

  @Autowired private ClientSyncStatusRepository clients;

  @Autowired private CurrentUserService currentUsers;

  @Autowired private IdGenerator generator;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private DemoUserProperties demoUsers;

  @PersistenceContext private EntityManager entityManager;

  private LocalDateTime now;

  /**
   * Creates the runner without registering it: a registered runner would be called by Spring Boot
   * when the test context starts, outside the test's transaction, and the test would then seed
   * twice.
   */
  static SeedDataRunner runnerOf(AutowireCapableBeanFactory beans) {
    return beans.createBean(SeedDataRunner.class);
  }

  @BeforeEach
  void seed() {
    // Sequences are not rolled back with the test transaction.
    jdbcTemplate.execute("ALTER SEQUENCE hazard_id_seq RESTART WITH 300");
    jdbcTemplate.execute("ALTER SEQUENCE warning_id_seq RESTART WITH 200");
    jdbcTemplate.execute("ALTER SEQUENCE hazard_report_id_seq RESTART WITH 1000");
    jdbcTemplate.execute("ALTER SEQUENCE disaster_report_id_seq RESTART WITH 184");
    jdbcTemplate.execute("ALTER SEQUENCE citizen_id_seq RESTART WITH 1000");
    runnerOf(beans).run(null);
    now = LocalDateTime.now(clock);
  }

  @Test
  @DisplayName(
      "The completed hazard HZ-2026-0142 is a closed Kalu Ganga flood that ended before now")
  void completedHazard() {
    Hazard hazard = hazards.findById("HZ-2026-0142").orElseThrow();

    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.CLOSED);
    assertThat(hazard.getType()).isEqualTo(HazardType.FLOOD);
    assertThat(hazard.getSeverity()).isEqualTo(HazardSeverity.MEDIUM);
    assertThat(hazard.getSource()).isEqualTo(HazardSource.AGENCY_BULLETIN);
    assertThat(hazard.getRiverBasin()).contains(RiverBasin.KALU);
    assertThat(hazard.getDistricts())
        .containsExactlyInAnyOrder(District.RATNAPURA, District.KALUTARA);
    assertThat(hazard.getEndTime()).hasValueSatisfying(end -> assertThat(end).isBefore(now));
  }

  @Test
  @DisplayName("WRN-2026-0087 is stored at Level 3 - Red with both rounds and a failed SMS")
  void completedWarning() {
    Warning warning = warnings.findById("WRN-2026-0087").orElseThrow();
    List<Notification> sent = warning.getNotifications();

    assertThat(warning.getHazard().getHazardId()).isEqualTo("HZ-2026-0142");
    assertThat(warning.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(warning.getTargetDistricts()).containsExactly(District.RATNAPURA);
    assertThat(warning.getTargetRiverBasins()).containsExactly(RiverBasin.KALU);
    assertThat(warning.getOfficer().getUserId()).isEqualTo("DMC-0001");
    assertThat(warning.getExpiryTime()).isBefore(now);
    assertThat(warnings.findActiveAt(now))
        .extracting(Warning::getWarningId)
        .doesNotContain("WRN-2026-0087");
    assertThat(sent).extracting(Notification::getRound).contains(1, 2);
    assertThat(sent)
        .extracting(Notification::getDeliveryStatus)
        .contains(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED);
    assertThat(sent)
        .filteredOn(notification -> notification.getRound() == 2)
        .extracting(Notification::getRecipientKind)
        .contains(
            RecipientKind.CITIZEN, RecipientKind.ORGANIZATION, RecipientKind.DISTRICT_OFFICER);
    assertThat(sent)
        .filteredOn(notification -> notification.getRound() == 1)
        .extracting(Notification::getRecipientKind)
        .doesNotContain(RecipientKind.DISTRICT_OFFICER);
    assertThat(sent)
        .filteredOn(notification -> notification.getType() == NotificationChannel.SMS)
        .allSatisfy(notification -> assertThat(notification.getMessage()).contains("\n"));
    assertThat(
            notifications.countCitizensReached(
                Set.of("WRN-2026-0087"), Set.of(District.RATNAPURA, District.KALUTARA)))
        .isPositive();
  }

  @Test
  @DisplayName(
      "The escalation audit record names DMC-0002, the warning and LEVEL_2_AMBER→LEVEL_3_RED")
  void escalationAuditRecord() {
    List<AuditRecord> escalations =
        auditRecords.findAll().stream()
            .filter(record -> record.getAction() == AuditAction.WARNING_ESCALATED)
            .toList();

    assertThat(escalations).hasSize(1);
    assertThat(escalations.get(0).getSubjectId()).isEqualTo("WRN-2026-0087");
    assertThat(escalations.get(0).getDetail()).contains("LEVEL_2_AMBER→LEVEL_3_RED");
    assertThat(escalations.get(0).getPerformedBy().getUserId()).isEqualTo("DMC-0002");
  }

  @Test
  @DisplayName(
      "The completed storyline's reports 0843, 0845 and 0847 are verified; 0852 is not stored")
  void completedReports() {
    HazardReport first = reports.findById("RPT-2026-KLT-0847").orElseThrow();

    assertThat(first.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(first.getReviewer().orElseThrow().getUserId()).isEqualTo("DMC-0002");
    assertThat(first.getHazard().orElseThrow().getHazardId()).isEqualTo("HZ-2026-0142");
    assertThat(first.getLocation().accuracyMetres()).isEqualTo(8.0);
    for (String id : List.of("RPT-2026-KLT-0843", "RPT-2026-KLT-0845")) {
      HazardReport duplicate = reports.findById(id).orElseThrow();
      assertThat(duplicate.getStatus()).isEqualTo(ReportStatus.VERIFIED);
      assertThat(duplicate.getReviewer().orElseThrow().getUserId()).isEqualTo("DMC-0002");
      assertThat(duplicate.getHazard().orElseThrow().getHazardId()).isEqualTo("HZ-2026-0142");
      assertThat(duplicate.getVerificationTime().orElseThrow())
          .isAfter(first.getVerificationTime().orElseThrow())
          .isBefore(first.getVerificationTime().orElseThrow().plusMinutes(10));
    }
    assertThat(reports.findById("RPT-2026-KLT-0852")).isEmpty();
  }

  @Test
  @DisplayName("Only the live reports 0901 to 0905 are pending")
  void onlyLiveReportsArePending() {
    assertThat(reports.findPending())
        .extracting(HazardReport::getReportId)
        .containsExactlyInAnyOrder(
            "RPT-2026-KLT-0901",
            "RPT-2026-KLT-0902",
            "RPT-2026-KLT-0903",
            "RPT-2026-KLT-0904",
            "RPT-2026-COL-0905");
  }

  @Test
  @DisplayName(
      "The live Kalu Ganga flood HZ-2026-0201 has the valid Level 2 - Amber warning WRN-2026-0101")
  void liveHazardAndWarning() {
    Hazard hazard = hazards.findById("HZ-2026-0201").orElseThrow();

    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.ACTIVE);
    assertThat(warnings.findActiveByHazard("HZ-2026-0201", now).orElseThrow().getWarningId())
        .isEqualTo("WRN-2026-0101");
    assertThat(warnings.findActiveByHazard("HZ-2026-0202", now)).isEmpty();
    assertThat(warnings.findActiveByDistrict(District.KALUTARA, now))
        .extracting(Warning::getWarningId)
        .contains("WRN-2026-0101");
    Warning live = warnings.findById("WRN-2026-0101").orElseThrow();
    assertThat(live.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(live.getNotifications())
        .isNotEmpty()
        .allSatisfy(notification -> assertThat(notification.getRound()).isEqualTo(1));
  }

  @Test
  @DisplayName("The Kelani Ganga flood HZ-2026-0202 and the district-less HZ-2026-0203 are active")
  void otherLiveHazards() {
    Hazard kelani = hazards.findById("HZ-2026-0202").orElseThrow();

    assertThat(kelani.getType()).isEqualTo(HazardType.FLOOD);
    assertThat(kelani.getSeverity()).isEqualTo(HazardSeverity.MEDIUM);
    assertThat(kelani.getStatus()).isEqualTo(HazardStatus.ACTIVE);
    assertThat(kelani.getRiverBasin()).contains(RiverBasin.KELANI);
    assertThat(kelani.getSource()).isEqualTo(HazardSource.AGENCY_BULLETIN);
    assertThat(kelani.getDistricts()).containsExactlyInAnyOrder(District.COLOMBO, District.GAMPAHA);
    Hazard without = hazards.findById("HZ-2026-0203").orElseThrow();
    assertThat(without.getStatus()).isEqualTo(HazardStatus.ACTIVE);
    assertThat(without.getDistricts()).isEmpty();
    assertThat(hazards.findActiveNear(KADUWELA, HazardType.FLOOD, District.COLOMBO))
        .extracting(Hazard::getHazardId)
        .contains("HZ-2026-0202");
  }

  @Test
  @DisplayName("RPT-2026-KLT-0901 is pending with two nearby reports; the road report is far away")
  void liveReports() {
    HazardReport first = reports.findById("RPT-2026-KLT-0901").orElseThrow();
    LocalDateTime since = now.minusHours(24);

    assertThat(first.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(first.getReporter().getUserId()).isEqualTo("CIT-KLT-0001");
    assertThat(first.getHazardType()).isEqualTo(HazardType.FLOOD);
    assertThat(first.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(reports.findNearby(first.getLocation(), 5, since))
        .extracting(HazardReport::getReportId)
        .containsExactlyInAnyOrder("RPT-2026-KLT-0901", "RPT-2026-KLT-0902", "RPT-2026-KLT-0903");
    HazardReport road = reports.findById("RPT-2026-KLT-0904").orElseThrow();
    assertThat(road.getHazardType()).isEqualTo(HazardType.BLOCKED_ROAD);
    assertThat(road.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(
            hazards.findActiveNear(road.getLocation(), HazardType.BLOCKED_ROAD, District.KALUTARA))
        .isEmpty();
  }

  @Test
  @DisplayName("The Kaduwela report is pending in Colombo and found by a search from Kaduwela")
  void kaduwelaReport() {
    HazardReport kaduwela = reports.findById("RPT-2026-COL-0905").orElseThrow();

    assertThat(kaduwela.getStatus()).isEqualTo(ReportStatus.PENDING_VERIFICATION);
    assertThat(kaduwela.getDistrict()).isEqualTo(District.COLOMBO);
    assertThat(kaduwela.getLocation().latitude()).isEqualTo(6.9356);
    assertThat(kaduwela.getLocation().longitude()).isEqualTo(79.9843);
    assertThat(kaduwela.getCapturedAt()).isBetween(now.minusHours(12), now);
    assertThat(reports.findNearby(KADUWELA, 5, now.minusHours(24)))
        .extracting(HazardReport::getReportId)
        .contains("RPT-2026-COL-0905");
  }

  @Test
  @DisplayName("The Colombo shelters of SC3 belong to DO-COL-01 and have no warning")
  void colomboShelters() {
    Shelter rathmalana = shelters.findById("SHL-COL-0001").orElseThrow();

    assertThat(rathmalana.getName()).isEqualTo("Rathmalana Maha Vidyalaya");
    assertThat(rathmalana.getCapacity()).isEqualTo(500);
    assertThat(rathmalana.getCurrentOccupancy()).isEqualTo(320);
    assertThat(rathmalana.getContactNumber()).isEqualTo("0712345678");
    assertThat(rathmalana.getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(rathmalana.getOfficer().getUserId()).isEqualTo("DO-COL-01");
    assertThat(rathmalana.getWarnings()).isEmpty();
    assertThat(shelters.findById("SHL-COL-0002").orElseThrow().getCapacity()).isEqualTo(400);
    assertThat(shelters.findById("SHL-COL-0002").orElseThrow().getCurrentOccupancy())
        .isEqualTo(120);
    assertThat(shelters.findById("SHL-COL-0003").orElseThrow().getCurrentOccupancy())
        .isEqualTo(410);
    assertThat(shelters.findWithSpace(District.COLOMBO, 200))
        .extracting(Shelter::getName)
        .containsExactly("Dehiwala Municipal Council Hall");
  }

  @Test
  @DisplayName(
      "The event shelters of Ratnapura and Kalutara are linked to WRN-2026-0087 and peak at 94 %")
  void eventShelters() {
    List<Shelter> event =
        Stream.of(District.RATNAPURA, District.KALUTARA)
            .flatMap(district -> shelters.findByDistrict(district).stream())
            .toList();

    assertThat(event)
        .hasSize(8)
        .allSatisfy(
            shelter ->
                assertThat(shelter.getWarnings())
                    .extracting(Warning::getWarningId)
                    .containsExactly("WRN-2026-0087"));
    int capacity = event.stream().mapToInt(Shelter::getCapacity).sum();
    long records =
        entityManager
            .createQuery(
                "select count(r) from Shelter s join s.occupancyRecords r"
                    + " where s.district in :districts",
                Long.class)
            .setParameter("districts", List.of(District.RATNAPURA, District.KALUTARA))
            .getSingleResult();
    long peak =
        entityManager
            .createQuery(
                "select max(r.occupantCount) from Shelter s join s.occupancyRecords r"
                    + " where s.district in :districts group by s.shelterId",
                Integer.class)
            .setParameter("districts", List.of(District.RATNAPURA, District.KALUTARA))
            .getResultList()
            .stream()
            .mapToLong(Integer::longValue)
            .sum();
    assertThat(records).isEqualTo(48);
    assertThat(peak * 100.0 / capacity).isEqualTo(94.0);
  }

  @Test
  @DisplayName("CARE Sri Lanka owns two lines of the event, Sarvodaya none")
  void distributionLines() {
    LocalDate start = now.toLocalDate().minusDays(8);
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA, District.KALUTARA),
            HazardType.FLOOD,
            start,
            start.plusDays(5),
            EnumSet.of(ReportMetric.RESOURCE_DISTRIBUTION),
            null);

    List<ResourceDistribution> lines = distributions.findMatching(criteria);

    assertThat(lines).hasSize(5);
    assertThat(lines)
        .extracting(ResourceDistribution::getQuantity)
        .containsExactlyInAnyOrder(1200.0, 18000.0, 480.0, 350.0, 220.0);
    assertThat(ownerNames(lines, "CARE Sri Lanka")).hasSize(2);
    assertThat(ownerNames(lines, "Sarvodaya")).isEmpty();
    assertThat(ownerNames(lines, "Sri Lanka Red Cross Society")).hasSize(1);
    assertThat(ownerNames(lines, "Sri Lanka Army")).hasSize(1);
    assertThat(lines)
        .allSatisfy(
            line ->
                assertThat(line.getHazard().orElseThrow().getHazardId()).isEqualTo("HZ-2026-0142"));
    assertThat(lines.stream().map(line -> line.getReliefSupply().getUnit()).distinct().count())
        .isGreaterThan(1);
  }

  private static List<ResourceDistribution> ownerNames(
      List<ResourceDistribution> lines, String organization) {
    return lines.stream()
        .filter(
            line ->
                line.getReliefSupply()
                    .getDonorOrganization()
                    .map(donor -> donor.getName().equals(organization))
                    .orElse(false))
        .toList();
  }

  @Test
  @DisplayName("The 14 organisations and the demo citizen exist")
  void people() {
    assertThat(organizations.findById("ORG-0001").orElseThrow().getName())
        .isEqualTo("CARE Sri Lanka");
    assertThat(organizations.count()).isEqualTo(14);
    Citizen demo = citizens.findById("CIT-KLT-0001").orElseThrow();
    assertThat(demo.getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(demo.getDeviceToken()).isPresent();
    assertThat(citizens.findAll().stream().map(Citizen::getPreferredLanguage).distinct().toList())
        .containsExactlyInAnyOrder(Language.values());
    assertThat(
            citizens.findAll().stream()
                .filter(citizen -> citizen.getDeviceToken().isEmpty())
                .count())
        .isPositive();
    assertThat(citizens.findAll().stream().map(Citizen::getDistrict).distinct().toList())
        .containsExactlyInAnyOrder(
            District.RATNAPURA, District.KALUTARA, District.COLOMBO, District.GAMPAHA);
  }

  @Test
  @DisplayName("Every demo user of application.properties exists")
  void demoUsers() {
    for (DemoRole role : DemoRole.values()) {
      assertThat(currentUsers.currentUser(role).name()).isNotBlank();
    }
    assertThat(currentUsers.currentUser(DemoRole.DMC_OFFICER).name()).isEqualTo("N. Jayasinghe");
    assertThat(currentUsers.currentUser(DemoRole.DMC_DUTY_OFFICER).name()).isEqualTo("K. Perera");
    assertThat(currentUsers.userId(DemoRole.DISTRICT_OFFICER)).isEqualTo("DO-COL-01");
    assertThat(citizens.findById(demoUsers.citizenId())).isPresent();
  }

  @Test
  @DisplayName("Seeded numbers are below the sequence starts; next identifiers are free")
  void sequencesStayAboveTheSeed() {
    assertThat(hazards.findAll())
        .extracting(Hazard::getHazardId)
        .allSatisfy(id -> assertThat(numberOf(id)).isLessThan(300));
    assertThat(warnings.findAll())
        .extracting(Warning::getWarningId)
        .allSatisfy(id -> assertThat(numberOf(id)).isLessThan(200));
    assertThat(reports.findAll())
        .extracting(HazardReport::getReportId)
        .allSatisfy(id -> assertThat(numberOf(id)).isLessThan(1000));
    assertThat(citizens.findAll())
        .extracting(Citizen::getUserId)
        .allSatisfy(id -> assertThat(numberOf(id)).isLessThan(1000));
    assertThat(generator.next(IdKind.HAZARD, null)).isEqualTo("HZ-2026-0300");
    assertThat(generator.next(IdKind.WARNING, null)).isEqualTo("WRN-2026-0200");
    assertThat(generator.next(IdKind.DISASTER_REPORT, null)).isEqualTo("DR-2026-0184");
  }

  private static int numberOf(String id) {
    return Integer.parseInt(id.substring(id.lastIndexOf('-') + 1));
  }

  @Test
  @DisplayName("The template belongs to DMC-0001 and no client is pending by default")
  void templateAndNoPendingClient() {
    assertThat(templates.findAll()).hasSize(1);
    assertThat(templates.findAll().get(0).getOwner().getUserId()).isEqualTo("DMC-0001");
    assertThat(templates.findAll().get(0).getCriteria().districts())
        .containsExactlyInAnyOrder(
            District.COLOMBO,
            District.GAMPAHA,
            District.KALUTARA,
            District.RATNAPURA,
            District.KEGALLE);
    assertThat(clients.findAll()).isEmpty();
  }
}
