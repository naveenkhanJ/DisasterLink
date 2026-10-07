package lk.sliit.se3070.group058.disasterlink.shared.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.config.DemoSeedProperties;
import lk.sliit.se3070.group058.disasterlink.shared.config.DemoUserProperties;
import lk.sliit.se3070.group058.disasterlink.shared.config.PhotoStorageProperties;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import lk.sliit.se3070.group058.disasterlink.shared.service.IdGenerator;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The seed with {@code demo.pending-client=true} saves the Kalutara client of SC4 A3. */
@DataJpaTest(properties = "demo.pending-client=true")
@ActiveProfiles("dev")
@Import({
  PeopleSeed.class,
  CompletedStorySeed.class,
  LiveStorySeed.class,
  ShelterSeed.class,
  ReportingSeed.class,
  PhotoSeed.class,
  IdGenerator.class,
  CurrentUserService.class,
  SeedDataRunnerTest.FixedClockConfiguration.class
})
@EnableConfigurationProperties({
  DemoUserProperties.class,
  DemoSeedProperties.class,
  PhotoStorageProperties.class
})
class PendingClientSeedTest {

  @TempDir static Path photoDirectory;

  @DynamicPropertySource
  static void photoStorage(DynamicPropertyRegistry registry) {
    registry.add("photo.storage-dir", () -> photoDirectory.toString());
  }

  @Autowired private AutowireCapableBeanFactory beans;

  @Autowired private ClientSyncStatusRepository clients;

  @Test
  @DisplayName("With demo.pending-client true one Kalutara client has pending items")
  void pendingClientIsSeeded() {
    SeedDataRunnerTest.runnerOf(beans).run(null);

    List<ClientSyncStatus> all = clients.findAll();
    assertThat(all).hasSize(1);
    assertThat(all.get(0).getDistrict()).isEqualTo(District.KALUTARA);
    assertThat(all.get(0).getPendingCount()).isPositive();
  }
}
