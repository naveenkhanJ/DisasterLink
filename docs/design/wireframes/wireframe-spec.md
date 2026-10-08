# Wireframe spec — Assignment 02 improved UIs (WF1–WF4)

**Status:** v0.5, 2026-10-09 (P91: navigation rule amended, §3 and §8 rule 12; P87: §8 rules 9–11 added; P85: design questions resolved, assumptions confirmed; P82) — tool, themes and assets decided; frame-by-frame content in `frames-WF1.md` … `frames-WF4.md` (this file is the index and the rules). Not yet drawn. Sources: change list v2.9 decision 8-2, CH-24, CH-27, CH-30, CH-33; `implementation-standards.md` §1 "Wireframes" and §10; `model-dictionary.md` v0.9 §4 (boundary displays); the five scenarios (SC1, Escalate Warning, SC2, SC3, SC4); `storyboard-corrections.md`; `assignment-02-brief.md` (G8, G11, I8, C7); Group_060's received hi-fi screens, colours measured from the page images in `01 Analysis/critique-verification/img-uc1…uc4` (P80).

## 1. Binding decisions (already agreed)

| Source | Rule |
|---|---|
| Brief C7, G8, I8 | The wireframes in our report are the **received wireframes as adjusted by our proposed UI changes**. The implemented screens must stay consistent with them. |
| Brief G11 | The report also needs **screenshots of the implemented app UIs**, with short descriptions of the flow. These are separate from the wireframes and are taken from the built system. |
| Change list 8-2 (tool amended P82: HTML, not Figma — §2) | **Hi-fi only**, every screen affected by a change. Message and error states are drawn as overlays or variants on their screen. Common frames are reused. Desktop **1440×1024**, phone **390×844**. **Count (amended P88): the 27 screens are drawn, one frame each, in both themes; the 62 states and the common banner are described in the captions, not drawn** (breakdown §5). The tool recorded in 8-2 is **Figma**. |
| Change list 8-2, standards §1 — **amended P82** | **Was:** Group_060's coloured style. **Now (Afham, P81–P82):** the report's hi-fi wireframes are **monochrome**, following Lecture 1's definition (DG §6.2: hi-fi = clean straight lines, typed real labels, black-white-grey; colour belongs to the prototype), as in A01. The report states this in one line. The coloured version in Group_060's style is the **implementation reference** (§2). Colour is never the only signal in either theme. |
| Change list 8-3 | Storyboards are **not redrawn**. Their corrections are written beside each figure (`storyboard-corrections.md`, SB1–SB4). |
| Change list 8-4 | Warning levels use NBRO's wording: **Level 1 – Yellow (Remain Vigilant)**, **Level 2 – Amber (Be Alert)**, **Level 3 – Red (Evacuate)**. |
| CH-24, CH-27, CH-30, CH-33 | Content of WF1–WF4 (§6). Screens without a flow are removed. Sample data is consistent and real, and arrows run between screens. |
| Standards §10 and others | Officer-screen constraints: the **UC1 map** uses Leaflet with a district-boundary GeoJSON layer and only displays the selected area (no drawing mode). **UC4 charts** use Chart.js. **On the phone, the GPS fix is shown as text; the manual location (A1) is chosen from a bundled Sri Lankan place list with a type-ahead dropdown and fine-tuned with a map pin; no coordinate fields** (P206, P213; standards §10). A server or network failure shows the **common error banner** (C1), one component drawn once. |
| MD §4 | Every screen element that shows data realises a named `show…` display of its boundary. Every control realises a named input operation (§4 lists them). Nothing appears that is not in the dictionary. |

## 2. Tool, themes, pipeline and assets (decided P81–P82)

**Tool.** HTML/CSS, rendered to PNG with Playwright and the pre-installed Chromium, at device scale factor 2 (Afham, P82). There is no Figma.

**Two themes from one source.** Every frame is written once and rendered twice. The only difference between the themes is the stylesheet (`theme-wf.css` or `theme-ui.css`):

| Theme | Use | Look |
|---|---|---|
| **wf** — monochrome hi-fi wireframe | the report's corrected hi-fi wireframes (the UI-changes section) | Lecture 1's hi-fi (DG §6.2) and A01's hi-fi frames: black `#111111` text and primary buttons, greys `#F2F2F2` / `#E0E0E0` / `#BDBDBD` / `#757575`, white cards, no colour. Images are a grey box with a crossed outline and a one-word label ("photo"). Map districts are white with grey borders; selected districts are filled grey with a hatch. Charts are grey bars and lines, with a dashed line for capacity. Levels and statuses are written out in full in a bordered tag; a level is never shown by colour. |
| **ui** — coloured implementation reference | the members' visual target and starting HTML/CSS; **not** report screenshots (§9) | Group_060's tokens (§3), with the NBRO level colours. |

**Font.** Manrope in both themes, which continues the received design's typeface. Sinhala text uses Noto Sans Sinhala and Tamil text uses Noto Sans Tamil, as fallbacks in the same `font-family` stack. The files are in `Assignment 02/GeoJSON and Fonts/`, all under the SIL OFL 1.1 (checked P82: `OFL.txt` present in each folder).

**Frames.** Desktop is exactly **1440×1024** and the phone exactly **390×844** (CSS px; the PNGs are 2880×2048 and 780×1688).
- No browser chrome is drawn, so the wireframe and the later implementation screenshot have the same size and can sit side by side.
- The phone has a status bar (time, signal, battery) and, in the wf theme, a 1 px black outline.

**States.** Each of the 62 states is a **full frame of its screen with that state applied**: an inline message, a banner, a modal or a changed panel. It is not a separate fragment. That gives 27 + 62 + 1 = 90 PNGs per theme.

**Flow maps.** CH-24/27/30/33 ask for "arrows between screens". One flow map per wireframe set (WF1, WF1b, WF2 phone, WF2 console, WF3, WF4) shows thumbnails of its screens with labelled arrows (the user action → the display). These are extra figures, not in the 90.

**Captions.** Frames carry no annotations, so they stay clean and are identical in both themes. Each frame's caption, generated into `captions.md`, gives:
- the frame ID and name;
- the scenario step or flow it shows;
- the `show…` display(s) it realises;
- the change rows it answers (CH-nn) and the received screen it corrects.

**Folder layout** (`Assignment 02/02 Group Design/wireframes/`):
- `src/` — HTML, CSS, the frame data and the render script;
- `src/assets/` — fonts (copied), `districts.svg`, icons;
- `out/wf/` and `out/ui/` — the PNGs;
- `captions.md`.

**File names:** `WF1-S01-warning-dashboard.png`, `WF1-S01a-hazard-incomplete.png`, … (the screen ID, then a letter per state).

**Maps** (from `geoBoundaries-LKA-ADM2.geojson`; checked P82):
- **The data.** The file has **25 features, one per district** (`shapeName` "Ratnapura District" …). ADM0 is the country outline and ADM1 the 9 provinces; neither is used.
- **Simplification.** It is simplified once (Douglas–Peucker, tolerance 0.003°; 51,256 → about 3,700 points) into `districts.svg`, with one `<path>` per district (`data-district="Ratnapura"`). The projection is equirectangular, lon 79.5–82.0 and lat 5.85–9.9, and the test render is correct.
- **Where maps appear:**
  - UC1 create form and preview: the selected districts are highlighted, and the map only displays (CH-22, D2-14);
  - UC1 dashboard and hazard details: the hazard's districts;
  - UC2 console report details: the report's districts with a dot at its coordinates (the dot is projected the same way).
- **No maps on the phone.** Location is text plus coordinates (standards §10).
- **No river-basin shapes.** No basin data is available, so the basin is a pick-list beside the district chips and is not drawn.
- **Implementation.** The implementation's Leaflet map (standards §10) uses the **same GeoJSON file**.

**Charts** (UC4):
- **In the frames:** inline SVG drawn from the scenario's figures:
  - alert timeline: two events on a time axis;
  - citizens reached: bars per channel plus the total;
  - occupancy over time: a line against a dashed capacity line;
  - distribution: a table plus bars by district and owner.
- **In the implementation:** Chart.js (standards §10) with the same data.

**Icons, photos and people:**
- Icons are simple line icons drawn in-house as inline SVG (bell, warning triangle, home, chart, shelter, report, sync, offline, camera, pin, check, cross); no external icon set is used.
- Photos are always a placeholder box (wf: grey with a cross; ui: a neutral tinted box with a camera icon); there are no stock photos.
- People are shown as an initials avatar, never a photograph.

**Sinhala and Tamil text.**
- **Where it appears:** wherever the scenario needs a warning in three languages (the UC1 form, preview and dispatch status; Escalate details; the citizen's home warnings in the chosen language), and in WF2's Sinhala decision notices and rejection reason (WF2 texts N-1, N-2, R-1).
- **Who writes it:** Claude drafts it in `frames-WF1.md` with its English source.
- **Check before release:** **a member who reads the language must check it before the report**. This is a release blocker, listed in §10.

**Implementation reuse.**
- **Officer screens:** the ui-theme CSS becomes `backend/src/main/resources/static/css/dmc.css`, and each ui-theme screen's HTML is the starting point for its Thymeleaf template.
- **Phone app:** its tokens go into the phone app's theme file.

**Licence and attribution.**
- **Fonts:** OFL 1.1, credited in the report's figure notes.
- **District boundaries:** geoBoundaries, the "LKA ADM2" dataset (gbOpen, boundary ID `LKA-ADM2-46371173`, build 12 Dec 2023). The licence is not stated inside the GeoJSON file; the release's metadata (recorded P160) names the source "OpenStreetMap, Wambacher" and the licence **ODbL 1.0**: credit "© OpenStreetMap contributors, via geoBoundaries (Runfola et al., 2020)" (implementation standards §1).

### 2.1 Canonical shell, grid and tokens (binding for every frame — P82 package review)

**Desktop shell (1440×1024):**
- **Sidebar.** 240 px wide, full height: the DMC badge and "DMC Sri Lanka" at the top, then the navigation items (§3).
- **Header.** **80 px** high across the remaining 1200 px: the page title (24 px bold), a one-line subtitle (14 px), and at the right the user's initials avatar with name and role.
- **Content area.** It starts at x = 240 and y = 80, with **24 px** padding on every side. That gives a usable area of **1152 × 896 px**.
- **Extra strips.** A page-level strip such as WF3's synchronisation strip (48 px) or the offline banner (48 px) sits at the top of the content area and reduces the usable height by its own height.
- **Grid.** 16 px gaps. Standard two-column split **744 + 16 + 392 = 1152**; three-column **373 + 16 + 374 + 16 + 373 = 1152**. Any other split must also add up to exactly 1152.

**Phone shell (390×844):**
- **Status bar:** 44 px.
- **App header:** 56 px, with the DMC badge and "DMC Sri Lanka". There is no bell or avatar.
- **Tab bar:** 64 px, with Home and Report, and Profile signposted (P91, §8 rule 12).
- **Content area:** **358 × 680 px**, with 16 px side padding.

**Type scale (Manrope):**
- page title 24/32 bold
- section title 18/24 bold
- body 14/20 regular
- label 12/16 semibold
- big number 28/36 bold
- phone body 16/24

**wf theme tokens (in addition to the colours in §2):**
- **Card:** white, 1 px `#BDBDBD` border, radius 12.
- **Primary button:** black fill, white text. **Secondary:** white fill, 1 px black border. **Danger:** white fill, **2 px black border, a cross icon and bold text**. **Disabled:** `#E0E0E0` fill with `#757575` text.
- **Selected chip or option:** 2 px black border, a check icon, bold text.
- **Status / level tag:** 1 px black border with the full text; Level 3 – Red (Evacuate) is the same tag with a 2 px border.
- **Error:** field border 2 px black, the **error icon** and the message in bold below the field. A form-level error box is a white card with a 2 px border, the error icon and a list.
- **Notice:** for information, warning, offline or provisional notices: `#F2F2F2` fill, 1 px border, the matching icon.
- **Modal:** a centred white card, 2 px border, over a 40 % black scrim.
- **Map:** districts white with `#BDBDBD` 0.8 px borders; selected districts `#BDBDBD` fill with a 45° hatch; a report location is a black dot of 6 px radius.
- **Charts:** bars `#757575`; lines 2 px black; capacity 1.5 px black dashed; axis and labels `#757575`.

**Icon set** (24 px line icons, 1.75 px stroke, drawn in-house as inline SVG): bell, warning-triangle (alerts and notices), error (circle with "!"), info, check, cross, home, chart, shelter, report (document), file, sync, offline (cloud with slash), wifi, signal, battery, camera, camera-off, pin, clock, lock, chevron-right, plus (circled), user, hazard (cloud with lightning; Hazards navigation item, P98).
- **Hazard types:** flood (waves), landslide (sliding slope), cyclone (spiral), tsunami (large wave), drought (cracked ground), road blocked (road with barrier), other (question mark).
- **Rules:** field and form errors always use **error**; warning-triangle is used for alerts, notices and the "Warnings" navigation item. No other icons are used.

**Text formats:**
- Dates as "20 May 2026"; date-times as "20 May 2026, 10:45" (a comma before the time); times alone as "10:45".
- Numbers with thousands separators (42,335).
- Quoted scenario messages keep their own wording.

**IDs and file names:**
- A frame's ID is the one used in its frames file:
  - WF1-Snn: UC1, spec §5 S1.1–S1.5.
  - WF1b-Snn: Escalate, S1.6–S1.9.
  - WF2P-Snn: phone, S2.1–S2.6.
  - WF2C-Snn: console, S2.7–S2.8.
  - WF3-Snn: S3.x.
  - WF4-Snn: S4.x.
  - COMMON-B1: the banner.
- States add a letter.
- The file name is `<ID>-<slug>.png`, where the slug is the frame heading lower-cased with spaces as hyphens, after dropping punctuation and the part after "—".

## 3. Style tokens of the ui theme (measured from Group_060's screens; the wf theme is in §2)

Colours were measured as the dominant pixel colour of each element (P80) and rounded to the matching Tailwind value. Every measured value fell on Tailwind's slate, red, amber, emerald and blue scales, so Group_060 almost certainly used them.

| Token | Value | Where Group_060 uses it |
|---|---|---|
| sidebar | `#0F172A` (slate-900) | left navigation, 240 px wide |
| sidebar-active | `#1E293B` (slate-800) | selected nav item (rounded 8 px) |
| page-bg | `#F8FAFC` (slate-50) | page and phone background |
| card | `#FFFFFF`, border `#E5E7EB` (gray-200), radius ≈ 12 px | every content card |
| text | `#0F172A`; muted `#64748B` (slate-500) | titles; subtitles and labels |
| primary action | `#FF0000` (CTA, white bold text) | "Issue New Hazard Warning →" |
| dark button | `#0F172A` / `#111827` | "Verify Report", "Open New Shelter" |
| danger | `#EF4444` (red-500) | Reject button, badges, phone primary button (`#EF4444`) |
| red tint | bg `#FEE2E2` (red-100), text `#DC2626` | red alert cards, "Critical" metric card |
| amber | `#F59E0B` (amber-500); tint `#FEF3C7` (amber-100) | Amber badges, "Pending review" card, DMC logo badge, progress bar |
| green | `#10B981` (emerald-500); tint `#D1FAE5` | "Low", shelter dots, success |
| blue | `#3B82F6` (blue-500); tint `#E0ECFF` | info pills, links, "Teams" dot |
| map fill | `#1E88AC` (teal-blue) with white district borders | district map |
| font | geometric sans, visually Manrope (**not confirmed**; it is an inference from the glyphs) | all text |
| shell | sidebar with the orange **DMC** badge and "DMC Sri Lanka"; a header with the page title, a one-line subtitle and the signed-in user (name, role) at the right; cards on a 24 px grid | every desktop screen |
| phone | iOS status bar; header with the DMC badge and a bell; content on `#F8FAFC`; full-width red primary button; bottom tab bar (Home, Alerts, Report, …) | every phone screen |

**Warning-level colours, ui theme only (8-4, replacing Group_060's scales; in the wf theme a level is its written label in a bordered tag):** Level 1 Yellow `#FACC15` on a `#FEF9C3` tint; Level 2 Amber `#F59E0B` on `#FEF3C7`; Level 3 Red `#DC2626` on `#FEE2E2`. The label is always written in full (e.g. "Level 3 – Red (Evacuate)").

**Our adjustments to the style (assumptions to confirm):**

- **Navigation (amended P91; was P85-4 "only the items with a use case").** Navigation shows every item whose use case is in our use case diagram for that role, and nothing else. Items whose use case is implemented work; items whose use case is designed but not implemented are **signposted**: shown muted with a small "Not in this version" tag, and a click shows the notice "<item> — not available in this version" (§8 rule 12). Reason: the assignment asks each member to implement one use case in depth rather than a broader scope (brief lines 71, 73).
  - DMC Officer: Warnings, Analytics; signposted Hazards (Record and Monitor Hazard), Operational picture (View Combined Operational Picture).
  - DMC Duty Officer (inherits the DMC Officer's use cases, CH-04): Verification, Warnings, Analytics; signposted Hazards, Operational picture.
  - District Officer: Shelters, for one district; signposted Rescue teams (Dispatch Rescue Team, Track Rescue Team Status), Relief resources (Manage Relief Resources, Record Resource Distribution).
  - Resource-Owning Organisation: Shared reports; signposted Resources (Register Resources), Donations (Donate Relief Supplies).
  - Citizen phone tab bar: Home, Report; signposted Profile (Register).
  - Dropped, no use case: Group_060's Dashboard, Settings, Map View, Communications, the Map tab and the Alerts tab (active warnings are on Home). Coordination, Rescue Dispatch, Supply Chain and Live Operations return under the names above.
- **No log-in screen**, which is not modelled. The header shows the seeded demo user.
- **Map as text on the phone.** The phone's location is shown as text and coordinates, not as Group_060's map image (standards §10).

## 4. Users, devices and frames

| User (actor) | Device | Boundary | Use cases |
|---|---|---|---|
| DMC Officer | desktop 1440×1024 | WarningDashboardUI, EscalationUI, AnalyticsUI | UC1, Escalate Warning, UC4 |
| DMC Duty Officer | desktop | VerificationConsoleUI (+ EscalationUI) | UC2 verification |
| District Officer (one district) | desktop browser, may go offline | ShelterManagementUI | UC3 |
| Resource-Owning Organisation | desktop | DonorReportView | UC4 steps 16–19 |
| Citizen / Community Volunteer | phone 390×844 | CitizenAppUI | UC2 report, warnings on the home screen |

## 5. Frame inventory — 27 screens, 62 states, 1 banner (decision 8-2; display names from MD §4) — P88: only the screens are drawn; states and banner are caption text

**WF1 — UC1 Issue Hazard Warning (SC1): 5 screens + 8 states**
- **Screens:**
  - S1.1 warning dashboard, the assessable hazards (`showHazards`).
  - S1.2 hazard details with the update panel (`showHazardDetails`).
  - S1.3 Create Hazard Warning form (`showWarningForm`).
  - S1.4 preview (`showPreview`).
  - S1.5 dispatch status (`showActiveWarning`).
- **States:**
  - Hazard information incomplete (A1, `showHazardIncomplete`).
  - Hazard-update errors (E5, `showValidationErrors`).
  - Hazard with an active warning offering "Escalate warning" (A5, step 19).
  - Affected area required (A2).
  - Validation errors with the counters (E3).
  - Delivery-failure alert (E1, `showDeliveryFailure`).
  - Dispatch status after the SMS resend (E1).
  - Active warning already exists (E6, `showActiveWarningExists`).

**WF1b — Escalate Warning: 4 screens + 4 states.** The dashboard and hazard details are reused from UC1, and the escalation prompt is counted in UC2.
- **Screens:**
  - S1.6 escalation options (`showEscalationOptions`).
  - S1.7 escalation details form, pre-filled, with counters.
  - S1.8 re-broadcast preview (`showEscalationPreview`).
  - S1.9 escalated warning with the broadcast summary (`showEscalated`).
- **States:**
  - No higher level (A3, `showNoHigherLevel`).
  - Not escalatable (E2, `showNotEscalatable`).
  - Validation errors (E3).
  - SMS-failure alert (E1, `showDeliveryFailure`).

**WF2 — UC2 phone (SC2): 6 screens + 15 states**
- **Screens:**
  - S2.1 home: language selector, warnings, My reports and waiting drafts, location, connectivity (`showHome`).
  - S2.2 hazard-type choice.
  - S2.3 report form: description, photo, location as text.
  - S2.4 review (`showReview`).
  - S2.5 confirmation (`showSubmissionResult`).
  - S2.6 report status (`showReportStatus`).
- **States:**
  - Home offline, from the saved copy, with the selector disabled (A8).
  - GPS unavailable with manual entry (A1, `showLocationUnavailable`).
  - Location error (E3).
  - Photo error (E2, `showPhotoError`).
  - Camera permission (E2).
  - Form validation errors (E1).
  - Confirmation "Pending synchronisation" (A2).
  - Synchronisation result "Report sent" (A2, `showSyncResults`).
  - Confirmation "not accepted" (E6).
  - Synchronisation result "Report not accepted", with the rejected draft in the waiting list (E8).
  - Rejected draft reopened on the form with its reason (E8).
  - Rejected draft discarded (E8).
  - Decision notice — verified (step 30, `showReportDecision`).
  - Decision notice — rejected, with the reason (A4).
  - Report status Rejected, with the reason (A4).
- The warning alert itself (`showAlert`) is shown on the home screen (UC1 step 15).

**WF2 — UC2 verification console: 2 screens + 8 states**
- **Screens:**
  - S2.7 verification queue (`showPendingReports`).
  - S2.8 report details: evidence, nearby reports with distance, candidate hazards, decisions (`showReportDetails`).
- **States:**
  - Rejection reason and confirmation (A4).
  - Reason required (E7, `showReasonRequired`).
  - New-hazard details (A6).
  - New-hazard validation errors (E9).
  - "Report verified" (step 28, `showDecisionRecorded`).
  - "Report rejected" (A4).
  - Escalation prompt (A7, `showEscalationPrompt`).
  - Report already decided (E10, `showAlreadyDecided`).

**WF3 — UC3 Manage Emergency Shelters (SC3): 4 screens + 15 states**
- **Screens:**
  - S3.1 shelter registry for one district, with pending items and their count (`showShelters`, `showPending`).
  - S3.2 shelter details with the occupancy update (`showShelterDetails`).
  - S3.3 edit-details form.
  - S3.4 registration form.
- **States:**
  - Occupancy updated (step 9).
  - Shelter full (A2).
  - Alternatives found (A1).
  - No shelter with space (A1, `showNoShelterAvailable`).
  - Details updated (A4).
  - Shelter registered (A5).
  - Offline banner (A6, `showConnectivity`).
  - Saved offline – pending sync, with the count (A6, `showSavedOffline`).
  - Pending items online with "Sync now" (A6).
  - Synchronisation results with the rejected item (A6, `showSyncResults`).
  - Rejected item dismissed (A6).
  - Capacity exceeded (E1, `showCapacityExceeded`).
  - Shelter-details errors (E2).
  - Invalid occupancy (E3, `showInvalidOccupancy`).
  - Capacity below occupancy (E4).

**WF4 — UC4 Analyse Disaster Response & Generate Reports (SC4): 6 screens + 12 states**
- **Screens:**
  - S4.1 criteria panel with templates and closed events (`showTemplates`, `showClosedEvents`, `showCriteria`).
  - S4.2 results with a chart per metric (`showResults`).
  - S4.3 report with findings.
  - S4.4 export and share.
  - S4.5 the organisation's shared-reports list (`showSharedReports`).
  - S4.6 the organisation's scoped report (`showScopedReport`).
- **States:**
  - Criteria from a template (A2).
  - Criteria errors (E1).
  - Out-of-scope district with adjusted criteria (E4, `showOutOfScope`).
  - No selected district in scope (E4).
  - No records, with a suggestion (A1, `showNoRecords`).
  - Provisional notice with each district's last synchronisation time (A3, `showProvisionalNotice`).
  - Computation error (E3, `showComputationError`).
  - Export file provided (step 13, `showExportFile`).
  - Export error (E2, `showExportError`).
  - Sharing outcome (step 15, A5, `showShared`).
  - "No data — not shared" (E5).
  - Report not shared (E6, `showNotShared`).

**Common:** the service-unavailable error banner (MD convention C1), drawn once.

**Frame-by-frame content (v2, after independent review, 2026-10-09):** every screen, state and flow map is specified region by region, with exact sample data, controls (MD input operations) and displays, in four files. Developers and the renderer build from these:

| File | Set | Screens | States |
|---|---|---|---|
| `frames-WF1.md` | WF1 UC1 + WF1b Escalate Warning + COMMON-B1 banner | 5 + 4 | 8 + 4 (+1 banner) |
| `frames-WF2.md` | WF2 UC2 phone (WF2P) + console (WF2C) | 6 + 2 | 15 + 8 |
| `frames-WF3.md` | WF3 UC3 | 4 | 15 |
| `frames-WF4.md` | WF4 UC4 | 6 | 12 |
| **Total** | | **27** | **62 + 1 = 63 → 90 frames** |

The three-language warning texts are defined once, in `frames-WF1.md`'s table "Texts in three languages (final drafts)", and copied verbatim into WF2. Per-language counters show the actual character count of the drawn text (≤ 201 for Sinhala and Tamil). The Tamil Level 2 and Level 3 texts are condensed to fit; what was dropped is listed in WF1 Gap 6.

## 6. Content rules per wireframe set (from the CH rows; the full text is in `change-list.md`)

- **WF1 (CH-24).** Every screen is 1440×1024.
  - **Flow:** dashboard of assessable hazards → hazard details with update → create form → complete preview → dispatch status, with the failure alert and the resend.
  - **Create form:**
    - Level on the NBRO scale.
    - Area chosen with district chips plus the river basin; the map only displays the selected area and has no "Geographical Boundary" mode.
    - Validity window.
    - Channels Push, SMS and Audible alert only; Group_060's TV/Radio and siren channels are removed.
    - Message and instructions in Sinhala, Tamil and English, each with its own counter.
  - **Preview:** type, level, covered districts, message, instructions, channels, the expiry time, and a **recipient summary** (citizens per district and organisations).
  - **Dashboard:** "Escalate" is offered on the hazard details when the hazard has an active warning.
  - **Removed:**
    - The citizen alert preview's "LOCATE NEAREST SHELTER" button. Instead, the officer's instructions name the safety centre.
    - Every screen that has no flow.
  - **Hazard label:** "type — locationName" (FX-14).
- **WF2 (CH-27).**
  - **Phone (390×844):**
    - **Home:** the language selector, warnings in the chosen language, My reports, the current location and the connectivity status (offline is driven by `HomeView.fromCache`).
    - **Report form:** the hazard types, one required photo, and the location from GPS or entered manually. The location's accuracy is shown and confirmed. A "GPS location is unavailable" notice offers manual entry.
    - Its error messages.
    - A **review-and-edit** screen.
    - A confirmation with separate online and "Pending synchronisation" states.
    - A **report-status** screen showing the decision and the reason.
  - **Desktop:**
    - **Queue:** statuses from CH-21, with no row-level verify or reject.
    - **Details:** the hazard type, the nearby reports, a mandatory rejection reason, and verify → link to a hazard or create one.
    - The escalation prompt, which hands over to Escalate Warning.
  - **Removed:**
    - "Estimated Households", "Alternative Shelters", "Preparing instant SMS siren dispatch", "Assigned to Capt. K. Perera" and "Submit Another Observation".
    - The Contacts and Shelters quick links.
  - **Labels:** the app's own labels stay in English. Only the warnings follow the chosen language (CH-25).
- **WF3 (CH-30).** Every screen is 1440×1024, for the District Officer of one district.
  - **Flow:** registry → details and availability (with edit details) → update occupancy, using a "New total occupancy" field with the remaining capacity shown → the full and over-capacity messages and the alternatives → register form.
  - **Offline:** an **offline banner**, a "saved offline – pending sync" state, the pending count and the sync results, plus "Sync now" and dismissing a rejected item.
  - **Removed:** no amenities, no "open new shelter" ambiguity.
  - **Statuses:** from CH-21.
- **WF4 (CH-33).** Every screen is 1440×1024. **Metrics come from the scenario only; the layout comes from the storyboard.**
  - **Criteria:** template, pre-fill from an event, districts, type, period, metrics, and the out-of-scope message.
  - **Results:** the alert timeline with the issue and the escalation, citizens reached per channel and in total, occupancy over time against the combined capacity, and distribution by district **and by owner**, including "Private individual". The provisional notice appears here.
  - **Then:** findings → export and share, with a scoped view per organisation.
  - **Organisation:** its list of shared reports and its scoped report.
  - **Removed:** standing dashboards and unsupported data.

## 7. Sample data (one storyline; exact values are in each scenario's main flow and Appendix 2)

The Kalu Ganga flood: hazard **HZ-2026-0142**, Flood, Ratnapura and Kalutara, from 20 May 2026 09:30, closed 25 May 2026.

- **Warning WRN-2026-0087:** issued at Level 2 – Amber on 20 May 2026 at 10:45 (SC1), escalated to Level 3 – Red on 21 May 2026 at 08:25 (Escalate Warning).
- **Citizens reached:** 42,335 (Push 30,100; SMS 42,335; Audible alert 30,100).
- **Shelters:** 19 shelters with 6,300 places; the peak was 5,920 occupants on 21 May.
- **Report:** DR-2026-0184, shared with CARE Sri Lanka.
- **SC2 and SC3:** their reports, shelters and officers come from their own main flows, and they share this storyline.

Every figure on a frame is taken from a scenario, never invented.

## 8. HCI rules for every frame

1. **Level and status:** every level and status is written in full in a bordered tag (wf theme); in the ui theme it also carries its colour.
2. **Main action:** one primary action per screen, with large touch targets on the phone (at least 44 px).
3. **Offline:** the offline or queued state is always visible, on the phone and on the UC3 page.
4. **Confirmation:** a submission or decision is always confirmed with its outcome.
5. **Typing:** kept to a minimum — pick-lists, chips, and an automatic location and time.
6. **Error messages:** each one names the field and the fix, and the form keeps the values entered.
7. **Languages:** three-language input wherever the scenario requires it (warnings); a language selector on the citizen's home screen.
8. **Annotations:** each frame carries the scenario step or flow it shows and the boundary display it realises, either in a caption under the frame or on the flow-map arrows.
9. **Level colour wins over selection colour (P87):** a selected warning-level tile takes its own level's colours (ui: Level 2 amber, not the generic red "selected" token, which would read as Level 3); every level tile also shows a small swatch of its colour (ui only). In wf the selected tile keeps the 2 px border and check mark.
10. **Selectable rows (P87):** a table row that opens something ends with a chevron-right icon in an unlabelled last column, so the row reads as clickable; the row stays the single target.
11. **Unknown values (P87):** a value the scenario does not give is shown as "—", never as an empty box.
12. **Signposted items (P91):** a navigation item whose use case is not implemented is drawn muted (muted text, no hover fill) with a small tag "Not in this version"; a click shows a notice at the top of the content area: "<item> — not available in this version." It never opens an empty page.

## 9. Real UI screenshots for the report (FAQ, G11) — separate from both themes

- The FAQ asks for "the screenshots of the implemented app UIs with short descriptions explaining the flow of the application". These are taken from the **running system**, never from the ui-theme mock-ups, because the demo must match the repository. Mock-ups shown as screenshots would misrepresent the work (P81).
- **How many** (Afham, P81): enough to show each use case's flow, not every screen. That is about 3–5 per use case: the main flow plus one key alternative (for example offline in UC2 and UC3, or no records in UC4).
- **Capture:**
  - officer screens with Playwright at 1440×1024;
  - phone screens from the Android emulator.
- **Captions:** each screenshot gets a one-line flow description and names the wireframe frame it implements, which is the visible evidence that the UIs are consistent with the wireframes (I8).
- **Prerequisite:** the skeleton and the implementations, not yet started (P79).

## 10. Release blockers and open items

1. **Sinhala and Tamil text:** every Sinhala and Tamil text (WF1's three-language table, including the condensed Tamil, and WF2's N-1, N-2 and R-1) checked by a member who reads the language.
2. **geoBoundaries licence:** recorded P160 — ODbL 1.0, source OpenStreetMap (from the release metadata; implementation standards §1).
3. **Manrope in the wf theme:** an assumption (P82); Afham can switch the font in one line.
4. **Assumptions — confirmed (P85-4):**
   - navigation items per role (§3) and no log-in screen;
   - the phone map as text;
   - the phone tabs Home and Report only, with no bell or avatar;
   - an organisation navigation item "Shared reports";
   - the nearby reports' reporter type "Citizen";
   - Manrope in both themes.
5. **Design questions — RESOLVED (P85, change list §20):**
   - (a) UC3 remaining capacity: MD v0.10 — the five UC3 confirmation displays take `ShelterView` (P85-1).
   - (b) UC2 place names: the frames and SC2 show the district (P85-2).
   - (c) The agency's name: the frames, SC1 and SC2 show "agency bulletin" (P85-3).
6. **Values the scenarios did not give — set as assumptions (P85-5):**
   - the UC4 generation time is 26 May 2026, 09:30;
   - the A3 count is 206,245;
   - the E3 count is 205,108 (derived);
   - the Piliyandala entry time is 14:20;
   - report RPT-2026-KLT-0852's unknown fields stay blank.

**Still open (Afham, not blocking the drawing):** item 1 (the native check of the Sinhala and Tamil text); item 2 closed P160.
