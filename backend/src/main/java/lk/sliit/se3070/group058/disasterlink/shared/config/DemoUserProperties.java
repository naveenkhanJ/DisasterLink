package lk.sliit.se3070.group058.disasterlink.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The fixed demo user of each role (implementation standards section 1: login is not built, so each
 * role has one configured user). The seed data (stage S4) creates these users; the page controllers
 * read the identifiers through {@code CurrentUserService}.
 *
 * @param dmcOfficerId the DMC officer (UC1, UC4)
 * @param dmcDutyOfficerId the DMC duty officer (UC2 verification)
 * @param districtOfficerId the district officer (UC3)
 * @param organizationId the resource-owning organisation (UC4's shared reports)
 * @param citizenId the citizen of the app (UC2)
 */
@ConfigurationProperties("demo.users")
public record DemoUserProperties(
    String dmcOfficerId,
    String dmcDutyOfficerId,
    String districtOfficerId,
    String organizationId,
    String citizenId) {}
