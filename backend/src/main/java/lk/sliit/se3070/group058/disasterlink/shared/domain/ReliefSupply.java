package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import java.util.Optional;

/**
 * A relief supply, donated by an organisation or by a citizen but never by both.
 *
 * <p>Implements CH-13, CH-19 and CH-20 (A17a, A17b). Seed data only; all its operations are
 * design-only.
 */
@Entity
public class ReliefSupply {

  @Id private String reliefSupplyId;

  private String name;

  @Enumerated(EnumType.STRING)
  private SupplyCategory category;

  private double quantity;

  private String unit;

  private String location;

  @Enumerated(EnumType.STRING)
  private SupplyStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "donor_organization_id")
  private Organization donorOrganization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "donor_citizen_id")
  private Citizen donorCitizen;

  /**
   * Creates a relief supply.
   *
   * @param donorOrganization the donating organisation, or {@code null}
   * @param donorCitizen the donating citizen, or {@code null}
   * @throws IllegalArgumentException if both donors are set (the {xor} constraint of A17a and A17b)
   * @throws NullPointerException if an argument other than the two donors is {@code null}
   */
  public ReliefSupply(
      String reliefSupplyId,
      String name,
      SupplyCategory category,
      double quantity,
      String unit,
      String location,
      SupplyStatus status,
      Organization donorOrganization,
      Citizen donorCitizen) {
    if (donorOrganization != null && donorCitizen != null) {
      throw new IllegalArgumentException("a supply has one donor: an organisation or a citizen");
    }
    this.reliefSupplyId = Objects.requireNonNull(reliefSupplyId, "reliefSupplyId");
    this.name = Objects.requireNonNull(name, "name");
    this.category = Objects.requireNonNull(category, "category");
    this.quantity = quantity;
    this.unit = Objects.requireNonNull(unit, "unit");
    this.location = Objects.requireNonNull(location, "location");
    this.status = Objects.requireNonNull(status, "status");
    this.donorOrganization = donorOrganization;
    this.donorCitizen = donorCitizen;
  }

  /** Constructor for JPA only. */
  protected ReliefSupply() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the supply.
   *
   * @return the identifier of the supply
   */
  public String getReliefSupplyId() {
    return reliefSupplyId;
  }

  /**
   * Returns the name of the supply.
   *
   * @return the name of the supply
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the category of the supply.
   *
   * @return the category of the supply
   */
  public SupplyCategory getCategory() {
    return category;
  }

  /**
   * Returns the quantity of the supply.
   *
   * @return the quantity of the supply
   */
  public double getQuantity() {
    return quantity;
  }

  /**
   * Returns the unit of the quantity.
   *
   * @return the unit of the quantity
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Returns the place the supply is stored at.
   *
   * @return the place the supply is stored at
   */
  public String getLocation() {
    return location;
  }

  /**
   * Returns the availability of the supply.
   *
   * @return the availability of the supply
   */
  public SupplyStatus getStatus() {
    return status;
  }

  /**
   * Returns the organisation that donated the supply (A17a).
   *
   * @return the organisation that donated the supply (A17a), or empty if there is none
   */
  public Optional<Organization> getDonorOrganization() {
    return Optional.ofNullable(donorOrganization);
  }

  /**
   * Returns the citizen who donated the supply (A17b).
   *
   * @return the citizen who donated the supply (A17b), or empty if there is none
   */
  public Optional<Citizen> getDonorCitizen() {
    return Optional.ofNullable(donorCitizen);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof ReliefSupply that
            && Objects.equals(getReliefSupplyId(), that.getReliefSupplyId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getReliefSupplyId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getReliefSupplyId() + "]";
  }
}
