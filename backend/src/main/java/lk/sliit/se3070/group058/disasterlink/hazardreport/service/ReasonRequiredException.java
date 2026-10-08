package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by {@code ReportVerificationController.rejectReport} when the duty officer confirms a
 * rejection with an empty reason. The report stays pending and nothing is recorded; the console
 * shows "A rejection reason is required" ({@code VerificationConsoleUI.showReasonRequired}).
 *
 * <p>Implements CH-25 (SC2 E7, step 25s). A missing reason is not a validation result but refusal
 * kind 2 of the standards (section 4, "Refusals"). It extends {@link RuntimeException}, so a
 * transaction rolls back, and it declares its HTTP status for the shared JSON handler (section 3,
 * package map).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReasonRequiredException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The message shown to the officer. */
  static final String MESSAGE = "A rejection reason is required";

  /** Creates the exception with the message "A rejection reason is required". */
  public ReasonRequiredException() {
    super(MESSAGE);
  }
}
