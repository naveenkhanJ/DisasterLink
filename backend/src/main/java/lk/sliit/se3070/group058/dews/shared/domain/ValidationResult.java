package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.List;

/**
 * The result of validating an input: valid, or a list of errors.
 *
 * <p>Implements CH-22, CH-26, CH-28 and CH-31.
 *
 * @param valid whether the input is valid
 * @param errors the errors found; empty if the input is valid
 */
public record ValidationResult(boolean valid, List<String> errors) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ValidationResult(boolean valid, List<String> errors) {
    this.valid = valid;
    this.errors = List.copyOf(errors);
  }
}
