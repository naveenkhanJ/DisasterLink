package lk.sliit.se3070.group058.disasterlink.shared.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for the push mock's outbox (model dictionary section 6, PushOutbox). */
public interface PushOutboxRepository extends JpaRepository<PushOutbox, String> {

  /**
   * Finds the entries of a recipient that the app has not acknowledged, oldest first.
   *
   * @param recipientId the identifier of the recipient
   * @return the undelivered entries
   */
  @Query(
      "select p from PushOutbox p where p.recipientId = :recipientId and p.deliveredAt is null"
          + " order by p.createdAt")
  List<PushOutbox> findUndelivered(@Param("recipientId") String recipientId);
}
