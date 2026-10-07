package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;

/**
 * The interface to an external notification service, one subclass per service (Strategy): push and
 * SMS. A gateway handles only the channels it {@link #supports}; the choice between push and SMS
 * for a decision notice belongs to {@code ReportVerificationController}.
 *
 * <p>Implements CH-10 (Strategy) and CH-26 (model dictionary section 4). External push and SMS
 * services are mocked behind this abstraction (dependency inversion).
 */
public abstract class NotificationGateway {

  /**
   * Tells whether this gateway sends on a channel.
   *
   * @param channel the channel
   * @return {@code true} if this gateway handles it
   */
  public abstract boolean supports(NotificationChannel channel);

  /**
   * Sends a notification to its recipient.
   *
   * @param notification the notification; it reaches the recipient it was made for
   * @return {@code true} if the service accepted it, {@code false} if the send failed
   */
  public abstract boolean send(Notification notification);

  /**
   * Sends a report decision notice on this gateway's channel, in the recipient's preferred
   * language.
   *
   * @param recipient the citizen the notice is for
   * @param notice the decision
   * @return {@code true} if the service accepted it, {@code false} if the send failed
   */
  public abstract boolean sendNotice(Recipient recipient, ReportDecisionNotice notice);
}
