package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Objects;

/**
 * A new occupancy total for a shelter.
 *
 * <p>Implements CH-28.
 *
 * @param shelterId the identifier of the shelter
 * @param newTotal the new total number of occupants
 * @param meta the client metadata of the write
 */
public record OccupancyUpdate(String shelterId, int newTotal, ClientMeta meta) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public OccupancyUpdate(String shelterId, int newTotal, ClientMeta meta) {
    this.shelterId = Objects.requireNonNull(shelterId, "shelterId");
    this.newTotal = newTotal;
    this.meta = Objects.requireNonNull(meta, "meta");
  }
}
