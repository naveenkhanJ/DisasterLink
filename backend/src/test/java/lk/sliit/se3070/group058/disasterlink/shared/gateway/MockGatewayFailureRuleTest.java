package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.config.MockGatewayProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Tests of {@link MockGatewayFailureRule} and of the binding of {@link MockGatewayProperties}. */
class MockGatewayFailureRuleTest {

  private static MockGatewayFailureRule rule(List<String> sms, List<String> push) {
    return new MockGatewayFailureRule(
        new MockGatewayProperties(
            new MockGatewayProperties.Sms(sms), new MockGatewayProperties.Push(push)));
  }

  @Test
  @DisplayName("With no prefixes nothing fails")
  void nothingFailsByDefault() {
    MockGatewayFailureRule rule = new MockGatewayFailureRule(new MockGatewayProperties(null, null));

    assertThat(rule.smsFails("CIT-COL-0001")).isFalse();
    assertThat(rule.pushFails("CIT-COL-0001")).isFalse();
  }

  @Test
  @DisplayName("A recipient fails when its identifier starts with a listed prefix")
  void failsByPrefix() {
    MockGatewayFailureRule rule = rule(List.of("CIT-COL-", "ORG-"), List.of("CIT-KLT-"));

    assertThat(rule.smsFails("CIT-COL-0001")).isTrue();
    assertThat(rule.smsFails("ORG-0007")).isTrue();
    assertThat(rule.smsFails("CIT-KLT-0001")).isFalse();
    assertThat(rule.smsFails("DO-COL-01")).isFalse();
    assertThat(rule.pushFails("CIT-KLT-0001")).isTrue();
    assertThat(rule.pushFails("CIT-COL-0001")).isFalse();
  }

  @Test
  @DisplayName("The prefixes can be set and cleared while the application runs, per channel")
  void setAndClear() {
    MockGatewayFailureRule rule = rule(List.of(), List.of());

    rule.setSmsPrefixes(List.of("CIT-RAT-"));
    rule.setPushPrefixes(List.of("CIT-GAL-"));
    assertThat(rule.smsFails("CIT-RAT-0001")).isTrue();
    assertThat(rule.pushFails("CIT-GAL-0001")).isTrue();
    assertThat(rule.pushFails("CIT-RAT-0001")).isFalse();

    rule.clearSmsPrefixes();
    assertThat(rule.smsFails("CIT-RAT-0001")).isFalse();
    assertThat(rule.pushFails("CIT-GAL-0001")).isTrue();
    rule.clearPushPrefixes();
    assertThat(rule.pushFails("CIT-GAL-0001")).isFalse();
  }

  @Test
  @DisplayName("A blank prefix is ignored, because it would fail everybody")
  void blankPrefixIsIgnored() {
    MockGatewayFailureRule rule = rule(List.of(" ", ""), List.of());

    assertThat(rule.smsFails("CIT-COL-0001")).isFalse();
    rule.setSmsPrefixes(List.of("", "CIT-COL-"));
    assertThat(rule.smsFails("CIT-GAL-0001")).isFalse();
    assertThat(rule.smsFails("CIT-COL-0001")).isTrue();
  }

  @Test
  @DisplayName("Null arguments are rejected")
  void nullArgumentsAreRejected() {
    MockGatewayFailureRule rule = rule(List.of(), List.of());

    assertThatNullPointerException().isThrownBy(() -> rule.smsFails(null));
    assertThatNullPointerException().isThrownBy(() -> rule.setPushPrefixes(null));
    assertThatNullPointerException().isThrownBy(() -> new MockGatewayFailureRule(null));
  }

  /** Enables the properties record for the binding tests. */
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(MockGatewayProperties.class)
  static class PropertiesConfiguration {}

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

  @Test
  @DisplayName("The properties bind comma-separated prefixes for both mocks")
  void propertiesBind() {
    contextRunner
        .withPropertyValues(
            "mock.sms.fail-recipient-prefixes=CIT-COL-,ORG-",
            "mock.push.fail-recipient-prefixes=CIT-KLT-")
        .run(
            context -> {
              MockGatewayProperties properties = context.getBean(MockGatewayProperties.class);
              assertThat(properties.sms().failRecipientPrefixes())
                  .containsExactly("CIT-COL-", "ORG-");
              assertThat(properties.push().failRecipientPrefixes()).containsExactly("CIT-KLT-");
            });
  }

  @Test
  @DisplayName("Without the properties both lists are empty")
  void propertiesDefaultToEmpty() {
    contextRunner.run(
        context -> {
          MockGatewayProperties properties = context.getBean(MockGatewayProperties.class);
          assertThat(properties.sms().failRecipientPrefixes()).isEmpty();
          assertThat(properties.push().failRecipientPrefixes()).isEmpty();
        });
    contextRunner
        .withPropertyValues(
            "mock.sms.fail-recipient-prefixes=", "mock.push.fail-recipient-prefixes=")
        .run(
            context ->
                assertThat(
                        context.getBean(MockGatewayProperties.class).sms().failRecipientPrefixes())
                    .isEmpty());
  }
}
