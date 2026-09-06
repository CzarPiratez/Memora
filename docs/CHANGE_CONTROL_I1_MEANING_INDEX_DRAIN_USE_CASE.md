# Change control: I1 — meaning-index drain is an application use case

**Date:** 2026-09-06  
**Type:** Application + UI mapping (meaning-index **construction**; not Find)  
**Closes:** PROGRAM_STATE Batch I / I1  
**Does not authorize:** I2/I3 workers, I4 time budget, AVAILABLE, a second Find path

## Pre-work record

- **Requirement IDs:** P-11 (invest intelligence at index time); P-14 (recoverable,
  bounded work); P-17 (UI → application → domain). PROGRAM_STATE I1.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work gate,
  CONTINUE, PRODUCT_SOURCE_REGISTRY, LOCAL_AI_TECHNICAL_SPEC (index locally;
  do not re-read originals at recall), RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE
  (Live/Dual **N = 0**), PROGRAM_STATE_AND_SEQUENCE_V1 §8b Batch I, CHANGE_CONTROL_TEMPLATE,
  CHANGE_CONTROL_D9_ASSEMBLY_DRAIN_CURSOR (sibling drain pattern),
  CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE (cutover still after a completed batch).
- **Current-code evidence inspected:** `AiPackDisclosureViewModel.onBuildIndexRequested`
  selected the queue, indexed summaries + PDF/OCR/note evidence, applied MIG-05
  cutover, and built remaining-hint copy from `pendingTotal - summaries.size`
  **before** the batch. `IndexMemoryEmbeddings` only indexes a provided list.
  `RunPendingAssetMemoryAssembly` already returns `hasMore` after work.
  `countMeaningIndexPending` / `listMeaningIndexSummaries` must stay in agreement (D-8).
- **Open ADRs / platform:** no new ADR. No schema change. No WorkManager.
- **Privacy:** on-device embeddings and skip/cutover metadata only. No originals
  leave the device. UI copy does not invent “available now.”
- **Smallest safe change:** one application use case owns select → index →
  evidence → cutover → after-batch `hasMore`. ViewModel maps result and progress
  to existing copy. Batch cap stays `MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP`.
- **Acceptance criteria:**
  - [x] ViewModel no longer talks to MemoryRepository / indexers / cutover
  - [x] `hasMore == remainingPending > 0` after the batch (invariant)
  - [x] Failed summaries still count as remaining (old arithmetic would have
        said “done”)
  - [x] Count without a selectable row is `SelectionDisagreed`, not empty copy
  - [x] NothingPending / SelectionDisagreed skip MIG-05 cutover
  - [x] UI remaining hint uses after-batch remaining
  - [x] Device (SM-A156E, 2026-09-06): Welcome → About → Build. No crash.
        Queue was empty (1001 summaries already indexed). First tap showed a
        contradictory empty-library line while corpus said “tap Build”. Honesty
        copy corrected and re-tapped (see Delivery record).
- **Holistic scenarios (before implement):**
  - User: empty library → “no READY memories,” not a mismatch
  - User: last 5 of 5 → indexed, no “tap again”
  - User: ~970 pending, 25/tap → honest remaining after the tap (not 970−25
        computed up front)
  - User: engine missing between button paint and tap → canned “model not ready”
  - User: count says N but select returns none → queue mismatch, not empty
  - User: double-tap while busy → ignored (`isBusy`)
  - User: a blank/failed summary in the batch → still told work remains
  - Technical: future I3 worker must call this use case, not a second indexer
  - Technical: I2/I3 workers must not exist yet
  - Edge: PDF pages only for PDFs; OCR only for photo/screenshot; notes only
        for notes; cutover only after a completed batch while the engine is Available
- **Alternatives considered:**
  - Leave orchestration in the ViewModel and only add `hasMore` there — rejected;
    I3 would then copy or bypass UI.
  - Worker in this slice — rejected; I2/I3 stay blocked as a pair after D-9, and
    I3 needs this use case first.
  - Time-budget batch (I4) — rejected for this slice; cap stays 25.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: meaning-index construction (`RunPendingMeaningIndex`)
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: Canonical Recall remains the sole product Find; this only builds
  the meaning index that candidate generation already reads
WHY THIS CHANGE CONVERGES: one drain owner UI and a future I3 worker both call;
  no new product search path
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: hand-cranked Build tap (I3)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

## Delivery record

- **Files/layers:** application `RunPendingMeaningIndex` + result/progress types;
  `AiPackDisclosureViewModel` thinned to one call; `AiPackDisclosureCopy`
  maps result/progress (application stays copy-free).
- **Automated verification:** `RunPendingMeaningIndexTest`,
  `AiPackDisclosureCopyTest`. Full `:app:testDebugUnitTest` **741 tests, 0
  failures** (2026-09-06).
- **Emulator/manual:** Samsung SM-A156E debug install after I1. Path:
  Welcome → About on-device meaning search → Build meaning index.
  Corpus: 1001 READY memories, 1001 summary vectors, 467 evidence vectors,
  `meaningIndexPending = 0`. First tap returned `NothingPending` immediately
  (no crash; progress not observable because the summary queue is empty).
  Corpus line always appended “Tap Build…” — fixed so that suffix is only
  when pending > 0. Empty-queue copy no longer says the library has no
  memories. Re-install + second tap: corpus and result agree. Leftover
  evidence (467 vs 1001 summaries) is still not a selectable queue; that is
  not this slice.
- **Failure/recovery:** engine unavailable writes nothing; disagreement is
  visible and retryable; `isBusy` still blocks a second tap; derived-data clear
  unchanged.
- **Known limitation:** I2/I3 workers and I4 time budget remain open. ~40 taps
  at 25/batch is still the hand-crank when summaries are pending.
  Quantity-as-count (“exactly N cards”) stays parked. Evidence vectors can
  lag summaries without becoming `meaningIndexPending` (only missing
  summaries and STALE_REINDEX_REQUIRED rows are selectable).
- **Git commit:** recorded after JVM green.
