package lk.sliit.se3070.group058.testprobe;

import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A test-only page controller. It lives outside the application's base package, so only the tests
 * that import it see it. {@code /probe-page/fail} raises an exception, as a page controller of a
 * member might; {@code /probe-page/render} renders any template, with or without a current user.
 */
@Controller
public class PageProbeController {

  /** Raises an unexpected exception from a page request. */
  @GetMapping("/probe-page/fail")
  public String fail() {
    throw new IllegalStateException("boom");
  }

  /**
   * Renders a template with an empty model, or with only a current user.
   *
   * @param name the name of the template, for example {@code warning/showHazards}
   * @param withUser whether to add a current user to the model
   * @param model the model
   * @return the template name
   */
  @GetMapping("/probe-page/render")
  public String render(
      @RequestParam String name,
      @RequestParam(defaultValue = "false") boolean withUser,
      Model model) {
    if (withUser) {
      model.addAttribute("currentUser", new CurrentUserView("NJ", "N. Jayasinghe", "DMC Officer"));
    }
    return name;
  }
}
