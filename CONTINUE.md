# Continue Here

## Current checkpoint

**Project:** Memora Android app

**Project folder:** `MemoraApp/`

**Current state:** Android Studio project is created and runs successfully on the
Medium Phone emulator. The app contains a welcome screen and a prototype photo
permission screen. The source-neutral Asset and indexing-state domain contracts are
implemented with local unit tests. Room persistence for Assets and indexing state is
implemented, its schema is exported to version control, and both local and
emulator-backed tests pass. Hilt now owns the application-level Room database and
repository bindings, and the unchanged app launches on the emulator. The app does
**not** yet discover, extract, understand, or search any asset. A source-neutral
Memory and evidence contract is now tested, but no Memory is persisted yet. The app
contains no WorkManager job or AI integration. The MediaStore image adapter is covered
by metadata-mapping and opaque-checkpoint tests and has queried the emulator's granted
MediaStore catalogue successfully. Room database version 2 now persists source-owned
opaque checkpoints and safely migrates version-1 Asset data. Tested application and
Room boundaries can now persist one source-neutral discovery page's Asset placeholders
and checkpoint atomically, while preserving explicit access and source-failure
outcomes. A tested use case can now resume one bounded source-neutral discovery page
from its saved source-owned checkpoint. A Hilt-bound application use case now binds
the read-only MediaStore adapter to that flow for one explicit, bounded page and
reports whether Android granted the full image library or selected photos. It has no
background caller yet, but the Compose setup screen can now invoke one explicit
foreground page. An emulator integration test has verified the same flow persists
one real, bounded emulator page and checkpoint atomically into an isolated in-memory
Room database. A Hilt ViewModel now exposes immutable setup/indexing state and accepts
permission results from the UI and is connected to the Compose setup screen. After a
user grants access and explicitly chooses `Start indexing`, the app persists one
bounded, read-only MediaStore metadata page and shows the truthful count and
full-versus-selected-photo scope. On 2026-07-20 the Medium Phone emulator completed
that flow with 0 permitted items; no image bytes were opened.
A source-neutral document-tree approval contract and Room database version 3 now
persist private SAF tree references. Each approved future folder has an independent,
hashed source identity and therefore independent indexing checkpoint. The Compose
setup flow launches Android's folder picker only after an explanation, persists read
access only when the user selects a folder, and saves the private reference through
its ViewModel. On 2026-07-20 the user connected one emulator folder successfully; no
PDF was opened or indexed. A read-only SAF adapter now freshly verifies that exact
persisted Android grant, then returns one bounded page of immediate-child PDF metadata
as source-neutral Asset placeholders with a source-owned checkpoint. On 2026-07-20,
the Medium Phone emulator ran that adapter successfully against the already approved
folder. It did not open, copy, extract, or index a document.

The visible prototype is not the final product contract. In particular, the prior
idea of importing notes through Share is rejected as the primary workflow because it
does not satisfy automatic source indexing.

On 2026-07-20, the original PRD and both accepted addenda were copied into
`docs/product-source/` with SHA-256 records. The new
`docs/LOCAL_AI_TECHNICAL_SPEC.md` and ADR-012 make the core memory lifecycle
local-first and offline after a required on-device capability is installed. No model,
AI Pack, cloud service, vector index, OCR, extraction implementation, or WorkManager
job exists yet. Existing source discovery remains compatible with this decision.

## Read in this order

1. `AGENTS.md`
2. `docs/PRODUCT_SOURCE_REGISTRY.md`
3. `docs/LOCAL_AI_TECHNICAL_SPEC.md`
4. `docs/GOVERNANCE.md`
5. `docs/PRODUCT_CONTRACT.md`
6. `docs/ARCHITECTURE.md`
7. `docs/DECISIONS.md`
8. `docs/ROADMAP.md`
9. `docs/PRD_TRACEABILITY.md`

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
- Hilt’s generated dependency graph compiled successfully with
  `:app:testDebugUnitTest` on 2026-07-18. The existing automated test task was
  current, and the unchanged welcome screen then launched successfully on the Medium
  Phone emulator.
- The Memory/evidence domain tests passed with `:app:testDebugUnitTest` on
  2026-07-18. The unchanged welcome screen launched successfully afterward on the
  Medium Phone emulator.
- `AssetDiscoverySourceTest` passed in Android Studio on 2026-07-18: 3 of 3 tests
  passed. It verifies bounded discovery, source-owned cursors, and rejection of mixed
  sources or duplicate identities. The emulator remained healthy during the run.
- The corrected `AssetDiscoverySourceTest` passed in Android Studio on 2026-07-18:
  3 of 3 tests passed. Completed and empty pages now retain a durable checkpoint for
  the next incremental pass.
- On 2026-07-19, the local Gradle task passed for the MediaStore metadata mapper and
  checkpoint codec: 5 of 5 selected unit tests passed. The adapter is metadata-only,
  bounded, and inactive.
- `MediaStoreImageDiscoverySourceTest` passed in Android Studio on the Medium Phone
  emulator on 2026-07-19: 1 of 1 test passed after the user granted photo access. It
  verifies a real, read-only bounded MediaStore query; it does not persist or index
  any result.
- `RoomDiscoveryCheckpointRepositoryTest` passed in Android Studio on the Medium
  Phone emulator on 2026-07-19: 2 of 2 tests passed. It verifies source-owned cursor
  round-trip storage and idempotent replacement.
- `MemoraDatabaseMigrationTest` passed in Android Studio on the Medium Phone emulator
  on 2026-07-19: 1 of 1 test passed. It verifies an existing version-1 Asset record
  survives the non-destructive migration to database version 2.
- On 2026-07-19, the local `PersistDiscoveryPageTest` passed: 1 of 1 test verified
  that the application use case delegates the complete source-neutral page to its
  atomic storage boundary.
- `RoomDiscoveryPageStoreTest` passed in Android Studio on the Medium Phone emulator
  on 2026-07-19: 3 of 3 tests passed. It verifies that a page saves its placeholders
  and checkpoint together, unchanged asset versions retain their current indexing
  state, changed versions return to `DISCOVERED`, and an empty completed page advances
  its checkpoint.
- `ProcessDiscoveryResultTest` passed in Android Studio on 2026-07-19: 3 of 3 tests
  passed. It verifies that a successful page is committed, explicit access and source
  failure outcomes are returned without a write, and a persistence exception becomes
  a safe retryable failure.
- `DiscoverSourcePageTest` passed in Android Studio on 2026-07-19: 6 of 6 tests
  passed. It verifies saved-cursor resumption, first-pass discovery without a cursor,
  explicit access handling, source failure handling, exception recovery, and rejection
  of a page from an unexpected source.
- `IndexMediaStoreImagesTest` passed in Android Studio on 2026-07-19: 5 of 5 tests
  passed. It verifies one bounded, explicitly requested page is persisted, full and
  selected-photo access remain distinct, missing access performs no query or write,
  and revoked/failure outcomes remain safe for a later ViewModel.
- `IndexMediaStoreImagesIntegrationTest` passed in Android Studio on the Medium Phone
  emulator on 2026-07-20: 1 of 1 test passed after photo access was granted. It
  verifies a real bounded MediaStore page and its source-owned checkpoint persist
  atomically into an isolated in-memory Room database, without opening or altering
  original media.
- `MediaStoreSetupViewModelTest` passed in Android Studio on 2026-07-20: 4 of 4
  tests passed. It verifies indexing is blocked until both an explicit permission
  result and an explicit user request, selected-photo access stays distinct, recovery
  states remain explicit, and duplicate requests are ignored while work is active.
- `IndexingSummaryTest` passed in Android Studio on 2026-07-20: 3 of 3 tests passed.
  It verifies the completion message remains truthful for full-library access,
  selected-photo access, an incomplete page, and an empty completed page.
- On 2026-07-20, the user ran the app on the Medium Phone emulator, granted access,
  explicitly started indexing, and observed the completed `0 items` full-library
  message. This was expected for the emulator's permitted catalogue.
- Android 17 (API 37.1) currently fails Compose instrumentation tests before their
  assertions because its test environment lacks `InputManager.getInstance`. The
  failure is in the emulator/test-tool bridge, not Memora. The blocked Compose test
  was removed; pure presentation copy is locally tested, and the visible flow is
  manually verified on the emulator. Revisit when the emulator image or Compose test
  tooling is compatible.
- `SafDocumentTreeSourceTest` passed in Android Studio on 2026-07-20: 3 of 3 tests
  passed. It verifies stable, distinct hashed source identities and keeps the raw
  tree URI out of the source ID.
- `RoomDocumentTreeApprovalRepositoryTest` passed on the Medium Phone emulator on
  2026-07-20: 2 of 2 tests passed. It verifies temporary in-memory persistence of
  independent document-tree approval records without accessing a real tree.
- `MemoraDatabaseMigrationTest` passed on the Medium Phone emulator on 2026-07-20:
  1 of 1 test passed after Room database version 3 added the document-tree approval
  table without destructively resetting the version-1 Asset fixture.
- `DocumentTreeSetupViewModelTest` passed in Android Studio on 2026-07-20: 4 of 4
  tests passed. It verifies Android's persisted read grant is the only path that can
  save a folder reference, and platform or persistence failures remain recoverable.
- `ApproveDocumentTreeTest` passed in Android Studio on 2026-07-20: 1 of 1 test
  passed. It verifies the application boundary saves one private approval only after
  it receives a persisted tree URI.
- On 2026-07-20 the user opened the live Android folder picker, selected an emulator
  folder, and observed `PDF folder connected. Memora has not opened or indexed any
  document yet.`
- `SafPdfDiscoverySourceTest` passed locally on 2026-07-20: 5 of 5 tests verify
  fresh-grant revocation, bounded metadata pages, PDF-only filtering, source-owned
  checkpoint rejection, and retryable provider failure handling.
- Android-test compilation passed on 2026-07-20 after the SAF adapter was added.
- `SafPdfDiscoverySourceIntegrationTest` passed in Android Studio on the Medium Phone
  emulator on 2026-07-20: 1 of 1 test queried the existing user-approved folder's
  bounded metadata page. It did not open, modify, copy, extract, or index any PDF.

## Next approved engineering step

Bind the verified SAF PDF adapter to the existing checkpoint-driven discovery use
case so one explicit foreground request can persist one bounded PDF placeholder page
and its source-owned checkpoint atomically. Do not add automatic/background work,
open PDF bytes, extract text, or change the current UI yet. The source must preserve
explicit access-revoked and retryable-failure outcomes.
This remains a discovery-only step; it must use the Local-AI change-control gate and
must not introduce model or cloud behaviour.

## Important open decision

The PRD requires automatic indexing of existing notes but also excludes user accounts
and cloud sync. Android cannot read private data from arbitrary note apps. A truthful
automatic note connector therefore needs a provider-specific, read-only connection
(for example, OneNote) or the MVP source definition must be narrowed. See ADR-003 in
`docs/DECISIONS.md`. Do not claim that all phone notes are automatically indexed until
this is resolved.
