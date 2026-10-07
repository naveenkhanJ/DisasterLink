# DisasterLink citizen app (`mobile/`)

The citizen app of use case UC2, Submit and Verify Hazard Report (SE3070 Group_058): React Native with Expo SDK 57 and
TypeScript, run in **Expo Go (SDK 57)** on a phone. It started from the UI prototype (static screens); stage S6 adds
the skeleton's modules around it. **UC2's screens and behaviour are Afham's** and are not here yet.

## Set-up

1. Node 22 or later (checked with 24.13) and `npm ci` in this folder.
2. Copy `.env.example` to `.env` and set the address of the PC that runs the server:
   * `EXPO_PUBLIC_API_URL=http://<the PC's LAN IP>:8080` — find the address with `ipconfig` (the IPv4 address of the Wi-Fi
     adapter). `npx expo start --tunnel` tunnels only the Metro bundler, not the API, so it does not replace this.
   * `EXPO_PUBLIC_CITIZEN_ID=CIT-KLT-0001` (the demo citizen; the default).
   * `.env` is git-ignored; `.env.example` is committed.
3. Let the phone reach the server: the iPhone and the PC on the **same Wi-Fi**, and a **Windows Firewall inbound rule for
   TCP port 8080 on Private networks** (Windows Defender Firewall → Advanced settings → Inbound Rules → New Rule → Port →
   TCP 8080 → Allow → Private only).
4. Start the back end with the `dev` profile (seeded demo): from the repository root, `cd backend` and
   `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`.
5. In this folder run `npx expo start`, scan the QR code with the iPhone camera and open it in **Expo Go (SDK 57)**. Each
   Expo Go build supports exactly one SDK; the one on the iPhone must be SDK 57.

## The first-run check (development only, removable)

Two points of the implementation standards (§10) are believed but not verified: a plain-HTTP `fetch` from Expo Go to the
PC's LAN address, and a local notification in Expo Go on iOS. The check settles both.

1. Add `EXPO_PUBLIC_FIRST_RUN_CHECK=1` to `.env` and restart Expo (`npx expo start --clear`).
2. Open the app in Expo Go and accept the notification permission when asked.
3. **Success:** a local notification "DisasterLink first-run check — Server reachable: *n* push entries" appears (*n* is the
   number of undelivered push entries of the demo citizen, 0 on a fresh start). That proves both points.
4. **Failure:** "Server NOT reachable: *error*" means the notification works but the server does not (check the address,
   the Wi-Fi, the firewall rule and that the back end is running); no notification at all means the permission was refused or
   notifications do not show in Expo Go on that phone.
5. Report both outcomes (the server point and the notification point), then remove `src/dev/firstRunCheck.ts`, its call in
   `App.tsx` and `EXPO_PUBLIC_FIRST_RUN_CHECK` from `.env`.

## Tests and tooling

| Command | What it does |
|---|---|
| `npm test` | Jest (`jest-expo` preset, React Native Testing Library) |
| `npm run test:coverage` | the same with coverage; global 80 % lines and branches |
| `npm run lint` | ESLint (`eslint-config-expo`, `eslint-config-prettier`) |
| `npm run typecheck` | `tsc --noEmit` (TypeScript `strict`) |
| `node scripts/gen-district-outlines.mjs` | rewrites `src/location/districtOutlines.ts` from the officer map's `districts.js` |

Prettier is configured in `.prettierrc` (2 spaces, single quotes, 100 columns); the prototype's screens are not
re-formatted.

## Who owns what

**Skeleton (S6)** — `src/config/env.ts`, `src/api/types.ts`, `src/api/httpClient.ts`, `src/api/skeletonApi.ts` (active
warnings and the push outbox), `src/notifications/notifications.ts` (handler, Android channel `warnings`, permission, local
alert), `src/location/districtOutlines.ts` (the 25 outlines, data only), `src/constants.ts`, `src/dev/firstRunCheck.ts`,
and the Jest, ESLint and TypeScript set-up.

**UC2 (Afham)** — `ReportCaptureController`, `LocationService`, `OfflineReportStore`, `HomeCache`, `resolveDistrict`, the
report API calls (`POST /api/reports`, …), the merge of polled push entries into one alert per warning (`showAlert`) and
every screen's behaviour. The prototype's screens in `src/screens/` and `src/components/` are static until then.

The district outlines are © OpenStreetMap contributors (ODbL 1.0); the fonts are under the SIL Open Font License. See
`../THIRD-PARTY-NOTICES.md`.
