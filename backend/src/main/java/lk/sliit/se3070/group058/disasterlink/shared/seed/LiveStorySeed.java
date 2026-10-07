package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.hazardreport.repository.HazardReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the live copy of the storyline, the state the demo starts from "now": the active Kalu Ganga
 * flood HZ-2026-0201 with its Level 2 - Amber warning WRN-2026-0101, the Kelani Ganga flood
 * HZ-2026-0202 without a warning (UC1, UC2 candidates), HZ-2026-0203 without districts (UC1 A1),
 * and the pending reports RPT-2026-KLT-0901 to 0904 and RPT-2026-COL-0905 (UC2).
 */
@Component
@Profile("!test")
class LiveStorySeed {

  /** Ingiriya, Kalutara District: about 28 km from the Kalutara bridge. Group_058 assumption. */
  static final GeoPoint INGIRIYA = new GeoPoint(6.7500, 80.1600, 20.0);

  /**
   * The location of RPT-2026-KLT-0901 (the Kalutara bridge point of standards section 10; SC2 step
   * 14).
   */
  static final GeoPoint KALUTARA_BRIDGE = new GeoPoint(6.5888, 79.9640, 8.0);

  /**
   * Kaduwela, Colombo District (S4 prompt, live additions iv); accuracy is a Group_058 assumption.
   */
  static final GeoPoint KADUWELA = new GeoPoint(6.9356, 79.9843, 10.0);

  static final double KM_AS_DEGREES = 1.0 / 111.195; // Group_058 assumption: 1 km of latitude

  private static final GeoPoint RATNAPURA_GAUGE =
      new GeoPoint(6.6828, 80.3992, null); // Group_058 assumption

  private static final GeoPoint KELANI_AT_KADUWELA =
      new GeoPoint(6.9300, 79.9900, null); // Group_058 assumption

  private final HazardRepository hazards;

  private final WarningRepository warnings;

  private final HazardReportRepository reports;

  LiveStorySeed(
      HazardRepository hazards, WarningRepository warnings, HazardReportRepository reports) {
    this.hazards = hazards;
    this.warnings = warnings;
    this.reports = reports;
  }

  /**
   * Saves the live hazards, the warning and the live reports; returns the live Kalu Ganga warning.
   */
  Warning seed(SeedContext context, SeedPeople people) {
    Hazard kaluGanga = hazards.save(kaluGanga(context));
    hazards.save(kelaniGanga(context));
    hazards.save(withoutDistricts(context));
    Warning warning = warnings.save(warning(context, people, kaluGanga));
    seedReports(context, people);
    return warning;
  }

  // Standards section 11 (live copy); the severity, the start time and the location are Group_058
  // assumptions, the name and the districts follow the completed storyline (SC1 step 5).
  private static Hazard kaluGanga(SeedContext context) {
    return new Hazard(
        "HZ-2026-0201",
        HazardType.FLOOD,
        "Kalu Ganga rising; minor flood level exceeded along the lower river",
        HazardSeverity.MEDIUM,
        context.before(3, 0),
        null,
        KALUTARA_BRIDGE,
        "Kalu Ganga at Kalutara",
        List.of(District.RATNAPURA, District.KALUTARA),
        RiverBasin.KALU,
        HazardSource.AGENCY_BULLETIN,
        HazardStatus.ACTIVE);
  }

  // S4 prompt, live copy: FLOOD, MEDIUM, COLOMBO and GAMPAHA, KELANI, agency bulletin, ACTIVE, no
  // warning.
  private static Hazard kelaniGanga(SeedContext context) {
    return new Hazard(
        "HZ-2026-0202",
        HazardType.FLOOD,
        "Kelani Ganga rising at Kaduwela; minor flood level exceeded", // Group_058 assumption
        HazardSeverity.MEDIUM,
        context.before(5, 0),
        null,
        KELANI_AT_KADUWELA,
        "Kelani Ganga at Kaduwela",
        List.of(District.COLOMBO, District.GAMPAHA),
        RiverBasin.KELANI,
        HazardSource.AGENCY_BULLETIN,
        HazardStatus.ACTIVE);
  }

  // SC1 A1 5a: the Kalu Ganga hazard recorded from the bulletin without its affected districts.
  private static Hazard withoutDistricts(SeedContext context) {
    return new Hazard(
        "HZ-2026-0203",
        HazardType.FLOOD,
        "Kalu Ganga rising at the Ratnapura gauge; minor flood level exceeded",
        HazardSeverity.MEDIUM,
        context.before(1, 0),
        null,
        RATNAPURA_GAUGE,
        "Kalu Ganga at Ratnapura",
        List.of(),
        RiverBasin.KALU,
        HazardSource.AGENCY_BULLETIN,
        HazardStatus.ACTIVE);
  }

  // Escalate Warning preconditions / A1 1d / E1 11e: Level 2 - Amber for Ratnapura District and the
  // Kalu river basin, as in SC1, so that Kalutara is covered through the basin; valid now (24 hours
  // from issue, SC1 step 13), with the round-1 notifications.
  private static Warning warning(SeedContext context, SeedPeople people, Hazard hazard) {
    WarningDetails details =
        new WarningDetails(
            WarningLevel.LEVEL_2_AMBER,
            SeedTexts.LEVEL_2_MESSAGE,
            SeedTexts.LEVEL_2_INSTRUCTIONS,
            List.of(District.RATNAPURA),
            List.of(RiverBasin.KALU),
            EnumSet.of(
                NotificationChannel.PUSH,
                NotificationChannel.SMS,
                NotificationChannel.AUDIBLE_ALERT),
            context.before(2, 0).plusHours(24));
    List<Notification> notifications =
        NotificationSeed.round(
            CompletedStorySeed.recipients(people, false),
            1,
            context.before(2, 0),
            SeedTexts.LEVEL_2_MESSAGE,
            SeedTexts.LEVEL_2_INSTRUCTIONS,
            true);
    return new Warning(
        "WRN-2026-0101", hazard, details, people.officer(), context.before(2, 0), notifications);
  }

  // S4 prompt, live copy (ii)-(iv). Distances from RPT-0901: 0902 1.2 km, 0903 2.8 km, Ingiriya
  // about
  // 28 km, Kaduwela about 40 km; capture times are within 24 hours of now. Descriptions, reporters
  // and
  // capture times are Group_058 assumptions.
  private void seedReports(SeedContext context, SeedPeople people) {
    List<HazardReport> live = new ArrayList<>();
    live.add(
        pending(
            "RPT-2026-KLT-0901",
            people.citizen("CIT-KLT-0001"),
            HazardType.FLOOD,
            District.KALUTARA,
            "Kalu Ganga water is over the road near the Kalutara bridge and rising.",
            KALUTARA_BRIDGE,
            context.before(2, 30)));
    live.add(
        pending(
            "RPT-2026-KLT-0902",
            people.citizen("CIT-KLT-0002"),
            HazardType.FLOOD,
            District.KALUTARA,
            "Houses on the riverside lane are flooded up to the doorsteps.",
            north(1.2),
            context.before(5, 0)));
    live.add(
        pending(
            "RPT-2026-KLT-0903",
            people.citizen("CIT-KLT-0003"),
            HazardType.FLOOD,
            District.KALUTARA,
            "The paddy fields beside the river are under water.",
            north(2.8),
            context.before(9, 0)));
    live.add(
        pending(
            "RPT-2026-KLT-0904",
            people.citizen("CIT-KLT-0004"),
            HazardType.BLOCKED_ROAD,
            District.KALUTARA,
            "The Ingiriya road is blocked by a fallen tree.",
            INGIRIYA,
            context.before(1, 30)));
    live.add(
        pending(
            "RPT-2026-COL-0905",
            people.citizen("CIT-COL-0001"),
            HazardType.FLOOD,
            District.COLOMBO,
            "The Kelani Ganga has overflowed onto the Kaduwela road.",
            KADUWELA,
            context.before(4, 0)));
    reports.saveAll(live);
  }

  private static GeoPoint north(double km) {
    return new GeoPoint(
        KALUTARA_BRIDGE.latitude() + km * KM_AS_DEGREES, KALUTARA_BRIDGE.longitude(), 12.0);
  }

  static HazardReport pending(
      String reportId,
      Citizen reporter,
      HazardType type,
      District district,
      String description,
      GeoPoint location,
      LocalDateTime captured) {
    return new HazardReport(
        reportId,
        reporter,
        type,
        description,
        reportId + ".jpg", // Group_058 assumption
        location,
        true,
        district,
        captured,
        captured.plusMinutes(2),
        ReportStatus.PENDING_VERIFICATION,
        null,
        null,
        null,
        null);
  }
}
