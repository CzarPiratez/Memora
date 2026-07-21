# Local PDF Parser Isolation Review

**Status:** Accepted architecture guardrail; synthetic boundary emulator-verified;
real-source parsing remains disabled
**Date:** 2026-07-21
**Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06

## Decision

Before Memora parses any real user PDF, PDFBox must run in a dedicated Android
service declared with `android:isolatedProcess="true"` and `android:exported="false"`.
The ordinary Memora process remains the only component that can verify a persisted
SAF grant and open the selected document. It may pass one duplicated, read-only
`ParcelFileDescriptor` to the isolated service for one bounded request. It must never
pass a content URI, tree URI, filesystem path, folder handle, account token, or broad
source permission to that service.

The service returns only bounded deterministic parse facts through a private Binder
contract. The ordinary process validates those facts against `PdfExtractionRequest`
and constructs the domain `PdfExtractionRecord`; the isolated service does not access
Room, Hilt bindings, the source repository, Compose UI, or the network.

This decision authorizes only the documented synthetic-service boundary. It does not
authorize opening a real PDF, changing the visible app, persisting extraction output,
or scheduling background parsing. Those require the acceptance gates below.

## Why isolation is required

PDFs are complex, potentially malicious input. A parser failure can consume memory or
CPU, crash a process, or exploit parser/native code. Android documents the same
separate-isolated-process precaution for untrusted PDF rendering. The standard
`<service>` manifest attribute gives an isolated service no permissions of its own;
the service API is its communication boundary. Therefore, a private isolated service
reduces the blast radius without weakening Memora's local-first promise.

Official platform references reviewed on 2026-07-21:

- [Android service manifest: `android:isolatedProcess`](https://developer.android.com/guide/topics/manifest/service-element)
- [Android `PdfRenderer` security guidance for untrusted PDFs](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [Android `ParcelFileDescriptor` ownership and duplication](https://developer.android.com/reference/android/os/ParcelFileDescriptor)
- [Android auto-closing descriptor input stream](https://developer.android.com/reference/android/os/ParcelFileDescriptor.AutoCloseInputStream)

## Required data flow

```text
Approved SAF source
  -> ordinary Memora process: fresh grant check + one read-only descriptor
  -> duplicate descriptor over private Binder
  -> isolated PDF parser service: PDFBox + bounded deterministic facts
  -> private Binder result chunks
  -> ordinary Memora process: validate + later application use case
```

The source broker must obtain the descriptor only after it has checked the exact,
persisted Android read grant associated with the source. A descriptor is a narrow,
single-document capability. The service receives no URI, so it cannot enumerate a
folder or reopen a different document. A `ParcelFileDescriptor.AutoCloseInputStream`
must own and close the received descriptor in the service; the ordinary process closes
its original descriptor as soon as ownership has been safely transferred. No temporary
source-file copy is permitted.

The first Binder contract must be deliberately small:

- request: opaque request ID, fixed extraction-schema version, and one descriptor;
- result: title, declared metadata, page count, and page text in bounded page/chunk
  responses; no URI, path, source ID, asset key, password, or raw exception text;
- control: explicit completion, cancellation, and close operations; and
- failure: a small failure category only. The ordinary process maps it to the
  user-safe `PdfExtractionOutcome` message and never logs source text or a password.

Page output must never be returned as one unbounded Binder transaction. The later
implementation must use a session/chunk protocol, validate every chunk, and reject a
duplicate, out-of-order, oversized, or schema-incompatible response. The ordinary
process creates the final domain record only after all expected pages are present.

## Threat model and mandatory controls

| Risk | Required control |
|---|---|
| Malformed objects, decompression bombs, parser/native vulnerability | Parse only in the private isolated service; no parser is invoked in the UI, Room, source broker, or ordinary app process. |
| Broad or revoked document access | The ordinary process freshly validates one persisted SAF read grant before opening one descriptor. The service receives no URI or permission. |
| Parser CPU/memory stall | Do not enable real-source parsing until representative-device measurements establish versioned file-size, page-count, text-output, elapsed-time, and memory budgets. Use bounded page/chunk work and cooperative cancellation. A service timeout is a retryable failure; it is not a false completion. |
| Binder transaction or output-memory exhaustion | Stream bounded page/text chunks, validate length before retaining them, and discard the entire attempt if the complete record cannot be assembled. |
| Leaking source content through logs, errors, or diagnostics | Do not include PDF text, title, URI, path, source key, password, or raw parser exception in logs, analytics, binder errors, or user messages. |
| Network or source mutation | The current manifest has no `INTERNET` permission. The isolated service has no permissions of its own and must declare no network, storage-write, provider, or export capability. It reads the supplied descriptor only. |
| Binder caller other than Memora | The service is explicit, non-exported, has no intent filter, and rejects malformed/unknown request versions. |
| Service crash or process death | Treat Binder death, bind failure, and incomplete output as an atomic retryable extraction failure. Close both descriptor sides, retain no partial searchable record, and leave the original document untouched. |

An isolated process limits damage; it is not a promise that a malformed PDF can never
consume resources. The measured input/output budgets and device test matrix remain
release gates. Android does not provide a general, safe app-level hard kill guarantee
for a currently executing parser request, so an implementation must combine service
isolation, cooperative cancellation, timeout reporting, bounded inputs, and device
measurement rather than claim a timeout alone stops all parser work immediately.

## Rejected alternatives

| Alternative | Rejection reason |
|---|---|
| Parse in the ordinary app process | A parser crash or exploit has the same process privileges as source access, Room, and UI. |
| Give the isolated service a SAF URI | An isolated service has no permissions of its own, and passing a URI would expand the service boundary beyond the single approved document. |
| Copy the PDF into app storage before parsing | It duplicates original user content, adds retention/cleanup risk, and is unnecessary for the descriptor-based design. |
| Cloud parsing or a remote AI service | Contradicts the accepted local-first specification and the core privacy contract. |
| Replace PDFBox with `PdfRenderer` | The current product support floor is API 26 and P-07 requires deterministic text extraction; the selected parser remains necessary for that scope. |

## Implementation gates

No real user source can be opened until all of the following are complete and recorded:

1. a private isolated-service manifest declaration and a minimal versioned Binder
   contract are implemented without exposing a URI or path;
2. descriptor ownership/closure, service-process identity, and no-export/no-permission
   manifest assertions pass on the Medium Phone emulator;
3. synthetic integration tests cover valid text, no-text, password, malformed input,
   service death, cancellation, timeout reporting, malformed result chunks, and
   descriptor cleanup;
4. offline runtime testing succeeds after dependencies are cached and the emulator
   network is unavailable;
5. representative-device measurements establish and document actual input, output,
   latency, memory, battery, and thermal limits plus recovery copy;
6. the platform adapter proves fresh grant validation before descriptor opening and
   maps revoked access before any parser call;
7. a persistence design stores only validated complete/explicit outcomes atomically;
   no incomplete parser response becomes searchable; and
8. a user-visible explicit foreground flow explains the source scope, local parsing,
   progress, pause/retry, and failure state before any document is opened.

If any gate fails, Memora must leave the Asset unextracted/retryable and must not
silently fall back to filename search, upload the PDF, or copy it elsewhere.

## Change-control record

### Pre-work record

- **Requirement IDs:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** product registry, Local AI Technical Specification,
  governance, continuation record, product contract, architecture, decisions,
  roadmap, traceability, PDF extraction plan, and change-control template.
- **Current-code evidence inspected:** the manifest has no `INTERNET` permission and
  declares a private non-exported isolated parser service; SAF discovery is
  metadata-only; `PdfBoxPdfDocumentMapper` accepts only a supplied `InputStream`;
  the service accepts only a descriptor and fixed protocol version; Android synthetic
  mapper tests previously passed 4 of 4; the service test passed 6 of 6 on the
  Medium Phone emulator using synthetic descriptor pipes.
- **Open ADRs / platform limitations checked:** ADR-003 remains unrelated and open;
  image-only PDF OCR remains a Local AI follow-up; the current parser must not open a
  real source until this isolation design's gates are met.
- **Privacy, source-access, dependency, offline, and data-retention impact:** no
  source is opened in this decision. The future service receives one ephemeral,
  read-only descriptor only and retains no source copy. No network or new dependency
  is introduced.
- **Smallest safe change:** document the mandatory isolation boundary and testable
  acceptance gates only.
- **Acceptance criteria:** the decision gives a narrow descriptor-only design,
  records rejected alternatives and known limits, and leaves real-source parsing
  disabled.
- **Test and emulator verification plan:** documentation/diff review now; later
  synthetic isolated-service instrumentation and offline/device tests as listed above.
- **User-visible quality/accessibility review plan:** no visible behavior changes in
  this decision. A later flow must use plain language and provide clear pause/retry
  states before source opening.

### Delivery record

- **Files/layers changed:** private AIDL contract, isolated Android service, parser
  split, manifest declaration, and a synthetic-only Android integration test.
- **Automated verification and result:** `:app:assembleDebugAndroidTest` passed on
  2026-07-21. The expanded test APK compiles no-text, password, malformed,
  unsupported-protocol, and descriptor-cleanup cases; it does not prove their live
  isolated-service execution.
- **Emulator/manual verification and result:** on 2026-07-22, the user ran
  `IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: 6 of 6
  tests passed. They prove no-export/isolated manifest configuration, a two-page
  repository-owned synthetic descriptor parse, no-text and password outcomes,
  malformed-input retryability, unsupported-protocol rejection, and caller-side
  descriptor closure after each request.
- **Failure/recovery paths verified:** no-text, password-protected, malformed, and
  unsupported-protocol synthetic outcomes are explicit; the service closes its input
  stream in the normal parse path and the test closes the caller-side descriptor after
  every request. Service death, cancellation, timeout, malformed-output chunks, and
  every error-path descriptor-closure case remain unverified and are not enabled for
  real sources.
- **Known limitation or follow-up:** exhaustive descriptor closure, service death,
  cancellation, timeout, bounded chunks, offline runtime verification, measured
  budgets, a real-source adapter, persistence, and UI remain separate steps.
- **Documentation/traceability/ADR updates:** ADR-017 and the continuation record
  must reference this review in the same checkpoint.
- **Git commit:** pending this documentation checkpoint.
