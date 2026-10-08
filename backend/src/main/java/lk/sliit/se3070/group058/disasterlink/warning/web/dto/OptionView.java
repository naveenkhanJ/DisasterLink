package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

/**
 * One choice of a pick-list, chip set or tile row: the constant the form posts and its label.
 * Implementation-only view record.
 *
 * @param code the enum constant name, for example {@code NUWARA_ELIYA}
 * @param label the label shown, for example "Nuwara Eliya"
 */
public record OptionView(String code, String label) {}
