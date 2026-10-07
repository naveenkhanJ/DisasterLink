package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The body of an occupancy update; the shelter is in the path. Bean Validation checks only that the
 * parts are present, so a negative total reaches {@code updateOccupancy} and comes back as a
 * REJECTED INVALID_VALUE reply.
 *
 * <p>Implementation-only wire record.
 *
 * @param newTotal the new total number of people in the shelter
 * @param meta the client meta
 */
public record OccupancyRequest(@NotNull Integer newTotal, @NotNull @Valid ClientMetaRequest meta) {}
