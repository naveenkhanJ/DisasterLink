package lk.sliit.se3070.group058.disasterlink.shared.repository;

import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.DIAGONAL;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.JAFFNA_POINT;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.NORTH_3_KM;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.NORTH_7_KM;
import static lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures.ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Tests of {@link HazardRepository} and {@link WarningRepository}. */
@DataJpaTest
class HazardAndWarningRepositoryTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);
  private static final LocalDate PERIOD_START = LocalDate.of(2026, 5, 20);
  private static final LocalDate PERIOD_END = LocalDate.of(2026, 5, 25);

  @Autowired private TestEntityManager entityManager;
  @Autowired private HazardRepository hazards;
  @Autowired private WarningRepository warnings;

  private void save(Object... entities) {
    for (Object entity : entities) {
      entityManager.persist(entity);
    }
    entityManager.flush();
    entityManager.clear();
  }

  private static Hazard hazard(
      String id, HazardType type, HazardStatus status, GeoPoint location, District... districts) {
    return FeatureFixtures.hazard(
        id, type, status, LocalDateTime.of(2026, 5, 21, 6, 0), null, location, districts);
  }

  private static List<String> ids(List<Hazard> found) {
    return found.stream().map(Hazard::getHazardId).toList();
  }

  private static List<String> warningIds(List<Warning> found) {
    return found.stream().map(Warning::getWarningId).toList();
  }

  // ---------------------------------------------------------------- HazardRepository

  @Test
  @DisplayName("findAssessable gives the ACTIVE hazards, oldest start first")
  void findAssessable() {
    save(
        FeatureFixtures.hazard(
            "H-NEW", HazardType.FLOOD, HazardStatus.ACTIVE, NOW, null, ORIGIN, District.GALLE),
        FeatureFixtures.hazard(
            "H-OLD",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            NOW.minusDays(1),
            null,
            ORIGIN,
            District.GALLE),
        FeatureFixtures.hazard(
            "H-CLOSED",
            HazardType.FLOOD,
            HazardStatus.CLOSED,
            NOW.minusDays(2),
            NOW,
            ORIGIN,
            District.GALLE));

    assertThat(ids(hazards.findAssessable())).containsExactly("H-OLD", "H-NEW");
  }

  @Test
  @DisplayName("findClosed gives the CLOSED hazards, latest end first")
  void findClosed() {
    save(
        FeatureFixtures.hazard(
            "H-C1",
            HazardType.FLOOD,
            HazardStatus.CLOSED,
            NOW.minusDays(5),
            NOW.minusDays(3),
            ORIGIN,
            District.GALLE),
        FeatureFixtures.hazard(
            "H-C2",
            HazardType.FLOOD,
            HazardStatus.CLOSED,
            NOW.minusDays(5),
            NOW.minusDays(1),
            ORIGIN,
            District.GALLE),
        FeatureFixtures.hazard(
            "H-ACTIVE", HazardType.FLOOD, HazardStatus.ACTIVE, NOW, null, ORIGIN, District.GALLE));

    assertThat(ids(hazards.findClosed())).containsExactly("H-C2", "H-C1");
  }

  @Test
  @DisplayName("findActiveNear keeps district matches and hazards within 5 km, and nothing else")
  void findActiveNear() {
    save(
        hazard("H-A", HazardType.FLOOD, HazardStatus.ACTIVE, JAFFNA_POINT, District.RATNAPURA),
        hazard("H-B", HazardType.FLOOD, HazardStatus.ACTIVE, NORTH_3_KM, District.GALLE),
        hazard("H-C", HazardType.FLOOD, HazardStatus.ACTIVE, NORTH_7_KM, District.GALLE),
        hazard("H-D", HazardType.LANDSLIDE, HazardStatus.ACTIVE, NORTH_3_KM, District.RATNAPURA),
        hazard("H-E", HazardType.FLOOD, HazardStatus.CLOSED, NORTH_3_KM, District.RATNAPURA),
        hazard("H-F", HazardType.FLOOD, HazardStatus.ACTIVE, NORTH_3_KM, District.RATNAPURA),
        hazard("H-G", HazardType.FLOOD, HazardStatus.ACTIVE, DIAGONAL, District.GALLE));

    List<Hazard> found = hazards.findActiveNear(ORIGIN, HazardType.FLOOD, District.RATNAPURA);

    // A: district only. B: 3 km. F: both, returned once. C: 7 km. D: type. E: closed. G: in the
    // box, 6 km.
    assertThat(ids(found)).containsExactlyInAnyOrder("H-A", "H-B", "H-F").doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("findMatching applies district, type and the period, with its edges")
  void hazardFindMatching() {
    LocalDateTime start = PERIOD_START.atStartOfDay();
    save(
        FeatureFixtures.hazard(
            "M1",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            start.plusDays(1),
            null,
            ORIGIN,
            District.RATNAPURA),
        FeatureFixtures.hazard(
            "M2",
            HazardType.FLOOD,
            HazardStatus.CLOSED,
            start.minusDays(10),
            start.minusMinutes(1),
            ORIGIN,
            District.RATNAPURA),
        FeatureFixtures.hazard(
            "M3",
            HazardType.FLOOD,
            HazardStatus.CLOSED,
            start.minusDays(10),
            start,
            ORIGIN,
            District.RATNAPURA),
        FeatureFixtures.hazard(
            "M4",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            PERIOD_END.plusDays(1).atStartOfDay(),
            null,
            ORIGIN,
            District.RATNAPURA),
        FeatureFixtures.hazard(
            "M5",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            PERIOD_END.plusDays(1).atStartOfDay().minusMinutes(1),
            null,
            ORIGIN,
            District.RATNAPURA),
        FeatureFixtures.hazard(
            "M6",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            start.plusDays(1),
            null,
            ORIGIN,
            District.GALLE),
        FeatureFixtures.hazard(
            "M7",
            HazardType.LANDSLIDE,
            HazardStatus.ACTIVE,
            start.plusDays(1),
            null,
            ORIGIN,
            District.RATNAPURA),
        FeatureFixtures.hazard(
            "M8",
            HazardType.FLOOD,
            HazardStatus.ACTIVE,
            start.plusDays(2),
            null,
            ORIGIN,
            District.RATNAPURA,
            District.GALLE));

    ReportCriteria floodInRatnapura =
        FeatureFixtures.criteria(
            Set.of(District.RATNAPURA), HazardType.FLOOD, PERIOD_START, PERIOD_END);
    ReportCriteria anyTypeInRatnapura =
        FeatureFixtures.criteria(Set.of(District.RATNAPURA), null, PERIOD_START, PERIOD_END);
    ReportCriteria floodInTwoDistricts =
        FeatureFixtures.criteria(
            Set.of(District.RATNAPURA, District.GALLE), HazardType.FLOOD, PERIOD_START, PERIOD_END);

    // M2 ended a minute before the period; M3 ended at its start (counts); M4 started at the end
    // (does not count); M5 started a minute before the end (counts); M6 is in another district.
    assertThat(ids(hazards.findMatching(floodInRatnapura)))
        .containsExactlyInAnyOrder("M1", "M3", "M5", "M8");
    assertThat(ids(hazards.findMatching(anyTypeInRatnapura)))
        .containsExactlyInAnyOrder("M1", "M3", "M5", "M7", "M8");
    assertThat(ids(hazards.findMatching(floodInTwoDistricts)))
        .containsExactlyInAnyOrder("M1", "M3", "M5", "M6", "M8")
        .doesNotHaveDuplicates();
  }

  // ---------------------------------------------------------------- WarningRepository

  private Warning warning(
      String id,
      Hazard hazard,
      DMCOfficer officer,
      LocalDateTime issued,
      LocalDateTime expiry,
      List<District> districts,
      List<RiverBasin> basins) {
    return FeatureFixtures.warning(id, hazard, officer, issued, expiry, districts, basins);
  }

  @Test
  @DisplayName("findActiveByDistrict keeps warnings that are active now and cover the district")
  void findActiveByDistrict() {
    Hazard hazard = hazard("H-1", HazardType.FLOOD, HazardStatus.ACTIVE, ORIGIN, District.COLOMBO);
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    LocalDateTime later = NOW.plusHours(2);
    save(
        hazard,
        officer,
        warning(
            "W1", hazard, officer, NOW.minusHours(2), later, List.of(District.COLOMBO), List.of()),
        warning(
            "W2",
            hazard,
            officer,
            NOW.minusHours(1),
            NOW.plusHours(1),
            List.of(),
            List.of(RiverBasin.KALU)),
        warning(
            "W3", hazard, officer, NOW.minusHours(2), NOW, List.of(District.COLOMBO), List.of()),
        warning(
            "W4", hazard, officer, NOW.plusSeconds(1), later, List.of(District.COLOMBO), List.of()),
        warning("W5", hazard, officer, NOW, later, List.of(District.COLOMBO), List.of()),
        warning(
            "W6", hazard, officer, NOW.minusHours(2), later, List.of(District.GALLE), List.of()));

    // W3 expires exactly now (expired); W4 is issued a second from now; W5 is issued exactly
    // now (active).
    assertThat(warningIds(warnings.findActiveByDistrict(District.COLOMBO, NOW)))
        .containsExactlyInAnyOrder("W1", "W5");
    assertThat(warningIds(warnings.findActiveByDistrict(District.KALUTARA, NOW)))
        .containsExactly("W2");
    assertThat(warnings.findActiveByDistrict(District.JAFFNA, NOW)).isEmpty();
  }

  @Test
  @DisplayName("findActiveByHazard gives the one active warning of a hazard, if there is one")
  void findActiveByHazard() {
    Hazard withActive =
        hazard("H-1", HazardType.FLOOD, HazardStatus.ACTIVE, ORIGIN, District.COLOMBO);
    Hazard withExpired =
        hazard("H-2", HazardType.FLOOD, HazardStatus.ACTIVE, ORIGIN, District.COLOMBO);
    Hazard withFuture =
        hazard("H-3", HazardType.FLOOD, HazardStatus.ACTIVE, ORIGIN, District.COLOMBO);
    Hazard withNone =
        hazard("H-4", HazardType.FLOOD, HazardStatus.ACTIVE, ORIGIN, District.COLOMBO);
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    save(
        withActive,
        withExpired,
        withFuture,
        withNone,
        officer,
        warning(
            "W1", withActive, officer, NOW.minusHours(1), NOW.plusHours(1), List.of(), List.of()),
        warning("W2", withExpired, officer, NOW.minusHours(3), NOW, List.of(), List.of()),
        warning(
            "W3", withFuture, officer, NOW.plusSeconds(1), NOW.plusHours(1), List.of(), List.of()));

    assertThat(warnings.findActiveByHazard("H-1", NOW)).map(Warning::getWarningId).contains("W1");
    assertThat(warnings.findActiveByHazard("H-2", NOW)).isEmpty();
    assertThat(warnings.findActiveByHazard("H-3", NOW)).isEmpty();
    assertThat(warnings.findActiveByHazard("H-4", NOW)).isEmpty();
  }

  @Test
  @DisplayName("Warning findMatching applies the period, the hazard type and the covered districts")
  void warningFindMatching() {
    Hazard flood = hazard("H-F", HazardType.FLOOD, HazardStatus.ACTIVE, ORIGIN, District.RATNAPURA);
    Hazard slide =
        hazard("H-L", HazardType.LANDSLIDE, HazardStatus.ACTIVE, ORIGIN, District.RATNAPURA);
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    LocalDateTime start = PERIOD_START.atStartOfDay();
    LocalDateTime endExclusive = PERIOD_END.plusDays(1).atStartOfDay();
    LocalDateTime expiry = endExclusive.plusDays(3);
    save(
        flood,
        slide,
        officer,
        warning("X1", flood, officer, start, expiry, List.of(District.RATNAPURA), List.of()),
        warning("X2", flood, officer, endExclusive, expiry, List.of(District.RATNAPURA), List.of()),
        warning(
            "X3",
            flood,
            officer,
            endExclusive.minusMinutes(1),
            expiry,
            List.of(),
            List.of(RiverBasin.KALU)),
        warning(
            "X4",
            flood,
            officer,
            start.minusMinutes(1),
            expiry,
            List.of(District.RATNAPURA),
            List.of()),
        warning(
            "X5",
            slide,
            officer,
            start.plusDays(1),
            expiry,
            List.of(District.RATNAPURA),
            List.of()),
        warning(
            "X6", flood, officer, start.plusDays(1), expiry, List.of(District.GALLE), List.of()));

    ReportCriteria floodInRatnapura =
        FeatureFixtures.criteria(
            Set.of(District.RATNAPURA), HazardType.FLOOD, PERIOD_START, PERIOD_END);
    ReportCriteria anyType =
        FeatureFixtures.criteria(Set.of(District.RATNAPURA), null, PERIOD_START, PERIOD_END);

    // X1 issued at the start (counts); X2 at the end (does not); X3 a minute before the end,
    // covered through the Kalu basin; X4 a minute before the start; X5 another hazard type; X6
    // another district.
    assertThat(warningIds(warnings.findMatching(floodInRatnapura))).containsExactly("X1", "X3");
    assertThat(warningIds(warnings.findMatching(anyType))).containsExactly("X1", "X5", "X3");
  }
}
