package lk.sliit.se3070.group058.disasterlink.analytics.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an exporter fails to generate an export file.
 *
 * <p>Implements CH-32 (E2); model dictionary section 6.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExportFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception with the failure message.
   *
   * @param message the failure message
   */
  public ExportFailedException(String message) {
    super(message);
  }

  /**
   * Creates the exception with the failure message and the underlying cause.
   *
   * @param message the failure message
   * @param cause the underlying cause
   */
  public ExportFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
