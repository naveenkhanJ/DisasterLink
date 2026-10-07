package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * A shared report without its results.
 *
 * <p>Implements CH-33.
 *
 * @param reportId the identifier of the report
 * @param reportTitle the title of the report
 * @param generatedDate when the report was generated
 * @param criteria the criteria of the report
 * @param provisionalSince per district, since when its data is provisional
 */
public record ReportHeader(
    String reportId,
    String reportTitle,
    LocalDateTime generatedDate,
    ReportCriteria criteria,
    Map<District, LocalDateTime> provisionalSince) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ReportHeader(
      String reportId,
      String reportTitle,
      LocalDateTime generatedDate,
      ReportCriteria criteria,
      Map<District, LocalDateTime> provisionalSince) {
    this.reportId = Objects.requireNonNull(reportId, "reportId");
    this.reportTitle = Objects.requireNonNull(reportTitle, "reportTitle");
    this.generatedDate = Objects.requireNonNull(generatedDate, "generatedDate");
    this.criteria = Objects.requireNonNull(criteria, "criteria");
    this.provisionalSince = Map.copyOf(provisionalSince);
  }
}
