# Change control: MIG-07 note only — keyword Find cutover to SearchMemoryEvidence

**Date:** 2026-08-30  
**Type:** Per-asset Find cutover (note only)  
**Decision guardrails:** Implement **MIG-07 note sub-migration only** (last
L1–L4 keyword Find). Do **not** claim full MIG-07 / Recall DONE. Do **not**
flip marketing AVAILABLE. Do **not** start MIG-05 claim B, MIG-07B, or
Canonical Recall facade rename. Leave unrelated dirty files
(`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`,
`_transcript_review.txt`, `docs/artifacts/M4-connectedDebugAndroidTest.txt`)
unstaged. No push.

## Lead decisions (LOCKED)

1. **Scope = note keyword UI path only:**
   `NotePageKeywordSearchViewModel` (+ readiness / open / Why as needed)
   uses `SearchMemoryEvidence` with `AssetType.NOTE`.
2. **Adapter** `MemoryEvidenceSearchHit` → existing
   `NotePageKeywordSearchHit` / UI models. Preserve Why / open-original
   (OneNote source identity; honest network/open separation).
3. **Readiness source change:** count READY note Memory evidence
   (distinct documents) — not raw `NotePageExtractionDao` corpus.
4. **DELETE** `SearchPersistedNotePageText` and dead
   `NotePageKeywordSearchSupport` from production note Find.
   Keep `PdfKeywordSearchSupport` for PDF highlight.
5. **Tests:** update VM / mapping / copy tests; adapter unit tests.
6. **LEGACY:** L4 → Retired; Live/Dual **N=2** (L7, L8 remain Live).
7. No AVAILABLE flip. No MIG-05 B. No Canonical Recall facade.
8. Architectural convergence block required (below).

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-07 (per-asset sequencing; note
  after photo); Architecture Freeze §3; ADR-049; Local AI Spec retrieval
  honesty; Product Contract recall/explain.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`,
  `ESCAPE_HATCH_AUDIT`, `RECALL_CONVERGENCE_DONE`, ADR-049 / ADR-050,
  `CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER`,
  `CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER`,
  `CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER`,
  `CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE`, CI guard script.
- **Current-code evidence inspected:** `NotePageKeywordSearchViewModel`,
  `SearchPersistedNotePageText`,
  `LoadPersistedNotePageKeywordSearchReadiness`,
  `OpenPersistedNotePageInOneNote`, `SearchMemoryEvidence` + photo adapter
  pattern, LEGACY L4 row, guard allowlist, `AssetType.NOTE`.
- **Open ADRs / platform limitations checked:** ADR-049 Canonical Recall not
  yet one App API; ADR-050 A done / B open; per-asset MIG-07 sequencing;
  note open-original may need network/OneNote (honest copy).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new network/permissions/cloud AI for keyword Find. Note Find reads
  Memory evidence only. Note extraction tables remain write targets — **not**
  product Find.
- **Smallest safe change:** note VM cutover + adapter; readiness from
  Memory corpus; delete `SearchPersistedNotePageText` + dead support; L4
  Retired; docs + guard.
- **Acceptance criteria:** A–D in delivery instruction.
- **Test and emulator verification plan:** focused unit tests (VM, adapter,
  copy); `:app:assembleDebug`; legacy guard PASS.
- **User-visible quality/accessibility review plan:** copy honesty for Memory
  evidence readiness; open-original network honesty preserved.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Note keyword Find product path (ui/search + application search)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): L4 (Note keyword Find via SearchPersistedNotePageText)
TARGET PATH: Canonical Recall via SearchMemoryEvidence (NOTE-filtered candidate generation; Canonical Recall API still not live)
WHY THIS CHANGE CONVERGES: retires extraction-table note Find; product note keyword hits now come from MemoryEvidence substrate; completes L1–L4 keyword cutovers
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L4 SearchPersistedNotePageText (this step); L7/L8 remain until Canonical Recall + ranking stage
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: L4 Live → Retired; Live/Dual N 3 → 2
ESCAPE-HATCH AFTER CHANGE: YES — UI can still produce hits without Canonical Recall via L7, L8 (L1–L4 no longer enable)
```

## Escape-hatch audit (this checkpoint)

```
Date: 2026-08-30
Live/Dual N: 2
Enabling L#s: L7, L8
Escape-hatch YES/NO: YES
Delta since last audit: L4 Live → Retired; N 3 → 2; note keyword Find on SearchMemoryEvidence
Auditor: MIG-07 note cutover delivery
```

## False-positive guard

- **ALLOWED:** note extraction tables remain write targets for
  extract/assembly.
- **FORBIDDEN:** note product Find reading `NotePageExtractionDao`
  for search results (satisfied: `SearchPersistedNotePageText` deleted;
  readiness/search use Memory).
- **FORBIDDEN:** extending note keyword ranking/features beyond cutover
  parity.
- **FORBIDDEN:** claiming Canonical Recall live API or Recall program DONE
  from this cutover alone (L7/L8 + MIG-07B remain).

## Delivery record

- **Files/layers changed:**
  - Application: `NoteMemoryEvidenceKeywordAdapter`;
    `NotePageKeywordSearchModels`; readiness → Memory corpus (NOTE);
    **deleted** `SearchPersistedNotePageText`;
    **deleted** `NotePageKeywordSearchSupport` (no remaining refs)
  - UI: `NotePageKeywordSearchViewModel` Hilt →
    `SearchMemoryEvidence(AssetType.NOTE)`; honesty copy updates
  - Tests: VM (existing), adapter unit tests, copy tests
  - Guard: removed L4 `SearchPersistedNotePageText` allowlist entry; allow
    note VM + adapter for `SearchMemoryEvidence`; resurrection check
  - Docs: this record; CONTINUE; GOVERNANCE; LEGACY; RECALL_ENFORCEMENT_INDEX;
    ESCAPE_HATCH_AUDIT; CHANGELOG Unreleased
- **Readiness source:** distinct READY note Memory documents
  (`documentCount` for `AssetType.NOTE`) — was:
  `NotePageExtractionDao.countCurrentSearchableCorpus`.
- **Automated verification and result:**
  - Guard: `powershell -File scripts/check-legacy-recall-surface.ps1` → **PASS**
  - Unit + assemble (`JAVA_HOME=C:\Users\DELL\.jdks\jdk-21.0.11+10`):
    ```
    .\gradlew.bat :app:testDebugUnitTest
      --tests com.memora.app.application.notes.NoteMemoryEvidenceKeywordAdapterTest
      --tests com.memora.app.ui.search.NotePageKeywordSearchViewModelTest
      --tests com.memora.app.ui.search.NotePageKeywordSearchCopyTest
      :app:assembleDebug
    ```
    → **BUILD SUCCESSFUL**
- **Emulator/manual verification and result:** not required for this gate
  (unit + assemble + guard)
- **Failure/recovery paths verified:** BlankQuery → EmptyQuery; NothingSavedToSearch;
  empty Matches → NoMatches; open-original source-identity (OneNote URLs)
- **Known limitation or follow-up:**
  - Canonical Recall **not** a live single App API (ADR-049)
  - Full MIG-07 / Recall convergence DONE **not** claimed (L7/L8 Live;
    MIG-07B; shared ranking; Canonical Recall facade remain)
  - Marketing AVAILABLE **not** claimed; MIG-05 claim B still open
  - Honest: “MIG-07 keyword L1–L4 cutovers complete” ≠ “Recall program DONE”
- **Documentation/traceability/ADR updates:** living status docs only; no new
  ADR (MIG-07 already sequenced; per-asset delivery)
- **Git commit:** `feat: MIG-07 cut note keyword Find over to SearchMemoryEvidence`
  (local; no push — hash in STOP REPORT / `git log -1`)

## Done vs remains (honesty)

| Done (this step) | Remains |
|------------------|---------|
| Note Find VM → `SearchMemoryEvidence` (NOTE filter) | Canonical Recall as one App API |
| `SearchPersistedNotePageText` deleted | Full MIG-07 / Recall DONE checklist |
| L4 Retired; Live/Dual **N=2** | L7 meaning-local ranking; L8 meaning Find |
| L1–L4 keyword cutovers complete | MIG-07B / shared ranking stage |
| Readiness from READY note Memory evidence | MIG-05 claim B (non-PDF indexer) |
| Guard + unit tests + assembleDebug | Marketing AVAILABLE decision |
