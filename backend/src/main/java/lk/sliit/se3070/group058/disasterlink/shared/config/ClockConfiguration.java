package lk.sliit.se3070.group058.disasterlink.shared.config;

import java.time.Clock;
import lk.sliit.se3070.group058.disasterlink.shared.web.WireTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the one {@link Clock} of the application, so that code never reads the system time
 * directly and tests can replace it with a fixed clock. Times are in the zone of Sri Lanka.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

  /**
   * Creates the system clock in the zone of Sri Lanka.
   *
   * @return the clock
   */
  @Bean
  public Clock clock() {
    return Clock.system(WireTime.ZONE);
  }
}
