package lk.sliit.se3070.group058.disasterlink.shelter.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link Shelter}, {@link OccupancyRecord} and the value types beside them. */
class ShelterTest {

  private static final DistrictOfficer OFFICER = SampleEntities.districtOfficer("DO-COL-01");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 9, 0);

  private static ShelterDetails details() {
    return new ShelterDetails(
        "Town Hall", "Main Street", District.COLOMBO, 120, "0112345678", null);
  }

  @Test
  @DisplayName("The drawn constructor reads the details and starts the shelter open and empty")
  void drawnConstructor() {
    Organization operator = SampleEntities.organization("ORG-0001");

    Shelter shelter = new Shelter("SHL-1", details(), OFFICER, operator);

    assertThat(shelter.getName()).isEqualTo("Town Hall");
    assertThat(shelter.getAddress()).isEqualTo("Main Street");
    assertThat(shelter.getDistrict()).isEqualTo(District.COLOMBO);
    assertThat(shelter.getCapacity()).isEqualTo(120);
    assertThat(shelter.getContactNumber()).isEqualTo("0112345678");
    assertThat(shelter.getCurrentOccupancy()).isZero();
    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);
    assertThat(shelter.getOfficer()).isSameAs(OFFICER);
    assertThat(shelter.getOperator()).containsSame(operator);
    assertThat(shelter.getWarnings()).isEmpty();
  }

  @Test
  @DisplayName(
      "A shelter may have no operator, and its warnings cannot be changed through the getter")
  void noOperatorAndUnmodifiableWarnings() {
    Shelter shelter = new Shelter("SHL-2", details(), OFFICER, null);
    List<Warning> warnings = shelter.getWarnings();
    Warning warning =
        SampleEntities.warning(
            "W",
            SampleEntities.hazard("H"),
            SampleEntities.officer("O", java.util.Set.of()),
            List.of(),
            List.of(),
            NOW);

    assertThat(shelter.getOperator()).isEmpty();
    assertThatThrownBy(() -> warnings.add(warning))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("The seed constructor keeps the stored state and copies the lists")
  void seedConstructor() {
    List<OccupancyRecord> records = new ArrayList<>();
    records.add(new OccupancyRecord(40, NOW, NOW.plusMinutes(1), OFFICER));
    List<Warning> warnings = new ArrayList<>();

    Shelter shelter =
        new Shelter(
            "SHL-3",
            "Hall",
            "Road",
            District.GALLE,
            50,
            50,
            "0771111111",
            ShelterStatus.FULL,
            OFFICER,
            null,
            records,
            warnings);
    records.clear();

    assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.FULL);
    assertThat(shelter.getCurrentOccupancy()).isEqualTo(50);
    assertThat(shelter.getShelterId()).isEqualTo("SHL-3");
  }

  @Test
  @DisplayName("The constructors reject null required arguments")
  void constructorsRejectNull() {
    assertThatNullPointerException().isThrownBy(() -> new Shelter(null, details(), OFFICER, null));
    assertThatNullPointerException().isThrownBy(() -> new Shelter("S", null, OFFICER, null));
    assertThatNullPointerException().isThrownBy(() -> new Shelter("S", details(), null, null));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Shelter(
                    "S",
                    null,
                    "a",
                    District.GALLE,
                    1,
                    0,
                    "c",
                    ShelterStatus.OPEN,
                    OFFICER,
                    null,
                    List.of(),
                    List.of()));
  }

  @Test
  @DisplayName("An occupancy record gets its own identifier and keeps its officer")
  void occupancyRecord() {
    OccupancyRecord first = new OccupancyRecord(10, NOW, NOW.plusMinutes(2), OFFICER);
    OccupancyRecord second = new OccupancyRecord(10, NOW, NOW.plusMinutes(2), OFFICER);

    assertThat(first.getRecordId()).hasSize(36).isNotEqualTo(second.getRecordId());
    assertThat(first.getOccupantCount()).isEqualTo(10);
    assertThat(first.getCapturedAt()).isEqualTo(NOW);
    assertThat(first.getRecordedAt()).isEqualTo(NOW.plusMinutes(2));
    assertThat(first.getOfficer()).isSameAs(OFFICER);
    assertThat(first)
        .isNotEqualTo(second)
        .isEqualTo(first)
        .hasToString("OccupancyRecord[" + first.getRecordId() + "]");
    assertThat(first.equals("x")).isFalse();
    assertThatNullPointerException().isThrownBy(() -> new OccupancyRecord(1, null, NOW, OFFICER));
    assertThatNullPointerException().isThrownBy(() -> new OccupancyRecord(1, NOW, null, OFFICER));
    assertThatNullPointerException().isThrownBy(() -> new OccupancyRecord(1, NOW, NOW, null));
  }

  @Test
  @DisplayName("Shelters are equal when their identifiers are equal")
  void equalsFollowsTheIdentifier() {
    Shelter shelter = new Shelter("SHL-1", details(), OFFICER, null);

    assertThat(shelter)
        .isEqualTo(new Shelter("SHL-1", details(), OFFICER, null))
        .isNotEqualTo(new Shelter("SHL-2", details(), OFFICER, null))
        .hasSameHashCodeAs(new Shelter("SHL-1", details(), OFFICER, null))
        .hasToString("Shelter[SHL-1]");
    assertThat(shelter.equals(shelter)).isTrue();
    assertThat(shelter.equals("x")).isFalse();
  }

  @Test
  @DisplayName("ShelterView keeps the shelter and its remaining capacity")
  void shelterView() {
    Shelter shelter = new Shelter("SHL-1", details(), OFFICER, null);

    ShelterView view = new ShelterView(shelter, 30);

    assertThat(view.shelter()).isSameAs(shelter);
    assertThat(view.remainingCapacity()).isEqualTo(30);
    assertThatNullPointerException().isThrownBy(() -> new ShelterView(null, 1));
  }

  @Test
  @DisplayName("ShelterOutcome copies the alternatives and allows no shelter")
  void shelterOutcome() {
    SyncResult result = new SyncResult("s", SyncOutcome.ACCEPTED, null, null);
    Shelter shelter = new Shelter("SHL-1", details(), OFFICER, null);
    List<ShelterView> alternatives = new ArrayList<>(List.of(new ShelterView(shelter, 5)));

    ShelterOutcome withShelter = new ShelterOutcome(result, shelter, 12, alternatives);
    final ShelterOutcome without = new ShelterOutcome(result, null, 0, List.of());
    alternatives.clear();

    assertThat(withShelter.alternatives()).hasSize(1);
    assertThat(withShelter.shelter()).isSameAs(shelter);
    assertThat(withShelter.remainingCapacity()).isEqualTo(12);
    assertThat(without.shelter()).isNull();
    assertThat(without.result()).isSameAs(result);
    assertThatThrownBy(() -> withShelter.alternatives().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatNullPointerException().isThrownBy(() -> new ShelterOutcome(null, null, 0, List.of()));
    assertThatNullPointerException().isThrownBy(() -> new ShelterOutcome(result, null, 0, null));
  }
}
