package lk.sliit.se3070.group058.disasterlink.shared.domain;

import java.util.Objects;

/**
 * One line of the relief distribution of a report.
 *
 * <p>Implements CH-33.
 *
 * @param district the district the supplies went to
 * @param ownerId the identifier of the owning organisation, or {@code null} for a private
 *     individual
 * @param ownerName the name of the owner
 * @param category the category of the supply
 * @param unit the unit of the quantity
 * @param quantity the quantity distributed
 */
public record DistributionLine(
    District district,
    String ownerId,
    String ownerName,
    SupplyCategory category,
    String unit,
    double quantity) {

  /**
   * Creates the record, checking the required components and copying the collections.
   *
   * @throws NullPointerException if a required component is {@code null}
   */
  public DistributionLine(
      District district,
      String ownerId,
      String ownerName,
      SupplyCategory category,
      String unit,
      double quantity) {
    this.district = Objects.requireNonNull(district, "district");
    this.ownerId = ownerId;
    this.ownerName = Objects.requireNonNull(ownerName, "ownerName");
    this.category = Objects.requireNonNull(category, "category");
    this.unit = Objects.requireNonNull(unit, "unit");
    this.quantity = quantity;
  }
}
