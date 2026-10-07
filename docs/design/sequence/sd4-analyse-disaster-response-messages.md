### Message script — sd Analyse Disaster Response & Generate Reports

Lifelines, left to right (draw the stereotype icon and underline the head):

- **OFF** — actor *DMC Officer*
- **UI** — «boundary» `: AnalyticsUI`
- **AC** — «control» `: AnalyticsController`
- **CALC** — «control» `calculator : MetricCalculator`

| No. | From | To | Message | Kind |
|---|---|---|---|---|
| | | | *note:* the officer is signed in (officerId); objects are loaded by their identifiers (persistence is not drawn); a server or network failure is shown on the screen's common error banner (not drawn) | note |
| 1 | OFF | UI | `openAnalytics()` | synchronous (filled arrowhead) |
| 1.1 | UI | AC | `getTemplates(officerId)` | synchronous (filled arrowhead) |
| — | AC | UI | templates | return (dashed, open arrowhead) |
| 1.2 | UI | AC | `getClosedEvents()` | synchronous (filled arrowhead) |
| — | AC | UI | hazards | return (dashed, open arrowhead) |
| 1.3 | UI | UI | `showTemplates(templates)` | synchronous (filled arrowhead) |
| 1.4 | UI | UI | `showClosedEvents(hazards)` | synchronous (filled arrowhead) |
| | | | **alt [the officer selects a closed hazard event]** | fragment |
| 2 | OFF | UI | `selectEvent(hazardId)` | synchronous (filled arrowhead) |
| 2.1 | UI | AC | `getCriteriaForEvent(hazardId)` | synchronous (filled arrowhead) |
| — | AC | UI | criteria | return (dashed, open arrowhead) |
| 2.2 | UI | UI | `showCriteria(criteria)` | synchronous (filled arrowhead) |
| | | | *note:* pre-filled from the hazard: its districts, its hazard type, the period from its start to its closing date, and a reference to the event | note |
| | | | **else [the officer selects a saved criteria template]** | fragment |
| 3 | OFF | UI | `selectTemplate(templateId)` | synchronous (filled arrowhead) |
| 3.1 | UI | UI | `showCriteria(criteria)` | synchronous (filled arrowhead) |
| | | | *note:* the template's criteria, received with the templates; templates are prepared in advance and none is saved here | note |
| | | | **end** | fragment |
| | | | *note:* without an event or a template, the officer selects the districts, the hazard type (which may be left empty) and the period directly; the officer keeps or changes them and selects the metrics; the panel keeps the values entered | note |
| | | | **loop [while the officer submits report criteria]** | fragment |
| 4 | OFF | UI | `enterCriteria(criteria)` | synchronous (filled arrowhead) |
| 4.1 | UI | AC | `validateCriteria(criteria, officerId)` | synchronous (filled arrowhead) |
| — | AC | UI | check | return (dashed, open arrowhead) |
| | | | *note:* the rules: at least one district, a start date not after the end date, a period of at most 24 months, at least one metric, and every district within the officer's reporting districts | note |
| | | | **alt [the criteria are valid]** | fragment |
| 4.2 | UI | AC | `generateReport(criteria, officerId)` | synchronous (filled arrowhead) |
| | | | **loop [for each selected metric]** | fragment |
| | | | **loop [for each calculator, until one supports the metric]** | fragment |
| 4.2.1 | AC | CALC | `supports(metric)` | synchronous (filled arrowhead) |
| — | CALC | AC | supported | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| 4.2.2 | AC | CALC | `calculate(criteria)` | synchronous (filled arrowhead) |
| | | | **alt [the metric is computed]** | fragment |
| — | CALC | AC | partialResults | return (dashed, open arrowhead) |
| | | | *note:* merged into the report's results; the matched-record counts of the metrics are added together | note |
| | | | **else [the calculator's records cannot be read]** | fragment |
| — | CALC | AC | failure | return (dashed, open arrowhead) |
| | | | *note:* recorded among the failed metrics; the remaining metrics are still computed | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | OFF … AC | | **ref** «include» Generate Statistical Report | fragment |
| | | | **else [the criteria are otherwise valid but some, not all, of the selected districts are outside the officer's reporting districts]** | fragment |
| 4.3 | UI | UI | `showOutOfScope(districts, adjusted)` | synchronous (filled arrowhead) |
| | | | *note:* the out-of-scope districts and the adjusted criteria without them come from the check | note |
| | | | **opt [the officer confirms the adjusted criteria]** | fragment |
| 5 | OFF | UI | `confirmAdjustedCriteria()` | synchronous (filled arrowhead) |
| | | | *note:* the criteria sent from here on are the adjusted criteria held by the screen from the check | note |
| 5.1 | UI | AC | `validateCriteria(criteria, officerId)` | synchronous (filled arrowhead) |
| — | AC | UI | check | return (dashed, open arrowhead) |
| | | | **alt [the adjusted criteria are still valid]** | fragment |
| 5.2 | UI | AC | `generateReport(criteria, officerId)` | synchronous (filled arrowhead) |
| | | | **loop [for each selected metric]** | fragment |
| | | | **loop [for each calculator, until one supports the metric]** | fragment |
| 5.2.1 | AC | CALC | `supports(metric)` | synchronous (filled arrowhead) |
| — | CALC | AC | supported | return (dashed, open arrowhead) |
| | | | **end** | fragment |
| 5.2.2 | AC | CALC | `calculate(criteria)` | synchronous (filled arrowhead) |
| | | | **alt [the metric is computed]** | fragment |
| — | CALC | AC | partialResults | return (dashed, open arrowhead) |
| | | | **else [the calculator's records cannot be read]** | fragment |
| — | CALC | AC | failure | return (dashed, open arrowhead) |
| | | | *note:* merged, or recorded among the failed metrics, as in the valid-criteria operand | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | OFF … AC | | **ref** «include» Generate Statistical Report | fragment |
| | | | **else [the adjusted criteria are no longer valid, e.g. the officer's reporting districts changed meanwhile]** | fragment |
| 5.3 | UI | UI | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the errors come from the check; no report is generated; the officer corrects the criteria and submits them again (message 4) | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | *note:* instead of confirming, the officer may change the criteria and submit them again (message 4) | note |
| | | | **else [the criteria are invalid, or none of the selected districts is within the officer's reporting districts]** | fragment |
| 4.4 | UI | UI | `showValidationErrors(errors)` | synchronous (filled arrowhead) |
| | | | *note:* the errors come from the check; no report is generated; the panel keeps the values entered; the officer corrects them and submits again (message 4) | note |
| | | | **end** | fragment |
| | | | **end** | fragment |
| | | | *note:* the officer submits criteria again after no records matched, to obtain a final report once a provisional district has synchronised, after changing the scope, or to retry a failed metric; each run that matches records stores a new report and keeps the earlier ones; otherwise this use case ends | note |
