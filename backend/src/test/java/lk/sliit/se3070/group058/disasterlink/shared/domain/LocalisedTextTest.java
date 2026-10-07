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
  @DisplayName("A text needs all three languages")
  void allThreeLanguagesAreRequired() {
    assertThatNullPointerException().isThrownBy(() -> new LocalisedText(null, "ta", "en"));
    assertThatNullPointerException().isThrownBy(() -> new LocalisedText("si", null, "en"));
    assertThatNullPointerException().isThrownBy(() -> new LocalisedText("si", "ta", null));
  }
}
