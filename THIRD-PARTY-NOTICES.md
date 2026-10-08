# Third-party notices

DisasterLink ships the following third-party software, fonts and data. Each keeps its own licence; the licence texts
are in the repository at the paths below. Nothing here has been changed from the upstream files except where a section
says so.

## Leaflet 1.9.4 — BSD-2-Clause

The map library of the officer screens, copied byte for byte from the npm package `leaflet@1.9.4`
(`backend/src/main/resources/static/vendor/leaflet-1.9.4/`: `leaflet.js`, `leaflet.css` and the marker and layer images).
Licence text: `backend/src/main/resources/static/vendor/leaflet-1.9.4/LICENSE`.

## Chart.js 4.4.4 — MIT

The chart library of the analytics screens, copied byte for byte from the npm package `chart.js@4.4.4`
(`backend/src/main/resources/static/vendor/chartjs-4.4.4/chart.umd.js`).
Licence text: `backend/src/main/resources/static/vendor/chartjs-4.4.4/LICENSE.md`.

## Fonts — SIL Open Font License 1.1

The fonts are under `backend/src/main/resources/static/fonts/`, each family with its licence text:

| Family | Files | Licence text |
|---|---|---|
| Manrope | `Manrope-VariableFont_wght.ttf` | `OFL-Manrope.txt` |
| Noto Sans Sinhala | `NotoSansSinhala-Regular.ttf`, `NotoSansSinhala-Bold.ttf` | `OFL-NotoSansSinhala.txt` |
| Noto Sans Tamil | `NotoSansTamil-Regular.ttf`, `NotoSansTamil-Bold.ttf` | `OFL-NotoSansTamil.txt` |

The citizen app uses the same fonts, with the same licence texts, under `mobile/assets/fonts/` (the three `OFL-*.txt`
files and the five `.ttf` files).

## District outlines — ODbL 1.0

District outlines in `static/js/districts.js` are derived (simplified, Douglas–Peucker 0.003°) from geoBoundaries gbOpen
LKA ADM2 (boundary ID LKA-ADM2-46371173, build 12 Dec 2023; Runfola et al., 2020), whose source is OpenStreetMap (via
Wambacher). © OpenStreetMap contributors. Available under the Open Data Commons Open Database License 1.0
(https://opendatacommons.org/licenses/odbl/1-0/); see https://www.openstreetmap.org/copyright. The simplified outlines are a
derivative database offered under the same licence.

The citizen app carries the same outlines in `mobile/src/location/districtOutlines.ts`, generated from `districts.js` by
`mobile/scripts/gen-district-outlines.mjs`; it is a derivative database under the same ODbL terms and credit, and the file
says so in its header.

The sidebar of every officer screen shows the credit "Map data © OpenStreetMap contributors (ODbL), via geoBoundaries".

## GeoNames — Sri Lanka place names (`LK` country file) — CC BY 4.0

Source: https://download.geonames.org/export/dump/LK.zip. Licence: Creative Commons Attribution 4.0 International
(CC BY 4.0), https://creativecommons.org/licenses/by/4.0/. GeoNames' own statement, from
https://download.geonames.org/export/dump/readme.txt, reads:

> This work is licensed under a Creative Commons Attribution 4.0 License,
> see https://creativecommons.org/licenses/by/4.0/
> The Data is provided "as is" without warranty or any representation of accuracy, timeliness or completeness.

Used in the citizen app's manual-location place list (`mobile/src/location/placeData.ts`), derived (filtered to populated
places). The app shows the credit "Place names: GeoNames (CC BY 4.0)".
