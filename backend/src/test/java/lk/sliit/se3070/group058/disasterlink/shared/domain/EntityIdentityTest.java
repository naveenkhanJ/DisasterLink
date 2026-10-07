package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that entities are equal when their identifiers are equal, and print as class and
 * identifier.
 */
class EntityIdentityTest {

  static Stream<Arguments> sameIdentifierPairs() {
    return Stream.of(
        Arguments.of(
            SampleEntities.citizen("U-1", null),
            SampleEntities.districtOfficer("U-1"),
            "Citizen[U-1]"),
        Arguments.of(
            SampleEntities.organization("ORG-1"),
            SampleEntities.organization("ORG-1"),
            "Organization[ORG-1]"),
        Arguments.of(SampleEntities.hazard("HZ-1"), SampleEntities.hazard("HZ-1"), "Hazard[HZ-1]"),
        Arguments.of(
            new ClientSyncStatus("c-1", District.GALLE, NOW, 0),
            new ClientSyncStatus("c-1", District.JAFFNA, NOW.plusDays(1), 4),
            "ClientSyncStatus[c-1]"),
        Arguments.of(
            new ProcessedSubmission("s-1", "c", NOW, NOW, null),
            new ProcessedSubmission("s-1", "d", NOW, NOW, "x"),
            "ProcessedSubmission[s-1]"));
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("sameIdentifierPairs")
  @DisplayName("Entities with the same identifier are equal, with the same hash code")
  void sameIdentifierMeansEqual(Object first, Object second, String text) {
    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    assertThat(first).hasToString(text);
  }

  @Test
  @DisplayName("Entities with different identifiers, or of another type, are not equal")
  void differentIdentifiersAreNotEqual() {
    Hazard hazard = SampleEntities.hazard("HZ-1");

    assertThat(hazard).isNotEqualTo(SampleEntities.hazard("HZ-2")).isNotEqualTo("HZ-1").isNotNull();
    assertThat(SampleEntities.organization("A")).isNotEqualTo(SampleEntities.organization("B"));
  }

  @Test
  @DisplayName("Subclasses of User share the User identity and print their own class name")
  void userSubclassesShareIdentity() {
    DMCDutyOfficer duty = SampleEntities.dutyOfficer("U-9");
    DMCOfficer officer = SampleEntities.officer("U-9", Set.of());

    assertThat(duty).isEqualTo(officer);
    assertThat(duty).hasToString("DMCDutyOfficer[U-9]");
  }

  @Test
  @DisplayName(
      "Warning, Notification, supplies, distributions and audit records print their identifier")
  void toStringNamesTheIdentifier() {
    Hazard hazard = SampleEntities.hazard("HZ-1");
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    Warning warning =
        SampleEntities.warning("WRN-1", hazard, officer, List.of(), List.of(), NOW.plusHours(1));
    Warning sameWarning =
        SampleEntities.warning("WRN-1", hazard, officer, List.of(), List.of(), NOW.plusHours(2));
    ReliefSupply supply =
        new ReliefSupply(
            "SUP-1", "n", SupplyCategory.FOOD, 1, "kg", "l", SupplyStatus.AVAILABLE, null, null);
    ResourceDistribution distribution =
        new ResourceDistribution(
            "DIST-1",
            1,
            NOW,
            District.GALLE,
            "d",
            supply,
            null,
            SampleEntities.districtOfficer("DO-1"),
            null);

    assertThat(warning).isEqualTo(sameWarning).hasToString("Warning[WRN-1]");
    assertThat(supply).hasToString("ReliefSupply[SUP-1]");
    assertThat(distribution).hasToString("ResourceDistribution[DIST-1]");
  }
}
