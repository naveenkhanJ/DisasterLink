package lk.sliit.se3070.group058.disasterlink.shared.seed;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReliefSupply;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyCategory;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ResourceDistributionRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the completed copy of the storyline (standards section 11 (i)): the closed Kalu Ganga flood
 * HZ-2026-0142, its warning WRN-2026-0087 at its final state (escalated to Level 3 - Red) with the
 * notifications of both broadcast rounds and the escalation audit record, the reports of the SC2
 * and Escalate Warning scenarios, and the relief supplies and distributions that SC4 analyses. The
 * numbers are scaled (60 citizens per district instead of 42,335).
 */
@Component
@Profile("!test")
class CompletedStorySeed {

  /** What the later groups need from the completed copy. */
  record CompletedStory(Hazard hazard, Warning warning) {}

  private static final GeoPoint RATNAPURA_GAUGE =
      new GeoPoint(6.6828, 80.3992, null); // Group_058 assumption

  @PersistenceContext private EntityManager entityManager;

  private final HazardRepository hazards;

  private final WarningRepository warnings;

  private final AuditRecordRepository auditRecords;

  private final HazardReportRepository reports;

  private final ResourceDistributionRepository distributions;

  CompletedStorySeed(
      HazardRepository hazards,
      WarningRepository warnings,
      AuditRecordRepository auditRecords,
      HazardReportRepository reports,
      ResourceDistributionRepository distributions) {
    this.hazards = hazards;
    this.warnings = warnings;
    this.auditRecords = auditRecords;
    this.reports = reports;
    this.distributions = distributions;
  }

  /** Saves the completed copy and returns its hazard and warning. */
  CompletedStory seed(SeedContext context, SeedPeople people) {
    Hazard hazard = hazards.save(hazard(context));
    Warning warning = warnings.save(warning(context, people, hazard));
    // Escalate Warning step 10: the audit record of the escalation, performed by DMC-0002 (K.
    // Perera,
    // the duty officer who escalates; S5 STEP 0, E1). The warning's issuing officer stays DMC-0001.
    auditRecords.save(
        new AuditRecord(
            AuditAction.WARNING_ESCALATED,
            people.dutyOfficer(),
            warning.getWarningId(),
            "LEVEL_2_AMBER→LEVEL_3_RED",
            context.event(1, 8, 25)));
    seedReports(context, people, hazard);
    seedDistributions(context, people, hazard);
    return new CompletedStory(hazard, warning);
  }

  // SC1 step 5, Escalate Warning preconditions, SC4 preconditions: closed on 25 May (day 5; the
  // time of
  // day is a Group_058 assumption).
  private Hazard hazard(SeedContext context) {
    return new Hazard(
        "HZ-2026-0142",
        HazardType.FLOOD,
        "Kalu Ganga rising at the Ratnapura gauge; minor flood level exceeded",
        HazardSeverity.MEDIUM,
        context.event(0, 9, 30),
        context.event(5, 12, 0),
        RATNAPURA_GAUGE,
        "Kalu Ganga at Ratnapura",
        List.of(District.RATNAPURA, District.KALUTARA),
        RiverBasin.KALU,
        HazardSource.AGENCY_BULLETIN,
        HazardStatus.CLOSED);
  }

  // SC1 steps 13-16 (issued 20 May 10:45, Level 2 - Amber, 24 hours); Escalate Warning steps 10-12
  // (21 May 08:25, Level 3 - Red, expires 22 May 08:25). Stored at its final state: Level 3 text
  // and
  // expiry, with the notifications of round 1 (Level 2 texts) and round 2 (Level 3 texts, plus the
  // district officers).
  private Warning warning(SeedContext context, SeedPeople people, Hazard hazard) {
    WarningDetails details =
        new WarningDetails(
            WarningLevel.LEVEL_3_RED,
            SeedTexts.LEVEL_3_MESSAGE,
            SeedTexts.LEVEL_3_INSTRUCTIONS,
            List.of(District.RATNAPURA),
            List.of(RiverBasin.KALU),
            EnumSet.of(
                NotificationChannel.PUSH,
                NotificationChannel.SMS,
                NotificationChannel.AUDIBLE_ALERT),
            context.event(2, 8, 25));
    List<Recipient> issueRecipients = recipients(people, false);
    List<Recipient> escalationRecipients = recipients(people, true);
    List<Notification> notifications = new ArrayList<>();
    notifications.addAll(
        NotificationSeed.round(
            issueRecipients,
            1,
            context.event(0, 10, 45),
            SeedTexts.LEVEL_2_MESSAGE,
            SeedTexts.LEVEL_2_INSTRUCTIONS,
            true));
    notifications.addAll(
        NotificationSeed.round(
            escalationRecipients,
            2,
            context.event(1, 8, 25),
            SeedTexts.LEVEL_3_MESSAGE,
            SeedTexts.LEVEL_3_INSTRUCTIONS,
            false));
    return new Warning(
        "WRN-2026-0087",
        hazard,
        details,
        people.officer(),
        context.event(0, 10, 45),
        notifications);
  }

  /**
   * The citizens and organisations of Ratnapura and Kalutara, and in an escalation their district
   * officers.
   */
  static List<Recipient> recipients(SeedPeople people, boolean withDistrictOfficers) {
    List<Recipient> recipients = new ArrayList<>();
    recipients.addAll(people.citizens().get(District.RATNAPURA));
    recipients.addAll(people.citizens().get(District.KALUTARA));
    recipients.addAll(people.organizations());
    if (withDistrictOfficers) {
      recipients.addAll(people.districtOfficersOf(District.RATNAPURA));
      recipients.addAll(people.districtOfficersOf(District.KALUTARA));
    }
    return recipients;
  }

  // SC2 steps 19 and 24-29 and Escalate Warning step 1: RPT-2026-KLT-0847 verified by K. Perera
  // (DMC-0002) and linked to HZ-2026-0142. RPT-2026-KLT-0843 (07:05, 1.2 km) and RPT-2026-KLT-0845
  // (07:20, 2.8 km) are the nearby reports of SC2 step 14; they are stored in their final state:
  // verified by DMC-0002 a few minutes after 0847 and linked to the same hazard (S5 STEP 0, D1:
  // duplicates of the same flood, SC2 A4). The blocked-road report of SC2 A6 25m is the live
  // RPT-2026-KLT-0904, so none is stored here. Positions and descriptions of 0843 and 0845 are
  // Group_058 assumptions; the distances are SC2's.
  private void seedReports(SeedContext context, SeedPeople people, Hazard hazard) {
    Citizen asela = people.citizen("CIT-KLT-0001");
    reports.save(
        new HazardReport(
            "RPT-2026-KLT-0847",
            asela,
            HazardType.FLOOD,
            "Kalu Ganga water has breached the sandbag barriers near the Kalutara bridge. "
                + "Flooding is now moving into the service road. Heavy continuous rainfall still "
                + "active.",
            "RPT-2026-KLT-0847.jpg", // Group_058 assumption
            LiveStorySeed.KALUTARA_BRIDGE,
            true,
            District.KALUTARA,
            context.event(1, 7, 40),
            context.event(1, 7, 42),
            ReportStatus.VERIFIED,
            context.event(1, 8, 15),
            null,
            people.dutyOfficer(),
            hazard));
    reports.save(
        verified(
            "RPT-2026-KLT-0843",
            people.citizen("CIT-KLT-0002"),
            "Water from the Kalu Ganga is rising over the riverside road at Kalutara North.",
            1.2,
            context.event(1, 7, 5),
            context.event(1, 8, 18), // Group_058 assumption: a few minutes after 0847
            people,
            hazard));
    reports.save(
        verified(
            "RPT-2026-KLT-0845",
            people.citizen("CIT-KLT-0003"),
            "The paddy fields beside the river are under water and the water is still rising.",
            2.8,
            context.event(1, 7, 20),
            context.event(1, 8, 21), // Group_058 assumption: a few minutes after 0847
            people,
            hazard));
  }

  private static HazardReport verified(
      String reportId,
      Citizen reporter,
      String description,
      double kmFromBridge,
      LocalDateTime captured,
      LocalDateTime verifiedAt,
      SeedPeople people,
      Hazard hazard) {
    GeoPoint location =
        new GeoPoint(
            LiveStorySeed.KALUTARA_BRIDGE.latitude() + kmFromBridge * LiveStorySeed.KM_AS_DEGREES,
            LiveStorySeed.KALUTARA_BRIDGE.longitude(),
            12.0);
    return new HazardReport(
        reportId,
        reporter,
        HazardType.FLOOD,
        description,
        reportId + ".jpg", // Group_058 assumption
        location,
        true,
        District.KALUTARA,
        captured,
        captured.plusMinutes(2),
        ReportStatus.VERIFIED,
        verifiedAt,
        null,
        people.dutyOfficer(),
        hazard);
  }

  // SC4 step 7, resource distribution: the five lines of the report (Ratnapura: CARE Sri Lanka
  // 1,200
  // dry-ration packs, Sri Lanka Army 18,000 litres of water, a private individual 480 dry-ration
  // packs; Kalutara: Sri Lanka Red Cross Society 350 tarpaulins, CARE Sri Lanka 220 first-aid kits)
  // and a sixth, 6,000 litres of water delivered to Kalutara on 22 May and recorded without a
  // hazard
  // link, which the hazard-type filter leaves out. The quantities of the lines are SC4's; the stock
  // quantities, locations, delivery times and identifiers are Group_058 assumptions. CARE Sri Lanka
  // owns two lines, Sarvodaya none (SC4 step 15, E5, E6).
  private void seedDistributions(SeedContext context, SeedPeople people, Hazard hazard) {
    ReliefSupply rations =
        supply(
            "SUP-0001",
            "Dry-ration packs",
            SupplyCategory.FOOD,
            3000,
            "dry-ration packs",
            people.organization("ORG-0001"),
            null);
    ReliefSupply water =
        supply(
            "SUP-0002",
            "Drinking water",
            SupplyCategory.WATER,
            40000,
            "litres",
            people.organization("ORG-0003"),
            null);
    ReliefSupply privateRations =
        supply(
            "SUP-0003",
            "Dry-ration packs (private donation)",
            SupplyCategory.FOOD,
            600,
            "dry-ration packs",
            null,
            people.citizen("CIT-RAT-0005"));
    ReliefSupply tarpaulins =
        supply(
            "SUP-0004",
            "Tarpaulins",
            SupplyCategory.SHELTER_MATERIAL,
            800,
            "tarpaulins",
            people.organization("ORG-0002"),
            null);
    ReliefSupply kits =
        supply(
            "SUP-0005",
            "First-aid kits",
            SupplyCategory.MEDICINE,
            500,
            "first-aid kits",
            people.organization("ORG-0001"),
            null);
    ReliefSupply navyWater =
        supply(
            "SUP-0006",
            "Drinking water (Navy bowser)",
            SupplyCategory.WATER,
            9000,
            "litres",
            people.organization("ORG-0005"),
            null);
    DistrictOfficer ratnapura = people.districtOfficer("DO-RAT-01");
    DistrictOfficer kalutara = people.districtOfficer("DO-KLT-01");
    distributions.saveAll(
        List.of(
            distribution(
                "DIST-0001",
                1200,
                context.event(2, 10, 0),
                District.RATNAPURA,
                rations,
                ratnapura,
                hazard),
            distribution(
                "DIST-0002",
                18000,
                context.event(2, 11, 30),
                District.RATNAPURA,
                water,
                ratnapura,
                hazard),
            distribution(
                "DIST-0003",
                480,
                context.event(3, 9, 0),
                District.RATNAPURA,
                privateRations,
                ratnapura,
                hazard),
            distribution(
                "DIST-0004",
                350,
                context.event(2, 14, 0),
                District.KALUTARA,
                tarpaulins,
                kalutara,
                hazard),
            distribution(
                "DIST-0005",
                220,
                context.event(3, 10, 30),
                District.KALUTARA,
                kits,
                kalutara,
                hazard),
            distribution(
                "DIST-0006",
                6000,
                context.event(2, 15, 0),
                District.KALUTARA,
                navyWater,
                kalutara,
                null)));
  }

  private ReliefSupply supply(
      String id,
      String name,
      SupplyCategory category,
      double quantity,
      String unit,
      Organization organization,
      Citizen citizen) {
    ReliefSupply supply =
        new ReliefSupply(
            id,
            name,
            category,
            quantity,
            unit,
            "Relief store, Colombo",
            SupplyStatus.AVAILABLE,
            organization,
            citizen);
    entityManager.persist(supply);
    return supply;
  }

  private static ResourceDistribution distribution(
      String id,
      double quantity,
      LocalDateTime date,
      District district,
      ReliefSupply supply,
      DistrictOfficer coordinator,
      Hazard hazard) {
    return new ResourceDistribution(
        id,
        quantity,
        date,
        district,
        "Relief camps of "
            + district.name().charAt(0)
            + district.name().substring(1).toLowerCase(Locale.ROOT),
        supply,
        null,
        coordinator,
        hazard);
  }
}
