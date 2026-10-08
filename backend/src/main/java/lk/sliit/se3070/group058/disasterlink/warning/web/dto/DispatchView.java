package lk.sliit.se3070.group058.disasterlink.warning.web.dto;

import java.util.List;

/**
 * The dispatch status of a warning's current round (WF1-S05, S05a, S05b), from a {@code
 * BroadcastSummary}. Implementation-only view record.
 *
 * @param delivered the delivered count, for example "102,549"
 * @param failedTotal the failed count over all channels, for example "1,240"
 * @param hasFailures whether any notification of the round failed
 * @param channels one row per channel, in the order Push, SMS, Audible alert
 * @param failures one alert per failed channel, for {@code showDeliveryFailure}
 * @param unreachable the unreachable recipients, "None" or a count
 */
public record DispatchView(
    String delivered,
    String failedTotal,
    boolean hasFailures,
    List<ChannelRow> channels,
    List<ChannelFailure> failures,
    String unreachable) {

  /**
   * One line of "Failures by channel".
   *
   * @param channel the channel label, for example "SMS"
   * @param failures "None" or, for example, "1,240 recipients"
   * @param failed whether this channel has failures
   */
  public record ChannelRow(String channel, String failures, boolean failed) {}

  /**
   * One delivery-failure alert ({@code showDeliveryFailure(channel, failedRecipientIds)}).
   *
   * @param channel the channel code, pre-selected in the resend pick-list
   * @param text the alert, for example "SMS delivery failed for 1,240 recipients"
   */
  public record ChannelFailure(String channel, String text) {}
}
