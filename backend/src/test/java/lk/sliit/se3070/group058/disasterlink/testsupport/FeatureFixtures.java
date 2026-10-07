package lk.sliit.se3070.group058.disasterlink.testsupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSource;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import org.springframework.test.util.ReflectionTestUtils;

/** Builds entities, criteria and points for the tests of the feature packages and repositories. */
public final class FeatureFixtures {

  /** A point in Colombo; the other points are measured from it. */
  public static final GeoPoint ORIGIN = new GeoPoint(6.9271, 79.8612, null);

  /** A point about 3.0 km north of {@link #ORIGIN}. */
  public static final GeoPoint NORTH_3_KM = new GeoPoint(6.9541, 79.8612, null);

  /** A point about 7.0 km north of {@link #ORIGIN}. */
  public static final GeoPoint NORTH_7_KM = new GeoPoint(6.9901, 79.8612, null);

  /** A point inside the 5 km bounding box of {@link #ORIGIN} but about 6.2 km away. */
  public static final GeoPoint DIAGONAL = new GeoPoint(6.9671, 79.9012, null);

  /** A point far from every other: Jaffna. */
  public static final GeoPoint JAFFNA_POINT = new GeoPoint(9.6615, 80.0255, null);

  public static final LocalDateTime DAY_START = LocalDateTime.of(2026, 5, 20, 0, 0);

  private FeatureFixtures() {
    throw new AssertionError("FeatureFixtures is a utility class");
  }

  /** Builds a valid sample: citizen in. */
  public static Citizen citizenIn(String userId, District district) {
    return new Citizen(
        userId,
        "Citizen " + userId,
        "0771111111",
        userId + "@example.lk",
        "NIC-" + userId,
        "Address",
        district,
        Language.ENGLISH,
        null);
  }

  /** Builds a valid sample: hazard. */
  public static Hazard hazard(
      String hazardId,
      HazardType type,
      HazardStatus status,
      LocalDateTime start,
      LocalDateTime end,
      GeoPoint location,
      District... districts) {
    return new Hazard(
        hazardId,
        type,
        "Description of " + hazardId,
        HazardSeverity.MEDIUM,
        start,
        end,
        location,
        "Place of " + hazardId,
        List.of(districts),
        null,
        HazardSource.AGENCY_BULLETIN,
        status);
  }

  /** Builds a valid sample: warning. */
  public static Warning warning(
      String warningId,
      Hazard hazard,
      DMCOfficer officer,
      LocalDateTime issuedTime,
      LocalDateTime expiryTime,
      List<District> districts,
      List<RiverBasin> basins) {
    return new Warning(
        warningId,
        hazard,
        SampleEntities.details(districts, basins, expiryTime),
        officer,
        issuedTime);
  }

  /**
   * Adds notifications to a warning by reflection, because {@code Warning.addNotification} is a
   * placeholder owned by Lakmal and the entity has no other way to take them.
   */
  @SuppressWarnings("unchecked")
  public static void addNotifications(Warning warning, Notification... notifications) {
    List<Notification> list =
        (List<Notification>) ReflectionTestUtils.getField(warning, "notifications");
    list.addAll(List.of(notifications));
  }

  /** Builds a valid sample: report. */
  public static HazardReport report(
      String reportId,
      Citizen reporter,
      GeoPoint location,
      LocalDateTime capturedAt,
      LocalDateTime reportTime,
      ReportStatus status) {
    return new HazardReport(
        reportId,
        reporter,
        HazardType.FLOOD,
        "Water is rising",
        "photo-" + reportId + ".jpg",
        location,
        true,
        District.COLOMBO,
        capturedAt,
        reportTime,
        status,
        null,
        null,
        null,
        null);
  }

  /** Builds a valid sample: criteria. */
  public static ReportCriteria criteria(
      Set<District> districts, HazardType type, LocalDate start, LocalDate end) {
    return new ReportCriteria(
        districts, type, start, end, Set.of(ReportMetric.ALERT_TIMELINE), null);
  }

  /** Builds a valid sample: results. */
  public static ReportResults results() {
    return new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
  }
}
