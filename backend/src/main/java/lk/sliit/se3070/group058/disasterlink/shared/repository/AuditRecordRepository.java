package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for audit records (model dictionary section 6, AuditRecordRepository). */
public interface AuditRecordRepository extends JpaRepository<AuditRecord, String> {

  /**
   * Finds the WARNING_ESCALATED records whose subject is one of some warnings, oldest first. The
   * warnings are resolved first; this serves the alert timeline (MD section 6, {@code
   * findEscalations}; P60 N24; PR6).
   *
   * @param warningIds the identifiers of the warnings
   * @return the escalation records
   */
  @Query(
      "select a from AuditRecord a where a.action = WARNING_ESCALATED"
          + " and a.subjectId in :warningIds order by a.performedAt")
  List<AuditRecord> findEscalations(@Param("warningIds") Set<String> warningIds);
}
