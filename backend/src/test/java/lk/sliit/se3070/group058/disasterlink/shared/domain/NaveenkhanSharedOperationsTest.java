package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Real tests of Naveenkhan's operation on shared entities: {@link
 * ReportResults#merge(ReportResults)}.
 *
 * <p>Implements CH-31 and CH-32; satisfies implementation standards section 3 and P155 X4.
 */
class NaveenkhanSharedOperationsTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

  @Test
  @DisplayName("ReportResults.merge rejects null other results")
  void mergeRejectsNull() {
    ReportResults results =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());

    assertThatNullPointerException().isThrownBy(() -> results.merge(null));
  }

  @Test
  @DisplayName("ReportResults.merge sums matchedRecordCount and combines metric fields")
  void mergeCombinesResultsCorrectly() {
    AlertTimelineEntry timelineEntry =
        new AlertTimelineEntry(
            "WRN-1",
            NOW,
            WarningLevel.LEVEL_2_AMBER,
            Set.of(District.RATNAPURA),
            TimelineEvent.ISSUED);
    ReportResults timelineResults =
        new ReportResults(
            1, List.of(timelineEntry), Map.of(), 0, List.of(), 0, List.of(), Set.of());

    ReportResults reachResults =
        new ReportResults(
            100,
            List.of(),
            Map.of(NotificationChannel.PUSH, 50, NotificationChannel.SMS, 80),
            80,
            List.of(),
            0,
            List.of(),
            Set.of());

    OccupancyPoint occPoint = new OccupancyPoint("SHL-1", District.RATNAPURA, NOW, 120, 200);
    ReportResults occupancyResults =
        new ReportResults(
            10,
            List.of(),
            Map.of(),
            0,
            List.of(occPoint),
            200,
            List.of(),
            Set.of(ReportMetric.RESOURCE_DISTRIBUTION));

    DistributionLine distLine =
        new DistributionLine(
            District.RATNAPURA, "ORG-1", "CARE", SupplyCategory.FOOD, "packs", 100.0);
    ReportResults distResults =
        new ReportResults(2, List.of(), Map.of(), 0, List.of(), 0, List.of(distLine), Set.of());

    ReportResults merged =
        timelineResults.merge(reachResults).merge(occupancyResults).merge(distResults);

    assertThat(merged.matchedRecordCount()).isEqualTo(113);
    assertThat(merged.alertTimeline()).containsExactly(timelineEntry);
    assertThat(merged.citizensReachedByChannel())
        .containsEntry(NotificationChannel.PUSH, 50)
        .containsEntry(NotificationChannel.SMS, 80);
    assertThat(merged.citizensReachedTotal()).isEqualTo(80);
    assertThat(merged.occupancyOverTime()).containsExactly(occPoint);
    assertThat(merged.combinedCapacity()).isEqualTo(200);
    assertThat(merged.distributionLines()).containsExactly(distLine);
    assertThat(merged.failedMetrics()).containsExactly(ReportMetric.RESOURCE_DISTRIBUTION);
  }

  @Test
  @DisplayName("ReportResults.merge with empty results preserves existing values")
  void mergeWithEmptyPreservesValues() {
    ReportResults populated =
        new ReportResults(
            5,
            List.of(),
            Map.of(NotificationChannel.SMS, 25),
            25,
            List.of(),
            150,
            List.of(),
            Set.of(ReportMetric.ALERT_TIMELINE));
    ReportResults empty =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());

    ReportResults result1 = populated.merge(empty);
    ReportResults result2 = empty.merge(populated);

    assertThat(result1.matchedRecordCount()).isEqualTo(5);
    assertThat(result1.citizensReachedTotal()).isEqualTo(25);
    assertThat(result1.combinedCapacity()).isEqualTo(150);
    assertThat(result1.failedMetrics()).containsExactly(ReportMetric.ALERT_TIMELINE);

    assertThat(result2.matchedRecordCount()).isEqualTo(5);
    assertThat(result2.citizensReachedTotal()).isEqualTo(25);
    assertThat(result2.combinedCapacity()).isEqualTo(150);
    assertThat(result2.failedMetrics()).containsExactly(ReportMetric.ALERT_TIMELINE);
  }
}
