package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.repository.NotificationRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.WarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation computing the citizens reached metric for disaster-response reports.
 *
 * <p>Implements CH-20, CH-31 and CH-32. Reads matching warnings and aggregates reach by channel and
 * distinct citizens reached via {@link NotificationRepository}.
 */
@Component
public class CitizensReachedCalculator implements MetricCalculator {

  private static final Logger LOG = LoggerFactory.getLogger(CitizensReachedCalculator.class);

  private final WarningRepository warningRepository;
  private final NotificationRepository notificationRepository;

  /**
   * Creates a citizens reached calculator with required repositories.
   *
   * @param warningRepository repository for resolving matching warnings
   * @param notificationRepository repository for querying notification counts
   */
  public CitizensReachedCalculator(
      WarningRepository warningRepository, NotificationRepository notificationRepository) {
    this.warningRepository = Objects.requireNonNull(warningRepository, "warningRepository");
    this.notificationRepository =
        Objects.requireNonNull(notificationRepository, "notificationRepository");
  }

  @Override
  public boolean supports(ReportMetric metric) {
    return metric == ReportMetric.CITIZENS_REACHED;
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
    Map<NotificationChannel, Integer> byChannel =
        notificationRepository.countCitizensReachedByChannel(warningIds, criteria.districts());
    int totalReached =
        notificationRepository.countCitizensReached(warningIds, criteria.districts());
    int matchedCount = notificationRepository.countByWarnings(warningIds);

    LOG.info(
        "Citizens reached calculated: {} notifications matched, {} distinct citizens reached",
        matchedCount,
        totalReached);

    return new ReportResults(
        matchedCount, List.of(), byChannel, totalReached, List.of(), 0, List.of(), Set.of());
  }
}
