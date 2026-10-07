package lk.sliit.se3070.group058.disasterlink.shared.repository;

import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ProcessedSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for processed submissions (model dictionary section 6). */
public interface ProcessedSubmissionRepository extends JpaRepository<ProcessedSubmission, String> {

  /**
   * Finds the record of an accepted submission (MD section 6, {@code findBySubmissionId}; derived
   * from the name).
   *
   * @param submissionId the identifier of the submission
   * @return the record, or empty if the submission was not accepted before
   */
  Optional<ProcessedSubmission> findBySubmissionId(String submissionId);
}
