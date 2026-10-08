package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The refusal of {@code WarningController.issueWarning} when the hazard already has an active
 * warning (SC1 E6; a hazard has at most one active warning, A1). It carries that warning, which
 * {@code WarningDashboardUI.showActiveWarningExists(warning)} displays so that the officer can
 * escalate it instead.
 *
 * <p>Implements CH-06 (MD section 6, exceptions table; V2C-12). Unchecked, so the transaction of
 * {@code issueWarning} rolls back and nothing is recorded or broadcast.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ActiveWarningExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  // Never serialised: the exception lives only between the control and its handler.
  private final transient Warning warning;

  /**
   * Creates the refusal.
   *
   * @param warning the hazard's active warning
   * @throws NullPointerException if {@code warning} is {@code null}
   */
  public ActiveWarningExistsException(Warning warning) {
    super(
        "The hazard already has the active warning "
            + Objects.requireNonNull(warning, "warning").getWarningId()
            + "; escalate that warning instead");
    this.warning = warning;
  }

  /**
   * Returns the hazard's active warning.
   *
   * @return the active warning
   */
  public Warning getWarning() {
    return warning;
  }
}
