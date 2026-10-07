package lk.sliit.se3070.group058.dews.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests that records holding collections copy them, so a caller cannot change them later. */
class DefensiveCopyTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 8, 15);
  private static final LocalisedText TEXT = new LocalisedText("si", "ta", "en");

  @Test
  @DisplayName("WarningDetails copies its lists and sets")
  void warningDetailsCopiesCollections() {
    List<District> districts = new ArrayList<>(List.of(District.COLOMBO));
    Set<NotificationChannel> channels = EnumSet.of(NotificationChannel.SMS);

    WarningDetails details =
        new WarningDetails(
            WarningLevel.LEVEL_2_AMBER,
            TEXT,
            TEXT,
            districts,
            List.of(RiverBasin.KELANI),
            channels,
            NOW);
    districts.add(District.GALLE);
    channels.add(NotificationChannel.PUSH);

    assertThat(details.targetDistricts()).containsExactly(District.COLOMBO);
    assertThat(details.channels()).containsExactly(NotificationChannel.SMS);
    assertThatThrownBy(() -> details.targetDistricts().add(District.GALLE))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("WarningPreview copies its set and its map")
  void warningPreviewCopiesCollections() {
    Set<District> covered = EnumSet.of(District.COLOMBO);
    Map<District, Integer> citizens = new HashMap<>(Map.of(District.COLOMBO, 10));

    WarningPreview preview =
        new WarningPreview(
            HazardType.FLOOD,
            WarningLevel.LEVEL_2_AMBER,
            covered,
            TEXT,
            TEXT,
            Set.of(NotificationChannel.PUSH),
            NOW,
            citizens,
            3,
            2);
    covered.add(District.GALLE);
    citizens.put(District.GALLE, 5);

    assertThat(preview.coveredDistricts()).containsExactly(District.COLOMBO);
    assertThat(preview.citizensPerDistrict()).containsOnlyKeys(District.COLOMBO);
  }

  @Test
  @DisplayName("BroadcastSummary copies its map and the lists inside it")
  void broadcastSummaryCopiesDeeply() {
    List<String> failed = new ArrayList<>(List.of("CIT-COL-0001"));
    Map<NotificationChannel, List<String>> failures = new HashMap<>();
    failures.put(NotificationChannel.SMS, failed);
    List<String> unreachable = new ArrayList<>(List.of("ORG-0001"));

    final BroadcastSummary summary = new BroadcastSummary(5, failures, unreachable);
    failed.add("CIT-COL-0002");
    failures.clear();
    unreachable.clear();

    assertThat(summary.failedRecipientIds().get(NotificationChannel.SMS))
        .containsExactly("CIT-COL-0001");
    assertThat(summary.unreachableRecipientIds()).containsExactly("ORG-0001");
    assertThatThrownBy(() -> summary.failedRecipientIds().get(NotificationChannel.SMS).add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("HazardUpdate, NewHazardDetails and ValidationResult copy their lists")
  void listRecordsCopyTheirLists() {
    List<District> districts = new ArrayList<>(List.of(District.KALUTARA));
    List<String> errors = new ArrayList<>(List.of("photo: missing"));

    final HazardUpdate update = new HazardUpdate("d", HazardSeverity.HIGH, districts);
    final NewHazardDetails details =
        new NewHazardDetails(HazardSeverity.LOW, districts, "Bridge", "d");
    final ValidationResult result = new ValidationResult(false, errors);
    districts.clear();
    errors.clear();

    assertThat(update.districts()).containsExactly(District.KALUTARA);
    assertThat(details.districts()).containsExactly(District.KALUTARA);
    assertThat(result.errors()).containsExactly("photo: missing");
  }

  @Test
  @DisplayName("ReportCriteria and CriteriaCheck copy their sets")
  void criteriaRecordsCopyTheirSets() {
    Set<District> districts = EnumSet.of(District.RATNAPURA);
    Set<ReportMetric> metrics = EnumSet.of(ReportMetric.ALERT_TIMELINE);
    ReportCriteria criteria =
        new ReportCriteria(
            districts, null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), metrics, null);
    Set<District> outOfScope = EnumSet.of(District.GALLE);
    final CriteriaCheck check =
        new CriteriaCheck(new ValidationResult(true, List.of()), outOfScope, criteria);

    districts.add(District.GALLE);
    metrics.add(ReportMetric.CITIZENS_REACHED);
    outOfScope.clear();

    assertThat(criteria.districts()).containsExactly(District.RATNAPURA);
    assertThat(criteria.metrics()).containsExactly(ReportMetric.ALERT_TIMELINE);
    assertThat(check.outOfScope()).containsExactly(District.GALLE);
    assertThat(check.adjusted()).isSameAs(criteria);
  }

  @Test
  @DisplayName("ReportResults copies all of its collections")
  void reportResultsCopiesCollections() {
    List<AlertTimelineEntry> timeline = new ArrayList<>();
    Map<NotificationChannel, Integer> reached = new HashMap<>(Map.of(NotificationChannel.SMS, 4));
    Set<ReportMetric> failed = EnumSet.noneOf(ReportMetric.class);
    List<OccupancyPoint> points = new ArrayList<>();
    List<DistributionLine> lines = new ArrayList<>();

    final ReportResults results =
        new ReportResults(0, timeline, reached, 4, points, 100, lines, failed);
    timeline.add(
        new AlertTimelineEntry(
            "WRN-1", NOW, WarningLevel.LEVEL_1_YELLOW, Set.of(), TimelineEvent.ISSUED));
    reached.clear();
    failed.add(ReportMetric.RESOURCE_DISTRIBUTION);
    points.add(new OccupancyPoint("S1", District.GALLE, NOW, 1, 2));
    lines.add(new DistributionLine(District.GALLE, null, "n", SupplyCategory.FOOD, "kg", 1.0));

    assertThat(results.alertTimeline()).isEmpty();
    assertThat(results.citizensReachedByChannel()).containsOnlyKeys(NotificationChannel.SMS);
    assertThat(results.failedMetrics()).isEmpty();
    assertThat(results.occupancyOverTime()).isEmpty();
    assertThat(results.distributionLines()).isEmpty();
  }

  @Test
  @DisplayName("ReportHeader copies its map and AlertTimelineEntry copies its districts")
  void reportHeaderAndTimelineEntryCopyTheirCollections() {
    Map<District, LocalDateTime> provisional = new HashMap<>(Map.of(District.GALLE, NOW));
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.GALLE),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31),
            Set.of(ReportMetric.ALERT_TIMELINE),
            "HZ-2026-0142");
    ReportHeader header = new ReportHeader("DR-2026-0001", "Title", NOW, criteria, provisional);
    Set<District> covered = EnumSet.of(District.GALLE);
    final AlertTimelineEntry entry =
        new AlertTimelineEntry(
            "WRN-1", NOW, WarningLevel.LEVEL_3_RED, covered, TimelineEvent.ESCALATED);

    provisional.clear();
    covered.clear();

    assertThat(header.provisionalSince()).containsOnlyKeys(District.GALLE);
    assertThat(entry.districts()).containsExactly(District.GALLE);
  }
}
