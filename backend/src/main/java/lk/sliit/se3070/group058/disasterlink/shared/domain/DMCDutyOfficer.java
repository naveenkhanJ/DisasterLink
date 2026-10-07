package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;

/**
 * A DMC officer on duty, the only kind that reviews hazard reports. It has no members of its own.
 *
 * <p>Implements CH-11 (restored by FQ1).
 */
@Entity
@Table(name = "dmc_duty_officer")
public class DMCDutyOfficer extends DMCOfficer {

  /**
   * Creates a DMC duty officer.
   *
   * @param reportingDistricts the districts the officer reports on; empty means national
   * @throws NullPointerException if an argument is {@code null}
   */
  public DMCDutyOfficer(
      String userId,
      String name,
      String phoneNumber,
      String email,
      String position,
      Set<District> reportingDistricts) {
    super(userId, name, phoneNumber, email, position, reportingDistricts);
  }

  /** Constructor for JPA only. */
  protected DMCDutyOfficer() {
    // Constructor for JPA only.
  }
}
