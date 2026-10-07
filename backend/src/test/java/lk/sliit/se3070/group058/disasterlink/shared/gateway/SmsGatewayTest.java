package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.config.MockGatewayProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Tests of {@link SmsGateway}. */
@ExtendWith(OutputCaptureExtension.class)
class SmsGatewayTest {

  private static final Citizen CITIZEN = SampleEntities.citizen("CIT-COL-0001", null);
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 8, 0);

  private final MockGatewayFailureRule rule =
      new MockGatewayFailureRule(
          new MockGatewayProperties(new MockGatewayProperties.Sms(List.of("CIT-FAIL-")), null));

  private final SmsGateway gateway = new SmsGateway(rule);

  @Test
  @DisplayName("The SMS gateway supports SMS only")
  void supportsSmsOnly() {
    assertThat(gateway.supports(NotificationChannel.SMS)).isTrue();
    assertThat(gateway.supports(NotificationChannel.PUSH)).isFalse();
    assertThat(gateway.supports(NotificationChannel.AUDIBLE_ALERT)).isFalse();
  }

  @Test
  @DisplayName("send succeeds and logs the recipient and the text length, never the text")
  void sendSucceeds(CapturedOutput output) {
    Notification notification =
        new Notification(CITIZEN, NotificationChannel.SMS, "Secret warning text", 1);

    assertThat(gateway.send(notification)).isTrue();

    assertThat(output.getOut()).contains("SMS to CIT-COL-0001 sent, 19 characters");
    assertThat(output.getOut()).doesNotContain("Secret warning text");
  }

  @Test
  @DisplayName("send fails for a recipient with a listed prefix")
  void sendFailsByPrefix() {
    Citizen failing = SampleEntities.citizen("CIT-FAIL-0001", null);
    Notification notification = new Notification(failing, NotificationChannel.SMS, "text", 1);

    assertThat(gateway.send(notification)).isFalse();

    rule.clearSmsPrefixes();
    assertThat(gateway.send(notification)).isTrue();
  }

  @Test
  @DisplayName("sendNotice succeeds, logs the length of the rendered text, and fails by prefix")
  void sendNotice(CapturedOutput output) {
    ReportDecisionNotice verified =
        new ReportDecisionNotice("RPT-1", ReportStatus.VERIFIED, null, NOW);
    String text =
        DecisionNoticeText.render(
            CITIZEN.getPreferredLanguage(), ReportStatus.VERIFIED, "RPT-1", null);

    assertThat(gateway.sendNotice(CITIZEN, verified)).isTrue();
    assertThat(output.getOut())
        .contains("SMS to CIT-COL-0001 sent, " + text.length() + " characters");
    assertThat(gateway.sendNotice(SampleEntities.citizen("CIT-FAIL-0002", null), verified))
        .isFalse();
  }
}
