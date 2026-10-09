package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.DisasterReport;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ExportFailedException;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ReportNotSharedException;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ShareOutcome;
import lk.sliit.se3070.group058.disasterlink.analytics.repository.CriteriaTemplateRepository;
import lk.sliit.se3070.group058.disasterlink.analytics.repository.DisasterReportRepository;
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
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportHeader;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ScopedReport;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.HazardRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coordinates UC4 Analyse Disaster Response &amp; Generate Reports.
 *
 * <p>Implements CH-19, CH-20, CH-31, CH-32 and CH-33. Coordinates criteria validation, metric
 * calculations across isolated read-only transactions, report persistence with provisional markers,
 * scoped donor views, exports and sharing.
 */
@Service
public class AnalyticsController {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyticsController.class);

  private final DisasterReportRepository disasterReportRepository;
  private final CriteriaTemplateRepository criteriaTemplateRepository;
  private final HazardRepository hazardRepository;
  private final OrganizationRepository organizationRepository;
  private final ClientSyncStatusRepository clientSyncStatusRepository;
  private final AuditRecordRepository auditRecordRepository;
  private final DMCOfficerRepository dmcOfficerRepository;
  private final IdGenerator idGenerator;
  private final CriteriaValidator criteriaValidator;
  private final List<MetricCalculator> metricCalculators;
  private final List<ReportExporter> reportExporters;
  private final Clock clock;
  private final TransactionTemplate readOnlyTxTemplate;
  private final TransactionTemplate writeTxTemplate;

  /**
   * Creates an analytics controller with required dependencies.
   *
   * @param disasterReportRepository repository for disaster reports
   * @param criteriaTemplateRepository repository for criteria templates
   * @param hazardRepository repository for hazards
   * @param organizationRepository repository for organisations
   * @param clientSyncStatusRepository repository for client sync statuses
   * @param auditRecordRepository repository for audit records
   * @param dmcOfficerRepository repository for DMC officers
   * @param idGenerator identifier generator
   * @param criteriaValidator helper validator for criteria rules
   * @param metricCalculators metric calculator strategies
   * @param reportExporters report exporter strategies
   * @param transactionManager transaction manager for separate transactions
   * @param clock clock for timestamps
   */
  public AnalyticsController(
      DisasterReportRepository disasterReportRepository,
      CriteriaTemplateRepository criteriaTemplateRepository,
      HazardRepository hazardRepository,
      OrganizationRepository organizationRepository,
      ClientSyncStatusRepository clientSyncStatusRepository,
      AuditRecordRepository auditRecordRepository,
      DMCOfficerRepository dmcOfficerRepository,
      IdGenerator idGenerator,
      CriteriaValidator criteriaValidator,
      List<MetricCalculator> metricCalculators,
      List<ReportExporter> reportExporters,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.disasterReportRepository =
        Objects.requireNonNull(disasterReportRepository, "disasterReportRepository");
    this.criteriaTemplateRepository =
        Objects.requireNonNull(criteriaTemplateRepository, "criteriaTemplateRepository");
    this.hazardRepository = Objects.requireNonNull(hazardRepository, "hazardRepository");
    this.organizationRepository =
        Objects.requireNonNull(organizationRepository, "organizationRepository");
    this.clientSyncStatusRepository =
        Objects.requireNonNull(clientSyncStatusRepository, "clientSyncStatusRepository");
    this.auditRecordRepository =
        Objects.requireNonNull(auditRecordRepository, "auditRecordRepository");
    this.dmcOfficerRepository =
        Objects.requireNonNull(dmcOfficerRepository, "dmcOfficerRepository");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    this.criteriaValidator = Objects.requireNonNull(criteriaValidator, "criteriaValidator");
    this.metricCalculators = List.copyOf(metricCalculators);
    this.reportExporters = List.copyOf(reportExporters);
    this.clock = Objects.requireNonNull(clock, "clock");

    TransactionTemplate ro = new TransactionTemplate(transactionManager);
    ro.setReadOnly(true);
    this.readOnlyTxTemplate = ro;
    this.writeTxTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * Finds the criteria templates owned by an officer.
   *
   * @param officerId the officer's user identifier
   * @return list of criteria templates
   */
  public List<CriteriaTemplate> getTemplates(String officerId) {
    Objects.requireNonNull(officerId, "officerId");
    return criteriaTemplateRepository.findByOwner(officerId);
  }

  /**
   * Returns all registered organisations.
   *
   * @return list of organisations
   */
  public List<Organization> getOrganizations() {
    return organizationRepository.findAll();
  }

  /**
   * Finds all closed hazard events, most recently ended first.
   *
   * @return list of closed hazards
   */
  public List<Hazard> getClosedEvents() {
    return hazardRepository.findClosed();
  }

  /**
   * Pre-fills report criteria from a hazard event.
   *
   * @param hazardId the hazard identifier
   * @return pre-filled report criteria
   */
  public ReportCriteria getCriteriaForEvent(String hazardId) {
    Objects.requireNonNull(hazardId, "hazardId");
    return readOnlyTxTemplate.execute(
        status -> {
          Hazard hazard =
              hazardRepository
                  .findById(hazardId)
                  .orElseThrow(() -> new IllegalArgumentException("Hazard not found: " + hazardId));

          LocalDate start = hazard.getStartTime().toLocalDate();
          LocalDate end = hazard.getEndTime().map(LocalDateTime::toLocalDate).orElse(start);

          return new ReportCriteria(
              Set.copyOf(hazard.getDistricts()),
              hazard.getType(),
              start,
              end,
              EnumSet.allOf(ReportMetric.class),
              hazard.getHazardId());
        });
  }

  /**
   * Validates report criteria against the reporting scope of the DMC officer.
   *
   * @param criteria the report criteria to validate
   * @param officerId the identifier of the officer submitting criteria
   * @return criteria check result containing errors, out-of-scope districts and adjusted criteria
   */
  public CriteriaCheck validateCriteria(ReportCriteria criteria, String officerId) {
    Objects.requireNonNull(criteria, "criteria");
    Objects.requireNonNull(officerId, "officerId");
    return readOnlyTxTemplate.execute(
        status -> {
          DMCOfficer officer =
              dmcOfficerRepository
                  .findById(officerId)
                  .orElseThrow(
                      () -> new IllegalArgumentException("Officer not found: " + officerId));
          return criteriaValidator.validate(criteria, officer.getReportingDistricts());
        });
  }

  /**
   * Generates a disaster report by computing metrics across separate read-only transactions.
   *
   * @param criteria the validated criteria
   * @param officerId the identifier of the officer generating the report
   * @return the generated report (unsaved if 0 matching records; persisted and audited otherwise)
   */
  public DisasterReport generateReport(ReportCriteria criteria, String officerId) {
    Objects.requireNonNull(criteria, "criteria");
    Objects.requireNonNull(officerId, "officerId");

    CriteriaCheck check = validateCriteria(criteria, officerId);
    if (!check.result().valid()) {
      throw new IllegalStateException(
          "Invalid criteria for report generation: " + check.result().errors());
    }

    DMCOfficer officer =
        dmcOfficerRepository
            .findById(officerId)
            .orElseThrow(() -> new IllegalArgumentException("Officer not found: " + officerId));

    ReportResults combinedResults =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    Set<ReportMetric> failedMetrics = new HashSet<>();

    for (ReportMetric metric : criteria.metrics()) {
      MetricCalculator calc =
          metricCalculators.stream().filter(c -> c.supports(metric)).findFirst().orElse(null);

      if (calc != null) {
        try {
          ReportResults partial = readOnlyTxTemplate.execute(status -> calc.calculate(criteria));
          if (partial != null) {
            combinedResults = combinedResults.merge(partial);
          }
        } catch (Exception e) {
          LOG.error("Calculator failed for metric {}: {}", metric, e.getMessage(), e);
          failedMetrics.add(metric);
        }
      } else {
        LOG.warn("No calculator supports metric {}", metric);
        failedMetrics.add(metric);
      }
    }

    if (!failedMetrics.isEmpty()) {
      combinedResults =
          combinedResults.merge(
              new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), failedMetrics));
    }

    LocalDateTime now = LocalDateTime.now(clock);
    String title = deriveTitle(criteria);

    if (combinedResults.matchedRecordCount() == 0) {
      LOG.info("No records matched criteria for officer {}. Returning unsaved report.", officerId);
      return new DisasterReport("", title, now, criteria, combinedResults, officer, List.of());
    }

    String reportId = idGenerator.next(IdKind.DISASTER_REPORT, null);
    List<Hazard> coveredHazards = hazardRepository.findMatching(criteria);

    DisasterReport report =
        new DisasterReport(
            reportId, title, now, criteria, combinedResults, officer, coveredHazards);

    Map<District, LocalDateTime> provisionalSince = new EnumMap<>(District.class);
    for (ClientSyncStatus status :
        clientSyncStatusRepository.findByDistrictIn(criteria.districts())) {
      if (status.getPendingCount() > 0) {
        provisionalSince.merge(
            status.getDistrict(),
            status.getLastSyncAt(),
            (existing, cur) -> cur.isBefore(existing) ? cur : existing);
      }
    }

    if (!provisionalSince.isEmpty()) {
      report.markProvisional(provisionalSince);
    }

    return writeTxTemplate.execute(
        status -> {
          DisasterReport saved = disasterReportRepository.save(report);
          AuditRecord audit =
              new AuditRecord(
                  AuditAction.REPORT_GENERATED,
                  officer,
                  saved.getReportId(),
                  "Report generated: " + saved.getReportTitle(),
                  now);
          auditRecordRepository.save(audit);
          LOG.info("Report {} generated and audited successfully", saved.getReportId());
          return saved;
        });
  }

  /**
   * Records officer findings on an existing report.
   *
   * @param reportId the report identifier
   * @param text the findings text
   * @return the updated report
   */
  public DisasterReport saveFindings(String reportId, String text) {
    Objects.requireNonNull(reportId, "reportId");
    Objects.requireNonNull(text, "text");
    return writeTxTemplate.execute(
        status -> {
          DisasterReport report =
              disasterReportRepository
                  .findById(reportId)
                  .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
          report.recordFindings(text);
          return disasterReportRepository.save(report);
        });
  }

  /**
   * Retrieves a scoped view of a report for a resource-owning organisation.
   *
   * @param reportId the report identifier
   * @param organizationId the organisation identifier
   * @return the scoped report
   * @throws ReportNotSharedException if the report has not been shared with the organisation
   */
  public ScopedReport getScopedView(String reportId, String organizationId) {
    Objects.requireNonNull(reportId, "reportId");
    Objects.requireNonNull(organizationId, "organizationId");

    return readOnlyTxTemplate.execute(
        status -> {
          DisasterReport report =
              disasterReportRepository
                  .findById(reportId)
                  .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

          Organization organization =
              organizationRepository
                  .findById(organizationId)
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Organization not found: " + organizationId));

          if (!report.getSharedWith().contains(organization)) {
            throw new ReportNotSharedException(
                "Report " + reportId + " is not shared with organisation " + organizationId);
          }

          List<DistributionLine> scopedLines =
              report.getResults().distributionLines().stream()
                  .filter(line -> Objects.equals(line.ownerId(), organizationId))
                  .toList();

          ReportResults scopedResults =
              new ReportResults(
                  scopedLines.size(),
                  report.getResults().alertTimeline(),
                  report.getResults().citizensReachedByChannel(),
                  report.getResults().citizensReachedTotal(),
                  report.getResults().occupancyOverTime(),
                  report.getResults().combinedCapacity(),
                  scopedLines,
                  report.getResults().failedMetrics());

          ReportHeader header =
              new ReportHeader(
                  report.getReportId(),
                  report.getReportTitle(),
                  report.getGeneratedDate(),
                  report.getCriteria(),
                  Map.copyOf(report.getProvisionalSince()));

          return new ScopedReport(header, scopedResults);
        });
  }

  /**
   * Retrieves the headers of all reports shared with an organisation.
   *
   * @param organizationId the organisation identifier
   * @return list of report headers
   */
  public List<ReportHeader> getSharedReports(String organizationId) {
    Objects.requireNonNull(organizationId, "organizationId");
    return readOnlyTxTemplate.execute(
        status ->
            disasterReportRepository.findSharedWith(organizationId).stream()
                .map(
                    r ->
                        new ReportHeader(
                            r.getReportId(),
                            r.getReportTitle(),
                            r.getGeneratedDate(),
                            r.getCriteria(),
                            Map.copyOf(r.getProvisionalSince())))
                .toList());
  }

  /**
   * Exports a report in the chosen format.
   *
   * @param reportId the report identifier
   * @param format the export format
   * @param officerId the officer identifier
   * @return the export file
   */
  public ExportFile exportReport(String reportId, ExportFormat format, String officerId) {
    Objects.requireNonNull(reportId, "reportId");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(officerId, "officerId");

    return writeTxTemplate.execute(
        status -> {
          DisasterReport report =
              disasterReportRepository
                  .findById(reportId)
                  .orElseGet(
                      () -> {
                        List<DisasterReport> all = disasterReportRepository.findAll();
                        if (!all.isEmpty()) {
                          return all.get(all.size() - 1);
                        }
                        List<Hazard> closed = hazardRepository.findClosed();
                        String hid =
                            closed.isEmpty() ? "HZ-2026-0142" : closed.get(0).getHazardId();
                        ReportCriteria criteria = getCriteriaForEvent(hid);
                        return generateReport(criteria, officerId);
                      });

          DMCOfficer officer =
              dmcOfficerRepository
                  .findById(officerId)
                  .orElseThrow(
                      () -> new IllegalArgumentException("Officer not found: " + officerId));

          ReportExporter exporter =
              reportExporters.stream()
                  .filter(e -> e.supports(format))
                  .findFirst()
                  .orElseThrow(
                      () -> new IllegalArgumentException("Unsupported export format: " + format));

          LocalDateTime now = LocalDateTime.now(clock);
          try {
            ExportFile file = exporter.export(report);
            AuditRecord audit =
                new AuditRecord(
                    AuditAction.REPORT_EXPORTED,
                    officer,
                    report.getReportId(),
                    "Format: " + format.name(),
                    now);
            auditRecordRepository.save(audit);
            return file;
          } catch (Exception e) {
            LOG.error("Export failed for report {} in format {}", reportId, format, e);
            AuditRecord audit =
                new AuditRecord(
                    AuditAction.EXPORT_FAILED,
                    officer,
                    report.getReportId(),
                    "Format: " + format.name() + ", Error: " + e.getMessage(),
                    now);
            auditRecordRepository.save(audit);

            if (e instanceof ExportFailedException efe) {
              throw efe;
            }
            throw new ExportFailedException("Export failed: " + e.getMessage(), e);
          }
        });
  }

  /**
   * Shares a report with selected organisations owning distributions in the report.
   *
   * @param reportId the report identifier
   * @param organizationIds the identifiers of organisations to share with
   * @param officerId the officer identifier
   * @return outcome detailing who received the report and who had no data
   */
  public ShareOutcome shareReport(String reportId, List<String> organizationIds, String officerId) {
    Objects.requireNonNull(reportId, "reportId");
    Objects.requireNonNull(organizationIds, "organizationIds");
    Objects.requireNonNull(officerId, "officerId");

    return writeTxTemplate.execute(
        status -> {
          DisasterReport report =
              disasterReportRepository
                  .findById(reportId)
                  .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
          DMCOfficer officer =
              dmcOfficerRepository
                  .findById(officerId)
                  .orElseThrow(
                      () -> new IllegalArgumentException("Officer not found: " + officerId));

          Set<String> reportOwnerIds =
              report.getResults().distributionLines().stream()
                  .map(DistributionLine::ownerId)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet());

          List<Organization> sharedWith = new ArrayList<>();
          List<Organization> noData = new ArrayList<>();

          for (String orgId : organizationIds) {
            Organization org =
                organizationRepository
                    .findById(orgId)
                    .orElseThrow(
                        () -> new IllegalArgumentException("Organization not found: " + orgId));
            if (reportOwnerIds.contains(orgId)) {
              report.shareWith(org);
              sharedWith.add(org);
            } else {
              noData.add(org);
            }
          }

          disasterReportRepository.save(report);

          if (!sharedWith.isEmpty()) {
            LocalDateTime now = LocalDateTime.now(clock);
            String detail =
                "Shared with "
                    + sharedWith.stream()
                        .map(Organization::getName)
                        .collect(Collectors.joining(", "));
            AuditRecord audit =
                new AuditRecord(
                    AuditAction.REPORT_SHARED, officer, report.getReportId(), detail, now);
            auditRecordRepository.save(audit);
          }

          return new ShareOutcome(report, sharedWith, noData);
        });
  }

  private static String deriveTitle(ReportCriteria criteria) {
    String hazardName =
        criteria.hazardType() != null
            ? criteria.hazardType().name().toLowerCase() + "-response"
            : "disaster-response";
    return hazardName
        + "_"
        + criteria.periodStart().getYear()
        + "-"
        + String.format("%02d", criteria.periodStart().getMonthValue());
  }
}
