package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * The criteria of a disaster-response report.
 *
 * <p>Implements CH-17, CH-18 and CH-31.
 *
 * @param districts the districts the report covers
 * @param hazardType the hazard type to report on, or {@code null} for every type
 * @param periodStart the first day of the period
 * @param periodEnd the last day of the period
 * @param metrics the metrics the report contains
 * @param hazardId the closed hazard used to pre-fill the criteria, or {@code null} if none
 */
public record ReportCriteria(
    Set<District> districts,
    HazardType hazardType,
    LocalDate periodStart,
    LocalDate periodEnd,
    Set<ReportMetric> metrics,
    String hazardId) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ReportCriteria(
      Set<District> districts,
      HazardType hazardType,
      LocalDate periodStart,
      LocalDate periodEnd,
      Set<ReportMetric> metrics,
      String hazardId) {
    this.districts = Set.copyOf(districts);
    this.hazardType = hazardType;
    this.periodStart = Objects.requireNonNull(periodStart, "periodStart");
    this.periodEnd = Objects.requireNonNull(periodEnd, "periodEnd");
    this.metrics = Set.copyOf(metrics);
    this.hazardId = hazardId;
  }
}
