package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Documents Afham's placeholder operation on the shared entities. It throws until he writes the
 * body on his branch; he replaces this file's test with the real tests of the operation
 * (implementation standards section 3, P155 X4).
 */
class AfhamSharedOperationsTest {

  @Test
  @DisplayName("Citizen.changePreferredLanguage is Afham's placeholder")
  void changePreferredLanguage() {
    Citizen citizen = SampleEntities.citizen("CIT-KLT-0001", null);

    assertThatThrownBy(() -> citizen.changePreferredLanguage(Language.TAMIL))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Afham");
  }
}
