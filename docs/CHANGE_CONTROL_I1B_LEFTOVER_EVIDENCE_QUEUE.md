# Change control: I1b — leftover embeddable evidence stays in the Build queue

**Date:** 2026-09-07  
**Type:** Data selection (meaning-index **construction**; not Find)  
**Closes:** leftover evidence after I1 device tap-through (1001 summaries / 467
evidence vectors; pending = 0)  
**Does not authorize:** I2/I3 workers, I4, AVAILABLE, mass-STALE, a second indexer

## Pre-work record

- **Requirement IDs:** P-11 (invest intelligence at index time); P-01
  evidence-backed recall; D-8 count/select agreement; I1 drain owner.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  PRODUCT_SOURCE_REGISTRY, RECALL_ENFORCEMENT_INDEX (N = 0),
  PROGRAM_STATE D-8 / Batch I, CHANGE_CONTROL_I1, CHANGE_CONTROL_RECALL_REACH_STALE_EVIDENCE_DEADLOCK,
  CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE (cutover is PDF-zero-vector only;
  never mass-STALE), CHANGE_CONTROL_TEMPLATE.
- **Current-code evidence inspected:** `countMeaningIndexPending` /
  `listMeaningIndexSummaries` admitted missing summaries or
  `STALE_REINDEX_REQUIRED`. Cutover marks STALE only for READY + summary +
  `pdf:page` evidence + **zero** evidence vectors, and restores READY on **any**
  one evidence vector. OCR / note leftovers, and partial PDF pages, never
  re-entered the queue. `RunPendingMeaningIndex` already indexes evidence for
  whatever batch it is given.
- **Open ADRs / platform:** no new ADR. No schema change. No WorkManager.
- **Privacy:** selection only over on-device Room evidence ids and excerpts.
  No originals reopened for the count.
- **Smallest safe change:** extend the shared pending/select WHERE with an
  EXISTS over embeddable evidence that lacks a vector for this model. Do **not**
  widen cutover STALE (would hide READY rows and restore too early).
- **Acceptance criteria:**
  - [x] READY + summary + unindexed OCR / note / `pdf:page` is pending and selectable
  - [x] Embedding that owed evidence clears the queue
  - [x] Partial evidence (1 of 2 OCR rows) stays pending
  - [x] SOURCE_METADATA-only and locator-shaped `pdf:page:N` ids are not pending
  - [x] READY + summary + no embeddable evidence is not pending (termination)
  - [x] D-8 tests still hold (missing summary, STALE, count/select agreement)
  - [x] Device (SM-A156E): after install, corpus still 1001 / 467 and pending
        0. Build returned empty-queue copy. That means every embeddable
        evidence row already has a vector; the remaining memories have no
        OCR / note / `pdf:page` excerpt to embed. Cursor and drain agree.
- **Holistic scenarios:**
  - User: 1001 summaries done, OCR leftover → Build selects a batch, corpus
    pending > 0, “tap Build” only while pending
  - User: photo with no OCR text → not pending (nothing to embed)
  - User: PDF with 1 of 10 pages embedded → still pending
  - Technical: I3 must not start until this queue can see leftover evidence
  - Technical: do not mass-STALE (Find stays READY; cutover restore stays coarse)
  - Edge: unresolvable locator-as-id never loops; other model still re-owes
- **Alternatives considered:**
  - Widen MIG-05 STALE to OCR/notes — rejected; cutover says never mass-STALE,
    restore on first vector would drop partial work, keyword/meaning lookups
    already paid a STALE deadlock once.
  - A second “evidence-only Build” — rejected; I1 is the sole drain.
  - I3 first — rejected; worker would see `NothingPending`.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: meaning-index construction (selection)
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: Canonical Recall remains sole Find; this only selects rows
  RunPendingMeaningIndex already writes
WHY THIS CHANGE CONVERGES: one queue, one drain; leftover evidence is no longer
  invisible construction
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: hand-cranked Build (I3)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Delivery record

- **Files/layers:** `MemoryDao` pending/select WHERE (shared); repository kdoc;
  `RoomMemoryRepositoryMeaningIndexSelectionIntegrationTest`.
- **Automated verification:** `RoomMemoryRepositoryMeaningIndexSelectionIntegrationTest`
  on Medium Phone emulator (D-8 cases + I1b OCR / note / PDF / partial /
  metadata / unresolvable id / termination). **PASS.**
- **Emulator/manual:** SM-A156E debug install. Pending stayed 0; 467 did not
  move. Confirms this library’s leftover is missing embeddable evidence
  rows, not an invisible embedding queue.
- **Known limitation:** I2/I3/I4 still open. Memories with no embeddable
  evidence rows cannot grow the 467 count — that is extraction / assembly
  completeness, not this cursor. Cutover remains PDF-zero-vector only.
- **Git commit:** recorded after verification.
