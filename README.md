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

## What the repository holds

| Folder or file | What it is |
|---|---|
| `backend/` | The server: Java 21, Spring Boot 4.1.x, Maven (wrapper included), Thymeleaf officer screens, JSON endpoints, H2 database with seed data. The shared skeleton, the browser offline module (`static/js/offline/`) and the stubs of the four use cases are here. |
| `web/` | The Jest test project for the browser scripts under `backend/src/main/resources/static/js/` (Node). It contains no application code. |
| `docs/` | The binding design: `docs/design/` (implementation standards, model dictionary, scenarios, sequence diagrams, wireframes) and the lecture study guides. Read `docs/design/README.md` first. |
| `AGENTS.md`, `CLAUDE.md` | The rules for coding agents; every member's agent reads them before any change. |
| `mobile/` | The citizen app (React Native with Expo, TypeScript). **It arrives in stage S6** and is not in the repository yet. |

External services (push notifications, SMS) are mocked behind gateway interfaces.

## Build, run, test

Needs JDK 21 and Node.js. On Windows use `.\mvnw.cmd`; on macOS or Linux use `./mvnw`.

Back end (from the repository root):

```
cd backend
.\mvnw.cmd clean verify                    # build, tests, Checkstyle, PMD, JaCoCo (warnings do not fail)
.\mvnw.cmd clean verify -Pfinal-check      # the same, and every gate must be met
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The last command starts the seeded demo at <http://localhost:8080/>: the data of the storyline (the closed Kalu
Ganga flood, the live flood with its warning and reports, the shelters) is created at start-up relative to the
current time, so start it fresh for a demo. Seed photos are written to `backend/data/photos/` (git-ignored).

The demo has no login. Each role is a fixed demo user (set in `backend/src/main/resources/application.properties`):

| Role | User |
|---|---|
| DMC Officer | `DMC-0001` (N. Jayasinghe) |
| DMC Duty Officer | `DMC-0002` (K. Perera) |
| District Officer | `DO-COL-01` |
| Resource-owning organisation | `ORG-0001` (CARE Sri Lanka) |
| Citizen (the app) | `CIT-KLT-0001` (Asela Gunawardene) |

Browser script tests:

```
cd web
npm ci
npm test
npm test -- --coverage
```

## Branches

| Branch | Purpose |
|---|---|
| `main` | Released code only (the version demonstrated at the viva). Never commit to it. |
| `develop` | Integration branch; finished work is merged here. Never commit to it; switch to it to read the latest shared code (`git switch develop`). |
| `shared/skeleton` | Shared code used by all four use cases. |
| `feature/uc1-lakmal`, `feature/uc2-afham`, `feature/uc3-weliwitigoda`, `feature/uc4-naveenkhan` | One branch per member and use case: work on your own `feature/…` branch. |

Rules: nobody commits directly to `main` or `develop`; branches are merged into `develop` with `--no-ff` after review;
members bring shared changes into their branch by merging `develop` (no rebase of pushed commits); shared files change
only on `shared/skeleton`.
