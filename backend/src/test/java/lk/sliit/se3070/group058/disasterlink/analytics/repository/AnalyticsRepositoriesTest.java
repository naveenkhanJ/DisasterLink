package lk.sliit.se3070.group058.disasterlink.analytics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.testsupport.FeatureFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Tests of {@link DisasterReportRepository} and {@link CriteriaTemplateRepository}. */
@DataJpaTest
class AnalyticsRepositoriesTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 25, 10, 0);
  private static final ReportCriteria CRITERIA =
      FeatureFixtures.criteria(
          Set.of(District.RATNAPURA),
          HazardType.FLOOD,
          LocalDate.of(2026, 5, 20),
          LocalDate.of(2026, 5, 25));

  @Autowired private TestEntityManager entityManager;
  @Autowired private DisasterReportRepository reports;
  @Autowired private CriteriaTemplateRepository templates;

  private void save(Object... entities) {
    for (Object entity : entities) {
      entityManager.persist(entity);
    }
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("findSharedWith gives the reports shared with an organisation, oldest first")
  void findSharedWith() {
    DMCOfficer officer = SampleEntities.officer("DMC-1", Set.of());
    Organization first = SampleEntities.organization("ORG-1");
    Organization second = SampleEntities.organization("ORG-2");
    DisasterReport both = report("DR-NEW", NOW.plusHours(1), officer, List.of(first, second));
    DisasterReport onlySecond = report("DR-OLD", NOW, officer, List.of(second));
    DisasterReport nobody = report("DR-PRIVATE", NOW, officer, List.of());
    save(officer, first, second, both, onlySecond, nobody);

    assertThat(reports.findSharedWith("ORG-2"))
        .extracting(DisasterReport::getReportId)
        .containsExactly("DR-OLD", "DR-NEW");
    assertThat(reports.findSharedWith("ORG-1"))
        .extracting(DisasterReport::getReportId)
        .containsExactly("DR-NEW");
    assertThat(reports.findSharedWith("ORG-3")).isEmpty();
  }

  private static DisasterReport report(
      String id, LocalDateTime generated, DMCOfficer officer, List<Organization> sharedWith) {
    return new DisasterReport(
        id,
        "Title " + id,
        generated,
        CRITERIA,
        FeatureFixtures.results(),
        officer,
        List.of(),
        null,
        Map.of(),
        sharedWith);
  }

  @Test
  @DisplayName("findByOwner gives one officer's templates by name")
  void findByOwner() {
    DMCOfficer owner = SampleEntities.officer("DMC-1", Set.of());
    DMCOfficer other = SampleEntities.officer("DMC-2", Set.of());
    save(
        owner,
        other,
        new CriteriaTemplate("TPL-B", "Beta", CRITERIA, owner),
        new CriteriaTemplate("TPL-A", "Alpha", CRITERIA, owner),
        new CriteriaTemplate("TPL-X", "Other", CRITERIA, other));

    assertThat(templates.findByOwner("DMC-1"))
        .extracting(CriteriaTemplate::getTemplateId)
        .containsExactly("TPL-A", "TPL-B");
    assertThat(templates.findByOwner("DMC-3")).isEmpty();
  }
}
