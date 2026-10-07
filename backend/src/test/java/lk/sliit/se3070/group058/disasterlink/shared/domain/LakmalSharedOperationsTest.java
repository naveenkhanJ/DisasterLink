package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of Lakmal's operations on the shared entities (implementation standards section 3, P155
 * X4): {@code Hazard.updateHazard}, {@code missingInformation} and {@code isAssessable}, {@code
 * Warning.escalate} and {@code addNotification}, and {@code Notification.markDelivered} and {@code
 * markFailed}.
 */
class LakmalSharedOperationsTest {

  private static Hazard hazardWithDistricts(List<District> districts, HazardStatus status) {
    return new Hazard(
        "HZ-2026-0203",
        HazardType.FLOOD,
        "Kalu Ganga rising at the Ratnapura gauge",
        HazardSeverity.MEDIUM,
        NOW,
        null,
        SampleEntities.RATNAPURA_POINT,
        "Kalu Ganga at Ratnapura",
        districts,
        RiverBasin.KALU,
        HazardSource.AGENCY_BULLETIN,
        status);
  }

  private static Warning warning() {
    return SampleEntities.warning(
        "WRN-1",
        SampleEntities.hazard("HZ-2026-0201"),
        SampleEntities.officer("DMC-1", Set.of()),
        List.of(District.GALLE),
        List.of(),
        NOW.plusHours(1));
  }

  private static Notification notification() {
    return new Notification(SampleEntities.organization("ORG-1"), NotificationChannel.SMS, "t", 1);
  }

  @Test
  @DisplayName("A1 5c — updateHazard records the new description, severity and districts")
  void updateHazardRecordsTheUpdate() {
    Hazard hazard = hazardWithDistricts(List.of(), HazardStatus.ACTIVE);

    hazard.updateHazard(
        new HazardUpdate(
            "Kalu Ganga above major flood level",
            HazardSeverity.HIGH,
            List.of(District.RATNAPURA, District.KALUTARA)));

    assertThat(hazard.getDescription()).isEqualTo("Kalu Ganga above major flood level");
    assertThat(hazard.getSeverity()).isEqualTo(HazardSeverity.HIGH);
    assertThat(hazard.getDistricts()).containsExactly(District.RATNAPURA, District.KALUTARA);
  }

  @Test
  @DisplayName("updateHazard refuses an update it was not meant to receive (no severity)")
  void updateHazardRequiresTheCheckedComponents() {
    Hazard hazard = hazardWithDistricts(List.of(District.RATNAPURA), HazardStatus.ACTIVE);

    assertThatNullPointerException()
        .isThrownBy(() -> hazard.updateHazard(new HazardUpdate("d", null, List.of())));
    assertThatNullPointerException().isThrownBy(() -> hazard.updateHazard(null));
  }

  @Test
  @DisplayName("Main step 5 — a complete hazard has no missing information")
  void completeHazardMissesNothing() {
    assertThat(SampleEntities.hazard("HZ-2026-0142").missingInformation()).isEmpty();
  }

  @Test
  @DisplayName("A1 5a — a hazard recorded without districts misses its affected districts")
  void hazardWithoutDistrictsMissesThem() {
    Hazard hazard = hazardWithDistricts(List.of(), HazardStatus.ACTIVE);

    assertThat(hazard.missingInformation()).containsExactly("affected districts");
  }

  @Test
  @DisplayName("A1 5a — a blank description is reported as missing")
  void blankDescriptionIsMissing() {
    Hazard hazard = hazardWithDistricts(List.of(District.RATNAPURA), HazardStatus.ACTIVE);
    hazard.updateHazard(new HazardUpdate(" ", HazardSeverity.LOW, List.of(District.RATNAPURA)));

    assertThat(hazard.missingInformation()).containsExactly("description");
  }

  @Test
  @DisplayName("Main step 2 — an active hazard is assessable, a closed one is not")
  void onlyActiveHazardsAreAssessable() {
    assertThat(hazardWithDistricts(List.of(), HazardStatus.ACTIVE).isAssessable()).isTrue();
    assertThat(hazardWithDistricts(List.of(), HazardStatus.CLOSED).isAssessable()).isFalse();
  }

  @Test
  @DisplayName("Main step 14 — addNotification adds the notification and attaches the warning")
  void addNotificationAddsAndAttaches() {
    Warning warning = warning();
    Notification notification = notification();

    warning.addNotification(notification);

    assertThat(warning.getNotifications()).containsExactly(notification);
    assertThat(notification.getWarning()).contains(warning);
  }

  @Test
  @DisplayName("addNotification refuses a missing notification")
  void addNotificationRefusesNull() {
    Warning warning = warning();

    assertThatNullPointerException().isThrownBy(() -> warning.addNotification(null));
    assertThat(warning.getNotifications()).isEmpty();
  }

  @Test
  @DisplayName("Main step 16 — markDelivered records DELIVERED with the time")
  void markDeliveredRecordsTheDelivery() {
    Notification notification = notification();

    notification.markDelivered(NOW);

    assertThat(notification.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    assertThat(notification.getSentTime()).contains(NOW);
  }

  @Test
  @DisplayName("E1 16b — markFailed records FAILED with the time")
  void markFailedRecordsTheFailure() {
    Notification notification = notification();

    notification.markFailed(NOW);

    assertThat(notification.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
    assertThat(notification.getSentTime()).contains(NOW);
  }

  @Test
  @DisplayName("markDelivered and markFailed need the time")
  void markingNeedsTheTime() {
    Notification notification = notification();

    assertThatNullPointerException().isThrownBy(() -> notification.markDelivered(null));
    assertThatNullPointerException().isThrownBy(() -> notification.markFailed(null));
    assertThat(notification.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
  }

  @Test
  @DisplayName("Escalate step 10 — escalate records the new level, texts and expiry time")
  void escalateRecordsTheNewDetails() {
    Warning warning = warning();
    LocalisedText text = new LocalisedText("si", "ta", "Evacuate now");

    warning.escalate(new EscalationDetails(WarningLevel.LEVEL_3_RED, text, text, NOW.plusDays(1)));

    assertThat(warning.getWarningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(warning.getMessage()).isEqualTo(text);
    assertThat(warning.getInstructions()).isEqualTo(text);
    assertThat(warning.getExpiryTime()).isEqualTo(NOW.plusDays(1));
    assertThat(warning.getWarningId()).isEqualTo("WRN-1");
  }
}
