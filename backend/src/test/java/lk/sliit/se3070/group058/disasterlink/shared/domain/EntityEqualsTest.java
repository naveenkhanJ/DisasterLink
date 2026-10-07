package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the identifier-based {@code equals} of every entity against an entity of the same class
 * with another identifier, against itself, against {@code null} and against another type.
 */
class EntityEqualsTest {

  private static final Hazard HAZARD = SampleEntities.hazard("HZ-1");
  private static final DMCOfficer OFFICER = SampleEntities.officer("DMC-1", Set.of());
  private static final ReliefSupply SUPPLY = supply("SUP-1");

  private static ReliefSupply supply(String id) {
    return new ReliefSupply(
        id, "n", SupplyCategory.FOOD, 1, "kg", "l", SupplyStatus.AVAILABLE, null, null);
  }

  private static Warning warning(String id) {
    return SampleEntities.warning(id, HAZARD, OFFICER, List.of(), List.of(), NOW.plusHours(1));
  }

  private static ResourceDistribution distribution(String id) {
    return new ResourceDistribution(
        id,
        1,
        NOW,
        District.GALLE,
        "d",
        SUPPLY,
        null,
        SampleEntities.districtOfficer("DO-1"),
        null);
  }

  private static Notification notification() {
    return new Notification(SampleEntities.organization("ORG-1"), NotificationChannel.SMS, "t", 1);
  }

  private static AuditRecord audit() {
    return new AuditRecord(AuditAction.REPORT_SHARED, OFFICER, "DR-1", null, NOW);
  }

  static Stream<Arguments> pairsWithDifferentIdentifiers() {
    return Stream.of(
        Arguments.of(SampleEntities.citizen("U-1", null), SampleEntities.citizen("U-2", null)),
        Arguments.of(SampleEntities.organization("ORG-1"), SampleEntities.organization("ORG-2")),
        Arguments.of(SampleEntities.hazard("HZ-1"), SampleEntities.hazard("HZ-2")),
        Arguments.of(warning("WRN-1"), warning("WRN-2")),
        Arguments.of(notification(), notification()),
        Arguments.of(SUPPLY, supply("SUP-2")),
        Arguments.of(distribution("DIST-1"), distribution("DIST-2")),
        Arguments.of(audit(), audit()),
        Arguments.of(
            new ClientSyncStatus("c-1", District.GALLE, NOW, 0),
            new ClientSyncStatus("c-2", District.GALLE, NOW, 0)),
        Arguments.of(
            new ProcessedSubmission("s-1", "c", NOW, NOW, null),
            new ProcessedSubmission("s-2", "c", NOW, NOW, null)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("pairsWithDifferentIdentifiers")
  @DisplayName("An entity equals itself but not another identifier, null or another type")
  void equalsFollowsTheIdentifier(Object first, Object second) {
    assertThat(first.equals(first)).isTrue();
    assertThat(first.equals(second)).isFalse();
    assertThat(first.equals(null)).isFalse();
    assertThat(first.equals("not an entity")).isFalse();
  }
}
