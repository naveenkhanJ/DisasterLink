package lk.sliit.se3070.group058.disasterlink.shared.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;

/**
 * One undelivered outbox entry as the app receives it. Fields that are empty for the entry's kind
 * are left out of the JSON. Times carry their offset ({@code +05:30}).
 *
 * <p>Implementation-only wire record (model dictionary section 0.3; no entity on the wire, P62
 * FX-29).
 *
 * @param id the identifier of the entry, used to acknowledge it
 * @param kind the kind of the entry
 * @param warningId the warning of an alert, or {@code null} for a decision
 * @param warningLevel the level of an alert, or {@code null} for a decision
 * @param reportId the report of a decision, or {@code null} for an alert
 * @param status the decision, or {@code null} for an alert
 * @param reason the rejection reason, or {@code null} if there is none
 * @param text the message in the recipient's language
 * @param createdAt when the entry was written
 * @param decidedAt when the officer decided, or {@code null} for an alert
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PushEntryResponse(
    String id,
    PushKind kind,
    String warningId,
    WarningLevel warningLevel,
    String reportId,
    ReportStatus status,
    String reason,
    String text,
    OffsetDateTime createdAt,
    OffsetDateTime decidedAt) {}
