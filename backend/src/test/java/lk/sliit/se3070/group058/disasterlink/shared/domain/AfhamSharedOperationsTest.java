package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of Afham's operation on the shared entities, {@link Citizen#changePreferredLanguage}
 * (implementation standards section 3, "Operations on shared entities"; P155 X4).
 */
class AfhamSharedOperationsTest {

  @Test
  @DisplayName("SD2 2.1.1.1: changePreferredLanguage sets the citizen's language")
  void changePreferredLanguage() {
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", null);
    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.SINHALA);

    citizen.changePreferredLanguage(Language.TAMIL);

    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.TAMIL);
  }

  @Test
  @DisplayName("SD2 2.1.1.1: changePreferredLanguage refuses a missing language")
  void changePreferredLanguageRefusesNull() {
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", null);

    assertThatNullPointerException()
        .isThrownBy(() -> citizen.changePreferredLanguage(null))
        .withMessageContaining("language");
    assertThat(citizen.getPreferredLanguage()).isEqualTo(Language.SINHALA);
  }
}
