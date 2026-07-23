# PDF Extraction Data-Persistence Design

**Status:** Privacy posture accepted; persistence implementation remains blocked.
This is not authorization to write PDF extraction content.

**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.

## Decision boundary

Memora may eventually retain deterministic, local PDF extraction facts so a later
Asset Memory can be understood, retrieved, and explained without reopening the
original PDF. Page text and metadata can contain more private information than the
source placeholder currently held by Room.

The existing `PdfExtractionPersistencePort` proves only that a complete or explicit
no-text record with matching identity, fingerprint, schema, page count, and integrity
can reach a future data adapter. It does **not** select a database design, backup
policy, encryption model, retention rule, storage budget, or migration plan.

Until this record is accepted, there must be no Room entity, DAO, migration, binding,
write implementation, source-content transport, UI, worker, search, semantic
understanding, or real-source parsing change.

## Fixed privacy and correctness invariants

1. Android remains owner of the original PDF. Memora never stores a second original,
   descriptor, file path, tree URI, opening URI, or copy of the document.
2. A stored extraction is immutable evidence for exactly one `(Asset identity,
   fingerprint, extraction schema version)` tuple. A changed fingerprint or schema
   requires a distinct revision/re-extraction path; it cannot overwrite the record.
3. Only the typed `PdfExtractionPersistenceWriteRequest` can enter the future
   adapter. It must retain verified facts and a matching record atomically, or retain
   neither.
4. `COMPLETE` and `NO_EXTRACTABLE_TEXT` are the only eligible coverages. `Partial`
   text is never durable or searchable as though it were complete.
5. Failed, cancelled, stale, revoked, and resource-limited work has an explicit
   integrity state and creates no partial searchable data.
6. The database contains no raw source location, Binder data, parser descriptor,
   credential, cross-source relationship, embedding, generated summary, AI
   observation, or Memory record in this phase.
7. Retained data stays local. Page text and document metadata never reach a network
   API, backup, sync, telemetry payload, or log.

## Proposed normalized storage model

This is a conceptual schema only. Names are deliberately not Room entities yet.

| Logical record | Key / relation | Permitted fields | Purpose |
|---|---|---|---|
| `pdf_extraction` | Composite immutable key: source ID, source asset key, Asset fingerprint, extraction schema version | page count, coverage, `VERIFIED` integrity, extraction time, creation time | Durable extraction header and provenance boundary. |
| `pdf_extraction_page` | Parent extraction key plus one-based page number; unique per parent/page | page text only for `COMPLETE` coverage | Page-level evidence and citation scope. Blank text is a valid represented page. |
| `pdf_extraction_metadata` | Parent extraction key plus normalized metadata name | bounded deterministic source-derived value | Only metadata named by `PdfExtractionRecord`; no URI/path or parser diagnostics. |

For `NO_EXTRACTABLE_TEXT`, the header is retained with truthful coverage and page
count, but has no page-text rows. Future Memory/evidence tables are out of scope.
There is no durable row for an attempted-but-not-persisted failure: the Asset/indexing
lifecycle remains the recovery authority until that lifecycle is separately integrated.

### Atomic write rule

The future Room transaction must insert the header, every numbered page when complete,
and allowed metadata in one transaction. It must reject duplicate page numbers,
missing/extra pages, mismatched keys, invalid metadata names, and page text for a
no-text record. If commit fails, no header, page, metadata, or searchable marker may
remain.

The adapter must be idempotent for the exact immutable key. A repeated verified
record may report success without duplicate rows. A conflict for the same identity
with another fingerprint/schema becomes `STALE_REINDEX_REQUIRED`, never an overwrite.

## Retention, deletion, and source revocation

- **Clear derived data:** Delete the header and every dependent page/metadata row in
  one local transaction, scoped to one source or all Memora data. This is idempotent
  and never alters the original PDF.
- **Remove a source:** Remove its derived extraction records in the same local
  deletion operation as its Memora-owned source reference/checkpoint. Do not touch
  the folder or documents.
- **Revoked access:** Stop all further reading. Existing derived extraction may remain
  only while the user retains it, becomes `SOURCE_UNAVAILABLE` through a future
  lifecycle integration, and must make future explanations say the original is
  unavailable. Offer `Clear indexed data for this source`; do not imply revocation
  automatically destroys locally retained derivations.
- **Changed source:** A changed fingerprint invalidates prior extraction for current
  recall. It cannot silently be used as current evidence.

The retention duration for superseded records is unresolved. Before writing content,
an ADR must choose either non-current provenance with user-controlled clearing or an
explicit deletion policy.

## Backup and encryption review

At this review's start, the manifest had `android:allowBackup="true"` and both
referenced XML rule files were Android Studio template defaults. New PDF page text
would then have inherited an unreviewed backup/device-transfer policy. This conflicts
with the product contract: backup and sync are optional future enhancements needing
consent and their own data-handling decision.

**Accepted MVP policy:** Memora private databases, preferences, files, external app
data, and app-root data are excluded from cloud backup and device-to-device transfer.
`android:allowBackup="false"` is set as defence in depth, but Android documents that
some manufacturers can still perform device-to-device transfer; explicit exclusions
in both `data-extraction-rules` and legacy `full-backup-content` rules are therefore
also required. There is no approved backup/sync feature, and restoring source
approvals or derived text to another device cannot safely restore Android source
grants. This policy is now implemented and verified on the emulator.

Android app-private storage benefits from device protection, but that is not an
app-level database-encryption design. Before persistent source-derived text is
enabled, Memora must explicitly accept one of these models:

1. **Accepted enterprise direction, design still required:** an encrypted database
   design with a
   locally generated secret protected by Android Keystore, including invalidation,
   rotation, recovery, lock-screen behavior, dependency licensing, migration, and
   testing; or
2. **Lower-complexity baseline:** device-protected internal storage only, with an
   explicit threat-model acceptance of what it does and does not protect.

No model is silently assumed. The accepted encryption direction still needs a detailed
ADR and supply-chain/security review before it adds a dependency or code.

## Migration, rollback, and bounded writes

The first content migration must be additive and forward-only. It must export its
Room schema; preserve every Asset, checkpoint, and approved-source record; and never
fall back to destructive migration or a database reset. Test both current-schema
upgrade and an empty install. If migration or validation fails, preserve the existing
database, avoid partial extraction data, expose a recoverable safe state, and provide
a user-controlled clear/retry path. App downgrade is not assumed safe; use staged
rollout with content-free diagnostics.

No production page-count, character, database-size, or transaction-time limit exists.
The synthetic parser limits are expressly not production limits. Before a write
implementation, a private synthetic corpus must measure no-text PDFs, blank pages,
dense selectable text, long metadata, large page counts, cancellation, low storage,
failed transactions, database growth, duration, heap pressure, battery, and thermal
impact on the representative device tier. The review then sets hard limits and a
deterministic overflow outcome. An overflow retains no partial ready record and uses
`FAILED_SAFELY` or `STALE_REINDEX_REQUIRED` with truthful recovery.

## Evidence required to enable the implementation

Before a production Room adapter is authorized, all of the following are required:

1. The superseded-record retention policy becomes an ADR; the accepted backup and
   encryption direction remains subject to the detailed reviews below.
2. An ADR and security review for the encrypted-database design, including dependency
   provenance, KeyStore lifecycle, recovery, and rollback.
3. Concrete normalized Room schema, exported schema JSON, additive migration,
   foreign-key/cascade design, and repository boundary review.
4. Tests for atomic success, duplicate idempotency, validation/transaction failure,
   migration preservation, delete-by-source, clear-all, revocation, and no partial
   searchable state.
5. Measured resource limits plus cancellation, low-storage, process-death, and
   recovery tests.
6. Privacy proof that text/metadata reaches no logs, backup, transfer, analytics,
   network, source identifiers, or unapproved parser-protocol boundary.
7. Emulator verification using repository-owned synthetic fixtures only. ADR-017's
   fresh-grant, descriptor-only, isolated-process, bounded-streaming, offline, and
   visible-recovery gates remain mandatory and unsatisfied by this design.

## Pre-work and delivery record

- **Inspected:** product registry, Local AI Technical Specification, governance,
  continuation record, product contract, architecture, ADR-017/ADR-019, persistence
  contract, implementation plan, traceability, manifest, and backup XML rules.
- **Affected layers:** documentation and future data boundary only; no executable
  Android, domain, application, data, or UI behavior changes.
- **Verification:** on 2026-07-24, `:app:assembleDebug :app:installDebug` succeeded
  on the Medium Phone emulator. The installed package does not have Android's
  allow-backup flag, and the packaged Android 12+ and legacy XML resources each
  contain exclusions for database, shared preference, file, external, and root data.
- **Known limitation:** PDF text remains in-memory only in synthetic contracts; it is
  neither persisted, searchable, visible to users, nor available for real PDFs.
