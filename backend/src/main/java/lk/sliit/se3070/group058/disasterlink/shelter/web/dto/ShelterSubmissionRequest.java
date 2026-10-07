package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The {@code ShelterSubmission} on the wire: the body of a registration and of a detail change.
 *
 * <p>Implementation-only wire record.
 *
 * @param details the shelter details
 * @param meta the client meta
 */
public record ShelterSubmissionRequest(
    @NotNull @Valid ShelterDetailsRequest details, @NotNull @Valid ClientMetaRequest meta) {}
