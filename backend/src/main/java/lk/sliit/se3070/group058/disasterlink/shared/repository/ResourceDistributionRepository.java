package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ResourceDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to resource distributions (model dictionary section 6). The period of a {@link
 * ReportCriteria} is whole days: from {@code periodStart} at 00:00, inclusive, to the day after
 * {@code periodEnd} at 00:00, exclusive (a Group_058 assumption).
 */
public interface ResourceDistributionRepository
    extends JpaRepository<ResourceDistribution, String> {

  /**
   * Finds the distributions that match report criteria: to a district of the criteria, dated in the
   * period, and, when the criteria set a hazard type, linked to a hazard of that type (a
   * distribution linked to no hazard is then excluded; MD section 6, criteria rule).
   *
   * @param criteria the report criteria
   * @return the matching distributions
   */
  @Transactional(readOnly = true)
  default List<ResourceDistribution> findMatching(ReportCriteria criteria) {
    return findMatchingInPeriod(
        criteria.districts(),
        criteria.hazardType() == null,
        criteria.hazardType(),
        criteria.periodStart().atStartOfDay(),
        criteria.periodEnd().plusDays(1).atStartOfDay());
  }

  /**
   * Helper of {@code findMatching}; controls never call it.
   *
   * @param districts the destination districts
   * @param anyType {@code true} to ignore the hazard type
   * @param type the hazard type, used when {@code anyType} is {@code false}
   * @param start the start of the period, inclusive
   * @param endExclusive the end of the period, exclusive
   * @return the matching distributions
   */
  @Query(
      "select rd from ResourceDistribution rd left join rd.hazard h"
          + " where rd.destinationDistrict in :districts"
          + " and rd.distributionDate >= :start and rd.distributionDate < :endExclusive"
          + " and (:anyType = true or h.type = :type) order by rd.distributionDate")
  List<ResourceDistribution> findMatchingInPeriod(
      @Param("districts") Set<District> districts,
      @Param("anyType") boolean anyType,
      @Param("type") HazardType type,
      @Param("start") LocalDateTime start,
      @Param("endExclusive") LocalDateTime endExclusive);
}
