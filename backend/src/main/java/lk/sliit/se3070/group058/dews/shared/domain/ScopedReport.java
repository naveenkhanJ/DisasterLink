package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.Objects;

/**
 * A report header with the results already limited to one organisation's districts.
 *
 * <p>Implements CH-33.
 *
 * @param header the report header
 * @param results the results limited to the organisation's scope
 */
public record ScopedReport(ReportHeader header, ReportResults results) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ScopedReport(ReportHeader header, ReportResults results) {
    this.header = Objects.requireNonNull(header, "header");
    this.results = Objects.requireNonNull(results, "results");
  }
}
