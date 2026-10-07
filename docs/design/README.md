# Design documents (read-only copies)

Snapshot taken on 12 October 2026 (standards v2.19, dictionary v0.14) from Group_058's design folder. **Do not
edit these files here.** The master copies live in the group's design project; a change is made there first and
then copied in again on `shared/skeleton` only.

| File or folder | What it is |
|---|---|
| `implementation-standards.md` (v2.19) | Binding rules for all code: stack, packages, naming, SOLID, smells, patterns, logging, exceptions, testing, coverage gates, seed data, per-member contracts (§12). |
| `model-dictionary.md` (v0.14) | The single source of names: every class, attribute, operation and literal the code uses. |
| `change-list.md` (v2.11) | The approved changes CH-01…CH-33; code that implements a change cites its ID in the Javadoc ("Implements CH-nn"). |
| `traceability.md` | Critique finding → change → artefacts; the code and test columns are filled during implementation. |
| `scenarios/` | The five use case scenarios (main, alternate and exception flows); every flow is implemented and tested. |
| `sequence/` | The 20 sequence diagrams (PNG) and their message lists in order (sender, receiver, operation, guard). |
| `wireframes/` | Frame specifications (`frames-WF1.md`…`frames-WF4.md`, `wireframe-spec.md`) and the 27 coloured screens (`png/`) the implemented screens follow. |
| `diagrams/` | Class diagram, UC4 strategies figure and use case diagram (final). |
| `../lectures/` | SE3070 lecture study guides L03 (SOLID, code smells) and L04 (best practices, refactoring) — the marking yardstick for code quality. |
