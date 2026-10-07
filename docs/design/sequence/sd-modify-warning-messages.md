### Message script — sd Modify Warning

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OFF** — actor *DMC Officer*
- **DASH** — «boundary» `: WarningDashboardUI`
- **WC** — «control» `: WarningController`
- **HAZ** — «entity» `: Hazard`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| 1 | OFF | DASH | `editWarning()` | synchronous (filled arrowhead) |
| 1.1 | DASH | DASH | `showWarningForm(hazard)` | synchronous (filled arrowhead) |
| | | | **opt [the hazard situation has changed]** | fragment |
| 2 | OFF | DASH | `editHazard(hazardId, update)` | synchronous (filled arrowhead) |
| 2.1 | DASH | WC | `updateHazard(hazardId, update)` | synchronous (filled arrowhead) |
| | | | **alt [update valid]** | fragment |
| 2.1.1 | WC | HAZ | `updateHazard(update)` | synchronous (filled arrowhead) |
| — | WC | DASH | hazard | return (dashed, open arrowhead) |
| 2.2 | DASH | DASH | `showWarningForm(hazard)` | synchronous (filled arrowhead) |
| | | | **else [update invalid]** | fragment |
| — | WC | DASH | InvalidHazardUpdateException(errors) | return (dashed, open arrowhead) |
| 2.3 | DASH | DASH | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **end** | fragment |
| 3 | OFF | DASH | `enterWarningDetails(details)` | synchronous (filled arrowhead) |
| 4 | OFF | DASH | `requestPreview()` | synchronous (filled arrowhead) |
| 4.1 | DASH | WC | `validateWarning(hazardId, details)` | synchronous (filled arrowhead) |
| 4.1.1 | WC | HAZ | `missingInformation()` | synchronous (filled arrowhead) |
| — | HAZ | WC | missing | return (dashed, open arrowhead) |
| — | WC | DASH | result | return (dashed, open arrowhead) |
| | | | **alt [details valid]** | fragment |
| 4.2 | DASH | WC | `previewWarning(hazardId, details)` | synchronous (filled arrowhead) |
| — | WC | DASH | preview | return (dashed, open arrowhead) |
| 4.3 | DASH | DASH | `showPreview(preview)` | synchronous (filled arrowhead) |
| | | | **else [details invalid]** | fragment |
| 4.4 | DASH | DASH | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
