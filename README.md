# DisasterLink

Smart Disaster Early-Warning and Emergency Coordination System for Sri Lanka — SE3070 Case Studies in
Software Engineering, Assignment 02 (Group_058, SLIIT Malabe). The design is Group_058's improved version of
Group_060's Assignment 01 design for Case Study 2.

## Use cases and owners

| Use case | Owner |
|---|---|
| UC1 Issue Hazard Warning (with Escalate Warning) | P. M. L. Lakmal |
| UC2 Submit and Verify Hazard Report (with the citizen mobile app) | M. I. M. Afham |
| UC3 Manage Emergency Shelters | H. P. Weliwitigoda |
| UC4 Analyse Disaster Response & Generate Reports | J. Naveenkhan |

## Technology

- `backend/` — Java 21, Spring Boot 4.1.x, Maven (wrapper included), Thymeleaf officer screens, H2 with seed data.
- `mobile/` — React Native with Expo (TypeScript), the citizen app for UC2.
- External services (push notifications, SMS) are mocked behind gateway interfaces.

The folders above are added in the next set-up stages.

## Branches

| Branch | Purpose |
|---|---|
| `main` | Released code only (the version demonstrated at the viva). |
| `develop` | Integration branch; finished work is merged here. |
| `shared/skeleton` | Shared code used by all four use cases. |
| `feature/uc1-lakmal`, `feature/uc2-afham`, `feature/uc3-weliwitigoda`, `feature/uc4-naveenkhan` | One branch per member and use case. |

Rules: nobody commits directly to `main` or `develop`; branches are merged into `develop` with `--no-ff` after review;
members bring shared changes into their branch by merging `develop` (no rebase of pushed commits); shared files change
only on `shared/skeleton`.
