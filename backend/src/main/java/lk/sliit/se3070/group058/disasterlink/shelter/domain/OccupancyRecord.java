package lk.sliit.se3070.group058.disasterlink.shelter.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;

/**
 * One recording of how many people were in a shelter at a time.
 *
 * <p>Implements CH-14 and CH-05 (A3, A12).
 */
@Entity
public class OccupancyRecord {

  @Id private String recordId;

  private int occupantCount;

  private LocalDateTime capturedAt;

  private LocalDateTime recordedAt;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "officer_id")
  private DistrictOfficer officer;

  /**
   * Creates a record of the occupancy of a shelter.
   *
   * @param occupantCount the number of occupants
   * @param capturedAt when the count was taken
   * @param recordedAt when the server received it
   * @param officer the officer who recorded it
   * @throws NullPointerException if an argument is {@code null}
   */
  public OccupancyRecord(
      int occupantCount,
      LocalDateTime capturedAt,
      LocalDateTime recordedAt,
      DistrictOfficer officer) {
    this.recordId = UUID.randomUUID().toString();
    this.occupantCount = occupantCount;
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    this.officer = Objects.requireNonNull(officer, "officer");
  }

  /** Constructor for JPA only. */
  protected OccupancyRecord() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the record.
   *
   * @return the identifier of the record
   */
  public String getRecordId() {
    return recordId;
  }

  /**
   * Returns the number of occupants recorded.
   *
   * @return the number of occupants recorded
   */
  public int getOccupantCount() {
    return occupantCount;
  }

  /**
   * Returns the time the count was taken.
   *
   * @return the time the count was taken
   */
  public LocalDateTime getCapturedAt() {
    return capturedAt;
  }

  /**
   * Returns the time the server received the record.
   *
   * @return the time the server received the record
   */
  public LocalDateTime getRecordedAt() {
    return recordedAt;
  }

  /**
   * Returns the district officer who recorded it (A12).
   *
   * @return the district officer who recorded it (A12)
   */
  public DistrictOfficer getOfficer() {
    return officer;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof OccupancyRecord that
            && Objects.equals(getRecordId(), that.getRecordId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getRecordId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getRecordId() + "]";
  }
}
