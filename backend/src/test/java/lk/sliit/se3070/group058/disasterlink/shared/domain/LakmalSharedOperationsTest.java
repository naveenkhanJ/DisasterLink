package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Documents Lakmal's seven placeholder operations on the shared entities. Each throws until he
 * writes the body on his branch; he replaces this file's tests with the real tests of his
 * operations (implementation standards section 3, P155 X4).
 */
class LakmalSharedOperationsTest {

  private static final Hazard HAZARD = SampleEntities.hazard("HZ-2026-0201");

  private static final Warning WARNING =
      SampleEntities.warning(
          "WRN-1",
          HAZARD,
          SampleEntities.officer("DMC-1", Set.of()),
          List.of(District.GALLE),
          List.of(),
          NOW.plusHours(1));

  private static final Notification NOTIFICATION =
      new Notification(SampleEntities.organization("ORG-1"), NotificationChannel.SMS, "t", 1);

  @Test
  @DisplayName("Hazard.updateHazard is Lakmal's placeholder")
  void updateHazard() {
    HazardUpdate update = new HazardUpdate("d", HazardSeverity.LOW, List.of());

    assertThatThrownBy(() -> HAZARD.updateHazard(update))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("Hazard.missingInformation is Lakmal's placeholder")
  void missingInformation() {
    assertThatThrownBy(HAZARD::missingInformation)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("Hazard.isAssessable is Lakmal's placeholder")
  void isAssessable() {
    assertThatThrownBy(HAZARD::isAssessable)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("Warning.escalate is Lakmal's placeholder")
  void escalate() {
    EscalationDetails details =
        new EscalationDetails(
            WarningLevel.LEVEL_3_RED, SampleEntities.TEXT, SampleEntities.TEXT, NOW);

    assertThatThrownBy(() -> WARNING.escalate(details))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("Warning.addNotification is Lakmal's placeholder")
  void addNotification() {
    assertThatThrownBy(() -> WARNING.addNotification(NOTIFICATION))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("Notification.markDelivered is Lakmal's placeholder")
  void markDelivered() {
    assertThatThrownBy(() -> NOTIFICATION.markDelivered(NOW))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("Notification.markFailed is Lakmal's placeholder")
  void markFailed() {
    assertThatThrownBy(() -> NOTIFICATION.markFailed(NOW))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }
}
