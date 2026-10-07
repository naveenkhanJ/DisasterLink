### Message script — sd Issue Hazard Warning

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OFF** — actor *DMC Officer*
- **DASH** — «boundary» `: WarningDashboardUI`
- **WC** — «control» `: WarningController`
- **HAZ** — «entity» `: Hazard`
- **AW** — «entity» `activeWarning : Warning`
- **WB** — «control» `: WarningBroadcaster`
- **WARN** — «entity» `warning : Warning`
- **APP** — «boundary» `: CitizenAppUI`
- **PNS** — actor *Push Notification Service*

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* objects are loaded by their identifiers (persistence is not drawn); a server or network failure is shown on the screen's common error banner (not drawn) | note |
| 1 | OFF | DASH | `openDashboard()` | synchronous (filled arrowhead) |
| 1.1 | DASH | WC | `getAssessableHazards()` | synchronous (filled arrowhead) |
| | | | **loop [for each recorded hazard]** | fragment |
| 1.1.1 | WC | HAZ | `isAssessable()` | synchronous (filled arrowhead) |
| — | HAZ | WC | assessable | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | WC | DASH | hazards | return (dashed, open arrowhead) |
| 1.2 | DASH | DASH | `showHazards(hazards)` | synchronous (filled arrowhead) |
| 2 | OFF | DASH | `selectHazard(hazardId)` | synchronous (filled arrowhead) |
| 2.1 | DASH | WC | `getHazardDetails(hazardId)` | synchronous (filled arrowhead) |
| 2.1.1 | WC | HAZ | `missingInformation()` | synchronous (filled arrowhead) |
| — | HAZ | WC | missing | return (dashed, open arrowhead) |
| 2.1.2 | WC | WC | `getActiveWarningForHazard(hazardId)` | synchronous (filled arrowhead) |
| | | | **opt [a warning is recorded for the hazard]** | fragment |
| 2.1.2.1 | WC | AW | `statusAt(time)` | synchronous (filled arrowhead) |
| — | AW | WC | status | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | WC | WC | activeWarning | return (dashed, open arrowhead) |
| — | WC | DASH | view | return (dashed, open arrowhead) |
| 2.2 | DASH | DASH | `showHazardDetails(view)` | synchronous (filled arrowhead) |
| | | | **alt [hazard information complete and no active warning]** | fragment |
| 2.3 | DASH | DASH | `showWarningForm(hazard)` | synchronous (filled arrowhead) |
| | | | **else [the hazard already has an active warning]** | fragment |
| 2.4 | DASH | WC | `getBroadcastSummary(warningId)` | synchronous (filled arrowhead) |
| 2.4.1 | WC | AW | `getNotifications()` | synchronous (filled arrowhead) |
| — | AW | WC | notifications | return (dashed, open arrowhead) |
| — | WC | DASH | summary | return (dashed, open arrowhead) |
| 2.5 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| | | | *note:* the officer may start the separate use case Escalate Warning for this warning; this use case ends here | note |
| | | | **else [hazard information incomplete and no active warning]** | fragment |
| 2.6 | DASH | DASH | `showHazardIncomplete(missing)` | synchronous (filled arrowhead) |
| 3 | OFF | DASH | `editHazard(hazardId, update)` | synchronous (filled arrowhead) |
| 3.1 | DASH | WC | `updateHazard(hazardId, update)` | synchronous (filled arrowhead) |
| | | | **alt [update valid]** | fragment |
| 3.1.1 | WC | HAZ | `updateHazard(update)` | synchronous (filled arrowhead) |
| — | WC | DASH | hazard | return (dashed, open arrowhead) |
| 3.2 | DASH | DASH | `showWarningForm(hazard)` | synchronous (filled arrowhead) |
| | | | **else [update invalid]** | fragment |
| — | WC | DASH | InvalidHazardUpdateException(errors) | return (dashed, open arrowhead) |
| 3.3 | DASH | DASH | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the officer corrects the update and sends it again (message 3) | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| 4 | OFF | DASH | `enterWarningDetails(details)` | synchronous (filled arrowhead) |
| 5 | OFF | DASH | `requestPreview()` | synchronous (filled arrowhead) |
| 5.1 | DASH | WC | `validateWarning(hazardId, details)` | synchronous (filled arrowhead) |
| 5.1.1 | WC | HAZ | `missingInformation()` | synchronous (filled arrowhead) |
| — | HAZ | WC | missing | return (dashed, open arrowhead) |
| — | WC | DASH | result | return (dashed, open arrowhead) |
| | | | **alt [details valid]** | fragment |
| 5.2 | DASH | WC | `previewWarning(hazardId, details)` | synchronous (filled arrowhead) |
| — | WC | DASH | preview | return (dashed, open arrowhead) |
| 5.3 | DASH | DASH | `showPreview(preview)` | synchronous (filled arrowhead) |
| | | | **opt [warning needs modification]** | fragment |
| | OFF … HAZ | | **ref** «extend» Modify Warning | fragment |
| | | | **end** | fragment |
| | | | **else [details invalid, or no affected area selected]** | fragment |
| 5.4 | DASH | DASH | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the errors come from the result; the officer corrects the details and requests the preview again (messages 4–5) | note |
| | | | **end** | fragment |
| 6 | OFF | DASH | `confirmBroadcast()` | synchronous (filled arrowhead) |
| 6.1 | DASH | WC | `validateWarning(hazardId, details)` | synchronous (filled arrowhead) |
| 6.1.1 | WC | HAZ | `missingInformation()` | synchronous (filled arrowhead) |
| — | HAZ | WC | missing | return (dashed, open arrowhead) |
| — | WC | DASH | result | return (dashed, open arrowhead) |
| | | | **alt [details still valid]** | fragment |
| 6.2 | DASH | WC | `issueWarning(hazardId, details, officerId)` | synchronous (filled arrowhead) |
| 6.2.1 | WC | WC | `getActiveWarningForHazard(hazardId)` | synchronous (filled arrowhead) |
| | | | **opt [a warning is recorded for the hazard]** | fragment |
| 6.2.1.1 | WC | AW | `statusAt(time)` | synchronous (filled arrowhead) |
| — | AW | WC | status | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| — | WC | WC | activeWarning | return (dashed, open arrowhead) |
| | | | **alt [no active warning for the hazard]** | fragment |
| 6.2.2 | WC | WARN | `Warning(hazard, details, officer)` | «create» |
| 6.2.3 | WC | WB | `broadcast(warning)` | synchronous (filled arrowhead) |
| 6.2.3.1 | WB | WARN | `coveredDistricts()` | synchronous (filled arrowhead) |
| — | WARN | WB | districts | return (dashed, open arrowhead) |
| | WB … WARN | | **ref** «include» Broadcast Warning | fragment |
| | | | *note:* targets: the citizens and organisations registered in the covered districts, on each selected channel | note |
| | | | **opt [the warning is broadcast to citizens with the app]** | fragment |
| 6.2.3.2 | PNS | APP | `receiveAlert(warningId, warningLevel, text)` | asynchronous (open arrowhead) |
| 6.2.3.2.1 | APP | APP | `showAlert(warningId, warningLevel, text)` | synchronous (filled arrowhead) |
| | | | *note:* while the broadcast runs, the push gateway hands each push notification to the Push Notification Service; the app shows it as one alert in the citizen's language, with the alarm sound; SMS recipients read the warning in their messaging app | note |
| | | | **end** | fragment |
| — | WB | WC | summary | return (dashed, open arrowhead) |
| — | WC | DASH | outcome | return (dashed, open arrowhead) |
| 6.3 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| | | | **opt [delivery failures reported]** | fragment |
| 6.4 | DASH | DASH | `showDeliveryFailure(channel, failedRecipientIds)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **else [the hazard has an active warning by now]** | fragment |
| — | WC | DASH | ActiveWarningExistsException(warning) | return (dashed, open arrowhead) |
| 6.5 | DASH | DASH | `showActiveWarningExists(warning)` | synchronous (filled arrowhead) |
| | | | *note:* issued by another officer after the hazard was selected, or recorded by the officer's own earlier confirmation whose reply was lost; no second warning is recorded and nothing is broadcast | note |
| | | | *note:* the officer may escalate that warning in Escalate Warning; this use case ends here | note |
| | | | **end** | fragment |
| | | | **else [details no longer valid, e.g. the expiry time has passed]** | fragment |
| 6.6 | DASH | DASH | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the officer corrects the details and requests the preview again (messages 4–5) | note |
| | | | **end** | fragment |
| | | | **opt [delivery failures reported and the gateway is available again]** | fragment |
| 7 | OFF | DASH | `resendOnChannel(warningId, channel)` | synchronous (filled arrowhead) |
| 7.1 | DASH | WC | `resendOnChannel(warningId, channel)` | synchronous (filled arrowhead) |
| 7.1.1 | WC | WB | `resend(warning, channel)` | synchronous (filled arrowhead) |
| | WB … WARN | | **ref** Broadcast Warning | fragment |
| | | | *note:* targets: the recipients whose latest notification in the current round failed, on the chosen channel | note |
| — | WB | WC | summary | return (dashed, open arrowhead) |
| — | WC | DASH | summary | return (dashed, open arrowhead) |
| 7.2 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| 8 | OFF | DASH | `selectHazard(hazardId)` | synchronous (filled arrowhead) |
| 8.1 | DASH | WC | `getHazardDetails(hazardId)` | synchronous (filled arrowhead) |
| 8.1.1 | WC | HAZ | `missingInformation()` | synchronous (filled arrowhead) |
| — | HAZ | WC | missing | return (dashed, open arrowhead) |
| 8.1.2 | WC | WC | `getActiveWarningForHazard(hazardId)` | synchronous (filled arrowhead) |
| 8.1.2.1 | WC | WARN | `statusAt(time)` | synchronous (filled arrowhead) |
| — | WARN | WC | status | return (dashed, open arrowhead) |
| — | WC | WC | warning | return (dashed, open arrowhead) |
| — | WC | DASH | view | return (dashed, open arrowhead) |
| 8.2 | DASH | WC | `getBroadcastSummary(warningId)` | synchronous (filled arrowhead) |
| 8.2.1 | WC | WARN | `getNotifications()` | synchronous (filled arrowhead) |
| — | WARN | WC | notifications | return (dashed, open arrowhead) |
| — | WC | DASH | summary | return (dashed, open arrowhead) |
| 8.3 | DASH | DASH | `showHazardDetails(view)` | synchronous (filled arrowhead) |
| 8.4 | DASH | DASH | `showActiveWarning(warning, summary)` | synchronous (filled arrowhead) |
| | | | *note:* Escalate Warning is offered for the active warning | note |
