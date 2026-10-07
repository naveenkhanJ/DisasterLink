package lk.sliit.se3070.group058.disasterlink.shelter.repository;

import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Tests of {@link ShelterRepository}. */
@DataJpaTest
class ShelterRepositoryTest {

  private static final LocalDate PERIOD_START = LocalDate.of(2026, 5, 20);
  private static final LocalDate PERIOD_END = LocalDate.of(2026, 5, 25);
  private static final LocalDateTime START = PERIOD_START.atStartOfDay();
  private static final LocalDateTime END_EXCLUSIVE = PERIOD_END.plusDays(1).atStartOfDay();

  @Autowired private TestEntityManager entityManager;
  @Autowired private ShelterRepository shelters;

  private final DistrictOfficer officer = SampleEntities.districtOfficer("DO-1");

  private void save(Object... entities) {
    for (Object entity : entities) {
      entityManager.persist(entity);
    }
    entityManager.flush();
    entityManager.clear();
  }

  private Shelter shelter(
      String id,
      District district,
      int capacity,
      int occupancy,
      ShelterStatus status,
      List<OccupancyRecord> records,
      List<Warning> warnings) {
    return new Shelter(
        id,
        "Shelter " + id,
        "Address",
        district,
        capacity,
        occupancy,
        "0112345678",
        status,
        officer,
        null,
        records,
        warnings);
  }

  private OccupancyRecord record(LocalDateTime capturedAt) {
    return new OccupancyRecord(10, capturedAt, capturedAt.plusMinutes(1), officer);
  }

  private static List<String> ids(List<Shelter> found) {
    return found.stream().map(Shelter::getShelterId).toList();
  }

  @Test
  @DisplayName("findByDistrict gives the shelters of one district")
  void findByDistrict() {
    save(
        officer,
        shelter("S1", District.COLOMBO, 10, 0, ShelterStatus.OPEN, List.of(), List.of()),
        shelter("S2", District.GALLE, 10, 0, ShelterStatus.OPEN, List.of(), List.of()));

    assertThat(ids(shelters.findByDistrict(District.COLOMBO))).containsExactly("S1");
    assertThat(shelters.findByDistrict(District.JAFFNA)).isEmpty();
  }

  @Test
  @DisplayName(
      "findWithSpace keeps open shelters of the district with enough room, most room first")
  void findWithSpace() {
    save(
        officer,
        shelter("S-MID", District.COLOMBO, 100, 60, ShelterStatus.OPEN, List.of(), List.of()),
        shelter("S-BIG", District.COLOMBO, 200, 50, ShelterStatus.OPEN, List.of(), List.of()),
        shelter("S-EXACT", District.COLOMBO, 50, 10, ShelterStatus.OPEN, List.of(), List.of()),
        shelter("S-TIGHT", District.COLOMBO, 50, 11, ShelterStatus.OPEN, List.of(), List.of()),
        shelter("S-FULL", District.COLOMBO, 100, 0, ShelterStatus.FULL, List.of(), List.of()),
        shelter("S-GALLE", District.GALLE, 500, 0, ShelterStatus.OPEN, List.of(), List.of()));

    // S-EXACT has exactly 40 free places; S-TIGHT has 39; S-FULL is not open; S-GALLE is elsewhere.
    assertThat(ids(shelters.findWithSpace(District.COLOMBO, 40)))
        .containsExactly("S-BIG", "S-MID", "S-EXACT");
    assertThat(ids(shelters.findWithSpace(District.COLOMBO, 151))).isEmpty();
  }

  @Test
  @DisplayName("findMatching needs a record in the period, in a selected district")
  void findMatchingByDistrictAndPeriod() {
    save(
        officer,
        shelter(
            "S-IN", District.COLOMBO, 10, 0, ShelterStatus.OPEN, List.of(record(START)), List.of()),
        shelter(
            "S-END",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(END_EXCLUSIVE)),
            List.of()),
        shelter(
            "S-LAST",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(END_EXCLUSIVE.minusMinutes(1))),
            List.of()),
        shelter(
            "S-BEFORE",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(START.minusMinutes(1))),
            List.of()),
        shelter("S-NONE", District.COLOMBO, 10, 0, ShelterStatus.OPEN, List.of(), List.of()),
        shelter(
            "S-ELSE", District.GALLE, 10, 0, ShelterStatus.OPEN, List.of(record(START)), List.of()),
        shelter(
            "S-MANY",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(START), record(START.plusDays(1))),
            List.of()));
    ReportCriteria criteria =
        FeatureFixtures.criteria(Set.of(District.COLOMBO), null, PERIOD_START, PERIOD_END);

    // S-IN has a record at the start (counts); S-END one at the end (does not); S-LAST one a minute
    // before the end (counts); S-BEFORE one a minute before the start; S-NONE has no record;
    // S-ELSE is in another district; S-MANY has two records in the period and is returned once.
    assertThat(ids(shelters.findMatching(criteria)))
        .containsExactlyInAnyOrder("S-IN", "S-LAST", "S-MANY");
  }

  @Test
  @DisplayName("findMatching with a hazard type keeps that type's shelters and unlinked ones")
  void findMatchingByHazardType() {
    Hazard flood =
        FeatureFixtures.hazard(
            "H-F",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            START.minusDays(1),
            null,
            ORIGIN,
            District.COLOMBO);
    Hazard slide =
        FeatureFixtures.hazard(
            "H-L",
            HazardType.LANDSLIDE,
            HazardStatus.ACTIVE,
            START.minusDays(1),
            null,
            ORIGIN,
            District.COLOMBO);
    DMCOfficer dmcOfficer = SampleEntities.officer("DMC-1", Set.of());
    Warning floodWarning =
        FeatureFixtures.warning(
            "W-F", flood, dmcOfficer, START, END_EXCLUSIVE, List.of(District.COLOMBO), List.of());
    Warning slideWarning =
        FeatureFixtures.warning(
            "W-L", slide, dmcOfficer, START, END_EXCLUSIVE, List.of(District.COLOMBO), List.of());
    save(flood, slide, dmcOfficer, floodWarning, slideWarning, officer);
    save(
        shelter(
            "S-FLOOD",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(START)),
            List.of(floodWarning)),
        shelter(
            "S-SLIDE",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(START)),
            List.of(slideWarning)),
        shelter(
            "S-BOTH",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(START)),
            List.of(slideWarning, floodWarning)),
        shelter(
            "S-FREE",
            District.COLOMBO,
            10,
            0,
            ShelterStatus.OPEN,
            List.of(record(START)),
            List.of()));

    ReportCriteria flood20 =
        FeatureFixtures.criteria(
            Set.of(District.COLOMBO), HazardType.FLOOD, PERIOD_START, PERIOD_END);
    ReportCriteria anyType =
        FeatureFixtures.criteria(Set.of(District.COLOMBO), null, PERIOD_START, PERIOD_END);

    assertThat(ids(shelters.findMatching(flood20)))
        .containsExactlyInAnyOrder("S-FLOOD", "S-BOTH", "S-FREE");
    assertThat(ids(shelters.findMatching(anyType)))
        .containsExactlyInAnyOrder("S-FLOOD", "S-SLIDE", "S-BOTH", "S-FREE");
  }
}
