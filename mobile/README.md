# DisasterLink citizen app (`mobile/`)

The citizen app of use case UC2, Submit and Verify Hazard Report (SE3070 Group_058): React Native with Expo SDK 57 and
TypeScript, run in **Expo Go (SDK 57)** on a phone. It started from the UI prototype (static screens); stage S6 adds
the skeleton's modules around it. **UC2's screens and behaviour are Afham's** and are not here yet.

## Set-up

1. Node 22 or later (checked with 24.13) and `npm ci` in this folder.
2. Copy `.env.example` to `.env` and set the address of the PC that runs the server:
   - `EXPO_PUBLIC_API_URL=auto` (the default) — the app takes the PC's address from the Expo development server, which runs
     on the same PC as the back end, and uses port 8080; it works on any network as long as Expo runs in LAN mode
     (`npx expo start`, not `--tunnel`). Alternatively set the address yourself:
     `EXPO_PUBLIC_API_URL=http://<the PC's LAN IP>:8080` — find the address with `ipconfig` (the IPv4 address of the Wi-Fi
     adapter). `npx expo start --tunnel` tunnels only the Metro bundler, not the API, so it does not replace this.
   - `EXPO_PUBLIC_CITIZEN_ID=CIT-KLT-0001` (the demo citizen; the default).
   - `EXPO_PUBLIC_CITIZEN_DISTRICT=KALUTARA` and `EXPO_PUBLIC_CITIZEN_LANGUAGE=ENGLISH` — the demo citizen's registered
     district and starting language; the defaults are the seeded CIT-KLT-0001's — the report's own district comes from
     the live GPS.
   - `.env` is git-ignored; `.env.example` is committed.
3. Let the phone reach the server: the iPhone and the PC on the **same Wi-Fi**, and a **Windows Firewall inbound rule for
   TCP port 8080 on Private networks** (Windows Defender Firewall → Advanced settings → Inbound Rules → New Rule → Port →
   TCP 8080 → Allow → Private only). If every call fails with "The server could not be reached.", check that (a) with an
   explicit address only, the IPv4 address of the PC's Wi-Fi adapter (`ipconfig`) is still the one in
   `EXPO_PUBLIC_API_URL` — the router may give the PC a new address — and restart Expo with `npx expo start --clear` after
   changing `.env` (with `auto` there is nothing to change); (b) Windows has the Wi-Fi network as **Private** (Settings →
   Network & internet → Wi-Fi → the network → Network profile type), because the firewall rule covers Private networks
   only; (c) `http://<that address>:8080/` opens in the iPhone's browser.
4. Start the back end with the `dev` profile (seeded demo): from the repository root, `cd backend` and
   `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`.
5. In this folder run `npx expo start`, scan the QR code with the iPhone camera and open it in **Expo Go (SDK 57)**. Each
   Expo Go build supports exactly one SDK; the one on the iPhone must be SDK 57.

## Verified on a phone

Verified on 13 October 2026 (Afham's iPhone, Expo Go SDK 57): plain-HTTP calls from Expo Go to the PC's LAN address work;
local notifications show as a banner with sound once permission is granted — await `configureNotifications()` before the
first alert. On 14 October 2026 a report upload from Expo Go to `POST /api/reports` (camera and gallery photo, JPEG at
quality 0.7) was ACCEPTED once the JSON parts were sent as `Blob`s and the photo as an `expo-file-system` `File`
(standards §12.3).

## Tests and tooling

| Command                                  | What it does                                                                      |
| ---------------------------------------- | --------------------------------------------------------------------------------- |
| `npm test`                               | Jest (`jest-expo` preset, React Native Testing Library)                           |
| `npm run test:coverage`                  | the same with coverage; global 80 % lines and branches                            |
| `npm run lint`                           | ESLint (`eslint-config-expo`, `eslint-config-prettier`)                           |
| `npm run typecheck`                      | `tsc --noEmit` (TypeScript `strict`)                                              |
| `node scripts/gen-district-outlines.mjs` | rewrites `src/location/districtOutlines.ts` from the officer map's `districts.js` |

Prettier is configured in `.prettierrc` (2 spaces, single quotes, 100 columns); the prototype's screens are not
re-formatted.

## Who owns what

**Skeleton (S6)** — `src/config/env.ts`, `src/api/types.ts`, `src/api/httpClient.ts`, `src/api/skeletonApi.ts` (active
warnings and the push outbox), `src/notifications/notifications.ts` (handler, Android channel `warnings`, permission, local
alert), `src/location/districtOutlines.ts` (the 25 outlines, data only), `src/constants.ts`, and the Jest, ESLint and TypeScript set-up.

**UC2 (Afham)** — `ReportCaptureController`, `LocationService`, `OfflineReportStore`, `HomeCache`, `resolveDistrict`, the
report API calls (`POST /api/reports`, …), the merge of polled push entries into one alert per warning (`showAlert`) and
every screen's behaviour. The prototype's screens in `src/screens/` and `src/components/` are static until then.

The district outlines are © OpenStreetMap contributors (ODbL 1.0); the fonts are under the SIL Open Font License. See
`../THIRD-PARTY-NOTICES.md`.
