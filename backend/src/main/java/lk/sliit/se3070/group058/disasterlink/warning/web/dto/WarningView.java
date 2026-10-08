package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

/**
 * An issued warning as the dashboard shows it (WF1-S05 banner, WF1-S02c card, WF1-S04a modal).
 * Implementation-only view record.
 *
 * @param warningId the identifier, for example {@code WRN-2026-0087}
 * @param hazardLabel the label of its hazard, for the subtitle
 * @param levelLabel the level, for example "Level 2 – Amber (Be Alert)"
 * @param levelClass the CSS modifier of the level tag, for example {@code tag--l2}
 * @param status the status label, "Active" or "Expired"
 * @param active whether the warning is active now
 * @param coveredDistricts the covered districts as text
 * @param issued the issue time, for example "20 May 2026, 10:45"
 * @param expires the expiry time
 */
public record WarningView(
    String warningId,
    String hazardLabel,
    String levelLabel,
    String levelClass,
    String status,
    boolean active,
    String coveredDistricts,
    String issued,
    String expires) {}
