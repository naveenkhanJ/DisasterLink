package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AlertTimelineEntry;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCDutyOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.TimelineEvent;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AlertTimelineCalculator}.
 *
 * <p>Implements tests for CH-20, CH-31, CH-32 and SC4 step 7.
 */
@ExtendWith(MockitoExtension.class)
class AlertTimelineCalculatorTest {

  @Mock private WarningRepository warningRepository;
  @Mock private AuditRecordRepository auditRecordRepository;

  private AlertTimelineCalculator calculator;
  private ReportCriteria criteria;

  @BeforeEach
  void setUp() {
    calculator = new AlertTimelineCalculator(warningRepository, auditRecordRepository);
    criteria =
        new ReportCriteria(
            Set.of(District.COLOMBO, District.KALUTARA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
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
    assertThatThrownBy(() -> new AlertTimelineCalculator(null, auditRecordRepository))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new AlertTimelineCalculator(warningRepository, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("supports() identifies only ALERT_TIMELINE")
  void supportsMetric() {
    assertThat(calculator.supports(ReportMetric.ALERT_TIMELINE)).isTrue();
    assertThat(calculator.supports(ReportMetric.CITIZENS_REACHED)).isFalse();
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
    assertThat(results.alertTimeline()).isEmpty();
    assertThat(results.failedMetrics()).isEmpty();
  }

  @Test
  @DisplayName("SC4 step 7 — builds timeline for warning without escalations")
  void buildsTimelineWithoutEscalations() {
    Warning warning =
        createWarning(
            "WRN-1",
            WarningLevel.LEVEL_2_AMBER,
            LocalDateTime.of(2026, 5, 20, 10, 0),
            List.of(District.COLOMBO));

    when(warningRepository.findMatching(criteria)).thenReturn(List.of(warning));
    when(auditRecordRepository.findEscalations(Set.of("WRN-1"))).thenReturn(List.of());

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isEqualTo(1);
    assertThat(results.alertTimeline()).hasSize(1);

    AlertTimelineEntry entry = results.alertTimeline().get(0);
    assertThat(entry.warningId()).isEqualTo("WRN-1");
    assertThat(entry.issuedTime()).isEqualTo(LocalDateTime.of(2026, 5, 20, 10, 0));
    assertThat(entry.warningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(entry.districts()).containsExactly(District.COLOMBO);
    assertThat(entry.event()).isEqualTo(TimelineEvent.ISSUED);
  }

  @Test
  @DisplayName("SC4 step 7 / PR7 — builds timeline with escalations and derives initial level")
  void buildsTimelineWithEscalations() {
    Warning warning =
        createWarning(
            "WRN-87",
            WarningLevel.LEVEL_3_RED,
            LocalDateTime.of(2026, 5, 20, 10, 45),
            List.of(District.KALUTARA));

    DMCDutyOfficer dutyOfficer =
        new DMCDutyOfficer(
            "OFF-2", "K. Perera", "0771234567", "perera@example.lk", "Duty Officer", Set.of());
    AuditRecord escalation =
        new AuditRecord(
            AuditAction.WARNING_ESCALATED,
            dutyOfficer,
            "WRN-87",
            "LEVEL_2_AMBER→LEVEL_3_RED",
            LocalDateTime.of(2026, 5, 21, 8, 25));

    when(warningRepository.findMatching(criteria)).thenReturn(List.of(warning));
    when(auditRecordRepository.findEscalations(Set.of("WRN-87"))).thenReturn(List.of(escalation));

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isEqualTo(1);
    assertThat(results.alertTimeline()).hasSize(2);

    AlertTimelineEntry issued = results.alertTimeline().get(0);
    assertThat(issued.warningId()).isEqualTo("WRN-87");
    assertThat(issued.issuedTime()).isEqualTo(LocalDateTime.of(2026, 5, 20, 10, 45));
    assertThat(issued.warningLevel()).isEqualTo(WarningLevel.LEVEL_2_AMBER);
    assertThat(issued.event()).isEqualTo(TimelineEvent.ISSUED);

    AlertTimelineEntry escalated = results.alertTimeline().get(1);
    assertThat(escalated.warningId()).isEqualTo("WRN-87");
    assertThat(escalated.issuedTime()).isEqualTo(LocalDateTime.of(2026, 5, 21, 8, 25));
    assertThat(escalated.warningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(escalated.event()).isEqualTo(TimelineEvent.ESCALATED);
  }

  @Test
  @DisplayName("Handles invalid or null detail gracefully")
  void handlesInvalidDetailGracefully() {
    Warning warning =
        createWarning(
            "WRN-1",
            WarningLevel.LEVEL_3_RED,
            LocalDateTime.of(2026, 5, 20, 10, 0),
            List.of(District.COLOMBO));

    DMCDutyOfficer dutyOfficer =
        new DMCDutyOfficer(
            "OFF-2", "K. Perera", "0771234567", "perera@example.lk", "Duty Officer", Set.of());
    AuditRecord badDetail =
        new AuditRecord(
            AuditAction.WARNING_ESCALATED,
            dutyOfficer,
            "WRN-1",
            "NOT_A_LEVEL",
            LocalDateTime.of(2026, 5, 21, 8, 0));
    AuditRecord nullDetail =
        new AuditRecord(
            AuditAction.WARNING_ESCALATED,
            dutyOfficer,
            "WRN-1",
            null,
            LocalDateTime.of(2026, 5, 21, 9, 0));

    when(warningRepository.findMatching(criteria)).thenReturn(List.of(warning));
    when(auditRecordRepository.findEscalations(Set.of("WRN-1")))
        .thenReturn(List.of(badDetail, nullDetail));

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isEqualTo(1);
    assertThat(results.alertTimeline()).hasSize(3);
    assertThat(results.alertTimeline().get(0).warningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(results.alertTimeline().get(1).warningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
    assertThat(results.alertTimeline().get(2).warningLevel()).isEqualTo(WarningLevel.LEVEL_3_RED);
  }
}
