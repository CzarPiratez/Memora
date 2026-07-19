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
UI caller yet, so the running app has not persisted a normal MediaStore discovery
page. An emulator integration test has verified the same flow persists one real,
bounded emulator page and checkpoint atomically into an isolated in-memory Room
database. A Hilt ViewModel now exposes immutable setup/indexing state and accepts
permission results from the UI, but the current prototype screen does not yet use it.
No image bytes were opened.

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
- Welcome screen displays the intended privacy-first language.
- Tapping setup can request image access and Android reports the result.

## Next approved engineering step

Connect the existing privacy/setup screen to the verified Hilt ViewModel without a
visual redesign. The Activity/Compose layer must keep ownership of Android's permission
launcher, report the result to the ViewModel, and expose one explicit `Start indexing`
action only after access is confirmed. Render truthful full-versus-selected scope,
success, and recoverable errors. Do not add background work or extraction.

## Important open decision

The PRD requires automatic indexing of existing notes but also excludes user accounts
and cloud sync. Android cannot read private data from arbitrary note apps. A truthful
automatic note connector therefore needs a provider-specific, read-only connection
(for example, OneNote) or the MVP source definition must be narrowed. See ADR-003 in
`docs/DECISIONS.md`. Do not claim that all phone notes are automatically indexed until
this is resolved.
