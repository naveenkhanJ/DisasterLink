package lk.sliit.se3070.group058.disasterlink.warning.web.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.EscalationOptions;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningStatus;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.EscalationForm;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.EscalationView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.OptionView;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.WarningView;

/**
 * Maps between the escalation screens' form and view records and the domain (WF1b-S01 to S04). The
 * mapping from entities runs inside the escalation screen's read-only transaction; the labels and
 * formats are those of {@link WarningDashboardMapper}, so both screens read the same.
 * Implementation-only class (standards section 4, "No entities on the wire").
 */
public final class EscalationMapper {

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.ENGLISH);

  private EscalationMapper() {
    throw new AssertionError("EscalationMapper is a utility class");
  }

  /**
   * Maps the escalation options; call inside a transaction (the warning's hazard and districts are
   * lazy).
   *
   * @param options the warning and the levels above its current one
   * @param now the time the warning's status is judged at
   * @return the view of the escalation screen
   */
  public static EscalationView toEscalationView(EscalationOptions options, LocalDateTime now) {
    Warning warning = options.warning();
    return new EscalationView(
        WarningDashboardMapper.toWarningView(warning, now),
        target(warning),
        Arrays.stream(NotificationChannel.values())
            .filter(warning.getChannels()::contains)
            .map(WarningDashboardMapper::channelLabel)
            .collect(Collectors.joining(", ")),
        options.allowedLevels().stream()
            .map(level -> new OptionView(level.name(), WarningDashboardMapper.levelLabel(level)))
            .toList());
  }

  /**
   * Maps the fields of a warning that are loaded without a transaction, for a refusal: the warning
   * an exception carries is detached, so its hazard and districts are not read (E2).
   *
   * @param warning the warning that cannot be escalated
   * @param now the time its status is judged at
   * @return the view, with no hazard label and no covered districts
   */
  public static WarningView toRefusedView(Warning warning, LocalDateTime now) {
    boolean active = warning.statusAt(now) == WarningStatus.ACTIVE;
    WarningLevel level = warning.getWarningLevel();
    return new WarningView(
        warning.getWarningId(),
        "",
        WarningDashboardMapper.levelLabel(level),
        "tag--l" + (level.ordinal() + 1),
        active ? "Active" : "Expired",
        active,
        null,
        DATE_TIME.format(warning.getIssuedTime()),
        DATE_TIME.format(warning.getExpiryTime()));
  }

  /**
   * The details form for a chosen level, pre-filled from the warning's current texts and expiry
   * time (Escalate step 6; MD C2).
   *
   * @param warning the warning
   * @param newLevel the chosen level
   * @return the pre-filled form
   */
  public static EscalationForm formOf(Warning warning, WarningLevel newLevel) {
    LocalisedText message = warning.getMessage();
    LocalisedText instructions = warning.getInstructions();
    return new EscalationForm(
        newLevel,
        message.textIn(Language.SINHALA),
        message.textIn(Language.TAMIL),
        message.textIn(Language.ENGLISH),
        instructions.textIn(Language.SINHALA),
        instructions.textIn(Language.TAMIL),
        instructions.textIn(Language.ENGLISH),
        warning.getExpiryTime());
  }

  /**
   * The same form with another level: the texts and expiry time are kept (Escalate A4 9b).
   *
   * @param form the form so far
   * @param newLevel the newly chosen level
   * @return the form with the new level
   */
  public static EscalationForm withLevel(EscalationForm form, WarningLevel newLevel) {
    return new EscalationForm(
        newLevel,
        form.messageSinhala(),
        form.messageTamil(),
        form.messageEnglish(),
        form.instructionsSinhala(),
        form.instructionsTamil(),
        form.instructionsEnglish(),
        form.expiryTime());
  }

  /**
   * Turns the posted form into the details of {@code enterEscalationDetails}; a blank text becomes
   * {@code null} (not entered).
   *
   * @param form the posted form
   * @return the escalation details
   */
  public static EscalationDetails toDetails(EscalationForm form) {
    return new EscalationDetails(
        form.newLevel(),
        new LocalisedText(
            blankToNull(form.messageSinhala()),
            blankToNull(form.messageTamil()),
            blankToNull(form.messageEnglish())),
        new LocalisedText(
            blankToNull(form.instructionsSinhala()),
            blankToNull(form.instructionsTamil()),
            blankToNull(form.instructionsEnglish())),
        form.expiryTime());
  }

  /** "Ratnapura district, Kalu river basin" (WF1b-S01). */
  private static String target(Warning warning) {
    List<String> parts = new ArrayList<>();
    if (!warning.getTargetDistricts().isEmpty()) {
      parts.add(
          WarningFormMapper.names(warning.getTargetDistricts())
              + (warning.getTargetDistricts().size() == 1 ? " district" : " districts"));
    }
    if (!warning.getTargetRiverBasins().isEmpty()) {
      parts.add(
          WarningFormMapper.names(warning.getTargetRiverBasins())
              + (warning.getTargetRiverBasins().size() == 1 ? " river basin" : " river basins"));
    }
    return String.join(", ", parts);
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}
