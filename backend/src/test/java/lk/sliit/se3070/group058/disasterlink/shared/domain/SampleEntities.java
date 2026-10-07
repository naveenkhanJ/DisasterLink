package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Builds valid entities for the tests of {@code shared.domain}. */
public final class SampleEntities {

  public static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 8, 15);

  public static final LocalisedText TEXT = new LocalisedText("සිං", "தமிழ்", "Flood warning");

  public static final GeoPoint RATNAPURA_POINT = new GeoPoint(6.6828, 80.3992, 15.5);

  private SampleEntities() {
    throw new AssertionError("SampleEntities is a utility class");
  }

  /** Builds a valid sample: citizen. */
  public static Citizen citizen(String userId, String deviceToken) {
    return new Citizen(
        userId,
        "Nimal Perera",
        "0771234567",
        "nimal@example.lk",
        "199012345678",
        "12 Galle Road, Kalutara",
        District.KALUTARA,
        Language.SINHALA,
        deviceToken);
  }

  /** Builds a valid sample: volunteer. */
  public static CommunityVolunteer volunteer(String userId, Organization organization) {
    return new CommunityVolunteer(
        userId,
        "Kamala Silva",
        "0712223344",
        "kamala@example.lk",
        "198811112222",
        "5 Temple Road, Ratnapura",
        District.RATNAPURA,
        Language.TAMIL,
        "token-kamala",
        "GN-12",
        organization);
  }

  /** Builds a valid sample: district officer. */
  public static DistrictOfficer districtOfficer(String userId) {
    return new DistrictOfficer(
        userId,
        "Sunil Fernando",
        "0765551111",
        "sunil@example.lk",
        District.COLOMBO,
        Language.ENGLISH);
  }

  /** Builds a valid sample: officer. */
  public static DMCOfficer officer(String userId, Set<District> districts) {
    return new DMCOfficer(
        userId, "Nuwan Jayasinghe", "0112222222", "nuwan@dmc.example.lk", "Director", districts);
  }

  /** Builds a valid sample: duty officer. */
  public static DMCDutyOfficer dutyOfficer(String userId) {
    return new DMCDutyOfficer(
        userId, "Amali Perera", "0113333333", "amali@dmc.example.lk", "Duty Officer", Set.of());
  }

  /** Builds a valid sample: a community volunteer group (the type A24 requires of a membership). */
  public static Organization volunteerGroup(String organizationId) {
    return new Organization(
        organizationId,
        "Kalutara Volunteers",
        OrganizationType.COMMUNITY_VOLUNTEER_GROUP,
        "0110000014",
        "Kalutara",
        List.of(District.KALUTARA),
        Language.ENGLISH);
  }

  /** Builds a valid sample: organization. */
  public static Organization organization(String organizationId) {
    return new Organization(
        organizationId,
        "Sri Lanka Red Cross",
        OrganizationType.NGO,
        "0112691095",
        "Colombo 07",
        List.of(District.COLOMBO, District.GAMPAHA),
        Language.ENGLISH);
  }

  /** Builds a valid sample: hazard. */
  public static Hazard hazard(String hazardId) {
    return new Hazard(
        hazardId,
        HazardType.FLOOD,
        "Kalu Ganga is rising",
        HazardSeverity.MEDIUM,
        NOW,
        null,
        RATNAPURA_POINT,
        "Kalu Ganga at Ratnapura",
        List.of(District.RATNAPURA, District.KALUTARA),
        RiverBasin.KALU,
        HazardSource.AGENCY_BULLETIN,
        HazardStatus.ACTIVE);
  }

  /** Builds a valid sample: details. */
  public static WarningDetails details(
      List<District> districts, List<RiverBasin> basins, LocalDateTime expiryTime) {
    return new WarningDetails(
        WarningLevel.LEVEL_2_AMBER,
        TEXT,
        new LocalisedText("උපදෙස්", "அறிவுறுத்தல்", "Move to higher ground"),
        districts,
        basins,
        Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
        expiryTime);
  }

  /** Builds a valid sample: warning. */
  public static Warning warning(
      String warningId,
      Hazard hazard,
      DMCOfficer officer,
      List<District> districts,
      List<RiverBasin> basins,
      LocalDateTime expiryTime) {
    return new Warning(
        warningId, hazard, details(districts, basins, expiryTime), officer, NOW.plusMinutes(5));
  }
}
