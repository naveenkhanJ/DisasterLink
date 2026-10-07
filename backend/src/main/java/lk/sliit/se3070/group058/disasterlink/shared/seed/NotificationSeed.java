package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;

/**
 * Builds the stored notifications of one broadcast round the way the broadcaster will: for each
 * recipient and each channel of the warning that the recipient can receive, one notification in the
 * recipient's preferred language (push and audible alert carry the message, an SMS the message
 * followed by the instructions). Citizens with the app get push, audible alert and SMS; citizens
 * without the app, organisations and district officers get SMS only (SC1 step 14).
 */
final class NotificationSeed {

  /** The channels of the seeded warnings, in the order SC1 step 10 names them. */
  private static final List<NotificationChannel> CHANNELS =
      List.of(NotificationChannel.PUSH, NotificationChannel.AUDIBLE_ALERT, NotificationChannel.SMS);

  /** Every this-many-th Kalutara citizen has a failed SMS (deterministic stand-in for SC1 E1). */
  private static final int FAILED_SMS_EVERY = 20;

  /**
   * Minutes after the broadcast at which a failed SMS is sent again and delivered (SC1 E1 16d-16e).
   */
  private static final int RESEND_AFTER_MINUTES = 25;

  /** How many notifications share one second of sent time. */
  private static final int PER_SECOND = 10;

  private NotificationSeed() {
    throw new AssertionError("NotificationSeed is a utility class");
  }

  /**
   * Builds one round of notifications.
   *
   * @param recipients the recipients of the round
   * @param round the broadcast round, 1 for the issue
   * @param sentAt when the round was broadcast
   * @param message the message of the round
   * @param instructions the instructions of the round
   * @param failSomeKalutaraSms {@code true} to fail every 20th Kalutara citizen's SMS and resend it
   * @return the notifications, in the order they were sent
   */
  static List<Notification> round(
      List<? extends Recipient> recipients,
      int round,
      LocalDateTime sentAt,
      LocalisedText message,
      LocalisedText instructions,
      boolean failSomeKalutaraSms) {
    List<Notification> notifications = new ArrayList<>();
    List<Notification> resends = new ArrayList<>();
    for (Recipient recipient : recipients) {
      for (NotificationChannel channel : CHANNELS) {
        if (!recipient.canReceive(channel)) {
          continue;
        }
        String text = text(recipient, channel, message, instructions);
        LocalDateTime sent = sentAt.plusSeconds(notifications.size() / PER_SECOND);
        boolean fails =
            failSomeKalutaraSms && channel == NotificationChannel.SMS && smsFails(recipient);
        notifications.add(
            notification(
                recipient,
                channel,
                text,
                round,
                fails ? DeliveryStatus.FAILED : DeliveryStatus.DELIVERED,
                sent));
        if (fails) {
          resends.add(
              notification(
                  recipient,
                  channel,
                  text,
                  round,
                  DeliveryStatus.DELIVERED,
                  sentAt.plusMinutes(RESEND_AFTER_MINUTES)));
        }
      }
    }
    notifications.addAll(resends);
    return notifications;
  }

  private static Notification notification(
      Recipient recipient,
      NotificationChannel channel,
      String text,
      int round,
      DeliveryStatus status,
      LocalDateTime sent) {
    return new Notification(
        recipient.getRecipientKind(),
        recipient.getRecipientId(),
        channel,
        text,
        round,
        status,
        sent);
  }

  private static String text(
      Recipient recipient,
      NotificationChannel channel,
      LocalisedText message,
      LocalisedText instructions) {
    String body = message.textIn(recipient.getPreferredLanguage());
    if (channel == NotificationChannel.SMS) {
      return body
          + Notification.SMS_TEXT_SEPARATOR
          + instructions.textIn(recipient.getPreferredLanguage());
    }
    return body;
  }

  private static boolean smsFails(Recipient recipient) {
    if (recipient instanceof Citizen citizen && citizen.getDistrict() == District.KALUTARA) {
      String id = citizen.getUserId();
      return Integer.parseInt(id.substring(id.length() - 4)) % FAILED_SMS_EVERY == 0;
    }
    return false;
  }
}
