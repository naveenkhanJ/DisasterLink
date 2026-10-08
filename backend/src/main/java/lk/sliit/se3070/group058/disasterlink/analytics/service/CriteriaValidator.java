package lk.sliit.se3070.group058.disasterlink.analytics.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.CriteriaCheck;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Validates report criteria and checks them against an officer's reporting scope.
 *
 * <p>Implements CH-31 (PD6, FX-10, P60 N27); extracted for L03 Large Class / L04 Extract Class to
 * keep {@code AnalyticsController} cohesive. Model dictionary section 6.
 */
@Component
public class CriteriaValidator {

  /** Maximum allowed period length in months (SC4 step 6, Group_060 figure). */
  private static final int MAX_PERIOD_MONTHS = 24;

  /**
   * Validates criteria against the business rules and officer's scope.
   *
   * @param criteria the report criteria to validate
   * @param officerDistricts the officer's assigned reporting districts; empty implies national
   *     scope
   * @return the validation check outcome, out-of-scope districts, and adjusted criteria if
   *     applicable
   * @throws NullPointerException if {@code criteria} is {@code null}
   */
  public CriteriaCheck validate(ReportCriteria criteria, Set<District> officerDistricts) {
    Objects.requireNonNull(criteria, "criteria");
    Set<District> scope = officerDistricts == null ? Set.of() : officerDistricts;
    List<String> errors = new ArrayList<>();

    if (criteria.districts().isEmpty()) {
      errors.add("Select at least one district");
    }

    if (criteria.periodStart() == null) {
      errors.add("Start date is required");
    }
    if (criteria.periodEnd() == null) {
      errors.add("End date is required");
    }
    if (criteria.periodStart() != null && criteria.periodEnd() != null) {
      if (criteria.periodStart().isAfter(criteria.periodEnd())) {
        errors.add("The start date is after the end date");
      } else if (criteria
          .periodStart()
          .plusMonths(MAX_PERIOD_MONTHS)
          .isBefore(criteria.periodEnd())) {
        errors.add("A period of at most 24 months");
      }
    }

    if (criteria.metrics().isEmpty()) {
      errors.add("Select at least one metric");
    }

    Set<District> outOfScope = new LinkedHashSet<>();
    ReportCriteria adjusted = null;

    if (!scope.isEmpty() && !criteria.districts().isEmpty()) {
      for (District district : criteria.districts()) {
        if (!scope.contains(district)) {
          outOfScope.add(district);
        }
      }

      if (outOfScope.size() == criteria.districts().size()) {
        errors.add("None of the selected districts is within your reporting districts");
      } else if (!outOfScope.isEmpty()) {
        Set<District> inScope =
            criteria.districts().stream().filter(scope::contains).collect(Collectors.toSet());
        adjusted =
            new ReportCriteria(
                inScope,
                criteria.hazardType(),
                criteria.periodStart(),
                criteria.periodEnd(),
                criteria.metrics(),
                criteria.hazardId());
      }
    }

    boolean valid = errors.isEmpty();
    if (!valid) {
      adjusted = null;
    }

    return new CriteriaCheck(new ValidationResult(valid, errors), outOfScope, adjusted);
  }
}
