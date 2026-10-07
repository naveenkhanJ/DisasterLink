package lk.sliit.se3070.group058.dews.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The notice that tells a citizen how a report was decided.
 *
 * <p>Implements CH-26.
 *
 * @param reportId the identifier of the decided report
 * @param status the decision, verified or rejected
 * @param reason the reason the officer gave, or {@code null} if the report was verified
 * @param decidedAt when the decision was made
 */
public record ReportDecisionNotice(
    String reportId, ReportStatus status, String reason, LocalDateTime decidedAt) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ReportDecisionNotice(
      String reportId, ReportStatus status, String reason, LocalDateTime decidedAt) {
    this.reportId = Objects.requireNonNull(reportId, "reportId");
    this.status = Objects.requireNonNull(status, "status");
    this.reason = reason;
    this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
  }
}
