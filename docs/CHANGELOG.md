# Change Log

## Unreleased

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
