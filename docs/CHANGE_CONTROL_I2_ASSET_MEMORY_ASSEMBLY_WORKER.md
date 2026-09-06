# Change control: I2 — Asset Memory assembly worker + Stop

**Date:** 2026-09-07  
**Type:** Application + WorkManager + UI (Asset Memory **construction**; not Find)  
**Closes:** PROGRAM_STATE Batch I / I2  
**Does not authorize:** I3 meaning-index worker, I4 time budget, AVAILABLE, a second
assembler or Find path

## Pre-work record

- **Requirement IDs:** P-11 (invest intelligence at index time); P-14 (recoverable,
  bounded work); P-17 (UI → application → domain). PROGRAM_STATE I2.
- **Source documents read:** ENGINEERING_CHARTER (holistic scenarios before
  implement), GOVERNANCE pre-work gate, CONTINUE, PRODUCT_SOURCE_REGISTRY,
  LOCAL_AI_TECHNICAL_SPEC (index locally; originals stay put),
  RECALL_ENFORCEMENT_INDEX (Live/Dual **N = 0**), LEGACY_RECALL_SURFACE,
  PROGRAM_STATE_AND_SEQUENCE_V1 §8b Batch I, CHANGE_CONTROL_TEMPLATE,
  CHANGE_CONTROL_D9_ASSEMBLY_DRAIN_CURSOR (FailedSafely must not Continue),
  CHANGE_CONTROL_I1 / I1b (I3 stays a later sibling; I2 is this drain).
- **Current-code evidence inspected:** `RunPendingAssetMemoryAssembly` already
  returns `hasMore` and skips terminal per-asset outcomes (D-9).
  `AssetMemorySetupViewModel.onBuildRequested` called that use case once per tap
  (batch of 25) with no Stop and no auto-continue. Existing drains use
  `Worker` + `Scheduler` + `DecisionMapper` (`MediaStorePhotoOcrExtract*`,
  `SafPdfExtract*`). UI observe + Stop is already on
  `PdfLocalReadingViewModel`. Clear-index cancels by tag.
- **Open ADRs / platform:** no new ADR. No schema change. One global unique
  work name (`asset-memory-assembly-drain`) — assembly is not per-`SourceId`.
- **Privacy:** construction only over already-saved deterministic facts. No
  originals reopened by the worker. WorkManager output is counts, not excerpts.
- **Smallest safe change:** worker calls only `RunPendingAssetMemoryAssembly`.
  Welcome Build enqueues; the card observes unique work and offers Stop.
  `FailedSafely` maps to `Result.retry()`, never Continue.
- **Acceptance criteria:**
  - [x] Decision mapper: `Completed(hasMore=true)` → Continue;
        `Completed(hasMore=false)` → CompletedDrain;
        `FailedSafely` → RetryableFailure (not Continue)
  - [x] Observation: cancelled chain is Ready, not Failed; succeeded batches sum
  - [x] Double-tap Build while Building does not enqueue again
  - [x] Returning to Welcome reconnects if unique work is still running
  - [x] Derived-data clear cancels the assembly tag
  - [x] Copy has Stop + honest progress; no “available now”
  - [x] Device (SM-A156E, 2026-09-07): Welcome showed 1001 READY memories.
        Tap **Build memories from saved facts** started
        `AssetMemoryAssemblyWorker` (tag `asset-memory-assembly`).
        `WM-WorkerWrapper`: SUCCESS in ~40 ms; `reschedule = false`
        (empty queue / CompletedDrain). Welcome stayed Ready with the same
        1001 line; no Failed, no crash. Stop was not visible because
        Building lasted less than a frame.
- **Holistic scenarios (before implement):**
  - User: pending 0 → one short job, then readiness, not a hang
  - User: 40 pending → worker continues; card shows built-so-far; Stop
    leaves remaining as Continue
  - User: leaves Welcome while running → same ViewModel keeps observing;
    process death reconnects from unique work
  - User: Stop mid-chain → CANCELLED is Ready with totals so far, not Failed
  - User: double-tap Build → ignored
  - User: clear index while running → cancel + Ready(0)
  - Technical: `FailedSafely` retries with backoff (D-9); must not APPEND
    another job on the same bad batch
  - Technical: I3 must still call `RunPendingMeaningIndex`, not this worker
  - Edge: empty infos on first observe do not overwrite a live Building
    refresh; historical SUCCEEDED after process death is not shown as a
    fresh “Built N” until the user starts a drain
- **Alternatives considered:**
  - I3 first — rejected; this phone’s meaning queue is empty, so I3 would
    idle. Assembly is the remaining hand-crank.
  - Keep tap-to-batch and only add a loop in the ViewModel — rejected;
    process death would lose the drain; other pipelines already use WorkManager.
  - Continue on `FailedSafely` — rejected; that is the D-9 hot-loop.
  - Per-source unique work — rejected; pending select is global.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Asset Memory assembly construction (I2 worker)
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: same MemoryBuilder → Memory substrate; Canonical Recall stays
  the sole product Find
WHY THIS CHANGE CONVERGES: one drain owner (RunPendingAssetMemoryAssembly)
  that UI and WorkManager both call; no new search path
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: hand-cranked 25-memory Build tap
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — construction still cannot produce a search
  hit without Canonical Recall
```

## Delivery record

- **Files/layers:** `work/AssetMemoryAssemblyWorker` + scheduler + decision
  mapper + observation mapper; `AssetMemorySetupViewModel` enqueues/observes;
  Welcome card Stop; Hilt bind; `ClearMemoraDerivedData` cancels the tag;
  `RunPendingAssetMemoryAssembly` kdoc only.
- **Automated verification:** `AssetMemoryAssemblyWorkDecisionMapperTest`,
  `AssetMemoryAssemblyWorkObservationTest`, `AssetMemorySetupViewModelTest`,
  `AssetMemorySetupCopyTest`. Full `:app:testDebugUnitTest` **756 tests, 0
  failures** (2026-09-07).
- **Emulator/manual:** `:app:installDebug` on SM-A156E (`RZCX12KZ6EN`).
  After unlock: Welcome → Build. Worker SUCCESS ~40 ms, no continuation.
  Card returned to “1001 current evidence-backed Asset Memories are saved.”
  No crash. Stop not observable on an empty queue.
- **Failure/recovery:** `FailedSafely` → retry; Stop → Ready; clear-index
  cancels work; empty queue completes the drain.
- **Known limitation:** I3/I4 remain open. Batch cap stays 25 memories
  (I4 is the time budget). This phone’s assembly queue may already be empty.
- **Git commit:** `93cfd2b` (local; not pushed).
