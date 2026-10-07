### Message script — sd Export or Share Report

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OFF** — actor *DMC Officer*
- **ORG** — actor *Resource-Owning Organisation*
- **UI** — «boundary» `: AnalyticsUI`
- **DRV** — «boundary» `: DonorReportView`
- **AC** — «control» `: AnalyticsController`
- **EXP** — «control» `exporter : ReportExporter`
- **DR** — «entity» `report : DisasterReport`
- **AR** — «entity» `exportRecord : AuditRecord`
- **AS** — «entity» `shareRecord : AuditRecord`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* the report is the stored report shown on the screen; objects are loaded by their identifiers (persistence is not drawn); nothing is recomputed | note |
| | | | **loop [while the officer requests an export]** | fragment |
| 1 | OFF | UI | `exportReport(reportId, format)` | synchronous (filled arrowhead) |
| 1.1 | UI | AC | `exportReport(reportId, format, officerId)` | synchronous (filled arrowhead) |
| | | | **loop [for each exporter, until one supports the format]** | fragment |
| 1.1.1 | AC | EXP | `supports(format)` | synchronous (filled arrowhead) |
| — | EXP | AC | supported | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| 1.1.2 | AC | EXP | `export(report)` | synchronous (filled arrowhead) |
| | | | **alt [the file is produced]** | fragment |
| — | EXP | AC | file | return (dashed, open arrowhead) |
| 1.1.3 | AC | AR | `AuditRecord(action, officer, subjectId, detail, performedAt)` | «create» |
| | | | *note:* report exported (one export record on either path) | note |
| — | AC | UI | file | return (dashed, open arrowhead) |
| 1.2 | UI | UI | `showExportFile(file)` | synchronous (filled arrowhead) |
| | | | *note:* the file (PDF, Excel or CSV; tabular, without charts) is offered for download | note |
| | | | **else [the exporter fails]** | fragment |
| — | EXP | AC | failure | return (dashed, open arrowhead) |
| 1.1.4 | AC | AR | `AuditRecord(action, officer, subjectId, detail, performedAt)` | «create» |
| | | | *note:* export failed | note |
| — | AC | UI | ExportFailedException(message) | return (dashed, open arrowhead) |
| 1.3 | UI | UI | `showExportError(message)` | synchronous (filled arrowhead) |
| | | | *note:* the report stays stored; the officer may export again in this or another format without regenerating the statistics | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | **loop [while the officer shares the report with organisations]** | fragment |
| 2 | OFF | UI | `shareReport(reportId, organizationIds)` | synchronous (filled arrowhead) |
| 2.1 | UI | AC | `shareReport(reportId, organizationIds, officerId)` | synchronous (filled arrowhead) |
| | | | **loop [for each selected organisation]** | fragment |
| | | | **opt [the report holds distribution lines owned by the organisation]** | fragment |
| 2.1.1 | AC | DR | `shareWith(organization)` | synchronous (filled arrowhead) |
| | | | **end** | fragment |
| | | | **end** | fragment |
| 2.1.2 | AC | AS | `AuditRecord(action, officer, subjectId, detail, performedAt)` | «create» |
| | | | *note:* report shared | note |
| — | AC | UI | outcome | return (dashed, open arrowhead) |
| 2.2 | UI | UI | `showShared(outcome)` | synchronous (filled arrowhead) |
| | | | *note:* the organisations shared with, and those owning no line in the report listed as "no data — not shared"; no notification is sent; the officer may share with another organisation, change the criteria in Analyse Disaster Response & Generate Reports, or end | note |
| | | | **end** | fragment |
| | | | *note:* if no organisation opens the report later, this use case ends here | note |
| | | | **opt [later, in its own session, a Resource-Owning Organisation opens the report]** | fragment |
| | | | **opt [the organisation opens its list of shared reports]** | fragment |
| 3 | ORG | DRV | `openSharedReports(organizationId)` | synchronous (filled arrowhead) |
| 3.1 | DRV | AC | `getSharedReports(organizationId)` | synchronous (filled arrowhead) |
| — | AC | DRV | reports | return (dashed, open arrowhead) |
| 3.2 | DRV | DRV | `showSharedReports(reports)` | synchronous (filled arrowhead) |
| | | | *note:* the title, generation time, period and provisional status of each report shared with the organisation, without results | note |
| | | | **end** | fragment |
| 4 | ORG | DRV | `openSharedReport(reportId, organizationId)` | synchronous (filled arrowhead) |
| | | | *note:* from the list, or by the report reference without the list | note |
| 4.1 | DRV | AC | `getScopedView(reportId, organizationId)` | synchronous (filled arrowhead) |
| | | | **alt [the report was shared with the organisation]** | fragment |
| — | AC | DRV | scopedReport | return (dashed, open arrowhead) |
| 4.2 | DRV | DRV | `showScopedReport(scopedReport)` | synchronous (filled arrowhead) |
| | | | *note:* title, period and provisional status; the alert timeline, citizens reached and shelter occupancy unchanged; the distribution table restricted on the server to the organisation's own lines; this use case ends here | note |
| | | | **else [the report was not shared with the organisation]** | fragment |
| — | AC | DRV | ReportNotSharedException() | return (dashed, open arrowhead) |
| 4.3 | DRV | DRV | `showNotShared()` | synchronous (filled arrowhead) |
| | | | *note:* no results are shown; this use case ends here | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
