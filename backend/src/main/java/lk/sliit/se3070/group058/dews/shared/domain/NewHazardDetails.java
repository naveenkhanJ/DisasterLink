package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * The details of a hazard that an officer records while verifying a report.
 *
 * <p>Implements CH-25.
 *
 * @param severity the severity of the new hazard
 * @param districts the districts the new hazard affects
 * @param locationName the name of the place of the hazard
 * @param description the description of the hazard
 */
public record NewHazardDetails(
    HazardSeverity severity, List<District> districts, String locationName, String description) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public NewHazardDetails(
      HazardSeverity severity, List<District> districts, String locationName, String description) {
    this.severity = Objects.requireNonNull(severity, "severity");
    this.districts = List.copyOf(districts);
    this.locationName = Objects.requireNonNull(locationName, "locationName");
    this.description = Objects.requireNonNull(description, "description");
  }
}
