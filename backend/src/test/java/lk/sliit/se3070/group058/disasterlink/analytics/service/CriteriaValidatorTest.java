package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CriteriaCheck;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link CriteriaValidator}.
 *
 * <p>Implements CH-31, SC4 step 6, E1, E4; model dictionary section 6.
 */
class CriteriaValidatorTest {

  private CriteriaValidator validator;
  private static final Set<District> OFFICER_DISTRICTS =
      Set.of(
          District.COLOMBO,
          District.GAMPAHA,
          District.KALUTARA,
          District.RATNAPURA,
          District.KEGALLE);

  @BeforeEach
  void setUp() {
    validator = new CriteriaValidator();
  }

  @Test
  @DisplayName("Valid criteria in officer scope passes with no out-of-scope districts")
  void validCriteriaWithinScope() {
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA, District.KALUTARA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE, ReportMetric.CITIZENS_REACHED),
            "HZ-2026-0142");

    CriteriaCheck check = validator.validate(criteria, OFFICER_DISTRICTS);

    assertThat(check.result().valid()).isTrue();
    assertThat(check.result().errors()).isEmpty();
    assertThat(check.outOfScope()).isEmpty();
    assertThat(check.adjusted()).isNull();
  }

  @Test
  @DisplayName("National officer scope allows any district")
  void nationalOfficerScope() {
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.JAFFNA, District.GALLE),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
            null);

    CriteriaCheck check = validator.validate(criteria, Set.of());

    assertThat(check.result().valid()).isTrue();
    assertThat(check.outOfScope()).isEmpty();
    assertThat(check.adjusted()).isNull();
  }

  @Test
  @DisplayName("Detects start date after end date and missing metrics (E1)")
  void invalidDatesAndMissingMetrics() {
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 25),
            LocalDate.of(2026, 5, 20),
            Set.of(),
            null);

    CriteriaCheck check = validator.validate(criteria, OFFICER_DISTRICTS);

    assertThat(check.result().valid()).isFalse();
    assertThat(check.result().errors())
        .contains("The start date is after the end date")
        .contains("Select at least one metric");
  }

  @Test
  @DisplayName("Detects period exceeding 24 months and missing dates or districts")
  void periodExceeding24MonthsAndMissingFields() {
    ReportCriteria criteriaTooLong =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            null,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2026, 1, 2),
            Set.of(ReportMetric.ALERT_TIMELINE),
            null);

    CriteriaCheck checkTooLong = validator.validate(criteriaTooLong, OFFICER_DISTRICTS);
    assertThat(checkTooLong.result().valid()).isFalse();
    assertThat(checkTooLong.result().errors()).contains("A period of at most 24 months");

    ReportCriteria criteriaMissing = new ReportCriteria(Set.of(), null, null, null, Set.of(), null);
    CriteriaCheck checkMissing = validator.validate(criteriaMissing, OFFICER_DISTRICTS);
    assertThat(checkMissing.result().valid()).isFalse();
    assertThat(checkMissing.result().errors())
        .contains("Select at least one district")
        .contains("Start date is required")
        .contains("End date is required")
        .contains("Select at least one metric");
  }

  @Test
  @DisplayName("Some districts out of scope creates adjusted criteria (E4 6d-6e)")
  void someDistrictsOutOfScope() {
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA, District.GALLE),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
            null);

    CriteriaCheck check = validator.validate(criteria, OFFICER_DISTRICTS);

    assertThat(check.result().valid()).isTrue();
    assertThat(check.outOfScope()).containsExactly(District.GALLE);
    assertThat(check.adjusted()).isNotNull();
    assertThat(check.adjusted().districts()).containsExactly(District.RATNAPURA);
    assertThat(check.adjusted().hazardType()).isEqualTo(HazardType.FLOOD);
  }

  @Test
  @DisplayName(
      "All selected districts out of scope causes validation error without adjusted criteria (E4 6g)")
  void allDistrictsOutOfScope() {
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.GALLE, District.MATARA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
            null);

    CriteriaCheck check = validator.validate(criteria, OFFICER_DISTRICTS);

    assertThat(check.result().valid()).isFalse();
    assertThat(check.result().errors())
        .contains("None of the selected districts is within your reporting districts");
    assertThat(check.adjusted()).isNull();
  }

  @Test
  @DisplayName("Null criteria throws NullPointerException")
  void nullCriteriaThrows() {
    assertThatNullPointerException().isThrownBy(() -> validator.validate(null, OFFICER_DISTRICTS));
  }
}
