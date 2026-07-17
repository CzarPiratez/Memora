# Continue Here

## Current checkpoint

**Project:** Memora Android app

**Project folder:** `MemoraApp/`

**Current state:** Android Studio project is created and runs successfully on the
Medium Phone emulator. The app contains a welcome screen and a prototype photo
permission screen. The source-neutral Asset and indexing-state domain contracts are
implemented with local unit tests. Room persistence for Assets and indexing state is
implemented, its schema is exported to version control, and both local and
emulator-backed tests pass. The app does **not** yet discover, extract, understand,
or search any asset. It contains no WorkManager job, source adapter, or AI integration.

The visible prototype is not the final product contract. In particular, the prior
idea of importing notes through Share is rejected as the primary workflow because it
does not satisfy automatic source indexing.

## Read in this order

1. `AGENTS.md`
2. `docs/GOVERNANCE.md`
3. `docs/PRODUCT_CONTRACT.md`
4. `docs/ARCHITECTURE.md`
5. `docs/DECISIONS.md`
6. `docs/ROADMAP.md`
7. `docs/PRD_TRACEABILITY.md`

No implementation begins until this read gate is complete and the next change is
mapped to its requirements, acceptance criteria, risks, and verification plan.

## Last verified behavior

- Gradle sync completed in Android Studio.
- The emulator starts and the app installs.
- `:app:testDebugUnitTest` passed after the source-neutral domain foundation was added.
- `:app:testDebugUnitTest` passed after adding Room persistence and exported schema
  version 1.
- `RoomAssetRepositoryTest` passed on the Medium Phone Android emulator on
  2026-07-18: 2 of 2 tests passed. It verifies record round-trip persistence and
  idempotent update behavior for a stable source identity.
- Welcome screen displays the intended privacy-first language.
- Tapping setup can request image access and Android reports the result.

## Next approved engineering step

Create the verified local Git checkpoint for the persistence boundary. Then plan the
next smallest foundation task before adding MediaStore, AI, WorkManager, or new UI.

## Important open decision

The PRD requires automatic indexing of existing notes but also excludes user accounts
and cloud sync. Android cannot read private data from arbitrary note apps. A truthful
automatic note connector therefore needs a provider-specific, read-only connection
(for example, OneNote) or the MVP source definition must be narrowed. See ADR-003 in
`docs/DECISIONS.md`. Do not claim that all phone notes are automatically indexed until
this is resolved.
