package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests of {@link LocalisedText}. */
class LocalisedTextTest {

  private static final LocalisedText TEXT = new LocalisedText("si", "ta", "en");

  @ParameterizedTest(name = "{0} returns {1}")
  @CsvSource({"SINHALA,si", "TAMIL,ta", "ENGLISH,en"})
  @DisplayName("textIn returns the text of the language asked for")
  void textInReturnsTheTextOfTheLanguage(Language language, String expected) {
    assertThat(TEXT.textIn(language)).isEqualTo(expected);
  }

  @Test
  @DisplayName("textIn rejects a null language")
  void textInRejectsNullLanguage() {
    assertThatNullPointerException().isThrownBy(() -> TEXT.textIn(null));
  }

  @Test
  @DisplayName("A language that was not entered is null, and textIn returns it as null (P155 X1)")
  void missingLanguageIsAccepted() {
    LocalisedText text = new LocalisedText(null, "ta", null);

    assertThat(text.textIn(Language.SINHALA)).isNull();
    assertThat(text.textIn(Language.TAMIL)).isEqualTo("ta");
    assertThat(text.textIn(Language.ENGLISH)).isNull();
  }
}
