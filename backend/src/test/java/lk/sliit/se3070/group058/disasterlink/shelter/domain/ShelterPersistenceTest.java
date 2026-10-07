package lk.sliit.se3070.group058.disasterlink.shelter.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Round trips of {@link Shelter} and {@link OccupancyRecord} through H2. */
@DataJpaTest
class ShelterPersistenceTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 9, 0);

  @Autowired private TestEntityManager entityManager;

  @Test
  @DisplayName("A shelter from the drawn constructor is stored with its officer and operator")
  void drawnShelterRoundTrip() {
    DistrictOfficer officer = SampleEntities.districtOfficer("DO-COL-01");
    Organization operator = SampleEntities.organization("ORG-0001");
    entityManager.persist(officer);
    entityManager.persist(operator);
    ShelterDetails details =
        new ShelterDetails("Town Hall", "Main Street", District.COLOMBO, 120, "0112345678", null);
    entityManager.persistAndFlush(new Shelter("SHL-1", details, officer, operator));
    entityManager.clear();

    Shelter loaded = entityManager.find(Shelter.class, "SHL-1");

    assertThat(loaded.getName()).isEqualTo("Town Hall");
    assertThat(loaded.getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(loaded.getCurrentOccupancy()).isZero();
    assertThat(loaded.getOfficer().getUserId()).isEqualTo("DO-COL-01");
    assertThat(loaded.getOperator().orElseThrow().getOrganizationId()).isEqualTo("ORG-0001");
    assertThat(loaded.getWarnings()).isEmpty();
  }

  @Test
  @DisplayName("A seeded shelter keeps its records in order and its allocated warnings")
  void seededShelterRoundTrip() {
    DistrictOfficer officer = SampleEntities.districtOfficer("DO-COL-01");
    Hazard hazard = SampleEntities.hazard("HZ-1");
    var dmcOfficer = SampleEntities.officer("DMC-1", Set.of());
    entityManager.persist(officer);
    entityManager.persist(hazard);
    entityManager.persist(dmcOfficer);
    Warning warning =
        SampleEntities.warning(
            "WRN-1", hazard, dmcOfficer, List.of(District.COLOMBO), List.of(), NOW.plusDays(1));
    entityManager.persist(warning);
    Shelter shelter =
        new Shelter(
            "SHL-2",
            "Hall",
            "Road",
            District.COLOMBO,
            100,
            60,
            "0771111111",
            ShelterStatus.OPEN,
            officer,
            null,
            List.of(
                new OccupancyRecord(20, NOW, NOW.plusMinutes(1), officer),
                new OccupancyRecord(
                    60, NOW.plusHours(1), NOW.plusHours(1).plusMinutes(1), officer)),
            List.of(warning));
    entityManager.persistAndFlush(shelter);
    entityManager.clear();

    Shelter loaded = entityManager.find(Shelter.class, "SHL-2");

    assertThat(loaded.getCurrentOccupancy()).isEqualTo(60);
    assertThat(loaded.getOperator()).isEmpty();
    assertThat(loaded.getWarnings()).extracting(Warning::getWarningId).containsExactly("WRN-1");
    // The shelter has no getter for its records (the owner writes getOccupancyRecords), so the
    // order is read through a query that uses the list index.
    List<Integer> counts =
        entityManager
            .getEntityManager()
            .createQuery(
                "select o.occupantCount from Shelter s join s.occupancyRecords o"
                    + " where s.shelterId = 'SHL-2' order by index(o)",
                Integer.class)
            .getResultList();
    assertThat(counts).containsExactly(20, 60);
  }

  @Test
  @DisplayName("A single occupancy record is stored with its officer")
  void occupancyRecordRoundTrip() {
    DistrictOfficer officer = SampleEntities.districtOfficer("DO-COL-01");
    entityManager.persist(officer);
    OccupancyRecord record = new OccupancyRecord(15, NOW, NOW.plusMinutes(3), officer);
    entityManager.persistAndFlush(record);
    entityManager.clear();

    OccupancyRecord loaded = entityManager.find(OccupancyRecord.class, record.getRecordId());

    assertThat(loaded.getOccupantCount()).isEqualTo(15);
    assertThat(loaded.getCapturedAt()).isEqualTo(NOW);
    assertThat(loaded.getRecordedAt()).isEqualTo(NOW.plusMinutes(3));
    assertThat(loaded.getOfficer().getUserId()).isEqualTo("DO-COL-01");
    assertThat(loaded).isEqualTo(record);
  }
}
