package lk.sliit.se3070.group058.disasterlink.warning.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardSeverity;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardUpdate;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Documents the signature stub {@link WarningController}: every operation throws with the owner's
 * name until Lakmal writes it. He deletes this test when he replaces the bodies.
 */
class WarningControllerStubTest {

  private static final WarningController CONTROLLER = new WarningController();
  private static final LocalisedText TEXT = new LocalisedText("si", "ta", "en");
  private static final WarningDetails DETAILS =
      new WarningDetails(
          WarningLevel.LEVEL_2_AMBER,
          TEXT,
          TEXT,
          List.of(District.COLOMBO),
          List.of(),
          Set.of(NotificationChannel.SMS),
          LocalDateTime.of(2026, 5, 22, 8, 0));

  static Stream<Arguments> operations() {
    return Stream.of(
        op("getAssessableHazards", CONTROLLER::getAssessableHazards),
        op("getHazardDetails", () -> CONTROLLER.getHazardDetails("HZ-1")),
        op("getActiveWarningForHazard", () -> CONTROLLER.getActiveWarningForHazard("HZ-1")),
        op(
            "updateHazard",
            () ->
                CONTROLLER.updateHazard(
                    "HZ-1", new HazardUpdate("d", HazardSeverity.LOW, List.of()))),
        op("validateWarning", () -> CONTROLLER.validateWarning("HZ-1", DETAILS)),
        op("previewWarning", () -> CONTROLLER.previewWarning("HZ-1", DETAILS)),
        op("issueWarning", () -> CONTROLLER.issueWarning("HZ-1", DETAILS, "DMC-1")),
        op("resendOnChannel", () -> CONTROLLER.resendOnChannel("WRN-1", NotificationChannel.SMS)),
        op("getBroadcastSummary", () -> CONTROLLER.getBroadcastSummary("WRN-1")),
        op("getActiveWarnings", () -> CONTROLLER.getActiveWarnings(District.COLOMBO)));
  }

  private static Arguments op(String name, Executable call) {
    return Arguments.of(name, call);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("operations")
  @DisplayName("Every operation is Lakmal's placeholder")
  void everyOperationThrowsWithTheOwnersName(String name, Executable call) {
    assertThatThrownBy(call::execute)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("not implemented yet — Lakmal");
  }

  @Test
  @DisplayName("The stub lists all ten operations of the dictionary row")
  void tenOperations() {
    org.assertj.core.api.Assertions.assertThat(operations().count()).isEqualTo(10);
  }
}
