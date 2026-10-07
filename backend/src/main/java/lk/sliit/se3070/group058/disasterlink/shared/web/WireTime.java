package lk.sliit.se3070.group058.disasterlink.shared.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * The one place that converts between the times of the system and the times on the wire (model
 * dictionary section 0.3, section 6). The system keeps {@code LocalDateTime} in Asia/Colombo; on
 * the wire a time is ISO-8601 with its offset (for example {@code 2026-05-21T08:15:00+05:30}).
 * Every web mapper uses these two conversions and the zone constant, so no other class holds a copy
 * of the zone (L03 Duplicate Code; P155 B-08).
 */
public final class WireTime {

  /** The zone all times of the system are in. */
  public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

  private WireTime() {
    throw new AssertionError("WireTime is a utility class");
  }

  /**
   * Gives a stored time its offset for the wire.
   *
   * @param time a time in Asia/Colombo, or {@code null}
   * @return the same moment with its offset, or {@code null} if {@code time} is {@code null}
   */
  public static OffsetDateTime toWire(LocalDateTime time) {
    return time == null ? null : time.atZone(ZONE).toOffsetDateTime();
  }

  /**
   * Reads a time from the wire. A time sent with another offset is converted to the same moment in
   * Asia/Colombo.
   *
   * @param time a time with an offset, or {@code null}
   * @return the time in Asia/Colombo, or {@code null} if {@code time} is {@code null}
   */
  public static LocalDateTime fromWire(OffsetDateTime time) {
    return time == null ? null : time.atZoneSameInstant(ZONE).toLocalDateTime();
  }
}
