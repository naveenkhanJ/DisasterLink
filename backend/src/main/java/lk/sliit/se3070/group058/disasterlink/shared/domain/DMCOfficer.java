package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * An officer of the Disaster Management Centre. An empty set of reporting districts means the
 * officer reports nationally.
 *
 * <p>Implements CH-11 and CH-31 (the reporting districts serve UC4's scope check).
 */
@Entity
@DiscriminatorValue("DMC_OFFICER")
public class DMCOfficer extends User {

  private String position;

  @ElementCollection
  @CollectionTable(
      name = "dmc_officer_reporting_district",
      joinColumns = @JoinColumn(name = "officer_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "district")
  private Set<District> reportingDistricts;

  /**
   * Creates a DMC officer.
   *
   * @param reportingDistricts the districts the officer reports on; empty means national
   * @throws NullPointerException if an argument is {@code null}
   */
  public DMCOfficer(
      String userId,
      String name,
      String phoneNumber,
      String email,
      String position,
      Set<District> reportingDistricts) {
    super(userId, name, phoneNumber, email);
    this.position = Objects.requireNonNull(position, "position");
    this.reportingDistricts = EnumSet.noneOf(District.class);
    this.reportingDistricts.addAll(reportingDistricts);
  }

  /** Constructor for JPA only. */
  protected DMCOfficer() {
    // Constructor for JPA only.
  }

  /**
   * Returns the position of the officer.
   *
   * @return the position of the officer
   */
  public String getPosition() {
    return position;
  }

  /**
   * Returns the districts the officer reports on; empty means national.
   *
   * @return an unmodifiable view of the districts the officer reports on; empty means national
   */
  public Set<District> getReportingDistricts() {
    return Collections.unmodifiableSet(reportingDistricts);
  }
}
