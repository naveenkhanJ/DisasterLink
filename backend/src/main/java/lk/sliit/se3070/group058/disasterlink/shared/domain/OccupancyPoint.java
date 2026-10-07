package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One point of a shelter's occupancy over time.
 *
 * <p>Implements CH-14 and CH-33.
 *
 * @param shelterId the identifier of the shelter
 * @param district the district of the shelter
 * @param capturedAt when the occupancy record was captured
 * @param occupantCount the number of occupants
 * @param capacity the shelter's current capacity
 */
public record OccupancyPoint(
    String shelterId,
    District district,
    LocalDateTime capturedAt,
    int occupantCount,
    int capacity) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public OccupancyPoint(
      String shelterId,
      District district,
      LocalDateTime capturedAt,
      int occupantCount,
      int capacity) {
    this.shelterId = Objects.requireNonNull(shelterId, "shelterId");
    this.district = Objects.requireNonNull(district, "district");
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    this.occupantCount = occupantCount;
    this.capacity = capacity;
  }
}
