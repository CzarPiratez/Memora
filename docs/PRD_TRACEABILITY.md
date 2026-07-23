# PRD Traceability Matrix

This is the implementation checklist derived from `Memora.docx`. It is intentionally
product-facing: a feature is not complete merely because code exists; it must satisfy
the behavior described here.

| ID | PRD requirement | Planned owner/layer | Verification evidence | Status |
|---|---|---|---|---|
| P-01 | Memora behaves as a memory retrieval engine, not conventional file search | Product, recall UI | Query can be answered from recall cues rather than filename | Planned |
| P-02 | Lifecycle is Discover -> Extract -> Understand -> Store -> Recall -> Explain | Application architecture | End-to-end test traces one asset through every stage | Planned |
| P-03 | Photos, screenshots, PDFs, and notes are MVP sources | Source adapters | Each approved source produces Asset candidates | Source-neutral discovery contract; MediaStore image/screenshot adapter and emulator query verification; plus user-approved SAF PDF-folder setup and a bounded metadata-only, depth-first SAF PDF descendant adapter-to-Room path. Nested traversal has deterministic local tests and a live Android adapter regression test; approved note adapters remain planned |
| P-04 | Discovery is continuous/incremental and source assets get stable internal identity | Source adapters, Room, WorkManager | Restart/change tests show no duplicates and changed assets requeue | Identity/fingerprint, Room upsert, bounded durable-checkpoint contract, emulator-verified MediaStore querying, Room cursor persistence, non-destructive v1-to-v3 migration, atomic page persistence, result coordination, checkpoint-driven bounded invocation, controlled MediaStore binding, live adapter-to-Room verification, independent SAF tree source identities, and a bounded source-owned depth-first SAF checkpoint with v1 compatibility complete; restart verification using a nested live fixture remains planned |
| P-05 | Discovery knows source facts before it knows semantic content | Domain | Placeholder Asset records contain identity/type/time/location only | Domain/Room placeholder records, source-neutral discovery contract, atomic placeholder-page persistence, result coordination, checkpoint-driven invocation, controlled MediaStore execution, live integration verification, verified Compose setup/indexing control, and live SAF PDF Asset-placeholder discovery and persistence complete; extraction planned |
| P-06 | Images expose deterministic metadata, including OCR and available EXIF/GPS | Extraction | Image fixture tests | Planned |
| P-07 | PDFs expose full text, page count, title, and metadata when available | Extraction | PDF fixture tests | Versioned, source-neutral deterministic PDF extraction contract, reviewed local parser, and synthetic-only mapper are complete. On 2026-07-21 the Medium Phone emulator passed all 4 mapper tests for selectable text/metadata/page count, blank-page representation, no-text truthfulness, password safety, and malformed input. On 2026-07-22, it passed all 6 private isolated-service tests for manifest isolation, a synthetic descriptor parse, no-text, password, malformed, and unsupported-protocol outcomes with caller-side descriptor closure; 10 ordinary-process client tests for bind/death/timeout/cancellation recovery plus strict malformed-response rejection and descriptor closure; 3 private service-binding tests with no parser request; 1 synthetic end-to-end binding/client round-trip test; 10 pure bounded-result contract tests; 10 Android Bundle-codec tests; an expanded 3-test synthetic parser benchmark through a 32-page fixture; and a 2-test offline runtime check. On 2026-07-23, the Medium Phone passed a one-test live isolated-process-death harness: the ordinary process returned an empty retryable failure, closed its descriptor, and became unavailable after only the uniquely identified isolated parser PID was crashed. It also passed a one-test fresh-SAF-grant regression: the exact user-approved tree must retain Android read access before metadata discovery, while a different or readless tree is denied. The grant adapter reads only Android's retained permission list. On the same day, focused Gradle and the user's Android Studio run passed the five-test pure approved-PDF descriptor-custody contract: only an exact approved source plus a freshly granted state yields content-free identity/fingerprint authorization; mismatch, required, revoked, and unavailable states deny it. The Medium Phone then passed the five-test Android synthetic-URI canonical-target boundary: it derives the only future opening URI from an approved tree plus opaque source document ID and denies non-PDF, source-mismatch, invalid-tree, and foreign-location inputs before platform I/O. A debug-only synthetic DocumentsProvider and unbound read-only descriptor broker then passed six emulator tests: fresh approval/grant sequencing, API 29+ membership, duplicate/original closure, and denial paths; the provider is excluded from release and no real PDF can be opened. A subsequent three-test Medium Phone integration run connected that fixture through the unbound broker-to-isolated-parser ownership handoff: exact approved access reached the status-only parser, last-moment revocation prevented a parser submission, and retryable parsing remained non-extraction. The offline check confirms that the release app requests no Internet permission and that the existing local parser can run after Android reports the emulator offline. The benchmark records only aggregate repository-owned fixture measurements and does not establish production limits. Cancellation stops Memora's local wait and does not claim to terminate the isolated process. Real-source descriptor access, representative measured bounded page/text policy, and persistence remain planned. Image-only/scanned PDFs remain an explicit local-OCR follow-up, not a silent completion claim |
| P-08 | Notes expose raw text and permitted source metadata | Note adapter/extraction | Provider fixture tests | Blocked by ADR-003 |
| P-09 | Assets are normalized into a common Memory structure | Domain, Room | Cross-source schema tests | Asset aggregate and evidence-backed Memory domain contract complete; Room persistence planned |
| P-10 | Semantic understanding creates memories/signatures/anchors/summary | Understanding service | Validated structured-output fixtures | Memory signature/anchor contract complete; understanding planned |
| P-11 | The system invests intelligence during indexing, not by repeatedly re-reading files during search | Repository, recall | Search test works from stored memory data | Evidence-backed storage contract complete; repository/recall planned |
| P-12 | Natural-language recall uses evidence-based ranking | Recall engine | Query ranking tests | Evidence-backed recall cues contract complete; ranking planned |
| P-13 | Explain Mode states why a result matched | Recall UI | Explanation references stored evidence fields | Evidence citations contract complete; Explain Mode planned |
| P-14 | Android work is offline-first, recoverable, and background-safe | WorkManager, Room | Interrupted/retry tests | Recoverable state, bounded discovery, access outcomes, durable source checkpoints, atomic page persistence, safe persistence-failure handling, checkpoint-driven invocation, controlled MediaStore binding, live source binding, explicit UI-state recovery modeling, live SAF source-to-isolated-Room persistence, and emulator-verified explicit PDF-folder UI recovery modeling complete; WorkManager planned |
| P-15 | Original files are not edited or deleted | All source adapters | Read-only permission and integration review | MediaStore adapter is metadata-only by implementation and live emulator integration test; SAF setup persists only a user-approved read grant and private folder reference, and the verified SAF adapter-to-Room path queries only bounded metadata without opening documents; remaining source adapters planned |
| P-16 | UI is recognition-first, not a dashboard of technical filters | Compose UI | User review against query/result flows | Privacy-first photo setup/indexing and PDF-folder approval flows are manually verified, including an explicit PDF indexing action and truthful `0 PDF items` result; retrieval UI planned |
| P-17 | MediaStore, Room, WorkManager, Compose, MVVM, repository pattern, and Hilt form the Android foundation | Platform/data/app layers | Architecture review and build | Room/repository/Hilt boundaries complete, including non-destructive Room migrations through version 3, Hilt-bound atomic discovery/page-indexing boundaries, verified Compose permission/indexing, Hilt-bound SAF approval, an emulator-verified Hilt-bound read-only SAF metadata adapter-to-Room use case, and an emulator-verified Hilt ViewModel foreground PDF indexing control; WorkManager planned |
| P-18 | WhatsApp, Gmail, Calendar, video, audio, timeline, cloud sync, collaboration, manual tags, folders, and phone-wide chat remain out of MVP | Scope control | PR review and roadmap check | Accepted |
| P-19 | User accounts are excluded, yet existing notes must be indexed | Product decision | ADR-003 resolved before note connector work | Open conflict |
| A-01 | Core memory creation, retrieval, ranking, and explanation work locally after required on-device capability installation | Local Intelligence Layer | Offline end-to-end verification with network unavailable | Planned |
| A-02 | Cloud AI is optional and never a core dependency | Product, architecture, dependency review | Dependency/data-flow review proves no remote AI path in normal operation | Accepted in specification; no implementation yet |
| A-03 | Models are accessed through replaceable capability interfaces and versioned AI Packs | Domain, application, platform/data | Contract tests, manifest integrity tests, compatibility and fallback tests | Planned |
| A-04 | Understanding is performed per source/model/schema version, not repeatedly at search time | Application, Room, workers | Reindex/version tests and recall test that does not reopen an Asset | Planned |
| A-05 | Local search ranks stored evidence and provides factual explanation | Recall engine, Room/vector index | Offline semantic recall and evidence-citation tests | Planned |
| A-06 | Indexing is bounded, battery-aware, cancellable, and truthful under constraints | WorkManager, UI | Constraint, cancellation, retry, and paused-state tests | Planned |
| A-07 | AI Pack installation/update is explicit, integrity-checked, version-compatible, and does not upload user data | Platform/data, product UI | Offline/install/failure/rollback and disclosure tests | Planned |
| E-01 | Future Event/Knowledge Memories add evidence-backed links without replacing Asset Memories | Domain, application, recall | Contract tests prove source/evidence provenance, reversibility, and non-destructive links | Accepted future architecture; not MVP |
| E-02 | Future links and derived memories remain explainable with evidence, provenance, matching factors, and calibrated uncertainty | Domain, recall, UI | Explain-mode tests reject unsupported links/explanations | Accepted future architecture; not MVP |
| E-03 | Future personalization is local, opt-in, reversible, and distinct from raw Memory meaning | Recall, product/privacy | Feedback lifecycle and ranking tests; user-control review | Planned future architecture; not MVP |
| E-04 | Every Memory has stable identity and traceable revisions; source/model changes do not silently rewrite its history | Domain, data, application | Contract, migration, and revision-history tests | Accepted architecture; planned implementation |
| E-05 | Every user-visible result has a truthful integrity state and “Why this result?” trust view | Domain, application, recall UI | State-transition and evidence/limitation presentation tests | Accepted architecture; planned implementation |
| E-06 | Confidence is calibrated and evaluated; overconfident errors are measured and rejected as a quality regression | Evaluation, understanding, recall | Fixture corpus with calibration and false-confidence reports | Accepted architecture; planned implementation |

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

### P-07 checkpoint correction — data-persistence design gate

On 2026-07-24, `PDF_EXTRACTION_DATA_PERSISTENCE_DESIGN.md` and proposed ADR-020
recorded the mandatory gate before derived PDF page text or metadata can enter Room.
The record defines normalization, provenance identity, atomicity, deletion/revocation,
migration/rollback, encryption/backup, bounded-resource, and verification
requirements. It found that the current manifest backup configuration is still the
Android Studio template default. This is documentation only; P-07 persistence remains
planned and neither real-source parsing nor searchable stored PDF text is enabled.

Before closing any feature, link its tests and visible behavior to at least one ID in
this table. If a proposed feature has no matching requirement, either decline it as
out of scope or record an explicit product decision before implementation.
