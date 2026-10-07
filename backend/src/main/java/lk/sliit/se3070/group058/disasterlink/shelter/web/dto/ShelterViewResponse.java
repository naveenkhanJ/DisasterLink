package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

/**
 * A shelter with its remaining capacity (the {@code ShelterView} of the model dictionary).
 *
 * <p>Implementation-only wire record.
 *
 * @param shelter the shelter
 * @param remainingCapacity the places still free
 */
public record ShelterViewResponse(ShelterResponse shelter, int remainingCapacity) {}
