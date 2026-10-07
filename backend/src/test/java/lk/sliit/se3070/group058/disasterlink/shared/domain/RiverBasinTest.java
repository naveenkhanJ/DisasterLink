package lk.sliit.se3070.group058.disasterlink.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests of {@link RiverBasin}. */
class RiverBasinTest {

  static Stream<Arguments> expectedDistricts() {
    return Stream.of(
        Arguments.of(
            RiverBasin.KELANI,
            new District[] {
              District.COLOMBO,
              District.GAMPAHA,
              District.KEGALLE,
              District.NUWARA_ELIYA,
              District.RATNAPURA
            }),
        Arguments.of(RiverBasin.KALU, new District[] {District.RATNAPURA, District.KALUTARA}),
        Arguments.of(RiverBasin.GIN, new District[] {District.GALLE}),
        Arguments.of(RiverBasin.NILWALA, new District[] {District.MATARA}),
        Arguments.of(
            RiverBasin.MAHAWELI,
            new District[] {
              District.KANDY,
              District.NUWARA_ELIYA,
              District.MATALE,
              District.BADULLA,
              District.AMPARA,
              District.POLONNARUWA,
              District.TRINCOMALEE
            }),
        Arguments.of(RiverBasin.ATTANAGALU, new District[] {District.GAMPAHA}),
        Arguments.of(
            RiverBasin.WALAWE,
            new District[] {
              District.RATNAPURA, District.BADULLA, District.MONARAGALA, District.HAMBANTOTA
            }),
        Arguments.of(RiverBasin.DEDURU, new District[] {District.KURUNEGALA, District.PUTTALAM}),
        Arguments.of(
            RiverBasin.MAHA_OYA,
            new District[] {District.KURUNEGALA, District.GAMPAHA, District.PUTTALAM}));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("expectedDistricts")
  @DisplayName("Each basin lies in exactly the districts of its Irrigation Department warnings")
  void basinHasExactlyTheExpectedDistricts(RiverBasin basin, District[] expected) {
    assertThat(basin.districts()).containsExactlyInAnyOrder(expected);
  }

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
