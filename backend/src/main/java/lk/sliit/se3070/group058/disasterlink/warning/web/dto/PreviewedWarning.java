package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

/**
 * The warning details an officer entered for a hazard, kept in the HTTP session between {@code
 * enterWarningDetails}, {@code requestPreview} and {@code confirmBroadcast}, whose inputs carry no
 * details (standards section 4, "Preview state"). Its session name differs from the form's model
 * name, so a re-posted form never binds onto it (R4G-09). Implementation-only record.
 *
 * @param hazardId the hazard the details are for
 * @param form the details as entered
 */
public record PreviewedWarning(String hazardId, WarningForm form) {}
