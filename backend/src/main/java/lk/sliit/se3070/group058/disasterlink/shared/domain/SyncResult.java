package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Objects;

/**
 * The result of sending or replaying one submission.
 *
 * <p>Implements CH-05 and CH-26.
 *
 * @param submissionId the identifier of the submission
 * @param outcome the outcome
 * @param reason the reason, set only when the outcome is rejected, otherwise {@code null}
 * @param message a message for the user, or {@code null} if there is none
 */
public record SyncResult(
    String submissionId, SyncOutcome outcome, RejectionReason reason, String message) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public SyncResult(
      String submissionId, SyncOutcome outcome, RejectionReason reason, String message) {
    this.submissionId = Objects.requireNonNull(submissionId, "submissionId");
    this.outcome = Objects.requireNonNull(outcome, "outcome");
    this.reason = reason;
    this.message = message;
  }
}
