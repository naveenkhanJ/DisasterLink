package lk.sliit.se3070.group058.disasterlink.shared.gateway;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lk.sliit.se3070.group058.disasterlink.shared.config.MockGatewayProperties;
import org.springframework.stereotype.Component;

/**
 * Decides which recipients the mock gateways fail for. A send fails when the recipient's identifier
 * starts with a listed prefix: no downcast and no repository lookup, because the seeded identifiers
 * encode the district ({@code CIT-KLT-} fails Kalutara citizens only; organisations {@code
 * ORG-nnnn} and district officers {@code DO-DDD-nn} are not matched).
 *
 * <p>The lists start from the properties and can be changed while the application runs, by the
 * {@code dev} toggle endpoint, so each is held in a thread-safe reference and only ever replaced by
 * an immutable copy. A blank prefix is ignored, because it would match every recipient.
 *
 * <p>Implementation-only class of the model dictionary section 6 (PR14, R3G-10; P62 FX-03, R4G-03,
 * R4G-23).
 */
@Component
public class MockGatewayFailureRule {

  private final AtomicReference<List<String>> smsPrefixes;

  private final AtomicReference<List<String>> pushPrefixes;

  /**
   * Creates the rule from the bound properties.
   *
   * @param properties the initial prefixes of the two mocks
   */
  public MockGatewayFailureRule(MockGatewayProperties properties) {
    Objects.requireNonNull(properties, "properties");
    this.smsPrefixes = new AtomicReference<>(cleaned(properties.sms().failRecipientPrefixes()));
    this.pushPrefixes = new AtomicReference<>(cleaned(properties.push().failRecipientPrefixes()));
  }

  /**
   * Tells whether the SMS mock fails for a recipient.
   *
   * @param recipientId the identifier of the recipient
   * @return {@code true} if the identifier starts with a listed SMS prefix
   */
  public boolean smsFails(String recipientId) {
    return startsWithAny(recipientId, smsPrefixes.get());
  }

  /**
   * Tells whether the push mock fails for a recipient.
   *
   * @param recipientId the identifier of the recipient
   * @return {@code true} if the identifier starts with a listed push prefix
   */
  public boolean pushFails(String recipientId) {
    return startsWithAny(recipientId, pushPrefixes.get());
  }

  /**
   * Replaces the SMS failure prefixes.
   *
   * @param prefixes the new prefixes; blank ones are ignored
   */
  public void setSmsPrefixes(List<String> prefixes) {
    smsPrefixes.set(cleaned(prefixes));
  }

  /**
   * Replaces the push failure prefixes.
   *
   * @param prefixes the new prefixes; blank ones are ignored
   */
  public void setPushPrefixes(List<String> prefixes) {
    pushPrefixes.set(cleaned(prefixes));
  }

  /** Removes every SMS failure prefix: the SMS gateway is available again. */
  public void clearSmsPrefixes() {
    smsPrefixes.set(List.of());
  }

  /** Removes every push failure prefix: the push gateway is available again. */
  public void clearPushPrefixes() {
    pushPrefixes.set(List.of());
  }

  private static boolean startsWithAny(String recipientId, List<String> prefixes) {
    Objects.requireNonNull(recipientId, "recipientId");
    return prefixes.stream().anyMatch(recipientId::startsWith);
  }

  private static List<String> cleaned(List<String> prefixes) {
    return Objects.requireNonNull(prefixes, "prefixes").stream()
        .filter(prefix -> !prefix.isBlank())
        .toList();
  }
}
