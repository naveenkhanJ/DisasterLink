package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Documents Naveenkhan's placeholder operation on the shared entities. It throws until he writes
 * the body on his branch; he replaces this file's test with the real tests of the operation
 * (implementation standards section 3, P155 X4).
 */
class NaveenkhanSharedOperationsTest {

  @Test
  @DisplayName("ReportResults.merge is Naveenkhan's placeholder")
  void mergeIsNotImplementedYet() {
    ReportResults results =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());

    assertThatThrownBy(() -> results.merge(results))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Naveenkhan");
  }
}
