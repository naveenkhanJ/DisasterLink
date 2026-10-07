package lk.sliit.se3070.group058.disasterlink.shared.domain;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * A person who uses the system. The class is abstract: citizens, volunteers and officers are its
 * subclasses, all stored in one table.
 *
 * <p>Implements CH-11; the login data and operations were removed by CH-09 and CH-19. The operation
 * updateProfile is design-only (D) and is not coded.
 */
@Entity
@Table(name = "app_user")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {

  @Id private String userId;

  private String name;

  private String phoneNumber;

  private String email;

  /** Creates the state of a user; the constructors of the subclasses call it. */
  protected User(String userId, String name, String phoneNumber, String email) {
    this.userId = Objects.requireNonNull(userId, "userId");
    this.name = Objects.requireNonNull(name, "name");
    this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
    this.email = Objects.requireNonNull(email, "email");
  }

  /** Constructor for JPA only. */
  protected User() {
    // Constructor for JPA only.
  }

  /**
   * Returns the identifier of the user.
   *
   * @return the identifier of the user
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns the name of the user.
   *
   * @return the name of the user
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the telephone number of the user.
   *
   * @return the telephone number of the user
   */
  public String getPhoneNumber() {
    return phoneNumber;
  }

  /**
   * Returns the e-mail address of the user.
   *
   * @return the e-mail address of the user
   */
  public String getEmail() {
    return email;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof User that && Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getUserId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getUserId() + "]";
  }
}
