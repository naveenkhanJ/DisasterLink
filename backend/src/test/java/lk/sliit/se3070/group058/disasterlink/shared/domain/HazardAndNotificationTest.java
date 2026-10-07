package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the constructors of {@link Hazard} and {@link Notification}. */
class HazardAndNotificationTest {

  @Test
  @DisplayName("The seed constructor of Hazard keeps the stored state")
  void hazardSeedConstructor() {
    Hazard hazard = SampleEntities.hazard("HZ-2026-0142");

    assertThat(hazard.getHazardId()).isEqualTo("HZ-2026-0142");
    assertThat(hazard.getType()).isEqualTo(HazardType.FLOOD);
    assertThat(hazard.getRiverBasin()).contains(RiverBasin.KALU);
    assertThat(hazard.getEndTime()).isEmpty();
    assertThat(hazard.getSource()).isEqualTo(HazardSource.AGENCY_BULLETIN);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.ACTIVE);
    assertThat(hazard.getLocation()).isEqualTo(SampleEntities.RATNAPURA_POINT);
    assertThat(hazard.getDistricts()).containsExactly(District.RATNAPURA, District.KALUTARA);
  }

  @Test
  @DisplayName("The seed constructor of Hazard can restore a closed hazard with an end time")
  void hazardSeedConstructorRestoresClosedHazard() {
    Hazard hazard =
        new Hazard(
            "HZ-2026-0142",
            HazardType.FLOOD,
            "Ended",
            HazardSeverity.HIGH,
            NOW.minusDays(5),
            NOW.minusDays(2),
            SampleEntities.RATNAPURA_POINT,
            "Kalu Ganga",
            List.of(District.RATNAPURA),
            null,
            HazardSource.VERIFIED_REPORT,
            HazardStatus.CLOSED);

    assertThat(hazard.getEndTime()).contains(NOW.minusDays(2));
    assertThat(hazard.getRiverBasin()).isEmpty();
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.CLOSED);
  }

  @Test
  @DisplayName(
      "Hazard(report, details) reads the report's values and fixes source, status and times")
  void hazardFromAVerifiedReport() {
    NewHazardDetails details =
        new NewHazardDetails(
            HazardSeverity.HIGH, List.of(District.KALUTARA), "Ingiriya bridge", "Bridge flooded");

    Hazard hazard =
        new Hazard(
            "HZ-2026-0301", HazardType.BLOCKED_ROAD, SampleEntities.RATNAPURA_POINT, NOW, details);

    assertThat(hazard.getType()).isEqualTo(HazardType.BLOCKED_ROAD);
    assertThat(hazard.getLocation()).isEqualTo(SampleEntities.RATNAPURA_POINT);
    assertThat(hazard.getStartTime()).isEqualTo(NOW);
    assertThat(hazard.getDescription()).isEqualTo("Bridge flooded");
    assertThat(hazard.getSeverity()).isEqualTo(HazardSeverity.HIGH);
    assertThat(hazard.getDistricts()).containsExactly(District.KALUTARA);
    assertThat(hazard.getLocationName()).isEqualTo("Ingiriya bridge");
    assertThat(hazard.getSource()).isEqualTo(HazardSource.VERIFIED_REPORT);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.ACTIVE);
    assertThat(hazard.getEndTime()).isEmpty();
    assertThat(hazard.getRiverBasin()).isEmpty();
  }

  @Test
  @DisplayName("Hazard rejects null details and the district list cannot be changed")
  void hazardRejectsNullDetails() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> new Hazard("HZ", HazardType.FLOOD, SampleEntities.RATNAPURA_POINT, NOW, null));
    List<District> districts = SampleEntities.hazard("HZ-1").getDistricts();
    assertThatThrownBy(() -> districts.add(District.JAFFNA))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("The drawn constructor of Notification takes kind and id from the recipient")
  void notificationForARecipient() {
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", "token-1");

    Notification notification = new Notification(citizen, NotificationChannel.PUSH, "Move now", 2);

    assertThat(notification.getRecipientKind()).isEqualTo(RecipientKind.CITIZEN);
    assertThat(notification.getRecipientId()).isEqualTo("CIT-KLT-0001");
    assertThat(notification.getRecipient()).containsSame(citizen);
    assertThat(notification.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
    assertThat(notification.getSentTime()).isEmpty();
    assertThat(notification.getType()).isEqualTo(NotificationChannel.PUSH);
    assertThat(notification.getMessage()).isEqualTo("Move now");
    assertThat(notification.getRound()).isEqualTo(2);
    assertThat(notification.getNotificationId()).hasSize(36);
  }

  @Test
  @DisplayName("Two notifications get different identifiers")
  void notificationIdentifiersAreUnique() {
    Organization organization = SampleEntities.organization("ORG-0001");

    Notification first = new Notification(organization, NotificationChannel.SMS, "a", 1);
    Notification second = new Notification(organization, NotificationChannel.SMS, "a", 1);

    assertThat(first.getNotificationId()).isNotEqualTo(second.getNotificationId());
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  @DisplayName("The seed constructor of Notification keeps a delivered notification as stored")
  void notificationSeedConstructor() {
    Notification notification =
        new Notification(
            RecipientKind.ORGANIZATION,
            "ORG-0001",
            NotificationChannel.SMS,
            "text",
            1,
            DeliveryStatus.DELIVERED,
            NOW);

    assertThat(notification.getRecipient()).isEmpty();
    assertThat(notification.getRecipientKind()).isEqualTo(RecipientKind.ORGANIZATION);
    assertThat(notification.getRecipientId()).isEqualTo("ORG-0001");
    assertThat(notification.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    assertThat(notification.getSentTime()).contains(NOW);
  }

  @Test
  @DisplayName("Notification rejects a null recipient")
  void notificationRejectsNullRecipient() {
    assertThatNullPointerException()
        .isThrownBy(() -> new Notification(null, NotificationChannel.SMS, "t", 1));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Notification(
                    null, "ORG-1", NotificationChannel.SMS, "t", 1, DeliveryStatus.FAILED, null));
  }
}
