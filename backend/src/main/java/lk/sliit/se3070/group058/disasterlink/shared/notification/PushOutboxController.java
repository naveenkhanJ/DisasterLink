package lk.sliit.se3070.group058.disasterlink.shared.notification;

import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The poll endpoint of the push mock. The app polls for its undelivered entries ({@code GET},
 * without side effects) and acknowledges the ones it has shown ({@code POST}, which sets {@code
 * deliveredAt}). Errors are handled by the shared JSON advice.
 *
 * <p>Implementation-only class of the model dictionary section 6 (PushOutbox and its poll endpoint;
 * FE-11; P62 FX-13, R4G-19).
 */
@RestController
@RequestMapping("/api/push/{recipientId}/outbox")
public class PushOutboxController {

  private static final Logger LOG = LoggerFactory.getLogger(PushOutboxController.class);

  private final PushOutboxRepository outbox;

  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param outbox the outbox repository
   * @param clock supplies the time of an acknowledgement
   */
  public PushOutboxController(PushOutboxRepository outbox, Clock clock) {
    this.outbox = Objects.requireNonNull(outbox, "outbox");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Returns the recipient's undelivered entries, oldest first. Polling changes nothing.
   *
   * @param recipientId the identifier of the recipient
   * @return the undelivered entries
   */
  @GetMapping
  @Transactional(readOnly = true)
  public List<PushEntryResponse> poll(@PathVariable String recipientId) {
    return outbox.findUndelivered(recipientId).stream().map(PushEntryMapper::toResponse).toList();
  }

  /**
   * Marks entries of the recipient as delivered. Identifiers that belong to another recipient, or
   * to no entry, are ignored.
   *
   * @param recipientId the identifier of the recipient
   * @param request the identifiers of the entries the app has shown
   */
  @PostMapping("/ack")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void acknowledge(
      @PathVariable String recipientId, @Valid @RequestBody PushAckRequest request) {
    LocalDateTime now = LocalDateTime.now(clock);
    List<PushOutbox> own =
        outbox.findAllById(request.ids()).stream()
            .filter(entry -> entry.getRecipientId().equals(recipientId))
            .toList();
    own.forEach(entry -> entry.markDelivered(now));
    int acknowledged = own.size();
    LOG.info("{} push entries of {} acknowledged", acknowledged, recipientId);
  }
}
