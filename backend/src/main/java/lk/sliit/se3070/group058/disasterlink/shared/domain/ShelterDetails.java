package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Objects;

/**
 * The details of a shelter, for registering or changing it.
 *
 * <p>Implements CH-28.
 *
 * @param name the name of the shelter
 * @param address the address of the shelter
 * @param district the district of the shelter
 * @param capacity the number of people the shelter holds
 * @param contactNumber the contact telephone number
 * @param operatorId the identifier of the operating organisation, or {@code null} if there is none
 */
public record ShelterDetails(
    String name,
    String address,
    District district,
    int capacity,
    String contactNumber,
    String operatorId) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public ShelterDetails(
      String name,
      String address,
      District district,
      int capacity,
      String contactNumber,
      String operatorId) {
    this.name = Objects.requireNonNull(name, "name");
    this.address = Objects.requireNonNull(address, "address");
    this.district = Objects.requireNonNull(district, "district");
    this.capacity = capacity;
    this.contactNumber = Objects.requireNonNull(contactNumber, "contactNumber");
    this.operatorId = operatorId;
  }
}
