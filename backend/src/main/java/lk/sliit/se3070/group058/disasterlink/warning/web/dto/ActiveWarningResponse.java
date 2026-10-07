package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;

/**
 * An active warning as the citizen app receives it: the warning's fields and its hazard's type and
 * {@code locationName}, so that the app labels the hazard as "Flood — Kalu Ganga at Ratnapura"
 * (standards section 12.1, "Active warnings for the app"; MD section 0.11, P62 FX-14). Times carry
 * their {@code +05:30} offset (section 12.1, item 3).
 *
 * <p>Implementation-only wire record.
 *
 * @param warningId the identifier of the warning
 * @param warningLevel the level of the warning
 * @param message the message in the three languages
 * @param instructions the instructions in the three languages
 * @param issuedTime when the warning was issued
 * @param expiryTime when the warning expires
 * @param targetDistricts the districts the warning targets
 * @param hazardType the type of the warning's hazard
 * @param locationName the name of the place of the warning's hazard
 */
public record ActiveWarningResponse(
    String warningId,
    WarningLevel warningLevel,
    LocalisedTextResponse message,
    LocalisedTextResponse instructions,
    OffsetDateTime issuedTime,
    OffsetDateTime expiryTime,
    List<District> targetDistricts,
    HazardType hazardType,
    String locationName) {}
