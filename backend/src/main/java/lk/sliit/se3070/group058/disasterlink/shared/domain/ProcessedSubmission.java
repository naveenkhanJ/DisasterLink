package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * The record that a client submission was accepted, so that a replay of it is recognised as a
 * duplicate.
 *
 * <p>Implementation-only class of the model dictionary section 6 (K-12): the one idempotency record
 * for both clients.
 */
@Entity
public class ProcessedSubmission {

  @Id private String submissionId;

  private String clientId;

  private LocalDateTime capturedAt;

  private LocalDateTime receivedAt;

  private String resultId;

  /**
   * Creates the record of an accepted submission.
   *
   * @param resultId the identifier of what the submission created, or {@code null} if it created
   *     nothing
   * @throws NullPointerException if an argument other than {@code resultId} is {@code null}
   */
  public ProcessedSubmission(
      String submissionId,
      String clientId,
      LocalDateTime capturedAt,
      LocalDateTime receivedAt,
      String resultId) {
    this.submissionId = Objects.requireNonNull(submissionId, "submissionId");
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
    this.resultId = resultId;
  }

  /** Constructor for JPA only. */
  protected ProcessedSubmission() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the submission.
   *
   * @return the identifier of the submission
   */
  public String getSubmissionId() {
    return submissionId;
  }

  /**
   * Returns the identifier of the client that sent it.
   *
   * @return the identifier of the client that sent it
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Returns the time the item was captured on the client.
   *
   * @return the time the item was captured on the client
   */
  public LocalDateTime getCapturedAt() {
    return capturedAt;
  }

  /**
   * Returns the time the server received it.
   *
   * @return the time the server received it
   */
  public LocalDateTime getReceivedAt() {
    return receivedAt;
  }

  /**
   * Returns the identifier of what the submission created, for example a report.
   *
   * @return the identifier of what the submission created, for example a report, or empty if there
   *     is none
   */
  public Optional<String> getResultId() {
    return Optional.ofNullable(resultId);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof ProcessedSubmission that
            && Objects.equals(getSubmissionId(), that.getSubmissionId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getSubmissionId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getSubmissionId() + "]";
  }
}
