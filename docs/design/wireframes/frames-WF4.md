# Frames WF4 — UC4 Analyse Disaster Response & Generate Reports
Status: v2 + P87 content review (2026-10-09) — after independent review (2026-10-09), aligned to wireframe-spec §2.1 (canonical shell, grid and tokens); v1 draft by Claude (WF4 writer), 2026-10-09. Sources: `wireframe-spec.md` v0.2 (all, esp. §1, §2 Charts, §2.1, §3, §4, §5 WF4, §6 WF4, §7, §8); `scenarios/SC4-analyse-disaster-response.md` (main flow 1–19, A1–A7, E1–E6, Appendix 1, Appendix 2); `model-dictionary.md` v0.9 (§0 C1/C2/FX-14/FX-17, §1 enumerations, §2 value types ReportCriteria, CriteriaCheck, ReportResults, AlertTimelineEntry, OccupancyPoint, DistributionLine, ReportHeader, ScopedReport, ShareOutcome, ExportFile; §3 DisasterReport, CriteriaTemplate; §4 AnalyticsUI, DonorReportView, AnalyticsController); `change-list.md` v2.9 (CH-33, decision 8-2 and its UC4 breakdown); `storyboard-corrections.md` (SB4-1…SB4-7); Received Design Record UC4 §3 (storyboard p. 53) and §5.1–§5.5 (hi-fi pp. 57–61); `critique-findings.md` D5-08, D5-10, D5-11, D5-12; `implementation-standards.md` §1 (exporters, charts, not built), §4 (preview state, C1 banner), §11 (seed), §12.5; SC1 (name of the demo DMC Officer only).

## Shell

Two shells: one for the DMC Officer (`AnalyticsUI`, frames S01–S04 and their states) and one for the Resource-Owning Organisation (`DonorReportView`, S05, S05a and S06). Every frame is desktop **1440×1024**, no browser chrome. Grey/monochrome in the wf theme; ui theme per wireframe-spec §3.

**Geometry (all frames; wireframe-spec §2.1).** Sidebar 240 px wide; header **80 px** high; content area starts at x = 240, y = 80 with **24 px padding on every side**: usable area = (1440 − 240 − 2 × 24) × (1024 − 80 − 2 × 24) = **1152 × 896 px**. 16 px gaps between regions. Two-column split **744 + 16 + 392 = 1152**; two equal cards inside a 744 column: 364 + 16 + 364 = 744. Every height list below adds up to exactly 896.

**Type scale and wf tokens (§2.1), used by name below:** page title 24/32 bold; section (card) title 18/24 bold; body 14/20; label 12/16 semibold; big number 28/36 bold. **card** (white, 1 px `#BDBDBD`, radius 12); **primary** / **secondary** / **disabled** buttons; **selected** (2 px black border, check icon, bold text); **tag** (1 px black border, full text; Level 3 – Red (Evacuate) with a 2 px border); **error** (field border 2 px black, error icon, bold message below the field; form-level error box = white card, 2 px border, error icon); **notice** (`#F2F2F2` fill, 1 px border, matching icon); **charts** (bars `#757575`, lines 2 px black, capacity 1.5 px black dashed, axes and labels `#757575`). Icons only from §2.1's set; field and form errors always use the **error** icon.

**Officer shell (`AnalyticsUI`)**
- Sidebar, left, 240 px wide, full height:
  - Logo block: rounded square badge "DMC", then "DMC Sri Lanka" (bold).
  - Navigation (wireframe-spec §3, DMC Officer): "Warnings" (warning-triangle icon, per §2.1; goes to WF1-S01 — `WarningDashboardUI.openDashboard()`), "Analytics" (chart icon; **selected**; `AnalyticsUI.openAnalytics()`); "Hazards" and "Operational picture" signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91).
  - Nothing else in the sidebar (Group_060's "Officer Portal / National Early Warning v2.1" block has no source and is dropped).
- Header bar, top, right of the sidebar, **80 px** high × 1200 px, white, bottom border:
  - Left: page title (24/32 bold, per frame, below) and a one-line subtitle (14 px, per frame).
  - Right: initials avatar "NJ" (circle; user icon not used), "N. Jayasinghe" (bold), "DMC Officer" (grey). (Demo user — Gaps 2.)
- Common error banner (MD C1): **not drawn in WF4** — it is the single frame COMMON-B1 owned by `frames-WF1.md`; on the analytics workspace it appears in the same place (top of the content area).

**Organisation shell (`DonorReportView`)**
- Sidebar as above, logo block "DMC" / "DMC Sri Lanka", navigation items: "Shared reports" (report icon; **selected**; `DonorReportView.openSharedReports(organizationId)`) (wording ours — Gaps 8); "Resources" (file icon; Register Resources) and "Donations" (plus icon; Donate Relief Supplies) signposted (muted, tag "Not in this version"; a click shows "<item> — not available in this version" — wireframe-spec §8 rule 12, P91).
- Header right: initials avatar "CS", "CARE Sri Lanka" (bold), "Resource-Owning Organisation" (grey). In S05a the signed-in organisation is Sarvodaya: avatar "S", "Sarvodaya", "Resource-Owning Organisation".

**Common value formats (all frames):** dates "20 May 2026", "1 May 2026" (no leading zero; quoted scenario messages keep their own wording); date-times "20 May 2026, 10:45" (comma before the time); times alone "10:45"; numbers with thousands separators "42,335"; warning levels exactly "Level 2 – Amber (Be Alert)", "Level 3 – Red (Evacuate)" in a **tag** (wf; Level 3 – Red with the 2 px border) / tinted tag (ui); report status as a word in a **tag**: "Final" or "Provisional"; hazard label "Flood — Kalu Ganga at Ratnapura" (MD C2, FX-14); hazard status **tag** "Closed"; supply categories "Food", "Water", "Medicine", "Shelter material"; channels "Push", "SMS", "Audible alert"; metrics "Alert timeline", "Citizens reached", "Shelter occupancy over time", "Resource distribution".

**Placeholder tokens.** The renderer prints these exactly as written until Afham sets values:
- Generation time: **26 May 2026, 09:30** (`DisasterReport.generatedDate` / `ReportHeader.generatedDate`) — an assumption, P85-5 (Gaps 1).
- A3 matched records: **206,245** — an assumption, P85-5 (Gaps 14).

**Occupancy chart rule (used on S02, S03, S04, S06 and their states; renderer must not guess).** Only two scenario values are drawn as data:
- a **capacity line** (1.5 px black dashed) at 6,300, labelled "Combined capacity 6,300";
- **one point** (black, 6 px radius) at 21 May, value 5,920, labelled "Peak 5,920 · 21 May · 94 %".
Everything else about occupancy is a **light placeholder band** (`#F2F2F2` fill, no outline, no vertices, no markers) spanning the x axis from 20 May to 25 May below the capacity line, labelled inside the band "occupancy from the stored records (1,140 records)". The x axis has day ticks "20 May" … "25 May". The y axis has **no tick values**: the only y-values printed are the capacity label (6,300) and the peak label (5,920). No other vertex, curve line or value is drawn. No escalation marker on this chart.
- Derivations (for the caption): "19 shelters" = the number of distinct `OccupancyPoint.shelterId` in `results.occupancyOverTime`; "94 %" = peak ÷ `combinedCapacity` = 5,920 ÷ 6,300 = 93.97 %, rounded. Both figures are stated in SC4 step 7; the derivation says how the screen gets them from the display.

## Screens

### WF4-S01 Analytics workspace — report criteria (event pre-filled)
- Frame: desktop 1440×1024
- Shows: scenario steps 1–5 (state after step 4's pre-fill with step 5's metric selection made, before "Generate report") ; boundary `AnalyticsUI` ; displays `showTemplates(templates)`, `showClosedEvents(hazards)`, `showCriteria(criteria)` ; reached by `openAnalytics()` (sidebar "Analytics") then `selectEvent("HZ-2026-0142")`
- Corrects received screen: p. 57 `Automated Report Builder` (its "1. Choose Template" list and "2. Configure Scope" district chips kept as layout; storyboard panels 2–6); answers CH-33 (criteria: template, event pre-fill, districts, type, period, metrics), D5-10, D5-11 ("Schedule briefing" removed), SB4-2
- Layout (top to bottom, left to right; widths 392 + 16 + 744 = 1152 px; heights: left card 392 × 896, right card 744 × 896 — both full usable height):
  1. Header — title "Analyse disaster response" (wording ours); subtitle "Choose a saved template or a closed hazard event, then set the report criteria." (wording ours).
  2. Left column (392 px), **card** "1. Start from" (wording ours), two sub-sections:
     - "Saved criteria templates" (wording ours) — one row (the seed has one template): title "Western and Sabaragamuwa monsoon flood review"; grey line "Colombo, Gampaha, Kalutara, Ratnapura, Kegalle · Flood · 15 May 2025 – 30 September 2025 · 4 metrics". Not selected (outline only).
     - "Closed hazard events" (wording ours) — one row, **selected** token (2 px black border, check icon, bold text): "HZ-2026-0142 · Flood — Kalu Ganga at Ratnapura · Ratnapura, Kalutara · 20–25 May 2026" and a **tag** "Closed". (Only this event is named — Gaps 5.)
  3. Right column (744 px), **card** "2. Report criteria" (wording ours):
     - Line under the card title, grey: "Pre-filled from event HZ-2026-0142" (wording ours; from `criteria.hazardId`).
     - "Districts" — chips "Ratnapura" and "Kalutara" (**selected** token, each with a cross icon to remove), then a pick-list "Add district" (plus icon; the 25 districts).
     - "Hazard type" — select showing "Flood" (options: "All hazard types" (wording ours, empty value, A6), "Flood", "Landslide", "Cyclone", "Tsunami", "Drought", "Blocked road", "Other").
     - "Period" — date field "20 May 2026", "to", date field "25 May 2026"; grey helper "A period of at most 24 months." (wording ours; rule from step 6).
     - "Metrics" — four checkboxes, **all ticked**: "Alert timeline", "Citizens reached", "Shelter occupancy over time", "Resource distribution".
     - Footer of the card, right-aligned: primary button "Generate report" (wording ours).
- Controls:
  - Template row — enabled, not selected → `AnalyticsUI.selectTemplate(templateId)` (A2).
  - Closed-event row "HZ-2026-0142 …" — enabled, selected → `AnalyticsUI.selectEvent(hazardId)` (step 3).
  - District chips (cross icon removes), "Add district" (plus icon), "Hazard type" select, the two date fields, the four metric checkboxes — form components of the criteria panel (MD C2); their values are carried by the submission.
  - "Generate report" — enabled, primary → `AnalyticsUI.enterCriteria(criteria)` (step 5; the handler calls `validateCriteria` then `generateReport` — step 6, FX-05).
- Map / chart / image: none (no UC4 map in wireframe-spec §2; storyboard panel 3's map sketch is not carried over).
- Not shown (removed on purpose): received "Incident Summary Report", "Resource Allocation Audit", "Safe Camps Occupancy", "Full National Disaster Log" template cards (no source; replaced by the seeded template — CH-31/CH-33); "OUTPUT FILE FORMAT" toggle (moved to the report page's Export card, S03/S04 — step 12); "Generate Document /" and "Schedule briefing" (no step schedules a report — D5-11); "Document Preview" skeleton (D5-10); "Recent Export Logs" table (export is recorded as an audit record, not listed — D5-10, CH-33); the 8 always-selected district chips Colombo…Kegalle; the photo avatar "Capt. K. Perera / Duty Officer - Colombo HQ" (initials avatar, demo user); storyboard panel 5's "Cost per beneficiary" (SB4-2).

### WF4-S02 Results — a chart per metric
- Frame: desktop 1440×1024
- Shows: scenario steps 6–10 (step 9 display; step 10 input) ; boundary `AnalyticsUI` ; displays `showResults(report)` (with `report.criteria` in the criteria strip) ; reached by `enterCriteria(criteria)` from S01 (valid criteria, report generated)
- Corrects received screens: p. 61 `Disaster Analytics Dashboard`, p. 60 `Historical Alert Timeline Logs`, p. 59 `Shelter Utilization & Capacity Audit`, p. 58 `Supply Chain & Resource Metrics` (four standing dashboards replaced by one generated report with one chart per selected metric); storyboard panels 7–8; answers CH-33, D5-08 (SB4-1, SB4-3, SB4-4, SB4-5), D5-10, D5-11, D5-12
- Heights (usable 896 px): 56 strip + 16 + 64 report header + 16 + 272 row 1 + 16 + 328 row 2 + 16 + 112 findings = **896**. Widths per row: 744 + 16 + 392 = **1152**.
- Layout (top to bottom, left to right):
  1. Header — title "Analyse disaster response" (wording ours); subtitle "Report generated from your criteria. Review the results and record your findings." (wording ours).
  2. Criteria strip (1152 × 56; the criteria panel shown compact, values kept — SC4 Conventions): label "Criteria" · chips "Ratnapura", "Kalutara" (**selected**) + "Add district" (plus icon) · select "Flood" · "20 May 2026" "to" "25 May 2026" · metric checkboxes (all four ticked) · secondary (outlined) button "Generate report".
  3. Report header **card** (1152 × 64):
     - Title (bold): "DR-2026-0184 · Flood response — Ratnapura, Kalutara, 20–25 May 2026".
     - Grey line: "Districts: Ratnapura, Kalutara · Hazard type: Flood · Period: 20–25 May 2026 · Event: HZ-2026-0142 · Matched records: 206,248 · Generated: 26 May 2026, 09:30".
     - Right: status tag "Final".
  4. Row 1 (height 272):
     a. Card "Alert timeline" (744 × 272) — chart (120 px) above, list below. List columns "Time", "Event", "Warning", "Level", "Districts"; rows:
        - "20 May 2026, 10:45" · "Issued" · "WRN-2026-0087" · tag "Level 2 – Amber (Be Alert)" · "Ratnapura, Kalutara"
        - "21 May 2026, 08:25" · "Escalated" · "WRN-2026-0087" · tag "Level 3 – Red (Evacuate)" · "Ratnapura, Kalutara"
     b. Card "Citizens reached" (392 × 272) — bar chart; under it grey note "Total = distinct citizens; a citizen reached on several channels counts once." (wording ours, from step 7).
  5. Row 2 (height 328):
     a. Card "Shelter occupancy over time" (392 × 328) — chart per the Shell's occupancy chart rule; caption line under it "Peak 5,920 occupants on 21 May — 94 % of combined capacity 6,300 (19 shelters)".
     b. Card "Resource distribution by district and owner" (744 × 328) — inside 16 px card padding: table on the left (424 px), 16 px gap, bars on the right (272 px): 16 + 424 + 16 + 272 + 16 = 744. Table columns "District", "Owner", "Category", "Quantity", "Unit"; rows:
        - "Ratnapura" · "CARE Sri Lanka" · "Food" · "1,200" · "dry-ration packs"
        - "Ratnapura" · "Sri Lanka Army" · "Water" · "18,000" · "litres"
        - "Ratnapura" · "Private individual" · "Food" · "480" · "dry-ration packs"
        - "Kalutara" · "Sri Lanka Red Cross Society" · "Shelter material" · "350" · "tarpaulins"
        - "Kalutara" · "CARE Sri Lanka" · "Medicine" · "220" · "first-aid kits"
        Grey note under the table: "Quantities are not added across units." (wording ours, step 7).
  6. Findings **card** (1152 × 112): label "Findings"; empty multi-line text area with grey placeholder "Record your findings on these results." (wording ours); right: primary button "Save findings".
- Controls:
  - Criteria strip fields and "Generate report" (secondary, enabled) → `AnalyticsUI.enterCriteria(criteria)` (A4 10a–10b: a new report; the earlier one is kept).
  - Findings text area — enabled, empty.
  - "Save findings" — primary, enabled → `AnalyticsUI.enterFindings(reportId, text)` (step 10).
- Map / chart / image (wireframe-spec §2 Charts; Chart.js in the implementation with the same data):
  - **Alert timeline:** horizontal time axis from "20 May" to "25 May 2026" (day ticks 20–25 May). Two event markers: (1) at 20 May 10:45 — circle marker, label "Issued · Level 2 – Amber (Be Alert)"; (2) at 21 May 08:25 — triangle marker, label "Escalated · Level 3 – Red (Evacuate)". Both labelled "WRN-2026-0087". Marker shape plus text distinguishes the events (never colour alone). Values from SC4 step 7.
  - **Citizens reached:** vertical bars, y axis 0–45,000 (ticks 0, 15,000, 30,000, 45,000). Bars with value labels: "Push" 30,100; "SMS" 42,335; "Audible alert" 30,100; then, separated by a gap and drawn as an outlined bar with a 45° hatch (it is not a channel), "Total (distinct)" 42,335. Values from SC4 step 7.
  - **Shelter occupancy over time:** exactly the Shell's occupancy chart rule — dashed capacity line 6,300, one labelled peak point 5,920 on 21 May (94 %), the light placeholder band "occupancy from the stored records (1,140 records)", no other vertices, no y tick values, no escalation marker.
  - **Resource distribution bars:** small multiples, one mini-chart per unit (no bar is compared across units), each a horizontal bar per distribution line labelled "District · Owner" with its quantity; owner told apart by a pattern plus the written owner name:
    - "dry-ration packs": "Ratnapura · CARE Sri Lanka" 1,200; "Ratnapura · Private individual" 480.
    - "litres": "Ratnapura · Sri Lanka Army" 18,000.
    - "tarpaulins": "Kalutara · Sri Lanka Red Cross Society" 350.
    - "first-aid kits": "Kalutara · CARE Sri Lanka" 220.
  - No map, no photo.
- Not shown (removed on purpose): every KPI card of pp. 58–61 ("Total Warnings Issued 247", "Citizen Reports Received 1,832", "People Evacuated", "Avg. Response Time", "Active Shelters 18", "Peak Occupancy Rate 12,400", "Average Citizen Stay", "Total Sheltered", "Active Shelters Running 24", "Total Distributions", "Ration Items Dispatched", "Districts Fully Shaded", "Mean Emergency Delivery") — standing dashboard data with no UC4 metric (CH-33, D5-10, D5-11, D5-12); "Verification Rate" ring and "Citizen Reports Trend" (UC2 data, D5-11); "Warnings by Hazard Type"; both teal maps and the "High-Risk Zones" list (no UC4 map; D5-12); "Capacity vs Actual Usage" bars, "Designated Safe House Directory" with "Meals Dispensed" and the Critical/Normal/High Occupancy/Stable tags (D5-11); "Distribution Category Breakdown" percentages and "Priority dispatch warehouses" (D5-11); timeline filters "All Hazards / Red Warnings / Last 7 Days" and the summary block "Avg Reach Rate", "Fastest Dispatch" (D5-10); per-entry "App Push Notifications … sent", "DMC Siren Alert SMS Network", "Cumulative Public Reach Rate" tiles (channels per CH-21 — SB4-4); storyboard panel 7's "Alerts issued vs verified reports" and "Median warning lead time 47 min (target 60)" (SB4-3); "Siren" and "Radio" reach bars (SB4-4); "2,914 records" (SB4-1); stacked unlabelled relief bars summed across units (SB4-5); the per-calculator breakdown of the 206,248 matched records (not a field of `ReportResults` — Gaps 13).

### WF4-S03 Report with findings (export and share panel)
- Frame: desktop 1440×1024
- Shows: scenario step 11 ; boundary `AnalyticsUI` ; displays `showResults(report)` (with `report.findings`) ; reached by `enterFindings(reportId, text)` from S02
- Corrects received screen: p. 57 `Automated Report Builder` (its "OUTPUT FILE FORMAT" segmented toggle "PDF (.pdf) / Excel (.xlsx) / CSV (.csv)" kept as layout, now in a right-hand action panel); storyboard panels 9 "Record Findings" and 10 "Export and Share"; answers CH-33 (findings → export/share), D5-08 (SB4-6), D5-10 (SB4-7), D5-11
- Widths: main column 744 + 16 + right panel 392 = **1152**.
- Heights, main column: 40 confirmation bar + 16 + 64 report header + 16 + 128 findings + 16 + 272 metric row A + 16 + 328 metric row B = **896**. Right panel: 232 Export card + 16 + 312 Share card = 560 (336 px free below it for the outcome boxes of the states).
- Layout (top to bottom, left to right):
  1. Header — title "Analyse disaster response" (wording ours); subtitle "DR-2026-0184 · Flood response — Ratnapura, Kalutara, 20–25 May 2026".
  2. Main column (744 px):
     a. Inline confirmation bar (744 × 40, **notice** token with the check icon): "Findings saved with report DR-2026-0184." (wording ours).
     b. Report header **card** (744 × 64): title "DR-2026-0184 · Flood response — Ratnapura, Kalutara, 20–25 May 2026" (bold); grey line "Ratnapura, Kalutara · Flood · 20–25 May 2026 · Event HZ-2026-0142 · Matched records: 206,248 · Generated: 26 May 2026, 09:30"; right: tag "Final".
     c. Findings **card** (744 × 128): label "Findings"; the text area holds the saved text, verbatim: "The warning reached 42,335 citizens; SMS reached every registered citizen, the app channels 30,100. Shelter capacity was near its limit on 21 May (94 %), after the escalation to Level 3 – Red. Kalutara received no food distribution — review the stock pre-positioned with the Kalutara DDMCU." Bottom right: secondary (outlined) "Save findings".
     d. Metric row A (272 px): card "Alert timeline" (364 × 272; same chart and two-row list as S02 4a) and card "Citizens reached" (364 × 272; same bars as S02 4b): 364 + 16 + 364 = 744.
     e. Metric row B (328 px): card "Shelter occupancy over time" (364 × 328; Shell occupancy chart rule, same caption as S02) and card "Resource distribution by district and owner" (364 × 328; the same five-row table, 11 px text; P88: the per-unit small-multiple bars do not fit this card with the table and are shown on S02 only; the note "Quantities are not added across units." stays).
  3. Right panel (392 px):
     a. **Card** "Export" (392 × 232, wording ours): "Format" — segmented control "PDF", "Excel", "CSV", **none selected**; grey note "Exports are tabular, without charts. Statistics are not recomputed." (wording ours, from step 13); button "Export" (**disabled** token until a format is chosen).
     b. **Card** "Share with organisations" (392 × 312, wording ours): checkbox list (organisations named in SC4 — Gaps 4): "CARE Sri Lanka", "Sri Lanka Red Cross Society", "Sri Lanka Army", "Sarvodaya" — **none ticked**; grey note "Each organisation sees the report with only its own distribution lines. No notification is sent; the organisation finds the report in its list of shared reports." (wording ours, from steps 15 and 19); button "Share report" (**disabled** token until an organisation is ticked).
- Controls:
  - Findings text area — enabled, holding the saved text; "Save findings" — secondary, enabled → `AnalyticsUI.enterFindings(reportId, text)` (findings can be saved again).
  - Format segmented control and organisation checkboxes — form components (MD C2).
  - "Export" — disabled here → `AnalyticsUI.exportReport(reportId, format)`.
  - "Share report" — disabled here → `AnalyticsUI.shareReport(reportId, organizationIds)`.
  - No criteria strip on this page (A4 changes criteria from S02).
- Map / chart / image: the four charts as S02 at the smaller card sizes, same values; occupancy per the Shell rule.
- Not shown (removed on purpose): storyboard panel 9's illustrative findings ("Lead time missed the 60-minute target in 4 of 9 escalations", "14% unreached", "12% below its population share", "08 May") (SB4-6); "Save draft" and the "418 / 1,500 characters" counter (no step or rule — D5-11); "criteria CR-0184" (criteria are not a separate record — CH-18); storyboard panel 10's "Donor read-only link … expires in 30 days" and "Share link" (no link is issued — SB4-7, CH-07, D5-10); "Logged to audit trail — AR-0416" (audit records are written, not shown — no display); p. 57's "Recent Export Logs" and "Schedule briefing" (D5-10, D5-11); "PDF brief / for the DMC Director" and "XLSX data / raw computed figures" (no source).

### WF4-S04 Report — export and share chosen
- Frame: desktop 1440×1024
- Shows: scenario steps 12 and 14 (choices made, before the officer presses the buttons) ; boundary `AnalyticsUI` ; displays `showResults(report)` (the same report page as S03) ; reached from S03 by choosing a format and ticking an organisation (form only, no operation)
- Corrects received screen: as S03 (p. 57 output-format toggle; storyboard panel 10); answers CH-33, D5-10 (SB4-7)
- Layout: identical to S03 (same widths and heights), except:
  1. The confirmation bar of S03 2a is gone; its 56 px (40 + 16) stay empty at the top of the main column, so every other region keeps S03's position (sum still 896).
  2. Export card: "PDF" **selected** token; "Export" enabled, **primary**.
  3. Share card: "CARE Sri Lanka" **ticked**; "Share report" enabled, **secondary** (one primary action per screen — wireframe-spec §8).
- Controls:
  - "Export" — primary, enabled → `AnalyticsUI.exportReport(reportId, format)` with `PDF` (step 12).
  - "Share report" — secondary, enabled → `AnalyticsUI.shareReport(reportId, organizationIds)` with CARE Sri Lanka's ID (step 14).
  - Findings and "Save findings" as S03.
- Map / chart / image: as S03.
- Not shown (removed on purpose): as S03.

### WF4-S05 Organisation — reports shared with it
- Frame: desktop 1440×1024
- Shows: scenario steps 16–17 ; boundary `DonorReportView` ; displays `showSharedReports(reports)` ; reached by `openSharedReports(organizationId)` (sidebar "Shared reports"), in CARE Sri Lanka's own session
- Corrects received screen: new — no received screen (Group_060 had no organisation view; storyboard panel 10 only); answers CH-33 (the organisation's list), CH-07, D5-10 (SB4-7)
- Layout (top to bottom, left to right):
  1. Header (organisation shell) — title "Shared reports" (wording ours); subtitle "Reports the DMC has shared with CARE Sri Lanka." (wording ours).
  2. Table **card** (1152 × 240) — columns "Report", "Generated", "Period", "Status", and an action column; one row (Gaps 5):
     - "DR-2026-0184 · Flood response — Ratnapura, Kalutara, 20–25 May 2026" · "26 May 2026, 09:30" · "20–25 May 2026" · **tag** "Final" · **primary** button "Open" (wording ours).
     No results are shown in the list (headers only — FX-04).
  3. 16 px below the table, **card** "Open a report by reference" (1152 × 120; wording ours): text field "Report reference" (empty, placeholder "e.g. DR-2026-0184" — wording ours) and secondary button "Open" (E6 18a; Gaps 10).
- Controls:
  - Row "Open" — primary, enabled → `DonorReportView.openSharedReport(reportId, organizationId)` with "DR-2026-0184" (step 18).
  - "Report reference" field + "Open" — secondary, enabled → `DonorReportView.openSharedReport(reportId, organizationId)` (E6 18a).
- Map / chart / image: none.
- Not shown (removed on purpose): any results in the list (FX-04); a notification of the share (none is sent — step 15, N21).

### WF4-S06 Organisation — scoped report
- Frame: desktop 1440×1024
- Shows: scenario steps 18–19 ; boundary `DonorReportView` ; displays `showScopedReport(scopedReport)` (from `scopedReport.header` — title, `criteria` period and districts, `provisionalSince` — and `scopedReport.results`) ; reached by "Open" on S05
- Corrects received screen: new — no received screen; answers CH-33 (scoped view per organisation), CH-07, D5-10 (SB4-7)
- Layout (top to bottom, left to right; rows 744 + 16 + 392 = 1152 wide as S02; heights 64 report header + 16 + 392 row 1 + 16 + 408 row 2 = **896**):
  1. Header (organisation shell) — title "DR-2026-0184 · Flood response — Ratnapura, Kalutara, 20–25 May 2026"; subtitle "Shared with CARE Sri Lanka by the DMC." (wording ours).
  2. Report header **card** (1152 × 64): "Period: 20–25 May 2026 · Districts: Ratnapura, Kalutara · Hazard type: Flood"; right: **tag** "Final" (no provisional districts) and the text link "Back to shared reports" (wording ours; no icon) → S05 (`openSharedReports`).
  3. Row 1 (392 px): card "Alert timeline" (744) (same chart and two-row list as S02 4a) and card "Citizens reached" (392; Push 30,100; SMS 42,335; Audible alert 30,100; Total (distinct) 42,335) — unchanged for the organisation (step 19).
  4. Row 2 (408 px): card "Shelter occupancy over time" (392; Shell occupancy chart rule; capacity 6,300, peak 5,920 on 21 May, 94 %) — unchanged; card "Resource distribution — your organisation's lines" (744; wording ours):
     - Grey note: "Only CARE Sri Lanka's distribution lines are shown." (wording ours).
     - Table columns "District", "Category", "Quantity", "Unit"; rows:
       - "Ratnapura" · "Food" · "1,200" · "dry-ration packs"
       - "Kalutara" · "Medicine" · "220" · "first-aid kits"
     - Bars (small multiples per unit): "dry-ration packs": "Ratnapura" 1,200; "first-aid kits": "Kalutara" 220.
- Controls: "Back to shared reports" link → `DonorReportView.openSharedReports(organizationId)`. No other control (read-only view).
- Map / chart / image: the three unchanged charts as S02; the distribution bars with CARE Sri Lanka's two lines only.
- Not shown (removed on purpose): the other three owners' lines (Sri Lanka Army, Private individual, Sri Lanka Red Cross Society — scoped by the server, ED2(a)); the findings, matched-record count and export/share controls (not part of the scoped view — step 19); "expires in 30 days" (SB4-7).

## States

### WF4-S01a Criteria from a template
- Base screen: WF4-S01; flow: A2 3a–3b; display: `showCriteria(criteria)` (from the template loaded at step 1)
- Change from base:
  - Template row "Western and Sabaragamuwa monsoon flood review" is **selected** token (2 px black border, check icon, bold text); the closed-event row is not selected.
  - Line under the criteria card title: "Filled from template Western and Sabaragamuwa monsoon flood review" (wording ours) instead of "Pre-filled from event …".
- Sample data: Districts chips (**selected**, each with a cross icon) "Colombo", "Gampaha", "Kalutara", "Ratnapura", "Kegalle"; Hazard type "Flood"; Period "15 May 2025" to "30 September 2025"; Metrics all four ticked. (3c — the officer then changes the period to 20–25 May 2026 and removes Colombo, Gampaha and Kegalle — is form editing that ends in S01's values; not a separate frame.)

### WF4-S01b Criteria errors
- Base screen: WF4-S01; flow: E1 6a–6b; display: `showValidationErrors(errors)`
- Change from base:
  - Period fields show the entered values in the **error** token (2 px black border); directly under them, error icon + bold "The start date is after the end date".
  - Metrics: all four checkboxes **unticked**; the metrics group gets the **error** token (2 px black border), and under it the error icon + bold "Select at least one metric". (wf theme: black border + error icon + bold message text — no colour; ui theme: the danger red on the same elements.)
  - No report is generated; the form keeps the values entered (C2). "Generate report" stays enabled.
- Sample data: Period "25 May 2026" to "20 May 2026"; no metric selected. Districts (Ratnapura, Kalutara), hazard type (Flood) and the selected event row are kept in the panel (Gaps 7).

### WF4-S01c Out-of-scope district with adjusted criteria
- Base screen: WF4-S01; flow: E4 6d–6f; display: `showOutOfScope(districts, adjusted)`
- Change from base:
  - Districts chips "Ratnapura", "Kalutara", "Galle" (each with a cross icon) — the "Galle" chip has the **error** token (2 px black border) and the error icon.
  - A **notice** box (warning-triangle icon) at the top of the criteria card: "Galle is outside your reporting districts (Colombo, Gampaha, Kalutara, Ratnapura, Kegalle)." Then "Adjusted criteria:" (wording ours) "Ratnapura, Kalutara · Flood · 20–25 May 2026 · Alert timeline, Citizens reached, Shelter occupancy over time, Resource distribution".
  - Buttons in the notice: primary "Confirm adjusted criteria" (wording ours) → `AnalyticsUI.confirmAdjustedCriteria()` (6f; re-validated, then the report is generated → S02). The card's "Generate report" becomes secondary (outlined) → `enterCriteria(criteria)` (the officer changes the criteria instead; continues at step 5).
- Sample data: selected districts Ratnapura, Kalutara, Galle (Gaps 7); `outOfScope` = {Galle}; `adjusted` = Ratnapura, Kalutara, Flood, 20–25 May 2026, four metrics.

### WF4-S01d No selected district in scope
- Base screen: WF4-S01; flow: E4 6g; display: `showValidationErrors(errors)`
- Change from base:
  - Districts: one chip "Galle" (cross icon) in the **error** token (2 px black border); directly under the districts field, error icon + bold "None of the selected districts is within your reporting districts".
  - No adjusted criteria are offered; no report is generated; "Generate report" stays enabled (continue as E1 6c).
- Sample data: Districts "Galle" only; hazard type, period and metrics as S01 (Gaps 7).

### WF4-S02a No records, with a suggestion
- Base screen: WF4-S02; flow: A1 8e–8g; display: `showNoRecords(suggestion)`
- Change from base:
  - Report header card, the four metric cards and the findings card are **not** shown (no report is stored).
  - In their place, one centred empty-state **notice** card with the chart icon and the cross icon side by side (744 × 240, centred in the freed area): heading "No data matches the criteria" (wording ours) and the suggestion verbatim: "No records for Kegalle, Landslide, 01–03 May 2026. Widen the districts or the period, or clear the hazard type."
  - Criteria strip stays editable; its "Generate report" button becomes **primary** (the only action — continue at step 5).
- Sample data: criteria strip — district chip "Kegalle"; hazard type "Landslide"; period "1 May 2026" to "3 May 2026"; metrics all four ticked (Gaps 7).

### WF4-S02b Provisional notice
- Base screen: WF4-S02; flow: A3 8a–8d; displays: `showResults(report)`, `showProvisionalNotice(since)`
- Heights: 56 strip + 16 + **40 notice bar + 16** + 64 report header + 16 + 244 row 1 + 16 + 300 row 2 + 16 + 112 findings = **896** (rows 1 and 2 each 28 px shorter than S02 to make room for the 56 px of the notice bar and its gap).
- Change from base:
  - Notice bar (1152 × 40, **notice** token, info icon) between the criteria strip and the report header card, verbatim: "Provisional — Kalutara: last synchronised 25 May 2026 16:40 with entries still pending on a device; Kalutara figures may be incomplete."
  - Report header status **tag** "Provisional" instead of "Final".
  - Report header line: "Matched records: 206,245" (A3 instance — assumption P85-5, Gaps 14).
- Sample data: `provisionalSince` = {Kalutara: 25 May 2026 16:40}. Title, criteria and the four charts' data as S02 (SC4 gives no other figures for this instance).

### WF4-S02c Computation error
- Base screen: WF4-S02; flow: E3 7a–7e; displays: `showResults(report)`, `showComputationError(metrics)`
- Heights: as S02b (a 40 px error bar + 16 px gap; rows 244 and 300; sum 896).
- Change from base:
  - Error bar (1152 × 40, form-level **error** box: white, 2 px border, error icon) between the criteria strip and the report header card, text (wording ours): "Shelter occupancy over time could not be computed. The other metrics are shown. Resubmit the criteria to try again."
  - Card "Shelter occupancy over time": no chart; in its place the chart icon and the cross icon side by side and "Not computed" (wording ours).
  - Alert timeline, Citizens reached and Resource distribution cards as S02 (same values). Findings card as S02 (empty, "Save findings" enabled — 7e continues at step 10).
  - Report header status **tag** "Final"; "Matched records: 205,108".
- Sample data: `failedMetrics` = {Shelter occupancy over time}; matched records 205,108 **(derived)** — 206,248 − 1,140 occupancy records, because each calculator counts its own part and `merge` sums them (FX-17), so the failed occupancy calculator contributes none (Gaps 6).

### WF4-S04a Export file provided
- Base screen: WF4-S04; flow: step 13; display: `showExportFile(file)`
- Change from base:
  - In the Export card, under the "Export" button, a success **notice** box (392 × 88, check icon): "PDF ready" (wording ours), file name "DR-2026-0184_flood-response_2026-05.pdf" with a file icon, and the link "Download" (wording ours). The link is part of `showExportFile(file)` — it hands the provided `ExportFile` to the browser — and is **not** an input operation.
  - "Share report" becomes **primary** (the next action); "Export" becomes secondary.
- Sample data: `ExportFile.fileName` "DR-2026-0184_flood-response_2026-05.pdf", `format` PDF.

### WF4-S04b Export error
- Base screen: WF4-S04; flow: E2 13a–13d; display: `showExportError(message)`
- Change from base:
  - In the Export card, under the "Export" button, a form-level **error** box (2 px border, error icon), verbatim: "The PDF could not be produced. The report is saved — try again, or export as Excel or CSV."
  - "PDF" stays **selected**; "Export" stays **primary** and enabled (retry without regenerating — 13d).
- Sample data: format PDF.

### WF4-S04c Sharing outcome
- Base screen: WF4-S04; flow: step 15 (A5 14b uses the same display); display: `showShared(outcome)`
- Change from base:
  - In the Share card, under "Share report", a success **notice** box (392 × 112, check icon):
    - "Shared with: CARE Sri Lanka" (labels wording ours; organisation from step 15)
    - "No data — not shared: none" (wording ours; step 15 "no organisation without data")
    - grey line: "CARE Sri Lanka will find the report in its list of shared reports." (wording ours, step 15)
  - Checkbox "CARE Sri Lanka" stays ticked.
- Sample data: `sharedWith` = [CARE Sri Lanka]; `noData` = []. A5 variant, not drawn separately (Gaps 11): CARE Sri Lanka and Sri Lanka Red Cross Society ticked → "Shared with: CARE Sri Lanka, Sri Lanka Red Cross Society".

### WF4-S04d "No data — not shared"
- Base screen: WF4-S04; flow: E5 15a–15c; display: `showShared(outcome)` (`noData` listed)
- Change from base:
  - Checkboxes "CARE Sri Lanka" and "Sarvodaya" ticked.
  - Outcome **notice** box (392 × 112, warning-triangle icon, not a plain success box):
    - "Shared with: CARE Sri Lanka"
    - "No data — not shared: Sarvodaya" (heading words from E5 15b)
    - grey line: "Sarvodaya owns no distribution line in this report." (wording ours, from 15a)
  - "Share report" stays enabled (15c: select a different organisation).
- Sample data: `sharedWith` = [CARE Sri Lanka]; `noData` = [Sarvodaya].

### WF4-S05a Report not shared
- Base screen: WF4-S05, in Sarvodaya's own session; flow: E6 18a–18b; displays: `showSharedReports(reports)` (empty list), then `showNotShared()`
- Change from base:
  - Header right: avatar "S", "Sarvodaya", "Resource-Owning Organisation"; subtitle "Reports the DMC has shared with Sarvodaya." (wording ours).
  - Table **card** (1152 × 240): the column headers "Report", "Generated", "Period", "Status" and **no rows**; one centred grey line in the table body: "No reports have been shared with Sarvodaya." (wording ours).
  - "Open a report by reference" card: field "Report reference" holding "DR-2026-0184"; secondary "Open" button; below it a form-level **error** box (2 px border, error icon), text (wording ours): "This report has not been shared with your organisation." No results are shown.
- Sample data: organisation Sarvodaya; reports = [] (Sarvodaya has no shared report after E5); report reference DR-2026-0184.

## Flow map

Officer (`AnalyticsUI`):
- [WF1 / sidebar "Analytics"] → **S01** : `openAnalytics()` → `showTemplates`, `showClosedEvents`; then `selectEvent("HZ-2026-0142")` → `showCriteria` (steps 1–4).
- S01 → **S01a** : `selectTemplate(templateId)` → `showCriteria` (A2 3a–3b); S01a → S01 : officer edits period and districts (A2 3c, form only).
- S01 → **S02** : "Generate report" `enterCriteria(criteria)` [valid] → `showResults(report)` (steps 5–9).
- S01 → **S01b** : `enterCriteria` [start after end, no metric] → `showValidationErrors` (E1); S01b → S01 : officer corrects (6c) and resubmits.
- S01 → **S01c** : `enterCriteria` [Galle out of scope] → `showOutOfScope` (E4 6d–6e); S01c → **S02** : `confirmAdjustedCriteria()` [valid] → `showResults` (6f); S01c → **S01b** (same display) : `confirmAdjustedCriteria()` [invalid] → `showValidationErrors` (6f, as E1); S01c → S01 : officer changes the criteria (step 5).
- S01 → **S01d** : `enterCriteria` [Galle only] → `showValidationErrors` (E4 6g); S01d → S01 : officer corrects (6c).
- S01 → **S02a** : `enterCriteria` [Kegalle, Landslide, 1–3 May 2026] → `showNoRecords(suggestion)` (A1); S02a → S02 : criteria strip `enterCriteria` with changed criteria (8g → step 5).
- S01 → **S02b** : `enterCriteria` [Kalutara client with pending items] → `showResults` + `showProvisionalNotice` (A3); S02b → S03 : `enterFindings` (8d → step 10).
- S01 → **S02c** : `enterCriteria` [occupancy calculator fails] → `showResults` + `showComputationError` (E3); S02c → S03 : `enterFindings` (7e → step 10).
- S02 → S02 (new report) : criteria strip `enterCriteria(criteria)` → `showResults` (A4).
- S02 → **S03** : "Save findings" `enterFindings(reportId, text)` → `showResults(report)` with findings and the Export/Share panel (steps 10–11).
- S03 → **S04** : officer selects "PDF" and ticks "CARE Sri Lanka" (form only, no operation; steps 12 and 14 choices).
- S04 → **S04a** : "Export" `exportReport(reportId, PDF)` → `showExportFile(file)` (step 13).
- S04 → **S04b** : "Export" `exportReport(reportId, PDF)` [exporter fails] → `showExportError(message)` (E2); S04b → S04a : retry `exportReport` (13d → step 12).
- S04 / S04a → **S04c** : "Share report" `shareReport(reportId, [CARE Sri Lanka])` → `showShared(outcome)` (steps 14–15; A5 with two organisations).
- S04 → **S04d** : "Share report" `shareReport(reportId, [CARE Sri Lanka, Sarvodaya])` → `showShared(outcome)` with `noData` (E5).
- A7: S03 with no export and no share → the use case ends (no arrow).

Organisation (`DonorReportView`, later, own session):
- CARE Sri Lanka: [sidebar "Shared reports"] → **S05** : `openSharedReports(organizationId)` → `showSharedReports(reports)` (steps 16–17).
- S05 → **S06** : row "Open" `openSharedReport("DR-2026-0184", organizationId)` → `showScopedReport(scopedReport)` (steps 18–19). S06 → S05 : "Back to shared reports".
- Sarvodaya (its own session): [sidebar "Shared reports"] `openSharedReports(organizationId)` → `showSharedReports([])`; then "Open a report by reference" `openSharedReport("DR-2026-0184", organizationId)` → `showNotShared()` → **S05a** (E6).

Technical failure on any frame → common banner COMMON-B1 (drawn in WF1 only).

## Gaps

1. **Generation time — RESOLVED as an assumption (P85-5):** 26 May 2026, 09:30 (after the hazard's closure on 25 May and after the A3 client's last synchronisation, 25 May 2026 16:40).
2. **Officer's name.** SC4 does not name the DMC Officer. The header uses "N. Jayasinghe": SC4 Appendix 2 uses the fixed demo user as `officerId`, standards §1 provides "a fixed demo user per role", and SC1 step 1 names that DMC Officer N. Jayasinghe.
3. **Occupancy curve.** SC4 gives only the combined capacity (6,300, 19 shelters), the peak (5,920 on 21 May, 94 %) and the record count (1,140). The chart draws only the capacity line and the labelled peak point; the curve is a light placeholder band labelled "occupancy from the stored records (1,140 records)", with no other vertices and no other y-values (Shell, occupancy chart rule).
4. **Organisation pick-list.** Pick-lists are screen components (MD C2), not displays; the S03/S04 Share card lists the four organisations SC4 names (CARE Sri Lanka, Sri Lanka Red Cross Society, Sri Lanka Army, Sarvodaya) out of the 14 seeded.
5. **Single rows.** SC4 says "among them" for the closed events (step 2) and the shared reports (step 17) but names only HZ-2026-0142 and DR-2026-0184; the seed has one template. Each list shows only the named row.
6. **E3 matched-record count.** SC4 gives none. S02c shows 205,108, marked **(derived)**: 206,248 − 1,140, since each calculator counts its own part and `merge` sums them (FX-17). The caption must say "derived".
7. **Unstated criteria in E1, E4 and A1.** The criteria panel keeps the values already entered (SC4 Conventions, MD C2), so the values these flows do not name are those of the main flow (Ratnapura, Kalutara, Flood, 20–25 May 2026, four metrics).
8. **Organisation navigation.** wireframe-spec §3 lists none for the Resource-Owning Organisation; the frames use one item, "Shared reports" (`openSharedReports`). Confirmed (P85-4; wireframe-spec §10).
9. Resolved by review fix E-1: export and share now sit in the report page's right-hand panel (S03/S04); no navigation-only button remains.
10. **"Open a report by reference" field (S05, S05a)** maps to `DonorReportView.openSharedReport(reportId, organizationId)`; it exists because E6 18a opens the report by its reference.
11. **A5** is not a separate frame; its text is in the S04c note.
12. Dropped (review fix M-2): no escalation marker on the occupancy chart.
13. **Matched-record breakdown.** `ReportResults` holds only `matchedRecordCount`; the frames show the total 206,248; the breakdown (1 warning, 205,102 notifications, 1,140 occupancy records, 5 distribution lines) goes in the caption.
14. **A3 matched-record count — RESOLVED as an assumption (P85-5):** 206,245 = the main flow's 206,248 minus the Kalutara client's 3 pending items, assumed to be occupancy updates.
