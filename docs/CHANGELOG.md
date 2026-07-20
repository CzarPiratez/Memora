# Change Log

## Unreleased

### Verified Compose setup and explicit MediaStore indexing control

- **Requirements:** P-04, P-05, P-14, P-15, P-16, P-17.
- **Delivered:** The Compose setup screen now reports Android permission results to
  the Hilt ViewModel and exposes `Start indexing` only after confirmed access. One
  user-initiated request indexes one bounded metadata-only page, then renders a
  truthful full-library or selected-photo completion message, or a recoverable error.
- **Privacy:** The Activity owns Android's permission launcher. The UI has no direct
  source, Room, or AI calls. The workflow does not open image bytes, modify originals,
  upload content, or schedule background work.
- **Verification:** On 2026-07-20, `IndexingSummaryTest` passed in Android Studio:
  3 of 3 tests passed. The Kotlin, Hilt, Android-test compilation, and local unit-test
  graphs compiled successfully. The user then verified the visible app flow on the
  Medium Phone emulator: it completed with 0 permitted items, a valid empty result.
- **Known limitation:** Android 17 (API 37.1) currently fails Compose
  instrumentation tests before test assertions due to the emulator/test bridge
  expecting the unavailable `InputManager.getInstance` method. This is recorded in
  `CONTINUE.md`; presentation copy is locally tested and the visible flow is manually
  verified until compatible tooling is available.

### Verified setup/indexing ViewModel state boundary

- **Requirements:** P-05, P-14, P-16, P-17.
- **Delivered:** A Hilt ViewModel with immutable state for photo access and one
  explicit indexing request. The ViewModel receives a permission result from the UI;
  it does not request Android permission. It blocks indexing without confirmed access,
  prevents concurrent duplicate requests, preserves selected-photo scope, and exposes
  recovery-safe access or failure outcomes.
- **Dependency note:** Added the official AndroidX Lifecycle ViewModel KTX runtime for
  `viewModelScope`, plus Kotlin coroutines test support for deterministic local state
  tests. Neither dependency adds a permission, network access, source scan, or user
  data collection.
- **Verification:** On 2026-07-20, `MediaStoreSetupViewModelTest` passed in Android
  Studio: 4 of 4 tests passed. The Hilt and Android-test compilation graph passed.
- **Known limitation:** This ViewModel starts one foreground, user-initiated page
  only. Background scheduling and extraction remain future work.

### Verified live MediaStore-to-Room indexing path

- **Requirements:** P-04, P-05, P-14, P-15, P-17.
- **Delivered:** An Android emulator integration test that executes one explicit,
  bounded `MediaStore -> discovery use case -> Room` path using a temporary in-memory
  database. It verifies the returned page's Asset placeholders and opaque checkpoint
  are saved together and that the reported access scope agrees with Android's live
  grant.
- **Privacy:** The test reads only the emulator's existing MediaStore metadata. It
  opens no image bytes, inserts no media, creates no thumbnail, changes no original,
  and leaves no derived records in the normal Memora database.
- **Verification:** On 2026-07-20,
  `IndexMediaStoreImagesIntegrationTest` passed in Android Studio on the Medium Phone
  emulator: 1 of 1 test passed after photo access was granted.
- **Known limitation:** The verified UI starts a foreground page only; background
  work remains intentionally unimplemented.

### Verified controlled MediaStore indexing boundary

- **Requirements:** P-04, P-05, P-14, P-15, P-17.
- **Delivered:** A Hilt-bound application use case that invokes the existing
  checkpoint-driven discovery flow for exactly one explicit, bounded MediaStore page.
  It returns an immutable outcome for a later ViewModel, including the truthful
  distinction between full-library and selected-photo access.
- **Privacy:** This boundary neither requests nor caches a permission, starts itself,
  schedules background work, opens image bytes, creates thumbnails, modifies original
  media, or changes the prototype UI. Without a future explicit caller it is inert.
- **Verification:** On 2026-07-19, `IndexMediaStoreImagesTest` passed in Android
  Studio: 5 of 5 tests passed. The Hilt application and Android-test graphs also
  compiled successfully.
- **Known limitation:** This use case has no user-facing ViewModel or UI caller yet.

### Verified checkpoint-driven discovery invocation

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application use case that checks a source's access state, loads
  only that source's saved opaque checkpoint, requests exactly one bounded page, then
  passes the outcome through the verified result coordinator. It rejects pages that
  claim a different source identity.
- **Privacy:** The use case is inactive until called explicitly, requests no Android
  permission, starts no background work, opens no original content, and has no UI.
- **Verification:** On 2026-07-19, `DiscoverSourcePageTest` passed in Android Studio:
  6 of 6 tests passed.
- **Known limitation:** The real MediaStore adapter is still not bound to this use
  case, so no actual source result is persisted by the running app.

### Verified discovery-result coordination

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application coordinator that sends only a successful discovery
  page to the atomic page-store use case. Missing access, revoked access, and source
  failures remain unchanged and cause no write. A storage exception becomes a
  recoverable, non-sensitive failure outcome.
- **Privacy:** The coordinator opens no source, requests no permission, starts no
  scan, and changes no UI. It does not expose internal storage errors to a user.
- **Verification:** On 2026-07-19, `ProcessDiscoveryResultTest` passed in Android
  Studio: 3 of 3 tests passed.
- **Known limitation:** No source calls this coordinator yet; no actual MediaStore
  discovery page is persisted by the running app.

### Verified atomic discovery-page persistence

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application `PersistDiscoveryPage` use case and a Room-backed
  atomic store. For every successful source-neutral page, it writes all Asset
  placeholders and that page's opaque source checkpoint in one database transaction.
  Unchanged versions retain their current indexing state; changed fingerprints are
  safely returned to `DISCOVERED`.
- **Privacy:** This component receives only source-neutral Asset metadata and opaque
  cursors. It opens no original content, requests no permission, performs no scan,
  starts no background work, and changes no UI.
- **Verification:** On 2026-07-19, local Gradle passed
  `PersistDiscoveryPageTest`: 1 of 1. `RoomDiscoveryPageStoreTest` then passed in
  Android Studio on the Medium Phone emulator: 3 of 3 tests passed.
- **Known limitation:** No platform source invokes this boundary yet; therefore no
  actual MediaStore result is persisted by the app, and restart behavior has not yet
  been verified end-to-end.

### Verified durable discovery checkpoints

- **Requirements:** P-04, P-14, P-17.
- **Delivered:** Room database version 2, a source-owned opaque checkpoint table,
  repository, Hilt binding, exported schema, and explicit migration from version 1.
- **Privacy:** A checkpoint contains only its source ID, opaque source cursor, and
  save time. It contains no original media, thumbnail, text, or semantic memory.
- **Verification:** On 2026-07-19, `RoomDiscoveryCheckpointRepositoryTest` passed in
  Android Studio on the Medium Phone emulator: 2 of 2 tests passed. The real
  `MemoraDatabaseMigrationTest` then passed: 1 of 1 test preserved a version-1 Asset
  across the migration.
- **Known limitation:** This repository is intentionally not yet connected to
  MediaStore discovery. The later atomic page-store boundary is tested, but no
  platform source invokes it yet.

### Verified MediaStore image/screenshot discovery adapter

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17.
- **Delivered:** A bounded, read-only `ContentResolver` query adapter for image and
  screenshot metadata; an opaque MediaStore version/watermark/ID checkpoint; and
  runtime access-state handling for full, selected, missing, and revoked access.
- **Privacy:** The adapter never opens image bytes, creates thumbnails, writes to
  MediaStore, requests location, persists scan results, or starts automatically.
- **Verification:** On 2026-07-19, local Gradle compiled the app and passed 5 of 5
  selected mapper/checkpoint unit tests. `MediaStoreImageDiscoverySourceTest` then
  passed in Android Studio on the Medium Phone emulator: 1 of 1 test passed after
  photo access was granted, proving a real read-only catalogue query.
- **Known limitation:** The adapter is intentionally not injected into a worker or
  UI and no cursor/Asset page is stored in Room yet.

### MediaStore adapter design approved

- **Requirements:** P-03, P-04, P-05, P-15, P-17.
- **Decision:** Query only Android-granted image metadata, distinguish full and
  selected-photo access, and use version/generation checkpoints (ADR-009).
- **Privacy:** No media location, image bytes, thumbnail, write operation, automatic
  scan, or user-content test fixture is permitted in this step.

### Verified incremental checkpoint correction

- **Requirements:** P-04, P-14.
- **Decision:** Completed and empty discovery pages retain a durable source checkpoint
  for the next incremental pass (ADR-008).
- **Scope:** Contract correction and unit tests only. No device media or permission is
  accessed.
- **Verification:** On 2026-07-18, `AssetDiscoverySourceTest` passed in Android
  Studio: 3 of 3 tests passed after the correction.

### Verified source discovery contract

- **Requirements:** P-03, P-04, P-05, P-14, P-15.
- **Decision:** Discovery returns bounded, source-specific Asset pages and explicit
  access/failure outcomes (ADR-008).
- **Scope:** Pure Kotlin contract and unit tests only. No device media, document,
  provider, permission, worker, or UI is accessed or changed.
- **Verification:** On 2026-07-18, `AssetDiscoverySourceTest` passed in Android
  Studio: 3 of 3 tests passed. The emulator remained healthy during the run.

### Verified Memory evidence contract

- **Requirements:** P-09, P-10, P-11, P-12, P-13.
- **Decision:** A Memory is versioned by its Asset and every summary/anchor cites
  stored evidence (ADR-007).
- **Scope:** Pure Kotlin domain model and unit tests only. No source is opened, no AI
  is called, no derived content is persisted, and no visible behavior is changed.
- **Verification:** On 2026-07-18, `:app:testDebugUnitTest` passed, including the
  Memory contract tests. The unchanged welcome screen then launched successfully on
  the Medium Phone emulator.

### Verified dependency-injection foundation

- **Requirement:** P-17.
- **Decision:** Hilt 2.60.1 owns application composition (ADR-006), using the existing
  Android legacy KAPT compatibility bridge. Java 17 is configured as required by the
  current official Hilt/Compose setup guidance.
- **Scope:** Application, Room database/DAO/repository bindings, and the Android
  entry point only. No source access, indexing work, permission, network behavior,
  or visible UI behavior is added.
- **Verification:** On 2026-07-18, `:app:testDebugUnitTest` completed successfully;
  Hilt's generated tasks compiled the application graph. The existing automated test
  task was current. The unchanged welcome screen then launched successfully on the
  Medium Phone emulator.

### Verified persistence boundary

- **Requirements:** P-04, P-05, P-09, P-14, P-17.
- **Decision:** Room 2.8.4 with exported schemas; Android legacy KAPT is used for
  Room generation under the current AGP built-in Kotlin toolchain (ADR-005).
- **Verification status:** Initial KSP configuration was rejected by AGP before
  compilation. The documented compatibility path uses legacy KAPT. The Room schema
  path is supplied explicitly to legacy KAPT because the Room Gradle plugin does not
  propagate it through this compatibility bridge.
- **Verification:** `:app:testDebugUnitTest` passed with the generated Room schema.
  On 2026-07-18, `RoomAssetRepositoryTest` passed on the Medium Phone emulator:
  2 of 2 tests passed. The tests verify record round-trip persistence and idempotent
  upsert behavior for a stable source identity.

### Source-neutral domain foundation

- **Requirements:** P-03, P-04, P-05, P-09, P-14.
- **Layers:** Domain only; no UI, Android platform API, data persistence, network, or
  AI dependency was added.
- **Delivered:** Asset types, immutable source identity/location/fingerprint values,
  read-only source capability contract, and validated recoverable indexing lifecycle.
- **Verification:** `:app:testDebugUnitTest` passed on 2026-07-17. Tests cover identity
  validation, source capability safety, valid indexing flow, retry behavior, and
  rejected invalid transitions.
- **Known limitation:** This does not discover, open, persist, or search real assets.
  The next phase is a Room persistence boundary; automatic existing-note access remains
  blocked by ADR-003.
