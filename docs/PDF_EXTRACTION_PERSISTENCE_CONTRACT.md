# PDF Extraction Persistence Contract

**Status:** Pure eligibility and repository-port contracts are implemented; no
persistence implementation exists.
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.

## Purpose

Before Memora can persist a deterministic PDF extraction, it needs an explicit rule
for what is safe to write and what must remain a recoverable, non-searchable state.
`PrepareApprovedPdfExtractionPersistence` is that rule. It accepts a request plus
the already-assembled parser/extraction outcome and returns a content-free decision.
It does not read a document, transport parser text, persist anything, or make an
Asset searchable.

## Future atomic-write facts

Only `EligibleForAtomicWrite` may enter the future persistence transaction. Its
`PdfExtractionPersistenceFacts` deliberately contains only:

| Fact | Why it must be retained |
|---|---|
| `AssetIdentity` | Binds the derived data to the source-owned asset. |
| `AssetFingerprint` | Prevents a record from being treated as valid after the original changes. |
| `ExtractionSchemaVersion` | Makes extraction shape/version compatibility explicit. |
| Positive page count | Preserves the verified structural scope of the record. |
| `COMPLETE` or `NO_EXTRACTABLE_TEXT` coverage | Prevents partial text from looking complete and preserves no-text/OCR follow-up truth. |
| `VERIFIED` integrity | Confirms request binding and allowed coverage passed this gate; it is not an AI confidence score. |
| `NO_RETRY_REQUIRED` and `READY` | States eligibility for the future atomic write, not that a write occurred. |

The object cannot carry page text, title, metadata, URI, document-tree reference,
descriptor, source location, parser output, user content, or a database handle.
When a real persistence implementation is separately approved, it must write the
already validated content-bearing record and these facts atomically under the same
identity/fingerprint/schema key. This contract does not define the Room schema or
authorize that implementation.

## Atomic repository port

`PdfExtractionPersistencePort` is a domain-layer contract for the future data
repository. It accepts only `PdfExtractionPersistenceWriteRequest`, whose constructor
is private. The only factory accepts a typed `EligibleForAtomicWrite` decision plus
the content-bearing `PdfExtractionRecord` and revalidates all of the following before
the port can receive a request:

- Asset identity, immutable fingerprint, and extraction schema match the facts;
- positive page count matches the facts; and
- coverage is exactly complete or explicit no-extractable-text, and agrees with the
  facts.

An ordinary `PdfExtractionPersistenceDecision` or `NotEligible` decision cannot be
given to the port. A future repository must write the record and facts in one
transaction, or return exactly one explicit outcome: `Persisted`,
`RetryableFailure`, `StaleReindexRequired`, or `FailedSafely`. No Room adapter exists
at this point, so none of these outcomes has been produced by real storage.

## Ineligible lifecycle and recovery facts

All other outcomes return `NotEligible` with the request's content-free key plus an
explicit lifecycle/retry instruction. They are not eligible to create or replace a
durable extraction record.

| Condition | Lifecycle | Retry instruction |
|---|---|---|
| Access required or revoked | `AWAITING_PERMISSION` | `USER_ACTION_REQUIRED` |
| Cancellation or retryable parser/extraction failure | `QUEUED` | `RETRY_WHEN_REQUEUED` |
| Source unavailable or source mismatch | `SOURCE_UNAVAILABLE` | `REQUIRES_FRESH_EXTRACTION` |
| Password protected or non-retryable parser/extraction failure | `FAILED_SAFELY` | `USER_ACTION_REQUIRED` or `REQUIRES_FRESH_EXTRACTION` |
| Inconsistent request/record binding or partial coverage | `STALE_REINDEX_REQUIRED` | `REQUIRES_FRESH_EXTRACTION` |

The vocabulary is intentionally aligned with the product integrity contract. It is
not yet a Room table, an `IndexingState` transition, a worker schedule, or UI copy.

## Pre-work record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** product registry, Local AI Technical Specification,
  governance, continuation record, product contract, architecture, decisions,
  roadmap, traceability, PDF extraction plan, parser-isolation review, descriptor
  custody/broker plans, and the change-control template.
- **Current-code evidence inspected:** `PdfExtractionRecord` binds identity,
  fingerprint, schema, page count, and complete/partial/no-text coverage;
  `AssembleApprovedPdfExtraction` already verifies parser status against that
  in-memory record but does not persist it. Existing Room persists Asset discovery,
  not PDF extraction content.
- **Open ADRs / platform limitations checked:** ADR-017 still blocks real-user PDF
  parsing. ADR-019 requires stable identity, explicit integrity, and truth before
  intelligence. No source-opening or content-persistence decision is implied here.
- **Privacy, source-access, dependency, offline, and data-retention impact:** pure
  Kotlin only; no Android API, source, descriptor, URI, parser transport, Room,
  WorkManager, UI, AI, network, dependency, or retained user content.
- **Smallest safe change:** metadata-only eligibility/lifecycle and atomic-port
  contracts with local tests; no schema, repository implementation, migration, or
  write path.
- **Acceptance criteria:** only matching complete/no-text records become eligible;
  partial, inconsistent, failed, access-blocked, and retryable outcomes are
  ineligible with explicit lifecycle/retry facts; the eligible facts expose no
  content or source location.
- **Test and emulator verification plan:** run
  `PrepareApprovedPdfExtractionPersistenceTest` and
  `PdfExtractionPersistencePortContractTest` locally. They should report **8** and
  **5 tests passed**. No emulator test is needed because this change has no Android
  behavior.
- **User-visible quality/accessibility review plan:** no user-visible behavior is
  added. Future UI may summarize these states only after its accessibility and plain-
  language copy are separately designed and tested.

## Delivery record

- **Files/layers changed:** a pure application eligibility policy, pure domain
  persistence-port contract, thirteen local unit tests, and this documentation. No
  platform adapter, parser protocol, database, UI, worker, model, or dependency
  changed.
- **Automated verification and result:** on 2026-07-23,
  `PrepareApprovedPdfExtractionPersistenceTest` passed **8 of 8** and
  `PdfExtractionPersistencePortContractTest` passed **5 of 5** local unit tests.
- **Emulator/manual verification and result:** not applicable: no Android framework
  type, emulator source, or UI is involved.
- **Failure/recovery paths verified:** partial coverage, inconsistent assembly,
  mismatched fingerprint, access revocation, retryable parser failure, and
  non-retryable extraction failure are all ineligible; only complete and no-text
  records are eligible.
- **Known limitation or follow-up:** there is no source-content transport, actual
  repository implementation or transaction, Room migration, retry scheduler, search, UI,
  semantic understanding/AI, or real-source PDF capability. ADR-017 remains binding.
- **Documentation/traceability/ADR updates:** continuation record, change log, PDF
  plan/review, and P-07 traceability are updated. No new ADR is needed because this
  contract implements the existing ADR-017/ADR-019 boundaries.
- **Git commit:** recorded with this verified delivery step.
