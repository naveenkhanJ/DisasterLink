package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.NotificationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link CitizensReachedCalculator}.
 *
 * <p>Implements tests for CH-20, CH-31, CH-32 and SC4 step 7.
 */
@ExtendWith(MockitoExtension.class)
class CitizensReachedCalculatorTest {

  @Mock private WarningRepository warningRepository;
  @Mock private NotificationRepository notificationRepository;

  private CitizensReachedCalculator calculator;
  private ReportCriteria criteria;

  @BeforeEach
  void setUp() {
    calculator = new CitizensReachedCalculator(warningRepository, notificationRepository);
    criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA, District.KALUTARA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.CITIZENS_REACHED),
            null);
  }

  private static Warning createWarning(
      String warningId, WarningLevel level, LocalDateTime issuedTime, List<District> districts) {
    Hazard hazard = SampleEntities.hazard("HZ-1");
    DMCOfficer officer =
        new DMCOfficer(
            "OFF-1", "A. Perera", "0771112233", "officer@example.lk", "HQ", Set.copyOf(districts));
    WarningDetails details =
        new WarningDetails(
            level,
            SampleEntities.TEXT,
            SampleEntities.TEXT,
            districts,
            List.of(),
            Set.of(NotificationChannel.PUSH, NotificationChannel.SMS),
            issuedTime.plusDays(2));
    return new Warning(warningId, hazard, details, officer, issuedTime);
  }

  @Test
  @DisplayName("Constructor rejects null arguments")
  void constructorRejectsNull() {
    assertThatThrownBy(() -> new CitizensReachedCalculator(null, notificationRepository))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new CitizensReachedCalculator(warningRepository, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("supports() identifies only CITIZENS_REACHED")
  void supportsMetric() {
    assertThat(calculator.supports(ReportMetric.CITIZENS_REACHED)).isTrue();
    assertThat(calculator.supports(ReportMetric.ALERT_TIMELINE)).isFalse();
    assertThat(calculator.supports(ReportMetric.SHELTER_OCCUPANCY_OVER_TIME)).isFalse();
    assertThat(calculator.supports(ReportMetric.RESOURCE_DISTRIBUTION)).isFalse();
  }

  @Test
  @DisplayName("calculate() rejects null criteria")
  void calculateRejectsNull() {
    assertThatThrownBy(() -> calculator.calculate(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("SC4 step 7 / empty results when no matching warnings exist")
  void returnsEmptyResultsWhenNoWarnings() {
    when(warningRepository.findMatching(criteria)).thenReturn(List.of());

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isZero();
    assertThat(results.citizensReachedByChannel()).isEmpty();
    assertThat(results.citizensReachedTotal()).isZero();
    assertThat(results.failedMetrics()).isEmpty();
  }

  @Test
  @DisplayName("SC4 step 7 / PR6 — aggregates citizens reached per channel and total")
  void aggregatesCitizensReached() {
    Warning warning =
        createWarning(
            "WRN-87",
            WarningLevel.LEVEL_2_AMBER,
            LocalDateTime.of(2026, 5, 20, 10, 45),
            List.of(District.RATNAPURA));

    when(warningRepository.findMatching(criteria)).thenReturn(List.of(warning));
    when(notificationRepository.countCitizensReachedByChannel(
            Set.of("WRN-87"), criteria.districts()))
        .thenReturn(
            Map.of(
                NotificationChannel.PUSH, 30100,
                NotificationChannel.SMS, 42335,
                NotificationChannel.AUDIBLE_ALERT, 30100));
    when(notificationRepository.countCitizensReached(Set.of("WRN-87"), criteria.districts()))
        .thenReturn(42335);
    when(notificationRepository.countByWarnings(Set.of("WRN-87"))).thenReturn(104735);

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isEqualTo(104735);
    assertThat(results.citizensReachedTotal()).isEqualTo(42335);
    assertThat(results.citizensReachedByChannel())
        .containsEntry(NotificationChannel.PUSH, 30100)
        .containsEntry(NotificationChannel.SMS, 42335)
        .containsEntry(NotificationChannel.AUDIBLE_ALERT, 30100);
  }
}
