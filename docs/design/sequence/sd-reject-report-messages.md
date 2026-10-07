### Message script — sd Reject Report

Lifelines, left to right (draw the stereotype icon and underline the head):

- **DUTY** — actor *DMC Duty Officer*
- **VC** — «boundary» `: VerificationConsoleUI`
- **RVC** — «control» `: ReportVerificationController`
- **HR** — «entity» `report : HazardReport`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* the reason field and the confirmation question are part of the console screen | note |
| 1 | DUTY | VC | `rejectReport(reportId, reason)` | synchronous (filled arrowhead) |
| 1.1 | VC | RVC | `rejectReport(reportId, reason, officerId)` | synchronous (filled arrowhead) |
| 1.1.1 | RVC | HR | `isPending()` | synchronous (filled arrowhead) |
| — | HR | RVC | pending | return (dashed, open arrowhead) |
| | | | **alt [the report is still pending]** | fragment |
| | VC … HR | | **ref** «include» Record Rejection Reason | fragment |
| | | | **opt [the rejection was recorded]** | fragment |
| | RVC … RVC | | **ref** Send Decision Notice | fragment |
| — | RVC | VC | report | return (dashed, open arrowhead) |
| 1.2 | VC | VC | `showDecisionRecorded(report)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **else [another decision was recorded first]** | fragment |
| — | RVC | VC | ReportAlreadyDecidedException(report) | return (dashed, open arrowhead) |
| 1.3 | VC | VC | `showAlreadyDecided(report)` | synchronous (filled arrowhead) |
| | | | *note:* nothing is changed and no notice is sent | note |
| | | | **end** | fragment |
