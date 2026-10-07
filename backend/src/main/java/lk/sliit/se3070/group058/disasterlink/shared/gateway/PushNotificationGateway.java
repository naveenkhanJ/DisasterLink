package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushOutbox;
import lk.sliit.se3070.group058.disasterlink.shared.notification.PushOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The mock of the push notification service. Instead of calling a service it writes an entry to the
 * {@link PushOutbox}, which the app polls and turns into a local notification. It reports a failure
 * for the recipients that {@link MockGatewayFailureRule} names.
 *
 * <p>Implements CH-10 (model dictionary section 4; mock writing to {@code PushOutbox}, section 6).
 */
@Component
public class PushNotificationGateway extends NotificationGateway {

  private static final Logger LOG = LoggerFactory.getLogger(PushNotificationGateway.class);

  private final MockGatewayFailureRule rule;

  private final PushOutboxRepository outbox;

  private final Clock clock;

  /**
   * Creates the mock.
   *
   * @param rule decides which recipients fail
   * @param outbox where the entries are written
   * @param clock supplies the time of each entry
   */
  public PushNotificationGateway(
      MockGatewayFailureRule rule, PushOutboxRepository outbox, Clock clock) {
    this.rule = Objects.requireNonNull(rule, "rule");
    this.outbox = Objects.requireNonNull(outbox, "outbox");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.PUSH || channel == NotificationChannel.AUDIBLE_ALERT;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The warning and its level are read from {@code notification.getWarning()}, so the
   * broadcaster must add the notification to its warning before it calls the gateway (standards
   * section 12.2).
   *
   * @throws IllegalStateException if the notification has not been added to a warning
   */
  @Override
  public boolean send(Notification notification) {
    String recipientId = notification.getRecipientId();
    if (rule.pushFails(recipientId)) {
      LOG.warn("Push to {} failed (mock failure rule)", recipientId);
      return false;
    }
    Warning warning =
        notification
            .getWarning()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "The notification is not attached to a warning: the broadcaster must call"
                            + " warning.addNotification(notification) before the gateway send"));
    outbox.save(
        PushOutbox.forWarningAlert(
            recipientId,
            warning.getWarningId(),
            warning.getWarningLevel(),
            notification.getMessage(),
            LocalDateTime.now(clock)));
    String warningId = warning.getWarningId();
    LOG.info("Push alert for warning {} queued for {}", warningId, recipientId);
    return true;
  }

  @Override
  public boolean sendNotice(Recipient recipient, ReportDecisionNotice notice) {
    String recipientId = recipient.getRecipientId();
    if (rule.pushFails(recipientId)) {
      LOG.warn("Push notice to {} failed (mock failure rule)", recipientId);
      return false;
    }
    String text =
        DecisionNoticeText.render(
            recipient.getPreferredLanguage(), notice.status(), notice.reportId(), notice.reason());
    outbox.save(PushOutbox.forReportDecision(recipientId, notice, text, LocalDateTime.now(clock)));
    String reportId = notice.reportId();
    LOG.info("Push decision for report {} queued for {}", reportId, recipientId);
    return true;
  }
}
