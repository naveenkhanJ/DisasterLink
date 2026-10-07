package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A warning sent to one recipient on one channel in one round.
 *
 * <p>Implements CH-12, CH-18 and CH-21 (A8). The recipient is stored as its kind and identifier
 * (model dictionary section 0.7).
 */
@Entity
public class Notification {

  @Id private String notificationId;

  @Enumerated(EnumType.STRING)
  private NotificationChannel type;

  @Column(length = 2000)
  private String message;

  private LocalDateTime sentTime;

  @Enumerated(EnumType.STRING)
  private DeliveryStatus deliveryStatus;

  private int round;

  @Enumerated(EnumType.STRING)
  private RecipientKind recipientKind;

  private String recipientId;

  @Transient private Recipient recipient;

  // The same column that Warning.notifications writes as its join column, so this side only reads.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warning_id", insertable = false, updatable = false)
  private Warning warning;

  /**
   * Creates a notification to send to a recipient: the drawn message of the broadcast. The delivery
   * status is {@code PENDING} and there is no sent time yet.
   *
   * @param recipient the recipient; it is kept for the gateway call, its kind and identifier are
   *     stored
   * @param channel the channel to send on
   * @param text the text to send, in the recipient's language
   * @param round the broadcast round, 1 for the issue broadcast
   * @throws NullPointerException if an argument is {@code null}
   */
  public Notification(Recipient recipient, NotificationChannel channel, String text, int round) {
    this.notificationId = UUID.randomUUID().toString();
    this.recipient = Objects.requireNonNull(recipient, "recipient");
    this.recipientKind = recipient.getRecipientKind();
    this.recipientId = recipient.getRecipientId();
    this.type = Objects.requireNonNull(channel, "channel");
    this.message = Objects.requireNonNull(text, "text");
    this.round = round;
    this.deliveryStatus = DeliveryStatus.PENDING;
  }

  /**
   * Creates a notification from its stored state. This is the seed's constructor, for stored
   * delivered or failed notifications; it changes no domain rule.
   *
   * @param sentTime the time the delivery was recorded, or {@code null} if it was not
   * @throws NullPointerException if an argument other than {@code sentTime} is {@code null}
   */
  public Notification(
      RecipientKind recipientKind,
      String recipientId,
      NotificationChannel channel,
      String text,
      int round,
      DeliveryStatus deliveryStatus,
      LocalDateTime sentTime) {
    this.notificationId = UUID.randomUUID().toString();
    this.recipientKind = Objects.requireNonNull(recipientKind, "recipientKind");
    this.recipientId = Objects.requireNonNull(recipientId, "recipientId");
    this.type = Objects.requireNonNull(channel, "channel");
    this.message = Objects.requireNonNull(text, "text");
    this.round = round;
    this.deliveryStatus = Objects.requireNonNull(deliveryStatus, "deliveryStatus");
    this.sentTime = sentTime;
  }

  /** Constructor for JPA only. */
  protected Notification() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the notification.
   *
   * @return the identifier of the notification
   */
  public String getNotificationId() {
    return notificationId;
  }

  /**
   * Returns the channel the notification is sent on.
   *
   * @return the channel the notification is sent on
   */
  public NotificationChannel getType() {
    return type;
  }

  /**
   * Returns the text sent, in the recipient's language.
   *
   * @return the text sent, in the recipient's language
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the time the delivery was recorded.
   *
   * @return the time the delivery was recorded, or empty if there is none
   */
  public Optional<LocalDateTime> getSentTime() {
    return Optional.ofNullable(sentTime);
  }

  /**
   * Returns the delivery status of the notification.
   *
   * @return the delivery status of the notification
   */
  public DeliveryStatus getDeliveryStatus() {
    return deliveryStatus;
  }

  /**
   * Returns the broadcast round the notification belongs to; 1 is the issue broadcast.
   *
   * @return the broadcast round the notification belongs to; 1 is the issue broadcast
   */
  public int getRound() {
    return round;
  }

  /**
   * Returns the kind of the recipient (A8 mapping).
   *
   * @return the kind of the recipient (A8 mapping)
   */
  public RecipientKind getRecipientKind() {
    return recipientKind;
  }

  /**
   * Returns the identifier of the recipient (A8 mapping).
   *
   * @return the identifier of the recipient (A8 mapping)
   */
  public String getRecipientId() {
    return recipientId;
  }

  /**
   * Returns the warning this notification belongs to (A2, seen from the notification). The warning
   * is set by {@code Warning.addNotification} and is read back from the stored {@code warning_id}.
   *
   * @return the warning, or empty if the notification has not been added to a warning yet
   */
  public Optional<Warning> getWarning() {
    return Optional.ofNullable(warning);
  }

  /**
   * Sets the warning this notification belongs to. Called by {@code Warning.addNotification},
   * Lakmal's operation; the column is written through the warning's join column, never from here.
   *
   * @param warning the warning that now holds this notification
   * @throws NullPointerException if {@code warning} is {@code null}
   */
  void attachTo(Warning warning) {
    this.warning = Objects.requireNonNull(warning, "warning");
  }

  /**
   * Returns the recipient kept for the gateway call.
   *
   * @return the recipient, or empty if the notification was loaded from the database
   */
  public Optional<Recipient> getRecipient() {
    return Optional.ofNullable(recipient);
  }

  /**
   * Records a successful delivery.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities").
   *
   * @param at when the gateway reported the delivery
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public void markDelivered(LocalDateTime at) {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  /**
   * Records a failed delivery.
   *
   * <p>Placeholder owned by Lakmal: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities").
   *
   * @param at when the gateway reported the failure
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public void markFailed(LocalDateTime at) {
    throw new UnsupportedOperationException("not implemented yet — Lakmal");
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof Notification that
            && Objects.equals(getNotificationId(), that.getNotificationId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getNotificationId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getNotificationId() + "]";
  }
}
