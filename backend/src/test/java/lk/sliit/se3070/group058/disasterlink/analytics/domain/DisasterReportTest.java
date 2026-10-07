package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link DisasterReport}, {@link CriteriaTemplate} and {@link ShareOutcome}. */
class DisasterReportTest {

  private static final DMCOfficer OFFICER = SampleEntities.officer("DMC-1", Set.of());
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 25, 10, 0);
  private static final ReportCriteria CRITERIA =
      FeatureFixtures.criteria(
          Set.of(District.RATNAPURA),
          HazardType.FLOOD,
          LocalDate.of(2026, 5, 20),
          LocalDate.of(2026, 5, 25));
  private static final ReportResults RESULTS = FeatureFixtures.results();

  @Test
  @DisplayName(
      "The drawn constructor starts a report without findings, provisional districts or shares")
  void drawnConstructor() {
    Hazard hazard = SampleEntities.hazard("HZ-2026-0142");

    DisasterReport report =
        new DisasterReport(
            "DR-2026-0200", "Kalu Ganga flood", NOW, CRITERIA, RESULTS, OFFICER, List.of(hazard));

    assertThat(report.getReportTitle()).isEqualTo("Kalu Ganga flood");
    assertThat(report.getGeneratedDate()).isEqualTo(NOW);
    assertThat(report.getCriteria()).isEqualTo(CRITERIA);
    assertThat(report.getResults()).isEqualTo(RESULTS);
    assertThat(report.getOfficer()).isSameAs(OFFICER);
    assertThat(report.getHazards()).containsExactly(hazard);
    assertThat(report.getFindings()).isEmpty();
    assertThat(report.getProvisionalSince()).isEmpty();
    assertThat(report.getSharedWith()).isEmpty();
  }

  @Test
  @DisplayName(
      "The seed constructor keeps findings, provisional districts and shares, and copies them")
  void seedConstructor() {
    Map<District, LocalDateTime> since = new EnumMap<>(District.class);
    since.put(District.KALUTARA, NOW.minusDays(1));
    List<Organization> shared = new ArrayList<>(List.of(SampleEntities.organization("ORG-1")));

    DisasterReport report =
        new DisasterReport(
            "DR-1", "T", NOW, CRITERIA, RESULTS, OFFICER, List.of(), "All clear", since, shared);
    since.clear();
    shared.clear();

    assertThat(report.getFindings()).contains("All clear");
    assertThat(report.getProvisionalSince()).containsEntry(District.KALUTARA, NOW.minusDays(1));
    assertThat(report.getSharedWith()).hasSize(1);
    assertThatThrownBy(() -> report.getProvisionalSince().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.getHazards().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("The constructors reject null required arguments")
  void constructorsRejectNull() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> new DisasterReport(null, "T", NOW, CRITERIA, RESULTS, OFFICER, List.of()));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new DisasterReport("D", null, NOW, CRITERIA, RESULTS, OFFICER, List.of()));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new DisasterReport("D", "T", null, CRITERIA, RESULTS, OFFICER, List.of()));
    assertThatNullPointerException()
        .isThrownBy(() -> new DisasterReport("D", "T", NOW, null, RESULTS, OFFICER, List.of()));
    assertThatNullPointerException()
        .isThrownBy(() -> new DisasterReport("D", "T", NOW, CRITERIA, null, OFFICER, List.of()));
    assertThatNullPointerException()
        .isThrownBy(() -> new DisasterReport("D", "T", NOW, CRITERIA, RESULTS, null, List.of()));
  }

  @Test
  @DisplayName("Reports are equal when their identifiers are equal")
  void equalsFollowsTheIdentifier() {
    DisasterReport report =
        new DisasterReport("DR-1", "T", NOW, CRITERIA, RESULTS, OFFICER, List.of());

    assertThat(report)
        .isEqualTo(new DisasterReport("DR-1", "U", NOW, CRITERIA, RESULTS, OFFICER, List.of()))
        .isNotEqualTo(new DisasterReport("DR-2", "T", NOW, CRITERIA, RESULTS, OFFICER, List.of()))
        .hasToString("DisasterReport[DR-1]");
    assertThat(report.equals(report)).isTrue();
    assertThat(report.equals("x")).isFalse();
    assertThat(report.hashCode()).isEqualTo("DR-1".hashCode());
  }

  @Test
  @DisplayName("A criteria template keeps its criteria and its owner")
  void criteriaTemplate() {
    CriteriaTemplate template = new CriteriaTemplate("TPL-1", "Monsoon", CRITERIA, OFFICER);

    assertThat(template.getName()).isEqualTo("Monsoon");
    assertThat(template.getCriteria()).isEqualTo(CRITERIA);
    assertThat(template.getOwner()).isSameAs(OFFICER);
    assertThat(template)
        .isEqualTo(new CriteriaTemplate("TPL-1", "Other", CRITERIA, OFFICER))
        .isNotEqualTo(new CriteriaTemplate("TPL-2", "Monsoon", CRITERIA, OFFICER))
        .hasToString("CriteriaTemplate[TPL-1]");
    assertThat(template.equals(template)).isTrue();
    assertThat(template.equals("x")).isFalse();
    assertThat(template.hashCode()).isEqualTo("TPL-1".hashCode());
    assertThatNullPointerException()
        .isThrownBy(() -> new CriteriaTemplate(null, "n", CRITERIA, OFFICER));
    assertThatNullPointerException()
        .isThrownBy(() -> new CriteriaTemplate("t", null, CRITERIA, OFFICER));
    assertThatNullPointerException()
        .isThrownBy(() -> new CriteriaTemplate("t", "n", null, OFFICER));
    assertThatNullPointerException()
        .isThrownBy(() -> new CriteriaTemplate("t", "n", CRITERIA, null));
  }

  @Test
  @DisplayName("ShareOutcome copies its lists and needs all of them")
  void shareOutcome() {
    DisasterReport report =
        new DisasterReport("DR-1", "T", NOW, CRITERIA, RESULTS, OFFICER, List.of());
    List<Organization> shared = new ArrayList<>(List.of(SampleEntities.organization("ORG-1")));

    ShareOutcome outcome = new ShareOutcome(report, shared, List.of());
    shared.clear();

    assertThat(outcome.sharedWith()).hasSize(1);
    assertThat(outcome.noData()).isEmpty();
    assertThat(outcome.report()).isSameAs(report);
    assertThatNullPointerException().isThrownBy(() -> new ShareOutcome(null, List.of(), List.of()));
    assertThatNullPointerException().isThrownBy(() -> new ShareOutcome(report, null, List.of()));
    assertThatNullPointerException().isThrownBy(() -> new ShareOutcome(report, List.of(), null));
  }
}
