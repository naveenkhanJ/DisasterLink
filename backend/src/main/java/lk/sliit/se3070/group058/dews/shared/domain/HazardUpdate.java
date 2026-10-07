package lk.sliit.se3070.group058.dews.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * An officer's update of a hazard's description, severity and districts.
 *
 * <p>Implements CH-22.
 *
 * @param description the new description
 * @param severity the new severity
 * @param districts the new affected districts
 */
public record HazardUpdate(String description, HazardSeverity severity, List<District> districts) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public HazardUpdate(String description, HazardSeverity severity, List<District> districts) {
    this.description = Objects.requireNonNull(description, "description");
    this.severity = Objects.requireNonNull(severity, "severity");
    this.districts = List.copyOf(districts);
  }
}
