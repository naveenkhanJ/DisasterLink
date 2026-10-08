package lk.sliit.se3070.group058.disasterlink.warning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.shared.domain.BroadcastSummary;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Citizen;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DeliveryStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Language;
import lk.sliit.se3070.group058.disasterlink.shared.domain.LocalisedText;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Notification;
import lk.sliit.se3070.group058.disasterlink.shared.domain.NotificationChannel;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Organization;
import lk.sliit.se3070.group058.disasterlink.shared.domain.OrganizationType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RecipientKind;
import lk.sliit.se3070.group058.disasterlink.shared.domain.RiverBasin;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SampleEntities;
import lk.sliit.se3070.group058.disasterlink.shared.domain.Warning;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningDetails;
import lk.sliit.se3070.group058.disasterlink.shared.domain.WarningLevel;
import lk.sliit.se3070.group058.disasterlink.shared.gateway.NotificationGateway;
import lk.sliit.se3070.group058.disasterlink.shared.repository.CitizenRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.DistrictOfficerRepository;
import lk.sliit.se3070.group058.disasterlink.shared.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link WarningBroadcaster}: the broadcast, the resend and the round summary (PR1). */
class WarningBroadcasterTest {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 45);

  /** When the stored notifications of the resend tests were recorded. */
  private static final LocalDateTime EARLIER = NOW.minusMinutes(10);

  private static final LocalisedText MESSAGE =
      new LocalisedText("සිංහල පණිවිඩය", "தமிழ் செய்தி", "Kalu Ganga rising");

  private static final LocalisedText INSTRUCTIONS =
      new LocalisedText("උපදෙස්", "அறிவுறுத்தல்", "Move to higher ground");

  private static final Set<NotificationChannel> ALL_CHANNELS =
      Set.of(NotificationChannel.PUSH, NotificationChannel.SMS, NotificationChannel.AUDIBLE_ALERT);

  private final CitizenRepository citizens = mock(CitizenRepository.class);

  private final OrganizationRepository organizations = mock(OrganizationRepository.class);

  private final DistrictOfficerRepository districtOfficers = mock(DistrictOfficerRepository.class);

  private final NotificationGateway pushGateway = mock(NotificationGateway.class);

  private final NotificationGateway smsGateway = mock(NotificationGateway.class);

  /** The notifications each gateway was given, with whether a warning was attached then. */
  private final List<Boolean> attachedWhenSent = new ArrayList<>();

  private WarningBroadcaster broadcaster;

  private final Citizen appUser =
      citizen("CIT-RAT-0001", District.RATNAPURA, Language.SINHALA, "token-1");

  private final Citizen smsOnly = citizen("CIT-KLT-0002", District.KALUTARA, Language.TAMIL, null);

  private final Organization redCross =
      new Organization(
          "ORG-0001",
          "Sri Lanka Red Cross",
          OrganizationType.NGO,
          "0112691095",
          "Colombo 07",
          List.of(District.RATNAPURA),
          Language.ENGLISH);

  @BeforeEach
  void setUp() {
    when(pushGateway.supports(any()))
        .thenAnswer(call -> call.getArgument(0) != NotificationChannel.SMS);
    when(smsGateway.supports(any()))
        .thenAnswer(call -> call.getArgument(0) == NotificationChannel.SMS);
    when(pushGateway.send(any())).thenAnswer(call -> recordAttachment(call.getArgument(0), true));
    when(smsGateway.send(any())).thenAnswer(call -> recordAttachment(call.getArgument(0), true));
    broadcaster =
        new WarningBroadcaster(
            List.of(pushGateway, smsGateway),
            citizens,
            organizations,
            districtOfficers,
            Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO));
  }

  private boolean recordAttachment(Notification notification, boolean result) {
    attachedWhenSent.add(notification.getWarning().isPresent());
    return result;
  }

  private static Citizen citizen(
      String userId, District district, Language language, String deviceToken) {
    return new Citizen(
        userId,
        "Citizen " + userId,
        "0771234567",
        userId + "@example.lk",
        "199012345678",
        "Address",
        district,
        language,
        deviceToken);
  }

  private static Warning warning(Set<NotificationChannel> channels, List<Notification> stored) {
    WarningDetails details =
        new WarningDetails(
            WarningLevel.LEVEL_2_AMBER,
            MESSAGE,
            INSTRUCTIONS,
            List.of(District.RATNAPURA),
            List.of(RiverBasin.KALU),
            channels,
            NOW.plusDays(1));
    return new Warning(
        "WRN-2026-0087",
        SampleEntities.hazard("HZ-2026-0142"),
        details,
        SampleEntities.officer("DMC-0001", Set.of()),
        NOW,
        stored);
  }

  private static Notification stored(
      RecipientKind kind,
      String recipientId,
      NotificationChannel channel,
      int round,
      DeliveryStatus status,
      LocalDateTime sentTime) {
    return new Notification(kind, recipientId, channel, "text", round, status, sentTime);
  }

  private void recipientsOfTheCoveredDistricts() {
    Set<District> covered = Set.of(District.RATNAPURA, District.KALUTARA);
    when(citizens.findByDistrictIn(covered)).thenReturn(List.of(appUser, smsOnly));
    when(organizations.findByDistrictIn(covered)).thenReturn(List.of(redCross));
  }

  @Test
  @DisplayName("Main step 14 — one notification per recipient and receivable channel, in round 1")
  void createsOneNotificationPerReceivableChannel() {
    recipientsOfTheCoveredDistricts();
    Warning warning = warning(ALL_CHANNELS, List.of());

    broadcaster.broadcast(warning);

    assertThat(warning.getNotifications())
        .extracting(Notification::getRecipientId, Notification::getType)
        .containsExactlyInAnyOrder(
            tuple("CIT-RAT-0001", NotificationChannel.PUSH),
            tuple("CIT-RAT-0001", NotificationChannel.SMS),
            tuple("CIT-RAT-0001", NotificationChannel.AUDIBLE_ALERT),
            tuple("CIT-KLT-0002", NotificationChannel.SMS),
            tuple("ORG-0001", NotificationChannel.SMS));
    assertThat(warning.getNotifications()).allMatch(n -> n.getRound() == 1);
  }

  @Test
  @DisplayName("Main step 14 — an SMS carries the message and the instructions in the language")
  void smsCarriesMessageAndInstructions() {
    recipientsOfTheCoveredDistricts();
    Warning warning = warning(ALL_CHANNELS, List.of());

    broadcaster.broadcast(warning);

    assertThat(warning.getNotifications())
        .filteredOn(n -> n.getRecipientId().equals("CIT-KLT-0002"))
        .singleElement()
        .extracting(Notification::getMessage)
        .isEqualTo("தமிழ் செய்தி\nஅறிவுறுத்தல்");
    assertThat(warning.getNotifications())
        .filteredOn(n -> n.getType() == NotificationChannel.PUSH)
        .singleElement()
        .extracting(Notification::getMessage)
        .isEqualTo("සිංහල පණිවිඩය");
  }

  @Test
  @DisplayName("Main step 14 — each notification is added to the warning before it is sent")
  void addsBeforeSending() {
    recipientsOfTheCoveredDistricts();

    broadcaster.broadcast(warning(ALL_CHANNELS, List.of()));

    assertThat(attachedWhenSent).hasSize(5).containsOnly(true);
  }

  @Test
  @DisplayName("Main step 16 — every delivery is recorded as DELIVERED at the clock's time")
  void recordsTheDeliveries() {
    recipientsOfTheCoveredDistricts();
    Warning warning = warning(ALL_CHANNELS, List.of());

    BroadcastSummary summary = broadcaster.broadcast(warning);

    assertThat(warning.getNotifications())
        .allMatch(n -> n.getDeliveryStatus() == DeliveryStatus.DELIVERED)
        .allMatch(n -> n.getSentTime().equals(Optional.of(NOW)));
    assertThat(summary.deliveredCount()).isEqualTo(5);
    assertThat(summary.failedRecipientIds()).isEmpty();
    assertThat(summary.unreachableRecipientIds()).isEmpty();
  }

  @Test
  @DisplayName("E1 16a–16b — a failed SMS is recorded as FAILED and listed per channel")
  void recordsAFailedSms() {
    recipientsOfTheCoveredDistricts();
    doAnswer(
            call -> {
              Notification notification = call.getArgument(0);
              return !notification.getRecipientId().startsWith("CIT-KLT-");
            })
        .when(smsGateway)
        .send(any());
    Warning warning = warning(ALL_CHANNELS, List.of());

    BroadcastSummary summary = broadcaster.broadcast(warning);

    assertThat(summary.deliveredCount()).isEqualTo(4);
    assertThat(summary.failedRecipientIds())
        .isEqualTo(Map.of(NotificationChannel.SMS, List.of("CIT-KLT-0002")));
    assertThat(warning.getNotifications())
        .filteredOn(n -> n.getDeliveryStatus() == DeliveryStatus.FAILED)
        .extracting(Notification::getRecipientId)
        .containsExactly("CIT-KLT-0002");
  }

  @Test
  @DisplayName("Main step 14 — a channel no gateway supports leaves its notification pending")
  void noGatewayLeavesThePendingNotification() {
    WarningBroadcaster smsOnlyBroadcaster =
        new WarningBroadcaster(
            List.of(smsGateway),
            citizens,
            organizations,
            districtOfficers,
            Clock.fixed(NOW.atZone(COLOMBO).toInstant(), COLOMBO));
    recipientsOfTheCoveredDistricts();
    Warning warning = warning(Set.of(NotificationChannel.PUSH), List.of());

    BroadcastSummary summary = smsOnlyBroadcaster.broadcast(warning);

    assertThat(warning.getNotifications())
        .singleElement()
        .extracting(Notification::getDeliveryStatus)
        .isEqualTo(DeliveryStatus.PENDING);
    assertThat(summary.deliveredCount()).isZero();
    verify(smsGateway, never()).send(any());
  }

  @Test
  @DisplayName("E1 16d–16e — the resend reaches the failed recipients on SMS, in the same round")
  void resendsToTheFailedRecipients() {
    Warning warning =
        warning(
            ALL_CHANNELS,
            List.of(
                stored(
                    RecipientKind.CITIZEN,
                    "CIT-KLT-0002",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.FAILED,
                    EARLIER),
                stored(
                    RecipientKind.ORGANIZATION,
                    "ORG-0001",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.FAILED,
                    EARLIER),
                stored(
                    RecipientKind.CITIZEN,
                    "CIT-RAT-0001",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.DELIVERED,
                    EARLIER)));
    when(citizens.findById("CIT-KLT-0002")).thenReturn(Optional.of(smsOnly));
    when(organizations.findById("ORG-0001")).thenReturn(Optional.of(redCross));

    BroadcastSummary summary = broadcaster.resend(warning, NotificationChannel.SMS);

    assertThat(warning.getNotifications()).hasSize(5);
    assertThat(warning.getNotifications().subList(3, 5))
        .extracting(Notification::getRecipientId, Notification::getRound)
        .containsExactly(tuple("CIT-KLT-0002", 1), tuple("ORG-0001", 1));
    assertThat(summary.deliveredCount()).isEqualTo(3);
    assertThat(summary.failedRecipientIds()).isEmpty();
    assertThat(summary.unreachableRecipientIds()).isEmpty();
    verify(citizens, never()).findById("CIT-RAT-0001");
  }

  @Test
  @DisplayName("E1 16d — a resend on Push skips those who cannot receive it and lists them")
  void resendOnPushListsTheUnreachable() {
    Warning warning =
        warning(
            ALL_CHANNELS,
            List.of(
                stored(
                    RecipientKind.CITIZEN,
                    "CIT-RAT-0001",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.FAILED,
                    EARLIER),
                stored(
                    RecipientKind.CITIZEN,
                    "CIT-KLT-0002",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.FAILED,
                    EARLIER)));
    when(citizens.findById("CIT-RAT-0001")).thenReturn(Optional.of(appUser));
    when(citizens.findById("CIT-KLT-0002")).thenReturn(Optional.of(smsOnly));

    BroadcastSummary summary = broadcaster.resend(warning, NotificationChannel.PUSH);

    assertThat(summary.unreachableRecipientIds()).containsExactly("CIT-KLT-0002");
    assertThat(summary.deliveredCount()).isEqualTo(1);
    assertThat(summary.failedRecipientIds())
        .isEqualTo(Map.of(NotificationChannel.SMS, List.of("CIT-RAT-0001", "CIT-KLT-0002")));
  }

  @Test
  @DisplayName("E1 16e — a recipient already reached on the chosen channel is not sent again")
  void resendSkipsThoseAlreadyReachedOnTheChannel() {
    Warning warning =
        warning(
            ALL_CHANNELS,
            List.of(
                stored(
                    RecipientKind.CITIZEN,
                    "CIT-RAT-0001",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.FAILED,
                    EARLIER),
                stored(
                    RecipientKind.CITIZEN,
                    "CIT-RAT-0001",
                    NotificationChannel.PUSH,
                    1,
                    DeliveryStatus.DELIVERED,
                    EARLIER)));

    broadcaster.resend(warning, NotificationChannel.PUSH);

    assertThat(warning.getNotifications()).hasSize(2);
    verify(pushGateway, never()).send(any());
  }

  @Test
  @DisplayName("E1 16e — a stored recipient that no longer exists cannot be resent to")
  void resendNeedsTheStoredRecipient() {
    Warning warning =
        warning(
            ALL_CHANNELS,
            List.of(
                stored(
                    RecipientKind.DISTRICT_OFFICER,
                    "DO-RAT-01",
                    NotificationChannel.SMS,
                    1,
                    DeliveryStatus.FAILED,
                    EARLIER)));
    when(districtOfficers.findById("DO-RAT-01")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> broadcaster.resend(warning, NotificationChannel.SMS))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("Main step 19 — a pair counts by its latest notification of the round (PR1)")
  void summaryUsesTheLatestNotificationOfEachPair() {
    List<Notification> notifications =
        List.of(
            stored(
                RecipientKind.CITIZEN,
                "CIT-KLT-0002",
                NotificationChannel.SMS,
                1,
                DeliveryStatus.FAILED,
                NOW),
            stored(
                RecipientKind.CITIZEN,
                "CIT-KLT-0002",
                NotificationChannel.SMS,
                1,
                DeliveryStatus.DELIVERED,
                NOW.plusMinutes(30)),
            stored(
                RecipientKind.ORGANIZATION,
                "ORG-0001",
                NotificationChannel.SMS,
                1,
                DeliveryStatus.DELIVERED,
                NOW.plusMinutes(1)),
            stored(
                RecipientKind.ORGANIZATION,
                "ORG-0001",
                NotificationChannel.SMS,
                1,
                DeliveryStatus.FAILED,
                NOW.plusMinutes(2)));

    BroadcastSummary summary = WarningBroadcaster.summarise(notifications, List.of());

    assertThat(summary.deliveredCount()).isEqualTo(1);
    assertThat(summary.failedRecipientIds())
        .isEqualTo(Map.of(NotificationChannel.SMS, List.of("ORG-0001")));
  }

  @Test
  @DisplayName("Main step 19 — the summary covers only the current round (PR1)")
  void summaryCoversTheCurrentRound() {
    List<Notification> notifications =
        List.of(
            stored(
                RecipientKind.CITIZEN,
                "CIT-KLT-0002",
                NotificationChannel.SMS,
                1,
                DeliveryStatus.FAILED,
                NOW),
            stored(
                RecipientKind.CITIZEN,
                "CIT-KLT-0002",
                NotificationChannel.SMS,
                2,
                DeliveryStatus.DELIVERED,
                NOW.plusHours(1)));

    BroadcastSummary summary = WarningBroadcaster.summarise(notifications, List.of("X"));

    assertThat(summary.deliveredCount()).isEqualTo(1);
    assertThat(summary.failedRecipientIds()).isEmpty();
    assertThat(summary.unreachableRecipientIds()).containsExactly("X");
  }

  @Test
  @DisplayName("A warning without notifications has an empty summary")
  void emptySummary() {
    BroadcastSummary summary = WarningBroadcaster.summarise(List.of(), List.of());

    assertThat(summary.deliveredCount()).isZero();
    assertThat(summary.failedRecipientIds()).isEmpty();
  }
}
