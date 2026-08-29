# Change control: MIG-07 photo only — keyword Find cutover to SearchMemoryEvidence

**Date:** 2026-08-30  
**Type:** Per-asset Find cutover (photo only)  
**Decision guardrails:** Implement **MIG-07 photo sub-migration only**. Do
**not** cut over note (L4 stays Live). Do **not** claim full MIG-07 /
Recall DONE. Do **not** flip marketing AVAILABLE. Do **not** start MIG-05 claim
B, MIG-07B, or Canonical Recall facade rename. Leave unrelated dirty files
(`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`,
`_transcript_review.txt`, and any leftover screenshot WIP) unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = photo keyword UI path only:**
   `PhotoOcrKeywordSearchViewModel` (+ readiness / open / Why as needed)
   uses `SearchMemoryEvidence` with `AssetType.PHOTO`.
2. **Adapter** `MemoryEvidenceSearchHit` → existing
   `PhotoOcrKeywordSearchHit` / UI models. Preserve Why / open-original
   (source identity; no PDF page — stay honest).
3. **Readiness source change:** count READY photo Memory evidence
   (distinct documents) — not raw `PhotoOcrExtractionDao` corpus.
4. **DELETE** `SearchPersistedPhotoOcrText` from production photo Find.
   Keep `PhotoOcrKeywordSearchSupport` / `PdfKeywordSearchSupport` for
   L4 + highlight.
5. **Tests:** update VM / mapping / copy tests; adapter unit tests.
6. **LEGACY:** L3 → Retired; Live/Dual **N=3**. Do not touch L4 screen.
7. No AVAILABLE flip. No MIG-05 B. No Canonical Recall facade.
8. Architectural convergence block required (below).

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-07 (per-asset sequencing; photo
  after screenshot); Architecture Freeze §3; ADR-049; Local AI Spec retrieval
  honesty; Product Contract recall/explain.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`,
  `ESCAPE_HATCH_AUDIT`, ADR-049 / ADR-050, `CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER`,
  `CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER`,
  `CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE`, CI guard script.
- **Current-code evidence inspected:** `PhotoOcrKeywordSearchViewModel`,
  `SearchPersistedPhotoOcrText`,
  `LoadPersistedPhotoOcrKeywordSearchReadiness`,
  `OpenPersistedPhotoForViewing`, `SearchMemoryEvidence` + screenshot adapter
  pattern, LEGACY L3 row, guard allowlist, `AssetType.PHOTO`.
- **Open ADRs / platform limitations checked:** ADR-049 Canonical Recall not
  yet one App API; ADR-050 A done / B open; per-asset MIG-07 sequencing;
  photo locator `image:whole` (no page).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new network/permissions/cloud AI. Photo Find reads Memory evidence
  only. Photo OCR extraction tables remain write targets — **not** product
  Find.
- **Smallest safe change:** photo VM cutover + adapter; readiness from
  Memory corpus; delete `SearchPersistedPhotoOcrText`; L3 Retired; docs +
  guard.
- **Acceptance criteria:** A–D in delivery instruction.
- **Test and emulator verification plan:** focused unit tests (VM, adapter,
  copy); `:app:assembleDebug`; legacy guard PASS.
- **User-visible quality/accessibility review plan:** copy honesty for Memory
  evidence readiness; open-original without inventing a page.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Photo keyword Find product path (ui/search + application search)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): L3 (Photo OCR keyword Find via SearchPersistedPhotoOcrText)
TARGET PATH: Canonical Recall via SearchMemoryEvidence (PHOTO-filtered candidate generation; Canonical Recall API still not live)
WHY THIS CHANGE CONVERGES: retires extraction-table photo Find; product photo keyword hits now come from MemoryEvidence substrate
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L3 SearchPersistedPhotoOcrText (this step); L4 remains until authorized sub-migration
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: L3 Live → Retired; Live/Dual N 4 → 3
ESCAPE-HATCH AFTER CHANGE: YES — UI can still produce hits without Canonical Recall via L4, L7, L8 (L1–L3 no longer enable)
```

## Escape-hatch audit (this checkpoint)

```
Date: 2026-08-30
Live/Dual N: 3
Enabling L#s: L4, L7, L8
Escape-hatch YES/NO: YES
Delta since last audit: L3 Live → Retired; N 4 → 3; photo keyword Find on SearchMemoryEvidence
Auditor: MIG-07 photo cutover delivery
```

## False-positive guard

- **ALLOWED:** photo OCR extraction tables remain write targets for
  extract/assembly.
- **FORBIDDEN:** photo product Find reading `PhotoOcrExtractionDao`
  for search results (satisfied: `SearchPersistedPhotoOcrText` deleted;
  readiness/search use Memory).
- **FORBIDDEN:** extending photo keyword ranking/features beyond cutover
  parity.
- **FORBIDDEN:** cutting over note in this step.

## Delivery record

- **Files/layers changed:**
  - Application: `PhotoMemoryEvidenceKeywordAdapter`;
    `PhotoOcrKeywordSearchModels`; readiness → Memory corpus (PHOTO);
    **deleted** `SearchPersistedPhotoOcrText`
  - UI: `PhotoOcrKeywordSearchViewModel` Hilt →
    `SearchMemoryEvidence(AssetType.PHOTO)`; honesty copy updates
  - Tests: VM (existing), adapter unit tests, copy tests
  - Guard: removed L3 `SearchPersistedPhotoOcrText` allowlist entry; allow
    photo VM + adapter for `SearchMemoryEvidence`; resurrection check
  - Docs: this record; CONTINUE; GOVERNANCE; LEGACY; RECALL_ENFORCEMENT_INDEX;
    ESCAPE_HATCH_AUDIT; CHANGELOG Unreleased
- **Readiness source:** distinct READY photo Memory documents
  (`documentCount` for `AssetType.PHOTO`) — was:
  `PhotoOcrExtractionDao.countCurrentSearchableCorpus`.
- **Automated verification and result:**
  - Guard: `powershell -File scripts/check-legacy-recall-surface.ps1` → **PASS**
  - Unit + assemble (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.application.images.PhotoMemoryEvidenceKeywordAdapterTest
      --tests com.memora.app.ui.search.PhotoOcrKeywordSearchViewModelTest
      --tests com.memora.app.ui.search.PhotoOcrKeywordSearchCopyTest
      :app:assembleDebug
    ```
    → **BUILD SUCCESSFUL**
- **Emulator/manual verification and result:** not required for this gate
  (unit + assemble + guard)
- **Failure/recovery paths verified:** BlankQuery; NothingSavedToSearch;
  empty Matches → NoMatches; open-original source-identity (no page)
- **Known limitation or follow-up:**
  - L4 note cutover **remains**
  - Canonical Recall **not** a live single App API (ADR-049)
  - Full MIG-07 / Recall convergence DONE **not** claimed
  - Marketing AVAILABLE **not** claimed; MIG-05 claim B still open
- **Documentation/traceability/ADR updates:** living status docs only; no new
  ADR (MIG-07 already sequenced; per-asset delivery)
- **Git commit:** `feat: MIG-07 cut photo keyword Find over to SearchMemoryEvidence`
  (local; no push — hash in STOP REPORT / `git log -1`)

## Done vs remains (honesty)

| Done (this step) | Remains |
|------------------|---------|
| Photo Find VM → `SearchMemoryEvidence` (PHOTO filter) | Note keyword cutover (L4) |
| `SearchPersistedPhotoOcrText` deleted | Canonical Recall as one App API |
| L3 Retired; Live/Dual **N=3** | Full MIG-07 / Recall DONE |
| Readiness from READY photo Memory evidence | MIG-05 claim B (non-PDF indexer) |
| Guard + unit tests + assembleDebug | Marketing AVAILABLE decision |
