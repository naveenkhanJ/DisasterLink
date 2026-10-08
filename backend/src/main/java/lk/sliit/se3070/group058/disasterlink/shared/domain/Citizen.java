package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Optional;

/**
 * A citizen who can receive warnings and submit hazard reports. A citizen receives SMS always, and
 * push or audible alerts only with a device token.
 *
 * <p>Implements CH-11, CH-21 and CH-25. Realises Recipient (G7).
 */
@Entity
@Table(name = "citizen")
public class Citizen extends User implements Recipient {

  private String nic;

  private String address;

  @Enumerated(EnumType.STRING)
  private District district;

  @Enumerated(EnumType.STRING)
  private Language preferredLanguage;

  private String deviceToken;

  /**
   * Creates a citizen.
   *
   * @param deviceToken the push token of the citizen's device, or {@code null} if the citizen has
   *     no app
   * @throws NullPointerException if an argument other than {@code deviceToken} is {@code null}
   */
  public Citizen(
      String userId,
      String name,
      String phoneNumber,
      String email,
      String nic,
      String address,
      District district,
      Language preferredLanguage,
      String deviceToken) {
    super(userId, name, phoneNumber, email);
    this.nic = Objects.requireNonNull(nic, "nic");
    this.address = Objects.requireNonNull(address, "address");
    this.district = Objects.requireNonNull(district, "district");
    this.preferredLanguage = Objects.requireNonNull(preferredLanguage, "preferredLanguage");
    this.deviceToken = deviceToken;
  }

  /** Constructor for JPA only. */
  protected Citizen() {
    // Constructor for JPA only.
  }

  /**
   * Returns the national identity card number of the citizen.
   *
   * @return the national identity card number of the citizen
   */
  public String getNic() {
    return nic;
  }

  /**
   * Returns the address of the citizen.
   *
   * @return the address of the citizen
   */
  public String getAddress() {
    return address;
  }

  /**
   * Returns the district the citizen is registered in.
   *
   * @return the district the citizen is registered in
   */
  public District getDistrict() {
    return district;
  }

  /**
   * Returns the language the citizen wants notices in.
   *
   * @return the language the citizen wants notices in
   */
  @Override
  public Language getPreferredLanguage() {
    return preferredLanguage;
  }

  /**
   * Returns the push token of the citizen's device.
   *
   * @return the push token of the citizen's device, or empty if there is none
   */
  @Override
  public Optional<String> getDeviceToken() {
    return Optional.ofNullable(deviceToken);
  }

  /**
   * Changes the citizen's preferred language (sequence diagram SD2, message 2.1.1.1). The language
   * decides the language of the alerts and notices the citizen receives.
   *
   * <p>Implements CH-21 and CH-25. The body is Afham's (implementation standards section 3,
   * "Operations on shared entities").
   *
   * @param language the new preferred language
   * @throws NullPointerException if {@code language} is {@code null}
   */
  public void changePreferredLanguage(Language language) {
    this.preferredLanguage = Objects.requireNonNull(language, "language");
  }

  @Override
  public String getRecipientId() {
    return getUserId();
  }

  @Override
  public RecipientKind getRecipientKind() {
    return RecipientKind.CITIZEN;
  }

  @Override
  public String getContactNumber() {
    return getPhoneNumber();
  }

  @Override
  public boolean canReceive(NotificationChannel channel) {
    return switch (channel) {
      case SMS -> true;
      case PUSH, AUDIBLE_ALERT -> deviceToken != null;
    };
  }
}
