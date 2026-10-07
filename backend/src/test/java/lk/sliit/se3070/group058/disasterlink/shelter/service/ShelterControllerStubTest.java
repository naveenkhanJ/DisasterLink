package lk.sliit.se3070.group058.disasterlink.shelter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientMeta;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OccupancyUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterSubmission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Documents the signature stub {@link ShelterController}: every operation throws with the owner's
 * name until Weliwitigoda writes it. He deletes this test when he replaces the bodies.
 */
class ShelterControllerStubTest {

  private static final ShelterController CONTROLLER = new ShelterController();
  private static final ClientMeta META =
      new ClientMeta("client-1", "DO-COL-01", "sub-1", LocalDateTime.of(2026, 5, 21, 8, 0), 0);
  private static final ShelterDetails DETAILS =
      new ShelterDetails("Hall", "Road", District.COLOMBO, 50, "0112345678", null);

  static Stream<Arguments> operations() {
    return Stream.of(
        op("getDistrictShelters", () -> CONTROLLER.getDistrictShelters("DO-COL-01")),
        op("getOrganizations", CONTROLLER::getOrganizations),
        op("getShelterDetails", () -> CONTROLLER.getShelterDetails("SHL-1")),
        op(
            "findAlternativeShelters",
            () -> CONTROLLER.findAlternativeShelters(District.COLOMBO, 10)),
        op(
            "updateOccupancy",
            () -> CONTROLLER.updateOccupancy(new OccupancyUpdate("SHL-1", 5, META))),
        op("validateShelterDetails", () -> CONTROLLER.validateShelterDetails(DETAILS, null)),
        op(
            "registerShelter",
            () -> CONTROLLER.registerShelter(new ShelterSubmission(DETAILS, META))),
        op(
            "updateShelterDetails",
            () -> CONTROLLER.updateShelterDetails("SHL-1", new ShelterSubmission(DETAILS, META))));
  }

  private static Arguments op(String name, Executable call) {
    return Arguments.of(name, call);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("operations")
  @DisplayName("Every operation is Weliwitigoda's placeholder")
  void everyOperationThrowsWithTheOwnersName(String name, Executable call) {
    assertThatThrownBy(call::execute)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Weliwitigoda");
  }

  @Test
  @DisplayName("The stub lists all eight operations of the dictionary row")
  void eightOperations() {
    assertThat(operations().count()).isEqualTo(8);
  }
}
