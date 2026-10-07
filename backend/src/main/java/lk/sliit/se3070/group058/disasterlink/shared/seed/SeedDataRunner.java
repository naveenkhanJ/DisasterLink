package lk.sliit.se3070.group058.disasterlink.shared.seed;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReliefSupply;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the empty database with the demo data of implementation standards section 11 when the
 * application starts (not under the {@code test} profile). The builders are split by group: {@link
 * PeopleSeed} (people and organisations), {@link CompletedStorySeed} (the closed Kalu Ganga flood),
 * {@link LiveStorySeed} (the live copy and its reports), {@link ShelterSeed} and {@link
 * ReportingSeed}.
 *
 * <p>Every time is relative to the injected {@link Clock}. The seed builds stored state through the
 * entities' seed constructors; it never calls an owner's operation. The numbers are scaled (60
 * citizens per district, eight event shelters) instead of the scenarios' absolute figures.
 */
@Component
@Profile("!test")
public class SeedDataRunner implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SeedDataRunner.class);

  /** The entities whose rows the closing log line counts. */
  private static final List<Class<?>> COUNTED =
      List.of(
          Citizen.class,
          DistrictOfficer.class,
          Organization.class,
          Hazard.class,
          Warning.class,
          Notification.class,
          HazardReport.class,
          Shelter.class,
          OccupancyRecord.class,
          ReliefSupply.class,
          ResourceDistribution.class,
          AuditRecord.class,
          CriteriaTemplate.class,
          ClientSyncStatus.class);

  @PersistenceContext private EntityManager entityManager;

  private final Clock clock;

  private final boolean pendingClient;

  private final PeopleSeed people;

  private final CompletedStorySeed completed;

  private final LiveStorySeed live;

  private final ShelterSeed shelters;

  private final ReportingSeed reporting;

  SeedDataRunner(
      Clock clock,
      @Value("${demo.pending-client:false}") boolean pendingClient,
      PeopleSeed people,
      CompletedStorySeed completed,
      LiveStorySeed live,
      ShelterSeed shelters,
      ReportingSeed reporting) {
    this.clock = clock;
    this.pendingClient = pendingClient;
    this.people = people;
    this.completed = completed;
    this.live = live;
    this.shelters = shelters;
    this.reporting = reporting;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    SeedContext context = new SeedContext(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
    SeedPeople seeded = people.seed();
    CompletedStorySeed.CompletedStory story = completed.seed(context, seeded);
    live.seed(context, seeded);
    shelters.seedColombo(context, seeded);
    shelters.seedEvent(context, seeded, story.warning());
    reporting.seedTemplate(context, seeded);
    if (pendingClient) {
      reporting.seedPendingClient(context);
    }
    entityManager.flush();
    logCounts();
  }

  private void logCounts() {
    String counts = COUNTED.stream().map(this::count).collect(Collectors.joining(", "));
    LOG.info("Seed data saved: {}", counts);
  }

  private String count(Class<?> entity) {
    Long rows =
        entityManager
            .createQuery("select count(e) from " + entity.getSimpleName() + " e", Long.class)
            .getSingleResult();
    return entity.getSimpleName() + "=" + rows;
  }
}
