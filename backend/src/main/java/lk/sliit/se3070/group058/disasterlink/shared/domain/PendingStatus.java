package lk.sliit.se3070.group058.disasterlink.shared.domain;

/**
 * The status of an item in a client store: queued for synchronisation, or rejected by the server on
 * replay.
 *
 * <p>Implements CH-21 and CH-26.
 */
public enum PendingStatus {
  QUEUED,
  REJECTED
}
