package lk.sliit.se3070.group058.disasterlink.hazardreport.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by {@code ReportVerificationController.verifyAsNewHazard} when the details of the new
 * hazard lack the severity, the districts or the place name. Nothing is created and the report
 * stays pending; the console shows the errors ({@code VerificationConsoleUI.showValidationErrors}).
 *
 * <p>Implements CH-25 (SC2 E9; P60 N12). It extends {@link RuntimeException}, so a transaction
 * rolls back, and it declares its HTTP status for the shared JSON handler (standards section 3,
 * package map).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidHazardDetailsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String ERROR_SEPARATOR = "; ";

  private final transient List<String> errors;

  /**
   * Creates the exception; its message is the errors joined with "; ".
   *
   * @param errors what is wrong with the details, for example "Districts are required"
   * @throws NullPointerException if {@code errors} is {@code null}
   */
  public InvalidHazardDetailsException(List<String> errors) {
    super(String.join(ERROR_SEPARATOR, errors));
    this.errors = List.copyOf(errors);
  }

  /**
   * Returns what is wrong with the details.
   *
   * @return an unmodifiable list of the errors
   */
  public List<String> getErrors() {
    return errors;
  }
}
