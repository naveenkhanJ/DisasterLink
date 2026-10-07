package lk.sliit.se3070.group058.disasterlink.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The recipient-ID prefixes for which the mock gateways report a failed send, bound from the
 * properties {@code mock.sms.fail-recipient-prefixes} and {@code mock.push.fail-recipient-prefixes}
 * (comma-separated, empty by default). A seeded citizen's identifier encodes the district ({@code
 * CIT-DDD-nnnn}), so the prefix {@code CIT-KLT-} fails Kalutara citizens only.
 *
 * <p>Implementation-only class of the model dictionary section 6 (MockGatewayFailureRule; P62
 * FX-03, R4G-03).
 *
 * @param sms the failure prefixes of the SMS mock
 * @param push the failure prefixes of the push mock
 */
@ConfigurationProperties("mock")
public record MockGatewayProperties(Sms sms, Push push) {

  /**
   * Creates the properties; a missing section means no prefixes.
   *
   * @param sms the SMS section, or {@code null} for none
   * @param push the push section, or {@code null} for none
   */
  public MockGatewayProperties(Sms sms, Push push) {
    this.sms = sms == null ? new Sms(List.of()) : sms;
    this.push = push == null ? new Push(List.of()) : push;
  }

  /**
   * The failure prefixes of the SMS mock.
   *
   * @param failRecipientPrefixes the prefixes; empty means no failures
   */
  public record Sms(List<String> failRecipientPrefixes) {

    /**
     * Creates the section; a missing list means no prefixes.
     *
     * @param failRecipientPrefixes the prefixes, or {@code null} for none
     */
    public Sms(List<String> failRecipientPrefixes) {
      this.failRecipientPrefixes =
          failRecipientPrefixes == null ? List.of() : List.copyOf(failRecipientPrefixes);
    }
  }

  /**
   * The failure prefixes of the push mock.
   *
   * @param failRecipientPrefixes the prefixes; empty means no failures
   */
  public record Push(List<String> failRecipientPrefixes) {

    /**
     * Creates the section; a missing list means no prefixes.
     *
     * @param failRecipientPrefixes the prefixes, or {@code null} for none
     */
    public Push(List<String> failRecipientPrefixes) {
      this.failRecipientPrefixes =
          failRecipientPrefixes == null ? List.of() : List.copyOf(failRecipientPrefixes);
    }
  }
}
