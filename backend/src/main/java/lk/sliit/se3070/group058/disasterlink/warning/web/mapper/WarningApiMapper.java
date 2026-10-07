package lk.sliit.se3070.group058.disasterlink.warning.web.mapper;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Hazard;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.web.WireTime;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.ActiveWarningResponse;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.LocalisedTextResponse;

/**
 * Maps a stored warning to the record the citizen app receives. It reads the warning's lazy hazard
 * and collections, so it runs inside the endpoint's read-only transaction. Times go through {@link
 * WireTime}, so they carry the {@code +05:30} offset.
 *
 * <p>Implementation-only class (standards section 4, "No entities on the wire"; section 12.1).
 */
public final class WarningApiMapper {

  private WarningApiMapper() {
    throw new AssertionError("WarningApiMapper is a utility class");
  }

  /**
   * Maps a warning with its hazard's type and place name.
   *
   * @param warning the warning
   * @return the record for the JSON reply
   */
  public static ActiveWarningResponse toResponse(Warning warning) {
    Hazard hazard = warning.getHazard();
    return new ActiveWarningResponse(
        warning.getWarningId(),
        warning.getWarningLevel(),
        toResponse(warning.getMessage()),
        toResponse(warning.getInstructions()),
        WireTime.toWire(warning.getIssuedTime()),
        WireTime.toWire(warning.getExpiryTime()),
        List.copyOf(warning.getTargetDistricts()),
        hazard.getType(),
        hazard.getLocationName());
  }

  private static LocalisedTextResponse toResponse(LocalisedText text) {
    return new LocalisedTextResponse(text.sinhala(), text.tamil(), text.english());
  }
}
