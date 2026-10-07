package lk.sliit.se3070.group058.disasterlink.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the one {@link Clock} of the application, so that code never reads the system time
 * directly and tests can replace it with a fixed clock. Times are in the zone of Sri Lanka.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

  /** The zone all times of the system are in. */
  static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

  /**
   * Creates the system clock in the zone of Sri Lanka.
   *
   * @return the clock
   */
  @Bean
  public Clock clock() {
    return Clock.system(ZONE);
  }
}
