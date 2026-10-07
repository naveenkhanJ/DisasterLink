package lk.sliit.se3070.group058.disasterlink.shared.seed;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.CriteriaTemplate;
import lk.sliit.se3070.group058.disasterlink.analytics.repository.CriteriaTemplateRepository;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ClientSyncStatus;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.repository.ClientSyncStatusRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds what UC4 reads before any report exists: the saved criteria template of SC4 A2 and, only
 * when {@code demo.pending-client} is true, the Kalutara district officer's browser that still
 * holds three pending items (SC4 A3).
 */
@Component
@Profile("!test")
class ReportingSeed {

  /** SC4 A3 8a: the client has 3 pending items at its last contact. */
  private static final int PENDING_ITEMS = 3;

  /** SC4 A2 3b: the template's period runs from 15 May to 30 September, which is 138 days. */
  private static final int TEMPLATE_PERIOD_DAYS = 138;

  /** The days from 15 May to the scenarios' 20 May, the day 0 of the seed. */
  private static final int DAYS_BEFORE_EVENT = 5;

  private final CriteriaTemplateRepository templates;

  private final ClientSyncStatusRepository clients;

  ReportingSeed(CriteriaTemplateRepository templates, ClientSyncStatusRepository clients) {
    this.templates = templates;
    this.clients = clients;
  }

  /** Saves the template of SC4 A2, owned by DMC-0001. */
  void seedTemplate(SeedContext context, SeedPeople people) {
    // SC4 A2 3a-3b: name, districts, hazard type and four metrics. The period is "15 May - 30
    // September 2025", the year before the event, kept relative to the event day like every seed
    // time. The identifier is a Group_058 assumption.
    LocalDate start = context.eventDay().toLocalDate().minusYears(1).minusDays(DAYS_BEFORE_EVENT);
    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(
                District.COLOMBO,
                District.GAMPAHA,
                District.KALUTARA,
                District.RATNAPURA,
                District.KEGALLE),
            HazardType.FLOOD,
            start,
            start.plusDays(TEMPLATE_PERIOD_DAYS),
            EnumSet.allOf(ReportMetric.class),
            null);
    templates.save(
        new CriteriaTemplate(
            "TPL-0001",
            "Western and Sabaragamuwa monsoon flood review",
            criteria,
            people.officer()));
  }

  /**
   * Saves the Kalutara client with pending items; the page of SC4 A3 then shows a provisional
   * report.
   */
  void seedPendingClient(SeedContext context) {
    // SC4 A3 8a: Kalutara, 3 pending items, last contact 25 May 16:40 (day 5). The client
    // identifier is a Group_058 assumption.
    clients.save(
        new ClientSyncStatus(
            "CLIENT-DO-KLT-01", District.KALUTARA, context.event(5, 16, 40), PENDING_ITEMS));
  }
}
