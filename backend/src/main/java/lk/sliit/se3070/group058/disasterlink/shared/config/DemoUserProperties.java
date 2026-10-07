package lk.sliit.se3070.group058.disasterlink.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The fixed demo user of each role (implementation standards section 1: login is not built, so each
 * role has one configured user). The seed data creates these users; the page controllers read the
 * identifiers through {@code CurrentUserService}. Every identifier is required: a missing one stops
 * the application at start-up with a message that names the property (P155 B-12).
 *
 * @param dmcOfficerId the DMC officer (UC1, UC4)
 * @param dmcDutyOfficerId the DMC duty officer (UC2 verification)
 * @param districtOfficerId the district officer (UC3)
 * @param organizationId the resource-owning organisation (UC4's shared reports)
 * @param citizenId the citizen of the app (UC2)
 */
@Validated
@ConfigurationProperties("demo.users")
public record DemoUserProperties(
    @NotBlank(message = "demo.users.dmc-officer-id must name the demo DMC officer")
        String dmcOfficerId,
    @NotBlank(message = "demo.users.dmc-duty-officer-id must name the demo DMC duty officer")
        String dmcDutyOfficerId,
    @NotBlank(message = "demo.users.district-officer-id must name the demo district officer")
        String districtOfficerId,
    @NotBlank(message = "demo.users.organization-id must name the demo organisation")
        String organizationId,
    @NotBlank(message = "demo.users.citizen-id must name the demo citizen") String citizenId) {}
