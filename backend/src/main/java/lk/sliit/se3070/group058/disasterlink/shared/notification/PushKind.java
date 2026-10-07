package lk.sliit.se3070.group058.disasterlink.shared.notification;

/**
 * The kind of a {@link PushOutbox} entry. The app turns a warning alert into {@code receiveAlert}
 * and a report decision into {@code receiveReportDecision}.
 *
 * <p>Implementation-only enumeration of the model dictionary section 1 (V2C-04, FE-11).
 */
public enum PushKind {
  WARNING_ALERT,
  REPORT_DECISION
}
