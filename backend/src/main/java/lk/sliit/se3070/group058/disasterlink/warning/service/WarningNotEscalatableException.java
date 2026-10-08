package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The refusal of {@code EscalationController.getEscalationOptions} when the warning has expired,
 * and of {@code escalateWarning} when it has expired or is no longer below the chosen level (for
 * example escalated by another officer meanwhile) (Escalate E2). It carries the warning, which
 * {@code EscalationUI.showNotEscalatable(warning)} displays; nothing is changed.
 *
 * <p>Implements CH-06 (MD section 6, exceptions table; P60 N6). Unchecked, so the transaction of
 * {@code escalateWarning} rolls back.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class WarningNotEscalatableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  // Never serialised: the exception lives only between the control and its handler.
  private final transient Warning warning;

  /**
   * Creates the refusal.
   *
   * @param warning the warning that cannot be escalated
   * @throws NullPointerException if {@code warning} is {@code null}
   */
  public WarningNotEscalatableException(Warning warning) {
    super(
        "The warning "
            + Objects.requireNonNull(warning, "warning").getWarningId()
            + " cannot be escalated");
    this.warning = warning;
  }

  /**
   * Returns the warning that cannot be escalated.
   *
   * @return the warning
   */
  public Warning getWarning() {
    return warning;
  }
}
