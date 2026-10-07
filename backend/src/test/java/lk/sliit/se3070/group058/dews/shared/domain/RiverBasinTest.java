package lk.sliit.se3070.group058.dews.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests of {@link RiverBasin}. */
class RiverBasinTest {

  @Test
  @DisplayName("The Kalu basin covers Ratnapura and Kalutara, as scenario SC1 states")
  void kaluBasinCoversRatnapuraAndKalutara() {
    assertThat(RiverBasin.KALU.districts())
        .containsExactlyInAnyOrder(District.RATNAPURA, District.KALUTARA);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(RiverBasin.class)
  @DisplayName("Every basin lies in at least one district")
  void everyBasinHasDistricts(RiverBasin basin) {
    assertThat(basin.districts()).isNotEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(RiverBasin.class)
  @DisplayName("The set of districts cannot be changed")
  void districtSetIsUnmodifiable(RiverBasin basin) {
    assertThatThrownBy(() -> basin.districts().add(District.JAFFNA))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("The dictionary's nine basins exist")
  void nineBasinsExist() {
    assertThat(RiverBasin.values())
        .extracting(Enum::name)
        .containsExactly(
            "KELANI",
            "KALU",
            "GIN",
            "NILWALA",
            "MAHAWELI",
            "ATTANAGALU",
            "WALAWE",
            "DEDURU",
            "MAHA_OYA");
  }
}
