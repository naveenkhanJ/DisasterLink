package lk.sliit.se3070.group058.disasterlink.shared.domain;

/**
 * A channel through which a warning notification is sent; an audible alert is a high-priority push.
 *
 * <p>Implements CH-21 and CH-10.
 */
public enum NotificationChannel {
  PUSH,
  SMS,
  AUDIBLE_ALERT
}
