package lk.sliit.se3070.group058.disasterlink.analytics.service;

import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;

/**
 * Strategy interface for computing a disaster-response report metric.
 *
 * <p>Implements CH-20 (Strategy pattern, model dictionary section 4 and section 6).
 */
public interface MetricCalculator {

  /**
   * Returns whether this calculator supports the given report metric.
   *
   * @param metric the report metric to check
   * @return {@code true} if this calculator supports the metric, {@code false} otherwise
   */
  boolean supports(ReportMetric metric);

  /**
   * Calculates the partial results for the supported metric based on the provided report criteria.
   *
   * @param criteria the report criteria defining scope, period and hazard filters
   * @return a partial {@link ReportResults} holding the computed metric
   */
  ReportResults calculate(ReportCriteria criteria);
}
