package lk.sliit.se3070.group058.disasterlink.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests that the test profile points the photo directory under {@code target/}, so a test run
 * writes no photo files next to the sources (re-check F-05).
 */
@SpringBootTest
class PhotoStoragePropertiesTest {

  @Autowired private PhotoStorageProperties properties;

  @Test
  @DisplayName("The test profile stores photos under target/test-photos")
  void testProfileKeepsPhotosUnderTarget() {
    assertThat(properties.storageDir()).isEqualTo("target/test-photos");
  }
}
