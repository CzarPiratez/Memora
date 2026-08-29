# Change control: MIG-05 step 1 — Evidence-level embedding store foundation

**Date:** 2026-08-29  
**Type:** Domain + Room persistence (additive foundation only)  
**Decision guardrails:** Implement MIG-05 **step 1 only**. Do not complete full
MIG-05. Do not cut over search/indexing. Do not remove `PdfPageEmbedding*`.
Do not start MIG-06–MIG-11 or MIG-07B. Do not implement Grounded Answers,
Links, Event/Knowledge, ranking changes, AVAILABLE claim, Act/agents,
VisionEngine, or package/applicationId/db rename (ADR-040). Do not rewrite
hashed Product Contract, Freeze, Migration Spec body, Local AI Spec,
Grounding, Experience Memory Amendment, or ADR-043 substance. No Class A /
public unfynd-core sync. Leave unrelated dirty files (`docs/ROADMAP.md`,
`libs.versions.toml`) unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = additive foundation only:** evidence-level embedding domain + Room
   table/store + migration + tests + change-control.
2. **Dual-store interim (mandatory):** after step 1, `PdfPageEmbedding*` remains
   the live path for PDF page meaning search; the new
   `MemoryEvidenceEmbedding*` store exists, is Hilt-bound, and is **UNUSED** by
   Search/Index use cases (`SearchAssetMemoriesByMeaning`,
   `IndexPdfPageEmbeddings`, `IndexMemoryEmbeddings`).
3. **STALE_REINDEX decision for step 1 (LOCKED):** DO NOT mark memories
   `STALE_REINDEX_REQUIRED`. Reason: old PDF page embeddings remain valid and
   still serve search; marking stale while the old store is authoritative would
   be a false signal. Stale/reindex into the new store belongs to a later
   MIG-05 step at cutover.
4. **Migration strategy (LOCKED):** CREATE new evidence-level embedding table.
   Do NOT bespoke SQL-map old `(revisionId, pageNumber, model)` vectors onto
   evidenceIds. Do NOT drop/clear `pdf_page_embeddings` in step 1. Embeddings
   remain derived artifacts per Local AI Spec §8; rebuild happens later via
   indexing pipeline.
5. **No product invention** beyond the locked foundation list above.

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-05 (canonical; step 1 = store
  foundation only); Local AI Spec §8 (derived embeddings / recoverable
  reindex; compatible embedding version and index state); Experience Memory
  Amendment evidence substrate (citation level — one evidence substrate);
  Architecture Freeze change-control expectations. Supporting: A-03
  (replaceable capability interfaces). MIG-02 dependency noted (evidence
  completeness prerequisite for later cutover meaning; not blocking additive
  empty store).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE` (pre-work gate + enterprise bar), `CONTINUE`,
  `PRODUCT_CONTRACT` (privacy / originals read-only),
  `LOCAL_AI_TECHNICAL_SPEC` §8, `EXPERIENCE_MEMORY_AMENDMENT_V1` (evidence
  substrate citation), `ARCHITECTURE_FREEZE_v1.0` (change-control),
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-05 + MIG-02 dependency note,
  `CHANGE_CONTROL_TEMPLATE`, `CHANGE_CONTROL_MIG04_MEMORY_BUILDER_CONTRACT`,
  `CHANGE_CONTROL_MIG01_EVIDENCE_CLASS`, ADR-040 / ADR-043.
- **Current-code evidence inspected:**
  - `PdfPageEmbeddingEntity` / `PdfPageEmbeddingDao` /
    `RoomPdfPageEmbeddingStore` / `PdfPageEmbeddingStore` (live PDF path;
    PK = revision + pageNumber + model)
  - `MemoryEmbeddingEntity` / `MemoryEmbeddingStore` (summary-level; do not
    conflate with evidence-level)
  - `MemoraDatabase` entities list, version **13**;
    `MemoraDatabaseMigrations` through `MIGRATION_12_13`
  - `SearchAssetMemoriesByMeaning`, `IndexPdfPageEmbeddings`,
    `IndexMemoryEmbeddings` (left unwired to new store)
  - `MemoraDatabaseMigrationTest` patterns (v1 chain + v12 evidence backfill)
  - `ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION` = **5** — this is the
    **conversion-journal** schema version used by
    `MemoraEncryptedDatabaseOpener` / conversion harness, **not** the Room
    `@Database` version. Step 1 does **not** require changing it (journal ≠
    Room; Room bumps 13→14 independently).
- **Open ADRs / platform limitations checked:** ADR-040 identity deferred;
  ADR-043 Act remains out. User authorizes MIG-05 step 1 over leftover
  “do not start MIG-05” governance lines (updated in this delivery to
  authorize step 1 only / block premature MIG-06/07 cutover claims). Room
  additive CREATE TABLE is SQLite-compatible.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new network, permissions, or cloud AI. Originals remain read-only. New
  table holds **derived vectors only** (Local AI Spec §8). No originals
  copied into the embedding store.
- **Smallest safe change:** Domain record + store interface; Room entity/DAO/
  store; Hilt bind; Room 13→14 CREATE TABLE; unit + migration tests; docs.
  Search/Index behavior unchanged.
- **Acceptance criteria:** See delivery instruction A–H (table exists after
  migration; PdfPageEmbedding path unchanged; unit put/get/list PASS;
  migration instrumentation PASS or blocker recorded; locked decisions in
  change-control; CONTINUE/CHANGELOG honest; local commit only MIG-05 step 1
  files; unverified areas reported plainly).
- **Test and emulator verification plan:** Focused debug unit tests for store
  contract + Room adapter; `MemoraDatabaseMigrationTest` including 13→14
  dual-store case; `:app:assembleDebug`; emulator smoke optional.
- **User-visible quality/accessibility review plan:** N/A (no UI; meaning
  search behavior unchanged).

## Delivery record

- **Files/layers changed:**
  - Domain: `MemoryEvidenceEmbeddingRecord`, `MemoryEvidenceEmbeddingStore` in
    `EmbeddingContracts.kt` (no Android imports)
  - Data: `MemoryEvidenceEmbeddingEntity`, `MemoryEvidenceEmbeddingDao`,
    `RoomMemoryEvidenceEmbeddingStore`; `MemoraDatabase` version 14 + entity/
    DAO; `MIGRATION_13_14` CREATE only; opener migration lists;
    androidTest migration lists
  - DI: `PersistenceModule.provideMemoryEvidenceEmbeddingStore`
  - Tests: `MemoryEvidenceEmbeddingStoreTest`,
    `RoomMemoryEvidenceEmbeddingStoreTest`;
    `MemoraDatabaseMigrationTest` (1→14 chain, 12→14 evidence backfill,
    focused 13→14 dual-store)
  - Schema export: Room kapt `14.json` (generated on compile)
  - Docs: GOVERNANCE + registry authorize MIG-05 step 1 / block MIG-06+;
    CONTINUE checkpoint; CHANGELOG Unreleased; this record
  - Unchanged (intentional): Search/Index use cases, `PdfPageEmbedding*`,
    hashed blobs, ROADMAP, `libs.versions.toml`,
    `ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION`
- **Automated verification and result:**
  - Unit (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStoreTest
      --tests com.memora.app.data.local.RoomMemoryEvidenceEmbeddingStoreTest
      --tests com.memora.app.domain.intelligence.EmbeddingContractsTest
    ```
    **10/10 passed** (5 + 2 + 3), 0 failures, BUILD SUCCESSFUL.
  - Compile: `:app:assembleDebug` **BUILD SUCCESSFUL**; Room schema export
    `app/schemas/.../14.json` present with `memory_evidence_embeddings`.
  - `EXPECTED_SCHEMA_VERSION` unchanged at **5** (conversion journal).
- **Emulator/manual verification and result:**
  - `MemoraDatabaseMigrationTest` **device-verified:** **3/3 PASSED** on
    Medium Phone emulator via Android Studio Run of class
    (`com.memora.app.data.local.MemoraDatabaseMigrationTest`). Room **13→14**
    additive CREATE (`memory_evidence_embeddings`) and dual-store assertions
    confirmed on device. Prior CLI residual (emulator stuck `offline` /
    `No connected devices!`) is closed by this Studio run.
  - Emulator smoke (app opens): not required for this residual close
    (migration class gate only).
- **Failure/recovery paths verified:** Empty-store honesty (null find, count
  0, empty list). Additive migration leaves `pdf_page_embeddings` intact
  (asserted in instrumentation; **3/3** device PASS).
- **Known limitation or follow-up (NOT done — remaining MIG-05):**
  - Rewriting `SearchAssetMemoriesByMeaning` / removing
    `SavedPdfPageTextSource` query-time dependency
  - Consolidating `Index*` into one evidence indexer / writing into the new
    store from production index paths
  - Removing `PdfPageEmbeddingEntity`/`Store`
  - `ResolveMeaningPdfOpenPage` changes
  - End-to-end non-PDF meaning indexing
  - `STALE_REINDEX` marking / mass reindex at cutover
  - MIG-06, MIG-07, MIG-07B, MIG-11
  - MIG-05 step 2+ (search/index cutover) — **not started**
- **Residual risks:** Dual-store divergence until cutover (new store empty;
  live path still PDF-page keyed). Operators must not treat empty evidence
  embedding counts as “meaning index empty” while PdfPageEmbedding remains
  authoritative. Room 13→14 migration device residual is closed.
- **Documentation/traceability/ADR updates:** GOVERNANCE,
  PRODUCT_SOURCE_REGISTRY, CONTINUE, CHANGELOG Unreleased, this record.
  Hashed specs unchanged. ADR-043 / ADR-040 substance unchanged.
- **Git commit:** Local docs checkpoint after device verification (no push).

## Explicit “not done” — full MIG-05 acceptance

This step does **not** satisfy Migration Spec MIG-05 acceptance criteria that
require Search to stop using `SavedPdfPageTextSource`, evidence-level
indexing for non-PDF kinds, or retirement of `PdfPageEmbedding*`. Those remain
later MIG-05 steps.
