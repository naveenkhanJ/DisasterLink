package lk.sliit.se3070.group058.disasterlink.shared.domain;

/**
 * The details of a shelter, for registering or changing it.
 *
 * <p>Implements CH-28.
 *
 * <p>An input value record (MD section 0.13, P155 X1): a field the officer has not entered is
 * {@code null}, so that {@code ShelterController.validateShelterDetails} reports it (SC3 E2).
 *
 * @param name the name of the shelter, or {@code null} if not entered — checked by {@code
 *     ShelterController.validateShelterDetails}
 * @param address the address of the shelter, or {@code null} if not entered — checked by {@code
 *     ShelterController.validateShelterDetails}
 * @param district the district of the shelter, or {@code null} if not entered — checked by {@code
 *     ShelterController.validateShelterDetails}
 * @param capacity the number of people the shelter holds (a whole number; the page blocks a
 *     non-numeric entry itself, MD section 4)
 * @param contactNumber the contact telephone number, or {@code null} if not entered — checked by
 *     {@code ShelterController.validateShelterDetails}
 * @param operatorId the identifier of the operating organisation, or {@code null} if there is none
 */
public record ShelterDetails(
    String name,
    String address,
    District district,
    int capacity,
    String contactNumber,
    String operatorId) {

  /** Creates the record; every text and the district may be {@code null} (not entered). */
  public ShelterDetails(
      String name,
      String address,
      District district,
      int capacity,
      String contactNumber,
      String operatorId) {
    this.name = name;
    this.address = address;
    this.district = district;
    this.capacity = capacity;
    this.contactNumber = contactNumber;
    this.operatorId = operatorId;
  }
}
