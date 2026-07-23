# SAF PDF Platform Descriptor Broker Plan

**Status:** Synthetic-only descriptor broker and broker-to-parser handoff verified;
real user-source parsing remains disabled.
**Date:** 2026-07-23
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Governing guardrail:** ADR-017 and `docs/PDF_PARSER_ISOLATION_REVIEW.md`.

## Purpose and boundary

This plan defines the only permitted platform path by which Memora may later open
one user-approved PDF for deterministic local extraction. It is a data/platform
adapter, not a UI, application, domain, Room, WorkManager, or parser-service
responsibility.

The broker's single responsibility is to convert an already validated
`PdfExtractionRequest` into one borrowed, read-only descriptor for a narrowly scoped
data-layer consumer. A dedicated ownership adapter may duplicate that borrowed handle
once to transfer ownership to the existing private isolated parser client. Neither
component may expose a descriptor, URI, path, or tree reference above the
data/platform boundary. Neither may copy, move, rename, modify, upload, or retain the
original document.

No implementation is authorized by this plan alone. It provides the precise design,
test corpus, failure behavior, and review gates required before a separate,
user-approved code step may open a repository-owned test document. Real user-source
parsing remains disabled.

## Authoritative Android basis

- Android documents are accessed through a valid URI grant. A tree-derived document
  URI built by `DocumentsContract.buildDocumentUriUsingTree` uses the selected tree's
  subtree grant, and Android requires the target to be a descendant of that tree.
  [Android DocumentsContract reference](https://developer.android.com/reference/android/provider/DocumentsContract)
- API 29+ additionally exposes `DocumentsContract.isChildDocument` for a provider
  check that a candidate is a descendant of a parent document.
- `ContentResolver.openFileDescriptor(uri, "r", cancellationSignal)` creates a
  caller-owned descriptor and may return `null` or a stream-backed pipe; it must be
  closed by the caller. The exclusive `"r"` mode is intentional.
  [Android ContentResolver reference](https://developer.android.com/reference/android/content/ContentResolver)
- `ParcelFileDescriptor.dup()` creates a separately closeable duplicate with shared
  file-position semantics. The broker never reads either descriptor.
  [Android ParcelFileDescriptor reference](https://developer.android.com/reference/android/os/ParcelFileDescriptor)

## Preconditions

The future broker may start only when all of these are true:

1. the request is a validated `PDF` `Asset` and has a supported extraction schema;
2. the document-tree repository finds one exact `DocumentTreeApproval` using the
   Asset's `SourceId`;
3. `DocumentTreeAccessValidator` reports `GRANTED` immediately before platform URI
   construction and again immediately before opening the descriptor;
4. the request passes `ApprovedPdfDescriptorCustodyContract`; and
5. the request's Asset fingerprint is still current under a separately designed
   minimal metadata revalidation step. A changed source is rediscovered/requeued; it
   is not parsed as the older Asset version.

The current domain outcome shape lacks an explicit stale-source result. That outcome
must be designed, tested, and persisted before a broker can claim a changed document
was handled correctly.

## Exact custody algorithm

All steps run on an IO dispatcher outside Compose and outside the isolated parser
service. No original content is read until step 9.

1. Receive only `PdfExtractionRequest` plus a cancellation signal. Do not accept a
   caller-supplied URI, path, descriptor, or approval.
2. Look up the exact private approval using `request.asset.identity.sourceId`. Missing
   approval returns the existing safe access-required outcome.
3. Freshly inspect Android's retained read grants for that exact approval. A missing
   or readless grant returns access-revoked before any provider query or open.
4. Call `ApprovedPdfDescriptorCustodyContract`. Any non-authorized result returns its
   corresponding content-free outcome and stops.
5. Parse the approved tree URI. Reject a non-`content` tree URI, a malformed URI, or
   a non-document Asset location without attempting to open anything.
6. Obtain the opaque document ID solely from the Asset's `SourceAssetKey`, which was
   produced by the approved tree's metadata discovery. Treat it as opaque: never
   split, prefix-match, normalize, or compare it as a filesystem path.
7. Construct the target only with
   `DocumentsContract.buildDocumentUriUsingTree(approvedTreeUri, documentId)`. Verify
   that its authority agrees with the approved tree. The broker must not directly
   use the persisted `AssetLocation` as an opening authority; it is only a
   consistency/audit reference.
8. On API 29+, construct the tree-root document URI from the approved tree and ask
   `DocumentsContract.isChildDocument` whether the canonical target is a descendant.
   A `false`, `SecurityException`, `IllegalArgumentException`, or provider failure
   denies the attempt. The first broker implementation returns an explicit
   `UnsupportedPlatform` result on API 26-28; it does not attempt a string-based or
   metadata fallback. A future targeted metadata-revalidation fallback needs its own
   acceptance corpus before it can open anything on those releases. This support
   difference must be shown in the capability matrix and user-safe fallback state.
9. Freshly inspect the retained read grant one last time, then open the canonical
   target once with `contentResolver.openFileDescriptor(target, "r", signal)`.
   `null`, cancellation, security, not-found, and provider errors are mapped to a
   bounded safe outcome. The broker never calls a write mode, `openOutputStream`,
   copy/move/delete/rename API, or `openInputStream`.
10. Duplicate the caller-owned descriptor exactly once with `descriptor.dup()`. If
    duplication fails, close the original and return a retryable safe failure. The
    broker owns that duplicate for the duration of its data-layer consumer callback.
11. Close the original descriptor immediately after successful duplication. A parser
    ownership adapter must duplicate the broker-owned handle once more before calling
    `IsolatedPdfParserClient`; the client owns and closes that transferred duplicate on
    completion, cancellation, timeout, malformed response, Binder death, and binding
    failure. The broker closes its own borrowed duplicate when the callback returns.
    The isolated service closes its received Binder descriptor through
    `AutoCloseInputStream`. This explicit second duplication avoids ambiguous
    double-ownership between the broker and the client.
12. Validate the parser result against the request and bounded result contract. Only a
    later application/persistence step may create a `PdfExtractionRecord`; no
    partial, failed, stale, or unavailable attempt becomes searchable.

## Ownership and prohibited data flow

```text
private Room approval + request Asset
  -> ordinary-process SAF broker: exact approval, grant, canonical tree target
  -> one read-only original descriptor (broker-owned)
  -> one duplicate borrowed by broker consumer (broker-owned)
  -> one ownership-transfer duplicate (parser-client-owned)
  -> private Binder transfer (isolated-service-owned received descriptor)
  -> bounded parse result only
```

The isolated service receives neither tree URI, document URI, source ID, document
ID, display name, Asset location, fingerprint, credentials, account data, nor a
broad source permission. The application layer receives only a source-neutral
`PdfExtractionOutcome`. Nothing writes the source document or copies it to Memora
storage.

## Required outcome taxonomy

The later implementation must map failures without exposing a URI, title, path,
document ID, password, raw exception, or extracted text in logs, analytics, or UI.

| Condition | Required behavior |
|---|---|
| No approval / access not yet granted | content-free access-required state; explain how to reconnect |
| Grant absent/revoked before open or `SecurityException` | content-free access-revoked state; no parser call |
| Source/approval mismatch or canonical-tree mismatch | safe failed state; security diagnostic only in private development test output |
| Source changed since discovery | stale/re-discovery-required state; no parse of stale Asset version |
| Cancellation before or during open | retryable cancellation state; close any opened descriptor |
| `null` descriptor, provider crash, I/O/not-found/duplication failure | retryable source-unavailable/failed-safely state; close any owned descriptor |
| Parser timeout, Binder death, malformed response | existing content-free retryable parser failure; descriptor cleanup is mandatory |

## Required implementation tests

No test may open a user document. The integration fixture must be a test-only,
repository-owned `DocumentsProvider` (or equivalent isolated Android fixture) that
contains a synthetic PDF and records only operation categories, never document text.

1. **Pure target/custody tests:** exact source acceptance; foreign source denial;
   opaque document IDs are never path-parsed; canonical URI derives from the approval
   tree plus `SourceAssetKey`, not `AssetLocation`; malformed/non-content authority
   inputs deny safely.
2. **Fresh-grant sequencing test:** a recording fake verifies a current grant check
   occurs before target construction and immediately before open. Revocation between
   those checks causes no parser submission.
3. **Tree-membership test:** API 29+ must exercise false and true
   `isChildDocument` results. API 26-28 must prove no heuristic or raw location URI
   is used and that the first implementation returns `UnsupportedPlatform` without
   opening a descriptor. Any later metadata-revalidation fallback needs separate
   tests before it can be enabled.
4. **Read-only/open test:** the test provider observes only `"r"`; no output,
   mutation, copy, or folder enumeration call is permitted from the broker.
5. **Descriptor ownership tests:** verify original closure after successful `dup`,
   closure after every duplication/open failure, and parser-client closure after
   cancellation, timeout, Binder death, and malformed response.
6. **Boundary test:** assert that neither the binder request nor isolated-service
   result contains URI/path/source identity fields, and the service remains
   non-exported and isolated.
7. **Integration run:** on the Medium Phone emulator, parse only the fixture
   provider's synthetic PDF after a fixture tree grant. Verify a complete/explicit
   outcome, no source mutation, no persisted extraction, no network, and descriptor
   cleanup. The first live user-source test needs a separately approved privacy
   screen, fixture-free test plan, and user instruction.
8. **Regression matrix:** rerun the existing parser isolation, cancellation,
   offline, result-codec, process-death, and fresh-grant suites. Record measured
   resource behavior before enabling foreground user-source parsing.

## Acceptance gate for the next code step

The next implementation may add only a testable platform broker and a test-only
synthetic DocumentsProvider fixture. It may not bind the broker to UI, WorkManager,
Room extraction persistence, semantic understanding, or real user documents. It
must pass the targeted unit and emulator tests above, add no permission or dependency,
and leave the running app's visible behavior unchanged.

## Delivery checkpoint: canonical target boundary

The first broker component is now implemented as
`SafPdfCanonicalDocumentTargetFactory` in the SAF data/platform layer. It constructs
an internal target exclusively from the approved tree URI and opaque
`SourceAssetKey`; it never uses the stored `AssetLocation` as an opening target. It
rejects non-PDF Assets, source mismatch, invalid/non-content tree approval, and a
location from another provider. It does not query a provider, validate a grant, open
a descriptor, read a source, call the parser, persist data, or change UI.

`SafPdfCanonicalDocumentTargetFactoryIntegrationTest` contains five Android tests
for these rules. They use only synthetic URI strings and no document provider; the
test-only provider fixture and all descriptor-opening behavior remain the next
separate slice. On 2026-07-23, the user ran this test on the Medium Phone emulator:
**5 tests passed**. It did not request permission or open a provider, descriptor,
document, parser, database, UI, worker, AI capability, or network connection.

## Delivery checkpoint: verified synthetic descriptor broker

An unbound `SafPdfDescriptorBroker`, its `ContentResolver` platform adapter, and a
debug-only synthetic `DocumentsProvider` are now implemented. The debug provider is
excluded from release builds. The broker looks up the exact approval, checks access
twice, uses the existing pure custody gate and canonical target, verifies Android
tree membership on API 29+, opens only `"r"`, duplicates the descriptor once, and
closes the original before the data-layer consumer receives the duplicate. The
consumer's descriptor is closed by the broker on every exit. API 26-28 returns a
safe unsupported-platform result and opens nothing.

The test fixture exposes one pipe-backed repository-owned synthetic PDF and rejects
all modes except `"r"`. The six Android tests cover successful synthetic handoff,
original/duplicate descriptor closure, denial before target/platform access,
revocation immediately before open, membership rejection without opening, and
foreign-source denial. The broker is not bound to Hilt, UI, WorkManager, Room
extraction persistence, semantic understanding, or the isolated parser client. On
2026-07-23, the focused test was run on the Medium Phone emulator: **6 tests
passed**. No user document is eligible to be opened or parsed, and the fixture
source appears only in the debug build.

## Delivery checkpoint: synthetic broker-to-isolated-parser handoff

`ParseApprovedPdfWithIsolatedParser` is an unbound application coordinator that
connects the existing custody broker to a `BorrowedPdfDescriptorParser` port without
exposing a descriptor outside the data/platform boundary. Its Android implementation,
`IsolatedPdfParserDescriptorHandoff`, duplicates the broker's borrowed descriptor
before transferring ownership to `IsolatedPdfParserClient`. The broker then closes its
own duplicate; the client closes its transferred duplicate. This preserves explicit
resource ownership on every success and failure path.

The debug-only provider now serves one valid, repository-owned one-page selectable
PDF. `ParseApprovedPdfWithIsolatedParserIntegrationTest` uses that fixture only. On
2026-07-23, it passed **3 tests** on the Medium Phone emulator: the exact
approval/grant/canonical-target/read-only path reaches the private isolated service;
revocation immediately before opening submits no descriptor to the parser; and a
content-free retryable parser failure is never represented as extraction. The new
coordinator has no Hilt binding, UI caller, WorkManager job, Room extraction write,
semantic-understanding/AI call, network path, or real user source. Its result is
status-only and is not searchable.

## Delivery checkpoint: bounded synthetic parser-result transport

The private isolated parser now uses protocol version 2 for repository-owned
synthetic descriptors. It transforms deterministic parser facts into the existing
strict page/chunk result codec, whose exact fields and complete page coverage are
validated before the Binder response leaves the isolated process. The synthetic-only
policy is deliberately fixed for this test boundary: at most 32 pages, four chunks
per page, 8,192 UTF-16 code-units per page, 65,536 in total, and 2,048 per chunk.

`IsolatedPdfParserClient` checks the isolated marker, exact outer-envelope shape, and
the strict inner result. It then deliberately discards every text chunk and returns
only the existing content-free outcome, retryability, and page count. This does not
create an extraction record, write Room, call understanding/AI, alter UI, schedule
WorkManager, use a real user source, or use network.

On 2026-07-23, the Medium Phone emulator completed **20 focused tests** across
`IsolatedPdfParserServiceIntegrationTest`,
`IsolatedPdfParserClientIntegrationTest`,
`IsolatedPdfParserEndToEndIntegrationTest`, and
`ParseApprovedPdfWithIsolatedParserIntegrationTest`. The cases cover complete
synthetic page coverage, malformed and over-limit content-free rejection, protocol
failure mapping, descriptor closure, and the approved synthetic broker handoff.

This is not a real-source approval. The one bounded synthetic Binder response is not
the future session/chunk streaming protocol required for user PDFs, and these
synthetic limits are not production resource budgets. Fresh real-source grant
validation, measured limits, streaming, atomic persistence, recovery UI, and all
remaining ADR-017 gates are still required.

## Pre-work record

- **Governing sources checked:** product registry, Local AI technical specification,
  governance, continuation record, product contract, architecture, ADRs, roadmap,
  traceability, PDF custody contract, PDF extraction plan, parser-isolation review,
  fresh-grant plan, and current source code.
- **Current-code evidence:** the current SAF catalog creates tree-derived document
  locations during metadata discovery; the fresh-grant validator checks only
  retained Android grants; the parser client accepts an already-opened descriptor
  and closes it; the isolated service is private and descriptor-only. No current
  production code calls `openFileDescriptor`.
- **Privacy/offline/dependency impact:** this document creates no code path, access,
  persistence, dependency, permission, AI model, network request, or data retention.
- **Open restriction:** ADR-017 continues to prohibit real-source parsing until this
  plan's implementation and acceptance gates are individually completed.
