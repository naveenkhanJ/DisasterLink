package lk.sliit.se3070.group058.disasterlink.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ExportFailedException;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ReportNotSharedException;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ShareOutcome;
import lk.sliit.se3070.group058.disasterlink.analytics.repository.CriteriaTemplateRepository;
import lk.sliit.se3070.group058.disasterlink.analytics.repository.DisasterReportRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AlertTimelineEntry;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditAction;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CriteriaCheck;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DMCOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistributionLine;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFile;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ExportFormat;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportHeader;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ScopedReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyCategory;
import lk.sliit.se3070.group058.disasterlink.shared.domain.TimelineEvent;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Unit tests for {@link AnalyticsController}.
 *
 * <p>Implements tests for CH-19, CH-20, CH-31, CH-32, CH-33 and all SC4 scenario flows (Main 1-19,
 * A1-A7, E1-E6).
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

  @Mock private DisasterReportRepository disasterReportRepository;
  @Mock private CriteriaTemplateRepository criteriaTemplateRepository;
  @Mock private HazardRepository hazardRepository;
  @Mock private OrganizationRepository organizationRepository;
  @Mock private ClientSyncStatusRepository clientSyncStatusRepository;
  @Mock private AuditRecordRepository auditRecordRepository;
  @Mock private DMCOfficerRepository dmcOfficerRepository;
  @Mock private IdGenerator idGenerator;
  @Mock private CriteriaValidator criteriaValidator;
  @Mock private MetricCalculator timelineCalculator;
  @Mock private MetricCalculator distributionCalculator;
  @Mock private ReportExporter pdfExporter;
  @Mock private PlatformTransactionManager transactionManager;

  private final Clock clock = Clock.fixed(Instant.parse("2026-05-26T09:30:00Z"), ZoneId.of("UTC"));

  private AnalyticsController controller;
  private DMCOfficer sampleOfficer;
  private ReportCriteria sampleCriteria;

  @BeforeEach
  void setUp() {
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenReturn(new SimpleTransactionStatus());

    sampleOfficer =
        new DMCOfficer(
            "OFF-1",
            "A. Perera",
            "0771234567",
            "perera@example.lk",
            "HQ",
            Set.of(District.RATNAPURA, District.KALUTARA));

    sampleCriteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE, ReportMetric.RESOURCE_DISTRIBUTION),
            "HZ-2026-0142");

    lenient().when(timelineCalculator.supports(ReportMetric.ALERT_TIMELINE)).thenReturn(true);
    lenient()
        .when(distributionCalculator.supports(ReportMetric.RESOURCE_DISTRIBUTION))
        .thenReturn(true);
    lenient().when(pdfExporter.supports(ExportFormat.PDF)).thenReturn(true);

    controller =
        new AnalyticsController(
            disasterReportRepository,
            criteriaTemplateRepository,
            hazardRepository,
            organizationRepository,
            clientSyncStatusRepository,
            auditRecordRepository,
            dmcOfficerRepository,
            idGenerator,
            criteriaValidator,
            List.of(timelineCalculator, distributionCalculator),
            List.of(pdfExporter),
            transactionManager,
            clock);
  }

  @Test
  @DisplayName("SC4 step 1 / CH-31 — getTemplates returns officer's saved templates")
  void getTemplatesReturnsOfficerTemplates() {
    CriteriaTemplate template =
        new CriteriaTemplate("TMP-1", "Monsoon Template", sampleCriteria, sampleOfficer);
    when(criteriaTemplateRepository.findByOwner("OFF-1")).thenReturn(List.of(template));

    List<CriteriaTemplate> result = controller.getTemplates("OFF-1");

    assertThat(result).containsExactly(template);
    verify(criteriaTemplateRepository).findByOwner("OFF-1");
  }

  @Test
  @DisplayName("SC4 A5 — getOrganizations returns all registered organisations")
  void getOrganizationsReturnsAll() {
    Organization org = SampleEntities.organization("ORG-1");
    when(organizationRepository.findAll()).thenReturn(List.of(org));

    List<Organization> result = controller.getOrganizations();

    assertThat(result).containsExactly(org);
  }

  @Test
  @DisplayName("SC4 step 1 / CH-31 — getClosedEvents returns closed hazards")
  void getClosedEventsReturnsClosedHazards() {
    Hazard hazard = SampleEntities.hazard("HZ-1");
    when(hazardRepository.findClosed()).thenReturn(List.of(hazard));

    List<Hazard> result = controller.getClosedEvents();

    assertThat(result).containsExactly(hazard);
  }

  @Test
  @DisplayName("SC4 step 2 / CH-31 — getCriteriaForEvent pre-fills criteria from hazard")
  void getCriteriaForEventPreFillsCriteria() {
    Hazard hazard = SampleEntities.hazard("HZ-1");
    when(hazardRepository.findById("HZ-1")).thenReturn(Optional.of(hazard));

    ReportCriteria criteria = controller.getCriteriaForEvent("HZ-1");

    assertThat(criteria.districts()).containsAll(hazard.getDistricts());
    assertThat(criteria.hazardType()).isEqualTo(hazard.getType());
    assertThat(criteria.hazardId()).isEqualTo("HZ-1");
    assertThat(criteria.metrics())
        .containsExactlyInAnyOrderElementsOf(Set.of(ReportMetric.values()));
  }

  @Test
  @DisplayName("SC4 step 6 / E4 — validateCriteria evaluates criteria rules with officer scope")
  void validateCriteriaDelegatesToValidator() {
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    CriteriaCheck check = new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null);
    when(criteriaValidator.validate(sampleCriteria, sampleOfficer.getReportingDistricts()))
        .thenReturn(check);

    CriteriaCheck result = controller.validateCriteria(sampleCriteria, "OFF-1");

    assertThat(result).isEqualTo(check);
  }

  @Test
  @DisplayName("SC4 step 7 / Main Flow — generateReport computes metrics, saves report and audit")
  void generateReportComputesMetricsAndPersists() {
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    when(criteriaValidator.validate(sampleCriteria, sampleOfficer.getReportingDistricts()))
        .thenReturn(new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null));

    AlertTimelineEntry entry =
        new AlertTimelineEntry(
            "WRN-87",
            LocalDateTime.of(2026, 5, 20, 10, 45),
            WarningLevel.LEVEL_2_AMBER,
            Set.of(District.RATNAPURA),
            TimelineEvent.ISSUED);
    ReportResults timelineResults =
        new ReportResults(1, List.of(entry), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    when(timelineCalculator.calculate(sampleCriteria)).thenReturn(timelineResults);

    DistributionLine line =
        new DistributionLine(
            District.RATNAPURA,
            "ORG-1",
            "CARE Sri Lanka",
            SupplyCategory.FOOD,
            "dry-ration packs",
            1200.0);
    ReportResults distResults =
        new ReportResults(1, List.of(), Map.of(), 0, List.of(), 0, List.of(line), Set.of());
    when(distributionCalculator.calculate(sampleCriteria)).thenReturn(distResults);

    when(idGenerator.next(IdKind.DISASTER_REPORT, null)).thenReturn("DR-2026-0184");
    when(hazardRepository.findMatching(sampleCriteria)).thenReturn(List.of());

    ClientSyncStatus clientStatus =
        new ClientSyncStatus("CLT-1", District.RATNAPURA, LocalDateTime.of(2026, 5, 25, 18, 0), 2);
    when(clientSyncStatusRepository.findByDistrictIn(sampleCriteria.districts()))
        .thenReturn(List.of(clientStatus));

    when(disasterReportRepository.save(any(DisasterReport.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DisasterReport report = controller.generateReport(sampleCriteria, "OFF-1");

    assertThat(report.getReportId()).isEqualTo("DR-2026-0184");
    assertThat(report.getResults().matchedRecordCount()).isEqualTo(2);
    assertThat(report.isProvisional()).isTrue();
    assertThat(report.getProvisionalSince()).containsKey(District.RATNAPURA);

    ArgumentCaptor<AuditRecord> auditCaptor = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecordRepository).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.REPORT_GENERATED);
    assertThat(auditCaptor.getValue().getSubjectId()).isEqualTo("DR-2026-0184");
  }

  @Test
  @DisplayName("SC4 step 8 / P60 N26 — generateReport returns unsaved report when 0 records match")
  void generateReportReturnsUnsavedWhenNoRecords() {
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    when(criteriaValidator.validate(sampleCriteria, sampleOfficer.getReportingDistricts()))
        .thenReturn(new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null));

    ReportResults emptyResults =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    when(timelineCalculator.calculate(sampleCriteria)).thenReturn(emptyResults);
    when(distributionCalculator.calculate(sampleCriteria)).thenReturn(emptyResults);

    DisasterReport report = controller.generateReport(sampleCriteria, "OFF-1");

    assertThat(report.getResults().matchedRecordCount()).isZero();
    verify(disasterReportRepository, never()).save(any());
    verify(auditRecordRepository, never()).save(any());
  }

  @Test
  @DisplayName("SC4 E1 / FX-05 — generateReport throws IllegalStateException if criteria invalid")
  void generateReportThrowsWhenInvalidCriteria() {
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    when(criteriaValidator.validate(sampleCriteria, sampleOfficer.getReportingDistricts()))
        .thenReturn(
            new CriteriaCheck(
                new ValidationResult(false, List.of("Start after end")), Set.of(), null));

    assertThatThrownBy(() -> controller.generateReport(sampleCriteria, "OFF-1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Start after end");
  }

  @Test
  @DisplayName("SC4 step 7 / FX-27 — calculator failure recorded in failedMetrics")
  void calculatorFailureRecordedInFailedMetrics() {
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    when(criteriaValidator.validate(sampleCriteria, sampleOfficer.getReportingDistricts()))
        .thenReturn(new CriteriaCheck(new ValidationResult(true, List.of()), Set.of(), null));

    when(timelineCalculator.calculate(sampleCriteria))
        .thenThrow(new RuntimeException("DB timeout"));

    DistributionLine line =
        new DistributionLine(
            District.RATNAPURA,
            "ORG-1",
            "CARE Sri Lanka",
            SupplyCategory.FOOD,
            "dry-ration packs",
            1200.0);
    ReportResults distResults =
        new ReportResults(1, List.of(), Map.of(), 0, List.of(), 0, List.of(line), Set.of());
    when(distributionCalculator.calculate(sampleCriteria)).thenReturn(distResults);

    when(idGenerator.next(IdKind.DISASTER_REPORT, null)).thenReturn("DR-2026-0184");
    when(disasterReportRepository.save(any(DisasterReport.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DisasterReport report = controller.generateReport(sampleCriteria, "OFF-1");

    assertThat(report.getResults().failedMetrics()).containsExactly(ReportMetric.ALERT_TIMELINE);
    assertThat(report.getResults().distributionLines()).hasSize(1);
  }

  @Test
  @DisplayName("SC4 step 10 — saveFindings updates report findings text")
  void saveFindingsUpdatesReport() {
    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(1, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of()),
            sampleOfficer,
            List.of());

    when(disasterReportRepository.findById("DR-1")).thenReturn(Optional.of(report));
    when(disasterReportRepository.save(any(DisasterReport.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DisasterReport updated = controller.saveFindings("DR-1", "Key findings note");

    assertThat(updated.getFindings()).contains("Key findings note");
    verify(disasterReportRepository).save(report);
  }

  @Test
  @DisplayName("SC4 step 19 / ED2(a) — getScopedView restricts distributions to organisation")
  void getScopedViewReturnsRestrictedReport() {
    Organization org = SampleEntities.organization("ORG-1");
    DistributionLine lineOrg =
        new DistributionLine(
            District.RATNAPURA, "ORG-1", "CARE", SupplyCategory.FOOD, "packs", 100.0);
    DistributionLine lineOther =
        new DistributionLine(
            District.RATNAPURA, "ORG-2", "Red Cross", SupplyCategory.FOOD, "packs", 50.0);

    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(
                2, List.of(), Map.of(), 0, List.of(), 0, List.of(lineOrg, lineOther), Set.of()),
            sampleOfficer,
            List.of());
    report.shareWith(org);

    when(disasterReportRepository.findById("DR-1")).thenReturn(Optional.of(report));
    when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));

    ScopedReport scoped = controller.getScopedView("DR-1", "ORG-1");

    assertThat(scoped.results().distributionLines()).containsExactly(lineOrg);
    assertThat(scoped.results().matchedRecordCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("SC4 E6 / FD-20 — getScopedView throws ReportNotSharedException when not shared")
  void getScopedViewThrowsWhenNotShared() {
    Organization org = SampleEntities.organization("ORG-1");
    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of()),
            sampleOfficer,
            List.of());

    when(disasterReportRepository.findById("DR-1")).thenReturn(Optional.of(report));
    when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));

    assertThatThrownBy(() -> controller.getScopedView("DR-1", "ORG-1"))
        .isInstanceOf(ReportNotSharedException.class)
        .hasMessageContaining("not shared");
  }

  @Test
  @DisplayName("SC4 step 16 / FX-04 — getSharedReports returns headers only")
  void getSharedReportsReturnsHeadersOnly() {
    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report Title",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of()),
            sampleOfficer,
            List.of());

    when(disasterReportRepository.findSharedWith("ORG-1")).thenReturn(List.of(report));

    List<ReportHeader> headers = controller.getSharedReports("ORG-1");

    assertThat(headers).hasSize(1);
    assertThat(headers.get(0).reportId()).isEqualTo("DR-1");
    assertThat(headers.get(0).reportTitle()).isEqualTo("Report Title");
  }

  @Test
  @DisplayName("SC4 step 13 / CH-32 — exportReport produces file and audits REPORT_EXPORTED")
  void exportReportProducesFileAndAudits() {
    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of()),
            sampleOfficer,
            List.of());

    when(disasterReportRepository.findById("DR-1")).thenReturn(Optional.of(report));
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));

    ExportFile expectedFile = new ExportFile("DR-1.pdf", ExportFormat.PDF, new byte[] {1, 2, 3});
    when(pdfExporter.export(report)).thenReturn(expectedFile);

    ExportFile result = controller.exportReport("DR-1", ExportFormat.PDF, "OFF-1");

    assertThat(result).isEqualTo(expectedFile);
    ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.REPORT_EXPORTED);
  }

  @Test
  @DisplayName(
      "SC4 E2 / CH-32 — exportReport audits EXPORT_FAILED and throws ExportFailedException")
  void exportReportAuditsFailureAndThrows() {
    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of()),
            sampleOfficer,
            List.of());

    when(disasterReportRepository.findById("DR-1")).thenReturn(Optional.of(report));
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    when(pdfExporter.export(report)).thenThrow(new ExportFailedException("Disk full"));

    assertThatThrownBy(() -> controller.exportReport("DR-1", ExportFormat.PDF, "OFF-1"))
        .isInstanceOf(ExportFailedException.class)
        .hasMessageContaining("Disk full");

    ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.EXPORT_FAILED);
  }

  @Test
  @DisplayName("SC4 step 15 / E5 — shareReport adds matching organisations and identifies noData")
  void shareReportCategorisesOrganisationsAndAudits() {
    Organization orgWithData = SampleEntities.organization("ORG-1");
    Organization orgNoData = SampleEntities.organization("ORG-2");

    DistributionLine line =
        new DistributionLine(
            District.RATNAPURA, "ORG-1", "CARE", SupplyCategory.FOOD, "packs", 100.0);
    DisasterReport report =
        new DisasterReport(
            "DR-1",
            "Report",
            LocalDateTime.now(clock),
            sampleCriteria,
            new ReportResults(1, List.of(), Map.of(), 0, List.of(), 0, List.of(line), Set.of()),
            sampleOfficer,
            List.of());

    when(disasterReportRepository.findById("DR-1")).thenReturn(Optional.of(report));
    when(dmcOfficerRepository.findById("OFF-1")).thenReturn(Optional.of(sampleOfficer));
    when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(orgWithData));
    when(organizationRepository.findById("ORG-2")).thenReturn(Optional.of(orgNoData));

    ShareOutcome outcome = controller.shareReport("DR-1", List.of("ORG-1", "ORG-2"), "OFF-1");

    assertThat(outcome.sharedWith()).containsExactly(orgWithData);
    assertThat(outcome.noData()).containsExactly(orgNoData);
    assertThat(report.getSharedWith()).containsExactly(orgWithData);

    ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.REPORT_SHARED);
  }
}
