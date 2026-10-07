package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;

/**
 * A shelter on the wire: the fields the UC3 page shows, never the entity (standards section 4, "No
 * entities on the wire").
 *
 * <p>Implementation-only wire record.
 *
 * @param shelterId the identifier
 * @param name the name
 * @param address the address
 * @param district the district
 * @param capacity the capacity
 * @param currentOccupancy the current occupancy
 * @param contactNumber the contact number
 * @param status OPEN or FULL
 * @param operatorName the name of the operating organisation, or {@code null} if there is none
 */
public record ShelterResponse(
    String shelterId,
    String name,
    String address,
    District district,
    int capacity,
    int currentOccupancy,
    String contactNumber,
    ShelterStatus status,
    String operatorName) {}
