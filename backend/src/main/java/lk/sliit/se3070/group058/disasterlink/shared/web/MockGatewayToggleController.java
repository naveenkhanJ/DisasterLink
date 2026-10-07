package lk.sliit.se3070.group058.disasterlink.shared.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.MockGatewayFailureRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The run-time switch of the mock gateways, present only in the {@code dev} profile. A demo turns a
 * gateway "off" for some recipients and back "on", for example to show UC1's exception flow where
 * the SMS gateway fails: {@code POST /dev/mock-gateways/sms/failures} with {@code {"prefixes":
 * ["CIT-COL-"]}} and {@code DELETE /dev/mock-gateways/sms/failures}. The channel is {@code sms} or
 * {@code push}; any other channel is unknown (404).
 *
 * <p>Implementation-only class (standards section 11; MD section 6, MockGatewayFailureRule). The
 * path is outside {@code /api/} by the standards' decision.
 */
@RestController
@Profile("dev")
@RequestMapping("/dev/mock-gateways/{channel}/failures")
public class MockGatewayToggleController {

  private static final Logger LOG = LoggerFactory.getLogger(MockGatewayToggleController.class);

  /** What the toggle can do to one channel. */
  private record ChannelControl(Consumer<List<String>> set, Runnable clear) {}

  private final Map<String, ChannelControl> channels;

  /**
   * Creates the controller.
   *
   * @param rule the failure rule of the mock gateways
   */
  public MockGatewayToggleController(MockGatewayFailureRule rule) {
    Objects.requireNonNull(rule, "rule");
    this.channels =
        Map.of(
            "sms", new ChannelControl(rule::setSmsPrefixes, rule::clearSmsPrefixes),
            "push", new ChannelControl(rule::setPushPrefixes, rule::clearPushPrefixes));
  }

  /**
   * Makes a gateway fail for the recipients whose identifier starts with one of the prefixes.
   *
   * @param channel {@code sms} or {@code push}
   * @param request the prefixes; they replace the earlier ones
   * @throws NoSuchElementException if the channel is unknown
   */
  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void fail(
      @PathVariable String channel, @Valid @RequestBody FailurePrefixesRequest request) {
    control(channel).set().accept(request.prefixes());
    List<String> prefixes = request.prefixes();
    LOG.info("Mock {} gateway now fails for prefixes {}", channel, prefixes);
  }

  /**
   * Makes a gateway available again for every recipient.
   *
   * @param channel {@code sms} or {@code push}
   * @throws NoSuchElementException if the channel is unknown
   */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void restore(@PathVariable String channel) {
    control(channel).clear().run();
    LOG.info("Mock {} gateway available again", channel);
  }

  private ChannelControl control(String channel) {
    ChannelControl control = channels.get(channel);
    if (control == null) {
      throw new NoSuchElementException("unknown channel " + channel);
    }
    return control;
  }
}
