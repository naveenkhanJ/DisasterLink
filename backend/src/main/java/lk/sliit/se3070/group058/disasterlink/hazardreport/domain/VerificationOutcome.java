package lk.sliit.se3070.group058.disasterlink.hazardreport.domain;

import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;

/**
 * The result of verifying a report: the report and the active warning of its hazard, which drives
 * the escalation prompt.
 *
 * <p>Implements CH-06 and CH-25; it lives beside the entity it references.
 *
 * @param report the verified report
 * @param activeWarning the active warning of the report's hazard, or {@code null} if it has none
 */
public record VerificationOutcome(HazardReport report, Warning activeWarning) {

  /**
   * Creates the outcome.
   *
   * @throws NullPointerException if {@code report} is {@code null}
   */
  public VerificationOutcome(HazardReport report, Warning activeWarning) {
    this.report = Objects.requireNonNull(report, "report");
    this.activeWarning = activeWarning;
  }
}
