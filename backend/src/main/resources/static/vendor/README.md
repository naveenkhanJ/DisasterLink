# Vendored browser libraries

Served locally, so the demo needs no network (implementation standards section 1, "Map and charts on officer screens").
The files are copied unchanged from the npm packages, taken with `npm pack` in a temporary folder.

| Folder | Library | Version | Source | Licence | Used by |
|---|---|---|---|---|---|
| `leaflet-1.9.4/` | Leaflet | 1.9.4 | npm package `leaflet@1.9.4`, `dist/leaflet.js`, `dist/leaflet.css`, `dist/images/*`, `LICENSE` | BSD-2-Clause (`LICENSE`) | `js/district-map.js` (the district map of UC1 and UC2) |
| `chartjs-4.4.4/` | Chart.js | 4.4.4 | npm package `chart.js@4.4.4`, `dist/chart.umd.js`, `LICENSE.md` | MIT (`LICENSE.md`) | UC4's charts (`report-charts.js`, written by Naveenkhan) |

Do not edit these files and do not load them from a CDN. A new version is a change on `shared/skeleton`.
