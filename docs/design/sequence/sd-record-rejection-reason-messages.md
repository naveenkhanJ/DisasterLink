### Message script — sd Record Rejection Reason

Lifelines, left to right (draw the stereotype icon and underline the head):

- **VC** — «boundary» `: VerificationConsoleUI`
- **RVC** — «control» `: ReportVerificationController`
- **HR** — «entity» `report : HazardReport`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *RVC is already executing (activation from the top of the frame)* | — |
| | | | *note:* continues the caller's rejectReport(reportId, reason, officerId); the officer and the time come from that call | note |
| | | | **alt [the reason is empty]** | fragment |
| — | RVC | VC | ReasonRequiredException | return (dashed, open arrowhead) |
| 1 | VC | VC | `showReasonRequired()` | synchronous (filled arrowhead) |
| | | | *note:* nothing is recorded; the officer enters the reason and confirms again | note |
| | | | **else [a reason is given]** | fragment |
| 2 | RVC | HR | `reject(officer, reason, at)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
