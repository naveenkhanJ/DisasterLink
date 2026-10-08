package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the text rules of a warning, shared by {@code WarningController.validateWarning} and
 * {@code EscalationController.validateEscalation}: the message and the instructions in all three
 * languages, the SMS length of each language's message plus instructions when SMS is a channel, and
 * an expiry time after now (SC1 step 11, E3).
 *
 * <p>The SMS encoding follows the actual text: GSM 7-bit when every character is in the GSM default
 * alphabet (an extension character such as {@code [} or {@code €} counts 2), otherwise UCS-2,
 * counted in UTF-16 code units. The configured limit is three concatenated SMS parts: 459
 * characters in GSM 7-bit and 201 in UCS-2 (a Group_058 assumption), read from {@code
 * warning.sms.gsm-limit} and {@code warning.sms.unicode-limit} with those defaults.
 *
 * <p>A helper (MD section 6, PD6): it evaluates the rules and returns the errors; the calling
 * control keeps its operation and decides what follows. Extracted so that both controls use one
 * rule (L03 Duplicate Code; L04 Extract Class). Implements CH-22 and CH-06 (FC-15, P60 N3, P62
 * FX-09).
 */
@Component
public class WarningTextValidator {

  /** Three GSM 7-bit parts of 153 characters (a Group_058 assumption, SC1 conventions). */
  static final int DEFAULT_GSM_LIMIT = 459;

  /** Three UCS-2 parts of 67 characters (a Group_058 assumption, SC1 conventions). */
  static final int DEFAULT_UNICODE_LIMIT = 201;

  /** The GSM 03.38 default alphabet; each of its characters counts 1. */
  private static final String GSM_BASIC_ALPHABET =
      "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
          + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";

  /** The GSM 03.38 extension characters; each is sent as an escape plus a code, so counts 2. */
  private static final String GSM_EXTENSION_ALPHABET = "^{}\\[~]|€";

  private static final int GSM_EXTENSION_LENGTH = 2;

  private final Clock clock;

  private final int gsmLimit;

  private final int unicodeLimit;

  /**
   * Creates the validator.
   *
   * @param clock supplies the time the expiry time is judged against
   * @param gsmLimit the SMS limit for a text in the GSM 7-bit alphabet
   * @param unicodeLimit the SMS limit for a text that needs UCS-2
   * @throws NullPointerException if {@code clock} is {@code null}
   */
  public WarningTextValidator(
      Clock clock,
      @Value("${warning.sms.gsm-limit:" + DEFAULT_GSM_LIMIT + "}") int gsmLimit,
      @Value("${warning.sms.unicode-limit:" + DEFAULT_UNICODE_LIMIT + "}") int unicodeLimit) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.gsmLimit = gsmLimit;
    this.unicodeLimit = unicodeLimit;
  }

  /**
   * Checks the text rules of a warning or an escalation.
   *
   * @param message the message in the three languages, or {@code null} if none was entered
   * @param instructions the instructions in the three languages, or {@code null} if none were
   *     entered
   * @param channels the channels chosen; the SMS length rule applies only when SMS is among them
   * @param expiryTime the end of the validity, or {@code null} if none was entered
   * @return the errors in the officer's words, for example "Tamil message and instructions: 236 /
   *     201 characters"; empty if the texts are valid
   * @throws NullPointerException if {@code channels} is {@code null}
   */
  public List<String> validate(
      LocalisedText message,
      LocalisedText instructions,
      Set<NotificationChannel> channels,
      LocalDateTime expiryTime) {
    boolean smsChosen = channels.contains(NotificationChannel.SMS);
    List<String> errors = new ArrayList<>();
    for (Language language : Language.values()) {
      checkLanguage(
          labelOf(language),
          textIn(message, language),
          textIn(instructions, language),
          smsChosen,
          errors);
    }
    checkExpiry(expiryTime, errors);
    return errors;
  }

  private void checkLanguage(
      String label,
      String messageText,
      String instructionsText,
      boolean smsChosen,
      List<String> errors) {
    boolean messageMissing = isBlank(messageText);
    boolean instructionsMissing = isBlank(instructionsText);
    if (messageMissing) {
      errors.add(label + " message is required");
    }
    if (instructionsMissing) {
      errors.add(label + " instructions are required");
    }
    if (smsChosen && !messageMissing && !instructionsMissing) {
      String sms = messageText + Notification.SMS_TEXT_SEPARATOR + instructionsText;
      boolean gsm = isGsm7(sms);
      int length = gsm ? gsm7Length(sms) : sms.length();
      int limit = gsm ? gsmLimit : unicodeLimit;
      if (length > limit) {
        errors.add(
            String.format(
                Locale.ROOT,
                "%s message and instructions: %d / %d characters",
                label,
                length,
                limit));
      }
    }
  }

  private void checkExpiry(LocalDateTime expiryTime, List<String> errors) {
    if (expiryTime == null) {
      errors.add("An expiry time is required");
    } else if (!expiryTime.isAfter(LocalDateTime.now(clock))) {
      errors.add("The expiry time must be after now");
    }
  }

  private static String textIn(LocalisedText text, Language language) {
    return text == null ? null : text.textIn(language);
  }

  private static boolean isBlank(String text) {
    return text == null || text.isBlank();
  }

  private static boolean isGsm7(String text) {
    return text.chars()
        .allMatch(
            c -> GSM_BASIC_ALPHABET.indexOf(c) >= 0 || GSM_EXTENSION_ALPHABET.indexOf(c) >= 0);
  }

  private static int gsm7Length(String text) {
    return text.chars()
        .map(c -> GSM_EXTENSION_ALPHABET.indexOf(c) >= 0 ? GSM_EXTENSION_LENGTH : 1)
        .sum();
  }

  /** "SINHALA" becomes "Sinhala", the label the form shows beside the field. */
  private static String labelOf(Language language) {
    String name = language.name();
    return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
  }
}
