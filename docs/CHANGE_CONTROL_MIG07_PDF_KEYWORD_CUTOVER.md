# Change control: MIG-07 PDF only — keyword Find cutover to SearchMemoryEvidence

**Date:** 2026-08-29  
**Type:** Per-asset Find cutover (PDF only)  
**Decision guardrails:** Implement **MIG-07 PDF sub-migration only**. Do **not**
cut over screenshot / photo / note (L2–L4 stay Live). Do **not** claim full
MIG-07 / Recall DONE. Do **not** flip marketing AVAILABLE. Do **not** start
MIG-05 claim B, MIG-07B, or Canonical Recall facade rename. Leave unrelated
dirty files (`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`,
`_transcript_review.txt`) unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = PDF keyword UI path only:** `PdfKeywordSearchViewModel` (+
   readiness / open / Why as needed) uses `SearchMemoryEvidence` instead of
   `SearchPersistedPdfPageText`.
2. **Filter to `AssetType.PDF`** via optional `assetType` on
   `SearchMemoryEvidence` (default remains all types).
3. **Preserve user-visible behavior** as far as the evidence substrate allows:
   query normalize/limits, Why/explain, open original to correct page
   (`openPageNumber` / `pdf:page:N`). Map via adapter to existing
   `PdfKeywordSearchHit` / UI models — no large UI redesign.
4. **Readiness source change:** count READY PDF Memory evidence (not raw
   `PdfExtractionDao` searchable corpus).
5. **DELETE** `SearchPersistedPdfPageText` from production PDF Find. Keep
   `PdfKeywordSearchSupport` (shared by L2–L4 + highlight).
6. **Tests:** update VM / mapping / SearchMemoryEvidence filter tests; keep
   `PdfKeywordSearchSupportTest`.
7. **LEGACY:** L1 → Retired; Live/Dual **N=5**. Do not touch L2–L4 screens.
8. No AVAILABLE flip. No MIG-05 B. No Canonical Recall facade.
9. Architectural convergence block required (below).

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-07 (per-asset sequencing; PDF first);
  Architecture Freeze §3; ADR-049 (Canonical Recall naming; keyword candidate
  gen); Local AI Spec retrieval honesty; Product Contract recall/explain.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`,
  `ESCAPE_HATCH_AUDIT`, ADR-049 / ADR-050, `ARCHITECTURAL_MIGRATION_SPEC_V1`
  MIG-07, `CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE`,
  `CANONICAL_RECALL_RESULT_CONTRACT` (DRAFT), CI guard script.
- **Current-code evidence inspected:** `PdfKeywordSearchViewModel`,
  `SearchPersistedPdfPageText`, `LoadPersistedPdfKeywordSearchReadiness`,
  `OpenPersistedPdfForViewing`, `SearchMemoryEvidence` +
  `MemoryEvidenceExcerptSearch` / `MemoryDao`, `PdfKeywordSearchSupport`
  (L2–L4 shared), LEGACY L1 row, guard allowlist.
- **Open ADRs / platform limitations checked:** ADR-049 Canonical Recall not
  yet one App API; ADR-050 A done / B open; per-asset MIG-07 sequencing.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new network/permissions/cloud AI. PDF Find reads Memory evidence only.
  PdfExtractionDao remains write target for extract/assembly — **not** product
  Find.
- **Smallest safe change:** optional `assetType` filter on SearchMemoryEvidence
  + Room queries; PDF VM cutover + adapter; readiness from Memory corpus;
  delete `SearchPersistedPdfPageText`; L1 Retired; docs + guard.
- **Acceptance criteria:** A–G in delivery instruction.
- **Test and emulator verification plan:** focused unit tests (VM, adapter,
  SearchMemoryEvidence filter, copy); `:app:assembleDebug`; legacy guard PASS.
- **User-visible quality/accessibility review plan:** copy honesty for Memory
  evidence readiness; open-original page parity via locator.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: PDF keyword Find product path (ui/search + application search)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): L1 (PDF keyword Find via SearchPersistedPdfPageText)
TARGET PATH: Canonical Recall via SearchMemoryEvidence (PDF-filtered candidate generation; Canonical Recall API still not live)
WHY THIS CHANGE CONVERGES: retires extraction-table PDF Find; product PDF keyword hits now come from MemoryEvidence substrate
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L1 SearchPersistedPdfPageText (this step); L2–L4 remain until authorized sub-migrations
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: L1 Live → Retired; Live/Dual N 6 → 5
ESCAPE-HATCH AFTER CHANGE: YES — UI can still produce hits without Canonical Recall via L2, L3, L4, L7, L8 (L1 no longer enables)
```

## Escape-hatch audit (this checkpoint)

```
Date: 2026-08-29
Live/Dual N: 5
Enabling L#s: L2, L3, L4, L7, L8
Escape-hatch YES/NO: YES
Delta since last audit: L1 Live → Retired; N 6 → 5; PDF keyword Find on SearchMemoryEvidence
Auditor: MIG-07 PDF cutover delivery
```

## False-positive guard

- **ALLOWED:** PDF extraction tables remain write targets for extract/assembly.
- **FORBIDDEN:** PDF product Find reading `PdfExtractionDao` for search results
  (satisfied: `SearchPersistedPdfPageText` deleted; readiness/search use Memory).
- **FORBIDDEN:** extending PDF keyword ranking/features beyond cutover parity.
- **FORBIDDEN:** cutting over screenshot/photo/note in this step.

## Delivery record

- **Files/layers changed:**
  - Application: optional `assetType` on `SearchMemoryEvidence` /
    `MemoryEvidenceExcerptSearch`; `PdfMemoryEvidenceKeywordAdapter`;
    `PdfKeywordSearchModels`; readiness → Memory corpus (PDF);
    **deleted** `SearchPersistedPdfPageText`
  - Data: `MemoryDao` asset-type-filtered count/search/corpus; Room adapter
  - UI: `PdfKeywordSearchViewModel` Hilt → `SearchMemoryEvidence(AssetType.PDF)`;
    honesty copy updates
  - Tests: VM (param rename), adapter unit tests, SearchMemoryEvidence filter
    tests, copy tests; deleted extraction-table keyword androidTest
  - Guard: removed L1 `SearchPersistedPdfPageText` allowlist entry; allow PDF
    VM + adapter for `SearchMemoryEvidence`; resurrection check
  - Docs: this record; CONTINUE; GOVERNANCE; LEGACY; RECALL_ENFORCEMENT_INDEX;
    ESCAPE_HATCH_AUDIT; CHANGELOG Unreleased
- **Readiness source:** READY PDF `MemoryEvidence` excerpt rows + distinct PDF
  documents (was: `PdfExtractionDao.countCurrentSearchableCorpus`).
- **Automated verification and result:**
  - Guard: `powershell -File scripts/check-legacy-recall-surface.ps1` → **PASS**
  - Unit + assemble (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.application.memory.SearchMemoryEvidenceTest
      --tests com.memora.app.application.documents.PdfMemoryEvidenceKeywordAdapterTest
      --tests com.memora.app.ui.search.PdfKeywordSearchViewModelTest
      --tests com.memora.app.ui.search.PdfKeywordSearchCopyTest
      --tests com.memora.app.application.documents.PdfKeywordSearchSupportTest
      :app:assembleDebug
    ```
    → **BUILD SUCCESSFUL**
- **Emulator/manual verification and result:** not required for this gate
  (unit + assemble + guard)
- **Failure/recovery paths verified:** BlankQuery; NothingSavedToSearch;
  empty Matches → NoMatches; non-page evidence dropped by adapter
- **Known limitation or follow-up:**
  - L2–L4 screenshot/photo/note cutovers **remain**
  - Canonical Recall **not** a live single App API (ADR-049)
  - Full MIG-07 / Recall convergence DONE **not** claimed
  - Marketing AVAILABLE **not** claimed; MIG-05 claim B still open
- **Documentation/traceability/ADR updates:** living status docs only; no new
  ADR (MIG-07 already sequenced; per-asset delivery)
- **Git commit:** `feat: MIG-07 cut PDF keyword Find over to SearchMemoryEvidence`
  (local; no push — hash in STOP REPORT / `git log -1`)

## Done vs remains (honesty)

| Done (this step) | Remains |
|------------------|---------|
| PDF Find VM → `SearchMemoryEvidence` (PDF filter) | Screenshot / photo / note keyword cutovers (L2–L4) |
| `SearchPersistedPdfPageText` deleted | Canonical Recall as one App API |
| L1 Retired; Live/Dual **N=5** | Full MIG-07 / Recall DONE |
| Readiness from READY PDF Memory evidence | MIG-05 claim B (non-PDF indexer) |
| Guard + unit tests + assembleDebug | Marketing AVAILABLE decision |
