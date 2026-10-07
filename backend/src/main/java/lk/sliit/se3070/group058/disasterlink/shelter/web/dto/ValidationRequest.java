package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The body of the online check of shelter details.
 *
 * <p>Implementation-only wire record.
 *
 * @param details the details to check
 * @param shelterId the shelter being edited, or {@code null} for a registration
 */
public record ValidationRequest(@NotNull @Valid ShelterDetailsRequest details, String shelterId) {}
