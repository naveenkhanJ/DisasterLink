package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A distribution of a relief supply to a destination, coordinated by a district officer.
 *
 * <p>Implements CH-12, CH-13 and CH-32 (A18, A19, A20, A26). Seed data only.
 */
@Entity
public class ResourceDistribution {

  @Id private String distributionId;

  private double quantity;

  private LocalDateTime distributionDate;

  @Enumerated(EnumType.STRING)
  private District destinationDistrict;

  private String destination;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "relief_supply_id")
  private ReliefSupply reliefSupply;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiving_citizen_id")
  private Citizen receivingCitizen;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "coordinator_id")
  private DistrictOfficer coordinator;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hazard_id")
  private Hazard hazard;

  /**
   * Creates a distribution.
   *
   * @param receivingCitizen the citizen who received the supply, or {@code null}
   * @param hazard the hazard the distribution responds to, or {@code null}
   * @throws NullPointerException if an argument other than the citizen and the hazard is {@code
   *     null}
   */
  public ResourceDistribution(
      String distributionId,
      double quantity,
      LocalDateTime distributionDate,
      District destinationDistrict,
      String destination,
      ReliefSupply reliefSupply,
      Citizen receivingCitizen,
      DistrictOfficer coordinator,
      Hazard hazard) {
    this.distributionId = Objects.requireNonNull(distributionId, "distributionId");
    this.quantity = quantity;
    this.distributionDate = Objects.requireNonNull(distributionDate, "distributionDate");
    this.destinationDistrict = Objects.requireNonNull(destinationDistrict, "destinationDistrict");
    this.destination = Objects.requireNonNull(destination, "destination");
    this.reliefSupply = Objects.requireNonNull(reliefSupply, "reliefSupply");
    this.receivingCitizen = receivingCitizen;
    this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    this.hazard = hazard;
  }

  /** Constructor for JPA only. */
  protected ResourceDistribution() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the distribution.
   *
   * @return the identifier of the distribution
   */
  public String getDistributionId() {
    return distributionId;
  }

  /**
   * Returns the quantity distributed.
   *
   * @return the quantity distributed
   */
  public double getQuantity() {
    return quantity;
  }

  /**
   * Returns the time of the distribution.
   *
   * @return the time of the distribution
   */
  public LocalDateTime getDistributionDate() {
    return distributionDate;
  }

  /**
   * Returns the district the supply went to.
   *
   * @return the district the supply went to
   */
  public District getDestinationDistrict() {
    return destinationDistrict;
  }

  /**
   * Returns the place the supply went to.
   *
   * @return the place the supply went to
   */
  public String getDestination() {
    return destination;
  }

  /**
   * Returns the supply that was distributed (A18).
   *
   * @return the supply that was distributed (A18)
   */
  public ReliefSupply getReliefSupply() {
    return reliefSupply;
  }

  /**
   * Returns the citizen who received the supply (A19).
   *
   * @return the citizen who received the supply (A19), or empty if there is none
   */
  public Optional<Citizen> getReceivingCitizen() {
    return Optional.ofNullable(receivingCitizen);
  }

  /**
   * Returns the district officer who coordinated the distribution (A20).
   *
   * @return the district officer who coordinated the distribution (A20)
   */
  public DistrictOfficer getCoordinator() {
    return coordinator;
  }

  /**
   * Returns the hazard the distribution responds to (A26).
   *
   * @return the hazard the distribution responds to (A26), or empty if there is none
   */
  public Optional<Hazard> getHazard() {
    return Optional.ofNullable(hazard);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof ResourceDistribution that
            && Objects.equals(getDistributionId(), that.getDistributionId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getDistributionId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getDistributionId() + "]";
  }
}
