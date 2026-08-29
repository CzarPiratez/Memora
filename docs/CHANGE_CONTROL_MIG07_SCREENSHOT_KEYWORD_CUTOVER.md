# Change control: MIG-07 screenshot only — keyword Find cutover to SearchMemoryEvidence

**Date:** 2026-08-30  
**Type:** Per-asset Find cutover (screenshot only)  
**Decision guardrails:** Implement **MIG-07 screenshot sub-migration only**. Do
**not** cut over photo / note (L3–L4 stay Live). Do **not** claim full MIG-07 /
Recall DONE. Do **not** flip marketing AVAILABLE. Do **not** start MIG-05 claim
B, MIG-07B, or Canonical Recall facade rename. Leave unrelated dirty files
(`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`,
`_transcript_review.txt`) unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = screenshot keyword UI path only:**
   `ScreenshotOcrKeywordSearchViewModel` (+ readiness / open / Why as needed)
   uses `SearchMemoryEvidence` with `AssetType.SCREENSHOT`.
2. **Adapter** `MemoryEvidenceSearchHit` → existing
   `ScreenshotOcrKeywordSearchHit` / UI models. Preserve Why / open-original
   (source identity; no PDF page — stay honest).
3. **Readiness source change:** count READY screenshot Memory evidence
   (distinct documents) — not raw `ScreenshotOcrExtractionDao` corpus.
4. **DELETE** `SearchPersistedScreenshotOcrText` from production screenshot Find.
   Keep `ScreenshotOcrKeywordSearchSupport` / `PdfKeywordSearchSupport` for
   L3–L4 + highlight.
5. **Tests:** update VM / mapping / copy tests; adapter unit tests.
6. **LEGACY:** L2 → Retired; Live/Dual **N=4**. Do not touch L3–L4 screens.
7. No AVAILABLE flip. No MIG-05 B. No Canonical Recall facade.
8. Architectural convergence block required (below).

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-07 (per-asset sequencing; screenshot
  after PDF); Architecture Freeze §3; ADR-049; Local AI Spec retrieval honesty;
  Product Contract recall/explain.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`,
  `ESCAPE_HATCH_AUDIT`, ADR-049 / ADR-050, `CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER`,
  `CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE`, CI guard script.
- **Current-code evidence inspected:** `ScreenshotOcrKeywordSearchViewModel`,
  `SearchPersistedScreenshotOcrText`,
  `LoadPersistedScreenshotOcrKeywordSearchReadiness`,
  `OpenPersistedScreenshotForViewing`, `SearchMemoryEvidence` + PDF adapter
  pattern, LEGACY L2 row, guard allowlist.
- **Open ADRs / platform limitations checked:** ADR-049 Canonical Recall not
  yet one App API; ADR-050 A done / B open; per-asset MIG-07 sequencing;
  screenshot locator `image:whole` (no page).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new network/permissions/cloud AI. Screenshot Find reads Memory evidence
  only. Screenshot OCR extraction tables remain write targets — **not** product
  Find.
- **Smallest safe change:** screenshot VM cutover + adapter; readiness from
  Memory corpus; delete `SearchPersistedScreenshotOcrText`; L2 Retired; docs +
  guard.
- **Acceptance criteria:** A–D in delivery instruction.
- **Test and emulator verification plan:** focused unit tests (VM, adapter,
  copy); `:app:assembleDebug`; legacy guard PASS.
- **User-visible quality/accessibility review plan:** copy honesty for Memory
  evidence readiness; open-original without inventing a page.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Screenshot keyword Find product path (ui/search + application search)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): L2 (Screenshot OCR keyword Find via SearchPersistedScreenshotOcrText)
TARGET PATH: Canonical Recall via SearchMemoryEvidence (SCREENSHOT-filtered candidate generation; Canonical Recall API still not live)
WHY THIS CHANGE CONVERGES: retires extraction-table screenshot Find; product screenshot keyword hits now come from MemoryEvidence substrate
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L2 SearchPersistedScreenshotOcrText (this step); L3–L4 remain until authorized sub-migrations
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: L2 Live → Retired; Live/Dual N 5 → 4
ESCAPE-HATCH AFTER CHANGE: YES — UI can still produce hits without Canonical Recall via L3, L4, L7, L8 (L1/L2 no longer enable)
```

## Escape-hatch audit (this checkpoint)

```
Date: 2026-08-30
Live/Dual N: 4
Enabling L#s: L3, L4, L7, L8
Escape-hatch YES/NO: YES
Delta since last audit: L2 Live → Retired; N 5 → 4; screenshot keyword Find on SearchMemoryEvidence
Auditor: MIG-07 screenshot cutover delivery
```

## False-positive guard

- **ALLOWED:** screenshot OCR extraction tables remain write targets for
  extract/assembly.
- **FORBIDDEN:** screenshot product Find reading `ScreenshotOcrExtractionDao`
  for search results (satisfied: `SearchPersistedScreenshotOcrText` deleted;
  readiness/search use Memory).
- **FORBIDDEN:** extending screenshot keyword ranking/features beyond cutover
  parity.
- **FORBIDDEN:** cutting over photo/note in this step.

## Delivery record

- **Files/layers changed:**
  - Application: `ScreenshotMemoryEvidenceKeywordAdapter`;
    `ScreenshotOcrKeywordSearchModels`; readiness → Memory corpus (SCREENSHOT);
    **deleted** `SearchPersistedScreenshotOcrText`
  - UI: `ScreenshotOcrKeywordSearchViewModel` Hilt →
    `SearchMemoryEvidence(AssetType.SCREENSHOT)`; honesty copy updates
  - Tests: VM (param rename), adapter unit tests, copy tests
  - Guard: removed L2 `SearchPersistedScreenshotOcrText` allowlist entry; allow
    screenshot VM + adapter for `SearchMemoryEvidence`; resurrection check
  - Docs: this record; CONTINUE; GOVERNANCE; LEGACY; RECALL_ENFORCEMENT_INDEX;
    ESCAPE_HATCH_AUDIT; CHANGELOG Unreleased
- **Readiness source:** distinct READY screenshot Memory documents
  (`documentCount` for `AssetType.SCREENSHOT`) — was:
  `ScreenshotOcrExtractionDao.countCurrentSearchableCorpus`.
- **Automated verification and result:**
  - Guard: `powershell -File scripts/check-legacy-recall-surface.ps1` → **PASS**
  - Unit + assemble (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.application.images.ScreenshotMemoryEvidenceKeywordAdapterTest
      --tests com.memora.app.ui.search.ScreenshotOcrKeywordSearchViewModelTest
      --tests com.memora.app.ui.search.ScreenshotOcrKeywordSearchCopyTest
      :app:assembleDebug
    ```
    → **BUILD SUCCESSFUL**
- **Emulator/manual verification and result:** not required for this gate
  (unit + assemble + guard)
- **Failure/recovery paths verified:** BlankQuery; NothingSavedToSearch;
  empty Matches → NoMatches; open-original source-identity (no page)
- **Known limitation or follow-up:**
  - L3–L4 photo/note cutovers **remain**
  - Canonical Recall **not** a live single App API (ADR-049)
  - Full MIG-07 / Recall convergence DONE **not** claimed
  - Marketing AVAILABLE **not** claimed; MIG-05 claim B still open
- **Documentation/traceability/ADR updates:** living status docs only; no new
  ADR (MIG-07 already sequenced; per-asset delivery)
- **Git commit:** `feat: MIG-07 cut screenshot keyword Find over to SearchMemoryEvidence`
  (local; no push — hash in STOP REPORT / `git log -1`)

## Done vs remains (honesty)

| Done (this step) | Remains |
|------------------|---------|
| Screenshot Find VM → `SearchMemoryEvidence` (SCREENSHOT filter) | Photo / note keyword cutovers (L3–L4) |
| `SearchPersistedScreenshotOcrText` deleted | Canonical Recall as one App API |
| L2 Retired; Live/Dual **N=4** | Full MIG-07 / Recall DONE |
| Readiness from READY screenshot Memory evidence | MIG-05 claim B (non-PDF indexer) |
| Guard + unit tests + assembleDebug | Marketing AVAILABLE decision |
