package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a resource-owning organisation attempts to view a report that was not shared with it.
 *
 * <p>Implements CH-31, CH-32 (E6, A23); model dictionary section 6.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ReportNotSharedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default refusal message. */
  public ReportNotSharedException() {
    super("This report has not been shared with your organisation.");
  }

  /**
   * Creates the exception with a specific detail message.
   *
   * @param message the detail message
   */
  public ReportNotSharedException(String message) {
    super(message);
  }
}
