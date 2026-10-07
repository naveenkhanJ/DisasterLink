package lk.sliit.se3070.group058.dews.shared.domain;

/**
 * The level of a warning, using NBRO's published three-level scale: Level 1 – Yellow (Remain
 * Vigilant), Level 2 – Amber (Be Alert) and Level 3 – Red (Evacuate). Applying the scale to every
 * hazard type is a Group_058 assumption.
 *
 * <p>Implements CH-21.
 */
public enum WarningLevel {
  LEVEL_1_YELLOW,
  LEVEL_2_AMBER,
  LEVEL_3_RED
}
