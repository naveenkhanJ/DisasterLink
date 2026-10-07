package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import java.util.List;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;

/**
 * The result of sharing a report: who now has it, and who has no data in the report's scope.
 *
 * <p>Implements CH-32 (UC4 E5); it lives beside the entity it references.
 *
 * @param report the shared report
 * @param sharedWith the organisations the report was shared with
 * @param noData the organisations that have no data in the report's districts
 */
public record ShareOutcome(
    DisasterReport report, List<Organization> sharedWith, List<Organization> noData) {

  /**
   * Creates the outcome, copying the lists.
   *
   * @throws NullPointerException if an argument is {@code null}
   */
  public ShareOutcome(
      DisasterReport report, List<Organization> sharedWith, List<Organization> noData) {
    this.report = Objects.requireNonNull(report, "report");
    this.sharedWith = List.copyOf(sharedWith);
    this.noData = List.copyOf(noData);
  }
}
