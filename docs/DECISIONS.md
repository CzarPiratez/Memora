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

**Status:** Open - requires an explicit product decision

**Facts:**

- The MVP requires automatic indexing of Notes.
- The PRD excludes user accounts and cloud sync.
- Android cannot permit Memora to scan another app's private note database.

**Viable choices:**

1. Allow one narrowly scoped, read-only provider connector (recommended first:
   OneNote) and explicitly treat it as source authorization rather than Memora user
   accounts or write-back sync.
2. Narrow the MVP notes claim to note files in user-approved storage locations.
3. Remove automatic existing notes from the MVP; this conflicts with the current PRD
   and is not recommended.

**Rule:** Do not implement or advertise automatic indexing of arbitrary note apps until
this decision is accepted.

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
live service-death recovery, a bounded result protocol, fresh-grant verification,
atomic persistence design, and an explicit user-facing flow remain mandatory gates.
The explicit private binding adapter and ordinary-process client are now
emulator-verified together through one repository-owned descriptor; this still does
not enable a real source. A versioned, bounded page/text result protocol remains a
mandatory gate before the service can return content. No partial result becomes
searchable. The full threat model, rejected alternatives, and implementation gates
are in
`docs/PDF_PARSER_ISOLATION_REVIEW.md`.
