package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An organisation that is warned and that can receive reports. An organisation receives SMS only.
 *
 * <p>Implements CH-15 and CH-18. Realises Recipient (G7).
 */
@Entity
public class Organization implements Recipient {

  @Id private String organizationId;

  private String name;

  @Enumerated(EnumType.STRING)
  private OrganizationType type;

  private String contactNumber;

  private String address;

  @ElementCollection
  @CollectionTable(
      name = "organization_district",
      joinColumns = @JoinColumn(name = "organization_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "district")
  @OrderColumn(name = "district_order")
  private List<District> districts;

  @Enumerated(EnumType.STRING)
  private Language preferredLanguage;

  /**
   * Creates an organisation.
   *
   * @param districts the districts the organisation serves
   * @throws NullPointerException if an argument is {@code null}
   */
  public Organization(
      String organizationId,
      String name,
      OrganizationType type,
      String contactNumber,
      String address,
      List<District> districts,
      Language preferredLanguage) {
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.name = Objects.requireNonNull(name, "name");
    this.type = Objects.requireNonNull(type, "type");
    this.contactNumber = Objects.requireNonNull(contactNumber, "contactNumber");
    this.address = Objects.requireNonNull(address, "address");
    this.districts = new ArrayList<>(districts);
    this.preferredLanguage = Objects.requireNonNull(preferredLanguage, "preferredLanguage");
  }

  /** Constructor for JPA only. */
  protected Organization() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the organisation.
   *
   * @return the identifier of the organisation
   */
  public String getOrganizationId() {
    return organizationId;
  }

  /**
   * Returns the name of the organisation.
   *
   * @return the name of the organisation
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the kind of the organisation.
   *
   * @return the kind of the organisation
   */
  public OrganizationType getType() {
    return type;
  }

  /**
   * Returns the contact telephone number of the organisation.
   *
   * @return the contact telephone number of the organisation
   */
  @Override
  public String getContactNumber() {
    return contactNumber;
  }

  /**
   * Returns the address of the organisation.
   *
   * @return the address of the organisation
   */
  public String getAddress() {
    return address;
  }

  /**
   * Returns the districts the organisation serves.
   *
   * @return an unmodifiable view of the districts the organisation serves
   */
  public List<District> getDistricts() {
    return Collections.unmodifiableList(districts);
  }

  /**
   * Returns the language the organisation wants notices in.
   *
   * @return the language the organisation wants notices in
   */
  @Override
  public Language getPreferredLanguage() {
    return preferredLanguage;
  }

  @Override
  public String getRecipientId() {
    return organizationId;
  }

  @Override
  public RecipientKind getRecipientKind() {
    return RecipientKind.ORGANIZATION;
  }

  @Override
  public Optional<String> getDeviceToken() {
    return Optional.empty();
  }

  @Override
  public boolean canReceive(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof Organization that
            && Objects.equals(getOrganizationId(), that.getOrganizationId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getOrganizationId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getOrganizationId() + "]";
  }
}
