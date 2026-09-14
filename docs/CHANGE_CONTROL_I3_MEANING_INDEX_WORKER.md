# Change control: I3 — meaning-index worker + Stop (I4-lite wall-clock)

**Date:** 2026-09-14  
**Type:** Application + WorkManager + UI (meaning-index **construction**; not Find)  
**Closes:** PROGRAM_STATE Batch I / I3  
**Does not authorize:** full I4 (measured per-item cost), I15/I31 (search-while-indexing
honesty), AVAILABLE, Act, a second indexer, or a new Find path

## Pre-work record

- **Requirement IDs:** P-11 (invest intelligence at index time); P-14 (recoverable,
  bounded work); P-17 (UI → application → domain). PROGRAM_STATE I3.
- **Source documents read:** ENGINEERING_CHARTER (holistic scenarios before
  implement), GOVERNANCE pre-work gate, CONTINUE, PRODUCT_SOURCE_REGISTRY,
  LOCAL_AI_TECHNICAL_SPEC (index locally; originals stay put),
  RECALL_ENFORCEMENT_INDEX (Live/Dual **N = 0**), LEGACY_RECALL_SURFACE,
  PROGRAM_STATE_AND_SEQUENCE_V1 §8b Batch I, CHANGE_CONTROL_TEMPLATE,
  CHANGE_CONTROL_I1 / I1b (this worker must call `RunPendingMeaningIndex`),
  CHANGE_CONTROL_I2 (clone the worker trio, do not copy `FailedSafely` Continue).
- **Current-code evidence inspected:** About **Build meaning index** called
  `RunPendingMeaningIndex` once per tap (cap 25) with no Stop and no
  auto-continue. `hasMore` is the after-batch pending count (I1). Leftover
  embeddable evidence is already in the same queue (I1b). I2 already ships
  Worker + Scheduler + DecisionMapper + unique-work observe + Stop +
  clear-index cancel-by-tag. No existing foreground-notification helper; I2
  did not add one.
- **Open ADRs / platform:** no new ADR. No schema change. One global unique
  work name (`meaning-index-drain`) — pending select is not per-`SourceId`.
  WorkManager execution ceiling is ~10 minutes; this slice uses a 4-minute
  wall-clock budget inside one worker, then re-enqueues (I4-lite, not I4).
- **Privacy:** construction only over already-saved deterministic facts and
  on-device embeddings. WorkManager output is counts and flags, not excerpts.
- **Smallest safe change:** worker calls only `RunPendingMeaningIndex`, looping
  batches until empty, Stop, a reportable stop, or the wall-clock budget.
  About Build enqueues; the screen observes unique work and offers Stop.
  `EngineUnavailable` / `SelectionDisagreed` map to StopAndReport, never
  Continue or retry. Count cap stays `MAX_MEMORIES_PER_TAP = 25`.
- **Acceptance criteria:**
  - [x] Decision mapper: BudgetExhausted → Continue; Finished → CompletedDrain;
        EngineUnavailable / SelectionDisagreed / Stopped → StopAndReport;
        unexpected exception → RetryableFailure
  - [x] Drain session loops batches under a 4-minute budget; first batch always
        runs; cancellation is not swallowed as retry
  - [x] Observation: running progress preferred for remaining; cancelled chain
        is not Failed; `KEY_STOPPED` success is Cancelled, not Completed
  - [x] Double-tap Build while indexing does not enqueue again
  - [x] Returning to About reconnects if unique work is still running
  - [x] Back remains enabled while indexing (`isBusy` still blocks only
        ack/download)
  - [x] Derived-data clear cancels the meaning-index tag
  - [x] Copy is one-tap drain + Stop; no “available now”; cap stays 25
  - [ ] Device: Welcome → About → one tap Build; Stop; leave About and
        reconnect; clear-index cancels work (founder rebuild before filming)
- **Holistic scenarios (before implement):**
  - User: pending 0 → one short job, then empty-queue copy, not a hang
  - User: hundreds pending → one tap drains; card shows indexed-so-far and
    remaining; Stop leaves remaining as Continue on the next Build
  - User: leaves About while running → same ViewModel keeps observing;
    process death reconnects from unique work
  - User: Stop mid-drain → Cancelled / stopped output is “Stopped,” not Failed
    and not “index complete”
  - User: double-tap Build → ignored
  - User: clear index while running → cancel + Ready
  - Technical: `EngineUnavailable` must not retry or Continue (missing model)
  - Technical: `SelectionDisagreed` must not retry or Continue (queue defect)
  - Technical: do not raise the 25 cap; do not `while (hasMore)` on the ViewModel
  - Technical: do not spawn one WorkManager node per 25 memories (~60 nodes
    on a 1,500 library) — loop inside the worker under a time budget
  - Edge: empty infos on first observe do not overwrite a live indexing
    refresh; historical SUCCEEDED after process death is not shown as a
    fresh “Indexed N” until the user starts a drain
- **Alternatives considered:**
  - Raise `MAX_MEMORIES_PER_TAP` — rejected; that is unbounded work dressed
    as a larger count, and I4 is the measured budget.
  - ViewModel `while (hasMore)` — rejected; process death loses the drain;
    other pipelines already use WorkManager.
  - One WorkManager node per batch (clone I2 literally) — rejected; a 1,500
    memory library would enqueue ~60 nodes. I4-lite folds the time budget
    into this slice.
  - Foreground service / notification — rejected for this slice; the app has
    no notification infra and I2 did not add one. Drain may pause if the
    process is killed. Recorded as a limitation, not hidden.
  - Retry `EngineUnavailable` — rejected; that is a hot loop on a missing
    model.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: meaning-index construction (I3 worker)
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: same RunPendingMeaningIndex → Memory embeddings Canonical Recall
  already reads; Canonical Recall stays the sole product Find
WHY THIS CHANGE CONVERGES: one drain owner (RunPendingMeaningIndex) that UI
  used to call per tap and WorkManager now calls; no new search path
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: hand-cranked 25-memory Build tap
  (retired by this change)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — construction still cannot produce a search
  hit without Canonical Recall
```

## Delivery record

- **Files/layers:** `MeaningIndexDrainSession` + `MeaningIndexDrainBudget`;
  `work/MeaningIndexWorker` + scheduler + decision mapper + observation;
  `AiPackDisclosureViewModel` enqueues/observes; About Stop; Hilt bind;
  `ClearMemoraDerivedData` cancels the tag; `RunPendingMeaningIndex` kdoc
  names the worker as drain owner. Count cap unchanged.
- **Automated verification:** `MeaningIndexDrainBudgetTest`,
  `MeaningIndexDrainSessionTest`, `MeaningIndexWorkDecisionMapperTest`,
  `MeaningIndexWorkObservationTest`, `AiPackDisclosureCopyTest`. Full
  `:app:testDebugUnitTest` **786 tests, 0 failures** (2026-09-14).
- **Emulator/manual:** pending founder device rebuild before filming. Path:
  Welcome → About → Build meaning index (one tap) → Stop → leave About and
  return → clear index. Empty-queue phones will complete in one short job.
- **Failure/recovery:** engine missing → StopAndReport, canned copy; selection
  disagreement → StopAndReport, mismatch copy; unexpected exception → retry;
  Stop → Stopped copy with remaining; clear-index cancels the tag.
- **Known limitation:** I4 measured per-item cost remains open; 4 minutes is a
  safety margin under WorkManager’s ~10 minute ceiling, not AVAILABLE.
  Progress is per completed 25-memory batch, not per PDF page inside the
  batch (`RunPendingMeaningIndex` still emits phase progress; this worker
  does not surface it). No foreground notification — the drain may pause if
  Android kills the process; tap Build again. I15/I31 (Find while indexing)
  deferred. Do not film paraphrase queries.
- **Git commit:** pending local checkpoint (not pushed).
