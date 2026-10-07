package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link WireTime}: the zone and the two conversions (P155 B-08). */
class WireTimeTest {

  @Test
  @DisplayName("The zone is Asia/Colombo")
  void zoneIsColombo() {
    assertThat(WireTime.ZONE).isEqualTo(ZoneId.of("Asia/Colombo"));
  }

  @Test
  @DisplayName("toWire gives a stored time the +05:30 offset")
  void toWireAddsTheOffset() {
    OffsetDateTime wire = WireTime.toWire(LocalDateTime.of(2026, 5, 21, 8, 15));

    assertThat(wire).hasToString("2026-05-21T08:15+05:30");
    assertThat(wire.getOffset()).isEqualTo(ZoneOffset.ofHoursMinutes(5, 30));
  }

  @Test
  @DisplayName("fromWire keeps a +05:30 time as it is")
  void fromWireKeepsColomboTimes() {
    LocalDateTime time = WireTime.fromWire(OffsetDateTime.parse("2026-05-21T14:05:00+05:30"));

    assertThat(time).isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 5));
  }

  @Test
  @DisplayName("fromWire converts another offset to the same moment in Colombo")
  void fromWireConvertsOtherOffsets() {
    assertThat(WireTime.fromWire(OffsetDateTime.parse("2026-05-21T08:35:00Z")))
        .isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 5));
    assertThat(WireTime.fromWire(OffsetDateTime.parse("2026-05-21T20:00:00-05:00")))
        .isEqualTo(LocalDateTime.of(2026, 5, 22, 6, 30));
  }

  @Test
  @DisplayName("Midnight crosses to the next day when the offset moves it")
  void midnightCrossesTheDate() {
    assertThat(WireTime.fromWire(OffsetDateTime.parse("2026-05-21T18:30:00Z")))
        .isEqualTo(LocalDateTime.of(2026, 5, 22, 0, 0));
    assertThat(WireTime.toWire(LocalDateTime.of(2026, 5, 22, 0, 0)).toInstant())
        .isEqualTo(OffsetDateTime.parse("2026-05-21T18:30:00Z").toInstant());
  }

  @Test
  @DisplayName("null in gives null out, in both directions")
  void nullStaysNull() {
    assertThat(WireTime.toWire(null)).isNull();
    assertThat(WireTime.fromWire(null)).isNull();
  }
}
