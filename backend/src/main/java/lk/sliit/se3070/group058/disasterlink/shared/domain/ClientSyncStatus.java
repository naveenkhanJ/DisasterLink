package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The last contact of a client with the server and the number of items it still holds in its queue.
 *
 * <p>Implements CH-31 (UC4 A3).
 */
@Entity
public class ClientSyncStatus {

  @Id private String clientId;

  @Enumerated(EnumType.STRING)
  private District district;

  private LocalDateTime lastSyncAt;

  private int pendingCount;

  /**
   * Creates the status of a client at its first contact.
   *
   * @throws NullPointerException if an argument is {@code null}
   * @throws IllegalArgumentException if {@code pendingCount} is negative
   */
  public ClientSyncStatus(
      String clientId, District district, LocalDateTime lastSyncAt, int pendingCount) {
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.district = Objects.requireNonNull(district, "district");
    this.lastSyncAt = Objects.requireNonNull(lastSyncAt, "lastSyncAt");
    this.pendingCount = requireNotNegative(pendingCount);
  }

  /** Constructor for JPA only. */
  protected ClientSyncStatus() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the client.
   *
   * @return the identifier of the client
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Returns the district of the client's user.
   *
   * @return the district of the client's user
   */
  public District getDistrict() {
    return district;
  }

  /**
   * Returns the time of the client's last contact.
   *
   * @return the time of the client's last contact
   */
  public LocalDateTime getLastSyncAt() {
    return lastSyncAt;
  }

  /**
   * Returns the number of queued items the client held at that contact.
   *
   * @return the number of queued items the client held at that contact
   */
  public int getPendingCount() {
    return pendingCount;
  }

  /**
   * Records a contact of the client with the server.
   *
   * @param at the time of the contact
   * @param pendingCount the number of queued items the client held, not counting rejected ones
   * @throws NullPointerException if {@code at} is {@code null}
   * @throws IllegalArgumentException if {@code pendingCount} is negative
   */
  public void recordSync(LocalDateTime at, int pendingCount) {
    Objects.requireNonNull(at, "at");
    this.pendingCount = requireNotNegative(pendingCount);
    this.lastSyncAt = at;
  }

  private static int requireNotNegative(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("pendingCount must not be negative: " + count);
    }
    return count;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof ClientSyncStatus that
            && Objects.equals(getClientId(), that.getClientId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClientId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getClientId() + "]";
  }
}
