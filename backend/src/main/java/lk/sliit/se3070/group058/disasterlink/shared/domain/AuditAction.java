package lk.sliit.se3070.group058.disasterlink.shared.domain;

/**
 * The kind of action an audit record documents.
 *
 * <p>Implements CH-32 and CH-06.
 */
public enum AuditAction {
  REPORT_GENERATED,
  REPORT_EXPORTED,
  EXPORT_FAILED,
  REPORT_SHARED,
  WARNING_ESCALATED
}
