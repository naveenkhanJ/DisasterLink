package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.OccupancyRecord;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import lk.sliit.se3070.group058.disasterlink.shelter.repository.ShelterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation computing shelter occupancy over time for disaster-response reports.
 *
 * <p>Implements CH-20, CH-31 and CH-32. Reads matching shelters and their occupancy records in the
 * criteria period, computing combined current capacity and temporal occupancy points.
 */
@Component
public class OccupancyTrendCalculator implements MetricCalculator {

  private static final Logger LOG = LoggerFactory.getLogger(OccupancyTrendCalculator.class);

  private final ShelterRepository shelterRepository;

  /**
   * Creates an occupancy trend calculator with required shelter repository.
   *
   * @param shelterRepository repository for resolving matching shelters
   */
  public OccupancyTrendCalculator(ShelterRepository shelterRepository) {
    this.shelterRepository = Objects.requireNonNull(shelterRepository, "shelterRepository");
  }

  @Override
  public boolean supports(ReportMetric metric) {
    return metric == ReportMetric.SHELTER_OCCUPANCY_OVER_TIME;
  }

  @Override
  public ReportResults calculate(ReportCriteria criteria) {
    Objects.requireNonNull(criteria, "criteria");
    List<Shelter> shelters = shelterRepository.findMatching(criteria);
    if (shelters.isEmpty()) {
      return new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    }

    LocalDateTime start = criteria.periodStart().atStartOfDay();
    LocalDateTime endExclusive = criteria.periodEnd().plusDays(1).atStartOfDay();

    int combinedCapacity = shelters.stream().mapToInt(Shelter::getCapacity).sum();

    List<OccupancyPoint> points = new ArrayList<>();
    for (Shelter shelter : shelters) {
      for (OccupancyRecord record : shelter.getOccupancyRecords()) {
        LocalDateTime capturedAt = record.getCapturedAt();
        if (!capturedAt.isBefore(start) && capturedAt.isBefore(endExclusive)) {
          points.add(
              new OccupancyPoint(
                  shelter.getShelterId(),
                  shelter.getDistrict(),
                  capturedAt,
                  record.getOccupantCount(),
                  shelter.getCapacity()));
        }
      }
    }

    points.sort(Comparator.comparing(OccupancyPoint::capturedAt));

    int matchedCount = points.size();
    LOG.info(
        "Occupancy trend calculated: {} matching shelters, {} occupancy points, combined capacity {}",
        shelters.size(),
        matchedCount,
        combinedCapacity);

    return new ReportResults(
        matchedCount, List.of(), Map.of(), 0, points, combinedCapacity, List.of(), Set.of());
  }
}
