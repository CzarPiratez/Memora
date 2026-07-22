# Change Log

## Unreleased

### Verified synthetic approved-PDF parser handoff

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** An unbound application coordinator now connects the approved SAF
  custody broker to the private isolated parser only through a dedicated descriptor
  ownership adapter. The broker retains and closes its borrowed duplicate; the adapter
  duplicates it before transferring ownership to the existing parser client, which
  closes its own handle. The debug-only fixture was upgraded to a valid one-page,
  repository-owned selectable-text PDF.
- **Verification:** On 2026-07-23,
  `ParseApprovedPdfWithIsolatedParserIntegrationTest` completed on the Medium Phone
  emulator: **3 tests passed**. It proves the synthetic approved path reaches the
  private parser, a grant revoked immediately before opening prevents parser
  submission, and a retryable parser status is not presented as extraction.
- **Truthfulness:** The result is content-free and not persisted or searchable. This
  does not open a real user PDF or add UI, Hilt, WorkManager, Room extraction,
  semantic understanding/AI, or network behavior.

### Verified synthetic SAF PDF descriptor broker

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** An unbound data/platform broker, Android tree-membership/read-only
  adapter, and debug-only synthetic DocumentsProvider fixture, excluded from release
  builds. The broker owns original/duplicate descriptor closure and has no UI, Hilt,
  Room, worker, parser, AI, or network binding. API 26-28 returns an explicit safe
  unsupported-platform result and opens nothing.
- **Verification:** On 2026-07-23,
  `SafPdfDescriptorBrokerIntegrationTest` completed on the Medium Phone emulator:
  **6 tests passed**.
- **Truthfulness:** The test uses only a pipe-backed repository fixture. It does not
  request or use a real persisted user grant, and it does not make a user PDF
  eligible for opening or parsing.

### Canonical SAF PDF target boundary

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A SAF data/platform factory now derives an internal future opening
  target from the approved tree plus opaque source document ID. It does not trust a
  stored location as opening authority and rejects non-PDF, source-mismatch, invalid
  tree, and foreign-provider cases before any platform I/O.
- **Verification:** On 2026-07-23, the user ran
  `SafPdfCanonicalDocumentTargetFactoryIntegrationTest` on the Medium Phone emulator:
  **5 tests passed**.
- **Truthfulness:** The test uses five synthetic URI-only cases. This component
  performs no grant validation, provider query, descriptor open, parser call, source
  read, persistence, UI, WorkManager, AI, or network operation.

### Approved Android PDF descriptor-broker design

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** The platform custody plan defines exact SAF approval/grant ordering,
  opaque document-ID handling, Android subtree membership checks, read-only descriptor
  ownership, duplicate transfer to the isolated parser, and a synthetic-fixture test
  matrix.
- **Truthfulness:** Documentation and review only. No descriptor-opening code, source
  read, PDF parse, UI, persistence, WorkManager job, AI capability, permission,
  dependency, or network path was added. Real user-source parsing remains disabled
  under ADR-017.

### Verified approved PDF descriptor-custody gate

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure domain contract now binds a future PDF extraction request to
  the exact approved source ID and a freshly observed source-access state. Its only
  successful output contains the immutable Asset identity and fingerprint; it cannot
  carry a URI, descriptor, stream, source content, or parser handle inward.
- **Verification:** On 2026-07-23, focused Gradle and the user's Android Studio run
  both reported `ApprovedPdfDescriptorCustodyContractTest`: **5 tests passed**.
  The tests cover authorization plus mismatch, access-required, access-revoked, and
  source-unavailable denials.
- **Truthfulness:** No Android source, document, descriptor, parser, Room state, UI,
  worker, AI capability, network, or dependency was accessed or changed. This does
  not enable real-source PDF parsing; ADR-017's platform descriptor-broker gates
  remain required.

### Frozen trust, identity, and quality rules

- **Requirements:** P-09–P-13, A-01–A-05, E-04–E-06.
- **Delivered:** ADR-019 and Experience Memory Amendment v1.1 establish stable
  Memory identity/revisions, “truth before intelligence,” explicit integrity states,
  user-facing `Why this result?`, evidence-support classes, and calibration/
  overconfident-error evaluation requirements.
- **Truthfulness:** Documentation only. The current one-Asset `Memory` code does not
  yet implement Memory IDs, revisions, persistence, state presentation, confidence,
  evaluation, or `Why this result?` UI.
- **Verification:** Cross-reference review completed against the governing product
  documents, current Memory contract, architecture, ADRs, roadmap, and traceability.

### Governed Experience Memory direction and behavioral boundary

- **Requirements:** P-01, P-02, P-09 through P-13, P-18; A-01 through A-05; future
  architecture IDs E-01 through E-03.
- **Delivered:** User-approved `EXPERIENCE_MEMORY_AMENDMENT_V1`, ADR-018, an updated
  Local-AI specification, product capability map, roadmap, and traceability entries.
  They establish Personal Knowledge Infrastructure as Memora's long-term direction:
  Asset Memories remain the MVP foundation; future Event and Knowledge Memories use
  evidence-backed links rather than destructive grouping.
- **Truthfulness:** This is governance and behavioral specification only. It adds no
  source access, AI dependency, model, event detection, timeline, WhatsApp/audio
  access, storage schema, UI, or background work. P-18 exclusions remain in force.
- **Verification:** Documentation cross-reference review against the product source
  registry, Local-AI specification, product contract, architecture, ADRs, roadmap,
  traceability matrix, and current one-Asset `Memory` domain contract.

### Verified fresh SAF read-grant validation boundary

- **Requirements:** P-03, P-05, P-07, P-14, P-15, P-17; Local AI principles A-01,
  A-02, A-06.
- **Delivered:** A source-neutral `DocumentTreeAccessValidator` now checks
  Android's persisted permission list for the exact user-approved document-tree URI
  and a retained read permission. The existing metadata-only SAF discovery adapter
  depends on this new boundary; the metadata catalog no longer owns authorization.
- **Verification:** Kotlin, unit-test, and Android-test compilation passed. The
  focused matcher result contains **3 tests passed** for exact-tree acceptance and
  different-tree/readless-grant rejection. On 2026-07-23, after explicitly
  reconnecting an emulator Documents folder, the user ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator:
  **1 test passed**.
- **Truthfulness:** The Android adapter reads only the retained grant list. It does
  not query a provider, open a user document or descriptor, parse a PDF, persist an
  extraction, call the isolated service, invoke AI, or access a network. This does
  not enable real-source parsing.

### Verified live isolated PDF parser-process death recovery

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A test-only harness binds the existing private isolated parser service,
  holds a synthetic pipe request open, identifies exactly one package-matching process
  with an isolated UID, then induces an Android `am crash <pid>` for that PID alone.
  It adds no production service behavior, permission, Binder method, or debug kill
  switch.
- **Verification:** On 2026-07-23, the user ran
  `LiveIsolatedPdfParserProcessDeathIntegrationTest` on the Medium Phone emulator:
  **1 test passed**. The ordinary process returned a retryable content-free failure,
  closed its supplied descriptor, and marked the Binder connection unavailable.
- **Truthfulness:** This proves a narrow synthetic live-death recovery path only. It
  does not authorize real-source access or prove grant validation, result transport,
  resource limits, persistence, reconnection scheduling, or recovery UI.

### Verified offline synthetic PDF parser runtime

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A synthetic-only Android test now confirms that the release app
  requests no `INTERNET` permission, then permits the existing local PDF parser to
  run only after Android reports the emulator has no Internet-capable or validated
  network. It obtains network-state visibility through a temporary test-shell identity
  and drops that identity before parsing; neither app manifest gains a network
  permission or a network client.
- **Verification:** On 2026-07-22, the user ran
  `PdfParserOfflineRuntimeIntegrationTest` on the offline Medium Phone emulator:
  **2 tests passed**. It parsed only a repository-owned fixture and accessed no user
  source, descriptor, URI, SAF tree, Room data, service, UI, WorkManager, AI, or
  network.
- **Truthfulness:** This closes only the deterministic parser's narrow offline runtime
  check. It does not authorize real-source parsing or prove future source access,
  isolated-service behavior, persistence, or semantic understanding offline.

### Verified many-page synthetic PDF parser baseline

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A test-only benchmark plan and Android harness measure aggregate
  input bytes, page count, extracted-text UTF-16 code units, future-result Bundle
  size, and parser elapsed-time range after a warm-up and five measured runs. The
  expanded corpus generates small, medium, larger, and 32-page repository-owned PDFs
  entirely in memory, then materializes input bytes before timing begins. It contains
  no user source access, source text logging, service binding, Room, UI, WorkManager,
  or AI.
- **Verification:** On 2026-07-22, the connected Medium Phone emulator ran
  `PdfParserSyntheticBenchmarkIntegrationTest`: 3 of 3 tests passed. The new
  32-page fixture produced 65,536 text code units; all measurements are recorded in
  `docs/PDF_PARSER_BENCHMARK_PLAN.md` and remain harness evidence only, not a
  production policy.

### Verified strict parser-result Bundle codec

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A future-only Android `Bundle` decoder accepts exactly the approved
  version-one keys and field types, rejects unexpected or missing fields, then feeds
  the existing bounded-result validator. It returns no candidate text when decoding
  or validation fails.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserResultBundleCodecIntegrationTest` on the Medium Phone emulator:
  10 of 10 tests passed. The live isolated service still returns status only and this
  change opens no descriptor or source.

### Verified bounded parser-result contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A pure validator now defines the future versioned page-text result
  shape. It requires complete page/chunk coverage and an injected, explicit limit for
  page count, chunks per page, page-text UTF-16 code units, and total-text UTF-16
  code units. Rejection exposes no candidate content; a later transport must map it
  to a retryable content-free outcome.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserResultContractTest` in Android Studio: 10 of 10 tests passed.
  The current Binder service still returns status only; this change opens no
  descriptor or source and enables no PDF content return.

### Verified isolated parser end-to-end transport

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** One Android test connects the existing private Binder adapter to the
  existing ordinary-process client, then passes only a repository-owned pipe
  descriptor through the isolated service. It asserts a validated status-only result
  and ordinary-process descriptor closure.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserEndToEndIntegrationTest` on the Medium Phone emulator: 1 of 1
  test passed. This does not enable SAF access, real-source parsing, text chunks,
  Room persistence, UI, or AI.

### Verified isolated-parser client cancellation contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic ordinary-process client accepts an Android
  `CancellationSignal`. Cancellation before submission prevents a parser call;
  cancellation while waiting cancels the client-side future and returns a
  content-free retryable result after descriptor closure.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 10 of 10
  tests passed. This does not claim to terminate a live isolated process or enable
  real PDF access.

### Verified private parser-service binding contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A private Android binding adapter can expose only the existing
  isolated parser Binder. It reports connecting, available, or retryable-unavailable
  status and accepts no descriptor, URI, path, source identity, or parser request.
- **Verification:** On 2026-07-22, the user ran
  `AndroidIsolatedPdfParserConnectionIntegrationTest` on the Medium Phone emulator:
  3 of 3 tests passed. It proves a live private binding, explicit bind failure, and
  explicit disconnection callback without parser work.

### Verified malformed parser-response handling

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic ordinary-process client test now covers unknown
  outcomes, a false isolated-process claim, missing page-count data, and an invalid
  page count. Each must become a content-free retryable failure and close its
  descriptor.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 8 of 8
  tests passed. This remains a transport-validation step only; it does not enable
  real PDF access, Binder page/text chunks, or UI.

### Verified ordinary-process parser recovery contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A private, synthetic-only client contract maps bind failure,
  simulated Binder death, bounded timeout, interruption, and malformed transport
  responses to content-free retryable results. It returns only a validated outcome,
  retryability, and optional page count, and closes every supplied descriptor.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 4 of 4
  tests passed. This does not enable real PDF access, a live service connection,
  real process-death testing, cancellation, chunking, offline checks, persistence,
  or visible recovery UI.

### PDF parser isolation guardrail

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Decision:** ADR-017 requires all future real-PDF parsing to run in a private
  Android isolated service. The normal app verifies one persisted SAF read grant and
  passes one read-only descriptor; the parser service receives neither URI/path nor
  broad source access.
- **Delivered:** The documented threat model is now represented by a private,
  non-exported `android:isolatedProcess` service and a fixed-version descriptor-only
  Binder interface. The service receives no URI, path, source identity, metadata,
  extracted text, Room access, Hilt graph, UI, or network permission. It currently
  accepts only a repository-owned synthetic descriptor and returns a small parser
  status summary. No SAF source access, UI, Room change, worker, AI capability, or
  real user document path has been added.
- **Verification:** Android-test APK compilation passed on 2026-07-21. The user ran
  `IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: 2 of 2
  tests passed. They prove the private/isolated manifest configuration and a two-page
  synthetic descriptor parse. Offline verification, no-text/password/malformed inputs,
  descriptor cleanup under every failure, service death, cancellation, timeout
  reporting, chunk validation, source access, persistence, and visible
  progress/recovery are still required before real-source enablement.

### Expanded isolated PDF parser safety cases

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic-only service test now covers no extractable text,
  password protection, malformed input, an unsupported protocol version, and
  client-side descriptor cleanup after every Binder call. The worker now also closes
  its received descriptor if submission or execution fails. Its result is still a
  tiny status summary only: no source text, title, metadata, URI, path, or identity.
- **Verification:** Android-test APK compilation passed on 2026-07-21. On
  2026-07-22, the user ran the expanded class on the Medium Phone emulator: 6 of 6
  tests passed. This does not verify service death, cancellation, timeout, output
  chunking, offline operation, actual source access, persistence, or UI recovery.

### Local PDF parser and fixture decision

- **Requirements:** P-05, P-07, P-14, P-15, P-17; scanned-PDF OCR remains a future
  Local AI capability.
- **Decision:** ADR-016 selects PDFBox-Android 2.0.27.0 as the local parser behind
  the existing domain port, subject to explicit licensing, dependency, supply-chain,
  fixture, isolated-process, resource-measurement, offline, and emulator gates. Its
  vulnerable declared Bouncy Castle 1.72 dependencies are explicitly overridden with
  the reviewed 1.84 set.
- **Delivered:** The data-layer mapper and generated synthetic-only Android test
  fixtures exist. No SAF URI, connected folder, Room record, UI, worker, or real user
  document is wired to the mapper.
- **Verification:** Dependency graph, SBOM, OSV review, third-party notices, debug
  APK build-size baseline (`12,423,988` to `18,734,630` bytes), and Android-test APK
  compilation are complete. On 2026-07-21, a failed initial emulator run exposed
  accidental leading patch markers in the repository-owned Base64 fixture payloads;
  all four corrected payloads then passed independent Base64 validation. The user
  reran `PdfBoxPdfDocumentMapperIntegrationTest` on the Medium Phone emulator: 4 of
  4 tests passed. Command-line Android tools still could not see that emulator.
- **Truthfulness:** Text-layer PDFs can become complete page-level extraction records;
  scanned/image-only PDFs remain `NoExtractableText` until a separately governed local
  OCR capability is delivered. This records the P-07 gap rather than hiding it.
- **Verification:** Governing-document/current-code review, official Android API and
  compatibility review, and Git diff check. No source content, app behavior, or test
  suite changed in this documentation-only decision.

### Deterministic PDF extraction contract

- **Requirements:** P-05, P-07, P-14, P-15; this step does not implement Local AI.
- **Decision:** ADR-015 defines a versioned record that ties deterministic PDF facts
  to the exact source identity, fingerprint, and extraction schema. It distinguishes
  complete, partial, and no-text-layer coverage rather than silently treating a PDF as
  fully extracted.
- **Delivered:** Pure Kotlin PDF request, record, coverage, and recoverable outcome
  contracts, plus an inward-facing platform-extractor boundary. No data/platform
  adapter exists yet.
- **Privacy:** No document is opened, copied, uploaded, persisted, changed, or
  deleted. No parser, model, cloud path, background work, or UI behavior is added.
- **Verification:** On 2026-07-21, local Gradle passed `PdfExtractionTest`: 6 of 6
  tests cover PDF-only input, source-version binding, complete-page coverage, partial
  coverage, no-text truthfulness, and recoverable failure construction.
  Android-facing verification is not required for this pure domain contract because
  the app's runtime behavior is unchanged.
- **Known limitation:** The local PDF parser, privacy-safe fixtures, Room persistence,
  platform adapter, and emulator test remain separate steps.

### Verified explicit PDF-folder indexing control

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-16, P-17. This step does not
  implement Local AI, extraction, or a cloud path.
- **Decision:** ADR-014 restores the most recently approved private folder reference
  into the setup screen but never scans it automatically. The user explicitly starts
  one bounded metadata page and explicitly chooses any later page.
- **Delivered:** A Hilt ViewModel owns restored connection, indexing, completed,
  retryable-failure, and access-recovery state. Compose only renders that immutable
  state and forwards actions; it does not access Room, SAF, or PDF content.
- **Verification:** Local ViewModel and presentation-copy tests passed on 2026-07-21.
  Android Hilt/test compilation passed. The user then launched the app on the Medium
  Phone emulator, restored the connected folder, explicitly selected `Index this
  folder`, and observed the truthful completed `0 PDF items` state.
- **Known limitation:** This interim screen activates the most recently connected
  folder. A future source-management experience must let people view/select all
  independently connected folders. PDF extraction and background scheduling remain
  intentionally out of scope.

### Verified SAF descendant traversal

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17; no Local-AI requirement is
  implemented by this step.
- **Decision:** ADR-013 replaces root-only traversal with a resumable depth-first
  checkpoint strategy. One invocation reads children from exactly one pending folder;
  it never recurses unboundedly in a single call.
- **Delivered:** The SAF adapter's v2 checkpoint stores pending folder frames and
  their source-owned cursors, discovers declared PDF metadata in descendant folders,
  and reads a prior v1 root checkpoint safely. An empty provider page that claims more
  data becomes an explicit retryable failure.
- **Privacy:** The implementation reads metadata only and changes no source content.
  It does not open a PDF, read bytes/text, copy a document, request broader access,
  start automatically, or schedule background work.
- **Verification:** Local `SafPdfDiscoverySourceTest` passed on 2026-07-20, including
  deterministic nested traversal and v1-resume coverage. Android-test compilation
  passed. The user then reran `SafPdfDiscoverySourceIntegrationTest` on the Medium
  Phone emulator: 1 of 1 test passed after reconnecting the approved folder.
- **Known limitation:** The emulator is not assumed to contain a nested PDF fixture,
  so its live test confirms platform access/regression while the nested logic remains
  deterministically covered locally. Background scheduling and PDF extraction remain
  future work.

### Verified SAF PDF metadata page persistence

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17; A-01 and A-02 remain
  unaffected because this has no model, cloud, or network path.
- **Delivered:** `IndexSafPdfFolder` accepts one exact private approved-folder
  source ID, constructs a source-neutral adapter through an injected factory, and
  delegates one bounded result to the existing atomic discovery-page persistence
  boundary. Its immutable outcome distinguishes unconnected source, required/revoked
  access, retryable failure, and success.
- **Architecture:** The application layer depends only on domain contracts. The
  Android SAF catalog/factory is Hilt-bound in the data layer; no composable is
  changed and no layer opens a PDF.
- **Verification:** Local `IndexSafPdfFolderTest` and Android-test compilation passed
  on 2026-07-20. The user then ran `IndexSafPdfFolderIntegrationTest` on the Medium
  Phone emulator: 1 of 1 test passed against the already approved folder. It read one
  bounded metadata page and wrote only its placeholders and checkpoint to an isolated
  in-memory Room database.
- **Known limitation:** This is one explicit bounded metadata page per invocation.
  Descendant traversal is now handled through its source-owned checkpoint (ADR-013),
  but PDF bytes/text extraction, background scheduling, and UI initiation remain
  deliberately out of scope.

### Local-first engineering governance checkpoint

- **Delivered:** Preserved immutable, versioned repository copies of the original
  PRD and both accepted addenda in `docs/product-source/`, with SHA-256 values in
  `docs/PRODUCT_SOURCE_REGISTRY.md`.
- **Decision:** Added ADR-012 and `docs/LOCAL_AI_TECHNICAL_SPEC.md`. Normal memory
  creation, retrieval, ranking, and explanation are now governed as local-first and
  offline after required on-device capability installation. Cloud AI is optional and
  cannot become a core dependency.
- **Process:** Strengthened the mandatory pre-work gate. Every meaningful delivery
  must use the product registry, local-AI specification, current-code inspection,
  traceability IDs, and `docs/CHANGE_CONTROL_TEMPLATE.md`; conversational memory is
  not an authority.
- **Scope:** Documentation and source-artifact checkpoint only. No Android code,
  dependencies, permissions, source access, model, network client, or user-visible
  behaviour changed.

### Verified bounded SAF PDF metadata discovery

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17.
- **Decision:** ADR-011 records the bounded immediate-child implementation boundary,
  the explicit persisted-grant check, and the required future descendant-traversal
  work. It is not a claim that all nested PDFs are already discoverable.
- **Delivered:** A read-only SAF platform catalog and source adapter now verify the
  exact retained Android read grant for each connected source, query one bounded page
  of immediate-child metadata, emit declared PDFs as source-neutral Asset
  placeholders, and produce a private source-owned checkpoint. Revocation and
  provider errors are explicit outcomes rather than an empty folder.
- **Privacy:** The adapter does not request a permission, open a document, read PDF
  bytes or text, copy source data, persist a discovery page, start automatically, or
  schedule background work.
- **Verification:** On 2026-07-20, local `SafPdfDiscoverySourceTest` passed: 5 of 5
  tests. Android-test compilation passed. The user then ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator: 1 of 1 test
  passed against the already approved folder.
- **Known limitation:** It currently discovers immediate children only; resumable
  nested-folder traversal, persistence, extraction, and background scheduling remain
  future work.

### Verified user-approved SAF PDF-folder connection

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-16, P-17.
- **Decision:** ADR-010 establishes each approved SAF document tree as an
  independently resumable source with a SHA-256-derived source ID. Room database
  version 3 stores the private URI reference and approval time required by a later
  platform adapter.
- **Delivered:** The privacy-explained Compose setup screen launches Android's
  `ACTION_OPEN_DOCUMENT_TREE` picker. It persists only the chosen tree's read grant,
  then asks a Hilt ViewModel and application use case to save the private reference.
  The UI never accesses Room directly.
- **Privacy:** This flow does not enumerate a tree, open a PDF, copy content, request
  broad storage access, or start background work. The raw tree URI remains private
  database data and does not appear in source IDs.
- **Verification:** On 2026-07-20, `SafDocumentTreeSourceTest` passed in Android
  Studio: 3 of 3 tests. `RoomDocumentTreeApprovalRepositoryTest` then passed on the
  Medium Phone emulator: 2 of 2 tests. `MemoraDatabaseMigrationTest` passed: 1 of 1
  test verified the original Asset fixture survives the version-3 migration.
  `DocumentTreeSetupViewModelTest` passed: 4 of 4 tests, and
  `ApproveDocumentTreeTest` passed: 1 of 1. The user then selected an emulator folder
  through Android's live picker and observed the truthful connected state.
- **Known limitation:** No document enumeration or PDF discovery exists yet. The
  connected folder remains inert until the later, bounded read-only SAF adapter.

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
