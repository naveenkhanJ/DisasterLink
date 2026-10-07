package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import jakarta.validation.constraints.NotNull;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;

/**
 * The {@code ShelterDetails} on the wire. An unentered name, address, district or contact number is
 * {@code null} and reaches {@code validateShelterDetails}, which reports it as an error (SC3 E2; MD
 * section 0.13, P155 X1), so Bean Validation does not check them. {@code capacity} is required
 * because the page blocks a non-numeric capacity itself, as it blocks a non-numeric occupancy (a
 * boundary-only rule, MD section 4 {@code showInvalidOccupancy}); a capacity below 1 is still
 * judged by {@code validateShelterDetails}.
 *
 * <p>Implementation-only wire record.
 *
 * @param name the name, or {@code null} if not entered
 * @param address the address, or {@code null} if not entered
 * @param district the district, or {@code null} if not entered
 * @param capacity the capacity
 * @param contactNumber the contact number, or {@code null} if not entered
 * @param operatorId the operating organisation, or {@code null}
 */
public record ShelterDetailsRequest(
    String name,
    String address,
    District district,
    @NotNull Integer capacity,
    String contactNumber,
    String operatorId) {}
