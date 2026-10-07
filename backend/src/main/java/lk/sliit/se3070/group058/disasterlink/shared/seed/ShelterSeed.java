package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.repository.ShelterRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the shelters: the three Colombo shelters of SC3 (UC3, no warning link) and the shelters of
 * Ratnapura and Kalutara of the completed storyline, with occupancy records over the event period,
 * linked to WRN-2026-0087 (SC4 A9) except Kalutara Town Hall, which SC4 step 7 names as the shelter
 * allocated to no warning. Eight event shelters stand in for the scenario's larger number (scaled
 * seed, standards section 11).
 */
@Component
@Profile("!test")
class ShelterSeed {

  /** The event shelter that is allocated to no warning (SC4 step 7; P155 G7). */
  static final String KALUTARA_TOWN_HALL = "SHL-KLT-0001";

  /** What a shelter row of the plan holds; the occupancy percentages are in {@link #PROFILE}. */
  private record Plan(
      String id,
      String name,
      String address,
      District district,
      int capacity,
      String contactNumber,
      String operatorId) {}

  /**
   * Occupancy of an event shelter as a percentage of its capacity at (day, hour) of the event: the
   * peak is 94 % on 21 May after the escalation to Level 3 (SC4 step 10), then it falls as the
   * water recedes. The other percentages are Group_058 assumptions.
   */
  private static final int[][] PROFILE = {
    {0, 12, 40}, {1, 8, 70}, {1, 20, 94}, {2, 12, 80}, {3, 12, 55}, {4, 12, 20}
  };

  /**
   * The offline update of SC4 step 7 as (day, captured hour, captured minute, received hour,
   * received minute, percent of capacity): captured 14:05 on 21 May, received 14:40. The percentage
   * is a Group_058 assumption.
   */
  private static final int[] OFFLINE_UPDATE = {1, 14, 5, 14, 40, 82};

  private static final int MINUTES_TO_RECORD = 1;

  private final ShelterRepository shelters;

  ShelterSeed(ShelterRepository shelters) {
    this.shelters = shelters;
  }

  /** Saves the Colombo shelters of UC3 and returns how many were saved. */
  int seedColombo(SeedContext context, SeedPeople people) {
    DistrictOfficer officer = people.districtOfficer("DO-COL-01");
    // SC3 preconditions and steps 4 and 8: Rathmalana Maha Vidyalaya 500 / 320, contact
    // 071 234 5678 (the scenario's number, kept as written); A1 4b: Dehiwala Municipal Council
    // Hall 400 / 120 and Piliyandala Central College 600 / 410. The addresses of Dehiwala and
    // Piliyandala, their fictional contact numbers, the operators (none) and the time of the
    // stored occupancy counts are Group_058 assumptions.
    List<Shelter> saved = new ArrayList<>();
    saved.add(
        colombo(
            "SHL-COL-0001",
            "Rathmalana Maha Vidyalaya",
            "Rathmalana, Colombo",
            500,
            320,
            "0712345678",
            officer,
            context));
    saved.add(
        colombo(
            "SHL-COL-0002",
            "Dehiwala Municipal Council Hall",
            "Galle Road, Dehiwala",
            400,
            120,
            "0110000102",
            officer,
            context));
    saved.add(
        colombo(
            "SHL-COL-0003",
            "Piliyandala Central College",
            "Moratuwa Road, Piliyandala",
            600,
            410,
            "0110000103",
            officer,
            context));
    shelters.saveAll(saved);
    return saved.size();
  }

  private Shelter colombo(
      String id,
      String name,
      String address,
      int capacity,
      int occupancy,
      String contact,
      DistrictOfficer officer,
      SeedContext context) {
    LocalDateTime counted = context.before(3, 0);
    OccupancyRecord record =
        new OccupancyRecord(occupancy, counted, counted.plusMinutes(MINUTES_TO_RECORD), officer);
    return new Shelter(
        id,
        name,
        address,
        District.COLOMBO,
        capacity,
        occupancy,
        contact,
        ShelterStatus.OPEN,
        officer,
        null,
        List.of(record),
        List.of());
  }

  /**
   * Saves the event shelters of Ratnapura and Kalutara, linked to the warning (Kalutara Town Hall
   * is not); returns how many.
   */
  int seedEvent(SeedContext context, SeedPeople people, Warning warning) {
    List<Shelter> saved = new ArrayList<>();
    for (Plan plan : eventPlans()) {
      saved.add(event(plan, context, people, warning));
    }
    shelters.saveAll(saved);
    return saved.size();
  }

  // SC4 step 10 gives only the 94 % peak of 21 May; the shelters' names, addresses, capacities,
  // fictional contact numbers and operators are Group_058 assumptions (capacities are multiples of
  // 50 so that 94 % is a whole number).
  private static List<Plan> eventPlans() {
    List<Plan> plans = new ArrayList<>(ratnapuraPlans());
    plans.addAll(kalutaraPlans());
    return plans;
  }

  private static List<Plan> ratnapuraPlans() {
    District rat = District.RATNAPURA;
    return List.of(
        new Plan(
            "SHL-RAT-0001",
            "Ratnapura Town Hall",
            "Main Street, Ratnapura",
            rat,
            500,
            "0110000201",
            "ORG-0002"),
        new Plan(
            "SHL-RAT-0002",
            "Sivali Central College",
            "Colombo Road, Ratnapura",
            rat,
            400,
            "0110000202",
            null),
        new Plan(
            "SHL-RAT-0003",
            "Kuruwita Maha Vidyalaya",
            "Kuruwita, Ratnapura",
            rat,
            300,
            "0110000203",
            null),
        new Plan(
            "SHL-RAT-0004",
            "Eheliyagoda Community Hall",
            "Eheliyagoda, Ratnapura",
            rat,
            200,
            "0110000204",
            "ORG-0001"));
  }

  private static List<Plan> kalutaraPlans() {
    District klt = District.KALUTARA;
    return List.of(
        new Plan(
            KALUTARA_TOWN_HALL,
            "Kalutara Town Hall",
            "Galle Road, Kalutara",
            klt,
            500,
            "0110000205",
            "ORG-0002"),
        new Plan(
            "SHL-KLT-0002",
            "Panadura Municipal Hall",
            "Galle Road, Panadura",
            klt,
            400,
            "0110000206",
            null),
        new Plan(
            "SHL-KLT-0003",
            "Horana Central College",
            "Horana, Kalutara",
            klt,
            300,
            "0110000207",
            null),
        new Plan(
            "SHL-KLT-0004",
            "Ingiriya Maha Vidyalaya",
            "Ingiriya, Kalutara",
            klt,
            250,
            "0110000208",
            "ORG-0014"));
  }

  private Shelter event(Plan plan, SeedContext context, SeedPeople people, Warning warning) {
    DistrictOfficer officer =
        plan.district() == District.RATNAPURA
            ? people.districtOfficer("DO-RAT-01")
            : people.districtOfficer("DO-KLT-01");
    Organization operator =
        plan.operatorId() == null ? null : people.organization(plan.operatorId());
    List<OccupancyRecord> records = eventRecords(plan, context, officer);
    int current = records.get(records.size() - 1).getOccupantCount();
    // SC4 step 7: Kalutara Town Hall is allocated to no warning.
    List<Warning> allocated = KALUTARA_TOWN_HALL.equals(plan.id()) ? List.of() : List.of(warning);
    return new Shelter(
        plan.id(),
        plan.name(),
        plan.address(),
        plan.district(),
        plan.capacity(),
        current,
        plan.contactNumber(),
        ShelterStatus.OPEN,
        officer,
        operator,
        records,
        allocated);
  }

  private List<OccupancyRecord> eventRecords(
      Plan plan, SeedContext context, DistrictOfficer officer) {
    List<OccupancyRecord> records = new ArrayList<>();
    for (int[] point : PROFILE) {
      LocalDateTime counted = context.event(point[0], point[1], 0);
      records.add(
          new OccupancyRecord(
              plan.capacity() * point[2] / 100,
              counted,
              counted.plusMinutes(MINUTES_TO_RECORD),
              officer));
    }
    if (KALUTARA_TOWN_HALL.equals(plan.id())) {
      // SC4 step 7: the update captured offline at 14:05 on 21 May and received at 14:40.
      records.add(
          new OccupancyRecord(
              plan.capacity() * OFFLINE_UPDATE[5] / 100,
              context.event(OFFLINE_UPDATE[0], OFFLINE_UPDATE[1], OFFLINE_UPDATE[2]),
              context.event(OFFLINE_UPDATE[0], OFFLINE_UPDATE[3], OFFLINE_UPDATE[4]),
              officer));
      records.sort(Comparator.comparing(OccupancyRecord::getCapturedAt));
    }
    return records;
  }
}
