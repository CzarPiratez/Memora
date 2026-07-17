# Continue Here

## Current checkpoint

**Project:** Memora Android app

**Project folder:** `MemoraApp/`

**Current state:** Android Studio project is created and runs successfully on the
Medium Phone emulator. The app presently contains a welcome screen and a prototype
photo permission screen. It does **not** yet discover, extract, persist, understand,
or search any asset. It contains no Room database, WorkManager job, source adapter,
or AI integration.

The visible prototype is not the final product contract. In particular, the prior
idea of importing notes through Share is rejected as the primary workflow because it
does not satisfy automatic source indexing.

## Read in this order

1. `AGENTS.md`
2. `docs/PRODUCT_CONTRACT.md`
3. `docs/ARCHITECTURE.md`
4. `docs/DECISIONS.md`
5. `docs/ROADMAP.md`

## Last verified behavior

- Gradle sync completed in Android Studio.
- The emulator starts and the app installs.
- Welcome screen displays the intended privacy-first language.
- Tapping setup can request image access and Android reports the result.

## Next approved engineering step

Create the source-neutral domain foundation only:

- `AssetType`, `Asset`, source capability, indexing status, and immutable source
  identity/fingerprint contracts.
- Unit tests for identity and state transitions.
- No MediaStore query, no AI call, no Room migration, and no UI redesign in that step.

After it runs, show the user the result and wait for feedback.

## Important open decision

The PRD requires automatic indexing of existing notes but also excludes user accounts
and cloud sync. Android cannot read private data from arbitrary note apps. A truthful
automatic note connector therefore needs a provider-specific, read-only connection
(for example, OneNote) or the MVP source definition must be narrowed. See ADR-003 in
`docs/DECISIONS.md`. Do not claim that all phone notes are automatically indexed until
this is resolved.
