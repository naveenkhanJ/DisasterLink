package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

/**
 * The escalation in progress, kept in the HTTP session from {@code startEscalation} to {@code
 * confirmEscalation} or {@code cancelEscalation}, whose inputs carry no details (standards section
 * 4, "Preview state"). Its session name differs from the form's model name (R4G-09).
 * Implementation-only record.
 *
 * @param warningId the warning being escalated
 * @param form the details entered so far, or {@code null} before a level is chosen
 * @param fromDashboard {@code true} if the DMC officer started it from the warning dashboard (A1),
 *     {@code false} if the duty officer came from the verification console's prompt
 */
public record PreviewedEscalation(String warningId, EscalationForm form, boolean fromDashboard) {}
