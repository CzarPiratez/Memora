# Architecture Decision Record

## ADR-001: Memora indexes sources; it is not an import inbox

**Status:** Accepted

**Decision:** The primary workflow is continuous, permissioned source discovery and
incremental indexing. Manual per-item sharing or uploading is not a substitute for a
source adapter.

**Reason:** This preserves the PRD's search-engine model and its Discover -> Extract
-> Understand lifecycle.

## ADR-002: Original source content remains read-only

**Status:** Accepted

**Decision:** Memora stores references and derived memory data. It never edits,
renames, moves, or deletes original assets.

**Reason:** Privacy and user trust are product requirements.

## ADR-003: Existing third-party notes versus no accounts/cloud sync

**Status:** Accepted (2026-07-31)

**Facts:**

- The MVP requires automatic indexing of Notes.
- The PRD excludes user accounts and cloud sync.
- Android cannot permit Memora to scan another app's private note database.

**Decision:** Choice 1 — implement **one narrowly scoped, read-only provider
connector** (first target: Microsoft OneNote) as an explicit source authorization
flow. This is not a Memora user account, not cloud sync of Memora data, and not
write-back into the note provider.

**Rejected for MVP:**

- Treating Android Share / manual per-note import as the primary Notes indexing
  model (violates ADR-001).
- Claiming arbitrary note apps are automatically indexed (impossible on Android
  without a provider API).
- Removing Notes from MVP without a PRD amendment (choice 3).

**Deferred alternative:** Choice 2 (SAF note files in user-approved folders) remains
a fallback if the OneNote-class connector cannot be delivered under privacy and
offline constraints; it requires a separate change-control before implementation.

**Rule:** Do not implement or advertise automatic indexing of arbitrary note apps.
UI and docs must say Notes indexing needs an approved connector until that
connector ships. Notes implementation code may land only under
`docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md` (opened 2026-07-31), phase by
phase after acceptance.

## ADR-004: Platform access is source-specific

**Status:** Accepted

**Decision:** Use MediaStore for images/screenshots, persisted Storage Access Framework
permissions for PDFs, and a provider adapter for external notes.

**Reason:** A single broad storage permission is neither sufficient nor appropriate on
modern Android.

## ADR-005: Room code generation uses Android legacy KAPT temporarily

**Status:** Accepted with review trigger

**Decision:** Use the `com.android.legacy-kapt` bridge for Room's compiler while this
project uses AGP built-in Kotlin.

**Reason:** The current KSP plugin attempts to configure generated Kotlin source sets
in a way that AGP built-in Kotlin rejects. Android's official migration guidance
provides legacy KAPT for this compatibility case. We will not suppress the AGP safety
check or disable built-in Kotlin merely to make KSP compile.

**Review trigger:** Re-evaluate KSP when a compatible plugin/toolchain combination is
available. This choice affects build-time code generation only; it does not affect the
app's runtime data or privacy model. While legacy KAPT is in use, the Room schema path
is passed explicitly to the annotation processor and the generated schema JSON remains
version-controlled.

## ADR-006: Hilt owns application composition

**Status:** Accepted

**Decision:** Use Hilt to create application-scoped Android dependencies. The first
binding module creates the Room database, its DAO, and the `AssetRepository` behind
the domain interface. It does not inject persistence into a composable or allow UI
code to access source content directly.

**Reason:** The PRD requires Hilt and the architecture requires the UI, application,
domain, and data layers to remain separately testable. Hilt provides compile-time
validation of this dependency graph without adding user-visible behavior.

**Privacy and data impact:** Hilt creates no account, network connection, permission,
source scan, or copy of original user content. Room retains only Memora-owned derived
records when a later use case explicitly requests the repository.

**Verification plan:** Compile the generated graph, run the existing unit and Room
tests, and launch the unchanged welcome screen on the Android emulator.

## ADR-007: Memories must be evidence-backed and versioned by their Asset

**Status:** Accepted

**Decision:** A searchable `Memory` is a separate domain model bound to the stable
identity and fingerprint of the Asset version from which it was created. Its summary
and every recall anchor must cite one or more evidence items stored in that Memory.

**Reason:** A memory retrieval engine must explain why a result matched from actual
source-derived evidence. This boundary prevents an understanding or recall component
from presenting an unsupported summary or recall cue as a fact.

**Scope:** This is a pure domain contract only. It does not decide how evidence is
extracted, call AI, read a source, persist Memory rows, or expose a search UI.

## ADR-008: Discovery is bounded, incremental, and explicit about access

**Status:** Accepted

**Decision:** Every source adapter implements one read-only discovery contract. It
returns source-neutral Asset pages and source-owned opaque cursors in bounded batches.
Every page, including a completed or empty page, returns a durable checkpoint for the
next incremental pass. It reports access required, access revoked, and recoverable
failure as explicit outcomes; it must never present them as an empty source.

**Reason:** The product requires continuous incremental discovery without duplicate
work, silent data loss, or misleading status after permissions are changed. A durable
completion checkpoint prevents a later pass from accidentally becoming a full rescan.
A cursor is source-specific because MediaStore, document trees, and providers cannot
safely share an assumed checkpoint format.

**Scope:** This contract does not open a source, request a permission, persist a
cursor, invoke a worker, or index a discovered Asset. Those responsibilities remain
in later platform, data, and application layers.

## ADR-009: MediaStore image discovery is read-only and permission-scoped

**Status:** Accepted

**Decision:** The first platform adapter will query only `MediaStore.Images` metadata
after a fresh permission check. It will request image access only (not video), support
Android 14+ selected-photo access through `READ_MEDIA_VISUAL_USER_SELECTED`, and
return only the media items currently granted by Android. It will not request media
location, open image bytes, generate thumbnails, edit files, or start automatically.

**Incremental strategy:** A MediaStore cursor will contain the MediaStore version and
generation checkpoint. A version change forces a safe full rescan; otherwise the
adapter queries forward in bounded generation/ID order. Identity is volume plus media
ID; the fingerprint includes the source generation and size. Screenshot classification
is deterministic from the MediaStore display name or relative path only.

**User-visible behavior:** Full, selected, missing, and revoked access are distinct
states. Selected access is not described as full-library indexing, and a user-initiated
control will be required before Android shows a reselection prompt.

**Verification plan:** Unit-test cursor/row mapping and screenshot classification;
then run an emulator integration test using only emulator-provided media. No user
content is needed for automated tests.

## ADR-010: Each approved SAF document tree is a distinct, private source

**Status:** Accepted

**Decision:** A user-approved Storage Access Framework document tree will be
represented as one distinct Memora source. Its `SourceId` is derived from a SHA-256
hash of the persisted tree URI, while Memora's private Room database retains the URI
and approval timestamp needed by the future platform adapter. The Android platform,
not Memora, remains the authority for whether the persisted URI permission is still
valid.

**Reason:** Different approved folders can be indexed, resumed, or revoked
independently. A stable hashed identity prevents the raw folder URI from becoming a
general source identifier or appearing in checkpoints, logs, or user-facing state.

**Privacy and scope:** Android's `ACTION_OPEN_DOCUMENT_TREE` picker is launched only
after the user sees the folder-scope explanation. Memora persists only Android's read
grant for the selected tree and stores its private reference. It does not enumerate
documents, open PDF bytes, extract text, copy a file, request broad storage
permission, schedule work, or enable manual per-file import. A later SAF adapter must
freshly verify the platform-held persisted grant and report revocation explicitly.

**Verification plan:** Unit-test deterministic source identity creation and
source-ownership validation, then run an emulator Room test for approval persistence
and idempotent replacement. No user folder or PDF is required.

## ADR-011: SAF PDF discovery is metadata-only, page-bounded, and starts at the selected tree root

**Status:** Accepted

**Decision:** The first SAF discovery adapter will freshly compare the stored private
tree reference against Android's persisted read grants before every query. It will
query only immediate children of the selected tree root, request a provider-side page
limit, consume at most one bounded metadata page, and return only documents whose
declared MIME type is `application/pdf`. It will build source-neutral PDF Asset
placeholders and an opaque source-owned cursor from document metadata. It will not
open a document URI, read PDF bytes, inspect text, copy a file, index automatically,
or schedule work.

**Reason:** Android's child-document API returns immediate descendants. Beginning with
one bounded root page lets Memora verify access, identity, cursor, and revocation
semantics without disguising an unbounded recursive scan as a safe operation. The
provider-side limit is requested and Memora also stops consuming rows after the
bounded page even if a provider does not honor that hint.

**Known gap and user impact:** Nested subfolders are not yet traversed by this
incremental adapter; this is an implementation milestone, not completion of the PDF
MVP. A later documented step must add resumable descendant traversal (or a supported
provider subtree query) before Memora can claim that it discovers every PDF beneath a
connected folder. Users can connect separate folders for separate locations today.

**Access semantics:** A saved source whose exact persisted Android read grant is no
longer present reports `AccessRevoked`, never an empty PDF page. A provider or cursor
failure reports a retryable structured failure. Existing source content remains
read-only in every outcome.

**Verification plan:** Unit-test grant loss, source-owned cursor validation, bounded
metadata-to-Asset mapping, non-PDF filtering, and provider failure handling; then
run a read-only emulator test against the already user-approved tree. The test must
not create, alter, open, or delete any document.

## ADR-012: Local-first AI and offline core are binding architecture

**Status:** Accepted

**Decision:** The original PRD remains the immutable product baseline. Addendum 1 is
accepted as the implementation amendment for any AI architecture conflict, and
`docs/LOCAL_AI_TECHNICAL_SPEC.md` is the binding, testable engineering interpretation.
Memora's normal memory creation, retrieval, ranking, and explanation paths execute
locally after a required capability is installed. Cloud AI is an optional future
enhancement, never a core dependency.

**Reason:** Memora's trust proposition is that the phone itself remembers. A
cloud-required AI pipeline would contradict its offline, privacy-first, source-owned
product model. Capability interfaces and versioned packs preserve future model choice
without coupling the domain to a vendor or a named model.

**Clarifications:** “AI runs once” means once per source fingerprint plus relevant
extraction, schema, and model version. “Memories live forever” means durable until
the user removes the source/data or a retention policy requires removal. Query-only
local encoding is allowed for semantic search; original assets must not be reopened or
reanalysed during normal recall.

**Consequences:** No AI SDK, model, model download, vector index, WorkManager AI job,
or network feature may be added without satisfying the Local AI Technical
Specification acceptance gate. Existing discovery work remains compatible because it
is read-only, bounded, local, and independent of AI.

## ADR-013: SAF descendant traversal is depth-first, resumable, and metadata-only

**Status:** Accepted

**Decision:** The SAF PDF adapter extends from selected-root-only discovery to a
source-owned, depth-first traversal of the approved document tree. Each invocation
reads the immediate children of exactly one pending folder, using Android's
child-document API and the existing bounded page limit. The opaque v2 checkpoint
stores the pending folder frames and each frame's last consumed child document ID.
When a page discovers folders, it schedules them depth-first; when a folder is
finished, its frame is removed. The prior v1 root-only checkpoint remains readable
and resumes as the root frame.

**Reason:** The product cannot claim a connected folder covers PDFs in nested folders
while only the root is queried. A depth-first continuation checkpoint lets Memora
move through descendants over small, restart-safe calls without an unbounded recursive
query or a provider-specific subtree assumption.

**Privacy and scope:** This remains metadata-only. The adapter reads only document
IDs, MIME types, names, sizes, and modification times from the existing approved
tree. It never opens a document URI, reads bytes or text, copies content, requests
broader access, starts automatically, schedules background work, or changes source
content. A provider page claiming more rows while returning no rows is an explicit
retryable failure rather than an endless or silent scan.

**Known limitation:** The traversal is not yet background-scheduled or wired to a
new visible control. Large-provider behavior and restart continuation across an
actual nested emulator tree still require live verification. PDF extraction remains
separate Phase 2 work.

**Verification plan:** Unit-test root and nested-folder traversal, v1 checkpoint
compatibility, bounded requests, grant loss, and failure handling; compile Android
tests; then rerun the existing read-only SAF adapter integration test on the approved
emulator folder. No test creates, opens, modifies, copies, extracts, or deletes a
source document.

## ADR-014: PDF folder indexing is explicit and restores one recent connection

**Status:** Accepted

**Decision:** The PDF setup screen restores the most recently approved SAF folder
reference from Memora's private database when the app starts. It exposes an explicit
`Index this folder` action that invokes exactly one bounded metadata page through the
existing application use case. If more metadata remains, the screen offers a separate
`Index next page` action. No folder is scanned merely because the app launches or the
connection is restored.

**Reason:** A connected folder must remain usable after a restart, while the product's
privacy promise requires the user—not the UI or application lifecycle—to start each
foreground indexing step. The bounded next-page control makes incomplete discovery
truthful and prevents an unbounded foreground scan.

**Multiple connections:** Every approved folder remains independently stored and
resumable. Until a dedicated source-management screen is designed, this setup screen
uses the most recently approved folder as its active foreground source. It does not
claim that all connected folders have been indexed; users can explicitly connect a
different folder to make it active.

**Privacy and scope:** Restoring a connection reads only Memora's private source ID
and approval metadata. It does not query the folder or expose its URI. Indexing reads
only one bounded page of already-approved metadata and retains all existing read-only
SAF constraints. No PDF bytes/text, model, cloud path, background work, or source
mutation is added.

**Verification plan:** Unit-test restoration, no indexing before explicit action,
successful bounded outcome, revoked-access recovery, provider-failure retry copy, and
truthful completion copy; then launch the app on the emulator, connect or restore a
folder, press the explicit action, and verify the visible outcome.

**Verification:** On 2026-07-21, the focused local ViewModel and presentation-copy
tests passed, Android Hilt/test compilation passed, and the user verified the visible
flow on the Medium Phone emulator. A restored approved folder remained idle until the
user selected `Index this folder`, then displayed a truthful completed `0 PDF items`
outcome. No source document was opened or changed.

## ADR-015: PDF extraction is versioned, local, and explicit about coverage

**Status:** Accepted

**Decision:** Deterministic PDF extraction begins with a source-neutral domain
contract. Every record is bound to one Asset identity, immutable fingerprint, and
extraction-schema version. It can contain source-provided title, page count, named
metadata, and numbered page text. The contract distinguishes complete page coverage,
partial coverage with an explicit reason, and a document with no extractable text.
Access-required, access-revoked, and recoverable-failure outcomes are also explicit.

**Reason:** The PRD requires PDFs to expose full text, page count, title, and
available metadata. A future platform adapter must not confuse an interrupted read or
a scanned/no-text-layer PDF with a complete record. Binding output to the exact source
fingerprint prevents old extracted facts from being reused for a changed PDF.

**Privacy and scope:** This contract does not open a PDF, add a parser or AI/model
dependency, write a database record, schedule background work, or change the UI. A
later Android adapter will freshly validate the existing SAF grant before reading only
the approved document locally. It must never upload, mutate, move, delete, or treat
the source document as Memora-owned. Parser selection, fixture corpus, persistence,
and UI diagnostics remain separate, reviewable steps.

**Verification plan:** Unit-test PDF-only input, Asset/fingerprint/schema binding,
complete page coverage, explicit partial coverage, no-text-layer truthfulness, and
recoverable failure construction. The next implementation step must select a local
parser and fixture strategy under the dependency and privacy gates before source bytes
are read.

## ADR-016: Initial local PDF text parser and fixture strategy

**Status:** Accepted; synthetic-fixture mapper verified

**Decision:** The first deterministic PDF extraction adapter will use the pinned
PDFBox-Android `2.0.27.0` library behind `PdfDeterministicExtractor`, subject to the
documented supply-chain, licensing, fixture, measured-resource, and emulator gates in
`docs/PDF_EXTRACTION_IMPLEMENTATION_PLAN.md`. Its declared Bouncy Castle `1.72`
transitives are rejected by the documented OSV review; the provider, PKIX, and utility
artifacts are pinned explicitly to `1.84`. This ADR does not authorize any real-source
adapter, Hilt binding, or user-document opening.

**Reason:** Memora currently supports API 26. Android's framework exposes page-text
content on API 35, with its compatible pre-V implementation covering API 30 through
34. The current AndroidX PDF library is alpha and begins at API 28. Neither gives a
single P-07 text extraction path across Memora's current Android support range.
PDFBox-Android is local, Android-targeted, Apache-2.0 licensed for its main code, and
supports deterministic metadata/text extraction.

**Scope and truthfulness:** The selected parser must not make scanned or image-only
PDFs appear text-searchable; it returns `NoExtractableText` until a separately
governed local OCR capability exists. Password-protected, malformed, revoked, and
resource-limited inputs are explicit outcomes. A complete result must represent every
page. These are P-07 implementation constraints, not a reduction of P-07.

**Privacy and security:** The future adapter freshly verifies the exact persisted
SAF read grant, reads only locally, runs off the UI thread, and never mutates,
copies, uploads, renames, moves, or deletes original content. The implementation must
review an isolated-process strategy for untrusted PDF parsing before it enables real
user documents.

**Verification:** On 2026-07-21, the user ran
`PdfBoxPdfDocumentMapperIntegrationTest` on the Medium Phone emulator: 4 of 4 tests
passed. They prove complete selectable-text extraction with a represented blank page,
truthful no-text handling, password-safe failure, and malformed-input failure using
repository-owned synthetic streams. The tests neither use a SAF URI nor open a user
document. An offline runtime check and an accepted isolated-process review remain
required before any real-source adapter can be enabled.

## ADR-017: Real PDF parsing requires a descriptor-only isolated service

**Status:** Accepted architecture guardrail; synthetic boundary emulator-verified;
real-source parsing remains disabled

**Decision:** Before any real user PDF is parsed, Memora will use a private,
non-exported Android service with `android:isolatedProcess="true"`. The ordinary app
process alone verifies the exact persisted SAF read grant and supplies one duplicated,
read-only `ParcelFileDescriptor` over a private Binder contract. The isolated parser
receives no source URI, tree URI, path, source identity, Room access, Hilt graph,
network permission, or UI capability. It returns only bounded deterministic parser
facts; the ordinary process validates and constructs the domain record.

**Reason:** PDFs are untrusted input. Android's PDF-renderer guidance recommends a
separate isolated process for untrusted PDF handling, and the service manifest
contract provides an isolated process with no permissions of its own. A single
descriptor is the least-privilege bridge that preserves local, read-only processing.

**Consequences:** The private service and fixed-version descriptor-only Binder
boundary now exist for repository-owned synthetic PDF bytes, but this does not enable
real-source parsing. Android-test APK compilation passed and the Medium Phone
emulator passed the initial two-test synthetic boundary check, the expanded six-test
deterministic outcome suite, a ten-test ordinary-process client suite, and three
private binding-adapter tests. The client maps synthetic bind failure, simulated
Binder death, timeout, malformed response data, and cancellation before or while
waiting to a content-free retryable failure and closes the supplied descriptor.
Cancellation stops Memora's local wait; it is not a claim that Android immediately
terminates an isolated process. Measured resource budgets, an offline runtime check,
live service-death recovery, a bounded result protocol, atomic persistence design,
and an explicit user-facing flow remain mandatory gates.
The explicit private binding adapter and ordinary-process client are now
emulator-verified together through one repository-owned descriptor; this still does
not enable a real source. A versioned, bounded page/text result protocol remains a
mandatory gate before the service can return content. Its first pure validation
contract accepts injected limits rather than unmeasured production constants, requires
complete page/chunk coverage, and emits no candidate content when it rejects a
malformed result. On 2026-07-22, the user verified all ten contract tests in Android
Studio. No partial result becomes searchable. A strict Android Bundle codec now feeds
the same validator.

On 2026-07-23, that codec became protocol version 2 of the private isolated service
for repository-owned synthetic descriptors only. The service emits a strictly
validated, bounded page/chunk envelope and the ordinary-process client validates it
before discarding every chunk and returning its existing content-free status summary.
The temporary synthetic policy is 32 pages, four chunks per page, 8,192 UTF-16
code-units per page, 65,536 total, and 2,048 per chunk. Twenty focused emulator tests
passed across the service, client, end-to-end transport, and approved-broker handoff.
Those limits are not measured production policy. One bounded Binder response is also
not the required real-source session/chunk streaming protocol; that remains a
mandatory ADR-017 gate before any user PDF can return text. No result is persisted,
searchable, or exposed to UI. An initial synthetic-only Android benchmark baseline
records aggregate two-page and no-text fixture measurements, but establishes no
production limit. Measured limit selection remains mandatory before any service
change that could return user-source page text. The full threat model, rejected alternatives, and
implementation gates are in
`docs/PDF_PARSER_ISOLATION_REVIEW.md`.

On 2026-07-23, the Medium Phone emulator passed the one-test live isolated-process
death harness. It retained a synthetic-only in-flight pipe request, identified exactly
one isolated-UID process, and used the instrumentation shell to crash that PID alone.
The ordinary process converted the resulting Binder loss into a retryable,
content-free failure, closed its descriptor, and marked the connection unavailable.
This verifies live service-death recovery without changing the production Binder
interface, service behavior, or app permissions. Representative resource budgets,
real-source descriptor access, atomic persistence, and visible recovery remain
mandatory gates.

On 2026-07-23, the Medium Phone emulator passed the one-test fresh-SAF-grant
regression after the user explicitly reconnected an emulator Documents folder. The
new source-neutral `DocumentTreeAccessValidator` confirms the exact private tree
reference still has an Android persisted read grant before discovery. Its Android
adapter reads only the retained permission list; it does not contact a provider,
open a document or descriptor, parse a PDF, persist an extraction, bind the parser,
or access a network. The pure matcher also rejects a different tree and an exact
tree lacking read permission. This closes the narrow fresh-grant validation gate
only; it does not authorize real-source descriptor opening or parsing.

For the required offline-runtime verification, the instrumentation test temporarily
adopts Android's test-shell identity for the normal `ACCESS_NETWORK_STATE` permission
only while it inspects the emulator's current network capabilities. It drops that
identity before parsing the repository-owned synthetic fixture. The production and
test-APK manifests remain unchanged: neither declares `ACCESS_NETWORK_STATE` nor
`INTERNET`. The test has no network client and makes no network request. This is
preferable to a manually supplied “offline” flag, which would not prove the runtime
condition, or adding network-state visibility to either shipped package.

On 2026-07-25, Binder protocol v3 (`begin` / `nextChunk` / `cancel`) streamed
bounded chunks for repository-owned synthetic descriptors on the Medium Phone
emulator (**25** focused isolation/handoff/process-death tests). The ordinary client
assembles through `IsolatedPdfParserSessionAssembler` and still returns status only.
The visible foreground recovery presentation (scope, progress, pause, retry,
unavailable) landed the same day with unit-tested copy and session transitions; it
does not open user PDFs. On 2026-07-25, fingerprint revalidation plus a live
approved-tree descriptor open through the broker and isolated parser returned
status only (**11** focused emulator tests). On 2026-07-25, the Local PDF reading
product UI was wired to one foreground parse of the first indexed PDF, then later
the same day retained validated wire text and persisted eligible extracted page text
to Room for search on-device (persist integration **1/1**). Search UI and
WorkManager remain separate follow-ups.

## ADR-018: Memora evolves through evidence-backed linking, not destructive grouping

**Status:** Accepted product-direction architecture; future phases only

**Decision:** Memora's long-term destination is Personal Knowledge Infrastructure,
not a conventional file browser or a static embedding index. The MVP foundation is an
evidence-backed **Asset Memory** derived from one Asset version. Future Event Memories
and Knowledge Memories may reference Asset Memories only through versioned,
evidence-backed links. They must not replace, delete, or silently merge the underlying
Asset Memories or original user sources.

**Reason:** People recall events, projects, decisions, relationships, and other
meaningful context—not merely individual files. At the same time, false associations
are harmful in a personal retrieval product. "Linking" preserves provenance and makes
uncertainty visible; "grouping" implies a certainty and ownership that Memora cannot
honestly claim.

**Trust contract:** Every future link or derived memory must expose, in appropriate
user-facing form, its supporting evidence, source/provenance, matching factors, and
calibrated confidence or uncertainty. Explain Mode may never fabricate a relationship
or disclose invented model reasoning. Embeddings are candidate-retrieval aids, never
sufficient evidence for a durable relationship.

**Memory evolution:** A derived Memory may gain a newer version when permitted new
evidence, a changed source, or an approved schema/model change justifies it. The prior
provenance and reason for supersession remain recoverable. A model update cannot
silently rewrite a user's completed Memory.

**Scope boundary:** This direction does not alter the current PRD MVP or P-18.
WhatsApp, audio, automatic experience detection/timeline generation, and broad
cross-source fusion are not now enabled. Future source access still needs its own
approved provider/privacy contract. Local, opt-in, reversible personalization is a
future design requirement, not a current behavior.

**Consequences:** The technical architecture remains `UI -> application -> domain ->
data/platform`. The product capability map is now Acquisition -> Understanding ->
Memory -> Retrieval and Trust -> Experience. Future implementation begins with
behavioral contracts and pure tests before storage, AI, or UI work.

## ADR-019: Truth before intelligence; stable memory identity and trust evaluation

**Status:** Accepted product and engineering rule

**Decision:** Memora adopts “truth before intelligence” as a binding rule: no
evidence means no assertion, and uncalibrated confidence means no precise confidence
claim. Every future Memory receives a stable Memora-owned identity and immutable,
traceable revisions. Source fingerprints, summaries, embeddings, observations,
confidence, and explanations may improve only through a documented revision; they do
not silently rewrite the Memory's history.

**Integrity:** Future persistence and UI must distinguish awaiting permission,
queued, indexing, ready, stale/re-index-required, source unavailable, failed safely,
and removed. “Needs attention” may be a friendly UI summary, never a loss of the
underlying actionable state.

**Trust surface:** The user-facing label is “Why this result?” rather than a promise
of generic AI explanation. It presents the available matching cues, evidence, safe
source provenance, uncertainty, freshness, availability, and limitations. Search
returns evidence-backed results, not unsupported chatbot answers.

**Quality:** Evidence is classified as direct, validated observation, retrieval
signal, or hypothesis. These classes describe provenance/support, not guaranteed
truth. Any future confidence display needs a representative evaluation corpus and
must measure calibration and overconfident errors alongside recall quality,
false-link rate, and explanation coverage.

**Consequences:** This decision adds no source, permission, model, storage schema,
UI, network path, or current-MVP feature. It adds E-04 through E-06 to the
traceability matrix. The next implementation of Memory persistence/creation must
start with a small pure identity/revision/integrity-state contract and tests.

## ADR-020: PDF extraction content persistence is blocked pending privacy review

**Status:** Accepted privacy posture; content persistence remains blocked pending
remaining gates below

**Decision:** The user accepted Memora's privacy posture: exclude private app data
from cloud backup and device-to-device transfer; use `allowBackup=false` as defence in
depth; plan a Keystore-protected encrypted database; let people clear derived data;
and keep retained derivations truthful when source access is revoked. Before Memora
writes PDF text or metadata to Room, the detailed encryption design, superseded-record
retention decision, measured resource limits, additive migration/rollback plan, and
atomic write/deletion verification must still be accepted.

**Gate progress (2026-07-25):** The encrypted-database design and production open path
are accepted and verified under ADR-021 (conversion rollout proofs #1–#10). That
satisfies the encryption-design prerequisite in this ADR. ADR-022 superseded-record
retention is accepted (non-current provenance). Remaining blockers before any
production PDF content write path / real-source parsing:

1. ~~superseded-record retention ADR~~ **done (ADR-022)**;
2. ~~concrete additive Room schema + migration/rollback verification~~ **done
   (schema v4 + migration 3→4 + synthetic Room port)**;
3. ~~measured write-path resource limits for extraction storage~~ **done
   (`PdfExtractionWriteBudgets` + write-path benchmark 5/5)**;
4. ~~atomic write/deletion verification tests~~ **done (synthetic instrumentation)**;
5. ~~ADR-017 real-source / bounded-streaming / visible-recovery~~ **done for current
   bar** (protocol v3 streaming, visible recovery UI, fingerprint revalidation,
   live approved-tree descriptor open, and foreground Local PDF reading status UI
   verified 2026-07-25).
6. ~~Foreground searchable PDF extraction persist from Local PDF reading Start~~
   **done** (validated wire retained → prepare → Room; persist integration **1/1**
   on 2026-07-25).
7. ~~Phase 1 WorkManager SAF PDF **discovery** drain~~ **done** (unique work per
   source; metadata placeholders/checkpoints only; extract WM deferred per
   ADR-017). Keyword search UI already landed separately.

Foreground Local PDF reading Start may open one indexed PDF and persist eligible
complete/no-text extraction text in encrypted Room. On 2026-07-25, interim
on-device **keyword** search over current-fingerprint page text landed (not
semantic Memory recall). The first WorkManager job is **discovery-only** (no PDF
bytes / extract). Extract WorkManager, on-device AI, and network remain blocked
until separately governed.

**Reason:** Derived page text is sensitive user content. The existing typed
persistence-port contract protects identity/fingerprint/schema/coverage correctness,
but cannot decide how sensitive local content is protected, retained, migrated, or
removed.

**Consequences:** The backup/device-transfer configuration is implemented as a
separate privacy foundation. ADR-021 encrypts Memora-owned Room data at rest.
ADR-022 retains superseded PDF extractions as non-current provenance. Additive PDF
extraction tables and a synthetic Room port may verify atomic writes. Production extract WorkManager, on-device AI, and network remain blocked
until separately governed. Discovery WorkManager and foreground keyword search are
Phase-1/local-reading follow-through only.

## ADR-022: Superseded PDF extraction retention

**Status:** Accepted on 2026-07-25 (product owner delegated lead; recommended
enterprise choice recorded)

**Decision:** Keep superseded PDF extraction records as **non-current provenance**
under their immutable
`(sourceId, sourceAssetKey, fingerprint, schemaVersion)` key. Current recall and
Explain Mode may use only the extraction that matches the Asset's current
fingerprint and schema. Superseded rows are never presented as current evidence.
User-confirmed **Clear Memora index** (and a future clear-by-source action) deletes
superseded and current derived rows alike. Measured storage budgets may later force
an overflow outcome that refuses new writes without silently deleting provenance.

**Rejected alternative:** Delete previous extraction automatically when a new
fingerprint or schema succeeds.

**Reason:** Memora is an evidence retrieval engine. Immutable provenance matches
ADR-018/ADR-019 and avoids silent rewrite of past evidence after a file change.
Clear derived data already gives calm user control.

**Consequences:** Future PDF content Room adapters must retain prior fingerprint /
schema rows. Acceptance does **not** alone authorize production/real-source content
writes; remaining ADR-020 gates and ADR-017 still apply. A synthetic-only additive
schema, migration, and Room port may proceed to verify atomicity.

## ADR-021: Encrypted database direction protects Memora-owned data at rest

**Status:** Accepted. Production encrypted open, conversion journal, BSD notices,
user-confirmed clear derived data, **live** conversion process-death resume,
low-storage / interruption denial, physical-device `arm64-v8a` conversion,
device-unlock deferred open, recovery/unlock copy guards, and conversion
performance/battery provisional budgets have landed. Encrypted-database conversion
rollout proofs #1–#10 are complete for the current bar. PDF content persistence
remains blocked (ADR-020 / ADR-017).

**Decision:** Use SQLCipher for Android integrated through Room's open-helper factory,
with a randomly generated database passphrase wrapped by a non-exportable, versioned
Android Keystore AES-GCM key. The initial default does not require biometric
authentication for every database use. It remains local-only and backup/device-
transfer exclusion remains binding.

**Product-owner approval:** Accepted on 2026-07-24. Ordinary use must stay
frictionless: no password to create or remember, no biometric prompt for every search
or index, and no encryption setup screen. Privacy should feel invisible. Recovery copy
must never mention SQLCipher, keys, or encryption failures. The only approved
user-facing recovery wording is:

> Your private Memora index needs to be rebuilt. Your original photos, documents, and
> notes are unchanged.

**Reason:** App-private storage and Android Keystore alone do not encrypt Room's
database. Custom column encryption risks exposing SQLite indexes, journals, and
metadata while creating fragile query/migration behavior. A whole-database cipher plus
Keystore-protected secret is the narrowest supported path that meets the accepted
local, privacy-first direction without cloud escrow or a user password.

**Required safeguards:** The dependency licence/provenance review in
`docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` is accepted for
`net.zetetic:sqlcipher-android:4.17.0`. Production path uses Keystore lifecycle tests,
plaintext-to-encrypted copy-and-validate conversion, crash-resume (simulated + live
kill covered), low-storage / interruption denial, physical-device `arm64-v8a`
verification, device-unlock deferred open, user-confirmed derived-data clearing,
recovery/unlock plain-language copy guards, BSD attribution notices, and content-
free conversion performance budgets. No destructive migration, silent reset, cloud
recovery, source mutation, or content logging is allowed.

**Consequences:** Production Room opens encrypted `memora.db` through
`MemoraEncryptedDatabaseOpener` / `MemoraDatabaseHandle`. This does **not** authorize
PDF content persistence, WorkManager indexing, search, AI, or network. ADR-017 and
ADR-020 remain independently binding. `ENCRYPTED_DATABASE_DECISION.md` remains the
binding detailed design.

## ADR-023: AI Pack delivery and security plan accepted for gate planning

**Status:** Accepted (planning only)

**Decision:** `docs/AI_PACK_DELIVERY_SECURITY_PLAN.md` is the binding delivery,
integrity, disclosure, and rollback plan for Spec §6 / A-07 until a later
implementation slice is separately change-controlled. Domain contracts for pack
manifest fields, install state, verification results, and an unavailable
`AiPackManager` stub are allowed without network, download UI, or model bytes.

**Reason:** The Local-AI architecture gate requires a documented licensing, delivery,
update, integrity, and rollback plan before any AI Pack download or inference
dependency. Recording the plan and testable manifest contracts keeps A-07
progress honest without claiming understanding works on device.

**Consequences:** No INTERNET permission, pack download, model file, OCR/embedding
SDK, or WorkManager AI job may land merely because this ADR exists. Compatibility/
fallback policy and Local-AI benchmark plan remain separate gate deliverables.
Capability engines remain unavailable until a verified pack (or approved system
runtime) is installed.

## ADR-024: Local-AI compatibility and fallback policy accepted for gate planning

**Status:** Accepted (planning only)

**Decision:** `docs/LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY.md` is the binding
supported-device / capability matrix and fallback policy for Spec §11 / §13.4 until
measured pack-specific rows are change-controlled. Domain
`CapabilitySupportDecision` types and a default unsupported resolver are allowed.
Silent semantic→filename fallback and unlabeled keyword-as-semantic paths are
forbidden.

**Reason:** Unsupported devices must receive truthful unavailable outcomes. A
documented matrix and forbidden-fallback rules make the offline-first promise
reviewable before any model dependency.

**Consequences:** No capability may report AVAILABLE merely because discovery,
extraction, or keyword search works. Concrete ABI/API/RAM cut lines require a
chosen pack plus Local-AI benchmark evidence. Gate exit still needs the benchmark
plan.

## ADR-025: Local-AI benchmark plan accepted for gate planning

**Status:** Accepted (planning only)

**Decision:** `docs/LOCAL_AI_BENCHMARK_PLAN.md` is the binding performance, quality,
offline, and privacy-safe measurement plan for Spec §11 / §13.6 until a pack
implementation produces measured baselines. Domain metric IDs and
`LocalAiBenchmarkClaim` rules forbid publishing unmeasured release promises.

**Reason:** Spec §11 rejects arbitrary latency, battery, storage, or quality claims
without evidence. Recording the plan and claim guards completes the documentation
side of the Local-AI architecture gate.

**Consequences:** No user-facing AVAILABLE intelligence claim, SLA, or pack-size
promise may cite planning targets alone. First measured baselines require a
separately approved pack/harness slice. Architecture-gate **planning** deliverables
are complete; measured pack proof remains open for A-01.

## ADR-026: Screenshot OCR is Phase 2 deterministic extract via bundled ML Kit

**Status:** Accepted

**Decision:** On-device OCR for catalogued `SCREENSHOT` assets is Phase 2
deterministic extraction, not Local-AI pack activation. Memora uses the bundled
Latin `com.google.mlkit:text-recognition` library behind a data-layer reader
interface. Discovery stays metadata-only (ADR-009). Extract is a separate
user-started WorkManager drain that opens permitted URIs read-only, persists OCR
text with engine provenance, and never adds INTERNET permission or uploads content.

**Scope for this ADR:** SCREENSHOT only. PHOTO OCR, non-Latin scripts, keyword
search over OCR text, Spec §4 `OcrEngine` availability, and AI Pack delivery remain
separate change-controlled slices.

**Reason:** Screenshots are text-heavy recall targets. Bundled ML Kit keeps OCR
offline and independent of the AI Pack download path, while still satisfying P-06’s
deterministic OCR requirement for screenshots.

**Consequences:** UI and docs must not claim searchable Memory or keyword recall
until a later OCR-search slice lands. LocalIntelligence `OcrEngine` remains
unavailable until an approved Local-AI capability path exists.

## ADR-027: Public positioning copy versus current MVP delivery scope

**Status:** Accepted

**Decision:** The repository README and GitHub About use investor-facing product
positioning: on-device personal AI memory infrastructure, natural-language recall
with evidence, and privacy that keeps private information on the phone. That copy
may mention long-term target categories (for example videos, medical records, and
receipts).

**Rule:** Public positioning does not expand the binding MVP delivery scope.
Current engineering still follows the product registry and ADRs: photos,
screenshots, PDFs, and notes as the MVP asset types, with source adapters and
permissions landed only when change-controlled. App UI and changelog truthfulness
rules still forbid claiming unfinished capabilities inside the product.

**Reason:** Investors need a clear product definition, while delivery must stay
honest about what ships today.

## ADR-028: Photo OCR parallels screenshot OCR as separate deterministic extraction

**Status:** Accepted

**Decision:** Extend ADR-026's bundled, on-device ML Kit Latin OCR pattern to
catalogued `PHOTO` assets through a separate explicit WorkManager drain and separate
Room `photo_ocr_extractions` table. PHOTO records, pending selection, readiness, and
keyword search remain distinct from SCREENSHOT equivalents.

**Privacy and truthfulness:** Original photos are opened read-only only after the
user starts text reading. OCR and keyword search run on-device without `INTERNET`,
upload, AI Pack activation, semantic Memory ranking, or natural-language claims.
Search reads persisted current-fingerprint OCR; only the separate Open original
action reopens a photo for a capped read-only preview.

**Reason:** P-06 requires OCR for images, while separate source-type provenance
prevents ordinary-photo text from being misrepresented as screenshot evidence.

## ADR-029: Embedding-first Local-AI implementation track

**Status:** Accepted

**Decision:** The next Local-AI implementation track prioritizes
`EmbeddingEngine` (and then embedding-backed recall) over Vision/OCR AI Pack
adapters. Delivery order is fixed:

1. install ledger + disclosure acknowledgment rules (domain),
2. durable persistence of pack install state,
3. user-facing disclosure UI (no AVAILABLE claim),
4. pack download + atomic verify for a chosen embedding pack,
5. bind `EmbeddingEngine`, index stored Memory text, candidate recall by cue,
6. only then measured AVAILABLE claims under ADR-024 / ADR-025.

Keyword recall and pre-AI Asset Memories remain honest interim paths and must not
be relabeled as Local Intelligence AVAILABLE.

**Privacy and truthfulness:** No model vendor is selected by this ADR. No pack
bytes, INTERNET permission, or inference SDK is authorized here. Failed or
absent packs stay Unavailable. Synthetic measured-baseline harnesses (L0–L2)
remain integrity evidence only.

**Reason:** Meaning-based recall is the core user promise after sources are
indexed. Embeddings create the highest user value next from facts Memora already
extracts, while Vision packs can follow without blocking natural-language recall.

## ADR-030: Offline pack-container activation before vendor download

**Status:** Accepted

**Decision:** Split embedding-pack E4 into:

1. **E4a (now):** After disclosure, user-affirmative **offline** verify-and-store of
   a Memora-owned synthetic embedding **pack container** into app-private
   no-backup storage, with ledger ACTIVE via existing integrity verifier.
2. **E4b (later):** Replace/extend with user-approved network download of a chosen
   vendor embedding model pack (INTERNET scoped; pack bytes only).

`EmbeddingEngine` remains Unavailable through E4a. Ledger ACTIVE means the install
pipeline succeeded — not Local Intelligence AVAILABLE / meaning search.

**Privacy and truthfulness:** No INTERNET permission in E4a. No user content in
pack files. Clear Memora index also deletes pack payload files. Product copy must
say meaning search stays off.

**Reason:** Waiting on an unresolved vendor choice must not block proving
disclosure → verify → durable ACTIVE on real devices — the trust path users will
rely on when a real model lands.

## ADR-031: MediaPipe Text Embedder as first EmbeddingEngine

**Status:** Accepted

**Decision:** The first product `EmbeddingEngine` uses MediaPipe Tasks Text
Embedder (`com.google.mediapipe:tasks-text`, **0.10.29+** for Android 16 KB
page-size native alignment) with the compact `average_word_embedder` float32
model. The model is downloaded to app-private no-backup storage after disclosure
+ affirmative action — never bundled in the APK. Synthetic pack-container ACTIVE
(E4a) does **not** make EmbeddingEngine Available.

Availability rules:

1. Model file missing or failed to load → Unavailable (plain reason).
2. Model loads successfully → Available with versioned model identity.
3. Meaning-search UI may appear only when Available; keyword paths stay labeled.

**Privacy and truthfulness:** Download carries model bytes only (no user
content). Inference is on-device. Clear Memora index deletes the model file.
Copy must disclose the compact-model limitation (DEGRADED_EXPLICIT quality vs a
future larger pack).

**Reason:** Delivers real on-device meaning vectors without waiting on a custom
vendor pack, while preserving Spec §6 small-APK and local-first rules.

## ADR-032: Universal Sentence Encoder as product on-device embedder (E4b)

**Status:** Accepted

**Decision:** Replace the product default MediaPipe model from compact
`average_word_embedder` (ADR-031 first ship) with MediaPipe **Universal
Sentence Encoder** float32
(`mediapipe-models/text_embedder/universal_sentence_encoder/float32/1`).
Same Tasks Text Embedder runtime (`tasks-text` 0.10.29+), same disclosure →
download → private no-backup install path. ADR-031’s availability rules remain.

Evidence for the switch: M1 JVM + M2 emulator page-recall baselines showed
cosine-only hit@1 **0/3** on the compact model; E5d token boost recovered
labeled @1 but is not a substitute for semantic-only quality.

Legacy `average_word_embedder` files must not count as installed for the product
path; clear/upgrade deletes them. Indexed vectors are model-version keyed — users
must rebuild the meaning index after upgrading.

**Privacy and truthfulness:** Still model-bytes-only download; still no marketing
AVAILABLE without measured midrange rows (ADR-024/025). Disclosure must state the
larger on-disk size. E5d disclosed assist may remain until a USE measurement
slice (M3) decides otherwise. EmbeddingGemma (and other packs) stay out of scope.

**Reason:** Closes ADR-030 E4b with Google’s recommended semantic Text Embedder
while keeping the proven MediaPipe install pipeline.

## ADR-033: Grounded Answers scope — retrieval-first, no chatbot

**Status:** Accepted product and architecture direction; implementation blocked on
readiness gates in `docs/GROUNDING_ARCHITECTURE.md`

**Decision:** Memora adopts **Grounded Answers** as a governed capability: answer
questions about the user’s own indexed information using only retrieved, stored,
authorized evidence, or explicitly abstain. Find remains the source of truth and a
permanent safety rail. User-facing naming (“Ask”, etc.) is not frozen.

**In scope (direction):** Evidence Package → ReasoningEngine → Verifier →
StructuredAnswer pipeline; PDF saved-text first slice per
`docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`.

**Out of scope:** Chatbot, chat history, personalities, agents, cloud core
reasoning, phone-wide assistant, goal orchestration, Event/Knowledge Memories as
answer substrate, numeric confidence without calibration.

**Spec carve-out:** Narrowly amends Local-AI Spec §9 to allow query-time generation
**only** for this capability over an Evidence Package (see
`docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`). Ordinary Find remains non-generative.

**Reason:** Category differentiation is grounded answers with evidence, not another
AI chat app. Architecture must be frozen before model wiring.

## ADR-034: Evidence Package contract

**Status:** Accepted contract; no production builder code in this ADR

**Decision:** The **Evidence Package** is the model-agnostic center of Grounded
Answers. It is an immutable, bounded, deterministic, checksummed snapshot of
permitted stored excerpts/pages with locators, task, constraints, budget, versions,
retrieval-path labels, source-availability, and **coverage/omission reasons**.

**Must not:** Call a model; reopen originals; inject unseen text; be named or
designed as a “prompt builder” that couples Memora to a single LLM API.

**Must:** Prefer stored extracts; record truncation honestly; remain independently
testable; be ephemeral by default in v1.

**Reason:** Package construction heuristics are long-lived IP; models are
replaceable. Completeness honesty depends on coverage metadata.

## ADR-035: Retriever port for Grounded Answers

**Status:** Accepted contract; unify later without deleting Find

**Decision:** Grounded Answers consumes candidates through a **Retriever** port that
returns labeled candidates from stored indexes (keyword, meaning, and future
authorized corpora). It must not invent candidates, reopen originals for ranking, or
silently present keyword hits as meaning hits (ADR-024).

**Cited open:** StructuredAnswer citations are authoritative for open-original; silent
post-answer page re-score that changes the cited page is forbidden.

**Reason:** Today’s dual Find paths would otherwise disagree with Ask and erode
trust. A port future-proofs approximate indexes without changing package/reasoner
APIs.

## ADR-036: ReasoningEngine capability and ReasoningTask

**Status:** Accepted contract; no generative SDK authorized here

**Decision:** Introduce a Local Intelligence capability **ReasoningEngine** that
only reasons over an Evidence Package for a **ReasoningTask**. v1 task is
`ANSWER_QUESTION`; other tasks are reserved. Do not overload Spec `DocumentEngine`
or index-time Memory assembly.

**Output:** Untrusted claim-shaped `ReasoningResult` (domain only) — never Compose
UI types or a bare product `String`.

**Runtime:** Dedicated lifecycle-aware owner; single-flight v1; cancellable;
not Compose-loaded unmanaged singleton.

**Reason:** Separates replaceable probabilistic reasoning from deterministic
retrieval/packaging and from index-time understanding.

## ADR-037: Verifier and abstention (structural, not oracle)

**Status:** Accepted contract

**Decision:** Every grounded answer passes a deterministic **Verifier** that checks
citation∈package, claim citation coverage, limits, and conflict/absence rules.
Failure yields abstention / `INSUFFICIENT_EVIDENCE` / `CONFLICTING_EVIDENCE` — never
an unverified prose answer.

**Must not:** Call a model; claim semantic paraphrase equivalence; market
“hallucination-proof”; invent numeric confidence.

**Reason:** Structural gates catch many fabrications; faithfulness still requires
evaluation and constrained answer forms (ADR-019).

## ADR-038: StructuredAnswer status and completeness

**Status:** Accepted contract

**Decision:** Grounded Answers return a domain **StructuredAnswer** with:

- status: `ANSWERED`, `INSUFFICIENT_EVIDENCE`, `CONFLICTING_EVIDENCE`,
  `CAPABILITY_UNAVAILABLE`, `CANCELLED`, `FAILED_SAFELY`, `SHOW_CANDIDATES_ONLY`;
- **completeness:** `COMPLETE` | `PARTIAL` | `UNKNOWN`;
- claims with per-claim citations; evidence excerpts; limitations; versions;
  optional next actions.

**No numeric confidence** in v1. UI maps domain → presentation and must not invent
supporting narrative.

**Reason:** Completeness honesty beats fake confidence. Silent incompleteness is the
primary long-term trust failure mode.

## ADR-039: Reasoning pack lifecycle separate from embeddings; interactive execution

**Status:** Accepted planning decision; specific model vendor not chosen here

**Decision:**

1. Reasoning/generation packs are **separate** from embedding packs (ADR-029–032).
   Each needs capability id, integrity, disclosure, ABI/device matrix, rollback,
   and measured availability under ADR-023/024/025 patterns.
2. Interactive grounded answers use a **foreground cancellable** flow with request +
   package IDs — not WorkManager as the default. Snapshot evidence before inference;
   never hold a Room transaction across inference. Process-death drops in-flight
   answers (retry), never restores partial prose.
3. Choosing and wiring a concrete on-device reasoner requires a follow-up ADR plus
   closed readiness gates (eval corpus, device policy/M4 honesty, adversarial pass).

**Reason:** Operational trust failures (cancel, OOM, stale UI, pack confusion with
USE) destroy product trust as surely as bad model output.

## ADR-040: Product identity UNFYND (formerly Memora)

**Status:** Accepted

**Decision:** The current product/brand name is **UNFYND**. Memora is the former
name. This is a product-identity decision, not an architecture rewrite, package
rename, or database rename.

**Binding interpretation:**

1. UNFYND is **Personal Knowledge Infrastructure**. That north star is already
   recorded in ADR-018 and `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md`. This ADR
   does not implement Event Memory, Knowledge Memory, or any later PKI stage.
2. The Android application is **one milestone / reference implementation**, not
   the whole system.
3. Search/retrieval is **one capability**, not the product definition.
4. Living canon may say UNFYND after later overlay steps in
   `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md`. Accepted ADRs **keep their
   original wording**. Identity is superseded here, not by editing history.
5. Domain language stays: `Memory`, `MemoryEvidence`, `MemoryAnchor`, `Asset`,
   Find, Evidence Package, Memory Builder, and Event/Knowledge Memory as staged
   concepts.
6. Immutable `docs/product-source/Memora.docx` and addenda stay historical
   baselines. Their SHA-256 hashes must not change in this step. New UNFYND
   architecture files are registered in playbook Step 2, not invented here.
7. No repository-wide find-and-replace of `Memora`.

**Out of scope until a later ADR:**

- `applicationId` / namespace (`com.memora.app`)
- `memora.db` and Keystore/file identities
- MSAL redirect host
- GitHub repository name
- `MemoraApp/` folder
- Event/Knowledge Memory implementation
- Grounded Answers code
- Agents

**Reason:** Brand and technical identity must stay distinct so a rename cannot
create a second app install, drop folder grants, break provider sign-in, or
make the encrypted index unreadable.

**Consequences:** Overlay of living constitutions, user-visible Android copy,
and any later technical-ID migration are separate playbook steps. This ADR
authorizes none of those by itself.

## ADR-041: Register Architecture Freeze v1.0 and Architectural Migration Spec V1

**Status:** Accepted as a registration record; **authority conflicts resolved by ADR-042**

**Decision:** The user-supplied documents

- `docs/ARCHITECTURE_FREEZE_v1.0.md`
- `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md`

are now hashed, tracked product-source artifacts. They are registered as
supplied. Their substantive text is not rewritten in this step. This ADR does
**not** authorize MIG-01–MIG-11, MIG-07B, or any application, database, retrieval,
OCR, MemoryBuilder, RecallRanker, Grounded Answers, Event/Knowledge/Links,
package, or identity change.

**Why they are being registered:** They were provided as binding UNFYND Core
architecture artifacts. Repository governance requires accepted architecture
files to be inventoried, hashed, and listed in `docs/PRODUCT_SOURCE_REGISTRY.md`
rather than used from conversational memory (playbook Step 2; registry change
rule).

**Intended placement (not a silent winner):**

1. Architecture Freeze v1.0 is the freeze / change-control declaration for the
   architecture it names (its §2 items 1–3 plus the sequencing role of the
   Migration Spec).
2. Architectural Migration Spec V1 governs implementation **sequencing only**. It
   does not define new architecture. Where it appears to imply a rule not stated
   in Product Contract, Local AI Technical Spec, or Experience Memory Amendment,
   those documents govern (Freeze §2 item 4; Migration Spec authority line).
3. Existing subsystem constitutions remain authoritative **within their defined
   scope** unless a later **human** decision accepts an explicit Freeze
   supersession. This ADR does not make that decision.

**Does Architecture Freeze supersede any existing document?**  
**Unresolved.** Freeze §1 claims four documents are the complete and exclusive
architectural authority and that no other document carries architectural
authority. That wording, if accepted as written, would strip authority from
already-registered artifacts (notably Grounding Architecture). This ADR does
**not** accept or reject that exclusivity claim.

**Does Grounding Architecture remain authoritative for Grounded Answers?**  
**Still listed as the sole Grounded Answers constitution** in the registry,
CONTINUE, ADR-033–039, and Local AI Spec §9 carve-out. Whether Freeze §1
revokes that status is the open conflict below. No Grounded Answers code is
authorized either way.

**Does the Migration Spec govern sequencing without changing architecture?**  
**Yes, as written.** Sequencing authority only. Starting any MIG-* item remains
blocked while the conflicts below are open if the work would depend on a chosen
authority winner.

**What happens if future documents conflict?**  
Stop. Record the conflict. Do not conceal it with a shortcut. Do not invent a
new constitution to paper over it. Use Freeze §5–§6 only for a proven
architectural flaw **after** a human owner has decided how Freeze §1 relates to
already-registered constitutions. `docs/GOVERNANCE.md` numbered order is not
rewritten in this step.

### Open conflicts (human decision required)

**CONFLICT 1**

- **SOURCE A:** `docs/ARCHITECTURE_FREEZE_v1.0.md` §1 (four documents are
  complete exclusive architectural authority).
- **SOURCE B:** `docs/PRODUCT_SOURCE_REGISTRY.md`,
  `docs/GROUNDING_ARCHITECTURE.md`, `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`,
  ADR-033–039, Local AI Spec §9 carve-out.
- **EXACT ISSUE:** The freeze’s exclusive list omits Grounding Architecture and
  the Grounded Answers amendment, and denies architectural authority to any
  other document.
- **WHY IT MATTERS:** Accepting exclusivity as written would unseat the
  registered Grounded Answers constitution. Rejecting exclusivity leaves Freeze
  §1 false as a global claim.
- **PROPOSED HUMAN DECISION:** Either (a) treat Freeze exclusivity as limited to
  the Asset-Memory / PKI core it audited, leaving Grounding Architecture as the
  sole Grounded Answers constitution, or (b) amend Freeze §1 to list Grounding
  Architecture / the GA amendment, or (c) explicitly retire Grounding
  Architecture — with a written product decision.

**CONFLICT 2**

- **SOURCE A:** `docs/ARCHITECTURE_FREEZE_v1.0.md` §2 (Product Contract above
  Local AI Technical Spec).
- **SOURCE B:** `docs/GOVERNANCE.md` governing order (Local AI Technical Spec
  above Product Contract).
- **EXACT ISSUE:** The two hierarchies invert Product Contract vs Local AI Spec.
- **WHY IT MATTERS:** A future conflict between those two living documents would
  be resolved in opposite directions.
- **PROPOSED HUMAN DECISION:** Align GOVERNANCE’s numbered order with Freeze §2,
  or amend Freeze §2 to match GOVERNANCE, or write a scoped rule (for example
  Freeze §2 applies only inside the four named freeze documents).

**CONFLICT 3**

- **SOURCE A:** `docs/ARCHITECTURE_FREEZE_v1.0.md` §7 (claims LOCAL_AI §15 now
  summarizes the retrieval-signal rule and defers to Experience Memory
  Amendment §7 after that §7 was generalized to cover confidence values and
  explanation elements).
- **SOURCE B:** Current hashed `docs/LOCAL_AI_TECHNICAL_SPEC.md` §15 and
  `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` §7.
- **EXACT ISSUE:** The registered repo copies do not contain that claimed
  correction. Experience Memory §7 still speaks only to embeddings/learning;
  Local AI §15 still restates embedding limits and defers to the amendment as a
  whole, not as “§15 defers to §7 as the single authoritative statement.”
- **WHY IT MATTERS:** The freeze certifies a published-document state that the
  hashed repo copies do not match. Applying the claimed edits here would rewrite
  already-hashed constitutions without a separate amendment step.
- **PROPOSED HUMAN DECISION:** Either apply the smallest Freeze-described
  correction to those two hashed documents and re-hash them, or amend Freeze §7
  to describe the repo text as it actually is.

**Consequences:** Registration only. No engineering. MIG-* stays unstarted.
Nothing in this ADR picks a winner among the conflicts. ADR-042 records the
human decisions for conflicts 1–3.

## ADR-042: Resolve ADR-041 Freeze vs Grounding and GOVERNANCE order

**Status:** Accepted

**Decision:** The three open authority conflicts recorded in ADR-041 are
resolved as follows. This ADR does not pick different winners than the
user-chosen options. It does not rewrite Freeze §1. It does not retire
Grounding Architecture. It does not edit hashed `docs/LOCAL_AI_TECHNICAL_SPEC.md`
or `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` §7. It does not change the
Architecture Freeze blob hash. It does not authorize MIG-01–MIG-11, MIG-07B, or
any application, database, retrieval, OCR, MemoryBuilder, RecallRanker,
Grounded Answers, Event/Knowledge/Links, package, or identity change.
`docs/PHASE_A_IMPLEMENTATION_PLAN_V1.md` is not architectural authority and is
not permission to implement.

**Conflict 1 — Freeze §1 exclusivity vs Grounding Architecture**

**Winner: option (a).** Freeze §1 exclusivity is the Asset-Memory / PKI core
named by Freeze §2 items 1–3 (`PRODUCT_CONTRACT.md`,
`LOCAL_AI_TECHNICAL_SPEC.md`, `EXPERIENCE_MEMORY_AMENDMENT_V1.md`), with
`ARCHITECTURAL_MIGRATION_SPEC_V1.md` as sequencing only. That core freeze does
not unseat already-registered Grounded Answers constitutions.

`docs/GROUNDING_ARCHITECTURE.md` and `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`
remain the sole Grounded Answers constitution and product-direction amendment
(ADR-033–039; Local AI Spec §9 carve-out). Grounding is not retired. Freeze §1
text is not rewritten in this step; the exclusivity claim is interpreted as
scoped to the audited Asset-Memory / PKI core, not as a global revocation of
other hashed constitutions.

Rejected here: option (b) amend Freeze §1 to list Grounding; option (c) retire
Grounding.

**Conflict 2 — GOVERNANCE numbered order vs Freeze §2**

**Winner: align `docs/GOVERNANCE.md` with Freeze §2.** After the user's latest
explicit instruction and the immutable PRD / addenda / registry amendments, the
delivery governing order is:

1. Product Contract
2. Local AI Technical Spec (the spec wins only where an implementation detail
   would otherwise require cloud AI or repeated original-asset analysis at
   recall)
3. Experience Memory Amendment
4. Architecture Freeze, as freeze / change-control for that Asset-Memory / PKI
   core
5. Migration Spec, sequencing only
6. Grounding Architecture, for Grounded Answers only
7. Other accepted ADRs, `docs/ARCHITECTURE.md`, traceability, `docs/ROADMAP.md`,
   and `CONTINUE.md`

Rejected here: amend Freeze §2 to match the prior GOVERNANCE inversion, or
confine Freeze §2 to the four named freeze documents only.

**Conflict 3 — Freeze §7 claimed Spec/Amendment correction vs hashed copies**

**Winner: record Freeze §7 as errata; do not rewrite hashed documents now.**
Freeze §7’s statement that Local AI Spec §15 now summarizes the
retrieval-signal rule and defers to Experience Memory Amendment §7 as the
single authoritative statement is **not present** in the current hashed
`docs/LOCAL_AI_TECHNICAL_SPEC.md` §15 or `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md`
§7. Experience Memory §7 still speaks to embeddings/learning; Local AI §15
still restates embedding limits and defers to the amendment as a whole.

That claimed correction is deferred until a later hashed amendment of those
two documents (or a Freeze §7 text correction) under Freeze §5–§6. Preferring
not to change the Freeze blob hash, this ADR does not edit
`docs/ARCHITECTURE_FREEZE_v1.0.md`. Until a hashed correction lands, engineers
must read the current hashed Spec and Amendment text as written, not the
Freeze §7 description of a correction that is not in those blobs.

Rejected here: applying the Freeze-described Spec/Amendment edits and
re-hashing them in this step.

**Consequences:** ADR-041 registration remains valid. GOVERNANCE numbered
order is rewritten in this step. MIG-* remains unstarted. Grounded Answers
remain architecture, not implementation. No `MemoraApp/` identity or schema
change.

## ADR-043: Confirm PKI vision vs frozen architecture; Act is out of current architecture

**Status:** Accepted

**Decision:** The user confirmed the north star in conversation.
Search/retrieval is one capability, not what UNFYND is. UNFYND is Personal
Knowledge Infrastructure: a local-first, storage-agnostic Memory Core that
perceives, remembers, connects, understands, and (later) reasons over a
person's digital life. The Android app is the first reference implementation,
not the full product.

Product-language progression: See → Remember → Connect → Understand →
Converse → Act.

This ADR does not rewrite hashed constitutions, reopen Architecture Freeze
v1.0, start MIG-01–MIG-11 or MIG-07B, or authorize application, database,
retrieval, UI, Grounded Answers, Event/Knowledge/Links, or identity change.

**Binding interpretation:**

1. **Cite ADR-040.** Identity is UNFYND. Personal Knowledge Infrastructure is
   the north star. Android is a milestone / reference implementation.
   Search/retrieval is not the product definition.
2. **The frozen architecture is accepted as suitable for that vision** through
   the stages already written. Frozen authority is Product Contract + Local AI
   Technical Spec + Experience Memory Amendment + Freeze change-control;
   Grounding Architecture governs Grounded Answers only (ADR-042).
   - **See / Remember:** source-neutral read-only Assets; Memory as derived
     intelligence; originals are not owned or uploaded as the core model.
   - **Connect / life-not-files:** staged Asset Memory → evidence-backed Link
     → Event Memory → Knowledge Memory (not implemented; not claimed).
   - **Retrieve by meaning:** one Memory / evidence substrate; embeddings are
     retrieval signals only.
   - **Understand / converse-as-Q&A:** Grounded Answers over retrieved stored
     evidence (or abstain); Find remains a safety rail. Not a generic chatbot.
3. **Explicitly out of current architecture** until a later ADR and
   product-contract change:
   - Act / agentic Personal AI / tools that act on the user's behalf
   - Chat history, agents, personalities, goal orchestration, multi-step life
     tasks (already deferred in `docs/GROUNDING_ARCHITECTURE.md` §16)
   - Treating UNFYND as a personal assistant that mutates originals
4. **Connector / asset-type expansions** (Drive, iCloud, NAS, email, video,
   audio, non-PDF office docs) are not a new Memory Core. They need their own
   source adapters, approval, and an explicit contract/ADR when added. Do not
   treat PRD MVP exclusions as silently in-scope.
5. **Product Contract still leads with “personal memory retrieval engine.”**
   That line is narrower than the north star. This ADR does not rewrite hashed
   `docs/PRODUCT_CONTRACT.md`. A later overlay/ADR may align the one-line
   definition; this ADR does not change hashed blobs.
6. **Do not reopen the Architecture Freeze for this vision.** Do not start
   MIG-01–MIG-11 or MIG-07B in this change. Do not skip Asset-Memory quality
   to jump to Links, Events, Grounded Answers, or conversation UI.

**Out of scope until a later ADR and product-contract change:**

- Act / agentic Personal AI
- Chat history, agents, personalities, goal orchestration
- Mutating original user content
- Rewriting hashed Product Contract, Freeze, Spec, or Amendment blobs
- MIG-* implementation
- New source types treated as already in-scope

**Reason:** The north star and the freeze already agree through See, Remember,
Connect (staged), retrieve-by-meaning, and Grounded Answers. Recording that
fit prevents two failure modes: treating UNFYND as “just search,” and treating
Act / assistant mutation as already architected.

**Consequences:** Registry interpretation points here. Optional human-readable
map `docs/UNFYND_VISION_ALIGNMENT.md` is not hashed and is not architectural
authority. MIG-* remains unstarted. Act remains out. Hashed architecture files
are unchanged.

## ADR-044: Low-power product behavior is the event-driven Memory lifecycle

**Status:** Accepted

**Decision:** UNFYND’s on-device power posture is not neuromorphic silicon and
not always-on sensing. It is the event-driven Memory lifecycle already required
by the Local AI Technical Spec and Architecture Freeze: do expensive work
rarely, store durable Memory / evidence / embeddings, stay quiet until a
discovery, fingerprint, user, or pack-upgrade cue, recall cheaply from stored
rows, and run expensive reasoning only when explicitly asked (Grounded Answers
over a frozen Evidence Package). Team language may say “hippocampus, not GPU
cluster” or “sparse index → cheap recall → rare deep thought.” This ADR records
that interpretation only.

This ADR does not add new architectural principles, reopen Architecture Freeze
v1.0, authorize MIG-*, authorize Grounded Answers implementation, authorize
always-on camera/mic, or authorize cloud AI on the core path.

**Binding interpretation:**

1. **Cite Local AI Spec §7.** Background discovery, extraction, and
   understanding run through WorkManager with battery-not-low,
   storage-not-low, prefer-charging/idle for heavier models, and stop when
   constraints or thermal/memory pressure require it. Honest paused /
   retryable UI when indexing yields to device health is correct product
   behavior, not a silent failure.
2. **Cite Local AI Spec §9 and Freeze §3.** Ordinary Find / recall does not
   reopen originals and does not run per-result generative reasoning.
   Retrieval-first and truth-before-intelligence remain binding. The narrow
   Grounded Answers carve-out (expensive reasoning over a frozen Evidence
   Package when explicitly asked) is unchanged and still not an
   implementation authorization.
3. **Cite ADR-043.** Personal Knowledge Infrastructure is the north star;
   Android remains a reference shell; Act remains out of current
   architecture. This ADR does not change that scope.
4. **No new requirements.** No neuromorphic hardware mandate. No performance
   number claims without benchmarks. No rewrite of hashed Spec, Freeze,
   Amendment, Grounding, or Product Contract blobs.

**Out of scope:**

- Application, database, Room, retrieval, UI, or MIG-* code
- Neuromorphic or always-on sensing requirements
- Grounded Answers implementation
- Cloud AI on the core path
- Rewriting hashed architecture files
- Performance SLAs without measured baselines

**Reason:** The product already behaves like a sparse, event-driven memory
system on phone constraints. Recording the interpretation prevents treating
paused indexing as a bug, or treating “low power” as a call for new silicon
or always-on capture.

**Consequences:** Registry and CONTINUE point here for language only. Spec §7 /
§9 and Freeze retrieval-first / truth-before-intelligence remain the binding
rules. Hashed architecture files are unchanged. No code in this decision.

## ADR-045: UNFYND Core is on-device intelligence infrastructure

**Status:** Accepted

**Decision:** Living-canon and public product language for the substrate is
**UNFYND Core — on-device intelligence infrastructure**: durable multimodal
memory next to data that cannot leave; local-first; evidence-backed. This is a
product-language decision. It does not reopen Architecture Freeze, authorize
MIG-*, Class A publish, opening the Android app, Grounded Answers code, or
Act / agents.

**Binding interpretation:**

1. **Living noun.** New living-canon lines, `CONTINUE.md`, the `AGENTS.md`
   identity blurb, and registry interpretation use **UNFYND Core — on-device
   intelligence infrastructure** (or short forms “UNFYND Core” / “on-device
   intelligence infrastructure”) for the substrate.
2. **Retired living phrases.** Do **not** use these going forward in new
   living-canon lines, CONTINUE, AGENTS identity blurb, or registry
   interpretation:
   - “Personal Knowledge Infrastructure”
   - “Personal Intelligence Infrastructure”
   - Third-party grant metaphors (for example “open her”) or naming external
     foundations
3. **Prior internal noun; hashed text unchanged.** Where already-hashed
   documents or ADR-043 still say “Personal Knowledge Infrastructure” / PKI,
   leave those blobs and accepted ADR bodies unchanged. PKI is a **prior
   internal noun**. The substance of ADR-043 stands: Android is a reference
   shell; search/retrieval is not the product definition; Act remains out of
   current architecture; the vision ladder is direction only.
4. **Technical IDs unchanged.** `com.memora.app`, `memora.db`, Keystore/MSAL
   hosts, and GitHub repo/folder name `Memora` remain deferred per ADR-040 and
   `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md`. Identity is UNFYND; Memora is
   the former name plus deferred technical IDs.
5. **Non-authorization.** This ADR does not authorize Class A publish, opening
   the Android app, MIG-*, Grounded Answers code, Act/agents, or Architecture
   Freeze reopen.

**Out of scope:**

- Class A pack folder, README Class A rewrite, public GitHub repo setup
- MIG-* implementation
- Grounded Answers code; Act / agents
- Rewriting hashed Architecture Freeze, Local AI Spec, Experience Memory
  Amendment, Grounding Architecture, Product Contract, or Grounded Answers
  amendment blobs
- Package, database, Keystore, MSAL, or repository rename

**Reason:** Product language must name the on-device intelligence substrate
without implying a third-party grant narrative or reusing retired PKI /
“Personal Intelligence Infrastructure” nouns in living canon, while preserving
hashed constitutions and ADR-043’s architectural substance.

**Consequences:** Registry interpretation, AGENTS identity blurb, CONTINUE
living pointers, and changelog record this noun. Historical ADRs and hashed
blobs keep their original wording. No application or schema change.

Public noun refined by ADR-046.

## ADR-046: Refine UNFYND Core public noun to on-device memory and intelligence infrastructure

**Status:** Accepted

**Decision:** Living-canon and public product language for the substrate is now
**UNFYND Core — on-device memory and intelligence infrastructure**: durable
multimodal memory next to data that cannot leave; local-first; evidence-backed.
This supersedes the public wording in ADR-045 (“on-device intelligence
infrastructure”) only. Prefer “memory and intelligence” so Core is not read as
a generic local-LLM stack without a Memory substrate.

**Binding interpretation:**

1. **Refined living noun.** New living-canon lines, `CONTINUE.md`, the
   `AGENTS.md` identity blurb, and registry interpretation use **UNFYND Core —
   on-device memory and intelligence infrastructure** (or short forms “UNFYND
   Core” / “on-device memory and intelligence infrastructure”).
2. **ADR-045 still stands** for: drop “Personal Knowledge Infrastructure” /
   “Personal Intelligence Infrastructure” in new living canon; no grant
   metaphors; hashed PKI blobs untouched; technical IDs unchanged; no Class A /
   app open / MIG-* / Act from ADR-045 alone. Only the public noun wording is
   refined here.
3. **Non-authorization.** This ADR does not authorize Class A publish, opening
   the Android app, MIG-*, Grounded Answers code, Act/agents, or Architecture
   Freeze reopen.

**Out of scope:**

- Class A pack folder, README Class A rewrite, public GitHub repo setup
- MIG-* implementation
- Grounded Answers code; Act / agents
- Rewriting hashed Architecture Freeze, Local AI Spec, Experience Memory
  Amendment, Grounding Architecture, Product Contract, or Grounded Answers
  amendment blobs
- Package, database, Keystore, MSAL, or repository rename

**Reason:** Naming memory in the public noun keeps UNFYND Core from being
mistaken for a generic on-device LLM product without the Memory substrate.

**Consequences:** CONTINUE, registry interpretation, AGENTS identity line, and
ADR-045 change-control / CONTINUE pointers use the refined noun. ADR-045 body
keeps its original wording plus the one-line refinement note. No application or
schema change.

## ADR-047: Class A public release of UNFYND Core contracts under Apache-2.0

**Status:** Accepted

**Decision:** UNFYND will publish a curated **Public Specification / Contract**
pack for UNFYND Core under **Apache License 2.0** (Release Class A per
`docs/OPEN_SOURCE_COMMERCIAL_STRATEGY.md` §23). This ADR authorizes the Class A
decision and boundary. Exact pack file set and publish mechanics are a
**following step**, not this ADR.

**Binding interpretation:**

1. **Product / Core noun (cite ADR-046).** UNFYND Core is **on-device memory and
   intelligence infrastructure** — durable multimodal memory next to data that
   cannot leave; local-first; evidence-backed. Do not use “Personal Knowledge
   Infrastructure,” “Personal Intelligence Infrastructure,” or third-party grant
   metaphors in new living canon.

2. **Release Class A (cite strategy §23).** The Class A pack is an **essential
   open, inspectable element** of Core: Memory/Evidence model, evidence classes,
   capability seams including MemoryBuilder, and local-first /
   truth-before-intelligence / retrieval-first principles in plain public
   markdown, licensed under Apache-2.0. Exact file set is implemented later;
   this ADR authorizes the decision and boundary, not the file tree yet.

3. **What stays private until a later ADR.** The Android reference application,
   private monorepo (GitHub path may still say Memora), proprietary tuning,
   evaluation corpora, AI Pack weights/configs, secrets, keystores, and anything
   not explicitly listed in the Class A pack. Class A does **not** mean the whole
   stack is open.

4. **Technical IDs unchanged (cite ADR-040 / playbook).** `com.memora.app`,
   `memora.db`, Keystore/MSAL hosts, and GitHub repo/folder name `Memora` are
   not renamed in this ADR.

5. **License without external counsel.** The product owner explicitly chooses
   Apache-2.0 for this Class A **docs/contracts** pack and accepts residual
   legal risk without external counsel for this step. Class B commercial
   licensing is **not** described as done.

6. **Honesty / vision.** We are **building toward** open on-device memory and
   intelligence. Do not claim unfinished Converse, Act, agents, or marketing
   AVAILABLE semantic recall. Vision ladder Sees→Remember→Connect→Understand→
   Converse→Act is direction; Act remains out of current architecture
   (ADR-043).

7. **Independence from any grant program.** Openness is product strategy. Do
   **not** name external grant programs, foundations, or RFPs in this ADR,
   CONTINUE, or registry. Language may align with industry themes; the product
   is not defined by a grant.

8. **Website alignment.** Public materials may state that UNFYND Core is open
   **in the Class A sense** (contracts/specs inspectable under Apache-2.0) with
   an explicit scope boundary — not that the entire Android app is open source
   until a later ADR says so.

9. **Non-goals.** Opening the full app; secrets; Architecture Freeze reopen;
   Act/GA implementation; MIG-* authorization; Class B commercial release
   gates; CLA/contributor policy beyond noting they are future work before
   accepting significant external code contributions (strategy §13).

10. **Follow-ups (not this ADR).** (a) Build `public/unfynd-core/` (or
    equivalent) pack contents; (b) rewrite root README to App vs Core; (c)
    create separate public GitHub repo and publish **only** the pack; (d)
    optional later Class B / more Core source under new ADRs.

**Out of scope:**

- Creating `public/unfynd-core/` or any Class A pack files
- Root README Class A rewrite
- Public GitHub repository creation or push
- MIG-* implementation
- Grounded Answers code; Act / agents
- Rewriting hashed Architecture Freeze, Local AI Spec, Experience Memory
  Amendment, Grounding Architecture, Product Contract, or Grounded Answers
  amendment blobs
- Package, database, Keystore, MSAL, or repository rename
- Class B commercial licensing completion

**Reason:** Strategy §23 / §24 allow Class A docs/contracts under an artifact-
specific license before full Model A vs B / Class B gates. Apache-2.0 for the
curated Core contracts pack makes an essential inspectable element of Core
public without opening the Android reference app or proprietary assets.

**Consequences:** Registry, CONTINUE, changelog, and strategy status note record
Class A authorization. Pack implementation, README rewrite, and public repo
publish remain deferred follow-ups. No application or schema change.

## ADR-048: Synthetic non-personal Class A reference samples under Apache-2.0

**Status:** Accepted

**Decision:** The Class A pack may include **synthetic, non-personal** reference
samples (JSON and/or short markdown sketches) under the **same Apache License
2.0** as the contracts pack. Samples illustrate Memory / evidence acceptance and
rejection shapes for human inspection.

**Binding interpretation:**

1. **Scope.** Authorized content is fabricated illustrative data only. No
   personal user content, private App fixtures, evaluation corpora, keys, or
   AI Pack material.
2. **Not a schema lock.** Samples do not freeze production App schemas or APIs.
3. **License.** Same Apache-2.0 as the Class A pack; no personal-vs-business
   license split on these files.
4. **Location.** Prefer `public/unfynd-core/examples/` (synced to the public
   unfynd-core repository).
5. **Non-authorization.** Does not open the UNFYND App, Class B packages, MIG-*,
   Act/agents, or runnable validator requirements.

**Out of scope:** App source release; proprietary corpora; CLA systems; CI for
the private monorepo; validator implementation (may be a later decision).

**Reason:** ROADMAP-OPEN listed “selected non-secret reference samples” as
needing an explicit decision. Synthetic examples improve inspectability without
leaking private fixtures.

**Consequences:** Pack may ship `examples/` and cite this ADR in ROADMAP-OPEN /
CITATIONS. Update CONTINUE / CHANGELOG when samples land.

## ADR-049: Canonical Recall naming (App Find boundary)

**Status:** Accepted

**Decision:** The sole App product-facing retrieval boundary that ViewModels
will call after MIG-07 cutover is named **Canonical Recall**. It does not exist
as a single application API in code yet. Today’s keyword Finds and
`SearchAssetMemoriesByMeaning` remain live legacy / interim paths and are not
renamed by this ADR.

**Binding interpretation:**

1. **Canonical Recall** is the frozen App Find / search boundary name. A future
   Cursor rule may say “ViewModels call Canonical Recall” with one meaning:
   the sole product-facing retrieval boundary after MIG-07 cutover. Naming only;
   no code claim.

2. **MIG-06 `SearchMemoryEvidence`** is keyword/literal **candidate-generation**
   into Canonical Recall. It is not a second product Find system and not a
   permanent parallel UI search architecture.

3. **Grounding Retriever — Option C (phased, same pipeline).** Near term: App
   Find converges under Canonical Recall (MIG-06 / MIG-07 / MIG-07B). Later,
   Grounded Answers binds to the **same** retrieval candidate pipeline
   (Grounding’s Retriever responsibilities). Do not create a second
   keyword/meaning candidate stack for Ask. “Retriever” and “Canonical Recall”
   are not competing product search systems: Retriever is the Grounding-facing
   name for the shared retrieval port/responsibilities; Canonical Recall is the
   App Find-facing boundary over that converged pipeline. This ADR does not
   implement Retriever code.

4. **Spec `RecallRanker`** is the ranking stage **inside** Canonical Recall —
   the future capability implementation for model-backed ranking, not an
   independent product search system. MIG-07B structured/anchor filter is a
   deterministic stage inside Canonical Recall, not `RecallRanker`.

5. **False-positive guard.** Extraction tables → `AssetMemoryFactSource` →
   MemoryBuilder remains **ALLOWED**. Forbidden (target after cutover):
   extraction DAO → user-visible search result outside Canonical Recall (or
   outside a future legacy allowlist).

**Out of scope / non-claims:**

- Does not authorize MIG-06, MIG-07, MIG-07B, or MIG-05 step 4
- Does not claim Canonical Recall exists in code
- Does not retire legacy keyword Find or `PdfPageEmbedding*` dual-write
- Does not change MIG-05 step 3 acceptance or search implementation
- Does not implement Grounded Answers, VisionEngine, Act, or package rename
- Does not rewrite hashed Freeze / Spec / Grounding / Experience Memory blobs
- Does not start `LEGACY_RECALL_SURFACE`, Cursor rules, or later naming steps

**Reason:** Architecture Freeze §3 already requires one canonical recall
pipeline (literal + semantic as candidate generation into one
structured-filter-and-ranking stage). Product and Grounding docs needed a
single App-facing name and Option C binding so MIG-06/07/07B and future Ask
do not spawn competing Find architectures. ADR-035’s Retriever port remains
the Grounding-facing name for shared retrieval responsibilities.

**Consequences:** Registry, CONTINUE, changelog, and change-control record
this naming decision. Implementation remains gated on future authorized
MIG steps. No application or schema change.

## ADR-050: MIG-05 claim levels (PDF delivery slice vs Spec full)

**Status:** Accepted

**Decision:** Living docs, grants, and marketing must distinguish two MIG-05
claim levels. This ADR does **not** rewrite hashed
`ARCHITECTURAL_MIGRATION_SPEC_V1`, Architecture Freeze, or Local AI Spec
blobs, and does **not** authorize a non-PDF evidence indexer or MIG-06+.

**Claim levels (binding):**

**A. MIG-05 PDF evidence-embedding delivery slice (steps 1–4) — COMPLETE**
for engineering checkpoint language when all of the following are true (as of
step 4): evidence-level `MemoryEvidenceEmbeddingStore`; product meaning search
ranks evidence embeddings (not `PdfPageEmbedding*`); `PdfPageEmbedding*`
entity/DAO/store/table retired; PDF page path indexes into the evidence store
(`IndexPdfPageEmbeddings` evidence-only). Engineering may say “MIG-05 PDF
slice complete” or “MIG-05 steps 1–4 delivered.”

**B. Migration Spec MIG-05 full acceptance — STILL OPEN** until non-PDF
`MemoryEvidence` kinds can be embedded/indexed at evidence granularity per
Spec, **or** a future ADR explicitly reinterprets MVP scope against Spec
(not this ADR rewriting the hashed Spec). Until B closes, do **not** say
“MIG-05 complete,” “MIG-05 done,” or “Spec MIG-05 done” in grants, public
pack, marketing, or unqualified CONTINUE/CHANGELOG headlines.

**Binding interpretation:**

1. Claim **A** is the honest label for the shipped PDF evidence-embedding
   delivery (steps 1–4). It does not equal Spec full acceptance.
2. Claim **B** remains open while the non-PDF evidence indexer is deferred
   (see `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE` FULL DONE checklist).
3. Grants / public / marketing **MUST NOT** claim MIG-05 or Spec MIG-05
   complete without stating that non-PDF evidence indexing remains open, or
   wait until B closes.
4. A later ADR may reinterpret MVP scope against Spec for B; this ADR does
   not do that and does not amend hashed Spec / Freeze / Local AI Spec.
5. Canonical Recall remains target-only (ADR-049). Live/Dual **N** is
   unchanged by this ADR (N=6 after MIG-05 step 4).

**Out of scope / non-claims:**

- Does not authorize implementing the non-PDF evidence indexer now
- Does not authorize MIG-06, MIG-07, MIG-07B, or other MIG code
- Does not claim Canonical Recall exists in code
- Does not change Room schema, search implementation, or Live/Dual N
- Does not rewrite hashed Freeze / Migration Spec / Local AI Spec /
  Grounding / Experience Memory / Product Contract blobs
- Does not close the MIG-05 FULL DONE checklist’s non-PDF box

**Reason:** Step 4 retired `PdfPageEmbedding*` and completed the PDF
evidence-embedding delivery slice, while Spec MIG-05 still expects evidence
granularity beyond PDF. Without two claim levels, CONTINUE / grants risk
saying “MIG-05 complete” when only A is true.

**Consequences:** Registry, CONTINUE, MIG-05 FULL DONE checklist, enforcement
index pointer, changelog, and this change-control record A vs B. Engineering
checkpoints may celebrate A; Spec/full claims wait for B.

## ADR-051: Evidence-native on-device RecallRanker

**Status:** Accepted

**Decision:** Spec `RecallRanker` inside Canonical Recall (ADR-049) is
implemented as an **Evidence-native on-device RecallRanker**: it reorders a
bounded pool of `MemoryEvidence` candidates using (1) a portable semantic
relevance head and (2) structured evidence signals already present on stored
Memories — not bare opaque `(query, passage)` text alone as the sole product
ranking story. FC-02 remains the delivery vehicle; this ADR upgrades what
"FC-02 done" means.

**Binding interpretation:**

1. **Single boundary.** All model-backed and multi-signal ranking for product
   Find stays inside Canonical Recall via `RecallRanker`. No new Find path.
   Live/Dual **N** must not grow. Keyword Find is out of scope unless a later
   ADR says otherwise (unchanged from FC-02 non-goals).

2. **Evidence-native thesis.** A candidate is ranked as stored evidence: excerpt
   text plus available structured signals (locator kind/value, evidence class /
   kind, anchors when present, lexical completeness against the cue, and
   honest corpus / availability context when surfaced). The semantic head may
   score text; product ranking must not pretend those structured signals do
   not exist. Why copy may cite inspectable factors; it must not invent
   evidence or disclose unsupported model-private reasoning.

3. **Two delivery stages (binding):**
   - **Stage A — Semantic head (FC-02 slice 2 baseline):** Bound an on-device
     relevance model (cross-encoder or measured equivalent) behind
     `RecallRanker`, pool default **40** / max **50**, wire through
     `AnchorAwareMeaningRecallRanking`, fall back to `IdentityRecallRanker`
     when the pack is unavailable, and show measurable lift on
     `meaning-pdf-page-recall-v1` (or successor) with midrange latency within
     budget or explicit degraded honesty. Stage A alone may ship as an
     engineering checkpoint; it is **not** the full ADR-051 product claim.
   - **Stage B — Evidence-native differentiator:** Extend candidates and rank
     outputs so structured evidence signals participate in ordering (and Why
     factors where disclosed). Stage B is the extraordinary / high-value bar
     this ADR exists to authorize. Marketing or public "evidence-native
     ranker" language requires Stage B measured green, not Stage A alone.

4. **FC-03 dependency for Stage B.** Stable per-type evidence chunking and
   locators (FC-03, starting with PDF page / OCR block / note section) are a
   **hard dependency before Stage B is declared complete**. Ranking cannot
   honestly claim evidence-native quality on mis-chunked passages. Stage A may
   proceed on current evidence slices; P2 may resequence FC-03 ahead of or in
   parallel with Stage B under existing change-control discipline.

5. **Type-agnostic ranker; type-specific ingress.** Future asset types (Word,
   Excel, PowerPoint, bookmarks, audio, video, and others) enter Find only via
   permissioned discovery → extraction → chunking + locator → MemoryBuilder →
   `MemoryEvidence`. `RecallRanker` scores stored evidence; it does not
   open raw files and does not become a per-format product search system.
   Each new Asset type still requires its own ADR / change control for
   access and extraction. Multimodal (non-text) evidence ranking needs a
   later ADR; this ADR covers text evidence passages first.

6. **Local-first and offline.** Core ranking runs on-device on already-stored
   excerpts. No cloud rerank API. Pack install may use network for **model
   bytes only** (same pattern as the embedding AI Pack). Clear index / pack
   removal must leave Find functional via identity / prior stages.

7. **Model selection is measured, not fashion.** The concrete Stage A pack
   (runtime, license, size, latency on Galaxy A15-class devices) is chosen in
   a follow-up change-control delivery record with benchmark evidence. This
   ADR does not freeze MiniLM, MediaPipe, ONNX, or any vendor artifact by
   name.

8. **Personalization is deferred.** Opt-in local feedback weights (open /
   select / dismiss) remain governed by Experience Memory Amendment §7 and
   require a later ADR. They are not Stage A or Stage B of this ADR.

**Out of scope / non-claims:**

- Does not authorize Grounded Answers runtime, Verifier, Act, Connect, or
  chat-first entry
- Does not authorize cloud inference on Memories or source content
- Does not authorize ColBERT, GraphRAG, multi-agent orchestration, or SLM as
  the primary Find ranker
- Does not rewrite hashed Freeze / Migration Spec / Local AI Spec / Grounding /
  Experience Memory / Product Contract blobs
- Does not claim marketing AVAILABLE or public "evidence-native ranker shipped"
  from this ADR alone
- Does not by itself bind a concrete model file or start App wiring — FC-02
  change control (updated under this ADR) remains the implementation gate
- Does not extend Live/Dual legacy rows

**Reason:** Industry default rerank (generic cross-encoder on top-k text) is
necessary but insufficient for UNFYND's purpose: trusted personal retrieval
over structured `MemoryEvidence` with explainable Why, offline operation,
and future asset types. Spec `RecallRanker` and FC-02 named the port and
the industry stage; this ADR locks the product-differentiating interpretation
so delivery does not collapse to "another MiniLM wrapper" while still
requiring a measured semantic baseline (Stage A) before the differentiator
(Stage B).

**Consequences:**

- `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK` is reinterpreted as Stage A/B
  under this ADR; slice 2 = Stage A; Stage B + FC-03 sequencing recorded there
  or in successor change control.
- `POST_MVP_PROGRAM_V1` P2 and `FUTURE_CAPABILITY_BACKLOG` FC-02 notes
  must cite ADR-051 (evidence-native, not text-only).
- Registry / CONTINUE / changelog record acceptance.
- Implementation proceeds only under FC-02 (and FC-03) change control with
  architectural convergence blocks; Live/Dual **N** unchanged by this ADR.

## ADR-052: Smart automatic AI pack onboarding (launch-ready UX)

**Status:** Accepted

**Decision:** At **launch-ready / marketing AVAILABLE**, on-device intelligence
packs install through **smart automatic onboarding** — not the interim engineering
setup path (separate disclosure tabs, manual “download meaning model”, manual
“download rerank pack”). The user gets **one combined, honest disclosure** and
**one primary Continue**; the app then **automatically** downloads and activates
only the packs appropriate for this phone (`RecallRankDevicePolicy` tiers).

**Binding interpretation:**

1. **Launch UX bar.** After source permission / first indexing setup, enabling
   meaning search must not require hunting multiple setup screens. Target:
   **one affirmative Continue** on the unified onboarding step (combined
   disclosure), then visible progress — not a chain of optional engineer tabs.

2. **Combined disclosure (not silent).** A single screen (or wizard step) discloses
   total download size, storage, network use (**model bytes only**), and licenses
   for every pack that **may** install on this device class. One user approval
   satisfies `AI_PACK_DELIVERY_SECURITY_PLAN` explicit user-approved download
   for all packs listed in that disclosure. Silent background install without
   disclosure is forbidden.

3. **Smart automatic install after Continue.**
   - **Meaning pack (USE / embedding):** Auto-download and verify on capable
     phones when user continues unified onboarding (core MVP path).
   - **Rerank pack (ADR-051 Stage A+):** Auto-download **in the same flow** when
     `RecallRankDevicePolicy` tier is FULL or REDUCED — not a second product
     journey. Skip rerank bytes on IDENTITY_ONLY tier.
   - **Index build:** Auto-start meaning index build after meaning pack is active
     (with honest progress); user should not need a separate “build index” tap for
     the default first-run path unless batch limits require “continue indexing”
     later.

4. **Device-aware (broader coverage).** `RecallRankDevicePolicy` + platform RAM/ABI
   signals decide FULL (pool 40) / REDUCED (pool 20) / IDENTITY_ONLY (no rerank).
   Weak phones still get keyword Find and honest copy; they are not forced to
   download packs that will not run well.

5. **Interim engineering path.** Today’s `AiPackDisclosureScreen` multi-button
   flow (acknowledge → download → build index) remains valid for **engineering
   checkpoints and device proof** until unified onboarding ships. It is **not** the
   launch-ready product UX and must not be described as final in grants or
   marketing.

6. **Not in APK.** Packs stay out of the base APK (Spec §6 small APK). Smart
   automatic means **automatic after consent**, not bundling weights in the store
   install.

7. **Wi‑Fi preference (recommended).** Launch UX should prefer Wi‑Fi for large
   pack bytes when not on Wi‑Fi (warn or defer — product copy in implementation
   slice). Hard Wi‑Fi-only gate is not required by this ADR.

8. **Relationship to ADR-051.** Rerank remains **optional to meaning Find**
   (keyword + meaning work without rerank model). “Optional” means **not required
   for search to function**, not “user must find a second download screen.” On
   eligible phones, rerank is **auto-installed in the unified flow** after the
   same Continue.

**Out of scope / non-claims:**

- Does not authorize silent install without disclosure
- Does not bundle model weights in APK
- Does not claim AVAILABLE or ship unified onboarding from this ADR alone
- Does not remove per-tap index batch limits where memory requires them
- Does not rewrite hashed Freeze / Spec blobs

**Reason:** MVP meaning search is core product value; launch UX must feel
automatic while preserving privacy disclosures and device-tier honesty. Separate
engineering tabs were for verified checkpoints (A-01, M4), not consumer onboarding.

**Consequences:**

- FC-02 / rerank model brief and `RecallRankAiPackTrack` interpret install as
  **smart automatic on eligible tiers** inside unified onboarding.
- Future launch slice: replace or wrap interim `AiPackDisclosure` with unified
  onboarding change control.
- Registry / CONTINUE / changelog record ADR-052.

## ADR-053: User-tapped share of a stored original URI is Open-adjacent, not Act

**Status:** Accepted  
**Date:** 2026-09-14

**Decision:** A person tapping **Share** on an already-opened original, which
opens the Android share sheet with a **read-only grant** of the stored content
URI (PDF: the same tree-document URI Open would read; photo/screenshot: a
readable MediaStore candidate), is the same class of work as **Open original**.
It is **not** Act.

ADR-043 still stands: Act / agentic tools that act on the user's behalf,
reminders, mutation of originals, and multi-step life tasks remain out of
current architecture until a later ADR **and** a product-contract change.

**Binding interpretation:**

1. **Who initiates.** Only an explicit user tap. UNFYND never shares in the
   background and never chooses the destination app.
2. **What is handed off.** The original the person already owns, via a content
   URI this app already had a grant to read. For a PDF that is the **whole
   file**, not the cited page. Notes have no local file URI in this slice;
   Open still goes to OneNote.
3. **What is granted.** `FLAG_GRANT_READ_URI_PERMISSION` only. No write grant.
   No copy of the original into UNFYND durable storage. No upload.
4. **What this is not.** Not Act product. Not AVAILABLE. Not a second Find
   path. Not `ACTION_VIEW` as a hidden editor. Not FileProvider cache of user
   pixels (rejected for this slice; stored URI only).
5. **Honest failure.** If the URI cannot be read or no target can accept it,
   say so. Do not silently copy the file to make share work.

**Out of scope / non-claims:**

- Reminders, agents, chat, mutation of originals
- Sharing a OneNote page as a local file
- Claiming Act or marketing AVAILABLE
- Rewriting hashed Product Contract / Freeze blobs

**Reason:** J17 / ACT1 asked for “open it, share” as Act-shaped jobs. Open
already existed. A share sheet of a stored URI lets the person hand *their*
file to *their* app without building an agent. Recording the boundary prevents
two failures: treating this as permission to implement Act, and refusing a
read-only handoff because the noun “share” appears in the Act row.

**Consequences:**

- Implementation under `CHANGE_CONTROL_OPEN_ORIGINAL_SHARE.md`
- Registry / CONTINUE / changelog record ADR-053
- HUMAN_RECALL J17 and MEANING_FIND ACT1: share sheet landed; remind remains out


