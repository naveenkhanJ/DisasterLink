package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDate;
import java.util.Set;

/**
 * The criteria of a disaster-response report.
 *
 * <p>Implements CH-17, CH-18 and CH-31.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the officer has not entered is
 * {@code null}, so that {@code AnalyticsController.validateCriteria} reports it. The finders that
 * take criteria are called only with criteria that passed that check.
 *
 * @param districts the districts the report covers, empty if none selected
 * @param hazardType the hazard type to report on, or {@code null} for every type
 * @param periodStart the first day of the period, or {@code null} if not entered — checked by
 *     {@code AnalyticsController.validateCriteria}
 * @param periodEnd the last day of the period, or {@code null} if not entered — checked by {@code
 *     AnalyticsController.validateCriteria}
 * @param metrics the metrics the report contains, empty if none selected
 * @param hazardId the closed hazard used to pre-fill the criteria, or {@code null} if none
 */
public record ReportCriteria(
    Set<District> districts,
    HazardType hazardType,
    LocalDate periodStart,
    LocalDate periodEnd,
    Set<ReportMetric> metrics,
    String hazardId) {

  /** Creates the record, replacing a {@code null} collection by an empty one and copying it. */
  public ReportCriteria(
      Set<District> districts,
      HazardType hazardType,
      LocalDate periodStart,
      LocalDate periodEnd,
      Set<ReportMetric> metrics,
      String hazardId) {
    this.districts = districts == null ? Set.of() : Set.copyOf(districts);
    this.hazardType = hazardType;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
    this.metrics = metrics == null ? Set.of() : Set.copyOf(metrics);
    this.hazardId = hazardId;
  }
}
