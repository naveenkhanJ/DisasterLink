package lk.sliit.se3070.group058.disasterlink.shared.web;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lk.sliit.se3070.group058.disasterlink.shared.config.DemoUserProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistrictOfficer;
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

  /** The configured identifier of each role's demo user. */
  private final Map<DemoRole, String> ids = new EnumMap<>(DemoRole.class);

  /** How each role's demo user is loaded and shown in the top bar (identifier to view). */
  private final Map<DemoRole, Function<String, CurrentUserView>> views =
      new EnumMap<>(DemoRole.class);

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
    Objects.requireNonNull(users, "users");
    Objects.requireNonNull(dmcOfficers, "dmcOfficers");
    Objects.requireNonNull(dutyOfficers, "dutyOfficers");
    Objects.requireNonNull(districtOfficers, "districtOfficers");
    Objects.requireNonNull(organizations, "organizations");
    ids.put(DemoRole.DMC_OFFICER, users.dmcOfficerId());
    ids.put(DemoRole.DMC_DUTY_OFFICER, users.dmcDutyOfficerId());
    ids.put(DemoRole.DISTRICT_OFFICER, users.districtOfficerId());
    ids.put(DemoRole.ORGANIZATION, users.organizationId());
    views.put(
        DemoRole.DMC_OFFICER,
        id -> view(dmcOfficers.findById(id).orElseThrow().getName(), DMC_OFFICER_LABEL));
    views.put(
        DemoRole.DMC_DUTY_OFFICER,
        id -> view(dutyOfficers.findById(id).orElseThrow().getName(), DMC_DUTY_OFFICER_LABEL));
    views.put(
        DemoRole.DISTRICT_OFFICER,
        id -> {
          DistrictOfficer officer = districtOfficers.findById(id).orElseThrow();
          return view(officer.getName(), titleCase(officer.getDistrict()) + " District");
        });
    views.put(
        DemoRole.ORGANIZATION,
        id -> view(organizations.findById(id).orElseThrow().getName(), ORGANIZATION_LABEL));
  }

  /**
   * Returns the identifier of the demo user of a role.
   *
   * @param role the role
   * @return the configured identifier
   */
  public String userId(DemoRole role) {
    return ids.get(role);
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
    return views.get(role).apply(ids.get(role));
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
