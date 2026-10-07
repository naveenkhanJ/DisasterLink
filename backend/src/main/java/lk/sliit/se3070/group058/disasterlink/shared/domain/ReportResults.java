package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The computed results of a disaster-response report.
 *
 * <p>Implements CH-20 and CH-33.
 *
 * @param matchedRecordCount the number of records retrieved across the selected metrics
 * @param alertTimeline the entries of the alert timeline
 * @param citizensReachedByChannel the citizens reached per channel
 * @param citizensReachedTotal the citizens reached in total
 * @param occupancyOverTime the occupancy points over time
 * @param combinedCapacity the sum of the current capacities of the matching shelters
 * @param distributionLines the lines of the resource distribution
 * @param failedMetrics the metrics that could not be computed
 */
public record ReportResults(
    int matchedRecordCount,
    List<AlertTimelineEntry> alertTimeline,
    Map<NotificationChannel, Integer> citizensReachedByChannel,
    int citizensReachedTotal,
    List<OccupancyPoint> occupancyOverTime,
    int combinedCapacity,
    List<DistributionLine> distributionLines,
    Set<ReportMetric> failedMetrics) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ReportResults(
      int matchedRecordCount,
      List<AlertTimelineEntry> alertTimeline,
      Map<NotificationChannel, Integer> citizensReachedByChannel,
      int citizensReachedTotal,
      List<OccupancyPoint> occupancyOverTime,
      int combinedCapacity,
      List<DistributionLine> distributionLines,
      Set<ReportMetric> failedMetrics) {
    this.matchedRecordCount = matchedRecordCount;
    this.alertTimeline = List.copyOf(alertTimeline);
    this.citizensReachedByChannel = Map.copyOf(citizensReachedByChannel);
    this.citizensReachedTotal = citizensReachedTotal;
    this.occupancyOverTime = List.copyOf(occupancyOverTime);
    this.combinedCapacity = combinedCapacity;
    this.distributionLines = List.copyOf(distributionLines);
    this.failedMetrics = Set.copyOf(failedMetrics);
  }

  /**
   * Merges these results with the results of another calculator into a new value.
   *
   * <p>Placeholder owned by Naveenkhan: the body is written on his branch (implementation standards
   * section 3, "Operations on shared entities"). The finished operation sums {@code
   * matchedRecordCount}, because each record is counted by one calculator only.
   *
   * @param other the results to merge with these
   * @return a new value that combines both
   * @throws UnsupportedOperationException always, until the operation is implemented
   */
  public ReportResults merge(ReportResults other) {
    throw new UnsupportedOperationException("not implemented yet — Naveenkhan");
  }
}
