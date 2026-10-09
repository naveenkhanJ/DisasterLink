package lk.sliit.se3070.group058.disasterlink.analytics.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.sliit.se3070.group058.disasterlink.analytics.domain.ReportNotSharedException;
import lk.sliit.se3070.group058.disasterlink.analytics.service.AnalyticsController;
import lk.sliit.se3070.group058.disasterlink.shared.domain.DistributionLine;
import lk.sliit.se3070.group058.disasterlink.shared.domain.District;
import lk.sliit.se3070.group058.disasterlink.shared.domain.HazardType;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportCriteria;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportHeader;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportMetric;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ReportResults;
import lk.sliit.se3070.group058.disasterlink.shared.domain.ScopedReport;
import lk.sliit.se3070.group058.disasterlink.shared.domain.SupplyCategory;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserService;
import lk.sliit.se3070.group058.disasterlink.shared.web.CurrentUserView;
import lk.sliit.se3070.group058.disasterlink.shared.web.DemoRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link DonorReportView}.
 *
 * <p>Implements scenario flow tests for SC4 steps 16-19 and E6.
 */
@ExtendWith(MockitoExtension.class)
class DonorReportViewTest {

  @Mock private AnalyticsController analyticsController;
  @Mock private CurrentUserService currentUserService;

  private MockMvc mockMvc;
  private CurrentUserView donorUserView;
  private ReportHeader sampleHeader;

  @BeforeEach
  void setUp() {
    DonorReportView controller = new DonorReportView(analyticsController, currentUserService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    donorUserView = new CurrentUserView("CS", "CARE Sri Lanka", "Resource-Owning Organisation");

    ReportCriteria criteria =
        new ReportCriteria(
            Set.of(District.RATNAPURA),
            HazardType.FLOOD,
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 25),
            Set.of(ReportMetric.ALERT_TIMELINE),
            "HZ-2026-0142");

    sampleHeader =
        new ReportHeader(
            "DR-2026-0184",
            "Flood response",
            LocalDateTime.of(2026, 10, 8, 20, 30),
            criteria,
            Map.of());

    when(currentUserService.userId(DemoRole.ORGANIZATION)).thenReturn("ORG-1");
    when(currentUserService.currentUser(DemoRole.ORGANIZATION)).thenReturn(donorUserView);
  }

  @Test
  @DisplayName(
      "SC4 Step 16 / WF4-S05 — openSharedReports displays shared reports list for organisation")
  void openSharedReportsDisplaysReports() throws Exception {
    when(analyticsController.getSharedReports("ORG-1")).thenReturn(List.of(sampleHeader));

    mockMvc
        .perform(get("/organisation/reports"))
        .andExpect(status().isOk())
        .andExpect(view().name("organisation/showSharedReports"))
        .andExpect(model().attribute("reports", List.of(sampleHeader)))
        .andExpect(model().attribute("organizationId", "ORG-1"))
        .andExpect(model().attribute("currentUser", donorUserView));

    verify(analyticsController).getSharedReports("ORG-1");
  }

  @Test
  @DisplayName("SC4 Step 18 / WF4-S06 — openSharedReport displays organisation scoped view")
  void openSharedReportDisplaysScopedView() throws Exception {
    DistributionLine line =
        new DistributionLine(
            District.RATNAPURA,
            "ORG-1",
            "CARE Sri Lanka",
            SupplyCategory.FOOD,
            "dry-ration packs",
            1200.0);
    ReportResults scopedResults =
        new ReportResults(1, List.of(), Map.of(), 0, List.of(), 0, List.of(line), Set.of());
    ScopedReport scopedReport = new ScopedReport(sampleHeader, scopedResults);

    when(analyticsController.getScopedView("DR-2026-0184", "ORG-1")).thenReturn(scopedReport);

    mockMvc
        .perform(get("/organisation/report").param("reportId", "DR-2026-0184"))
        .andExpect(status().isOk())
        .andExpect(view().name("organisation/showScopedReport"))
        .andExpect(model().attribute("scopedReport", scopedReport))
        .andExpect(model().attribute("organizationId", "ORG-1"))
        .andExpect(model().attribute("currentUser", donorUserView));

    verify(analyticsController).getScopedView("DR-2026-0184", "ORG-1");
  }

  @Test
  @DisplayName("SC4 E6 / WF4-S05a — openSharedReport refuses unshared report with showNotShared")
  void openSharedReportRefusesUnsharedReport() throws Exception {
    when(analyticsController.getScopedView("DR-2026-9999", "ORG-1"))
        .thenThrow(new ReportNotSharedException("Not shared"));

    mockMvc
        .perform(get("/organisation/report").param("reportId", "DR-2026-9999"))
        .andExpect(status().isOk())
        .andExpect(view().name("organisation/showNotShared"))
        .andExpect(model().attribute("notSharedError", "Not shared"));
  }

  @Test
  @DisplayName(
      "SC4 Step 16 / PR15 — openSharedReports resolves custom organizationId and falls back on blank")
  void openSharedReportsResolvesOrganizationId() throws Exception {
    when(analyticsController.getSharedReports("ORG-2")).thenReturn(List.of(sampleHeader));

    mockMvc
        .perform(get("/organisation/reports").param("organizationId", "ORG-2"))
        .andExpect(status().isOk())
        .andExpect(view().name("organisation/showSharedReports"))
        .andExpect(model().attribute("organizationId", "ORG-2"));

    when(analyticsController.getSharedReports("ORG-1")).thenReturn(List.of(sampleHeader));
    mockMvc
        .perform(get("/organisation/reports").param("organizationId", "   "))
        .andExpect(status().isOk())
        .andExpect(view().name("organisation/showSharedReports"))
        .andExpect(model().attribute("organizationId", "ORG-1"));
  }
}
