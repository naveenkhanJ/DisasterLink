package lk.sliit.se3070.group058.disasterlink.shelter.web.dto;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;

/**
 * The reply of every UC3 write: the typed result, the shelter (present on ACCEPTED and on
 * OVER_CAPACITY), the remaining capacity and the alternatives. A REJECTED result travels in a
 * normal HTTP 200 reply.
 *
 * <p>Implementation-only wire record.
 *
 * @param result the outcome, the typed reason of a rejection and the message
 * @param shelter the shelter, or {@code null}
 * @param remainingCapacity the places still free
 * @param alternatives the alternative shelters, possibly empty
 */
public record ShelterOutcomeResponse(
    SyncResult result,
    ShelterResponse shelter,
    int remainingCapacity,
    List<ShelterViewResponse> alternatives) {}
