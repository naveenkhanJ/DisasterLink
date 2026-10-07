package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import jakarta.validation.constraints.NotNull;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;

/**
 * The {@code ShelterDetails} on the wire. Bean Validation checks only that the parts are present
 * and of the right type; every rule (capacity at least 1, contact number, district) is judged by
 * {@code validateShelterDetails}.
 *
 * <p>Implementation-only wire record.
 *
 * @param name the name
 * @param address the address
 * @param district the district
 * @param capacity the capacity
 * @param contactNumber the contact number
 * @param operatorId the operating organisation, or {@code null}
 */
public record ShelterDetailsRequest(
    @NotNull String name,
    @NotNull String address,
    @NotNull District district,
    @NotNull Integer capacity,
    @NotNull String contactNumber,
    String operatorId) {}
