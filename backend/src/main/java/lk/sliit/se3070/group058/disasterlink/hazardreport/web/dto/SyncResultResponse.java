package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import lk.sliit.se3070.group058.disasterlink.shared.domain.RejectionReason;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncOutcome;

/**
 * The {@code SyncResult} of a report upload on the wire. The enums travel as their constant names.
 *
 * <p>Implementation-only wire record.
 *
 * @param submissionId the identifier of the submission
 * @param outcome ACCEPTED, DUPLICATE or REJECTED
 * @param reason the reason, set only when the outcome is REJECTED, otherwise {@code null}
 * @param message a message for the user, or {@code null} if there is none
 */
public record SyncResultResponse(
    String submissionId, SyncOutcome outcome, RejectionReason reason, String message) {}
