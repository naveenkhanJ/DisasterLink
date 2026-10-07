package lk.sliit.se3070.group058.dews.shared.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * A text written in the three languages of the system.
 *
 * <p>Implements CH-21.
 *
 * @param sinhala the text in Sinhala
 * @param tamil the text in Tamil
 * @param english the text in English
 */
@Embeddable
public record LocalisedText(String sinhala, String tamil, String english) {

  /**
   * Creates the text, checking that it exists in all three languages.
   *
   * @throws NullPointerException if a language has no text
   */
  public LocalisedText(String sinhala, String tamil, String english) {
    this.sinhala = Objects.requireNonNull(sinhala, "sinhala");
    this.tamil = Objects.requireNonNull(tamil, "tamil");
    this.english = Objects.requireNonNull(english, "english");
  }

  /**
   * Returns the text in one language.
   *
   * @param language the language wanted
   * @return the text in that language
   * @throws NullPointerException if {@code language} is {@code null}
   */
  public String textIn(Language language) {
    Objects.requireNonNull(language, "language");
    return switch (language) {
      case SINHALA -> sinhala;
      case TAMIL -> tamil;
      case ENGLISH -> english;
    };
  }
}
