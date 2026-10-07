package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.config.MockGatewayProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushKind;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushOutbox;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushOutboxRepository;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Tests of {@link PushNotificationGateway} against H2, with the failure prefix set by property. */
@DataJpaTest(properties = "mock.push.fail-recipient-prefixes=CIT-FAIL-")
@Import({PushNotificationGateway.class, MockGatewayFailureRule.class})
class PushNotificationGatewayTest {

  private static final Instant FIXED = Instant.parse("2026-05-21T03:00:00Z");

  /** A clock fixed at 08:30 in Colombo, and the properties record the failure rule needs. */
  @TestConfiguration
  @EnableConfigurationProperties(MockGatewayProperties.class)
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(FIXED, ZoneId.of("Asia/Colombo"));
    }
  }

  @Autowired private PushNotificationGateway gateway;
  @Autowired private PushOutboxRepository outbox;

  private static Warning warning() {
    Hazard hazard = SampleEntities.hazard("HZ-1");
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    return SampleEntities.warning(
        "WRN-2026-0101",
        hazard,
        officer,
        List.of(District.COLOMBO),
        List.of(),
        SampleEntities.NOW.plusDays(1));
  }

  @Test
  @DisplayName("The push gateway supports PUSH and AUDIBLE_ALERT, not SMS")
  void supports() {
    assertThat(gateway.supports(NotificationChannel.PUSH)).isTrue();
    assertThat(gateway.supports(NotificationChannel.AUDIBLE_ALERT)).isTrue();
    assertThat(gateway.supports(NotificationChannel.SMS)).isFalse();
  }

  @Test
  @DisplayName("send writes a WARNING_ALERT entry from the notification's warning")
  void sendWritesAnAlertEntry() {
    Citizen citizen = SampleEntities.citizen("CIT-COL-0001", "token");
    Notification notification =
        new Notification(citizen, NotificationChannel.PUSH, "Move to higher ground", 1);
    FeatureFixtures.attach(notification, warning());

    assertThat(gateway.send(notification)).isTrue();

    List<PushOutbox> entries = outbox.findUndelivered("CIT-COL-0001");
    assertThat(entries).hasSize(1);
    PushOutbox entry = entries.get(0);
    assertThat(entry.getKind()).isEqualTo(PushKind.WARNING_ALERT);
    assertThat(entry.getWarningId()).contains("WRN-2026-0101");
    assertThat(entry.getWarningLevel()).contains(WarningLevel.LEVEL_2_AMBER);
    assertThat(entry.getText()).isEqualTo("Move to higher ground");
    assertThat(entry.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 8, 30));
    assertThat(entry.getReportId()).isEmpty();
    assertThat(entry.getDeliveredAt()).isEmpty();
  }

  @Test
  @DisplayName("send writes nothing and returns false for a recipient with a listed prefix")
  void sendFailsByPrefix() {
    Citizen citizen = SampleEntities.citizen("CIT-FAIL-0001", "token");
    Notification notification = new Notification(citizen, NotificationChannel.PUSH, "text", 1);
    FeatureFixtures.attach(notification, warning());

    assertThat(gateway.send(notification)).isFalse();
    assertThat(outbox.findUndelivered("CIT-FAIL-0001")).isEmpty();
  }

  @Test
  @DisplayName("send needs the notification to be attached to its warning first")
  void sendNeedsTheWarning() {
    Citizen citizen = SampleEntities.citizen("CIT-COL-0002", "token");
    Notification notification = new Notification(citizen, NotificationChannel.PUSH, "text", 1);

    assertThatIllegalStateException()
        .isThrownBy(() -> gateway.send(notification))
        .withMessageContaining("warning.addNotification");
  }

  @Test
  @DisplayName("sendNotice writes a REPORT_DECISION entry in the recipient's language")
  void sendNoticeWritesADecisionEntry() {
    Citizen citizen = SampleEntities.citizen("CIT-COL-0003", "token"); // Sinhala
    LocalDateTime decidedAt = LocalDateTime.of(2026, 5, 21, 8, 0);
    ReportDecisionNotice notice =
        new ReportDecisionNotice("RPT-9", ReportStatus.REJECTED, "Not a flood", decidedAt);

    assertThat(gateway.sendNotice(citizen, notice)).isTrue();

    PushOutbox entry = outbox.findUndelivered("CIT-COL-0003").get(0);
    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.SINHALA);
    assertThat(entry.getKind()).isEqualTo(PushKind.REPORT_DECISION);
    assertThat(entry.getReportId()).contains("RPT-9");
    assertThat(entry.getStatus()).contains(ReportStatus.REJECTED);
    assertThat(entry.getReason()).contains("Not a flood");
    assertThat(entry.getDecidedAt()).contains(decidedAt);
    assertThat(entry.getText())
        .isEqualTo("ඔබගේ උපද්‍රව වාර්තාව RPT-9 පිළිගනු නොලැබීය: Not a flood");
    assertThat(entry.getWarningId()).isEmpty();
  }

  @Test
  @DisplayName("sendNotice fails for a recipient with a listed prefix and writes nothing")
  void sendNoticeFailsByPrefix() {
    Citizen citizen = SampleEntities.citizen("CIT-FAIL-0002", "token");
    ReportDecisionNotice notice =
        new ReportDecisionNotice(
            "RPT-9", ReportStatus.VERIFIED, null, LocalDateTime.of(2026, 5, 21, 8, 0));

    assertThat(gateway.sendNotice(citizen, notice)).isFalse();
    assertThat(outbox.findUndelivered("CIT-FAIL-0002")).isEmpty();
  }
}
