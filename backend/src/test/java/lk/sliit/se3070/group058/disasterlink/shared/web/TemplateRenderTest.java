package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lk.sliit.se3070.group058.testprobe.PageProbeController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Renders every template through Thymeleaf with a test-only controller. The copied prototype pages
 * read their data from the model, which the members' controllers will fill, so most of them cannot
 * render from an empty model. The test therefore separates the two ways a page can fail: a page
 * that needs its model (an expression evaluated on a missing value) is only listed; a page that
 * cannot be parsed or that names a missing fragment fails the test. The lists are written to {@code
 * target/template-render-report.txt}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PageProbeController.class)
class TemplateRenderTest {

  @Autowired private MockMvc mockMvc;

  private static List<String> templateNames() throws IOException {
    List<String> names = new ArrayList<>();
    Resource[] resources =
        new PathMatchingResourcePatternResolver().getResources("classpath:/templates/**/*.html");
    for (Resource resource : resources) {
      String path = resource.getURL().toString();
      String name =
          path.substring(
              path.indexOf("/templates/") + "/templates/".length(),
              path.length() - ".html".length());
      names.add(name);
    }
    return names.stream().sorted().toList();
  }

  /** Renders a template and returns {@code null} when it renders, otherwise the failure. */
  private Throwable render(String name, boolean withUser) {
    try {
      mockMvc.perform(
          get("/probe-page/render")
              .param("name", name)
              .param("withUser", String.valueOf(withUser)));
      return null;
    } catch (Exception exception) {
      return exception;
    }
  }

  /**
   * A failure caused by a value the model does not supply, as opposed to a syntax or fragment
   * error.
   */
  private static boolean needsModel(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      String text = String.valueOf(cause.getMessage());
      if (cause.getClass().getSimpleName().contains("SpelEvaluationException")
          || text.contains("EL1007E")
          || text.contains("EL1008E")
          || text.contains("EL1011E")) {
        return true;
      }
    }
    return false;
  }

  @Test
  @DisplayName("Every template parses; those that cannot render from an empty model are listed")
  void everyTemplateParses() throws IOException {
    List<String> names = templateNames();
    List<String> rendersEmpty = new ArrayList<>();
    List<String> needsModelOnly = new ArrayList<>();
    List<String> rendersWithUser = new ArrayList<>();
    List<String> broken = new ArrayList<>();
    for (String name : names) {
      Throwable empty = render(name, false);
      if (empty == null) {
        rendersEmpty.add(name);
        continue;
      }
      if (!needsModel(empty)) {
        broken.add(name + ": " + empty);
        continue;
      }
      Throwable withUser = render(name, true);
      if (withUser == null) {
        rendersWithUser.add(name);
      } else if (needsModel(withUser)) {
        needsModelOnly.add(name);
      } else {
        broken.add(name + " (with a current user): " + withUser);
      }
    }
    Path report = Path.of("target", "template-render-report.txt");
    Files.createDirectories(report.getParent());
    Files.writeString(
        report,
        "templates: "
            + names.size()
            + "\nrender from an empty model ("
            + rendersEmpty.size()
            + "): "
            + rendersEmpty
            + "\nrender once the model has a current user ("
            + rendersWithUser.size()
            + "): "
            + rendersWithUser
            + "\nneed more of their model ("
            + needsModelOnly.size()
            + "): "
            + needsModelOnly
            + "\n",
        StandardCharsets.UTF_8);

    assertThat(names).hasSize(36); // the layout, the 33 prototype pages, index and error/5xx
    assertThat(broken).as("templates that fail for a reason other than a missing model").isEmpty();
  }
}
