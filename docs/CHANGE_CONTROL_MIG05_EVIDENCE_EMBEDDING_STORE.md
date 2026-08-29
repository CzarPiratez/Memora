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

---

# Change control: MIG-05 step 2 — PDF page embedding dual-write

**Date:** 2026-08-29  
**Type:** Application indexing dual-write (no Room schema change)  
**Decision guardrails:** Implement MIG-05 **step 2 only** (INDEX dual-write
foundation). Do not cut over search. Do not remove `PdfPageEmbedding*` /
`SavedPdfPageTextSource` from query paths. Do not consolidate a full
`IndexMemoryEvidenceEmbeddings` for OCR/notes. Do not start MIG-06–MIG-11 or
MIG-07B. Do not implement Grounded Answers, Links, Event/Knowledge, ranking
changes, AVAILABLE claim, Act/agents, VisionEngine, or
package/applicationId/db rename (ADR-040). Do not rewrite hashed Product
Contract, Freeze, Migration Spec body, Local AI Spec, Grounding, Experience
Memory Amendment, or ADR-043 substance. No Class A / public unfynd-core sync.
Leave unrelated dirty files (`docs/ROADMAP.md`, `libs.versions.toml`)
unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = INDEX dual-write only:** when `IndexPdfPageEmbeddings`
   successfully upserts a `PdfPageEmbeddingRecord`, also upsert the same
   vector into `MemoryEvidenceEmbeddingStore` keyed by
   `(revisionId, evidenceId, model)`.
2. **Search stays on PdfPageEmbedding\*:** `SearchAssetMemoriesByMeaning`,
   `LoadMeaningSearchReadiness`, and `ResolveMeaningPdfOpenPage` remain on
   the live PDF page path. `SavedPdfPageTextSource` is not removed from search.
3. **evidenceId resolution:** real `MemoryEvidence.id` for that page,
   matched by locator `pdf:page:N` via `PdfPageEvidenceLocator` /
   `MemoryRepository.findPdfPageEvidenceIds`. **NEVER** use `"pdf:page:N"`
   as `MemoryEvidenceId` (DeterministicMemoryBuilder assigns `e{n}`).
4. **Unresolved evidenceId:** still write `PdfPageEmbedding` as today; do
   not invent an evidence id; count as `unresolvedEvidence` in result
   metrics.
5. **STALE_REINDEX (LOCKED for step 2):** NO mass-mark of memories. Dual-write
   on existing index invocations (including fingerprint-skip **backfill** of
   the evidence store from the existing page vector) fills the new store on
   the next user-driven meaning-index run without breaking search.
6. **No full evidence indexer yet:** PDF dual-write only inside
   `IndexPdfPageEmbeddings`.
7. **No ranking / AVAILABLE / Act / MIG-06+ / package rename / push.**

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-05 (canonical; step 2 = dual-write
  foundation only); Local AI Spec §8 (derived embeddings / recoverable
  reindex; compatible embedding version and index state); Experience Memory
  Amendment evidence substrate; Architecture Freeze change-control.
  Supporting: A-03. Step 1 store foundation is prerequisite.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LOCAL_AI_TECHNICAL_SPEC` §8,
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-05, this file step 1,
  `CHANGE_CONTROL_MIG04_MEMORY_BUILDER_CONTRACT` format, ADR-040 / ADR-043.
- **Current-code evidence inspected:**
  - `IndexPdfPageEmbeddings` + `AiPackDisclosureViewModel` candidate build
  - `SearchAssetMemoriesByMeaning` (left reading `PdfPageEmbeddingStore` +
    `SavedPdfPageTextSource`)
  - `MemoryEvidenceEmbeddingStore` / Room store (step 1)
  - `PdfPageEvidenceLocator`, `DeterministicMemoryBuilder` evidence id
    (`e{n}`) vs locator (`pdf:page:N`)
  - `MemoryRepository` / `MemoryDao.findEvidence` patterns
  - `IndexMemoryEmbeddingsTest` / `SearchAssetMemoriesByMeaningTest` patterns
- **Open ADRs / platform limitations checked:** ADR-040 identity deferred;
  ADR-043 Act remains out. User authorizes MIG-05 step 2 over leftover
  “step 1 only” governance lines (updated to authorize step 2 / block search
  cutover). No Room bump required.
- **Privacy / retention:** No new network, permissions, or cloud AI.
  Originals remain read-only. Dual-write stores derived vectors only
  (Local AI Spec §8).
- **Smallest safe change:** Thread optional `evidenceId` on
  `PdfPageEmbeddingCandidate`; resolve via repository batch locator→id map;
  dual-write + fingerprint-skip backfill inside `IndexPdfPageEmbeddings`;
  focused unit tests; docs. Search untouched.
- **Acceptance criteria:** A–H from delivery instruction (both stores on
  success; search unchanged; evidenceId ≠ locator; focused tests +
  assembleDebug PASS; change-control; local commit; unverified areas plain).
- **Test plan:** `IndexPdfPageEmbeddingsTest`, `PdfPageEvidenceLocatorTest`,
  `SearchAssetMemoriesByMeaningTest`; `:app:assembleDebug`. Emulator optional
  (no Room bump).

## Delivery record

- **Files/layers changed:**
  - Domain: `PdfPageEvidenceLocator.formatLocator` /
    `evidenceIdForPage`; `MemoryRepository.findPdfPageEvidenceIds`
  - Data: `MemoryDao.findEvidenceLocators` + row type;
    `RoomMemoryRepository.findPdfPageEvidenceIds`
  - Application: `IndexPdfPageEmbeddings` dual-write + metrics
    (`unresolvedEvidence`, `evidenceDualWrites`);
    `PdfPageEmbeddingCandidate.evidenceId`
  - UI (caller only): `AiPackDisclosureViewModel` resolves evidence ids when
    building page candidates
  - Contracts comment: evidence store written by dual-write; Search still on
    page store
  - Tests: `IndexPdfPageEmbeddingsTest` (new); `PdfPageEvidenceLocatorTest`
    extended; Fake `MemoryRepository` methods for compile
  - Docs: GOVERNANCE + registry authorize step 2 / block search cutover;
    CONTINUE checkpoint; CHANGELOG Unreleased; this step 2 section
  - Unchanged (intentional): `SearchAssetMemoriesByMeaning`, Room version
    **14**, `PdfPageEmbedding*` as live search path, hashed blobs, ROADMAP,
    `libs.versions.toml`, no STALE mass-mark
- **Automated verification and result:**
  - Unit (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.application.intelligence.IndexPdfPageEmbeddingsTest
      --tests com.memora.app.domain.memory.PdfPageEvidenceLocatorTest
      --tests com.memora.app.application.intelligence.SearchAssetMemoriesByMeaningTest
      --tests com.memora.app.application.memory.AssembleAssetMemoryFromExtractionFactsTest
    ```
    **28/28 passed** (6 + 5 + 5 + 12), 0 failures, BUILD SUCCESSFUL.
  - Compile: `:app:assembleDebug` **BUILD SUCCESSFUL**.
  - Room `@Database` version remains **14** (no migration in step 2).
  - `EXPECTED_SCHEMA_VERSION` unchanged at **5** (conversion journal).
- **Emulator/manual verification and result:** Not required for this step
  (no Room schema change). Step 1 Room 13→14 device residual remains closed.
- **Failure/recovery paths verified:** Unresolved / locator-shaped evidenceId
  → page store still written; evidence store untouched; metrics honest.
  Fingerprint skip → no re-embed; evidence backfill when missing.
- **Known limitation or follow-up (residuals for step 3 — search cutover):**
  - Rewire `SearchAssetMemoriesByMeaning` to read
    `MemoryEvidenceEmbeddingStore` (+ `MemoryEvidence`) instead of
    `PdfPageEmbeddingStore` / `SavedPdfPageTextSource` at query time
  - `LoadMeaningSearchReadiness` / `ResolveMeaningPdfOpenPage` alignment
  - Decide cutover STALE / retirement of `PdfPageEmbedding*` table
  - Non-PDF evidence embedding indexing end-to-end
  - Full MIG-05 acceptance criteria
  - MIG-06 / MIG-07 / MIG-07B / MIG-11
- **Residual risks:** Dual-store divergence until cutover for memories not
  yet re-indexed (page store authoritative for Search; evidence store fills
  on next meaning-index pass). Operators must not treat empty evidence
  embedding counts as “meaning index empty” while PdfPageEmbedding remains
  authoritative.
- **Documentation/traceability/ADR updates:** GOVERNANCE,
  PRODUCT_SOURCE_REGISTRY, CONTINUE, CHANGELOG Unreleased, this record.
  Hashed specs unchanged. ADR-043 / ADR-040 substance unchanged.
- **Git commit:** Local only after verification (no push).

## Explicit “not done” — full MIG-05 acceptance

Step 2 does **not** satisfy Migration Spec MIG-05 acceptance criteria that
require Search to stop using `SavedPdfPageTextSource`, evidence-level
indexing for non-PDF kinds, or retirement of `PdfPageEmbedding*`. Those remain
step 3+ / later MIG-05 work.

---

# Change control: MIG-05 pre-step-3 — e2e dual-write device proof

**Date:** 2026-08-29  
**Type:** Device/Room instrumentation gate (no production Search cutover)  
**Decision guardrails:** Prove on emulator that step-2 dual-write persists into
both Room stores with a real `MemoryEvidence.id` (`e{n}`). Do **not** start
MIG-05 step 3 (Search cutover). Do not retire `PdfPageEmbedding*`. Do not mass
`STALE_REINDEX`. Do not expand non-PDF indexers. No Room version bump. No push.
No public pack sync. Leave unrelated dirty files (`docs/ROADMAP.md`,
`libs.versions.toml`) unstaged.

## Lead decisions (LOCKED)

1. Preferred verification = focused `androidTest` against real Room + real
   `IndexPdfPageEmbeddings` + Available `EmbeddingEngine` test double.
2. `SearchAssetMemoriesByMeaning` remains on `PdfPageEmbeddingStore` +
   `SavedPdfPageTextSource` (asserted; no production search rewire).
3. `evidenceId` must be real `MemoryEvidence.id` (`e{n}`); locator-shaped ids
   rejected by production dual-write path (covered by unit + unresolved case).
4. No mass STALE; no PdfPage retirement; no non-PDF indexer expansion.
5. Record results here + CONTINUE one line; local commit after green device run.
6. Residual = MIG-05 step 3 (search cutover) still open.

## Purpose

Enterprise-grade e2e proof that the dual-write path from step 2 actually lands
rows in `memory_evidence_embeddings` on device Room (schema **v14**) keyed by
real evidence id, while `pdf_page_embeddings` continues to receive the same
vector — before any Search cutover.

## Method

- New instrumentation:
  `IndexPdfPageEmbeddingsDualWriteInstrumentedTest`
  (`androidTest/.../application/intelligence/`).
- Harness: `Room.inMemoryDatabaseBuilder` at current schema **14**
  (same in-memory pattern as other Room androidTests; no migration bump).
- Seeds minimal `memories` + `memory_evidence` with locator `pdf:page:N` and id
  `e{n}`.
- Runs `IndexPdfPageEmbeddings` with a fixed Available `EmbeddingEngine` fake.
- Asserts:
  - PdfPageEmbedding row present **and** MemoryEvidenceEmbedding row present
    for same revision/model with `evidenceId == e3` (not locator).
  - Unresolved (`evidenceId == null`) writes page store only; evidence store
    count stays 0.
  - `SearchAssetMemoriesByMeaning` constructor still takes
    `PdfPageEmbeddingStore` and does **not** take
    `MemoryEvidenceEmbeddingStore`.

## Results

- Device: **Medium_Phone(AVD) - API 17** (`emulator-5554`,
  `sdk_gphone16k_x86_64`).
- `JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`.
- Known-good command path (PASS):
  ```
  .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
  adb install -r app\build\outputs\apk\debug\app-debug.apk
  adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
  adb shell am instrument -w -r ^
    -e class com.memora.app.application.intelligence.IndexPdfPageEmbeddingsDualWriteInstrumentedTest ^
    com.memora.app.test/androidx.test.runner.AndroidJUnitRunner
  ```
  **OK (3 tests)** — PASS count **3/3**, 0 failures. Alternate: Android Studio
  Run of that class on Medium Phone.
- Note: filtered
  `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...`
  currently reports “Starting 0 tests” / process crash on this AGP setup for
  the same class; direct `am instrument` (and Studio Run) are the authoritative
  gate for this residual.

## Search not cut over

Production `SearchAssetMemoriesByMeaning` still depends on
`PdfPageEmbeddingStore` + `SavedPdfPageTextSource` only (confirmed by source
inspection + instrumentation constructor assert). No Search file changes in
this gate. Step 3 remains **not started**.

## Residual (still open)

- MIG-05 **step 3**: rewire Search to `MemoryEvidenceEmbeddingStore` /
  `MemoryEvidence`; readiness / open-page alignment; PdfPage retirement /
  STALE decision; non-PDF evidence embedding end-to-end; full MIG-05
  acceptance; MIG-06+.

## Files / commit scope

- Test:
  `MemoraApp/app/src/androidTest/.../IndexPdfPageEmbeddingsDualWriteInstrumentedTest.kt`
- Docs: this section; CONTINUE one-line checkpoint; CHANGELOG Unreleased note
- Unchanged (intentional): Search production path, Room **14**, PdfPage live
  path, ROADMAP, `libs.versions.toml`, hashed specs

---

# Change control: MIG-05 step 3 — meaning search cutover to evidence embeddings

**Date:** 2026-08-29  
**Type:** Application search/readiness cutover + integrity cutover helper  
**Decision guardrails:** Implement MIG-05 **step 3 only** (SEARCH cutover).
Do not drop/retire `PdfPageEmbedding*` (step 4). Do not build a new non-PDF
OCR evidence indexer beyond existing dual-write. Do not start MIG-06–MIG-11 or
MIG-07B. Do not implement Grounded Answers, Links, Event/Knowledge, ranking
algorithm changes, AVAILABLE claim, Act/agents, VisionEngine, or
package/applicationId/db rename (ADR-040). Do not rewrite hashed Product
Contract, Freeze, Migration Spec body, Local AI Spec, Grounding, Experience
Memory Amendment, or ADR-043 substance. No Class A / public unfynd-core sync.
Leave unrelated dirty files (`docs/ROADMAP.md`, `libs.versions.toml`)
unstaged. No push. Do not claim full MIG-05 complete.

## Lead decisions (LOCKED)

1. Step 3 = cut Find-by-meaning **page/evidence-level** retrieval to
   `MemoryEvidenceEmbeddingStore` + `MemoryEvidence`. Summary-level
   `MemoryEmbeddingStore` stays.
2. `SearchAssetMemoriesByMeaning` MUST NOT import or call
   `SavedPdfPageTextSource` (Migration Spec MIG-05 acceptance).
3. `SearchAssetMemoriesByMeaning` MUST NOT read `PdfPageEmbeddingStore` for
   ranking. `IndexPdfPageEmbeddings` may CONTINUE dual-writing
   `PdfPageEmbedding*` + evidence store.
4. Page/evidence excerpt for scoring + hit text = `MemoryEvidence.excerpt`
   (and locator→page via `PdfPageEvidenceLocator`). `rankedPdfPageNumber`
   from locator page number when applicable.
5. `LoadMeaningSearchReadiness` `indexedCount` = `MemoryEmbeddingStore` count
   + `MemoryEvidenceEmbeddingStore` count for model (**NOT**
   `PdfPageEmbeddingStore` — avoid double-count under dual-write).
6. **CUTOVER STALE (LOCKED):** On readiness/search path (and after meaning-index
   taps) — mark `MemoryIntegrityState.STALE_REINDEX_REQUIRED` for current
   READY revisions that have `PdfPageEmbedding` rows for the active model but
   **ZERO** `MemoryEvidenceEmbedding` rows for that revisionId+model. Do **NOT**
   mass-STALE everything. Restore READY when evidence embeddings appear.
   Meaning-index drain includes STALE so the next Build Index tap dual-write-
   fills the evidence store. Purpose: search is not silently empty while the
   old page table still has vectors.
7. Do **NOT** drop/retire `PdfPageEmbedding*` in step 3 (that is step 4).
8. Do **NOT** implement full non-PDF evidence indexing beyond dual-write; if
   evidence embeddings exist for other kinds, search MAY rank them using
   `MemoryEvidence`.
9. `ResolveMeaningPdfOpenPage` / open-original: preserve correct page open using
   ranked/cited page from hits + existing helpers.
10. No AVAILABLE claim, no Act/GA/Links, no MIG-06/07, no package rename, no
    public unfynd-core sync, no push.

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-05 acceptance (Search stops using
  `SavedPdfPageTextSource`; evidence-level retrieval; stale-reindex recoverable
  path per Local AI Spec §8); Experience Memory evidence substrate; Architecture
  Freeze change-control. Supporting: A-03. Steps 1–2 + pre-step-3 e2e are
  prerequisites.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, this file (steps 1–2 + pre-step-3),
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-05, `LOCAL_AI_TECHNICAL_SPEC` §8,
  ADR-040 / ADR-043.
- **Current-code evidence inspected:**
  - `SearchAssetMemoriesByMeaning` (+Test) — previously
    `PdfPageEmbeddingStore` + `SavedPdfPageTextSource`
  - `LoadMeaningSearchReadiness` — previously counted page store
  - `IndexPdfPageEmbeddings` dual-write (unchanged writer)
  - `MemoryEvidenceEmbeddingStore` / Room store
  - `MemoryRepository` meaning lookups / evidence locators
  - `PdfPageEvidenceLocator`, `OpenMeaningSearchOriginal` /
    `ResolveMeaningPdfOpenPage`
  - DI `PersistenceModule`; no prior Memory integrity STALE writers (new)
- **Open ADRs / platform limitations checked:** ADR-040 identity deferred;
  ADR-043 Act remains out. User authorizes MIG-05 step 3 over leftover
  “do not start search cutover” governance lines (updated in this delivery).
  No Room bump required (schema stays 14).
- **Privacy / retention:** No new network, permissions, or cloud AI.
  Originals remain read-only. Cutover marks derived integrity only.
- **Smallest safe change:** Rewire Search + Readiness; add evidence search-row
  repository API; cutover STALE selection + apply helper; meaning-index drain
  includes STALE; focused unit tests; docs. No PdfPage retirement.
- **Acceptance criteria:** A–I from delivery instruction (zero
  `SavedPdfPageTextSource` on Search; evidence-store ranking; readiness
  counts; STALE selection tested; unit + assembleDebug; device dual-write
  regression preferred; change-control; local commit; stop report).
- **Test plan:** `SearchAssetMemoriesByMeaningTest`,
  `LoadMeaningSearchReadinessTest`, `Mig05EvidenceSearchCutoverSelectionTest`,
  `ApplyMig05EvidenceSearchCutoverTest`, `OpenMeaningSearchOriginalTest`,
  `AssembleAssetMemoryFromExtractionFactsTest`,
  `IndexPdfPageEmbeddingsTest`; `:app:assembleDebug`; prefer
  `IndexPdfPageEmbeddingsDualWriteInstrumentedTest` on device.

## Exact STALE trigger (documented)

**Trigger:** `ApplyMig05EvidenceSearchCutover.ensureApplied(model)` invoked from:
1. `LoadMeaningSearchReadiness` when the embedder is Available
2. `SearchAssetMemoriesByMeaning` after model resolution
3. `AiPackDisclosureViewModel` after a meaning-index tap completes

**Selection rule (pure):**  
`readyRevisionIds ∩ pdfPageEmbeddingRevisionIds − evidenceEmbeddingRevisionIds`  
for the active model only. Unrelated READY revisions are never selected.

**Restore rule (pure):**  
`staleReindexRevisionIds ∩ evidenceEmbeddingRevisionIds` → mark READY.

**Meaning-index drain:** `listMeaningIndexSummaries` /
`countMeaningIndexCandidates` include READY + `STALE_REINDEX_REQUIRED` so the
next Build Index tap can dual-write-fill the evidence store.

## Delivery record

- **Files/layers changed:**
  - Application: `SearchAssetMemoriesByMeaning` (evidence store + evidence
    excerpts); `LoadMeaningSearchReadiness` (counts); 
    `Mig05EvidenceSearchCutoverSelection`; `ApplyMig05EvidenceSearchCutover`;
    `AiPackDisclosureViewModel` (meaning-index drain + post-index cutover)
  - Domain: `MemoryRepository` (+ meaning-index / integrity / evidence search
    rows); `MemoryEvidenceSearchRow`; EmbeddingContracts comment
  - Data: `MemoryDao` queries/update; `RoomMemoryRepository`
  - Tests: search/readiness/cutover unit tests; Assemble fake repo; dual-write
    androidTest constructor assert flipped to evidence store
  - Docs: GOVERNANCE + registry authorize step 3 / block full MIG-05 +
    MIG-06; CONTINUE; CHANGELOG Unreleased; this step 3 section
  - Unchanged (intentional): Room **14**, `PdfPageEmbedding*` dual-write
    writer, hashed blobs, ROADMAP, `libs.versions.toml`, no public pack sync
- **Automated verification and result:**
  - Unit (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests ...SearchAssetMemoriesByMeaningTest
      --tests ...LoadMeaningSearchReadinessTest
      --tests ...Mig05EvidenceSearchCutoverSelectionTest
      --tests ...ApplyMig05EvidenceSearchCutoverTest
      --tests ...OpenMeaningSearchOriginalTest
      --tests ...IndexPdfPageEmbeddingsTest
      --tests ...AssembleAssetMemoryFromExtractionFactsTest
      --tests ...PdfPageEvidenceLocatorTest
    ```
    **40/40 passed** (6+2+5+1+3+6+12+5), 0 failures, BUILD SUCCESSFUL.
  - Compile: `:app:assembleDebug` **BUILD SUCCESSFUL**.
  - Room `@Database` version remains **14** (no migration in step 3).
  - `EXPECTED_SCHEMA_VERSION` unchanged at **5** (conversion journal).
- **Emulator/manual verification and result:**
  - `IndexPdfPageEmbeddingsDualWriteInstrumentedTest` **3/3 PASSED** on
    Medium Phone (`emulator-5554`) via assemble + `adb install` +
    `am instrument` (known-good path). Constructor assert now requires
    evidence store / forbids page store on Search.
- **Failure/recovery paths verified:** NothingIndexed when summary+evidence
  empty even if page store has rows; gap-only STALE; restore after evidence
  fill; open prefers `rankedPdfPageNumber` from locator.
- **Known limitation or follow-up (residuals for step 4):**
  - Retire/drop `PdfPageEmbedding*` entity/table/store/indexer writes
  - Non-PDF evidence embedding production indexer (beyond dual-write)
  - Full Migration Spec MIG-05 acceptance close
  - MIG-06 / MIG-07 / MIG-07B / MIG-11
- **Residual risks:** Upgraded devices with page embeddings but no evidence
  embeddings briefly show STALE until the next meaning-index tap dual-write-
  fills; summary-only hits remain for READY revisions that already have
  summary vectors and evidence fill. Dual-write continues until step 4.
- **Documentation/traceability/ADR updates:** GOVERNANCE,
  PRODUCT_SOURCE_REGISTRY, CONTINUE, CHANGELOG Unreleased, this record.
  Hashed specs unchanged. ADR-043 / ADR-040 substance unchanged.
- **Git commit:** Local only after verification (no push).

## Explicit “not done” — full MIG-05 acceptance

Step 3 does **not** retire `PdfPageEmbedding*`, does **not** add a full
non-PDF evidence indexer, and does **not** close Migration Spec MIG-05 until
step 4 / remaining acceptance criteria are verified.

---

## Already true after step 3 (must not regress)

These are **not** claims that full MIG-05 is closed. They are regression
anchors after the step 3 search cutover; keep them true until step 4 and
beyond:

- Product meaning ranking does **not** read `PdfPageEmbeddingStore`.
- Product meaning ranking does **not** use `SavedPdfPageTextSource`
  (`LEGACY_RECALL_SURFACE` L6 remains Retired).
- Product meaning ranking reads `MemoryEvidenceEmbeddingStore` +
  `MemoryEvidence` (excerpt / locator→page).

Do **not** check the full MIG-05 DONE boxes below from this subsection alone.

---

## MIG-05 FULL DONE only when (retirement checklist)

**Status: OPEN** — do not claim full MIG-05 complete until every box below is
verified. Step 3 alone ≠ full MIG-05. Step 4 retirement is mandatory for L5.

Must **ALL** be true before claiming full MIG-05 complete:

- [ ] Product meaning ranking does not use `PdfPageEmbeddingStore`
      (step 3 — already true; keep as regression gate)
- [ ] Product meaning ranking does not use `SavedPdfPageTextSource`
      (step 3 — already true; L6 Retired; keep as regression gate)
- [ ] Product meaning ranking reads `MemoryEvidenceEmbeddingStore` +
      `MemoryEvidence`
- [ ] `IndexPdfPageEmbeddings` dual-write of `PdfPageEmbedding*` stopped OR
      PdfPage path is write-obsolete with documented no readers
- [ ] `PdfPageEmbedding*` store/table/DAO retired or formally deleted per
      step 4 plan (L5 → Retired on `LEGACY_RECALL_SURFACE`)
- [ ] `LEGACY_RECALL_SURFACE` updated: L5 Retired; Live/Dual count decreased
- [ ] Cutover STALE / backfill path verified empty or no longer needed for
      page→evidence gap
- [ ] Non-PDF evidence embedding: either implemented end-to-end OR
      explicitly deferred with ADR/CHANGE_CONTROL note (do not silently
      claim full Spec MIG-05 if only PDF evidence vectors exist)
- [ ] CHANGELOG + CONTINUE record full MIG-05 closed only after above

**Explicit:** Step 3 alone ≠ full MIG-05. Step 4 retirement is mandatory for
L5. “New path works” is insufficient; migration finished means old paths
retired.