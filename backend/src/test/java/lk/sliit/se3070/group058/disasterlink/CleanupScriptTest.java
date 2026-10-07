package lk.sliit.se3070.group058.disasterlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

/**
 * Proves {@code cleanup.sql}: a test without a transaction commits its data, the script empties it
 * after the method, and the next test finds the tables empty. A last test checks that the script
 * names every table of the schema.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CleanupScriptTest {

  @Autowired private OrganizationRepository organizations;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @Order(1)
  @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  @DisplayName("Data saved without a transaction is committed")
  void dataIsCommittedWithoutATransaction() {
    organizations.save(
        new Organization(
            "ORG-CLEANUP",
            "Cleanup",
            OrganizationType.NGO,
            "0112222222",
            "Colombo",
            List.of(District.COLOMBO),
            Language.ENGLISH));

    assertThat(organizations.count()).isEqualTo(1);
  }

  @Test
  @Order(2)
  @DisplayName("The script has emptied the tables, including the collection tables")
  void tablesAreEmptyAfterTheScript() {
    assertThat(organizations.count()).isZero();
    Integer districts =
        jdbcTemplate.queryForObject("select count(*) from organization_district", Integer.class);
    assertThat(districts).isZero();
  }

  @Test
  @Order(3)
  @DisplayName("The script names every table of the schema and leaves the sequences alone")
  void scriptNamesEveryTable() throws IOException {
    String script =
        new ClassPathResource("cleanup.sql")
            .getContentAsString(StandardCharsets.UTF_8)
            .toLowerCase();
    List<String> tables =
        jdbcTemplate.queryForList(
            "select lower(table_name) from information_schema.tables"
                + " where table_schema = 'PUBLIC' and table_type = 'BASE TABLE'",
            String.class);

    assertThat(tables)
        .isNotEmpty()
        .allSatisfy(table -> assertThat(script).contains("truncate table " + table + ";"));
    assertThat(script.lines().filter(line -> !line.startsWith("--")).toList())
        .noneMatch(line -> line.contains("sequence"));
  }
}
