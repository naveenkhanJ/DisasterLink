package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access for warnings (model dictionary section 6, WarningRepository).
 *
 * <p>The period of a {@link ReportCriteria} is whole days: from {@code periodStart} at 00:00,
 * inclusive, to the day after {@code periodEnd} at 00:00, exclusive (a Group_058 assumption).
 */
@Transactional(readOnly = true)
public interface WarningRepository extends JpaRepository<Warning, String> {

  /**
   * Finds the warnings that are active at a time and cover a district: issued at or before the
   * time, expiring after it, with the district among {@code coveredDistricts()}. The covered
   * districts include those of the target river basins, which JPQL cannot reach, so a helper query
   * selects the active warnings and Java filters them (MD section 6, {@code findActiveByDistrict}).
   *
   * @param district the district
   * @param now the time to judge at
   * @return the matching warnings
   */
  @Transactional(readOnly = true)
  default List<Warning> findActiveByDistrict(District district, LocalDateTime now) {
    return findActiveAt(now).stream()
        .filter(warning -> warning.coveredDistricts().contains(district))
        .toList();
  }

  /**
   * Helper of {@code findActiveByDistrict}; controls never call it. Finds the warnings issued at or
   * before a time and expiring after it.
   *
   * @param now the time to judge at
   * @return the active warnings
   */
  @Query("select w from Warning w where w.issuedTime <= :now and w.expiryTime > :now")
  List<Warning> findActiveAt(@Param("now") LocalDateTime now);

  /**
   * Finds the active warning of a hazard, issued at or before a time and expiring after it. The
   * constraint of A1 allows at most one (MD section 6, {@code findActiveByHazard}).
   *
   * @param hazardId the identifier of the hazard
   * @param now the time to judge at
   * @return the active warning, or empty if the hazard has none
   */
  @Query(
      "select w from Warning w where w.hazard.hazardId = :hazardId"
          + " and w.issuedTime <= :now and w.expiryTime > :now")
  Optional<Warning> findActiveByHazard(
      @Param("hazardId") String hazardId, @Param("now") LocalDateTime now);

  /**
   * Finds the warnings that match report criteria: issued in the period, for hazards of the
   * criteria's type when it is set, and covering at least one district of the criteria. A helper
   * query selects by period and type, and Java keeps the warnings whose {@code coveredDistricts()}
   * meet the districts (MD section 6, {@code findMatching}; PR6, R3G-04).
   *
   * <p>Called after {@code AnalyticsController.validateCriteria}; a criteria without a period is a
   * programming error.
   *
   * @param criteria the report criteria
   * @return the matching warnings
   * @throws NullPointerException if the criteria has no period (a call before validation)
   */
  @Transactional(readOnly = true)
  default List<Warning> findMatching(ReportCriteria criteria) {
    return findIssuedInPeriod(
            criteria.periodStart().atStartOfDay(),
            criteria.periodEnd().plusDays(1).atStartOfDay(),
            criteria.hazardType() == null,
            criteria.hazardType())
        .stream()
        .filter(warning -> !Collections.disjoint(warning.coveredDistricts(), criteria.districts()))
        .toList();
  }

  /**
   * Helper of {@code findMatching}; controls never call it. Its period comes from criteria that
   * passed {@code AnalyticsController.validateCriteria}.
   *
   * @param start the start of the period, inclusive
   * @param endExclusive the end of the period, exclusive
   * @param anyType {@code true} to ignore the hazard type
   * @param type the hazard type, used when {@code anyType} is {@code false}
   * @return the warnings issued in the period
   */
  @Query(
      "select w from Warning w where w.issuedTime >= :start and w.issuedTime < :endExclusive"
          + " and (:anyType = true or w.hazard.type = :type) order by w.issuedTime")
  List<Warning> findIssuedInPeriod(
      @Param("start") LocalDateTime start,
      @Param("endExclusive") LocalDateTime endExclusive,
      @Param("anyType") boolean anyType,
      @Param("type") HazardType type);
}
