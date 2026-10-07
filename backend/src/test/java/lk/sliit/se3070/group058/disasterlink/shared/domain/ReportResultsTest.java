package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link ReportResults}. */
class ReportResultsTest {

  @Test
  @DisplayName("merge is a placeholder owned by Naveenkhan and throws until he writes it")
  void mergeIsNotImplementedYet() {
    ReportResults results =
        new ReportResults(0, List.of(), Map.of(), 0, List.of(), 0, List.of(), Set.of());

    assertThatThrownBy(() -> results.merge(results))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Naveenkhan");
  }
}
