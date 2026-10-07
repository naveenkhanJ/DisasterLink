package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Optional;

/**
 * A citizen who volunteers in a community group; as a recipient the volunteer is a citizen.
 *
 * <p>Implements CH-04 and CH-11 (A24 for the organisation).
 */
@Entity
@Table(name = "community_volunteer")
public class CommunityVolunteer extends Citizen {

  private String gnDivision;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  /**
   * Creates a community volunteer.
   *
   * @param organization the community group the volunteer belongs to, or {@code null} if none
   * @throws NullPointerException if an argument other than {@code deviceToken} and {@code
   *     organization} is {@code null}
   * @throws IllegalArgumentException if {@code organization} is not of type {@code
   *     COMMUNITY_VOLUNTEER_GROUP} (the A24 constraint, checked like the xor of {@code
   *     ReliefSupply}; P155 A-11)
   */
  public CommunityVolunteer(
      String userId,
      String name,
      String phoneNumber,
      String email,
      String nic,
      String address,
      District district,
      Language preferredLanguage,
      String deviceToken,
      String gnDivision,
      Organization organization) {
    super(userId, name, phoneNumber, email, nic, address, district, preferredLanguage, deviceToken);
    if (organization != null
        && organization.getType() != OrganizationType.COMMUNITY_VOLUNTEER_GROUP) {
      throw new IllegalArgumentException(
          "a volunteer belongs to an organisation of type COMMUNITY_VOLUNTEER_GROUP");
    }
    this.gnDivision = Objects.requireNonNull(gnDivision, "gnDivision");
    this.organization = organization;
  }

  /** Constructor for JPA only. */
  protected CommunityVolunteer() {
    // Constructor for JPA only.
  }

  /**
   * Returns the Grama Niladhari division of the volunteer.
   *
   * @return the Grama Niladhari division of the volunteer
   */
  public String getGnDivision() {
    return gnDivision;
  }

  /**
   * Returns the organisation the volunteer is a member of (A24).
   *
   * @return the organisation the volunteer is a member of (A24), or empty if there is none
   */
  public Optional<Organization> getOrganization() {
    return Optional.ofNullable(organization);
  }
}
