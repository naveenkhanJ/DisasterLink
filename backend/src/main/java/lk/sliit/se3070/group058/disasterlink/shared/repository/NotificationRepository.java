package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counting queries over notifications (model dictionary section 6, NotificationRepository). The
 * repository is query-only: notifications are saved through their warning (A2), so it offers no
 * {@code save}.
 *
 * <p>A citizen counts as reached when the stored recipient kind is CITIZEN, the citizen is
 * registered in a selected district and at least one notification of the selected warnings to the
 * citizen is DELIVERED (FD-04; PR6).
 */
@Transactional(readOnly = true)
public interface NotificationRepository extends Repository<Notification, String> {

  /**
   * Counts, per channel, the distinct citizens reached by the notifications of some warnings. A
   * channel on which no citizen was reached is absent from the map (MD section 6, {@code
   * countCitizensReachedByChannel}).
   *
   * @param warningIds the identifiers of the warnings
   * @param districts the districts the citizens must be registered in
   * @return the number of citizens reached per channel
   */
  @Transactional(readOnly = true)
  default Map<NotificationChannel, Integer> countCitizensReachedByChannel(
      Set<String> warningIds, Set<District> districts) {
    Map<NotificationChannel, Integer> counts = new EnumMap<>(NotificationChannel.class);
    for (Object[] row : countReachedPerChannel(warningIds, districts)) {
      counts.put((NotificationChannel) row[0], Math.toIntExact((Long) row[1]));
    }
    return counts;
  }

  /**
   * Helper of {@code countCitizensReachedByChannel}; controls never call it. Returns one row per
   * channel: the channel and the number of distinct citizens reached on it.
   *
   * @param warningIds the identifiers of the warnings
   * @param districts the districts the citizens must be registered in
   * @return the rows of channel and count
   */
  @Query(
      "select n.type, count(distinct c.userId) from Warning w join w.notifications n, Citizen c"
          + " where w.warningId in :warningIds and n.deliveryStatus = DELIVERED"
          + " and n.recipientKind = CITIZEN and c.userId = n.recipientId"
          + " and c.district in :districts group by n.type")
  List<Object[]> countReachedPerChannel(
      @Param("warningIds") Set<String> warningIds, @Param("districts") Set<District> districts);

  /**
   * Counts the distinct citizens reached by the notifications of some warnings, on any channel (MD
   * section 6, {@code countCitizensReached}).
   *
   * @param warningIds the identifiers of the warnings
   * @param districts the districts the citizens must be registered in
   * @return the number of citizens reached
   */
  @Transactional(readOnly = true)
  default int countCitizensReached(Set<String> warningIds, Set<District> districts) {
    return Math.toIntExact(countDistinctCitizensReached(warningIds, districts));
  }

  /**
   * Helper of {@code countCitizensReached}; controls never call it.
   *
   * @param warningIds the identifiers of the warnings
   * @param districts the districts the citizens must be registered in
   * @return the number of distinct citizens reached
   */
  @Query(
      "select count(distinct c.userId) from Warning w join w.notifications n, Citizen c"
          + " where w.warningId in :warningIds and n.deliveryStatus = DELIVERED"
          + " and n.recipientKind = CITIZEN and c.userId = n.recipientId"
          + " and c.district in :districts")
  long countDistinctCitizensReached(
      @Param("warningIds") Set<String> warningIds, @Param("districts") Set<District> districts);

  /**
   * Counts every notification of some warnings, including those to organisations and district
   * officers and those not delivered; it feeds {@code matchedRecordCount} (MD section 6, {@code
   * countByWarnings}; N28).
   *
   * @param warningIds the identifiers of the warnings
   * @return the number of notifications
   */
  @Transactional(readOnly = true)
  default int countByWarnings(Set<String> warningIds) {
    return Math.toIntExact(countNotificationsOf(warningIds));
  }

  /**
   * Helper of {@code countByWarnings}; controls never call it.
   *
   * @param warningIds the identifiers of the warnings
   * @return the number of notifications
   */
  @Query("select count(n) from Warning w join w.notifications n where w.warningId in :warningIds")
  long countNotificationsOf(@Param("warningIds") Set<String> warningIds);
}
