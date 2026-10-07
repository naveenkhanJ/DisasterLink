package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.util.List;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardDetailsView;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.IssueOutcome;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningPreview;
import org.springframework.stereotype.Service;

/**
 * The control of use case UC1, Issue Hazard Warning.
 *
 * <p>Signature stub from the skeleton; Lakmal replaces the bodies and adds the constructor on his
 * branch. Other use cases call these operations before he writes them: {@code
 * ReportVerificationController} calls {@code getActiveWarningForHazard}, and the citizen app reads
 * {@code getActiveWarnings}. The signatures are those of the model dictionary section 5 (CH-19,
 * CH-22 and CH-23).
 */
@Service
public class WarningController {

  private static final String OWNER_MESSAGE = "not implemented yet — Lakmal";

  /**
   * Finds the hazards a warning can be issued for.
   *
   * @return the assessable hazards
   */
  public List<Hazard> getAssessableHazards() {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Shows a hazard with its active warning and what its record lacks.
   *
   * @param hazardId the identifier of the hazard
   * @return the hazard details
   */
  public HazardDetailsView getHazardDetails(String hazardId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Finds the active warning of a hazard; also called by {@code ReportVerificationController}.
   *
   * @param hazardId the identifier of the hazard
   * @return the active warning, or empty if there is none
   */
  public Optional<Warning> getActiveWarningForHazard(String hazardId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Applies an update to a hazard; an invalid update is refused with {@code
   * InvalidHazardUpdateException}.
   *
   * @param hazardId the identifier of the hazard
   * @param update the new description, severity and districts
   * @return the updated hazard
   */
  public Hazard updateHazard(String hazardId, HazardUpdate update) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Validates the details of a warning.
   *
   * @param hazardId the identifier of the hazard
   * @param details the warning details
   * @return the validation result
   */
  public ValidationResult validateWarning(String hazardId, WarningDetails details) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Previews a broadcast: the covered districts and the number of recipients.
   *
   * @param hazardId the identifier of the hazard
   * @param details the warning details
   * @return the preview
   */
  public WarningPreview previewWarning(String hazardId, WarningDetails details) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Issues a warning and broadcasts it; refused with {@code ActiveWarningExistsException} when the
   * hazard already has an active warning.
   *
   * @param hazardId the identifier of the hazard
   * @param details the warning details
   * @param officerId the identifier of the officer who issues it
   * @return the warning and the summary of its broadcast
   */
  public IssueOutcome issueWarning(String hazardId, WarningDetails details, String officerId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Sends the warning again on one channel to the recipients whose last notification failed.
   *
   * @param warningId the identifier of the warning
   * @param channel the channel to resend on
   * @return the summary of the current round
   */
  public BroadcastSummary resendOnChannel(String warningId, NotificationChannel channel) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Summarises the delivery of a warning's current round.
   *
   * @param warningId the identifier of the warning
   * @return the summary
   */
  public BroadcastSummary getBroadcastSummary(String warningId) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }

  /**
   * Finds the warnings active now that cover a district; the citizen app reads them.
   *
   * @param district the district
   * @return the active warnings
   */
  public List<Warning> getActiveWarnings(District district) {
    throw new UnsupportedOperationException(OWNER_MESSAGE);
  }
}
