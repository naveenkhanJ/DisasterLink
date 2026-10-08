package lk.sliit.se3070.group058.disasterlink.warning.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Recipient;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RecipientKind;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.NotificationGateway;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delivers a warning to its recipients (the included use case Broadcast Warning, sd Broadcast
 * Warning): for each recipient and channel it can receive, it creates a {@link Notification} in the
 * recipient's language, adds it to the warning, sends it through the {@link NotificationGateway}
 * that supports the channel and records the result.
 *
 * <p>The summaries it returns cover the warning's current round (PR1): a recipient–channel pair
 * counts by its latest notification in that round, "latest" being the highest {@code sentTime} with
 * the {@code notificationId} as tie-break (P62 FX-09). Notifications loaded from the database have
 * no recipient object, so {@link #resend} resolves the stored kind and identifier through a map
 * from {@link RecipientKind} to a finder, built in the constructor (MD section 0.7, FE-10).
 *
 * <p>Implements CH-23 and CH-06 (UC1 steps 14–16, E1). The gateways are a Strategy, chosen by their
 * {@code supports} operation.
 */
@Service
public class WarningBroadcaster {

  /** The round of a warning's issue broadcast; each escalation adds one (PR1). */
  static final int FIRST_ROUND = 1;

  private static final Logger LOG = LoggerFactory.getLogger(WarningBroadcaster.class);

  /** Orders the notifications of one recipient–channel pair; the greatest is the latest. */
  private static final Comparator<Notification> BY_SENT_TIME =
      Comparator.comparing((Notification n) -> n.getSentTime().orElse(LocalDateTime.MIN))
          .thenComparing(Notification::getNotificationId);

  private final List<NotificationGateway> gateways;

  private final CitizenRepository citizens;

  private final OrganizationRepository organizations;

  private final DistrictOfficerRepository districtOfficers;

  private final Map<RecipientKind, Function<String, Optional<? extends Recipient>>>
      recipientFinders;

  private final Clock clock;

  /**
   * Creates the broadcaster.
   *
   * @param gateways the notification gateways, one per external service
   * @param citizens finds the citizens of the covered districts
   * @param organizations finds the organisations serving the covered districts
   * @param districtOfficers finds the district officers of the covered districts on an escalation,
   *     and a district officer named by a stored notification
   * @param clock supplies the time a delivery is recorded
   * @throws NullPointerException if an argument is {@code null}
   */
  public WarningBroadcaster(
      List<NotificationGateway> gateways,
      CitizenRepository citizens,
      OrganizationRepository organizations,
      DistrictOfficerRepository districtOfficers,
      Clock clock) {
    this.gateways = List.copyOf(gateways);
    this.citizens = Objects.requireNonNull(citizens, "citizens");
    this.organizations = Objects.requireNonNull(organizations, "organizations");
    this.districtOfficers = Objects.requireNonNull(districtOfficers, "districtOfficers");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.recipientFinders = new EnumMap<>(RecipientKind.class);
    this.recipientFinders.put(RecipientKind.CITIZEN, citizens::findById);
    this.recipientFinders.put(RecipientKind.ORGANIZATION, organizations::findById);
    this.recipientFinders.put(RecipientKind.DISTRICT_OFFICER, districtOfficers::findById);
  }

  /**
   * Broadcasts a newly issued warning as round 1 (SC1 step 14): to the citizens registered in its
   * covered districts and the organisations serving them, on each of its channels that the
   * recipient can receive. The notifications are added to the warning, which stores them with it.
   *
   * @param warning the warning just created
   * @return the summary of round 1
   * @throws NullPointerException if {@code warning} is {@code null}
   */
  public BroadcastSummary broadcast(Warning warning) {
    Set<District> districts = warning.coveredDistricts();
    List<Recipient> recipients = new ArrayList<>(citizens.findByDistrictIn(districts));
    recipients.addAll(organizations.findByDistrictIn(districts));
    return broadcastRound(warning, recipients, FIRST_ROUND);
  }

  /**
   * Re-broadcasts an escalated warning as a new round (Escalate step 11; sd Escalate Warning
   * message 10.2.4): to the citizens, the organisations and the district officers of its covered
   * districts, on each of its channels that the recipient can receive (PR1). The notifications of
   * the earlier rounds are kept.
   *
   * @param warning the warning just escalated
   * @return the summary of the new round
   * @throws NullPointerException if {@code warning} is {@code null}
   */
  public BroadcastSummary broadcastEscalation(Warning warning) {
    Set<District> districts = warning.coveredDistricts();
    List<Recipient> recipients = new ArrayList<>(citizens.findByDistrictIn(districts));
    recipients.addAll(organizations.findByDistrictIn(districts));
    recipients.addAll(districtOfficers.findByDistrictIn(districts));
    return broadcastRound(warning, recipients, currentRound(warning.getNotifications()) + 1);
  }

  private BroadcastSummary broadcastRound(Warning warning, List<Recipient> recipients, int round) {
    deliver(warning, recipients, warning.getChannels(), round);
    BroadcastSummary summary = summarise(warning.getNotifications(), List.of());
    String warningId = warning.getWarningId();
    int recipientCount = recipients.size();
    int delivered = summary.deliveredCount();
    Set<NotificationChannel> failedChannels = summary.failedRecipientIds().keySet();
    LOG.info(
        "Warning {} broadcast in round {} to {} recipients: {} delivered, failures on {}",
        warningId,
        round,
        recipientCount,
        delivered,
        failedChannels);
    return summary;
  }

  /**
   * Sends a warning again on one channel (SC1 E1 16d–16e). The targets are the recipients with a
   * recipient–channel pair whose latest notification in the current round is FAILED, and who have
   * no DELIVERED notification on {@code channel} in that round. Those who can receive {@code
   * channel} are sent a new notification in the same round; the others are listed as unreachable.
   *
   * @param warning the warning, loaded with its notifications
   * @param channel the channel the officer chose
   * @return the summary of the current round, with the unreachable recipients
   * @throws NullPointerException if an argument is {@code null}
   * @throws java.util.NoSuchElementException if a stored recipient no longer exists
   */
  public BroadcastSummary resend(Warning warning, NotificationChannel channel) {
    Objects.requireNonNull(channel, "channel");
    List<Notification> notifications = warning.getNotifications();
    int round = currentRound(notifications);
    List<Recipient> reachable = new ArrayList<>();
    List<String> unreachable = new ArrayList<>();
    for (Notification target : resendTargets(notifications, round, channel)) {
      Recipient recipient = resolve(target);
      if (recipient.canReceive(channel)) {
        reachable.add(recipient);
      } else {
        unreachable.add(recipient.getRecipientId());
      }
    }
    deliver(warning, reachable, Set.of(channel), round);
    String warningId = warning.getWarningId();
    int reachableCount = reachable.size();
    int unreachableCount = unreachable.size();
    LOG.info(
        "Warning {} resent on {} to {} recipients, {} unreachable",
        warningId,
        channel,
        reachableCount,
        unreachableCount);
    return summarise(warning.getNotifications(), unreachable);
  }

  /**
   * Summarises the current round of a warning's notifications (the rule at {@code
   * BroadcastSummary}, PR1). Package-private so that {@code WarningController.getBroadcastSummary}
   * applies the same rule without a second copy of it (L03 Duplicate Code).
   *
   * @param notifications the warning's notifications
   * @param unreachableRecipientIds the recipients a resend skipped; empty otherwise
   * @return the summary of the current round
   */
  static BroadcastSummary summarise(
      List<Notification> notifications, List<String> unreachableRecipientIds) {
    int delivered = 0;
    Map<NotificationChannel, List<String>> failed = new EnumMap<>(NotificationChannel.class);
    for (Notification latest : latestPerPair(notifications, currentRound(notifications))) {
      if (latest.getDeliveryStatus() == DeliveryStatus.DELIVERED) {
        delivered++;
      } else if (latest.getDeliveryStatus() == DeliveryStatus.FAILED) {
        failed
            .computeIfAbsent(latest.getType(), c -> new ArrayList<>())
            .add(latest.getRecipientId());
      }
    }
    return new BroadcastSummary(delivered, failed, unreachableRecipientIds);
  }

  private void deliver(
      Warning warning, List<Recipient> recipients, Set<NotificationChannel> channels, int round) {
    for (Recipient recipient : recipients) {
      for (NotificationChannel channel : channels) {
        if (recipient.canReceive(channel)) {
          send(warning, recipient, channel, round);
        }
      }
    }
  }

  /** Messages 2–8 of sd Broadcast Warning for one recipient and channel. */
  private void send(Warning warning, Recipient recipient, NotificationChannel channel, int round) {
    Language language = recipient.getPreferredLanguage();
    Notification notification =
        new Notification(recipient, channel, textFor(warning, channel, language), round);
    // Before the send: the push mock reads the warning from the notification (standards 12.2).
    warning.addNotification(notification);
    Optional<NotificationGateway> gateway =
        gateways.stream().filter(candidate -> candidate.supports(channel)).findFirst();
    if (gateway.isEmpty()) {
      LOG.warn("No gateway supports {}; notification {} stays pending", channel, notification);
      return;
    }
    LocalDateTime at = LocalDateTime.now(clock);
    if (gateway.get().send(notification)) {
      notification.markDelivered(at);
    } else {
      notification.markFailed(at);
      String recipientId = recipient.getRecipientId();
      LOG.debug("{} to {} failed", channel, recipientId);
    }
  }

  /** An SMS carries the message, the separator and the instructions (P60 N3, P62 FX-09). */
  private static String textFor(Warning warning, NotificationChannel channel, Language language) {
    String message = warning.getMessage().textIn(language);
    return channel == NotificationChannel.SMS
        ? message + Notification.SMS_TEXT_SEPARATOR + warning.getInstructions().textIn(language)
        : message;
  }

  /** One failed notification per target recipient, in the order the failures were recorded. */
  private static Collection<Notification> resendTargets(
      List<Notification> notifications, int round, NotificationChannel channel) {
    Set<List<Object>> deliveredOnChannel = new LinkedHashSet<>();
    for (Notification notification : notifications) {
      if (notification.getRound() == round
          && notification.getType() == channel
          && notification.getDeliveryStatus() == DeliveryStatus.DELIVERED) {
        deliveredOnChannel.add(recipientKey(notification));
      }
    }
    Map<List<Object>, Notification> targets = new LinkedHashMap<>();
    for (Notification latest : latestPerPair(notifications, round)) {
      List<Object> recipient = recipientKey(latest);
      if (latest.getDeliveryStatus() == DeliveryStatus.FAILED
          && !deliveredOnChannel.contains(recipient)) {
        targets.putIfAbsent(recipient, latest);
      }
    }
    return targets.values();
  }

  private Recipient resolve(Notification notification) {
    return recipientFinders
        .get(notification.getRecipientKind())
        .apply(notification.getRecipientId())
        .orElseThrow();
  }

  private static Collection<Notification> latestPerPair(
      List<Notification> notifications, int round) {
    Map<List<Object>, Notification> latest = new LinkedHashMap<>();
    for (Notification notification : notifications) {
      if (notification.getRound() == round) {
        List<Object> pair = new ArrayList<>(recipientKey(notification));
        pair.add(notification.getType());
        latest.merge(List.copyOf(pair), notification, BinaryOperator.maxBy(BY_SENT_TIME));
      }
    }
    return latest.values();
  }

  private static List<Object> recipientKey(Notification notification) {
    return List.of(notification.getRecipientKind(), notification.getRecipientId());
  }

  private static int currentRound(List<Notification> notifications) {
    return notifications.stream().mapToInt(Notification::getRound).max().orElse(FIRST_ROUND);
  }
}
