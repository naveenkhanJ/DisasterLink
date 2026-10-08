package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.time.LocalDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * The escalation details form as posted (WF1b-S02; the fields of {@code
 * enterEscalationDetails(details)}). The mapper turns it into {@code EscalationDetails}.
 *
 * <p>Nothing is required here: unlike a warning's level, the new level is a control rule ({@code
 * validateEscalation} checks it is above the current one — standards section 12.2), and so are the
 * texts and the expiry time. The {@code datetime-local} input carries no offset, so the expiry time
 * is bound as a local time in Asia/Colombo (standards section 12.1, item 3). Implementation-only
 * form record (MD section 0.7).
 *
 * @param newLevel the chosen level, or {@code null} if none
 * @param messageSinhala the message in Sinhala, or {@code null} if not entered
 * @param messageTamil the message in Tamil, or {@code null} if not entered
 * @param messageEnglish the message in English, or {@code null} if not entered
 * @param instructionsSinhala the instructions in Sinhala, or {@code null} if not entered
 * @param instructionsTamil the instructions in Tamil, or {@code null} if not entered
 * @param instructionsEnglish the instructions in English, or {@code null} if not entered
 * @param expiryTime the new end of the validity, or {@code null} if not entered
 */
public record EscalationForm(
    WarningLevel newLevel,
    String messageSinhala,
    String messageTamil,
    String messageEnglish,
    String instructionsSinhala,
    String instructionsTamil,
    String instructionsEnglish,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiryTime) {}
