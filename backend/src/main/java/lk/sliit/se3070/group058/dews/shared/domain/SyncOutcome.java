package lk.sliit.se3070.group058.dews.shared.domain;

/**
 * The outcome of sending or replaying a submission.
 *
 * <p>Implements CH-05, CH-26 and CH-28.
 */
public enum SyncOutcome {
  ACCEPTED,
  DUPLICATE,
  REJECTED
}
