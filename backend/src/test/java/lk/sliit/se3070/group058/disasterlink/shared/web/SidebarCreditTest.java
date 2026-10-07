package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import lk.sliit.se3070.group058.testprobe.PageProbeController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks that each of the four sidebar fragments of the layout shows the credit of the district
 * outlines (ODbL 1.0 requires the attribution; THIRD-PARTY-NOTICES.md). The pages are rendered
 * through the test-only probe controller, one page per fragment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PageProbeController.class)
class SidebarCreditTest {

  private static final String CREDIT =
      "<p class=\"sidebar__credit\">"
          + "Map data © OpenStreetMap contributors (ODbL), via geoBoundaries</p>";

  @Autowired private MockMvc mockMvc;

  static Stream<Arguments> pagesOfEachSidebar() {
    return Stream.of(
        Arguments.of("sidebar-dmc", "warning/showActiveWarning"),
        Arguments.of("sidebar-duty", "hazardreport/showPendingReports"),
        Arguments.of("sidebar-district", "shelter/editShelterDetails"),
        Arguments.of("sidebar-org", "organisation/showSharedReports"));
  }

  @ParameterizedTest(name = "{0}: {1}")
  @MethodSource("pagesOfEachSidebar")
  @DisplayName("Every sidebar fragment shows the OpenStreetMap credit after its navigation")
  void sidebarShowsTheCredit(String fragment, String template) throws Exception {
    String html =
        mockMvc
            .perform(
                get("/probe-page/render")
                    .param("name", template)
                    .param("withUser", "true")
                    .characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html).contains(CREDIT);
    assertThat(html.indexOf("</nav>")).isLessThan(html.indexOf(CREDIT));
  }
}
