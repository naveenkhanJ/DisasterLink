package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The result of a broadcast or resend, covering the warning's current round.
 *
 * <p>Implements CH-23.
 *
 * @param deliveredCount the number of recipient–channel pairs whose latest notification of the
 *     round is delivered
 * @param failedRecipientIds per channel, the recipients whose latest notification of the round
 *     failed
 * @param unreachableRecipientIds the recipients skipped on a resend because they cannot receive the
 *     chosen channel; empty after a first broadcast
 */
public record BroadcastSummary(
    int deliveredCount,
    Map<NotificationChannel, List<String>> failedRecipientIds,
    List<String> unreachableRecipientIds) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public BroadcastSummary(
      int deliveredCount,
      Map<NotificationChannel, List<String>> failedRecipientIds,
      List<String> unreachableRecipientIds) {
    this.deliveredCount = deliveredCount;
    this.failedRecipientIds = copyOfListMap(failedRecipientIds);
    this.unreachableRecipientIds = List.copyOf(unreachableRecipientIds);
  }

  private static Map<NotificationChannel, List<String>> copyOfListMap(
      Map<NotificationChannel, List<String>> source) {
    return source.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
  }
}
