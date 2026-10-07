package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests of {@link DecisionNoticeText}. */
class DecisionNoticeTextTest {

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "ENGLISH|VERIFIED|Your hazard report RPT-1 was verified. Thank you.",
        "SINHALA|VERIFIED|ඔබගේ උපද්‍රව වාර්තාව RPT-1 තහවුරු කරන ලදී. ස්තූතියි.",
        "TAMIL|VERIFIED|உங்கள் அபாய அறிக்கை RPT-1 உறுதிப்படுத்தப்பட்டது. நன்றி.",
        "ENGLISH|REJECTED|Your hazard report RPT-1 was not accepted: Not a flood",
        "SINHALA|REJECTED|ඔබගේ උපද්‍රව වාර්තාව RPT-1 පිළිගනු නොලැබීය: Not a flood",
        "TAMIL|REJECTED|உங்கள் அபாய அறிக்கை RPT-1 ஏற்றுக்கொள்ளப்படவில்லை: Not a flood"
      })
  @DisplayName("A notice is worded in the recipient's language for each decision")
  void rendersEachLanguageAndDecision(Language language, ReportStatus status, String expected) {
    assertThat(DecisionNoticeText.render(language, status, "RPT-1", "Not a flood"))
        .isEqualTo(expected);
  }

  @Test
  @DisplayName(
      "A verified notice ignores the reason, and a rejected one without a reason ends empty")
  void reasonIsOptionalForVerified() {
    assertThat(DecisionNoticeText.render(Language.ENGLISH, ReportStatus.VERIFIED, "RPT-1", null))
        .isEqualTo("Your hazard report RPT-1 was verified. Thank you.");
    assertThat(DecisionNoticeText.render(Language.ENGLISH, ReportStatus.REJECTED, "RPT-1", null))
        .isEqualTo("Your hazard report RPT-1 was not accepted: ");
  }

  @Test
  @DisplayName("A report that is still pending has no notice")
  void pendingReportHasNoNotice() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                DecisionNoticeText.render(
                    Language.ENGLISH, ReportStatus.PENDING_VERIFICATION, "RPT-1", null));
  }

  @Test
  @DisplayName("The utility class cannot be instantiated")
  void constructorThrowsAssertionError() throws ReflectiveOperationException {
    Constructor<DecisionNoticeText> constructor = DecisionNoticeText.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    org.assertj.core.api.Assertions.assertThatThrownBy(constructor::newInstance)
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(AssertionError.class);
  }
}
