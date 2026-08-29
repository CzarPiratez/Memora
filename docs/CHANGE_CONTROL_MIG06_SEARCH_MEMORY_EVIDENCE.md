# Change control: MIG-06 step 1 — Additive SearchMemoryEvidence

**Date:** 2026-08-29  
**Type:** Application use case + Room query (additive only)  
**Decision guardrails:** Implement MIG-06 **step 1 / Spec additive scope only**.
Do **not** start MIG-07. Do **not** rewire any ViewModel/screen. Do **not**
modify L1–L4 `SearchPersisted*` / `*KeywordSearch*` production call sites. Do
**not** claim Canonical Recall exists as a live App API (ADR-049). Do **not**
flip marketing AVAILABLE. Do **not** implement non-PDF evidence indexer
(ADR-050 claim B remains open). Do **not** bump Room schema unless inspection
proves an index is required (prefer no migration). Leave unrelated dirty files
(`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`,
`_transcript_review.txt`) unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = MIG-06 Spec additive only:** `SearchMemoryEvidence` querying
   `MemoryEvidence` excerpts; unit-tested; Hilt-injectable; **no**
   ViewModel/screen/keyword-class modifications.
2. **Result contract** carries future MIG-07 Why + open-original fields:
   `memoryId`, `revisionId`, `evidenceId`, `kind`, `locator`, display
   `excerpt`, asset identity (`sourceId`, `sourceAssetKey`, `assetType`,
   `label`), optional `openPageNumber` from `pdf:page:N`, and
   `retrievalPath=KEYWORD`.
3. **Tests cover** all current production evidence kinds used today:
   `SOURCE_METADATA`, `OCR_TEXT`, `DOCUMENT_TEXT`, `NOTE_TEXT` (plus empty
   query / empty corpus honesty).
4. **No Room version bump** — LIKE query only; no new index (Spec: no DB
   migration required).
5. **Do not extend L1–L4** ranking/features. Do not claim Canonical Recall
   exists. Do not start MIG-07. Do not flip AVAILABLE. Do not non-PDF indexer.
6. **Architectural convergence** filled below (template).

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-06 (additive unified Memory-Evidence
  search); Architecture Freeze §3 (one evidence substrate); ADR-049
  (`SearchMemoryEvidence` = keyword candidate generation into Canonical
  Recall); Local AI Spec retrieval honesty (keyword ≠ meaning); Product
  Contract recall/explain. Supporting: A-03 replaceable use cases.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`,
  `ESCAPE_HATCH_AUDIT`, ADR-049 / ADR-050, `ARCHITECTURAL_MIGRATION_SPEC_V1`
  MIG-06 (+ MIG-07 out of scope), `CANONICAL_RECALL_RESULT_CONTRACT` (DRAFT
  field parity), `CHANGE_CONTROL_TEMPLATE`, prior MIG-05 change-control,
  CI guard script.
- **Current-code evidence inspected:**
  - `SearchPersistedPdfPageText` / photo / screenshot / note (+ hit/outcome
    shapes; LIKE + excerpt helpers)
  - `MemoryDao` / `MemoryEvidenceEntity` / `MemoryEvidenceKind`
  - `MemoryRepository.findEvidenceSearchRows` (meaning path; id-keyed, not
    substring search — not reused as product Find)
  - `PersistenceModule` Hilt patterns; `SearchAssetMemoriesByMeaning` as
    Memory-backed search sibling (meaning, not keyword)
  - `scripts/check-legacy-recall-surface.sh` section D (was forbid
    `SearchMemoryEvidence` until authorized)
- **Open ADRs / platform limitations checked:** ADR-049 Canonical Recall
  target-only; ADR-050 A done / B open; no FTS project before MIG-06 (Spec);
  LIKE on `excerpt` does not need a new index for correctness.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new network, permissions, or cloud AI. Reads derived Memory evidence only.
  Originals remain read-only. No extraction-table Find path added.
- **Smallest safe change:** Application use case + excerpt-search port + Room
  adapter + Dao LIKE queries + Hilt bind + unit tests + docs + CI allowlist.
  Zero UI/VM/`SearchPersisted*` production edits.
- **Acceptance criteria:** A–G in delivery instruction (literal matches all
  kinds; no UI cutover; guard PASS; CONTINUE/change-control honest;
  assembleDebug + focused tests PASS; local commit; STOP REPORT).
- **Test and emulator verification plan:** Focused debug unit tests for use
  case; `:app:assembleDebug`; guard script; no instrumentation required (no
  UI).
- **User-visible quality/accessibility review plan:** N/A (no UI wired).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: application keyword candidate generation over MemoryEvidence
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (additive; L1–L4 unchanged Live)
TARGET PATH: Canonical Recall via SearchMemoryEvidence (MIG-06 ready; MIG-07 cutover later)
WHY THIS CHANGE CONVERGES: proves one evidence-substrate literal search without extending L1–L4
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L1–L4 SearchPersisted* / per-type KeywordSearch (Retire by MIG-07)
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: unchanged (N=6; L1–L4 still Live; optional note only)
ESCAPE-HATCH AFTER CHANGE: YES — UI still produces hits without Canonical Recall via L1–L4, L7, L8
```

## Escape-hatch audit (this checkpoint)

```
Date: 2026-08-29
Live/Dual N: 6
Enabling L#s: L1, L2, L3, L4, L7, L8
Escape-hatch YES/NO: YES
Delta since last audit: none (MIG-06 additive; product escape-hatch unchanged)
Auditor: MIG-06 step 1 delivery
```

## Delivery record

- **Files/layers changed:**
  - Application: `SearchMemoryEvidence`, `MemoryEvidenceExcerptSearch` port,
    `MemoryEvidenceLiteralSearchSupport`
  - Data: `MemoryDao` LIKE count/search projections;
    `RoomMemoryEvidenceExcerptSearch`; `PersistenceModule` bind
  - Tests: `SearchMemoryEvidenceTest`
  - Guard: `scripts/check-legacy-recall-surface.sh` (ALLOW MIG-06 application
    files; still FORBIDDEN under `ui/**`; still FORBIDDEN `CanonicalRecall`)
  - Docs: this record; `CONTINUE`; `GOVERNANCE`; `LEGACY_RECALL_SURFACE`
    optional note; `RECALL_ENFORCEMENT_INDEX` / `ESCAPE_HATCH_AUDIT`;
    `PRODUCT_SOURCE_REGISTRY`; `CHANGELOG` Unreleased
  - Unchanged (intentional): all `ui/search/**`, all `SearchPersisted*`,
    Room version 15, hashed freeze/spec, ROADMAP / libs.versions.toml /
    `_transcript_review.txt`
- **Automated verification and result:**
  - Guard: `powershell -File scripts/check-legacy-recall-surface.ps1` →
    **PASS** (exit 0)
  - Unit + assemble (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.application.memory.SearchMemoryEvidenceTest
      :app:assembleDebug
    ```
    → **BUILD SUCCESSFUL** (SearchMemoryEvidenceTest + assembleDebug)
- **Emulator/manual verification and result:** N/A — no UI cutover
- **Failure/recovery paths verified:** BlankQuery; NothingSavedToSearch;
  empty Matches when corpus exists but no literal hit
- **Known limitation or follow-up:**
  - **MIG-07** cutover (wire ViewModels; retire L1–L4) **not started**
  - **MIG-05 claim B** (non-PDF evidence indexer) **still open** (ADR-050)
  - Canonical Recall **not** a live single App API (ADR-049)
  - Marketing **AVAILABLE** still **not** claimed
  - No FTS; LIKE scan acceptable for additive proof
- **Documentation/traceability/ADR updates:** living status docs only; no new
  ADR (MIG-06 already sequenced; ADR-049 naming already accepted)
- **Git commit:** `feat: MIG-06 step 1 additive SearchMemoryEvidence`
  (local; no push — see `git log -1` for hash)

## Done vs remains (honesty)

| Done (this step) | Remains |
|------------------|---------|
| Additive `SearchMemoryEvidence` + port + Room LIKE + Hilt | MIG-07 UI/VM cutover + L1–L4 retirement |
| Unit tests all current kinds + empty-query honesty | Canonical Recall as one App API |
| CI allowlist for MIG-06 application files | MIG-05 claim B (non-PDF indexer) |
| Docs: CONTINUE / change-control / N=6 unchanged | Marketing AVAILABLE decision |
| L1–L4 still Live; product Find escape-hatch unchanged | FTS / ranking redesign / Grounded Answers code |
