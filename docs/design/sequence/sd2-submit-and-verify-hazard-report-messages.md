### Message script — sd Submit and Verify Hazard Report

Lifelines, left to right (draw the stereotype icon and underline the head):

- **CIT** — actor *Citizen*
- **DUTY** — actor *DMC Duty Officer*
- **APP** — «boundary» `: CitizenAppUI`
- **VC** — «boundary» `: VerificationConsoleUI`
- **RCC** — «control» `: ReportCaptureController`
- **RSC** — «control» `: ReportSubmissionController`
- **RVC** — «control» `: ReportVerificationController`
- **WC** — «control» `: WarningController`
- **CITE** — «entity» `reporter : Citizen`
- **HR** — «entity» `report : HazardReport`
- **NH** — «entity» `newHazard : Hazard`
- **CSS** — «entity» `: ClientSyncStatus`
- **ORS** — «entity» `: OfflineReportStore`
- **LOC** — «boundary» `: LocationService`
- **PNS** — actor *Push Notification Service*

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* objects are loaded by their identifiers (persistence is not drawn); a server or network failure on the console is shown on its common error banner (not drawn) | note |
| 1 | CIT | APP | `openHome()` | synchronous (filled arrowhead) |
| 1.1 | APP | RCC | `loadHome()` | synchronous (filled arrowhead) |
| | | | **alt [the DMC server is reachable]** | fragment |
| 1.1.1 | RCC | RSC | `getReportsByReporter(citizenId)` | synchronous (filled arrowhead) |
| — | RSC | RCC | reports | return (dashed, open arrowhead) |
| 1.1.2 | RCC | WC | `getActiveWarnings(district)` | synchronous (filled arrowhead) |
| — | WC | RCC | warnings | return (dashed, open arrowhead) |
| 1.1.3 | RCC | ORS | `findPending()` | synchronous (filled arrowhead) |
| — | ORS | RCC | drafts | return (dashed, open arrowhead) |
| | | | **else [the DMC server cannot be reached]** | fragment |
| 1.1.4 | RCC | ORS | `findPending()` | synchronous (filled arrowhead) |
| — | ORS | RCC | drafts | return (dashed, open arrowhead) |
| | | | *note:* the home screen saved on the phone is used, marked Offline; the language cannot be changed | note |
| | | | **end** | fragment |
| — | RCC | APP | view | return (dashed, open arrowhead) |
| 1.2 | APP | APP | `showHome(view)` | synchronous (filled arrowhead) |
| 1.3 | APP | RCC | `captureLocation()` | synchronous (filled arrowhead) |
| 1.3.1 | RCC | LOC | `getCurrentLocation()` | synchronous (filled arrowhead) |
| — | LOC | RCC | location | return (dashed, open arrowhead) |
| — | RCC | APP | location | return (dashed, open arrowhead) |
| 1.4 | APP | APP | `showLocation(location)` | synchronous (filled arrowhead) |
| | | | **opt [the home screen is online and the citizen changes the language]** | fragment |
| 2 | CIT | APP | `selectLanguage(language)` | synchronous (filled arrowhead) |
| 2.1 | APP | RCC | `changeLanguage(language)` | synchronous (filled arrowhead) |
| 2.1.1 | RCC | RSC | `changePreferredLanguage(citizenId, language)` | synchronous (filled arrowhead) |
| 2.1.1.1 | RSC | CITE | `changePreferredLanguage(language)` | synchronous (filled arrowhead) |
| 2.2 | APP | RCC | `loadHome()` | synchronous (filled arrowhead) |
| — | RCC | APP | view | return (dashed, open arrowhead) |
| 2.3 | APP | APP | `showHome(view)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| 3 | CIT | APP | `startReport()` | synchronous (filled arrowhead) |
| 3.1 | APP | RCC | `startReport()` | synchronous (filled arrowhead) |
| — | RCC | APP | draft | return (dashed, open arrowhead) |
| 4 | CIT | APP | `selectHazardType(type)` | synchronous (filled arrowhead) |
| 5 | CIT | APP | `enterDescription(text)` | synchronous (filled arrowhead) |
| 6 | CIT | APP | `attachPhoto(photoUri)` | synchronous (filled arrowhead) |
| 6.1 | APP | RCC | `validateDraft(draft)` | synchronous (filled arrowhead) |
| — | RCC | APP | result | return (dashed, open arrowhead) |
| | | | **opt [the photo is not a readable JPEG or PNG image]** | fragment |
| 6.2 | APP | APP | `showPhotoError(message)` | synchronous (filled arrowhead) |
| | | | *note:* no photo is attached; the citizen takes or chooses another photo (message 6). If camera access is refused, the app screen asks for it (not drawn) | note |
| | | | **end** | fragment |
| | CIT … LOC | | **ref** «include» Capture GPS Location | fragment |
| 7 | CIT | APP | `reviewReport()` | synchronous (filled arrowhead) |
| 7.1 | APP | RCC | `validateDraft(draft)` | synchronous (filled arrowhead) |
| — | RCC | APP | result | return (dashed, open arrowhead) |
| | | | **alt [the draft is valid]** | fragment |
| 7.2 | APP | APP | `showReview(draft)` | synchronous (filled arrowhead) |
| | | | **else [information missing or invalid]** | fragment |
| 7.3 | APP | APP | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the citizen corrects the fields and reviews again (message 7) | note |
| | | | **end** | fragment |
| | | | **opt [the citizen edits the report]** | fragment |
| 8 | CIT | APP | `editReport()` | synchronous (filled arrowhead) |
| | | | *note:* the report form returns with every value kept; the citizen reviews again (message 7) | note |
| | | | **end** | fragment |
| 9 | CIT | APP | `submitReport()` | synchronous (filled arrowhead) |
| 9.1 | APP | RCC | `submit(draft)` | synchronous (filled arrowhead) |
| 9.1.1 | RCC | LOC | `resolveDistrict(location)` | synchronous (filled arrowhead) |
| — | LOC | RCC | district | return (dashed, open arrowhead) |
| | | | **alt [the phone is online and the server replies]** | fragment |
| 9.1.2 | RCC | RSC | `receiveReport(draft, photo, meta)` | synchronous (filled arrowhead) |
| 9.1.2.1 | RSC | RSC | `validateReport(draft, photo)` | synchronous (filled arrowhead) |
| — | RSC | RSC | result | return (dashed, open arrowhead) |
| | | | **alt [the report is valid]** | fragment |
| 9.1.2.2 | RSC | HR | `HazardReport(reporter, draft, photo, receivedAt)` | «create» |
| 9.1.2.3 | RSC | CSS | `recordSync(at, pendingCount)` | synchronous (filled arrowhead) |
| — | RSC | RCC | receipt | return (dashed, open arrowhead) |
| | | | **else [the report is not valid]** | fragment |
| — | RSC | RCC | receipt | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | RCC | APP | result | return (dashed, open arrowhead) |
| 9.2 | APP | APP | `showSubmissionResult(result)` | synchronous (filled arrowhead) |
| | | | **opt [the report was not accepted]** | fragment |
| | | | *note:* the citizen corrects the report at the review screen (message 7) | note |
| | | | **end** | fragment |
| | | | **else [no connection, or no reply from the server — extension point: network unavailable]** | fragment |
| | CIT … LOC | | **ref** «extend» Save Data Offline | fragment |
| | | | **end** | fragment |
| 10 | DUTY | VC | `openQueue()` | synchronous (filled arrowhead) |
| 10.1 | VC | RVC | `getPendingReports()` | synchronous (filled arrowhead) |
| — | RVC | VC | reports | return (dashed, open arrowhead) |
| 10.2 | VC | VC | `showPendingReports(reports)` | synchronous (filled arrowhead) |
| 11 | DUTY | VC | `selectReport(reportId)` | synchronous (filled arrowhead) |
| 11.1 | VC | RVC | `getReportDetails(reportId)` | synchronous (filled arrowhead) |
| — | RVC | VC | report | return (dashed, open arrowhead) |
| 11.2 | VC | RVC | `findNearbyReports(reportId)` | synchronous (filled arrowhead) |
| — | RVC | VC | nearbyReports | return (dashed, open arrowhead) |
| 11.3 | VC | RVC | `findCandidateHazards(reportId)` | synchronous (filled arrowhead) |
| — | RVC | VC | candidateHazards | return (dashed, open arrowhead) |
| 11.4 | VC | VC | `showReportDetails(report, nearbyReports, candidateHazards)` | synchronous (filled arrowhead) |
| | | | **alt [the report is valid and a candidate hazard fits]** | fragment |
| 12 | DUTY | VC | `verifyReport(reportId, hazardId)` | synchronous (filled arrowhead) |
| 12.1 | VC | RVC | `verifyReport(reportId, hazardId, officerId)` | synchronous (filled arrowhead) |
| 12.1.1 | RVC | HR | `isPending()` | synchronous (filled arrowhead) |
| — | HR | RVC | pending | return (dashed, open arrowhead) |
| | | | **alt [the report is still pending]** | fragment |
| 12.1.2 | RVC | HR | `verify(officer, hazard, at)` | synchronous (filled arrowhead) |
| 12.1.3 | RVC | WC | `getActiveWarningForHazard(hazardId)` | synchronous (filled arrowhead) |
| — | WC | RVC | activeWarning | return (dashed, open arrowhead) |
| | RVC … CITE | | **ref** Send Decision Notice | fragment |
| — | RVC | VC | outcome | return (dashed, open arrowhead) |
| 12.2 | VC | VC | `showDecisionRecorded(report)` | synchronous (filled arrowhead) |
| | | | **opt [the linked hazard has an active warning — extension point: verified report affects an active hazard]** | fragment |
| 12.3 | VC | VC | `showEscalationPrompt(hazard, activeWarning)` | synchronous (filled arrowhead) |
| | | | **alt [the officer chooses Escalate]** | fragment |
| | DUTY … VC | | **ref** «extend» Escalate Warning | fragment |
| | | | **else [the officer chooses Not now]** | fragment |
| 13 | DUTY | VC | `dismissEscalation()` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | **else [another decision was recorded first]** | fragment |
| — | RVC | VC | ReportAlreadyDecidedException(report) | return (dashed, open arrowhead) |
| 12.4 | VC | VC | `showAlreadyDecided(report)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is changed and no notice is sent; the officer returns to the queue (message 10) | note |
| | | | **end** | fragment |
| | | | **else [the report is valid and no candidate hazard fits]** | fragment |
| 14 | DUTY | VC | `verifyAsNewHazard(reportId, details)` | synchronous (filled arrowhead) |
| 14.1 | VC | RVC | `verifyAsNewHazard(reportId, details, officerId)` | synchronous (filled arrowhead) |
| 14.1.1 | RVC | HR | `isPending()` | synchronous (filled arrowhead) |
| — | HR | RVC | pending | return (dashed, open arrowhead) |
| | | | **alt [the report is still pending and the details are complete]** | fragment |
| 14.1.2 | RVC | NH | `Hazard(report, details)` | «create» |
| 14.1.3 | RVC | HR | `verify(officer, hazard, at)` | synchronous (filled arrowhead) |
| 14.1.4 | RVC | WC | `getActiveWarningForHazard(hazardId)` | synchronous (filled arrowhead) |
| — | WC | RVC | none | return (dashed, open arrowhead) |
| | RVC … CITE | | **ref** Send Decision Notice | fragment |
| — | RVC | VC | outcome | return (dashed, open arrowhead) |
| 14.2 | VC | VC | `showDecisionRecorded(report)` | synchronous (filled arrowhead) |
| | | | **else [the report is still pending and the districts, severity or place name are missing]** | fragment |
| — | RVC | VC | InvalidHazardDetailsException(errors) | return (dashed, open arrowhead) |
| 14.3 | VC | VC | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is created or recorded; the officer completes the details and tries again (message 14) | note |
| | | | **else [another decision was recorded first]** | fragment |
| — | RVC | VC | ReportAlreadyDecidedException(report) | return (dashed, open arrowhead) |
| 14.4 | VC | VC | `showAlreadyDecided(report)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **else [the report is invalid, unreliable or a duplicate — extension point: report rejected]** | fragment |
| | DUTY … HR | | **ref** «extend» Reject Report | fragment |
| | | | **else [the officer needs further review]** | fragment |
| | | | *note:* the report stays Pending verification; the officer returns to the queue (message 10) and selects it again later (message 11) | note |
| | | | **end** | fragment |
| | | | **opt [a decision was recorded and the push notice was delivered]** | fragment |
| 15 | PNS | APP | `receiveReportDecision(notice)` | synchronous (filled arrowhead) |
| 15.1 | APP | APP | `showReportDecision(notice)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | *note:* a notice sent by SMS is read in the phone's messaging app | note |
| 16 | CIT | APP | `openReportStatus(reportId)` | synchronous (filled arrowhead) |
| 16.1 | APP | RCC | `getReportStatus(reportId)` | synchronous (filled arrowhead) |
| 16.1.1 | RCC | RSC | `getReportStatus(reportId)` | synchronous (filled arrowhead) |
| — | RSC | RCC | report | return (dashed, open arrowhead) |
| — | RCC | APP | report | return (dashed, open arrowhead) |
| 16.2 | APP | APP | `showReportStatus(report)` | synchronous (filled arrowhead) |
