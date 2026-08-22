# PRD Traceability Matrix

This is the implementation checklist derived from `Memora.docx`. It is intentionally
product-facing: a feature is not complete merely because code exists; it must satisfy
the behavior described here.

| ID | PRD requirement | Planned owner/layer | Verification evidence | Status |
|---|---|---|---|---|
| P-01 | UNFYND behaves as a memory retrieval engine, not conventional file search | Product, recall UI | Query can be answered from recall cues rather than filename | Planned |
| P-02 | Lifecycle is Discover -> Extract -> Understand -> Store -> Recall -> Explain | Application architecture | End-to-end test traces one asset through every stage | Planned |
| P-03 | Photos, screenshots, PDFs, and notes are MVP sources | Source adapters | Each approved source produces Asset candidates | Source-neutral discovery contract; MediaStore image/screenshot adapter and emulator query verification; plus user-approved SAF PDF-folder setup and a bounded metadata-only, depth-first SAF PDF descendant adapter-to-Room path. Nested traversal has deterministic local tests and a live Android adapter regression test; approved note adapters remain planned |
| P-04 | Discovery is continuous/incremental and source assets get stable internal identity | Source adapters, Room, WorkManager | Restart/change tests show no duplicates and changed assets requeue | Identity/fingerprint, Room upsert, bounded durable-checkpoint contract, emulator-verified MediaStore querying, Room cursor persistence, non-destructive v1-to-v3 migration, atomic page persistence, result coordination, checkpoint-driven bounded invocation, controlled MediaStore binding, live adapter-to-Room verification, independent SAF tree source identities, and a bounded source-owned depth-first SAF checkpoint with v1 compatibility complete; restart verification using a nested live fixture remains planned |
| P-05 | Discovery knows source facts before it knows semantic content | Domain | Placeholder Asset records contain identity/type/time/location only | Domain/Room placeholder records, source-neutral discovery contract, atomic placeholder-page persistence, result coordination, checkpoint-driven invocation, controlled MediaStore execution, live integration verification, verified Compose setup/indexing control, and live SAF PDF Asset-placeholder discovery and persistence complete; extraction planned |
| P-06 | Images expose deterministic metadata, including OCR and available EXIF/GPS | Extraction | Image fixture tests | MediaStore EXIF extract for PHOTO/SCREENSHOT complete (2026-07-28). Screenshot OCR and PHOTO OCR use separate bundled Latin ML Kit drains and separate Room tables (`screenshot_ocr_extractions`, `photo_ocr_extractions` v7); current-fingerprint keyword search is available for each corpus. Both are deterministic keyword paths, not semantic Memory recall; emulator verification for PHOTO remains pending |
| P-07 | PDFs expose full text, page count, title, and metadata when available | Extraction | PDF fixture tests | Versioned, source-neutral deterministic PDF extraction contract, reviewed local parser, and synthetic-only mapper are complete. On 2026-07-21 the Medium Phone emulator passed all 4 mapper tests for selectable text/metadata/page count, blank-page representation, no-text truthfulness, password safety, and malformed input. On 2026-07-22, it passed all 6 private isolated-service tests for manifest isolation, a synthetic descriptor parse, no-text, password, malformed, and unsupported-protocol outcomes with caller-side descriptor closure; 10 ordinary-process client tests for bind/death/timeout/cancellation recovery plus strict malformed-response rejection and descriptor closure; 3 private service-binding tests with no parser request; 1 synthetic end-to-end binding/client round-trip test; 10 pure bounded-result contract tests; 10 Android Bundle-codec tests; an expanded 3-test synthetic parser benchmark through a 32-page fixture; and a 2-test offline runtime check. On 2026-07-23, the Medium Phone passed a one-test live isolated-process-death harness: the ordinary process returned an empty retryable failure, closed its descriptor, and became unavailable after only the uniquely identified isolated parser PID was crashed. It also passed a one-test fresh-SAF-grant regression: the exact user-approved tree must retain Android read access before metadata discovery, while a different or readless tree is denied. The grant adapter reads only Android's retained permission list. On the same day, focused Gradle and the user's Android Studio run passed the five-test pure approved-PDF descriptor-custody contract: only an exact approved source plus a freshly granted state yields content-free identity/fingerprint authorization; mismatch, required, revoked, and unavailable states deny it. The Medium Phone then passed the five-test Android synthetic-URI canonical-target boundary: it derives the only future opening URI from an approved tree plus opaque source document ID and denies non-PDF, source-mismatch, invalid-tree, and foreign-location inputs before platform I/O. A debug-only synthetic DocumentsProvider and unbound read-only descriptor broker then passed six emulator tests: fresh approval/grant sequencing, API 29+ membership, duplicate/original closure, and denial paths; the provider is excluded from release and no real PDF can be opened. A subsequent three-test Medium Phone integration run connected that fixture through the unbound broker-to-isolated-parser ownership handoff: exact approved access reached the status-only parser, last-moment revocation prevented a parser submission, and retryable parsing remained non-extraction. The offline check confirms that the release app requests no Internet permission and that the existing local parser can run after Android reports the emulator offline. The benchmark records only aggregate repository-owned fixture measurements and does not establish production limits. Cancellation stops Memora's local wait and does not claim to terminate the isolated process. Real-source descriptor access, representative measured bounded page/text policy, and persistence remain planned. Image-only/scanned PDFs remain an explicit local-OCR follow-up, not a silent completion claim |
| P-08 | Notes expose raw text and permitted source metadata | Note adapter/extraction | Provider fixture tests | ADR-003 accepted; change-control opened (`CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md`). Extract phases N4+ planned; no live note text yet |
| P-09 | Assets are normalized into a common Memory structure | Domain, Room | Cross-source schema tests | Room v8 persists normalized Asset Memory revisions, evidence, extraction-schema provenance, TEXT anchors, and citation joins. The explicit bounded assembler uses current-fingerprint PDF text/metadata, screenshot/photo OCR, and useful EXIF facts only. Unit assembler/mapping coverage is complete; migration/repository emulator execution remains pending |
| P-10 | Semantic understanding creates memories/signatures/anchors/summary | Understanding service | Validated structured-output fixtures | Memory signature/anchor contract complete; understanding planned |
| P-11 | The system invests intelligence during indexing, not by repeatedly re-reading files during search | Repository, recall | Search test works from stored memory data | Deterministic pre-AI Asset Memories now persist entirely from saved extraction rows without reopening originals. Existing PDF/image keyword search remains the interim recall path; semantic Memory recall is planned |
| P-12 | Natural-language recall uses evidence-based ranking | Recall engine | Query ranking tests | Evidence-backed recall cues contract complete; ranking planned |
| P-13 | Explain Mode states why a result matched | Recall UI | Explanation references stored evidence fields | Evidence citations contract complete; Explain Mode planned |
| P-14 | Android work is offline-first, recoverable, and background-safe | WorkManager, Room | Interrupted/retry tests | Recoverable state, bounded discovery, access outcomes, durable source checkpoints, atomic page persistence, safe persistence-failure handling, checkpoint-driven invocation, controlled MediaStore binding, live source binding, explicit UI-state recovery modeling, live SAF source-to-isolated-Room persistence, and emulator-verified explicit PDF-folder UI recovery modeling complete; SAF PDF discovery WorkManager drain verified 2026-07-25 (extract WM deferred) |
| P-15 | Original files are not edited or deleted | All source adapters | Read-only permission and integration review | MediaStore adapter is metadata-only by implementation and live emulator integration test; SAF setup persists only a user-approved read grant and private folder reference, and the verified SAF adapter-to-Room path queries only bounded metadata without opening documents; remaining source adapters planned |
| P-16 | UI is recognition-first, not a dashboard of technical filters | Compose UI | User review against query/result flows | Privacy-first photo setup/indexing and PDF-folder approval flows are manually verified, including an explicit PDF indexing action and truthful `0 PDF items` result; retrieval UI planned |
| P-17 | MediaStore, Room, WorkManager, Compose, MVVM, repository pattern, and Hilt form the Android foundation | Platform/data/app layers | Architecture review and build | Room/repository/Hilt boundaries complete, including non-destructive Room migrations through version 3, Hilt-bound atomic discovery/page-indexing boundaries, verified Compose permission/indexing, Hilt-bound SAF approval, an emulator-verified Hilt-bound read-only SAF metadata adapter-to-Room use case, an emulator-verified Hilt ViewModel PDF indexing control, and SAF PDF discovery WorkManager + HiltWorker verified 2026-07-25 |
| P-18 | WhatsApp, Gmail, Calendar, video, audio, timeline, cloud sync, collaboration, manual tags, folders, and phone-wide chat remain out of MVP | Scope control | PR review and roadmap check | Accepted |
| P-19 | User accounts are excluded, yet existing notes must be indexed | Product decision | ADR-003 Choice 1 (OneNote-class connector; not UNFYND account) | Decision accepted 2026-07-31; connector change-control opened; implementation phased N1–N5 |
| A-01 | Core memory creation, retrieval, ranking, and explanation work locally after required on-device capability installation | Local Intelligence Layer | Offline end-to-end verification with network unavailable | Planned; measured pack baselines L0 opened 2026-08-02 (docs only) |
| A-02 | Cloud AI is optional and never a core dependency | Product, architecture, dependency review | Dependency/data-flow review proves no remote AI path in normal operation | Accepted in specification; no implementation yet |
| A-03 | Models are accessed through replaceable capability interfaces and versioned AI Packs | Domain, application, platform/data | Contract tests, manifest integrity tests, compatibility and fallback tests | Domain Spec §4 interfaces + pack/support contracts unit-tested 2026-07-25; pack download/install still Planned |
| A-04 | Understanding is performed per source/model/schema version, not repeatedly at search time | Application, Room, workers | Reindex/version tests and recall test that does not reopen an Asset | Pre-AI deterministic assembly is keyed by stable Memory identity + Asset fingerprint + assembly/extraction schemas and inserts immutable revisions. Model-backed understanding and recall remain planned |
| A-05 | Local search ranks stored evidence and provides factual explanation | Recall engine, Room/vector index | Offline semantic recall and evidence-citation tests | Keyword Why this result? interim 2026-07-25; semantic recall still Planned; Local-AI benchmark plan accepted ADR-025 |
| A-06 | Indexing is bounded, battery-aware, cancellable, and truthful under constraints | WorkManager, UI | Constraint, cancellation, retry, and paused-state tests | SAF PDF discovery unique work uses battery-not-low; drain/resume/access-stop androidTests verified 2026-07-25; extract WM / full cancellation UI still follow-up |
| A-07 | AI Pack installation/update is explicit, integrity-checked, version-compatible, and does not upload user data | Platform/data, product UI | Offline/install/failure/rollback and disclosure tests | Delivery/security plan (ADR-023) + compatibility/fallback (ADR-024) + benchmark plan (ADR-025) accepted 2026-07-25; measured pack baselines L1 synthetic integrity harness verified 2026-08-02; download/install UI still Planned |
| E-01 | Future Event/Knowledge Memories add evidence-backed links without replacing Asset Memories | Domain, application, recall | Contract tests prove source/evidence provenance, reversibility, and non-destructive links | Accepted future architecture; not MVP |
| E-02 | Future links and derived memories remain explainable with evidence, provenance, matching factors, and calibrated uncertainty | Domain, recall, UI | Explain-mode tests reject unsupported links/explanations | Accepted future architecture; not MVP |
| E-03 | Future personalization is local, opt-in, reversible, and distinct from raw Memory meaning | Recall, product/privacy | Feedback lifecycle and ranking tests; user-control review | Planned future architecture; not MVP |
| E-04 | Every Memory has stable identity and traceable revisions; source/model changes do not silently rewrite its history | Domain, data, application | Contract, migration, and revision-history tests | Implemented for deterministic Asset Memory persistence: stable identity, fingerprint/schema-derived immutable revision identity, conflict-safe insert, and retained prior fingerprints. Model-version supersession policy remains planned |
| E-05 | Every user-visible result has a truthful integrity state and “Why this result?” trust view | Domain, application, recall UI | State-transition and evidence/limitation presentation tests | Accepted architecture; planned implementation |
| E-06 | Confidence is calibrated and evaluated; overconfident errors are measured and rejected as a quality regression | Evaluation, understanding, recall | Fixture corpus with calibration and false-confidence reports | Local-AI benchmark plan accepts calibration metrics (ADR-025); measured pack baselines L0 opened 2026-08-02; corpus/harness still Planned |
| G-01 | Grounded Answers only assert claims supported by retrieved stored evidence, or abstain | Grounding domain/application | Fixture tests: answer / abstain / conflict | Accepted architecture (ADR-033–038); implementation blocked on readiness gates |
| G-02 | Evidence Package is immutable, bounded, coverage-honest, and model-agnostic | Evidence Package Builder | Unit tests for limits, omissions, determinism, checksum | Accepted contract (ADR-034); no production builder yet |
| G-03 | ReasoningEngine is a replaceable capability; v1 task is ANSWER_QUESTION only | Reasoning runtime | Capability availability + contract tests; no UI/SDK leakage | Accepted contract (ADR-036); no generative SDK wired |
| G-04 | Deterministic Verifier rejects unsupported claims; never marketed as hallucination-proof | Verifier | Reject/accept fixtures; no model in verifier | Accepted contract (ADR-037) |
| G-05 | StructuredAnswer carries status + completeness (COMPLETE/PARTIAL/UNKNOWN); no numeric confidence in v1 | Domain, UI mapping | Contract + UI honesty tests | Accepted contract (ADR-038) |
| G-06 | Find remains available; grounded answers do not silently replace Find or invent Why copy | Recall UI, grounding UI | Dual-path honesty review | Accepted (ADR-033/035); UI not built |
| G-07 | Interactive answers are cancellable, process-death safe, single-flight; no Room txn across inference | Application, runtime | Cancel / process recreation tests | Accepted execution rules (ADR-039); not implemented |
| G-08 | First slice is offline PDF saved-text only with citations and non-advice limitations | PDF grounding slice | `GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` | Acceptance spec accepted; implementation blocked |

## Definition of traceable delivery

### P-07 checkpoint correction — bounded synthetic parser transport

On 2026-07-23, protocol version 2 of the private isolated parser service became
emulator-verified through **20 focused tests** across the service, client,
end-to-end-transport, and approved-broker-handoff integration suites. It validates a
strict bounded page/chunk envelope for repository-owned synthetic descriptors, while
the ordinary-process client intentionally discards every chunk and exposes only a
content-free status summary. The temporary synthetic policy is 32 pages, four chunks
per page, 8,192 UTF-16 code-units per page, 65,536 total, and 2,048 per chunk.

This checkpoint is not completion of P-07. It enables no real user PDF, extraction
persistence, search, UI, WorkManager, semantic understanding/AI, or network. Its
single bounded Binder envelope is not the mandatory real-source session/chunk
streaming design, and the limits are not production budgets. ADR-017 keeps real
source parsing disabled.

### P-07 checkpoint correction — Binder protocol v3 session streaming

On 2026-07-25, Medium Phone emulator verification passed **25** focused tests for
protocol v3 `begin` / `nextChunk` / `cancel` against repository-owned synthetic
descriptors. The ordinary client validates through the session assembler and returns
status only. Visible recovery presentation landed separately the same day; real-source
opening remains a separate ADR-017 gate.

### P-07 checkpoint correction — visible local-reading recovery presentation

On 2026-07-25, `PdfLocalReadingCopyTest` **2 of 2** and `PdfLocalReadingSessionTest`
**5 of 5** passed. The connected PDF-folder screen shows scope explanation, progress,
pause/resume/stop, retryable recovery, and an honest unavailable path without opening
any PDF. Real-source descriptor opening remains blocked.

### P-07 checkpoint correction — real-source SAF descriptor open (status-only)

On 2026-07-25, fingerprint revalidation unit tests **3 of 3** and Medium Phone
emulator **11 of 11** passed, including creating one PDF under the user-approved SAF
tree, opening it through the real broker with fresh-grant and fingerprint checks,
parsing in the isolated service, and returning status only. Production UI/Room
searchable wiring remains a separate follow-up.

### P-07 checkpoint correction — pure session/chunk assembler

On 2026-07-25, `IsolatedPdfParserSessionAssemblerTest` passed **8 of 8** local unit
tests against the plan in `docs/PDF_PARSER_SESSION_STREAMING_PLAN.md`. The assembler
validates ordered header/chunk/complete sessions and rejects or cancels without
exposing partial text. Binder protocol v3 later closed the transport half of this
gate for synthetic fixtures only.

### P-07 checkpoint correction — validated domain handoff

On 2026-07-23, a synthetic-only mapper from an already validated isolated-parser
result to the existing in-memory `PdfExtractionOutcome` passed **4 of 4** local unit
tests. It deterministically rejoins complete chunks, retains explicit no-text and
failure truthfulness, and binds an extracted record to the supplied Asset
identity/fingerprint/schema version. The private-parser Medium Phone regression also
passed **20 of 20** Android tests. This does not persist or search extraction data,
open a source, expose UI, add WorkManager or AI, or enable user-source parsing.

### P-07 checkpoint correction — approved parser/extraction assembly

On 2026-07-23, an application-level synthetic consistency gate passed **6 of 6**
local unit tests. It combines a content-free approved parser status with an in-memory
extraction outcome only when Asset identity/fingerprint/schema, page count, and
coverage agree; otherwise it produces an explicit non-extraction outcome. The
existing approved synthetic descriptor handoff also passed **3 of 3** Medium Phone
emulator tests after the parser-status port refactor. This does not transfer page text
from the service, persist data, or enable source access, retrieval, UI, WorkManager,
AI, or real-source parsing.

### P-07 checkpoint correction â€” content-free persistence eligibility

On 2026-07-23, `PrepareApprovedPdfExtractionPersistenceTest` passed **8 of 8**
local unit tests. It permits only exact identity/fingerprint/schema-bound complete or
explicit no-text records to become eligible for a future atomic write, yielding only
content-free page-count, coverage, integrity, lifecycle, and retry facts. Partial,
inconsistent, failed, and access-blocked outcomes are explicitly ineligible. This
adds no Room write, source access, UI, WorkManager, AI, or real-source parsing.

### P-07 checkpoint correction â€” atomic persistence-port contract

On 2026-07-23, `PdfExtractionPersistencePortContractTest` passed **5 of 5** local
unit tests. The future repository port receives only a typed request constructed from
an eligible decision and a record whose identity/fingerprint/schema/page-count/
complete-or-no-text coverage match. It has explicit persisted, retryable, stale, and
safe-failure outcomes but no data implementation, Room write, source access, UI,
WorkManager, AI, or real-source parsing.

### P-07 checkpoint correction â€” persistence-port application coordinator

On 2026-07-23, `PersistApprovedPdfExtractionTest` passed **7 of 7** local unit tests.
It invokes the future persistence port only for a valid eligible decision/record pair,
maps persisted/retryable/stale/safe-failure outcomes explicitly, and proves
ineligible, missing, and mismatched input never reaches the port. It uses a fake port
only and adds no Room write, source access, UI, WorkManager, AI, or real-source parsing.

### P-07 checkpoint correction — data-persistence privacy gate

On 2026-07-24, `PDF_EXTRACTION_DATA_PERSISTENCE_DESIGN.md` and accepted ADR-020
recorded the mandatory gate before derived PDF page text or metadata can enter Room.
The user accepted local-only backup/transfer exclusion, a future Keystore-protected
encrypted-database direction, user-controlled derived-data clearing, and truthful
source-unavailable behavior. The manifest and both Android backup rule formats now
exclude all Memora-private data from cloud backup and device-to-device transfer.
On 2026-07-24, the app assembled and installed on the Medium Phone; its installed
manifest and packaged XML resources verified this configuration. P-07 persistence
remains planned: no Room content write, real-source parsing, or searchable stored PDF
text is enabled.

### Accepted encrypted-database direction and verified synthetic PoC

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Decision:** ADR-021 is accepted. Memora will use SQLCipher-for-Android plus an
  Android-Keystore-wrapped database secret, with frictionless ordinary use and
  plain-language recovery copy that never mentions SQLCipher, keys, or encryption
  failures.
- **Supply chain:** `docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` records Maven
  Central `net.zetetic:sqlcipher-android:4.17.0` hashes and a 2026-07-24 OSV check
  with no listed advisories for that version.
- **PoC verification:** On 2026-07-24, `EncryptedDatabasePocIntegrationTest` passed
  **7 of 7** on the Medium Phone emulator against `memora_encrypted_poc.db` only.
  Resolved versions: SQLCipher `4.17.0`, `androidx.sqlite` `2.6.2`, Room `2.8.4`.
- **Conversion harness:** On 2026-07-24,
  `PlaintextToEncryptedConversionIntegrationTest` passed **5 of 5**, proving
  copy-and-validate, plaintext retention until finalize, empty conversion, validation
  failure safety, and interrupted `ROWS_COPIED` retry.
- **Rollout plan:** `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` records
  PersistenceModule switch criteria, crash-resume, BSD attribution, and rollback.
- **Classpath Slice 1 (2026-07-24):** SQLCipher `4.17.0` and `androidx.sqlite`
  `2.6.2` are on `implementation` after hash/OSV re-check; emulator PoC **7/7** and
  conversion **5/5** still pass. Production opening remains plaintext.
- **Production-named conversion (2026-07-24):**
  `ProductionNamedConversionIntegrationTest` passed **3 of 3**, proving disposable
  `memora.db` → candidate → rename finalize.
- **Live encrypted open + notices (2026-07-24):** `PersistenceModule` opens through
  `MemoraEncryptedDatabaseOpener`; welcome screen ships Open-source licenses with
  SQLCipher Community BSD text. PDF-text persistence remains blocked.
- **Clear derived data (2026-07-24):** user-confirmed Clear Memora index removes only
  Memora-owned DB/wrapper/journal state, reopens a fresh encrypted empty index, and
  shows ADR-021 rebuild copy only (`ClearMemoraDerivedDataIntegrationTest` 1/1).
- **Process-death resume (2026-07-24):** Simulated suite **4 of 4** plus live
  `am crash`/kill suite **2 of 2** (`ConversionLiveProcessDeathIntegrationTest`) close
  rollout proof #3.
- **Low-storage / interruption (2026-07-24):**
  `ConversionLowStorageDenialIntegrationTest` **2 of 2** closes rollout proof #4
  (plaintext retained; `FAILED_SAFE` / `CONVERSION_VALIDATION_FAILED`; retry ok).
- **Physical arm64 device (2026-07-25):** Galaxy A15 5G (`SM-A156E`) suites **13 of
  13** close rollout proof #6 (native load, reopen, wrong-passphrase, conversion).
- **Device unlock + recovery copy (2026-07-25):** deferred open without mutation
  (`DeviceUnlockDeferredOpenIntegrationTest` 2/2); unlock UI; rebuild/unlock jargon
  guards close proofs #5 and #8. BSD notices already closed proof #7 (2026-07-24).
- **Conversion performance budget (2026-07-25):**
  `ConversionPerformanceBenchmarkIntegrationTest` **4 of 4** closes proof #9 with
  provisional ceilings recorded in
  `docs/ENCRYPTED_DATABASE_CONVERSION_BENCHMARK_PLAN.md`.
- **Scope:** No PDF content write, retrieval, explanation, AI, or worker. No
  user-facing encryption/recovery jargon beyond the approved rebuild wording.

### Proposed encrypted-database direction

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Decision:** Historical proposal retained; superseded by ADR-021 acceptance above.
- **Scope:** Documentation only. No encryption dependency, database conversion, Room
  entity/migration, content write, source access, retrieval, explanation, AI, worker,
  or UI has been added.

Before closing any feature, link its tests and visible behavior to at least one ID in
this table. If a proposed feature has no matching requirement, either decline it as
out of scope or record an explicit product decision before implementation.
