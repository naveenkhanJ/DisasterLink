package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * A hazard with the active warning it has, if any, and what is missing from its record.
 *
 * <p>Implements CH-22.
 *
 * @param hazard the hazard
 * @param activeWarning the active warning of the hazard, or {@code null} if it has none
 * @param missingInformation the items a hazard update could still supply
 */
public record HazardDetailsView(
    Hazard hazard, Warning activeWarning, List<String> missingInformation) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public HazardDetailsView(Hazard hazard, Warning activeWarning, List<String> missingInformation) {
    this.hazard = Objects.requireNonNull(hazard, "hazard");
    this.activeWarning = activeWarning;
    this.missingInformation = List.copyOf(missingInformation);
  }
}
