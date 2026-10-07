package lk.sliit.se3070.group058.dews.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * What a client adds to every write: its identity, the acting user and the idempotency key.
 *
 * <p>Implements CH-05 and CH-31.
 *
 * @param clientId the identifier of the client device or browser
 * @param userId the signed-in user who acts, the reporter or the officer
 * @param submissionId the identifier that makes the write idempotent
 * @param capturedAt when the item was captured on the client
 * @param pendingCount the number of queued items on the client other than the one being sent
 */
public record ClientMeta(
    String clientId,
    String userId,
    String submissionId,
    LocalDateTime capturedAt,
    int pendingCount) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ClientMeta(
      String clientId,
      String userId,
      String submissionId,
      LocalDateTime capturedAt,
      int pendingCount) {
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.submissionId = Objects.requireNonNull(submissionId, "submissionId");
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    this.pendingCount = pendingCount;
  }
}
