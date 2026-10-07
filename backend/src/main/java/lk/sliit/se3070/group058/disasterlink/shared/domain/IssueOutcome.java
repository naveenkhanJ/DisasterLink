package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Objects;

/**
 * The result of issuing a warning: the warning and the summary of its first broadcast.
 *
 * <p>Implements CH-23.
 *
 * @param warning the issued warning
 * @param summary the summary of the broadcast
 */
public record IssueOutcome(Warning warning, BroadcastSummary summary) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public IssueOutcome(Warning warning, BroadcastSummary summary) {
    this.warning = Objects.requireNonNull(warning, "warning");
    this.summary = Objects.requireNonNull(summary, "summary");
  }
}
