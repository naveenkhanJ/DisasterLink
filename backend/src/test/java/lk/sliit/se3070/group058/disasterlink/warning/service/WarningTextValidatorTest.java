package lk.sliit.se3070.group058.disasterlink.warning.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link WarningTextValidator}: the three languages, the SMS length rule and expiry. */
class WarningTextValidatorTest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 45);

  private static final LocalDateTime TOMORROW = NOW.plusDays(1);

  private static final Set<NotificationChannel> WITH_SMS =
      Set.of(NotificationChannel.PUSH, NotificationChannel.SMS, NotificationChannel.AUDIBLE_ALERT);

  /** SC1 step 9: 116 characters. */
  private static final String ENGLISH_MESSAGE =
      "DMC FLOOD WARNING - Level 2 Amber: Kalu Ganga rising in Ratnapura and Kalutara."
          + " Prepare to evacuate low-lying areas.";

  /** SC1 step 9: 122 characters. */
  private static final String ENGLISH_INSTRUCTIONS =
      "Move documents and valuables to higher ground. If told to evacuate, go to the safety"
          + " centre named by your Grama Niladhari.";

  private static final String SINHALA = "කළු ගඟ ගංවතුර අනතුරු ඇඟවීම";

  private static final String TAMIL = "களு கங்கை வெள்ள எச்சரிக்கை";

  private final WarningTextValidator validator =
      new WarningTextValidator(
          Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO),
          WarningTextValidator.DEFAULT_GSM_LIMIT,
          WarningTextValidator.DEFAULT_UNICODE_LIMIT);

  private static LocalisedText message(String english) {
    return new LocalisedText(SINHALA, TAMIL, english);
  }

  @Test
  @DisplayName("Main step 11 — the scenario's texts are valid (English 116 + 1 + 122 = 239 / 459)")
  void acceptsTheScenarioTexts() {
    assertThat(ENGLISH_MESSAGE).hasSize(116);
    assertThat(ENGLISH_INSTRUCTIONS).hasSize(122);

    assertThat(
            validator.validate(
                message(ENGLISH_MESSAGE), message(ENGLISH_INSTRUCTIONS), WITH_SMS, TOMORROW))
        .isEmpty();
  }

  @Test
  @DisplayName("E3 11d — a Tamil SMS over 201 and empty Sinhala instructions are both reported")
  void reportsTooLongTamilAndMissingSinhala() {
    LocalisedText instructions = new LocalisedText(null, "த".repeat(200), ENGLISH_INSTRUCTIONS);
    LocalisedText messageText = new LocalisedText(SINHALA, "க".repeat(35), ENGLISH_MESSAGE);

    assertThat(validator.validate(messageText, instructions, WITH_SMS, TOMORROW))
        .containsExactly(
            "Sinhala instructions are required",
            "Tamil message and instructions: 236 / 201 characters");
  }

  @Test
  @DisplayName("E3 11d — a missing message is required in every language")
  void requiresTheMessageInEveryLanguage() {
    assertThat(validator.validate(null, message(ENGLISH_INSTRUCTIONS), WITH_SMS, TOMORROW))
        .containsExactly(
            "Sinhala message is required",
            "Tamil message is required",
            "English message is required");
  }

  @Test
  @DisplayName("E3 11d — a blank text counts as not entered")
  void blankTextIsMissing() {
    LocalisedText blankEnglish = new LocalisedText(SINHALA, TAMIL, "  ");

    assertThat(validator.validate(blankEnglish, message("x"), WITH_SMS, TOMORROW))
        .containsExactly("English message is required");
  }

  @Test
  @DisplayName("E3 — an English SMS of exactly 459 GSM characters fits, 460 does not")
  void gsmLimitBoundary() {
    LocalisedText instructions = message("b".repeat(229));

    assertThat(validator.validate(message("a".repeat(229)), instructions, WITH_SMS, TOMORROW))
        .isEmpty();
    assertThat(validator.validate(message("a".repeat(230)), instructions, WITH_SMS, TOMORROW))
        .containsExactly("English message and instructions: 460 / 459 characters");
  }

  @Test
  @DisplayName("E3 — a GSM extension character such as € counts 2")
  void extensionCharacterCountsTwo() {
    LocalisedText messageText = message("a".repeat(228) + "€");

    assertThat(validator.validate(messageText, message("b".repeat(229)), WITH_SMS, TOMORROW))
        .containsExactly("English message and instructions: 460 / 459 characters");
  }

  @Test
  @DisplayName("E3 — an en dash forces English into UCS-2, with the 201 limit")
  void nonGsmCharacterForcesUnicode() {
    LocalisedText messageText = message("Level 2 – Amber " + "a".repeat(150));

    assertThat(validator.validate(messageText, message("b".repeat(40)), WITH_SMS, TOMORROW))
        .containsExactly("English message and instructions: 207 / 201 characters");
  }

  @Test
  @DisplayName("E3 — a Sinhala zero-width joiner counts as one UTF-16 unit")
  void zeroWidthJoinerCounts() {
    LocalisedText messageText = new LocalisedText("ක‍".repeat(100), TAMIL, ENGLISH_MESSAGE);
    LocalisedText instructions = new LocalisedText("ය", TAMIL, ENGLISH_INSTRUCTIONS);

    assertThat(validator.validate(messageText, instructions, WITH_SMS, TOMORROW))
        .containsExactly("Sinhala message and instructions: 202 / 201 characters");
  }

  @Test
  @DisplayName("Main step 11 — without SMS the length rule does not apply")
  void noLengthRuleWithoutSms() {
    Set<NotificationChannel> pushOnly = Set.of(NotificationChannel.PUSH);

    assertThat(
            validator.validate(
                message("a".repeat(600)), message("b".repeat(600)), pushOnly, TOMORROW))
        .isEmpty();
  }

  @Test
  @DisplayName("E3 13g — an expiry time that is not after now is refused")
  void expiryMustBeAfterNow() {
    LocalisedText text = message(ENGLISH_MESSAGE);

    assertThat(validator.validate(text, text, WITH_SMS, NOW))
        .containsExactly("The expiry time must be after now");
    assertThat(validator.validate(text, text, WITH_SMS, NOW.minusMinutes(1)))
        .containsExactly("The expiry time must be after now");
  }

  @Test
  @DisplayName("E3 11d — a missing expiry time is required")
  void expiryIsRequired() {
    LocalisedText text = message(ENGLISH_MESSAGE);

    assertThat(validator.validate(text, text, WITH_SMS, null))
        .containsExactly("An expiry time is required");
  }

  @Test
  @DisplayName("The configured limits are the ones applied")
  void appliesTheConfiguredLimits() {
    WarningTextValidator strict =
        new WarningTextValidator(Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO), 8, 5);
    LocalisedText messageText = new LocalisedText("කකක", "abcd", "ab");
    LocalisedText instructions = new LocalisedText("යයය", "efgh", "cd");

    assertThat(strict.validate(messageText, instructions, WITH_SMS, TOMORROW))
        .containsExactly(
            "Sinhala message and instructions: 7 / 5 characters",
            "Tamil message and instructions: 9 / 8 characters");
  }
}
