package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;

/**
 * The "Update hazard" panel as posted (WF1-S02, WF1-S03 drawer; the fields of {@code
 * editHazard(hazardId, update)}). Nothing is required here: {@code WarningController.updateHazard}
 * refuses an incomplete update (SC1 E5). Implementation-only form record (MD section 0.7).
 *
 * @param description the new description, or {@code null} if not entered
 * @param severity the new severity, or {@code null} if not chosen
 * @param districts the chosen districts, or {@code null} if none
 */
public record HazardUpdateForm(
    String description, HazardSeverity severity, List<District> districts) {}
