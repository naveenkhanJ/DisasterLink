package lk.sliit.se3070.group058.disasterlink.shared.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests of {@link IdGenerator} against H2, which also shows that the sequences of {@code
 * schema.sql} exist in tests.
 */
@DataJpaTest
@Import(IdGenerator.class)
class IdGeneratorTest {

  /** Supplies a clock fixed in 2026. */
  @TestConfiguration
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-05-21T02:45:00Z"), ZoneId.of("Asia/Colombo"));
    }
  }

  @Autowired private IdGenerator generator;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** Sequences are not rolled back with the test transaction, so each test restarts them. */
  @BeforeEach
  void restartSequences() {
    jdbcTemplate.execute("ALTER SEQUENCE hazard_id_seq RESTART WITH 300");
    jdbcTemplate.execute("ALTER SEQUENCE warning_id_seq RESTART WITH 200");
    jdbcTemplate.execute("ALTER SEQUENCE hazard_report_id_seq RESTART WITH 1000");
    jdbcTemplate.execute("ALTER SEQUENCE disaster_report_id_seq RESTART WITH 184");
    jdbcTemplate.execute("ALTER SEQUENCE citizen_id_seq RESTART WITH 1000");
  }

  @Test
  @DisplayName("The sequences of schema.sql exist in the test database")
  void sequencesExist() {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from information_schema.sequences"
                + " where upper(sequence_name) in ('HAZARD_ID_SEQ', 'WARNING_ID_SEQ',"
                + " 'HAZARD_REPORT_ID_SEQ', 'DISASTER_REPORT_ID_SEQ', 'CITIZEN_ID_SEQ')",
            Integer.class);

    assertThat(count).isEqualTo(5);
  }

  @Test
  @DisplayName("Hazard identifiers start at HZ-2026-0300 and count up")
  void hazardIdentifiersCountUp() {
    assertThat(generator.next(IdKind.HAZARD, null)).isEqualTo("HZ-2026-0300");
    assertThat(generator.next(IdKind.HAZARD, District.GALLE)).isEqualTo("HZ-2026-0301");
  }

  @Test
  @DisplayName("Warning identifiers start at 0200 and disaster report identifiers at 0184")
  void warningAndDisasterReportIdentifiers() {
    assertThat(generator.next(IdKind.WARNING, null)).isEqualTo("WRN-2026-0200");
    assertThat(generator.next(IdKind.DISASTER_REPORT, null)).isEqualTo("DR-2026-0184");
  }

  @Test
  @DisplayName("A hazard report identifier carries the year and the district code")
  void hazardReportIdentifier() {
    assertThat(generator.next(IdKind.HAZARD_REPORT, District.KALUTARA))
        .isEqualTo("RPT-2026-KLT-1000");
  }

  @Test
  @DisplayName("A citizen identifier carries the district code and no year")
  void citizenIdentifier() {
    assertThat(generator.next(IdKind.CITIZEN, District.COLOMBO)).isEqualTo("CIT-COL-1000");
  }

  @Test
  @DisplayName("Numbering is global per kind, whatever the district")
  void numberingIsGlobalPerKind() {
    assertThat(generator.next(IdKind.CITIZEN, District.GALLE)).isEqualTo("CIT-GAL-1000");
    assertThat(generator.next(IdKind.CITIZEN, District.MATARA)).isEqualTo("CIT-MTR-1001");
  }

  @Test
  @DisplayName("A hazard report or citizen identifier needs a district")
  void districtIsRequiredWhereTheFormatHasOne() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> generator.next(IdKind.HAZARD_REPORT, null));
    assertThatIllegalArgumentException().isThrownBy(() -> generator.next(IdKind.CITIZEN, null));
  }

  @Test
  @DisplayName("A null kind is rejected")
  void nullKindIsRejected() {
    assertThatNullPointerException().isThrownBy(() -> generator.next(null, District.GALLE));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(District.class)
  @DisplayName("Every district has a three-letter code")
  void everyDistrictHasAThreeLetterCode(District district) {
    String identifier = generator.next(IdKind.CITIZEN, district);

    assertThat(identifier).matches("CIT-[A-Z]{3}-\\d{4}");
  }
}
