package lk.sliit.se3070.group058.disasterlink.shelter.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ShelterStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;

/**
 * An emergency shelter with its capacity, its current occupancy and the history of its occupancy
 * records.
 *
 * <p>Implements CH-14 and CH-28 (A3, A9, A10, A11). The rules of the shelter (canAccommodate,
 * updateOccupancy and the others) are written by its owner.
 */
@Entity
public class Shelter {

  @Id private String shelterId;

  private String name;

  private String address;

  @Enumerated(EnumType.STRING)
  private District district;

  private int capacity;

  private int currentOccupancy;

  private String contactNumber;

  @Enumerated(EnumType.STRING)
  private ShelterStatus status;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "shelter_id")
  @OrderColumn(name = "record_order")
  private List<OccupancyRecord> occupancyRecords;

  @ManyToMany
  @JoinTable(
      name = "shelter_warning",
      joinColumns = @JoinColumn(name = "shelter_id"),
      inverseJoinColumns = @JoinColumn(name = "warning_id"))
  @OrderColumn(name = "warning_order")
  private List<Warning> warnings;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "officer_id")
  private DistrictOfficer officer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "operator_id")
  private Organization operator;

  /**
   * Registers a shelter from the details an officer entered: the drawn message of sequence diagram
   * SD3 (2.2.1.1). The shelter starts open and empty, with no records and no warnings.
   *
   * @param shelterId the identifier, a UUID made by the control for a shelter registered at run
   *     time
   * @param details the name, address, district, capacity and contact number; the shelter is created
   *     only after {@code validateShelterDetails}, so the components that the record accepts as
   *     {@code null} are required here (P155 X1)
   * @param officer the district officer who registers the shelter
   * @param operator the operating organisation, or {@code null} if there is none
   * @throws NullPointerException if an argument other than {@code operator}, or a required
   *     component of {@code details}, is {@code null}
   */
  public Shelter(
      String shelterId, ShelterDetails details, DistrictOfficer officer, Organization operator) {
    this.shelterId = Objects.requireNonNull(shelterId, "shelterId");
    Objects.requireNonNull(details, "details");
    this.officer = Objects.requireNonNull(officer, "officer");
    this.operator = operator;
    this.name = Objects.requireNonNull(details.name(), "name");
    this.address = Objects.requireNonNull(details.address(), "address");
    this.district = Objects.requireNonNull(details.district(), "district");
    this.capacity = details.capacity();
    this.contactNumber = Objects.requireNonNull(details.contactNumber(), "contactNumber");
    this.currentOccupancy = 0;
    this.status = ShelterStatus.OPEN;
    this.occupancyRecords = new ArrayList<>();
    this.warnings = new ArrayList<>();
  }

  /**
   * Creates a shelter from its full stored state. This is the seed's constructor, for shelters with
   * an occupancy history and allocated warnings; it changes no domain rule.
   *
   * @param operator the operating organisation, or {@code null}
   * @param occupancyRecords the occupancy history
   * @param warnings the warnings the shelter is allocated to
   * @throws NullPointerException if an argument other than {@code operator} is {@code null}
   */
  public Shelter(
      String shelterId,
      String name,
      String address,
      District district,
      int capacity,
      int currentOccupancy,
      String contactNumber,
      ShelterStatus status,
      DistrictOfficer officer,
      Organization operator,
      List<OccupancyRecord> occupancyRecords,
      List<Warning> warnings) {
    this.shelterId = Objects.requireNonNull(shelterId, "shelterId");
    this.name = Objects.requireNonNull(name, "name");
    this.address = Objects.requireNonNull(address, "address");
    this.district = Objects.requireNonNull(district, "district");
    this.capacity = capacity;
    this.currentOccupancy = currentOccupancy;
    this.contactNumber = Objects.requireNonNull(contactNumber, "contactNumber");
    this.status = Objects.requireNonNull(status, "status");
    this.officer = Objects.requireNonNull(officer, "officer");
    this.operator = operator;
    this.occupancyRecords = new ArrayList<>(occupancyRecords);
    this.warnings = new ArrayList<>(warnings);
  }

  /** Constructor for JPA only. */
  protected Shelter() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the shelter.
   *
   * @return the identifier of the shelter
   */
  public String getShelterId() {
    return shelterId;
  }

  /**
   * Returns the name of the shelter.
   *
   * @return the name of the shelter
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the address of the shelter.
   *
   * @return the address of the shelter
   */
  public String getAddress() {
    return address;
  }

  /**
   * Returns the district of the shelter.
   *
   * @return the district of the shelter
   */
  public District getDistrict() {
    return district;
  }

  /**
   * Returns the number of people the shelter holds.
   *
   * @return the number of people the shelter holds
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Returns the number of people in the shelter now.
   *
   * @return the number of people in the shelter now
   */
  public int getCurrentOccupancy() {
    return currentOccupancy;
  }

  /**
   * Returns the contact telephone number of the shelter.
   *
   * @return the contact telephone number of the shelter
   */
  public String getContactNumber() {
    return contactNumber;
  }

  /**
   * Returns the status of the shelter.
   *
   * @return the status of the shelter
   */
  public ShelterStatus getStatus() {
    return status;
  }

  /**
   * Returns the occupancy history of the shelter (MD section 3 Shelter, PR8), in the order the
   * records were added. Written by the skeleton because UC4's {@code OccupancyTrendCalculator}
   * reads it and cannot wait for UC3 (P155 X5).
   *
   * @return an unmodifiable view of the occupancy records
   */
  public List<OccupancyRecord> getOccupancyRecords() {
    return Collections.unmodifiableList(occupancyRecords);
  }

  /**
   * Returns the warnings the shelter is allocated to (A9).
   *
   * @return an unmodifiable view of the warnings the shelter is allocated to (A9)
   */
  public List<Warning> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  /**
   * Returns the district officer who registered the shelter (A10).
   *
   * @return the district officer who registered the shelter (A10)
   */
  public DistrictOfficer getOfficer() {
    return officer;
  }

  /**
   * Returns the organisation that operates the shelter (A11).
   *
   * @return the organisation that operates the shelter (A11), or empty if there is none
   */
  public Optional<Organization> getOperator() {
    return Optional.ofNullable(operator);
  }

  /**
   * Checks whether the shelter can accommodate the specified occupancy total (CH-28).
   *
   * @param newTotal the candidate total occupancy
   * @return {@code true} if newTotal is at least 0 and does not exceed capacity
   */
  public boolean canAccommodate(int newTotal) {
    return newTotal >= 0 && newTotal <= capacity;
  }

  /**
   * Checks remaining capacity for evacuees (CH-28, P85).
   *
   * @return the number of available places (capacity minus current occupancy, minimum 0)
   */
  public int checkAvailability() {
    return Math.max(0, capacity - currentOccupancy);
  }

  /**
   * Updates occupancy record history and recomputes current occupancy and status (CH-14, CH-28,
   * PR8).
   *
   * <p>Keeps every record in history, but updates {@code currentOccupancy} and {@code status} only
   * when the record's {@code capturedAt} timestamp is not earlier than the latest recorded capture
   * time (PR8 stale-replay rule).
   *
   * @param record the occupancy record to add
   * @throws NullPointerException if record is {@code null}
   */
  public void updateOccupancy(OccupancyRecord record) {
    Objects.requireNonNull(record, "record");
    LocalDateTime latestCapturedAt = null;
    for (OccupancyRecord existing : occupancyRecords) {
      if (latestCapturedAt == null || existing.getCapturedAt().isAfter(latestCapturedAt)) {
        latestCapturedAt = existing.getCapturedAt();
      }
    }
    occupancyRecords.add(record);
    if (latestCapturedAt == null || !record.getCapturedAt().isBefore(latestCapturedAt)) {
      this.currentOccupancy = record.getOccupantCount();
      if (this.currentOccupancy >= this.capacity) {
        this.status = ShelterStatus.FULL;
      } else {
        this.status = ShelterStatus.OPEN;
      }
    }
  }

  /**
   * Updates the details of the shelter (CH-28).
   *
   * @param details the updated details
   * @throws NullPointerException if details is {@code null}
   */
  public void updateDetails(ShelterDetails details) {
    Objects.requireNonNull(details, "details");
    if (details.name() != null) {
      this.name = details.name();
    }
    if (details.address() != null) {
      this.address = details.address();
    }
    if (details.district() != null) {
      this.district = details.district();
    }
    if (details.contactNumber() != null) {
      this.contactNumber = details.contactNumber();
    }
    this.capacity = details.capacity();
    if (this.currentOccupancy >= this.capacity) {
      this.status = ShelterStatus.FULL;
    } else {
      this.status = ShelterStatus.OPEN;
    }
  }

  /**
   * Sets the operating organisation of the shelter.
   *
   * @param operator the operating organisation, or {@code null} if there is none
   */
  public void setOperator(Organization operator) {
    this.operator = operator;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof Shelter that && Objects.equals(getShelterId(), that.getShelterId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getShelterId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getShelterId() + "]";
  }
}
