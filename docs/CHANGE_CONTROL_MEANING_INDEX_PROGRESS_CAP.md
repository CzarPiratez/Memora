# Change control: Meaning index progress + per-tap cap UI

## Pre-work record

- **Requirement IDs:** Spec §11 unbounded-work gate; enterprise completion §C
  (cap / progress UI); ADR-024 honesty.
- **Source documents read:** GOVERNANCE, CONTINUE, M4 plan, enterprise checklist,
  AiPackDisclosure ViewModel/Screen, IndexMemoryEmbeddings, MemoryRepository.
- **Current-code evidence inspected:** Build meaning index loads up to **50**
  summaries with a magic number, no live progress copy, no “remaining / tap
  again” honesty when more READY memories exist; busy state only shows a
  spinner; Back sits at top of a long scroll (user friction on smoke).
- **Open ADRs / limitations:** Cap applies per tap (like Asset Memory build),
  not a global library limit. WorkManager PDF extract progress counts remain a
  follow-up. No AVAILABLE flip.
- **Privacy:** Progress/feedback are aggregate counts only — no URIs or summary
  text in logs/UI beyond existing index result pattern.
- **Smallest safe change:** Named batch limit; count READY summaries; progress
  feedback while indexing; remaining-tap copy; bottom Back on disclosure
  screen; unit tests; docs.
- **Acceptance criteria:**
  1. Per-tap memory embed cap is a named constant (≤25 for snappy UX).
  2. Busy UI shows progress text (e.g. memories N of M / pages…).
  3. When more READY memories remain, feedback tells user to tap Build again.
  4. Disclosure copy states the per-tap cap.
  5. Unit tests green; no AVAILABLE claim change.

## Delivery record

- **Status:** Accepted (engineering verification)
- **Date:** 2026-08-04
- **Delivered:**
  - `MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP = 25`
  - Optional progress callbacks on `IndexMemoryEmbeddings` /
    `IndexPdfPageEmbeddings`
  - `AiPackDisclosureViewModel` uses READY count + batch remaining hint
  - Live progress copy + bottom Back on `AiPackDisclosureScreen`
  - Honest INDEX_BATCH disclosure copy
- **Verification:** `:app:testDebugUnitTest` (targeted + related) green
- **Truthfulness:** Candidate meaning path unchanged; not marketing AVAILABLE
- **Git commit:** `5331baf`
- **Later (I1, 2026-09-06):** remaining work is counted **after** the batch by
  `RunPendingMeaningIndex`, not `pendingTotal - thisBatchSize` in the ViewModel.
  Cap, progress phases, and “tap Build again” copy stay. See
  `CHANGE_CONTROL_I1_MEANING_INDEX_DRAIN_USE_CASE`.
