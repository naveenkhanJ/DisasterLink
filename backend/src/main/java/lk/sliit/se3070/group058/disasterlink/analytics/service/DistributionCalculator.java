package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistributionLine;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReliefSupply;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ResourceDistributionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation computing the resource distribution metric for disaster-response reports.
 *
 * <p>Implements CH-20, CH-31 and CH-32. Reads matching resource distributions and transforms them
 * into distribution lines detailing destination district, owner, category, quantity and unit.
 */
@Component
public class DistributionCalculator implements MetricCalculator {

  private static final Logger LOG = LoggerFactory.getLogger(DistributionCalculator.class);
  private static final String DEFAULT_PRIVATE_OWNER_NAME = "Private individual";

  private final ResourceDistributionRepository resourceDistributionRepository;

  /**
   * Creates a distribution calculator with required resource distribution repository.
   *
   * @param resourceDistributionRepository repository for querying matching distributions
   */
  public DistributionCalculator(ResourceDistributionRepository resourceDistributionRepository) {
    this.resourceDistributionRepository =
        Objects.requireNonNull(resourceDistributionRepository, "resourceDistributionRepository");
  }

  @Override
  public boolean supports(ReportMetric metric) {
    return metric == ReportMetric.RESOURCE_DISTRIBUTION;
  }

  @Override
  public ReportResults calculate(ReportCriteria criteria) {
    Objects.requireNonNull(criteria, "criteria");
    List<ResourceDistribution> distributions =
        resourceDistributionRepository.findMatching(criteria);
    if (distributions.isEmpty()) {
      return new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());
    }

    List<DistributionLine> lines = new ArrayList<>();
    for (ResourceDistribution rd : distributions) {
      ReliefSupply supply = rd.getReliefSupply();
      String ownerId =
          supply.getDonorOrganization().map(Organization::getOrganizationId).orElse(null);
      String ownerName =
          supply
              .getDonorOrganization()
              .map(Organization::getName)
              .orElse(DEFAULT_PRIVATE_OWNER_NAME);

      lines.add(
          new DistributionLine(
              rd.getDestinationDistrict(),
              ownerId,
              ownerName,
              supply.getCategory(),
              supply.getUnit(),
              rd.getQuantity()));
    }

    int matchedCount = lines.size();
    LOG.info("Distribution calculated: {} distribution lines", matchedCount);

    return new ReportResults(matchedCount, List.of(), Map.of(), 0, List.of(), 0, lines, Set.of());
  }
}
