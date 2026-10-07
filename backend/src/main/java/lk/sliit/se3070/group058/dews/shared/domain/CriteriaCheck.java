package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.Objects;
import java.util.Set;

/**
 * The result of checking report criteria against an officer's scope.
 *
 * <p>Implements CH-31.
 *
 * @param result the validation result
 * @param outOfScope the requested districts outside the officer's scope
 * @param adjusted the criteria limited to the officer's scope, or {@code null} if none were
 *     adjusted
 */
public record CriteriaCheck(
    ValidationResult result, Set<District> outOfScope, ReportCriteria adjusted) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public CriteriaCheck(ValidationResult result, Set<District> outOfScope, ReportCriteria adjusted) {
    this.result = Objects.requireNonNull(result, "result");
    this.outOfScope = Set.copyOf(outOfScope);
    this.adjusted = adjusted;
  }
}
