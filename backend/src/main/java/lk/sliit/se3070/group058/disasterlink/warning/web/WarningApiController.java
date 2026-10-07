package lk.sliit.se3070.group058.disasterlink.warning.web;

import java.util.List;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.warning.service.WarningController;
import lk.sliit.se3070.group058.disasterlink.warning.web.dto.ActiveWarningResponse;
import lk.sliit.se3070.group058.disasterlink.warning.web.mapper.WarningApiMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The endpoint through which the citizen app reads the warnings that are active for its district
 * ({@code GET /api/warnings/active?district=KALUTARA}; standards sections 12.1 and 12.2). It calls
 * {@link WarningController#getActiveWarnings} and maps each warning, with its hazard's type and
 * {@code locationName}, to a wire record. A missing or unknown {@code district} is a malformed
 * request, a 400 problem detail from {@code ApiExceptionHandler}; until the owner writes the body
 * of {@code getActiveWarnings} the answer is that handler's 500 problem.
 *
 * <p>The method is read-only transactional, like {@code PushOutboxController.poll}, so that the
 * mapper can read the warning's lazy hazard and collections (open-in-view is off; standards section
 * 4, "Exceptions"; section 12.1, item 10).
 *
 * <p>Written by the skeleton, not by the use-case owner. The POM's package rule for {@code
 * warning.web} excludes this class by name, and a class-level rule gates it at the same limits
 * (standards sections 8, 12.1 and 12.2). Implements CH-19 and CH-22 (the control's operation, MD
 * section 5) and CH-10 (the app's boundary).
 */
@RestController
@RequestMapping("/api/warnings")
public class WarningApiController {

  private final WarningController warningController;

  /**
   * Creates the controller.
   *
   * @param warningController the UC1 control
   */
  public WarningApiController(WarningController warningController) {
    this.warningController = warningController;
  }

  /**
   * Lists the warnings that are active now and cover a district.
   *
   * @param district the district of the citizen
   * @return the active warnings, possibly none
   */
  @GetMapping("/active")
  @Transactional(readOnly = true)
  public List<ActiveWarningResponse> getActiveWarnings(@RequestParam District district) {
    return warningController.getActiveWarnings(district).stream()
        .map(WarningApiMapper::toResponse)
        .toList();
  }
}
