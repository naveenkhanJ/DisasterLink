package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The refusal of {@code WarningController.updateHazard} when the officer's update is invalid (SC1
 * E5, for example no district given). It carries the errors, which {@code
 * WarningDashboardUI.showValidationErrors(errors)} displays; the hazard is not changed.
 *
 * <p>Implements CH-22 (MD section 6, exceptions table; V2C N-02). Unchecked, so the transaction of
 * {@code updateHazard} rolls back.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidHazardUpdateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  // Never serialised: the exception lives only between the control and its handler.
  private final transient List<String> errors;

  /**
   * Creates the refusal.
   *
   * @param errors the reasons the update is invalid, at least one
   * @throws NullPointerException if {@code errors} is {@code null}
   */
  public InvalidHazardUpdateException(List<String> errors) {
    super("The hazard update is invalid: " + String.join("; ", errors));
    this.errors = List.copyOf(errors);
  }

  /**
   * Returns the reasons the update is invalid.
   *
   * @return an unmodifiable list of the errors
   */
  public List<String> getErrors() {
    return errors;
  }
}
