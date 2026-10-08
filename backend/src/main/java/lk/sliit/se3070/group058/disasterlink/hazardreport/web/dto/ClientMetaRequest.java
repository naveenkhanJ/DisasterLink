package lk.sliit.se3070.group058.disasterlink.hazardreport.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.OffsetDateTime;

/**
 * The {@code meta} part of a report upload: the {@code ClientMeta} on the wire, with {@code
 * capturedAt} as ISO-8601 with its offset. Bean Validation checks only that the parts {@code
 * ClientMeta} requires are present.
 *
 * <p>Implementation-only wire record. It mirrors the UC3 record of the same name, which stays in
 * the shelter feature: each feature owns its DTOs.
 *
 * @param clientId the identifier of the phone
 * @param userId the signed-in user, the reporter
 * @param submissionId the identifier that makes the upload idempotent (the draft's identifier)
 * @param capturedAt when the report was captured on the phone
 * @param pendingCount the queued drafts other than this one
 */
public record ClientMetaRequest(
    @NotBlank String clientId,
    @NotBlank String userId,
    @NotBlank String submissionId,
    @NotNull OffsetDateTime capturedAt,
    @NotNull @PositiveOrZero Integer pendingCount) {}
