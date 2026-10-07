### Message script — sd Escalate Warning

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OFF** — actor *DMC Officer*
- **DASH** — «boundary» `: WarningDashboardUI`
- **EUI** — «boundary» `: EscalationUI`
- **WC** — «control» `: WarningController`
- **EC** — «control» `: EscalationController`
- **WB** — «control» `: WarningBroadcaster`
- **WARN** — «entity» `warning : Warning`
- **AR** — «entity» `: AuditRecord`
- **APP** — «boundary» `: CitizenAppUI`
- **PNS** — actor *Push Notification Service*

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* a DMC Duty Officer escalates as a DMC Officer; objects are loaded by their identifiers (persistence is not drawn); a server or network failure is shown on the screen's common error banner and the officer confirms again later (not drawn) | note |
| | | | **opt [started from the warning dashboard]** | fragment |
| 1 | OFF | DASH | `openDashboard()` | synchronous (filled arrowhead) |
| 1.1 | DASH | WC | `getAssessableHazards()` | synchronous (filled arrowhead) |
| — | WC | DASH | hazards | return (dashed, open arrowhead) |
| 1.2 | DASH | DASH | `showHazards(hazards)` | synchronous (filled arrowhead) |
| 2 | OFF | DASH | `selectHazard(hazardId)` | synchronous (filled arrowhead) |
| 2.1 | DASH | WC | `getHazardDetails(hazardId)` | synchronous (filled arrowhead) |
| — | WC | DASH | view | return (dashed, open arrowhead) |
| | | | *note:* the hazard view carries the hazard's active warning (warning, warningId) | note |
| 2.2 | DASH | WC | `getBroadcastSummary(warningId)` | synchronous (filled arrowhead) |
| — | WC | DASH | summary | return (dashed, open arrowhead) |
| 2.3 | DASH | DASH | `showHazardDetails(view)` | synchronous (filled arrowhead) |
| 2.4 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| | | | *note:* the dashboard's calls run as drawn in Issue Hazard Warning (messages 1 and 2 there) | note |
| | | | **end** | fragment |
| 3 | OFF | EUI | `startEscalation(warningId)` | synchronous (filled arrowhead) |
| 3.1 | EUI | EC | `getEscalationOptions(warningId)` | synchronous (filled arrowhead) |
| 3.1.1 | EC | WARN | `statusAt(time)` | synchronous (filled arrowhead) |
| — | WARN | EC | status | return (dashed, open arrowhead) |
| | | | **alt [the warning is still active]** | fragment |
| — | EC | EUI | options | return (dashed, open arrowhead) |
| | | | **alt [a higher level exists]** | fragment |
| 3.2 | EUI | EUI | `showEscalationOptions(options)` | synchronous (filled arrowhead) |
| | | | **else [the warning is already at Level 3 – Red]** | fragment |
| 3.3 | EUI | EUI | `showNoHigherLevel(warning)` | synchronous (filled arrowhead) |
| | | | *note:* the warning is unchanged and nothing is sent; this use case ends here | note |
| | | | **end** | fragment |
| | | | **else [the warning has expired]** | fragment |
| — | EC | EUI | WarningNotEscalatableException(warning) | return (dashed, open arrowhead) |
| 3.4 | EUI | EUI | `showNotEscalatable(warning)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is changed; the officer may issue a new warning in Issue Hazard Warning; this use case ends here | note |
| | | | **end** | fragment |
| | | | **break [the officer cancels instead of choosing a level]** | fragment |
| 4 | OFF | EUI | `cancelEscalation()` | synchronous (filled arrowhead) |
| | | | *note:* the screen closes; the warning is unchanged, nothing is sent and nothing is recorded; this use case ends here | note |
| | | | **end** | fragment |
| 5 | OFF | EUI | `chooseEscalationLevel(newLevel)` | synchronous (filled arrowhead) |
| | | | *note:* the details form opens pre-filled from the warning: message, instructions (Sinhala, Tamil, English) and expiry time | note |
| | | | **break [the officer cancels instead of completing the details]** | fragment |
| 6 | OFF | EUI | `cancelEscalation()` | synchronous (filled arrowhead) |
| | | | *note:* the screen closes; the warning is unchanged, nothing is sent and nothing is recorded; this use case ends here | note |
| | | | **end** | fragment |
| 7 | OFF | EUI | `enterEscalationDetails(details)` | synchronous (filled arrowhead) |
| 7.1 | EUI | EC | `validateEscalation(warningId, details)` | synchronous (filled arrowhead) |
| — | EC | EUI | result | return (dashed, open arrowhead) |
| | | | **alt [the details are valid]** | fragment |
| 7.2 | EUI | EC | `previewEscalation(warningId, details)` | synchronous (filled arrowhead) |
| — | EC | EUI | preview | return (dashed, open arrowhead) |
| 7.3 | EUI | EUI | `showEscalationPreview(preview)` | synchronous (filled arrowhead) |
| | | | **else [a language is missing, the SMS limit is exceeded, the expiry time is not in the future, or the level is not higher]** | fragment |
| 7.4 | EUI | EUI | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the errors come from the result; the form keeps the details; the officer corrects them and previews again (message 7) | note |
| | | | **end** | fragment |
| | | | **opt [the officer chooses another offered level instead of confirming]** | fragment |
| 8 | OFF | EUI | `chooseEscalationLevel(newLevel)` | synchronous (filled arrowhead) |
| | | | *note:* the form keeps the text and expiry time and changes only the level; the officer previews again (message 7) | note |
| | | | **end** | fragment |
| | | | **break [the officer cancels instead of confirming]** | fragment |
| 9 | OFF | EUI | `cancelEscalation()` | synchronous (filled arrowhead) |
| | | | *note:* the screen closes; the warning is unchanged, nothing is sent and nothing is recorded; this use case ends here | note |
| | | | **end** | fragment |
| 10 | OFF | EUI | `confirmEscalation()` | synchronous (filled arrowhead) |
| 10.1 | EUI | EC | `validateEscalation(warningId, details)` | synchronous (filled arrowhead) |
| — | EC | EUI | result | return (dashed, open arrowhead) |
| | | | **alt [the details are still valid]** | fragment |
| 10.2 | EUI | EC | `escalateWarning(warningId, details, officerId)` | synchronous (filled arrowhead) |
| 10.2.1 | EC | WARN | `statusAt(time)` | synchronous (filled arrowhead) |
| — | WARN | EC | status | return (dashed, open arrowhead) |
| | | | **alt [the warning is still active and below the chosen level]** | fragment |
| 10.2.2 | EC | WARN | `escalate(details)` | synchronous (filled arrowhead) |
| 10.2.3 | EC | AR | `AuditRecord(action, officer, subjectId, detail, performedAt)` | «create» |
| | | | *note:* WARNING_ESCALATED for this warning, with the level change (old level → new level) | note |
| 10.2.4 | EC | WB | `broadcastEscalation(warning)` | synchronous (filled arrowhead) |
| 10.2.4.1 | WB | WARN | `coveredDistricts()` | synchronous (filled arrowhead) |
| — | WARN | WB | districts | return (dashed, open arrowhead) |
| | WB … WARN | | **ref** «include» Broadcast Warning | fragment |
| | | | *note:* targets: the citizens, organisations and district officers registered in the covered districts, in a new broadcast round | note |
| | | | **opt [the escalated warning is broadcast to citizens with the app]** | fragment |
| 10.2.4.2 | PNS | APP | `receiveAlert(warningId, warningLevel, text)` | asynchronous (open arrowhead) |
| 10.2.4.2.1 | APP | APP | `showAlert(warningId, warningLevel, text)` | synchronous (filled arrowhead) |
| | | | *note:* while the re-broadcast runs, the push gateway hands each push notification to the Push Notification Service; the app shows the Level 3 – Red alert with the evacuation instructions, in the citizen's language and with the alarm sound; organisations, district officers and SMS-only citizens read it by SMS | note |
| | | | **end** | fragment |
| — | WB | EC | summary | return (dashed, open arrowhead) |
| — | EC | EUI | outcome | return (dashed, open arrowhead) |
| 10.3 | EUI | EUI | `showEscalated(warning, summary)` | synchronous (filled arrowhead) |
| | | | **opt [delivery failures reported]** | fragment |
| 10.4 | EUI | EUI | `showDeliveryFailure(channel, failedRecipientIds)` | synchronous (filled arrowhead) |
| | | | *note:* the escalation screen offers no resend; the officer resends from the warning dashboard (messages 11-12) | note |
| | | | **end** | fragment |
| | | | **else [the warning has expired, or another officer escalated it meanwhile]** | fragment |
| — | EC | EUI | WarningNotEscalatableException(warning) | return (dashed, open arrowhead) |
| 10.5 | EUI | EUI | `showNotEscalatable(warning)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is changed, sent or recorded; the officer reviews the escalated warning; this use case ends here | note |
| | | | **end** | fragment |
| | | | **else [the details are no longer valid, e.g. the expiry time has passed or the warning was escalated meanwhile]** | fragment |
| 10.6 | EUI | EUI | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the officer corrects the details and previews again (message 7), reviews the escalated warning, or cancels (message 9) | note |
| | | | **end** | fragment |
| | | | **opt [delivery failures were reported and the gateway is available again]** | fragment |
| 11 | OFF | DASH | `selectHazard(hazardId)` | synchronous (filled arrowhead) |
| 11.1 | DASH | WC | `getHazardDetails(hazardId)` | synchronous (filled arrowhead) |
| — | WC | DASH | view | return (dashed, open arrowhead) |
| 11.2 | DASH | WC | `getBroadcastSummary(warningId)` | synchronous (filled arrowhead) |
| — | WC | DASH | summary | return (dashed, open arrowhead) |
| 11.3 | DASH | DASH | `showHazardDetails(view)` | synchronous (filled arrowhead) |
| 11.4 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| 11.5 | DASH | DASH | `showDeliveryFailure(channel, failedRecipientIds)` | synchronous (filled arrowhead) |
| | | | *note:* the failures of the escalation round, per channel, taken from the summary; the dashboard's calls run as in Issue Hazard Warning (message 2 there) | note |
| 12 | OFF | DASH | `resendOnChannel(warningId, channel)` | synchronous (filled arrowhead) |
| 12.1 | DASH | WC | `resendOnChannel(warningId, channel)` | synchronous (filled arrowhead) |
| 12.1.1 | WC | WB | `resend(warning, channel)` | synchronous (filled arrowhead) |
| | WB … WARN | | **ref** Broadcast Warning | fragment |
| | | | *note:* targets: the recipients whose latest notification in the escalation round failed, on the chosen channel | note |
| — | WB | WC | summary | return (dashed, open arrowhead) |
| — | WC | DASH | summary | return (dashed, open arrowhead) |
| 12.2 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
