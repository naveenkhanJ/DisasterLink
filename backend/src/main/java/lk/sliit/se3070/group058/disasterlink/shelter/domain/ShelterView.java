package lk.sliit.se3070.group058.disasterlink.shelter.domain;

import java.util.Objects;

/**
 * A shelter with the remaining capacity the screens show for it.
 *
 * <p>Implements CH-28 (P85); it lives beside the entity it references.
 *
 * @param shelter the shelter
 * @param remainingCapacity the number of free places, from {@code Shelter.checkAvailability()}
 */
public record ShelterView(Shelter shelter, int remainingCapacity) {

  /**
   * Creates the view.
   *
   * @throws NullPointerException if {@code shelter} is {@code null}
   */
  public ShelterView(Shelter shelter, int remainingCapacity) {
    this.shelter = Objects.requireNonNull(shelter, "shelter");
    this.remainingCapacity = remainingCapacity;
  }
}
