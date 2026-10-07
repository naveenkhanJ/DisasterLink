package lk.sliit.se3070.group058.disasterlink.shared.web;

import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Answers every error of the JSON API (the {@code RestController}s) as an RFC 9457 {@code
 * ProblemDetail} with an HTTP status and a plain-language {@code detail}.
 *
 * <ul>
 *   <li>400: a malformed request, or one that Bean Validation rejects, or one with a missing part
 *       or a value of the wrong type;
 *   <li>404: {@code NoSuchElementException}, the convention for an unknown identifier: the members'
 *       services use {@code orElseThrow()} on a repository lookup;
 *   <li>the status of any exception that carries {@code @ResponseStatus}, with its message (the
 *       domain exceptions declare their 4xx status this way, so this class imports no feature
 *       package);
 *   <li>the status of the framework's own errors (for example 405 or 415);
 *   <li>500 for anything else, logged at ERROR with the exception.
 * </ul>
 *
 * <p>Validation and capacity results are not errors: they come back as normal replies carrying
 * {@code REJECTED}.
 *
 * <p>Implementation-only class (standards section 4, "Handlers (FE-14)"; MD section 0.2).
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /** Said for a request the server cannot accept. */
  static final String BAD_REQUEST_DETAIL =
      "The request is not valid: check the values you sent. Nothing was changed.";

  /** Said for an identifier that does not exist. */
  static final String NOT_FOUND_DETAIL = "The requested item does not exist.";

  /** Said for an unexpected failure. */
  static final String UNAVAILABLE_DETAIL = "The service is unavailable and nothing was changed.";

  /**
   * Answers a malformed or invalid request.
   *
   * @param exception the exception
   * @return a 400 reply
   */
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HandlerMethodValidationException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestPartException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception) {
    String summary = exception.toString();
    LOG.debug("Bad request: {}", summary);
    return reply(HttpStatus.BAD_REQUEST, BAD_REQUEST_DETAIL);
  }

  /**
   * Answers a lookup of an unknown identifier.
   *
   * @param exception the exception
   * @return a 404 reply
   */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException exception) {
    String summary = exception.toString();
    LOG.debug("Not found: {}", summary);
    return reply(HttpStatus.NOT_FOUND, NOT_FOUND_DETAIL);
  }

  /**
   * Answers every other exception: the status that {@code @ResponseStatus} declares, the status of
   * a framework error, or 500.
   *
   * @param exception the exception
   * @return the reply
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleOther(Exception exception) {
    ResponseStatus declared =
        AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
    if (declared != null) {
      return reply(declared.code(), messageOf(exception));
    }
    if (exception instanceof ErrorResponse errorResponse) {
      return reply(errorResponse.getStatusCode(), errorResponse.getBody().getDetail());
    }
    LOG.error("Unexpected error in the JSON API", exception);
    return reply(HttpStatus.INTERNAL_SERVER_ERROR, UNAVAILABLE_DETAIL);
  }

  private static String messageOf(Exception exception) {
    return exception.getMessage() == null
        ? exception.getClass().getSimpleName()
        : exception.getMessage();
  }

  private static ResponseEntity<ProblemDetail> reply(HttpStatusCode status, String detail) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }
}
