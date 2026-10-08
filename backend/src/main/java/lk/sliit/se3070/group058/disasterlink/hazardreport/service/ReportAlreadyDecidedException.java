package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.hazardreport.domain.HazardReport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by {@code ReportVerificationController} when a decision reaches a report that is no longer
 * pending: another duty officer has decided it, or an earlier attempt whose reply was lost did, or
 * a concurrent decision failed the optimistic-lock check on the report's version. Nothing was
 * changed by the refused decision; the console shows the report as it now stands ({@code
 * VerificationConsoleUI.showAlreadyDecided}).
 *
 * <p>Implements CH-25 and CH-26 (SC2 E10; PD5, FX-07). It extends {@link RuntimeException}, so a
 * transaction rolls back, and it declares its HTTP status for the shared JSON handler (standards
 * section 3, package map).
 *
 * <p>The report it carries is for the display only: it may have been read in a transaction that has
 * ended, so a lazy link (reporter, reviewer, hazard) cannot be read from it; the console re-reads
 * what it shows.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ReportAlreadyDecidedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The message shown to the officer. */
  static final String MESSAGE = "This report has already been decided";

  private final transient HazardReport report;

  /**
   * Creates the exception with the message "This report has already been decided".
   *
   * @param report the report that was already decided, as it stands now
   * @throws NullPointerException if {@code report} is {@code null}
   */
  public ReportAlreadyDecidedException(HazardReport report) {
    super(MESSAGE);
    this.report = Objects.requireNonNull(report, "report");
  }

  /**
   * Creates the exception for a decision that lost a race: the lock failure that showed it is kept
   * as the cause.
   *
   * @param report the report as it stands now, re-read after the failure
   * @param cause the optimistic-lock failure of the commit
   * @throws NullPointerException if {@code report} is {@code null}
   */
  public ReportAlreadyDecidedException(HazardReport report, Throwable cause) {
    super(MESSAGE, cause);
    this.report = Objects.requireNonNull(report, "report");
  }

  /**
   * Returns the report that was already decided.
   *
   * @return the report, for the display only
   */
  public HazardReport getReport() {
    return report;
  }
}
