package lk.sliit.se3070.group058.disasterlink.shared.notification;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Maps outbox entries to their wire records. Stored times are local to Asia/Colombo; on the wire
 * they carry the offset (model dictionary section 0.3).
 */
public final class PushEntryMapper {

  private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

  private PushEntryMapper() {
    throw new AssertionError("PushEntryMapper is a utility class");
  }

  /**
   * Maps an entry to its wire record.
   *
   * @param entry the entry
   * @return the record for the JSON reply
   */
  public static PushEntryResponse toResponse(PushOutbox entry) {
    return new PushEntryResponse(
        entry.getId(),
        entry.getKind(),
        entry.getWarningId().orElse(null),
        entry.getWarningLevel().orElse(null),
        entry.getReportId().orElse(null),
        entry.getStatus().orElse(null),
        entry.getReason().orElse(null),
        entry.getText(),
        withOffset(entry.getCreatedAt()),
        entry.getDecidedAt().map(PushEntryMapper::withOffset).orElse(null));
  }

  private static OffsetDateTime withOffset(LocalDateTime time) {
    return time.atZone(ZONE).toOffsetDateTime();
  }
}
