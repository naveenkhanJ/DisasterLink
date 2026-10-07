package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoBox;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoDistance;
import lk.sliit.se3070.group058.disasterlink.shared.domain.GeoPoint;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access for hazards (model dictionary section 6, HazardRepository).
 *
 * <p>The period of a {@link ReportCriteria} is whole days: from {@code periodStart} at 00:00,
 * inclusive, to the day after {@code periodEnd} at 00:00, exclusive (a Group_058 assumption).
 */
@Transactional(readOnly = true)
public interface HazardRepository extends JpaRepository<Hazard, String> {

  /** The radius, in kilometres, within which a hazard counts as near a location (PR5). */
  double NEAR_RADIUS_KM = 5.0;

  /**
   * Finds the hazards a warning can be issued for: those with the status ACTIVE, oldest start first
   * (MD section 6, {@code findAssessable}).
   *
   * @return the active hazards
   */
  @Query("select h from Hazard h where h.status = ACTIVE order by h.startTime")
  List<Hazard> findAssessable();

  /**
   * Finds the closed hazards, most recently ended first (MD section 6, {@code findClosed}).
   *
   * @return the closed hazards
   */
  @Query("select h from Hazard h where h.status = CLOSED order by h.endTime desc")
  List<Hazard> findClosed();

  /**
   * Finds the ACTIVE hazards of a type whose districts contain a district or whose location is
   * within {@link #NEAR_RADIUS_KM} of a location (MD section 6, {@code findActiveNear}; PR5). The
   * candidates come from two helper queries, the second restricted to a bounding box, and the
   * radius is then checked with {@link GeoDistance#haversineKm}. A hazard found by both is returned
   * once.
   *
   * @param location the location to search around
   * @param type the hazard type
   * @param district the district a hazard may list instead of being near
   * @return the matching hazards, those listing the district first
   */
  @Transactional(readOnly = true)
  default List<Hazard> findActiveNear(GeoPoint location, HazardType type, District district) {
    GeoBox box = GeoBox.around(location, NEAR_RADIUS_KM);
    Set<Hazard> found = new LinkedHashSet<>(findActiveByTypeAndDistrict(type, district));
    for (Hazard hazard :
        findActiveByTypeInBox(
            type, box.minLatitude(), box.maxLatitude(), box.minLongitude(), box.maxLongitude())) {
      if (GeoDistance.haversineKm(location, hazard.getLocation()) <= NEAR_RADIUS_KM) {
        found.add(hazard);
      }
    }
    return new ArrayList<>(found);
  }

  /**
   * Helper of {@code findActiveNear}; controls never call it. Finds the ACTIVE hazards of a type
   * that list a district.
   *
   * @param type the hazard type
   * @param district the district
   * @return the matching hazards
   */
  @Query(
      "select h from Hazard h where h.status = ACTIVE and h.type = :type"
          + " and :district member of h.districts order by h.startTime")
  List<Hazard> findActiveByTypeAndDistrict(
      @Param("type") HazardType type, @Param("district") District district);

  /**
   * Helper of {@code findActiveNear}; controls never call it. Finds the ACTIVE hazards of a type
   * whose location lies inside a box of latitudes and longitudes.
   *
   * @param type the hazard type
   * @param minLatitude the southern edge
   * @param maxLatitude the northern edge
   * @param minLongitude the western edge
   * @param maxLongitude the eastern edge
   * @return the candidate hazards
   */
  @Query(
      "select h from Hazard h where h.status = ACTIVE and h.type = :type"
          + " and h.location.latitude between :minLatitude and :maxLatitude"
          + " and h.location.longitude between :minLongitude and :maxLongitude"
          + " order by h.startTime")
  List<Hazard> findActiveByTypeInBox(
      @Param("type") HazardType type,
      @Param("minLatitude") double minLatitude,
      @Param("maxLatitude") double maxLatitude,
      @Param("minLongitude") double minLongitude,
      @Param("maxLongitude") double maxLongitude);

  /**
   * Finds the hazards that match report criteria: at least one district of the criteria, the hazard
   * type of the criteria when it is set, and active in the period, which means it started before
   * the end of the period and has no end time or ended no earlier than the start of the period (MD
   * section 6, {@code findMatching}; criteria rule).
   *
   * @param criteria the report criteria
   * @return the matching hazards
   */
  @Transactional(readOnly = true)
  default List<Hazard> findMatching(ReportCriteria criteria) {
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
   * @param districts the districts, at least one of which the hazard lists
   * @param anyType {@code true} to ignore the type
   * @param type the hazard type, used when {@code anyType} is {@code false}
   * @param start the start of the period, inclusive
   * @param endExclusive the end of the period, exclusive
   * @return the matching hazards
   */
  @Query(
      "select distinct h from Hazard h join h.districts d where d in :districts"
          + " and (:anyType = true or h.type = :type)"
          + " and h.startTime < :endExclusive"
          + " and (h.endTime is null or h.endTime >= :start) order by h.startTime")
  List<Hazard> findMatchingInPeriod(
      @Param("districts") Set<District> districts,
      @Param("anyType") boolean anyType,
      @Param("type") HazardType type,
      @Param("start") LocalDateTime start,
      @Param("endExclusive") LocalDateTime endExclusive);
}
