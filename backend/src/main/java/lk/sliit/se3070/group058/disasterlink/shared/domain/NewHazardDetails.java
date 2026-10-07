package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.List;

/**
 * The details of a hazard that an officer records while verifying a report.
 *
 * <p>Implements CH-25.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the officer has not entered is
 * {@code null}, so that {@code ReportVerificationController.verifyAsNewHazard} can refuse it with
 * {@code InvalidHazardDetailsException} (SC2 E9).
 *
 * @param severity the severity of the new hazard, or {@code null} if not entered — checked by
 *     {@code ReportVerificationController.verifyAsNewHazard}
 * @param districts the districts the new hazard affects, empty if none entered
 * @param locationName the name of the place of the hazard, or {@code null} if not entered — checked
 *     by {@code ReportVerificationController.verifyAsNewHazard}
 * @param description the description of the hazard, or {@code null} if not entered — checked by
 *     {@code ReportVerificationController.verifyAsNewHazard}
 */
public record NewHazardDetails(
    HazardSeverity severity, List<District> districts, String locationName, String description) {

  /** Creates the record, replacing a {@code null} collection by an empty one and copying it. */
  public NewHazardDetails(
      HazardSeverity severity, List<District> districts, String locationName, String description) {
    this.severity = severity;
    this.districts = districts == null ? List.of() : List.copyOf(districts);
    this.locationName = locationName;
    this.description = description;
  }
}
