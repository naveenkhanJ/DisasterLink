package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;

/**
 * The new-hazard form of the verification console (SC2 A6, WF2C-S02c) as the browser posts it. It
 * is mapped to {@code NewHazardDetails} with blank text as {@code null}; the checks of the details
 * are {@code ReportVerificationController.verifyAsNewHazard}'s, never Bean Validation's (standards
 * section 4, "Input validation").
 *
 * <p>Implementation-only form record (model dictionary section 0.7; C2 screen component).
 *
 * @param severity the severity chosen, or {@code null} if none
 * @param districts the districts ticked, empty if none
 * @param locationName the place name typed, or {@code null}
 * @param description the optional description typed, or {@code null}
 */
public record NewHazardForm(
    HazardSeverity severity, List<District> districts, String locationName, String description) {

  /** Creates the form, replacing a {@code null} list by an empty one. */
  public NewHazardForm(
      HazardSeverity severity, List<District> districts, String locationName, String description) {
    this.severity = severity;
    this.districts = districts == null ? List.of() : List.copyOf(districts);
    this.locationName = locationName;
    this.description = description;
  }
}
