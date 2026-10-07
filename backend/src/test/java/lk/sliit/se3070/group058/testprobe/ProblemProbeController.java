package lk.sliit.se3070.group058.testprobe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * A test-only JSON controller whose endpoints raise each exception {@code ApiExceptionHandler}
 * answers. It lives outside the application's base package and is used only with a standalone
 * MockMvc, so it never joins an application context.
 */
@RestController
public class ProblemProbeController {

  /** A domain-style exception that declares its status with {@code @ResponseStatus}. */
  @ResponseStatus(HttpStatus.CONFLICT)
  public static class ConflictingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ConflictingException(String message) {
      super(message);
    }
  }

  /** The body that Bean Validation rejects when {@code name} is missing. */
  public record Body(@NotNull String name) {}

  @PostMapping("/probe/body")
  String body(@Valid @RequestBody Body body) {
    return body.name();
  }

  @GetMapping("/probe/parameter")
  String parameter(@RequestParam @Min(1) int count) {
    return String.valueOf(count);
  }

  @PostMapping("/probe/part")
  String part(@org.springframework.web.bind.annotation.RequestPart MultipartFile file) {
    return file.getOriginalFilename();
  }

  @GetMapping("/probe/missing")
  String missing() {
    throw new NoSuchElementException("No value present");
  }

  @GetMapping("/probe/declared")
  String declared() {
    throw new ConflictingException("The report was already decided");
  }

  @GetMapping("/probe/framework")
  String framework() {
    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot process this");
  }

  @GetMapping("/probe/failure")
  String failure() {
    throw new IllegalStateException("secret internal detail");
  }
}
