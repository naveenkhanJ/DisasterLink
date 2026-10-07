package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import lk.sliit.se3070.group058.disasterlink.shared.config.DemoUserProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests the binding of {@link DemoUserProperties}: from {@code application.properties} and by key.
 */
@SpringBootTest
class DemoUserPropertiesTest {

  @Autowired private DemoUserProperties properties;

  @Test
  @DisplayName("application.properties sets the five demo users")
  void applicationPropertiesSetTheDemoUsers() {
    assertThat(properties.dmcOfficerId()).isEqualTo("DMC-0001");
    assertThat(properties.dmcDutyOfficerId()).isEqualTo("DMC-0002");
    assertThat(properties.districtOfficerId()).isEqualTo("DO-COL-01");
    assertThat(properties.organizationId()).isEqualTo("ORG-0001");
    assertThat(properties.citizenId()).isEqualTo("CIT-KLT-0001");
  }

  /** Enables the record for the key-by-key binding test. */
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DemoUserProperties.class)
  static class PropertiesConfiguration {}

  @Test
  @DisplayName("Each key binds to its own component")
  void eachKeyBindsToItsComponent() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "demo.users.dmc-officer-id=a",
            "demo.users.dmc-duty-officer-id=b",
            "demo.users.district-officer-id=c",
            "demo.users.organization-id=d",
            "demo.users.citizen-id=e")
        .run(
            context -> {
              DemoUserProperties bound = context.getBean(DemoUserProperties.class);
              assertThat(bound.dmcOfficerId()).isEqualTo("a");
              assertThat(bound.dmcDutyOfficerId()).isEqualTo("b");
              assertThat(bound.districtOfficerId()).isEqualTo("c");
              assertThat(bound.organizationId()).isEqualTo("d");
              assertThat(bound.citizenId()).isEqualTo("e");
            });
  }

  @Test
  @DisplayName("A missing demo user id stops the start-up with a message that names the property")
  void missingIdFailsAtStartUp() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "demo.users.dmc-officer-id=a",
            "demo.users.dmc-duty-officer-id=b",
            "demo.users.district-officer-id=c",
            "demo.users.organization-id=d")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("demo.users.citizen-id must name the demo citizen"));
  }

  @Test
  @DisplayName("A blank demo user id also stops the start-up")
  void blankIdFailsAtStartUp() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "demo.users.dmc-officer-id= ",
            "demo.users.dmc-duty-officer-id=b",
            "demo.users.district-officer-id=c",
            "demo.users.organization-id=d",
            "demo.users.citizen-id=e")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("demo.users.dmc-officer-id must name"));
  }
}
