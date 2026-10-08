package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.repository.ShelterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link OccupancyTrendCalculator}.
 *
 * <p>Implements tests for CH-20, CH-31, CH-32, PR8, FX-16 and SC4 step 7.
 */
@ExtendWith(MockitoExtension.class)
class OccupancyTrendCalculatorTest {

  @Mock private ShelterRepository shelterRepository;

  private OccupancyTrendCalculator calculator;
  private ReportCriteria criteria;

  @BeforeEach
  void setUp() {
    calculator = new OccupancyTrendCalculator(shelterRepository);
    criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA, District.KALUTARA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.SHELTER_OCCUPANCY_OVER_TIME),
            null);
  }

  @Test
  @DisplayName("Constructor rejects null arguments")
  void constructorRejectsNull() {
    assertThatThrownBy(() -> new OccupancyTrendCalculator(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("supports() identifies only SHELTER_OCCUPANCY_OVER_TIME")
  void supportsMetric() {
    assertThat(calculator.supports(ReportMetric.SHELTER_OCCUPANCY_OVER_TIME)).isTrue();
    assertThat(calculator.supports(ReportMetric.ALERT_TIMELINE)).isFalse();
    assertThat(calculator.supports(ReportMetric.CITIZENS_REACHED)).isFalse();
    assertThat(calculator.supports(ReportMetric.RESOURCE_DISTRIBUTION)).isFalse();
  }

  @Test
  @DisplayName("calculate() rejects null criteria")
  void calculateRejectsNull() {
    assertThatThrownBy(() -> calculator.calculate(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("SC4 step 7 / empty results when no matching shelters exist")
  void returnsEmptyResultsWhenNoShelters() {
    when(shelterRepository.findMatching(criteria)).thenReturn(List.of());

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.matchedRecordCount()).isZero();
    assertThat(results.occupancyOverTime()).isEmpty();
    assertThat(results.combinedCapacity()).isZero();
    assertThat(results.failedMetrics()).isEmpty();
  }

  @Test
  @DisplayName("SC4 step 7 / PR8, FX-16 — builds occupancy points and computes combined capacity")
  void buildsOccupancyPointsInPeriod() {
    DistrictOfficer officer = SampleEntities.districtOfficer("OFF-1");

    OccupancyRecord rec1 =
        new OccupancyRecord(
            150,
            LocalDateTime.of(2026, 5, 21, 14, 0),
            LocalDateTime.of(2026, 5, 21, 14, 5),
            officer);
    // Before period
    OccupancyRecord recBefore =
        new OccupancyRecord(
            50,
            LocalDateTime.of(2026, 5, 19, 10, 0),
            LocalDateTime.of(2026, 5, 19, 10, 5),
            officer);
    // After period
    OccupancyRecord recAfter =
        new OccupancyRecord(
            30, LocalDateTime.of(2026, 5, 26, 1, 0), LocalDateTime.of(2026, 5, 26, 1, 5), officer);

    Shelter shelter1 =
        new Shelter(
            "SHL-1",
            "Ratnapura Central College",
            "Main St, Ratnapura",
            District.RATNAPURA,
            500,
            150,
            "0451234567",
            ShelterStatus.OPEN,
            officer,
            null,
            List.of(recBefore, rec1, recAfter),
            List.of());

    OccupancyRecord rec2 =
        new OccupancyRecord(
            300,
            LocalDateTime.of(2026, 5, 21, 10, 0),
            LocalDateTime.of(2026, 5, 21, 10, 5),
            officer);
    Shelter shelter2 =
        new Shelter(
            "SHL-2",
            "Kalutara Vidyalaya",
            "Galle Rd, Kalutara",
            District.KALUTARA,
            800,
            300,
            "0341234567",
            ShelterStatus.OPEN,
            officer,
            null,
            List.of(rec2),
            List.of());

    when(shelterRepository.findMatching(criteria)).thenReturn(List.of(shelter1, shelter2));

    ReportResults results = calculator.calculate(criteria);

    assertThat(results.combinedCapacity()).isEqualTo(1300);
    assertThat(results.matchedRecordCount()).isEqualTo(2);
    assertThat(results.occupancyOverTime()).hasSize(2);

    OccupancyPoint firstPoint = results.occupancyOverTime().get(0);
    assertThat(firstPoint.shelterId()).isEqualTo("SHL-2");
    assertThat(firstPoint.capturedAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 10, 0));
    assertThat(firstPoint.occupantCount()).isEqualTo(300);
    assertThat(firstPoint.capacity()).isEqualTo(800);

    OccupancyPoint secondPoint = results.occupancyOverTime().get(1);
    assertThat(secondPoint.shelterId()).isEqualTo("SHL-1");
    assertThat(secondPoint.capturedAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 0));
    assertThat(secondPoint.occupantCount()).isEqualTo(150);
    assertThat(secondPoint.capacity()).isEqualTo(500);
  }
}
