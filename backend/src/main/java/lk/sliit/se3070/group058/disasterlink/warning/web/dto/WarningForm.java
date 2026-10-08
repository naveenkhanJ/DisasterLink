package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * The Create Hazard Warning form as posted (WF1-S03; the fields of {@code
 * enterWarningDetails(details)}). The mapper turns it into {@code WarningDetails}.
 *
 * <p>Bean Validation checks only the shape (standards section 4): the level is required (standards
 * section 12.2, a request without it is a 400); every other rule is {@code validateWarning}'s, so a
 * missing text or expiry time reaches the control. The {@code datetime-local} input carries no
 * offset, so the expiry time is bound as a local time in Asia/Colombo (standards section 12.1, item
 * 3). Implementation-only form record (MD section 0.7).
 *
 * @param warningLevel the chosen level, required
 * @param messageSinhala the message in Sinhala, or {@code null} if not entered
 * @param messageTamil the message in Tamil, or {@code null} if not entered
 * @param messageEnglish the message in English, or {@code null} if not entered
 * @param instructionsSinhala the instructions in Sinhala, or {@code null} if not entered
 * @param instructionsTamil the instructions in Tamil, or {@code null} if not entered
 * @param instructionsEnglish the instructions in English, or {@code null} if not entered
 * @param targetDistricts the selected districts, or {@code null} if none
 * @param targetRiverBasins the selected river basins, or {@code null} if none
 * @param channels the checked channels, or {@code null} if none
 * @param expiryTime the end of the validity, or {@code null} if not entered
 */
public record WarningForm(
    @NotNull WarningLevel warningLevel,
    String messageSinhala,
    String messageTamil,
    String messageEnglish,
    String instructionsSinhala,
    String instructionsTamil,
    String instructionsEnglish,
    List<District> targetDistricts,
    List<RiverBasin> targetRiverBasins,
    Set<NotificationChannel> channels,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiryTime) {}
