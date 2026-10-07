package lk.sliit.se3070.group058.disasterlink.hazardreport.domain;

import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;

/**
 * The server's reply to a report upload or replay. A rejected report is a reply with the outcome
 * {@code REJECTED}, never an exception.
 *
 * <p>Implements RC-01 (model dictionary section 2); it lives beside the entity it references.
 *
 * @param result the outcome: accepted, duplicate, or rejected with the reason
 * @param report the report that was stored, or {@code null} if none was (a rejected upload)
 */
public record ReportReceipt(SyncResult result, HazardReport report) {

  /**
   * Creates the receipt.
   *
   * @throws NullPointerException if {@code result} is {@code null}
   */
  public ReportReceipt(SyncResult result, HazardReport report) {
    this.result = Objects.requireNonNull(result, "result");
    this.report = report;
  }
}
