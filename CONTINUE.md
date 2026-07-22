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

The verified SAF PDF adapter is now bound behind a source-neutral factory to one
explicit application use case. Given the private source ID of an already approved
folder, it requests one bounded, metadata-only page through the existing checkpoint
flow and atomically persists its PDF placeholders and source-owned checkpoint. It
reports source-not-connected, access-required, access-revoked, and retryable failure
outcomes without writing a page. Local `IndexSafPdfFolderTest` passed and the Android
integration-test source compiled on 2026-07-20. The user then ran
`IndexSafPdfFolderIntegrationTest` on the Medium Phone emulator: 1 of 1 test passed.
The live test queried only the already approved folder's bounded PDF metadata page and
persisted only its placeholders and checkpoint to an isolated in-memory Room database.
The SAF adapter now has a depth-first, source-owned v2 traversal checkpoint so it can
resume nested folders through one bounded metadata page per invocation; prior v1
root-only checkpoints remain readable. Local traversal tests and Android-test
compilation passed on 2026-07-20. The user then reran
`SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator: 1 of 1 test
passed after reconnecting the approved folder. That regression test verifies live
Android access; deterministic local tests verify the nested-folder behavior because
the emulator folder is not assumed to contain a nested PDF fixture.
A Hilt ViewModel now restores the most recently approved PDF-folder source ID from
Memora's private database, then waits for an explicit user request before it invokes
the existing bounded PDF indexing use case. It exposes truthful in-progress,
completed, retryable-failure, and revoked-connection states. Local ViewModel and
presentation-copy tests passed on 2026-07-21. The user then ran the app on the Medium
Phone emulator, restored the connected folder, explicitly started indexing, and saw
the truthful completed `0 PDF items` result. No PDF was opened, copied, uploaded,
edited, or deleted.

A pure, source-neutral deterministic PDF extraction contract now binds every future
extraction record to the Asset identity, immutable fingerprint, and schema version.
It represents source-provided title, page count, metadata, and page-level text while
making complete coverage, partial coverage, and no-text-layer outcomes distinct. It
also reserves explicit access and retry outcomes for a later read-only platform
adapter. No user document was opened, no model dependency was added, and no record is
persisted yet.

The reviewed local PDFBox mapper now operates only on already-supplied synthetic
repository-owned streams. On 2026-07-21, the user ran its Android integration test on
the Medium Phone emulator: all 4 tests passed. They cover complete selectable-text
extraction including a represented blank page, image-only no-text truthfulness,
password protection, and malformed input. This mapper is not connected to a SAF URI,
a real folder, Room persistence, UI, WorkManager, or an AI capability; no user PDF was
opened or altered.

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
- `IndexSafPdfFolderTest` passed locally on 2026-07-20. Android-test compilation also
  passed. The user then ran `IndexSafPdfFolderIntegrationTest` on the Medium Phone
  emulator: 1 of 1 test passed. It verifies one real, bounded, read-only SAF PDF
  metadata page persists its placeholders and source-owned checkpoint atomically into
  an isolated in-memory Room database, without opening, copying, extracting, changing,
  or deleting a PDF.
- `SafPdfDiscoverySourceTest` passed locally on 2026-07-20 after descendant traversal
  was added. It verifies depth-first nested-folder resumption, bounded folder pages,
  v1 root-checkpoint compatibility, access revocation, and safe failure behavior.
  The user then reran `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone
  emulator: 1 of 1 test passed after reconnecting the approved folder. It confirms
  the changed adapter remains metadata-only under a live persisted Android grant.
- On 2026-07-21, local Gradle ran `PdfExtractionTest`: 6 of 6 tests passed. It
  verifies PDF-only requests, asset/fingerprint/schema binding, complete coverage,
  partial coverage, no-text-layer truthfulness, and recoverable failure construction.
  The app has no changed Android-facing behavior in this domain-only step.

## Next approved engineering step

The synthetic isolated-service smoke test is verified. On 2026-07-21, the user ran
`IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: **2 tests
passed**. It bound the private, non-exported `android:isolatedProcess` service and
parsed a repository-owned two-page PDF sent through a pipe, with no URI, path, SAF
folder, real user PDF, Room record, or UI action.

The expanded isolated-service test is verified. On 2026-07-22, the user ran
`IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: **6 tests
passed**. It verifies the private isolated manifest, selectable-text success,
image-only/no-text truthfulness, password-protected output, malformed-input
retryability, unsupported-protocol rejection, and caller-side descriptor cleanup after
each request. It has no real PDF, folder, URI, source identity, Room, UI, WorkManager,
or AI dependency.

## Next approved engineering step

The synthetic cancellation suite is verified. On 2026-07-22, the user ran
`IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: **10 tests
passed**. It proves that an already-cancelled request never reaches the parser and
that cancelling while the ordinary process waits returns a content-free retryable
failure and closes the descriptor. It does not claim to hard-kill the isolated
service, and it opens no real source.

## Next approved engineering step

The synthetic end-to-end transport test is verified. On 2026-07-22, the user ran
`IsolatedPdfParserEndToEndIntegrationTest` on the Medium Phone emulator: **1 test
passed**. It explicitly bound `AndroidIsolatedPdfParserConnection`, gave
`IsolatedPdfParserClient` only a repository-owned pipe descriptor, and verified the
private service returned a validated status-only result with caller-side descriptor
closure. It used no SAF URI, user PDF, path, source identity, Room, UI, WorkManager,
or AI.

## Next approved engineering step

The pure bounded-result contract is verified. On 2026-07-22, the user ran
`IsolatedPdfParserResultContractTest` in Android Studio: **10 tests passed**. It
verifies the future page-text response contract: known schema version, complete page
coverage, final chunk sequence, injected maximum page/chunk/page-text/total-text
limits, no-text truthfulness, and content-free rejection of malformed data. It uses
only synthetic strings and has no Binder service change, PDF descriptor, SAF URI,
user source, Room, UI, WorkManager, or AI.

## Next approved engineering step

The strict future result codec is verified. On 2026-07-22, the user ran
`IsolatedPdfParserResultBundleCodecIntegrationTest` on the Medium Phone emulator:
**10 tests passed**. It translates exactly-versioned synthetic `Bundle` fields into
the already-verified pure result contract, rejecting unknown, missing, malformed, and
over-limit data without exposing candidate text. It does not change the isolated
service, open a descriptor, access a SAF URI or user source, persist Room data, alter
UI, schedule WorkManager, or invoke AI.

## Next approved engineering step

The synthetic-only benchmark plan and Android harness are verified. On 2026-07-22,
`PdfParserSyntheticBenchmarkIntegrationTest` completed on the connected Medium Phone
emulator: **2 tests passed**. It measured repository fixtures after one warm-up and
five runs, logging only page count, text code-unit count, serialized future-result
size, and parser elapsed time. The initial values are recorded in
`docs/PDF_PARSER_BENCHMARK_PLAN.md`. It logged no text and opened no descriptor, URI,
SAF or user source. It did not alter the service protocol, source access, Room, UI,
WorkManager, or AI. This is a measurement baseline, not a production limit or
user-facing performance promise. A live in-flight parser-process-death check,
offline check, fresh-grant verification, source access, persistence, and user-visible
recovery remain separate ADR-017 gates.

## Next approved engineering step

The expanded synthetic PDF corpus is verified. On 2026-07-22, the user ran
`PdfParserSyntheticBenchmarkIntegrationTest` on the Medium Phone emulator: **3 tests
passed**. The corpus deterministically generated only three repository-owned PDFs in
memory: one 1,024-code-unit page, four 2,048-code-unit pages, and eight
4,096-code-unit pages. The benchmark materialized every fixture before timing parser
work, emitted only aggregate input-byte/page/text/result-size/elapsed-time metrics,
and verified the expected growth relationship. Recorded measurements are in
`docs/PDF_PARSER_BENCHMARK_PLAN.md`. It did not bind or change the isolated service,
open a descriptor/URI/SAF or user source, persist Room data, alter UI, schedule
WorkManager, or invoke AI. These test shapes are not production limits and do not
close ADR-017's memory, battery, thermal, offline, live-process-death, fresh-grant,
persistence, or recovery gates.

## Next approved engineering step

Design the next privacy-safe representative benchmark corpus and measurement method
before choosing any production parser policy. It must extend the evidence to
many-page, blank/no-text, password-protected, malformed, cancellation, timeout,
memory-pressure, battery/thermal, and offline cases while maintaining the existing
strict boundary: no real user PDF, SAF URI, source identity, Room persistence,
service protocol change, UI change, WorkManager, AI, network, or claimed production
limit. Record the proposal in `docs/PDF_PARSER_BENCHMARK_PLAN.md` and then implement
only the smallest approved synthetic test fixture or harness change.

## Important open decision

The PRD requires automatic indexing of existing notes but also excludes user accounts
and cloud sync. Android cannot read private data from arbitrary note apps. A truthful
automatic note connector therefore needs a provider-specific, read-only connection
(for example, OneNote) or the MVP source definition must be narrowed. See ADR-003 in
`docs/DECISIONS.md`. Do not claim that all phone notes are automatically indexed until
this is resolved.
