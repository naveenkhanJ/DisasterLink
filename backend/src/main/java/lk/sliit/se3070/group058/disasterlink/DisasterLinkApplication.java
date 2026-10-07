package lk.sliit.se3070.group058.disasterlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the DisasterLink back end: the Smart Disaster Early-Warning and Emergency
 * Coordination System (SE3070 Group_058). Starts the Spring Boot application that serves the
 * officer screens and the citizen app's JSON API.
 *
 * <p>The class declares no {@code @Bean} methods, so bean-method proxying is switched off; that
 * lets the constructor stay private, as PMD's {@code UseUtilityClass} rule asks for a class that
 * only has a static method. Configuration belongs in the {@code shared.config} package.
 */
@SpringBootApplication(proxyBeanMethods = false)
@ConfigurationPropertiesScan
public final class DisasterLinkApplication {

  private DisasterLinkApplication() {}

  /**
   * Starts the application.
   *
   * @param args command-line arguments handed on to Spring Boot
   */
  public static void main(String[] args) {
    SpringApplication.run(DisasterLinkApplication.class, args);
  }
}
