package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A record of an action an officer performed, such as generating, exporting or sharing a report or
 * escalating a warning.
 *
 * <p>Implements CH-32 and CH-06 (A25).
 */
@Entity
public class AuditRecord {

  @Id private String auditId;

  @Enumerated(EnumType.STRING)
  private AuditAction action;

  private LocalDateTime performedAt;

  private String subjectId;

  @Column(length = ColumnLengths.LONG_TEXT)
  private String detail;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "performed_by_id")
  private User performedBy;

  /**
   * Creates an audit record.
   *
   * @param action the action that was performed
   * @param officer the user who performed it
   * @param subjectId the identifier of the subject, for example the warning that was escalated
   * @param detail more detail, or {@code null} if there is none
   * @param performedAt when the action was performed
   * @throws NullPointerException if an argument other than {@code detail} is {@code null}
   */
  public AuditRecord(
      AuditAction action,
      User officer,
      String subjectId,
      String detail,
      LocalDateTime performedAt) {
    this.auditId = UUID.randomUUID().toString();
    this.action = Objects.requireNonNull(action, "action");
    this.performedBy = Objects.requireNonNull(officer, "officer");
    this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
    this.detail = detail;
    this.performedAt = Objects.requireNonNull(performedAt, "performedAt");
  }

  /** Constructor for JPA only. */
  protected AuditRecord() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the record.
   *
   * @return the identifier of the record
   */
  public String getAuditId() {
    return auditId;
  }

  /**
   * Returns the action that was performed.
   *
   * @return the action that was performed
   */
  public AuditAction getAction() {
    return action;
  }

  /**
   * Returns the time the action was performed.
   *
   * @return the time the action was performed
   */
  public LocalDateTime getPerformedAt() {
    return performedAt;
  }

  /**
   * Returns the identifier of the subject the action was performed on.
   *
   * @return the identifier of the subject the action was performed on
   */
  public String getSubjectId() {
    return subjectId;
  }

  /**
   * Returns the detail of the action.
   *
   * @return the detail of the action, or empty if there is none
   */
  public Optional<String> getDetail() {
    return Optional.ofNullable(detail);
  }

  /**
   * Returns the user who performed the action (A25).
   *
   * @return the user who performed the action (A25)
   */
  public User getPerformedBy() {
    return performedBy;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof AuditRecord that && Objects.equals(getAuditId(), that.getAuditId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getAuditId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getAuditId() + "]";
  }
}
