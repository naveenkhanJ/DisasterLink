package lk.sliit.se3070.group058.disasterlink.shared.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportDecisionNotice;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;

/**
 * One entry of the push mock's outbox: a message the app will pick up when it polls. The mock push
 * gateway writes a {@code WARNING_ALERT} entry for each push or audible alert and a {@code
 * REPORT_DECISION} entry for each decision notice; the app polls for its undelivered entries and
 * acknowledges the ones it has shown.
 *
 * <p>Implementation-only entity of the model dictionary section 6 (FE-11, V2C-04; P62 FX-13,
 * R4G-19).
 */
@Entity
public class PushOutbox {

  @Id private String id;

  private String recipientId;

  @Enumerated(EnumType.STRING)
  private PushKind kind;

  private String warningId;

  @Enumerated(EnumType.STRING)
  private WarningLevel warningLevel;

  private String reportId;

  @Enumerated(EnumType.STRING)
  private ReportStatus status;

  @Column(length = 2000)
  private String reason;

  @Column(length = 2000)
  private String text;

  private LocalDateTime createdAt;

  private LocalDateTime decidedAt;

  private LocalDateTime deliveredAt;

  private PushOutbox(String recipientId, PushKind kind, String text, LocalDateTime createdAt) {
    this.id = UUID.randomUUID().toString();
    this.recipientId = Objects.requireNonNull(recipientId, "recipientId");
    this.kind = kind;
    this.text = Objects.requireNonNull(text, "text");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /** Constructor for JPA only. */
  protected PushOutbox() {
    // Constructor for JPA only.
  }

  /**
   * Makes the entry for a warning alert.
   *
   * @param recipientId the identifier of the recipient
   * @param warningId the identifier of the warning
   * @param warningLevel the level of the warning
   * @param text the message in the recipient's language
   * @param createdAt when the entry was written
   * @return the new, undelivered entry
   * @throws NullPointerException if an argument is {@code null}
   */
  public static PushOutbox forWarningAlert(
      String recipientId,
      String warningId,
      WarningLevel warningLevel,
      String text,
      LocalDateTime createdAt) {
    PushOutbox entry = new PushOutbox(recipientId, PushKind.WARNING_ALERT, text, createdAt);
    entry.warningId = Objects.requireNonNull(warningId, "warningId");
    entry.warningLevel = Objects.requireNonNull(warningLevel, "warningLevel");
    return entry;
  }

  /**
   * Makes the entry for a report decision notice.
   *
   * @param recipientId the identifier of the recipient
   * @param notice the decision
   * @param text the notice text in the recipient's language
   * @param createdAt when the entry was written
   * @return the new, undelivered entry
   * @throws NullPointerException if an argument is {@code null}
   */
  public static PushOutbox forReportDecision(
      String recipientId, ReportDecisionNotice notice, String text, LocalDateTime createdAt) {
    Objects.requireNonNull(notice, "notice");
    PushOutbox entry = new PushOutbox(recipientId, PushKind.REPORT_DECISION, text, createdAt);
    entry.reportId = notice.reportId();
    entry.status = notice.status();
    entry.reason = notice.reason();
    entry.decidedAt = notice.decidedAt();
    return entry;
  }

  /**
   * Marks the entry as shown by the app. The first time is kept: marking it again changes nothing.
   *
   * @param at when the app acknowledged the entry
   * @throws NullPointerException if {@code at} is {@code null}
   */
  public void markDelivered(LocalDateTime at) {
    Objects.requireNonNull(at, "at");
    if (deliveredAt == null) {
      deliveredAt = at;
    }
  }

  /**
   * Returns the identifier of the entry.
   *
   * @return the identifier, a UUID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the identifier of the recipient.
   *
   * @return the recipient identifier
   */
  public String getRecipientId() {
    return recipientId;
  }

  /**
   * Returns the kind of the entry.
   *
   * @return the kind
   */
  public PushKind getKind() {
    return kind;
  }

  /**
   * Returns the identifier of the warning of an alert.
   *
   * @return the warning identifier, or empty for a decision entry
   */
  public Optional<String> getWarningId() {
    return Optional.ofNullable(warningId);
  }

  /**
   * Returns the level of the warning of an alert.
   *
   * @return the warning level, or empty for a decision entry
   */
  public Optional<WarningLevel> getWarningLevel() {
    return Optional.ofNullable(warningLevel);
  }

  /**
   * Returns the identifier of the report of a decision.
   *
   * @return the report identifier, or empty for an alert entry
   */
  public Optional<String> getReportId() {
    return Optional.ofNullable(reportId);
  }

  /**
   * Returns the decision of a decision entry.
   *
   * @return the status, or empty for an alert entry
   */
  public Optional<ReportStatus> getStatus() {
    return Optional.ofNullable(status);
  }

  /**
   * Returns the rejection reason of a decision entry.
   *
   * @return the reason, or empty if there is none
   */
  public Optional<String> getReason() {
    return Optional.ofNullable(reason);
  }

  /**
   * Returns the text of the entry.
   *
   * @return the text in the recipient's language
   */
  public String getText() {
    return text;
  }

  /**
   * Returns when the entry was written.
   *
   * @return the creation time
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the decision time of a decision entry.
   *
   * @return the time the officer decided, or empty for an alert entry
   */
  public Optional<LocalDateTime> getDecidedAt() {
    return Optional.ofNullable(decidedAt);
  }

  /**
   * Returns when the app acknowledged the entry.
   *
   * @return the delivery time, or empty while the entry is undelivered
   */
  public Optional<LocalDateTime> getDeliveredAt() {
    return Optional.ofNullable(deliveredAt);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof PushOutbox that && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
