package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.util.List;

/**
 * A warning offered for escalation (WF1b-S01 warning card and level tiles, WF1b-S02 "Warning"
 * card). Implementation-only view record.
 *
 * @param warning the warning's banner fields
 * @param target its target, for example "Ratnapura district, Kalu river basin"
 * @param channels its channels, for example "Push, SMS, Audible alert"
 * @param allowedLevels the levels above its current one; empty at Level 3 – Red
 */
public record EscalationView(
    WarningView warning, String target, String channels, List<OptionView> allowedLevels) {}
