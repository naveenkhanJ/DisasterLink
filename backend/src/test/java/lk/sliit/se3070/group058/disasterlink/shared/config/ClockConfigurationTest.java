package lk.sliit.se3070.group058.disasterlink.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link ClockConfiguration}. */
class ClockConfigurationTest {

  @Test
  @DisplayName("The clock runs in the zone of Sri Lanka")
  void clockIsInColomboZone() {
    Clock clock = new ClockConfiguration().clock();

    assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Colombo"));
  }
}
