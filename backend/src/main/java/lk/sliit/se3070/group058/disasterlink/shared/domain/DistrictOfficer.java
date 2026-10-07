package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
import java.util.Optional;

/**
 * An officer of one district. A district officer receives warning escalations by SMS only.
 *
 * <p>Implements CH-11 and CH-06. Realises Recipient (G7).
 */
@Entity
@DiscriminatorValue("DISTRICT_OFFICER")
public class DistrictOfficer extends User implements Recipient {

  @Enumerated(EnumType.STRING)
  private District district;

  @Enumerated(EnumType.STRING)
  private Language preferredLanguage;

  /**
   * Creates a district officer.
   *
   * @throws NullPointerException if an argument is {@code null}
   */
  public DistrictOfficer(
      String userId,
      String name,
      String phoneNumber,
      String email,
      District district,
      Language preferredLanguage) {
    super(userId, name, phoneNumber, email);
    this.district = Objects.requireNonNull(district, "district");
    this.preferredLanguage = Objects.requireNonNull(preferredLanguage, "preferredLanguage");
  }

  /** Constructor for JPA only. */
  protected DistrictOfficer() {
    // Constructor for JPA only.
  }

  /**
   * Returns the district the officer serves.
   *
   * @return the district the officer serves
   */
  public District getDistrict() {
    return district;
  }

  /**
   * Returns the language the officer wants notices in.
   *
   * @return the language the officer wants notices in
   */
  @Override
  public Language getPreferredLanguage() {
    return preferredLanguage;
  }

  @Override
  public String getRecipientId() {
    return getUserId();
  }

  @Override
  public RecipientKind getRecipientKind() {
    return RecipientKind.DISTRICT_OFFICER;
  }

  @Override
  public String getContactNumber() {
    return getPhoneNumber();
  }

  @Override
  public Optional<String> getDeviceToken() {
    return Optional.empty();
  }

  @Override
  public boolean canReceive(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }
}
