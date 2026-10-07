package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the JSON attribute converters of {@link ReportCriteria} and {@link ReportResults}. */
class ConverterTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 8, 15, 30);

  private static final ReportCriteria CRITERIA =
      new ReportCriteria(
          Set.of(District.RATNAPURA, District.KALUTARA),
          HazardType.FLOOD,
          LocalDate.of(2026, 5, 20),
          LocalDate.of(2026, 5, 25),
          Set.of(ReportMetric.ALERT_TIMELINE, ReportMetric.CITIZENS_REACHED),
          "HZ-2026-0142");

  private static final ReportResults RESULTS =
      new ReportResults(
          42,
          List.of(
              new AlertTimelineEntry(
                  "WRN-2026-0087",
                  NOW,
                  WarningLevel.LEVEL_2_AMBER,
                  Set.of(District.RATNAPURA),
                  TimelineEvent.ISSUED)),
          Map.of(NotificationChannel.SMS, 100, NotificationChannel.PUSH, 60),
          160,
          List.of(new OccupancyPoint("SHL-1", District.KALUTARA, NOW, 80, 120)),
          120,
          List.of(
              new DistributionLine(
                  District.RATNAPURA, null, "Private donor", SupplyCategory.FOOD, "kg", 12.5)),
          Set.of(ReportMetric.RESOURCE_DISTRIBUTION));

  private final ReportCriteriaConverter criteriaConverter = new ReportCriteriaConverter();

  private final ReportResultsConverter resultsConverter = new ReportResultsConverter();

  @Test
  @DisplayName("A fully populated ReportCriteria survives the round trip through JSON")
  void criteriaRoundTrip() {
    String json = criteriaConverter.convertToDatabaseColumn(CRITERIA);

    assertThat(json).contains("2026-05-20").contains("HZ-2026-0142");
    assertThat(criteriaConverter.convertToEntityAttribute(json)).isEqualTo(CRITERIA);
  }

  @Test
  @DisplayName("ReportCriteria without its optional values survives the round trip")
  void criteriaWithoutOptionalValuesRoundTrip() {
    ReportCriteria minimal =
        new ReportCriteria(
            Set.of(District.GALLE),
            null,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            Set.of(ReportMetric.RESOURCE_DISTRIBUTION),
            null);

    assertThat(
            criteriaConverter.convertToEntityAttribute(
                criteriaConverter.convertToDatabaseColumn(minimal)))
        .isEqualTo(minimal);
  }

  @Test
  @DisplayName("A fully populated ReportResults survives the round trip through JSON")
  void resultsRoundTrip() {
    String json = resultsConverter.convertToDatabaseColumn(RESULTS);

    assertThat(json).contains("2026-05-21T08:15:30").contains("LEVEL_2_AMBER");
    assertThat(resultsConverter.convertToEntityAttribute(json)).isEqualTo(RESULTS);
  }

  @Test
  @DisplayName("null is stored as null and read back as null")
  void nullStaysNull() {
    assertThat(criteriaConverter.convertToDatabaseColumn(null)).isNull();
    assertThat(criteriaConverter.convertToEntityAttribute(null)).isNull();
    assertThat(resultsConverter.convertToDatabaseColumn(null)).isNull();
    assertThat(resultsConverter.convertToEntityAttribute(null)).isNull();
  }

  @Test
  @DisplayName("Broken JSON is reported as an IllegalStateException with the cause")
  void brokenJsonIsNotSwallowed() {
    assertThatThrownBy(() -> criteriaConverter.convertToEntityAttribute("{not json"))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
    assertThatThrownBy(() -> resultsConverter.convertToEntityAttribute("{not json"))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
  }
}
