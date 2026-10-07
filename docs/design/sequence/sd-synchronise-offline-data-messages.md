### Message script — sd Synchronise Offline Data

Lifelines, left to right (draw the stereotype icon and underline the head):

- **CIT** — actor *Citizen*
- **DO** — actor *District Officer*
- **APP** — «boundary» `: CitizenAppUI`
- **RCC** — «control» `: ReportCaptureController`
- **RSC** — «control» `: ReportSubmissionController`
- **ORS** — «entity» `: OfflineReportStore`
- **SMUI** — «boundary» `: ShelterManagementUI`
- **OSC** — «control» `: OfflineSyncController`
- **OUT** — «entity» `: OfflineOutbox`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | **alt [citizen app]** | fragment |
| 1 | ● | APP | `onConnectivityRestored()` | found message (arrow from a filled dot; sender unknown) |
| | | | *note:* also raised when the app starts, or returns to the foreground, with a connection | note |
| 1.1 | APP | RCC | `syncPending()` | synchronous (filled arrowhead) |
| 1.1.1 | RCC | ORS | `findPending()` | synchronous (filled arrowhead) |
| — | ORS | RCC | drafts | return (dashed, open arrowhead) |
| | | | **loop [for each draft pending synchronisation]** | fragment |
| 1.1.2 | RCC | RSC | `receiveReport(draft, photo, meta)` | synchronous (filled arrowhead) |
| | | | *note:* the server validates and records the report as for an online submission; a submission it has already recorded returns the recorded report | note |
| | | | **alt [the report was accepted, or had already been recorded]** | fragment |
| — | RSC | RCC | receipt | return (dashed, open arrowhead) |
| 1.1.3 | RCC | ORS | `remove(draftId)` | synchronous (filled arrowhead) |
| | | | **else [the report was not accepted]** | fragment |
| — | RSC | RCC | receipt | return (dashed, open arrowhead) |
| 1.1.4 | RCC | ORS | `markRejected(draftId, reason)` | synchronous (filled arrowhead) |
| | | | **else [no reply: server error or connection lost]** | fragment |
| 1.1.5 | RCC | ORS | `save(draft)` | synchronous (filled arrowhead) |
| | | | *note:* kept as Pending synchronisation with its attempt count increased; tried again later | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| — | RCC | APP | results | return (dashed, open arrowhead) |
| 1.2 | APP | APP | `showSyncResults(results)` | synchronous (filled arrowhead) |
| | | | **opt [a draft was not accepted]** | fragment |
| 2 | CIT | APP | `openDraft(draftId)` | synchronous (filled arrowhead) |
| 2.1 | APP | RCC | `getDraft(draftId)` | synchronous (filled arrowhead) |
| — | RCC | APP | draft | return (dashed, open arrowhead) |
| | | | **alt [the citizen corrects the draft]** | fragment |
| | | | *note:* the draft opens on the report form with the reason; it is reviewed and submitted again as a new report | note |
| | | | **else [the citizen discards the draft]** | fragment |
| 3 | CIT | APP | `discardDraft(draftId)` | synchronous (filled arrowhead) |
| 3.1 | APP | RCC | `discardDraft(draftId)` | synchronous (filled arrowhead) |
| 3.1.1 | RCC | ORS | `remove(draftId)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | **else [shelter page]** | fragment |
| | | | **alt [the connection is restored, or the page opens with a connection]** | fragment |
| 4 | ● | SMUI | `onConnectivityRestored()` | found message (arrow from a filled dot; sender unknown) |
| 4.1 | SMUI | SMUI | `showConnectivity(online)` | synchronous (filled arrowhead) |
| | SMUI … OUT | | **ref** Replay Pending Shelter Changes | fragment |
| | | | **else [the officer selects Sync now]** | fragment |
| 5 | DO | SMUI | `syncNow()` | synchronous (filled arrowhead) |
| | SMUI … OUT | | **ref** Replay Pending Shelter Changes | fragment |
| | | | **end** | fragment |
| | | | **opt [the officer dismisses a change that was not accepted]** | fragment |
| 6 | DO | SMUI | `dismissPending(submissionId)` | synchronous (filled arrowhead) |
| 6.1 | SMUI | OSC | `dismiss(submissionId)` | synchronous (filled arrowhead) |
| 6.1.1 | OSC | OUT | `remove(submissionId)` | synchronous (filled arrowhead) |
| 6.2 | SMUI | OSC | `getPending()` | synchronous (filled arrowhead) |
| — | OSC | SMUI | items | return (dashed, open arrowhead) |
| 6.3 | SMUI | SMUI | `showPending(items)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **end** | fragment |
