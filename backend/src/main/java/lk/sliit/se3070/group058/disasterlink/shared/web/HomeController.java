package lk.sliit.se3070.group058.disasterlink.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the home page, which links to the five page roots: {@code /warning}, {@code
 * /hazardreport}, {@code /shelter}, {@code /analytics} and {@code /organisation}. The roots are
 * fixed for the members; the sidebar links of the layout already use them.
 *
 * <p>Implementation-only class (implementation standards section 11).
 */
@Controller
public class HomeController {

  /**
   * Shows the home page.
   *
   * @return the name of the home template
   */
  @GetMapping("/")
  public String home() {
    return "index";
  }
}
