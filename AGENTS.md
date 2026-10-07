# Rules for coding agents in this repository (Group_058, SE3070 Assignment 02)

These rules apply to every member's coding agent, whatever the tool. Read them before any change.

## Sources of truth
- `docs/design/implementation-standards.md` is binding for all code. Read §1–§11 before writing code, and your
  use case's section of §12 (contracts).
- `docs/design/model-dictionary.md` is the single source of names. Every class, attribute, operation, parameter and
  enum literal it marks ✓ or S exists in the code with exactly that name and signature. Never invent a design-level
  class, method or field that is not in it; implementation-only types are allowed only as the standards describe
  (DTOs, mappers, configuration, web adapters, and the classes listed in model-dictionary §6). If something seems
  missing, stop and report it — do not add it.
- Code follows the sequence diagrams' message order and guards (`docs/design/sequence/*-messages.md`) and every
  flow of the use case's scenario (`docs/design/scenarios/`).
- Screens follow `docs/design/wireframes/` (frame files and `png/`).
- Code that implements a change cites it in the Javadoc: "Implements CH-nn" (`docs/design/change-list.md`).
- Do not edit anything under `docs/`, `AGENTS.md` or `CLAUDE.md`.

## Git
- Do not run git commands that create commits, branches, merges or tags, push, or change history, unless the
  developer explicitly asks in this session.
- Work only on your own branch. Never commit to `main` or `develop`.

## Ownership
- Change only your own use case's packages (standards §3 package map and §8 package → gate → owner table).
- Shared files — `pom.xml`, `application*.properties`, `schema.sql`, the seed data, `shared.*` packages, the layout
  templates and static assets — change only on `shared/skeleton`. If your use case needs a change there, report it
  instead of making it.
- A method in another member's class whose body throws "not implemented yet" belongs to that member: do not fill it
  in; mock it in your tests.

## Quality (marked against docs/lectures/L03 and L04)
- Javadoc on every class and every public method; SLF4J logging; constructor injection; no magic numbers; no
  `System.out`; no commented-out code; no unused members.
- Tests: JUnit 5 + Mockito + AssertJ (backend), Jest (JavaScript/TypeScript). Every scenario flow has a test named
  after its behaviour, with the flow ID in `@DisplayName`. Coverage above 80 % line and branch for your packages.
- Before reporting a task as done, run the build and the tests and show their output.
