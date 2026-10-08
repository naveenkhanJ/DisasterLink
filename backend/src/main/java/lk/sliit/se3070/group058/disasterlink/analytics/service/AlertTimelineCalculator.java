package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AlertTimelineEntry;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.TimelineEvent;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.repository.AuditRecordRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation computing the alert timeline metric for disaster-response reports.
 *
 * <p>Implements CH-20, CH-31 and CH-32. Reads matching warnings and their escalation audit records,
 * producing ISSUED and ESCALATED timeline entries chronologically.
 */
@Component
public class AlertTimelineCalculator implements MetricCalculator {

  private static final Logger LOG = LoggerFactory.getLogger(AlertTimelineCalculator.class);

  private final WarningRepository warningRepository;
  private final AuditRecordRepository auditRecordRepository;

  /**
   * Creates an alert timeline calculator with required repositories.
   *
   * @param warningRepository repository for resolving matching warnings
   * @param auditRecordRepository repository for reading warning escalation audit records
   */
  public AlertTimelineCalculator(
      WarningRepository warningRepository, AuditRecordRepository auditRecordRepository) {
    this.warningRepository = Objects.requireNonNull(warningRepository, "warningRepository");
    this.auditRecordRepository =
        Objects.requireNonNull(auditRecordRepository, "auditRecordRepository");
  }

  @Override
  public boolean supports(ReportMetric metric) {
    return metric == ReportMetric.ALERT_TIMELINE;
  }

  @Override
  public ReportResults calculate(ReportCriteria criteria) {
    Objects.requireNonNull(criteria, "criteria");
    List<Warning> warnings = warningRepository.findMatching(criteria);
    if (warnings.isEmpty()) {
      return new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    }

    Set<String> warningIds =
        warnings.stream().map(Warning::getWarningId).collect(Collectors.toSet());
    List<AuditRecord> escalations = auditRecordRepository.findEscalations(warningIds);

    Map<String, List<AuditRecord>> escalationsByWarning =
        escalations.stream().collect(Collectors.groupingBy(AuditRecord::getSubjectId));

    List<AlertTimelineEntry> entries = new ArrayList<>();
    for (Warning warning : warnings) {
      List<AuditRecord> warningEscalations =
          escalationsByWarning.getOrDefault(warning.getWarningId(), List.of());

      WarningLevel initialLevel;
      if (!warningEscalations.isEmpty()) {
        AuditRecord earliest = warningEscalations.get(0);
        initialLevel = parseOldLevel(earliest.getDetail().orElse(null), warning.getWarningLevel());
      } else {
        initialLevel = warning.getWarningLevel();
      }

      entries.add(
          new AlertTimelineEntry(
              warning.getWarningId(),
              warning.getIssuedTime(),
              initialLevel,
              warning.coveredDistricts(),
              TimelineEvent.ISSUED));

      for (AuditRecord esc : warningEscalations) {
        WarningLevel newLevel =
            parseNewLevel(esc.getDetail().orElse(null), warning.getWarningLevel());
        entries.add(
            new AlertTimelineEntry(
                warning.getWarningId(),
                esc.getPerformedAt(),
                newLevel,
                warning.coveredDistricts(),
                TimelineEvent.ESCALATED));
      }
    }

    entries.sort(Comparator.comparing(AlertTimelineEntry::issuedTime));

    int matchedCount = warnings.size();
    LOG.info(
        "Alert timeline calculated: {} warnings, {} timeline entries",
        matchedCount,
        entries.size());

    return new ReportResults(matchedCount, entries, Map.of(), 0, List.of(), 0, List.of(), Set.of());
  }

  private static WarningLevel parseOldLevel(String detail, WarningLevel fallback) {
    if (detail == null) {
      return fallback;
    }
    String[] parts = detail.split("→|->");
    if (parts.length > 0 && !parts[0].isBlank()) {
      try {
        return WarningLevel.valueOf(parts[0].trim());
      } catch (IllegalArgumentException e) {
        LOG.warn("Could not parse old warning level from detail: {}", detail);
      }
    }
    return fallback;
  }

  private static WarningLevel parseNewLevel(String detail, WarningLevel fallback) {
    if (detail == null) {
      return fallback;
    }
    String[] parts = detail.split("→|->");
    if (parts.length > 1 && !parts[1].isBlank()) {
      try {
        return WarningLevel.valueOf(parts[1].trim());
      } catch (IllegalArgumentException e) {
        LOG.warn("Could not parse new warning level from detail: {}", detail);
      }
    }
    return fallback;
  }
}
