package lk.sliit.se3070.group058.disasterlink.shelter.domain;

import java.util.List;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SyncResult;

/**
 * The reply of every shelter write, online or replayed. Over-capacity and invalid values are
 * rejected replies, never exceptions.
 *
 * <p>Implements CH-28 and CH-29; it lives beside the entity it references.
 *
 * @param result the outcome, with the typed reason and the text when rejected
 * @param shelter the shelter, present when accepted and when rejected for over-capacity, otherwise
 *     {@code null}
 * @param remainingCapacity the number of free places
 * @param alternatives other shelters with space
 */
public record ShelterOutcome(
    SyncResult result, Shelter shelter, int remainingCapacity, List<ShelterView> alternatives) {

  /**
   * Creates the outcome, copying the list of alternatives.
   *
   * @throws NullPointerException if {@code result} or {@code alternatives} is {@code null}
   */
  public ShelterOutcome(
      SyncResult result, Shelter shelter, int remainingCapacity, List<ShelterView> alternatives) {
    this.result = Objects.requireNonNull(result, "result");
    this.shelter = shelter;
    this.remainingCapacity = remainingCapacity;
    this.alternatives = List.copyOf(alternatives);
  }
}
