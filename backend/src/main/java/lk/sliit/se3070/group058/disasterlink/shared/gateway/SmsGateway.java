package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The mock of the SMS gateway of the mobile operator. It sends nothing: it logs the recipient and
 * the length of the text, never the text, and reports a failure for the recipients that {@link
 * MockGatewayFailureRule} names.
 *
 * <p>Implements CH-10 and CH-09 (model dictionary section 4; mock, section 6).
 */
@Component
public class SmsGateway extends NotificationGateway {

  private static final Logger LOG = LoggerFactory.getLogger(SmsGateway.class);

  private final MockGatewayFailureRule rule;

  /**
   * Creates the mock.
   *
   * @param rule decides which recipients fail
   */
  public SmsGateway(MockGatewayFailureRule rule) {
    this.rule = Objects.requireNonNull(rule, "rule");
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }

  @Override
  public boolean send(Notification notification) {
    String recipientId = notification.getRecipientId();
    return deliver(recipientId, notification.getMessage().length());
  }

  @Override
  public boolean sendNotice(Recipient recipient, ReportDecisionNotice notice) {
    String text =
        DecisionNoticeText.render(
            recipient.getPreferredLanguage(), notice.status(), notice.reportId(), notice.reason());
    return deliver(recipient.getRecipientId(), text.length());
  }

  private boolean deliver(String recipientId, int textLength) {
    if (rule.smsFails(recipientId)) {
      LOG.warn("SMS to {} failed (mock failure rule)", recipientId);
      return false;
    }
    LOG.info("SMS to {} sent, {} characters", recipientId, textLength);
    return true;
  }
}
