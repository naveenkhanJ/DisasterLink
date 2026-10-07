### Message script — sd Generate Statistical Report

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OFF** — actor *DMC Officer*
- **UI** — «boundary» `: AnalyticsUI`
- **AC** — «control» `: AnalyticsController`
- **DR** — «entity» `report : DisasterReport`
- **AR** — «entity» `generatedRecord : AuditRecord`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *AC is already executing (activation from the top of the frame)* | — |
| | | | *note:* continues AnalyticsController.generateReport(criteria, officerId), called by the analytics screen, after each selected metric has been computed (the results, with any failed metric) | note |
| | | | **alt [records match the criteria]** | fragment |
| 1 | AC | DR | `DisasterReport(reportTitle, generatedDate, criteria, results, officer, hazards)` | «create» |
| | | | *note:* the hazards matching the criteria are read and linked; the generation time, the criteria, a snapshot of the results with the number of matched records and any failed metric, the hazards the report covers and the officer who generated it | note |
| | | | *note:* the last synchronisation of every client in the selected districts is read | note |
| | | | **opt [a client in a selected district still holds pending items]** | fragment |
| 2 | AC | DR | `markProvisional(since)` | synchronous (filled arrowhead) |
| | | | *note:* for each such district, the earliest last synchronisation among its clients still holding pending items | note |
| | | | **end** | fragment |
| 3 | AC | AR | `AuditRecord(action, officer, subjectId, detail, performedAt)` | «create» |
| | | | *note:* report generated, for this report | note |
| | | | **else [no records match the criteria]** | fragment |
| | | | *note:* an unsaved report: it is not stored and no audit record is made; its number of matched records is 0 | note |
| | | | **end** | fragment |
| — | AC | UI | report | return (dashed, open arrowhead) |
| | | | **alt [the report holds matched records]** | fragment |
| 4 | UI | UI | `showResults(report)` | synchronous (filled arrowhead) |
| | | | *note:* the scope, the number of matched records, the generation time and each computed metric with its chart | note |
| | | | **opt [the report is provisional]** | fragment |
| 5 | UI | UI | `showProvisionalNotice(since)` | synchronous (filled arrowhead) |
| | | | *note:* each provisional district with its last synchronisation time, from the report | note |
| | | | **end** | fragment |
| | | | **opt [a metric could not be computed]** | fragment |
| 6 | UI | UI | `showComputationError(metrics)` | synchronous (filled arrowhead) |
| | | | *note:* the failed metrics, from the report; the other metrics are shown | note |
| | | | **end** | fragment |
| | | | *note:* instead of recording findings, the officer may change the criteria and submit them again in Analyse Disaster Response & Generate Reports, which stores a new report and keeps this one | note |
| 7 | OFF | UI | `enterFindings(reportId, text)` | synchronous (filled arrowhead) |
| 7.1 | UI | AC | `saveFindings(reportId, text)` | synchronous (filled arrowhead) |
| 7.1.1 | AC | DR | `recordFindings(text)` | synchronous (filled arrowhead) |
| — | AC | UI | report | return (dashed, open arrowhead) |
| 7.2 | UI | UI | `showResults(report)` | synchronous (filled arrowhead) |
| | | | **opt [the officer requests an export or a share — extension point: export or share requested]** | fragment |
| | OFF … DR | | **ref** «extend» Export or Share Report | fragment |
| | | | **end** | fragment |
| | | | *note:* otherwise this use case ends with the stored report and its findings | note |
| | | | **else [no records matched]** | fragment |
| 8 | UI | UI | `showNoRecords(suggestion)` | synchronous (filled arrowhead) |
| | | | *note:* the suggestion is built from the criteria (widen the districts or the period, or clear the hazard type); the officer changes the criteria and submits them again in Analyse Disaster Response & Generate Reports; this interaction ends here | note |
| | | | **end** | fragment |
