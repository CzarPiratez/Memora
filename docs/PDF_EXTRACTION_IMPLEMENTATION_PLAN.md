# Local PDF Extraction Decision and Acceptance Plan

**Status:** Reviewed parser dependencies and synthetic-only mapper verified; no
source-opening adapter is enabled.
**Date:** 2026-07-21  
**Requirements:** P-05, P-07, P-14, P-15, P-17; future OCR capability A-01, A-03,
A-04, A-06.

## Decision

The first deterministic PDF text adapter will use
[`com.tom-roush:pdfbox-android:2.0.27.0`](https://github.com/tomroush/pdfbox-android)
behind the existing `PdfDeterministicExtractor` domain port. It will be a
read-only, local data/platform adapter. It will not be exposed to the UI, domain,
or application callers as a PDFBox type.

This is a dependency decision and implementation plan only. The parser dependency is
present in the Gradle catalog with reviewed, pinned transitive overrides. No user PDF
bytes are read, and no extraction record is persisted by this decision.

## Why this path

Memora's current Android support floor is API 26. Android's built-in `PdfRenderer`
can report page count from API 21, but its page text APIs arrive with Android V
(API 35); `PdfRendererPreV` makes those newer APIs available only for API 30 through
34. That leaves API 26 through 29 unable to meet P-07's text requirement.

The current Jetpack PDF artefacts provide a viewer/document-service capability but
are alpha releases and their documented compatibility begins at API 28. They also
solve a user-facing viewing problem that is not part of this extraction step.

PDFBox-Android is an Android port of Apache PDFBox, declares Apache-2.0 for its main
code, is distributed from Maven Central, and exposes deterministic PDF metadata and
text extraction without a network or AI provider. Using one parser for the first
supported range avoids device-version-dependent text results.

Sources checked on 2026-07-21:

- [Android PDF viewer and framework API compatibility](https://developer.android.com/develop/ui/views/layout/pdf/pdf-viewer)
- [AndroidX PDF release notes](https://developer.android.com/jetpack/androidx/releases/pdf)
- [PDFBox-Android project documentation](https://github.com/tomroush/pdfbox-android)
- [PDFBox-Android Maven Central metadata](https://central.sonatype.com/artifact/com.tom-roush/pdfbox-android)

## Explicit compatibility and quality boundary

| PDF condition | First deterministic adapter outcome | Truthfulness rule |
|---|---|---|
| Valid PDF with selectable text | Page count, available metadata, and page-level text with `Complete` coverage | It may proceed to later local understanding only after all pages are represented. |
| Valid PDF with no selectable text | Metadata/page count plus `NoExtractableText` | It must not invent text or claim the PDF is searchable by its image contents. |
| Password-protected, malformed, unreadable, revoked, or unsupported PDF | Explicit recoverable failure or access outcome | It must never be represented as an empty or fully extracted PDF. |
| Interrupted bounded run | `Partial` coverage with a reason | It is not eligible for a complete searchable Memory until completed/retried under the later persistence policy. |

Image-only/scanned PDFs reveal a real product gap: a parser cannot turn page images
into text. They require the separately governed local OCR capability. This preserves
P-07 rather than silently narrowing it; Memora will state the unavailable/partial
truth until OCR is implemented under the Local AI acceptance gate.

## Architecture and source-access plan

```text
application use case -> PdfDeterministicExtractor (domain port)
  -> SAF/PDFBox data-platform adapter -> Android persisted-read-grant check
  -> read-only source stream -> deterministic record/outcome
```

The future adapter must:

1. accept only a `PDF` Asset and find its exact approved SAF source by `SourceId`;
2. freshly verify Android's persisted read grant before opening the document URI;
3. operate off the main thread, close streams/documents promptly, and support
   cancellation between bounded units of work;
4. use memory-only parser processing for the initial design—no source-file copy,
   thumbnail, mutation, upload, or write-back path;
5. map only declared metadata and extracted page text into `PdfExtractionRecord`;
6. treat a changed fingerprint, revoked grant, parser exception, password, malformed
   input, cancellation, and resource limit as truthful outcomes; and
7. keep parser initialization and all Android/PDFBox types in the data/platform layer.

PDF parsers process untrusted input. Before enabling real user documents, the
implementation review must decide the isolated-process boundary or document why the
measured and mitigated in-process approach is acceptable. Android's own `PdfRenderer`
documentation recommends an isolated process for untrusted files; the same defensive
review applies here.

## Dependency, licensing, and supply-chain gate

The dependency change must include:

- the exact, pinned version in `libs.versions.toml`, with no dynamic version range;
- a checked-in third-party notice and license inventory for PDFBox-Android and every
  transitive dependency (including the Bouncy Castle dependencies declared by its
  Maven metadata);
- a dependency/SBOM and vulnerability scan recorded at the chosen version;
- a build-size measurement before and after the dependency is introduced;
- an offline runtime check proving no parser path contacts a network; and
- a rollback plan: remove the Hilt binding and dependency while preserving any
  unprocessed Assets as retryable, never deleting originals.

### 2026-07-21 dependency review result

`pdfbox-android:2.0.27.0` declares Bouncy Castle `1.72` transitively. An OSV scan
found advisories against `bcprov-jdk15to18:1.72` and
`bcpkix-jdk15to18:1.72`; those versions are therefore prohibited. Memora pins the
compatible Bouncy Castle provider, PKIX, and utility artifacts to `1.84` explicitly.
The same OSV batch scan reported no advisories for those exact `1.84` coordinates at
the time of review. The checked-in SBOM, notice inventory, and scan record are:

- `docs/dependency-review/pdfbox-android-2.0.27.0-sbom.cdx.json`
- `docs/dependency-review/pdfbox-android-2.0.27.0-vulnerability-review.md`
- `docs/THIRD_PARTY_NOTICES.md`

This is evidence at a point in time, not a permanent guarantee. The exact resolved
dependency graph must be checked again in CI and before release.

### Build-size measurement

On the same checkout and Medium Phone development environment, `:app:assembleDebug`
produced a debug APK of `12,423,988` bytes before this dependency set and
`18,734,630` bytes after it: an increase of `6,310,642` bytes (about `6.02 MiB`).
This is a development baseline, not a release-size promise. A release build and a
representative-device memory/battery measurement remain required before real-source
enablement.

No hard page-count, file-size, time, RAM, or battery target is assumed here. The
first real adapter must measure its fixture corpus on the representative Medium Phone
emulator/device tier and then record evidence-backed limits and user-facing recovery
copy.

## Privacy-safe fixture corpus and acceptance tests

All fixtures will be small, synthetic, repository-owned documents. No personal,
downloaded, or user-selected PDF is permitted in automated tests.

| Fixture | Expected result |
|---|---|
| Two-page selectable-text PDF with title, author, and Unicode text | Exact title/metadata, page count `2`, and complete numbered page text. |
| Selectable-text PDF with a blank/whitespace page | Complete page coverage with an empty page-text record, not a missing page. |
| Image-only/scanned-style PDF | `NoExtractableText`; no fabricated OCR text. |
| Password-protected PDF | Explicit, non-sensitive recoverable failure; no password appears in logs or persistence. |
| Malformed/truncated PDF | Explicit retryable/non-sensitive failure; no crash, source write, or partial false success. |
| Revoked SAF grant | `AccessRevoked` before any parser call. |

The first implementation must add:

1. local unit tests for the PDFBox-to-domain mapper and every coverage/failure path;
2. an Android integration test using only the synthetic app fixture to verify the
   read-only adapter and resource cleanup;
3. an emulator run with network disabled after dependencies have already been
   resolved, proving runtime extraction is local; and
4. a user-visible test only after an explicit foreground indexing experience and its
   privacy/progress/recovery copy are designed. The current setup UI must not begin
   opening PDFs merely because a folder is connected or metadata discovery completed.

## Pre-work record

- **Source documents read:** product registry, Local AI Technical Specification,
  governance, continuation record, product contract, architecture, decisions,
  roadmap, traceability, and change-control template.
- **Current-code evidence:** `PdfExtraction.kt` defines the pure domain port and
  coverage model; `PdfBoxPdfDocumentMapper` maps only an already-supplied stream into
  that contract; its Android integration tests use only repository-owned synthetic
  streams. SAF code still performs metadata-only discovery and forbids document
  opening. Gradle pins the reviewed parser and Bouncy Castle overrides; `minSdk` is
  26.
- **Open decisions/limitations:** ADR-003 (notes) remains unrelated and open. P-07
  requires an OCR follow-up for scanned PDFs. Android framework text APIs do not span
  the current minimum Android version.
- **Smallest safe change delivered:** a parser mapper tested only with synthetic
  fixtures, plus pinned/dependency-reviewed parser dependencies and explicit failure
  outcomes. No source access, database migration, WorkManager, model, or UI change.
- **Acceptance result:** on 2026-07-21, the Medium Phone emulator passed
  `PdfBoxPdfDocumentMapperIntegrationTest`: 4 of 4 tests. A later accepted
  isolated-process review is still required before the parser can open a real user
  source. No source mutation or network path is permitted.

## Delivery checkpoint: validated isolated-parser result to domain extraction

On 2026-07-23, `ValidatedIsolatedPdfResultToExtractionMapper` was added as a
synthetic-only, in-memory data-to-domain handoff. It accepts only the already
validated isolated-parser transport type; it does not decode a Binder bundle, open a
descriptor, or access a source. For an extracted result, it deterministically sorts
page chunks by page number and chunk index, rejoins them into complete page text, and
creates a `PdfExtractionRecord` bound to the supplied `PdfExtractionRequest`.

No-text, password-protected, and failure outcomes remain explicit. The handoff does
not invent page text, metadata, title, partial coverage, or a source result that was
not present in the validated transport. It does not write that record to Room or make
it searchable. `ValidatedIsolatedPdfResultToExtractionMapperTest` passed **4 of 4**
local unit tests; the related private-parser emulator regression passed **20 of 20**
focused Android tests on the Medium Phone. This is not real-source parsing or
persistence; ADR-017 remains in force.

## Delivery checkpoint: approved parser and extraction outcome assembly

On 2026-07-23, `AssembleApprovedPdfExtraction` was added as an application-level
consistency gate. It invokes an approved parser-status port first; only `Extracted`
or `NoExtractableText` outcomes may invoke the in-memory extraction-result provider.
It then requires the extraction record to match the request's Asset identity,
fingerprint, and schema, and to agree on page count and complete/no-text coverage.
Any disagreement is an explicit `InconsistentExtraction`, never a usable record.

Transport failures, protected PDFs, access outcomes, cancellation, and post-parser
extraction failures remain separate outcomes. The provider is synthetic-only and
receives no descriptor, URI, path, source content, Room handle, UI, worker, AI, or
network capability. It does not establish that the isolated service has delivered
page text to this layer; that future transport/persistence work remains separately
gated. `AssembleApprovedPdfExtractionTest` passed **6 of 6** local unit tests. The
existing `ParseApprovedPdfWithIsolatedParserIntegrationTest` passed **3 of 3** on the
Medium Phone emulator after the port refactor.

## Delivery checkpoint: content-free persistence eligibility

On 2026-07-23, `PrepareApprovedPdfExtractionPersistence` added the pure decision
that must precede any future atomic persistence of a successfully assembled PDF
extraction. It accepts the existing request and assembly outcome, then emits either
metadata-only facts eligible for a future write or an explicit ineligible lifecycle
and retry directive. Eligible facts retain identity, fingerprint, schema version,
positive page count, complete/no-text coverage, verified integrity, and a no-retry
ready state. They carry no PDF text, title, metadata, URI, source location, or parser
result.

Partial coverage, inconsistent outcome, changed fingerprint, access loss, source
failure, cancellation, and parser/extraction failure remain ineligible and have
explicit recovery instructions. No Room entity, migration, record write, source
access, parser transport, UI, worker, AI, or network behavior was added.
`PrepareApprovedPdfExtractionPersistenceTest` passed **8 of 8** local unit tests.
The complete contract is recorded in `PDF_EXTRACTION_PERSISTENCE_CONTRACT.md`.

## Delivery checkpoint: atomic persistence-port contract

On 2026-07-23, the generic persistence facts were promoted into the domain layer and
`PdfExtractionPersistencePort` was added as the only future repository boundary.
It accepts a private-constructor write request made solely from an eligible decision
and the corresponding record. The request factory rechecks identity, fingerprint,
schema, page count, and complete/no-text coverage before the port sees it. Explicit
outcomes distinguish persisted, retryable failure, stale reindexing, and safe failure.

This is not a data adapter: it has no Room implementation, transaction, text write,
source opening, parser transport, UI, worker, AI, or network behavior.
`PdfExtractionPersistencePortContractTest` passed **5 of 5** local unit tests; the
related eligibility regression remained **8 of 8**.
