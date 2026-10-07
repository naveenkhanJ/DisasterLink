package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CommunityVolunteer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCDutyOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the people: the DMC officers, the district officers, the 14 organisations and a scaled
 * number of citizens of the four districts of the storylines (standards section 11; Group_058
 * assumption: 60 citizens per district instead of the scenarios' 42,335).
 *
 * <p>The names of the officers and of CARE Sri Lanka, the Red Cross, the Army and Sarvodaya come
 * from the scenarios; every other name, number and address is a Group_058 assumption.
 */
@Component
@Profile("!test")
class PeopleSeed {

  /** The demo citizen of standards section 11 (the reporter of RPT-2026-KLT-0847 and 0901). */
  private static final String DEMO_CITIZEN = "CIT-KLT-0001";

  /** The fictional office phone series 0110000nnn (organisations 001-014, officers from 801). */
  private static final String FICTIONAL_OFFICE_PHONE = "0110000%03d";

  /** The first number of the district officers in the fictional series. */
  private static final int FIRST_OFFICER_SERIAL = 801;

  /** Citizens per block of serial numbers, so that a serial identifies district and number. */
  private static final int SERIAL_BLOCK = 100;

  /** Group_058 assumption: citizens seeded per district. */
  static final int CITIZENS_PER_DISTRICT = 60;

  /**
   * The first number of the citizen identifiers that are CommunityVolunteers (Group_058
   * assumption).
   */
  private static final int FIRST_VOLUNTEER_NUMBER = 58;

  /**
   * Share of app users, in tenths (Group_058 assumption; the scenarios have 30,100 of 42,335, about
   * 7 in 10).
   */
  private static final int APP_USERS_IN_TEN = 7;

  /**
   * Districts that get citizens, with the district code of the identifiers (IdGenerator's table).
   */
  private static final Map<District, String> DISTRICT_CODES = districtCodes();

  /** Group_058 assumption: given names. */
  private static final List<String> FIRST_NAMES =
      List.of(
          "Nimal",
          "Kamala",
          "Sunil",
          "Chamari",
          "Ruwan",
          "Dilani",
          "Pradeep",
          "Sanduni",
          "Kasun",
          "Nadeesha",
          "Lakshman",
          "Iresha",
          "Ravi",
          "Anusha",
          "Tharindu",
          "Malini",
          "Suresh",
          "Priyanka",
          "Mohamed",
          "Fathima",
          "Kumar",
          "Selvi",
          "Ajith",
          "Shamila");

  /** Group_058 assumption: family names. */
  private static final List<String> LAST_NAMES =
      List.of(
          "Perera",
          "Silva",
          "Fernando",
          "Jayawardena",
          "Bandara",
          "Wijesinghe",
          "Rajapaksa",
          "Dissanayake",
          "Herath",
          "Gamage",
          "Senanayake",
          "Kumarasinghe",
          "Pillai",
          "Nadarajah",
          "Rahman",
          "Cassim",
          "Weerasinghe",
          "Amarasinghe",
          "Ratnayake",
          "Karunaratne",
          "Samarasekara",
          "Abeysekara",
          "Mendis",
          "Liyanage");

  private final DMCOfficerRepository dmcOfficers;

  private final DMCDutyOfficerRepository dutyOfficers;

  private final DistrictOfficerRepository districtOfficers;

  private final OrganizationRepository organizations;

  private final CitizenRepository citizens;

  PeopleSeed(
      DMCOfficerRepository dmcOfficers,
      DMCDutyOfficerRepository dutyOfficers,
      DistrictOfficerRepository districtOfficers,
      OrganizationRepository organizations,
      CitizenRepository citizens) {
    this.dmcOfficers = dmcOfficers;
    this.dutyOfficers = dutyOfficers;
    this.districtOfficers = districtOfficers;
    this.organizations = organizations;
    this.citizens = citizens;
  }

  /** Saves the people and returns them. */
  SeedPeople seed() {
    // SC4 preconditions and E4: the reporting districts of the demo DMC officer.
    DMCOfficer officer =
        dmcOfficers.save(
            new DMCOfficer(
                "DMC-0001",
                "N. Jayasinghe",
                "0110000901", // Group_058 assumption (fictional series)
                "n.jayasinghe@dmc.example.lk", // Group_058 assumption
                "Director, Emergency Operations", // Group_058 assumption
                EnumSet.of(
                    District.COLOMBO,
                    District.GAMPAHA,
                    District.KALUTARA,
                    District.RATNAPURA,
                    District.KEGALLE)));
    // SC2 step 25 and Escalate Warning: DMC Duty Officer K. Perera.
    DMCDutyOfficer dutyOfficer =
        dutyOfficers.save(
            new DMCDutyOfficer(
                "DMC-0002",
                "K. Perera",
                "0110000902", // Group_058 assumption (fictional series)
                "k.perera@dmc.example.lk", // Group_058 assumption
                "Duty Officer", // Group_058 assumption
                Set.of()));
    List<DistrictOfficer> districtOfficerList = districtOfficers.saveAll(districtOfficerList());
    List<Organization> organizationList = organizations.saveAll(organizationList());
    Map<District, List<Citizen>> citizenMap =
        citizens(organizationList.get(organizationList.size() - 1));
    return new SeedPeople(officer, dutyOfficer, districtOfficerList, organizationList, citizenMap);
  }

  // Group_058 assumption: the names, languages and numbers of the district officers. The
  // identifiers DO-COL-01, DO-GAM-01, DO-KLT-01 and DO-RAT-01 are fixed by the seed plan;
  // DO-KLT-02 and DO-RAT-02 are the second officer of each district that the Escalate Warning
  // scenario counts (2 per district).
  private List<DistrictOfficer> districtOfficerList() {
    return List.of(
        officerOf(1, "DO-COL-01", "R. Wickramasinghe", District.COLOMBO, Language.SINHALA),
        officerOf(2, "DO-GAM-01", "M. Dissanayake", District.GAMPAHA, Language.SINHALA),
        officerOf(3, "DO-KLT-01", "S. Ratnayake", District.KALUTARA, Language.SINHALA),
        officerOf(4, "DO-KLT-02", "A. Mendis", District.KALUTARA, Language.ENGLISH),
        officerOf(5, "DO-RAT-01", "T. Herath", District.RATNAPURA, Language.SINHALA),
        officerOf(6, "DO-RAT-02", "P. Senanayake", District.RATNAPURA, Language.ENGLISH));
  }

  // Phone numbers are a fictional 0110000nnn series (district officers 801 to 806), never a
  // real-looking spread (P155 E-12).
  private static DistrictOfficer officerOf(
      int serial, String userId, String name, District district, Language language) {
    return new DistrictOfficer(
        userId,
        name,
        String.format(FICTIONAL_OFFICE_PHONE, FIRST_OFFICER_SERIAL + serial - 1),
        userId.toLowerCase(Locale.ROOT) + "@ddmcu.example.lk", // Group_058 assumption
        district,
        language);
  }

  // The 14 response organisations: CARE Sri Lanka (the organisation demo user, SC4 step 15), the
  // Sri Lanka Red Cross Society (SC4 A5), the Sri Lanka Army (SC4 step 7) and Sarvodaya (SC4
  // E5/E6) are named by the scenarios; the other ten are Group_058 assumptions. Every one serves
  // the districts of the storylines, so all 14 are recipients of WRN-2026-0087.
  private List<Organization> organizationList() {
    List<Organization> list = new ArrayList<>();
    list.add(organization(1, "CARE Sri Lanka", OrganizationType.NGO));
    list.add(organization(2, "Sri Lanka Red Cross Society", OrganizationType.NGO));
    list.add(organization(3, "Sri Lanka Army", OrganizationType.ARMED_FORCES));
    list.add(organization(4, "Sarvodaya", OrganizationType.NGO));
    list.add(organization(5, "Sri Lanka Navy", OrganizationType.ARMED_FORCES));
    list.add(organization(6, "Sri Lanka Air Force", OrganizationType.ARMED_FORCES));
    list.add(organization(7, "Sri Lanka Police", OrganizationType.GOVERNMENT_BODY));
    list.add(organization(8, "Ministry of Health", OrganizationType.GOVERNMENT_BODY));
    list.add(organization(9, "Irrigation Department", OrganizationType.GOVERNMENT_BODY));
    list.add(organization(10, "World Vision Lanka", OrganizationType.NGO));
    list.add(organization(11, "Save the Children Sri Lanka", OrganizationType.NGO));
    list.add(organization(12, "Hayleys Foundation", OrganizationType.PRIVATE_DONOR));
    list.add(organization(13, "Dialog Axiata CSR", OrganizationType.PRIVATE_DONOR));
    list.add(
        organization(
            14,
            "Kalutara Disaster Management Volunteers",
            OrganizationType.COMMUNITY_VOLUNTEER_GROUP));
    return list;
  }

  private static Organization organization(int number, String name, OrganizationType type) {
    return new Organization(
        String.format("ORG-%04d", number),
        name,
        type,
        String.format(FICTIONAL_OFFICE_PHONE, number), // Group_058 assumption (fictional)
        "Colombo", // Group_058 assumption
        List.of(
            District.RATNAPURA,
            District.KALUTARA,
            District.COLOMBO,
            District.GAMPAHA,
            District.KEGALLE),
        Language.values()[number % Language.values().length]);
  }

  private Map<District, List<Citizen>> citizens(Organization volunteerGroup) {
    Map<District, List<Citizen>> byDistrict = new EnumMap<>(District.class);
    DISTRICT_CODES.forEach(
        (district, code) -> {
          List<Citizen> list = new ArrayList<>();
          for (int number = 1; number <= CITIZENS_PER_DISTRICT; number++) {
            list.add(citizen(district, code, number, volunteerGroup));
          }
          byDistrict.put(district, citizens.saveAll(list));
        });
    return byDistrict;
  }

  private static Citizen citizen(District district, String code, int number, Organization group) {
    String userId = String.format("CIT-%s-%04d", code, number);
    // SC2 and the seed plan: CIT-KLT-0001 is Asela Gunawardene, a Kalutara citizen with the app.
    String name =
        DEMO_CITIZEN.equals(userId)
            ? "Asela Gunawardene"
            : FIRST_NAMES.get((number * 7 + district.ordinal()) % FIRST_NAMES.size())
                + " "
                + LAST_NAMES.get((number * 11 + district.ordinal()) % LAST_NAMES.size());
    // Group_058 assumption: 7 in 10 citizens use the app (a device token), the rest are SMS-only.
    String deviceToken = number % 10 < APP_USERS_IN_TEN ? "demo-device-" + userId : null;
    // Group_058 assumption: languages in turn; the demo citizen starts in English (standards
    // section 11; SC2 steps 2-4 change it to Sinhala in the demo).
    Language language =
        DEMO_CITIZEN.equals(userId)
            ? Language.ENGLISH
            : Language.values()[(number + 2) % Language.values().length];
    int serial = serial(district, number);
    // Clearly fictional contact data: phones 0700000nnn, NICs SEED + number (P155 E-12).
    String phone = String.format("0700000%03d", serial);
    String email = "citizen." + userId.toLowerCase(Locale.ROOT) + "@example.lk";
    String nic = String.format("SEED%04d", serial);
    String address = "No. " + number + ", Main Street, " + titleCase(district);
    if (district == District.KALUTARA
        && number >= FIRST_VOLUNTEER_NUMBER
        && number < FIRST_VOLUNTEER_NUMBER + 2) {
      // A24: community volunteers who are members of the volunteer group (Group_058 assumption).
      return new CommunityVolunteer(
          userId,
          name,
          phone,
          email,
          nic,
          address,
          district,
          language,
          deviceToken,
          "GN-" + number,
          group);
    }
    return new Citizen(userId, name, phone, email, nic, address, district, language, deviceToken);
  }

  // A number below 1000 that is unique over the four seeded districts: 100 per district.
  private static int serial(District district, int number) {
    return new ArrayList<>(DISTRICT_CODES.keySet()).indexOf(district) * SERIAL_BLOCK + number;
  }

  private static String titleCase(District district) {
    String name = district.name();
    return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
  }

  private static Map<District, String> districtCodes() {
    Map<District, String> codes = new EnumMap<>(District.class);
    codes.put(District.RATNAPURA, "RAT");
    codes.put(District.KALUTARA, "KLT");
    codes.put(District.COLOMBO, "COL");
    codes.put(District.GAMPAHA, "GAM");
    return codes;
  }
}
