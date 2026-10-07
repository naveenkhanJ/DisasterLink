package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * The {@code ClientMeta} of a write on the wire; {@code capturedAt} is ISO-8601 with its offset
 * (for example {@code 2026-05-21T14:05:00+05:30}). Bean Validation checks only that the parts are
 * present.
 *
 * <p>Implementation-only wire record.
 *
 * @param clientId the browser's client identifier
 * @param userId the acting officer
 * @param submissionId the identifier of this submission
 * @param capturedAt when the entry was made
 * @param pendingCount the queued items other than this one
 */
public record ClientMetaRequest(
    @NotNull String clientId,
    @NotNull String userId,
    @NotNull String submissionId,
    @NotNull OffsetDateTime capturedAt,
    @NotNull Integer pendingCount) {}
