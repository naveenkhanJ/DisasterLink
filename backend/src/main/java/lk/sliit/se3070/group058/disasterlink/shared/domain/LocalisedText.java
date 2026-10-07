package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * A text written in the three languages of the system.
 *
 * <p>Implements CH-21.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a language the officer has not written is
 * {@code null}, so that {@code WarningTextValidator} reports the missing text.
 *
 * @param sinhala the text in Sinhala, or {@code null} if not entered — checked by {@code
 *     WarningTextValidator}
 * @param tamil the text in Tamil, or {@code null} if not entered — checked by {@code
 *     WarningTextValidator}
 * @param english the text in English, or {@code null} if not entered — checked by {@code
 *     WarningTextValidator}
 */
@Embeddable
public record LocalisedText(String sinhala, String tamil, String english) {

  /** Creates the text; a language may be {@code null} (not entered). */
  public LocalisedText(String sinhala, String tamil, String english) {
    this.sinhala = sinhala;
    this.tamil = tamil;
    this.english = english;
  }

  /**
   * Returns the text in one language.
   *
   * @param language the language wanted
   * @return the text in that language, or {@code null} if it was not entered
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
