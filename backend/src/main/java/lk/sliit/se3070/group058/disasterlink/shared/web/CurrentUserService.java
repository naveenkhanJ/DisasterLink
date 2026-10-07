package lk.sliit.se3070.group058.disasterlink.shared.web;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import lk.sliit.se3070.group058.disasterlink.shared.config.DemoUserProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCDutyOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DMCOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives the page controllers the fixed demo user of a role: its identifier, and the name, initials
 * and role label the layout's top bar shows. Members call {@link #currentUser} in their page
 * controllers and add the result to the model as {@code currentUser}.
 *
 * <p>Implementation-only class (implementation standards section 1: no login, a fixed demo user per
 * role).
 */
@Service
public class CurrentUserService {

  private static final String DMC_OFFICER_LABEL = "DMC Officer";

  private static final String DMC_DUTY_OFFICER_LABEL = "DMC Duty Officer";

  private static final String ORGANIZATION_LABEL = "Resource-Owning Organisation";

  private final DemoUserProperties users;

  private final DMCOfficerRepository dmcOfficers;

  private final DMCDutyOfficerRepository dutyOfficers;

  private final DistrictOfficerRepository districtOfficers;

  private final OrganizationRepository organizations;

  /**
   * Creates the service.
   *
   * @param users the configured demo user identifiers
   * @param dmcOfficers the DMC officers
   * @param dutyOfficers the DMC duty officers
   * @param districtOfficers the district officers
   * @param organizations the organisations
   */
  public CurrentUserService(
      DemoUserProperties users,
      DMCOfficerRepository dmcOfficers,
      DMCDutyOfficerRepository dutyOfficers,
      DistrictOfficerRepository districtOfficers,
      OrganizationRepository organizations) {
    this.users = Objects.requireNonNull(users, "users");
    this.dmcOfficers = Objects.requireNonNull(dmcOfficers, "dmcOfficers");
    this.dutyOfficers = Objects.requireNonNull(dutyOfficers, "dutyOfficers");
    this.districtOfficers = Objects.requireNonNull(districtOfficers, "districtOfficers");
    this.organizations = Objects.requireNonNull(organizations, "organizations");
  }

  /**
   * Returns the identifier of the demo user of a role.
   *
   * @param role the role
   * @return the configured identifier
   */
  public String userId(DemoRole role) {
    return switch (role) {
      case DMC_OFFICER -> users.dmcOfficerId();
      case DMC_DUTY_OFFICER -> users.dmcDutyOfficerId();
      case DISTRICT_OFFICER -> users.districtOfficerId();
      case ORGANIZATION -> users.organizationId();
    };
  }

  /**
   * Loads the demo user of a role for the top bar.
   *
   * @param role the role
   * @return the initials, name and role label
   * @throws java.util.NoSuchElementException if the configured user does not exist
   */
  @Transactional(readOnly = true)
  public CurrentUserView currentUser(DemoRole role) {
    String id = userId(role);
    return switch (role) {
      case DMC_OFFICER -> view(dmcOfficers.findById(id).orElseThrow().getName(), DMC_OFFICER_LABEL);
      case DMC_DUTY_OFFICER ->
          view(dutyOfficers.findById(id).orElseThrow().getName(), DMC_DUTY_OFFICER_LABEL);
      case DISTRICT_OFFICER -> {
        var officer = districtOfficers.findById(id).orElseThrow();
        yield view(officer.getName(), titleCase(officer.getDistrict()) + " District");
      }
      case ORGANIZATION ->
          view(organizations.findById(id).orElseThrow().getName(), ORGANIZATION_LABEL);
    };
  }

  private static CurrentUserView view(String name, String roleLabel) {
    return new CurrentUserView(initials(name), name, roleLabel);
  }

  /** The first letters of the first and the last word of a name, in upper case. */
  static String initials(String name) {
    String[] words = name.trim().split("\\s+");
    String first = words[0].substring(0, 1);
    String last = words[words.length - 1].substring(0, 1);
    return (words.length == 1 ? first : first + last).toUpperCase(Locale.ROOT);
  }

  private static String titleCase(District district) {
    return Arrays.stream(district.name().split("_"))
        .map(word -> word.charAt(0) + word.substring(1).toLowerCase(Locale.ROOT))
        .reduce((left, right) -> left + " " + right)
        .orElseThrow();
  }
}
