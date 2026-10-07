package lk.sliit.se3070.group058.disasterlink.shelter.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shelter.domain.Shelter;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access for shelters (model dictionary section 6, ShelterRepository). The period of a {@link
 * ReportCriteria} is whole days: from {@code periodStart} at 00:00, inclusive, to the day after
 * {@code periodEnd} at 00:00, exclusive (a Group_058 assumption).
 *
 * <p>The finders that the UC3 JSON endpoints read ({@code findByDistrict}, {@code findWithSpace}
 * and {@code findById}) fetch the operating organisation with an entity graph, because the
 * endpoints map {@code Shelter.operator} after the control's transaction has ended and open-in-view
 * is off (standards section 12.4, "Lazy reads"; P155 X2).
 */
public interface ShelterRepository extends JpaRepository<Shelter, String> {

  /** The association that the UC3 endpoints read after the transaction (P155 X2). */
  String OPERATOR = "operator";

  /**
   * Finds the shelters of a district (MD section 6, {@code findByDistrict}; derived from the name).
   *
   * @param district the district
   * @return the shelters of the district
   */
  @EntityGraph(attributePaths = OPERATOR)
  List<Shelter> findByDistrict(District district);

  /**
   * Finds a shelter by its identifier, with its operating organisation loaded (P155 X2).
   *
   * @param shelterId the identifier
   * @return the shelter, or empty if there is none
   */
  @Override
  @EntityGraph(attributePaths = OPERATOR)
  Optional<Shelter> findById(String shelterId);

  /**
   * Finds the open shelters of a district that can take a number of people: status OPEN and
   * capacity minus current occupancy at least {@code needed}, the one with most space first (MD
   * section 6, {@code findWithSpace}).
   *
   * @param district the district
   * @param needed the number of people to accommodate
   * @return the shelters with enough space
   */
  @EntityGraph(attributePaths = OPERATOR)
  @Query(
      "select s from Shelter s where s.district = :district and s.status = OPEN"
          + " and s.capacity - s.currentOccupancy >= :needed"
          + " order by s.capacity - s.currentOccupancy desc")
  List<Shelter> findWithSpace(@Param("district") District district, @Param("needed") int needed);

  /**
   * Finds the shelters that match report criteria: in a district of the criteria and with at least
   * one occupancy record captured in the period. When the criteria set a hazard type, only shelters
   * linked (A9) to a warning whose hazard has that type are kept, together with shelters linked to
   * no warning (MD section 6, {@code findMatching}; criteria rule; P62 FX-16).
   *
   * <p>Called after {@code AnalyticsController.validateCriteria}; a criteria without a period is a
   * programming error.
   *
   * @param criteria the report criteria
   * @return the matching shelters
   * @throws NullPointerException if the criteria has no period (a call before validation)
   */
  @Transactional(readOnly = true)
  default List<Shelter> findMatching(ReportCriteria criteria) {
    return findMatchingInPeriod(
        criteria.districts(),
        criteria.periodStart().atStartOfDay(),
        criteria.periodEnd().plusDays(1).atStartOfDay(),
        criteria.hazardType() == null,
        criteria.hazardType());
  }

  /**
   * Helper of {@code findMatching}; controls never call it. Its period comes from criteria that
   * passed {@code AnalyticsController.validateCriteria}.
   *
   * @param districts the districts of the shelters
   * @param start the start of the period, inclusive
   * @param endExclusive the end of the period, exclusive
   * @param anyType {@code true} to ignore the hazard type
   * @param type the hazard type, used when {@code anyType} is {@code false}
   * @return the matching shelters
   */
  @Query(
      "select s from Shelter s where s.district in :districts"
          + " and exists (select o from s.occupancyRecords o"
          + " where o.capturedAt >= :start and o.capturedAt < :endExclusive)"
          + " and (:anyType = true or s.warnings is empty"
          + " or exists (select w from s.warnings w where w.hazard.type = :type))"
          + " order by s.shelterId")
  List<Shelter> findMatchingInPeriod(
      @Param("districts") Set<District> districts,
      @Param("start") LocalDateTime start,
      @Param("endExclusive") LocalDateTime endExclusive,
      @Param("anyType") boolean anyType,
      @Param("type") HazardType type);
}
