package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;

/**
 * A hazard as the dashboard shows it (WF1-S01 row, WF1-S02 details, WF1-S03 hazard card), mapped
 * inside a read-only transaction so that the template reads no lazy entity. Labelled by type plus
 * location name ("Flood — Kalu Ganga at Ratnapura"; MD section 0.11). Implementation-only view
 * record.
 *
 * @param hazardId the identifier, for example {@code HZ-2026-0142}
 * @param label the type plus the location name
 * @param type the type, for example "Flood"
 * @param description the description
 * @param severity the severity label, for example "Medium"
 * @param severityCode the severity, to pre-select the update panel's pick-list
 * @param locationName the name of the place
 * @param districts the districts as text, or "—" if none
 * @param mapDistricts the districts as the map's {@code data-selected} text
 * @param districtCodes the districts, to pre-select the update panel's chips
 * @param riverBasin the river basin label, or "—" if none
 * @param source the source label, for example "Agency bulletin"
 * @param startTime the start time, for example "20 May 2026, 09:30"
 * @param status the status label, for example "Active"
 */
public record HazardView(
    String hazardId,
    String label,
    String type,
    String description,
    String severity,
    HazardSeverity severityCode,
    String locationName,
    String districts,
    String mapDistricts,
    List<District> districtCodes,
    String riverBasin,
    String source,
    String startTime,
    String status) {}
