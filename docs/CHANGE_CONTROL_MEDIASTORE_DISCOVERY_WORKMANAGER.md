# Change Control: WorkManager MediaStore Discovery Drain

**Date:** 2026-07-25
**Requirements:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrails:** Phase 1 WorkManager parity with SAF PDF discovery; no OCR/AI.

## Pre-work record

- **Requirement IDs:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  SAF PDF discovery WM change-control.
- **Current-code evidence inspected:** `IndexMediaStoreImages` one page + checkpoints;
  `MediaStoreSetupViewModel` foreground one-shot; SAF `SafPdfDiscoveryWorker` pattern.
- **Open ADRs:** No OCR/extract/AI/network. Selected-photos scope must stay truthful.
- **Dependencies added:** None new (reuse WorkManager + Hilt Worker already present).
- **Privacy:** Photo/screenshot metadata placeholders only; no image bytes; no cloud.
- **Smallest safe change:** Explicit Start indexing enqueues unique MediaStore discovery
  work that drains pages via existing indexer/checkpoints until done or access fails.
- **Acceptance criteria:** Drain without per-page taps; checkpoint resume; access stop
  with recovery UI; never opens image bytes; unit + WorkManager androidTest; honest
  full-vs-selected copy.

## Delivery record

- **Files/layers changed:** MediaStoreDiscoveryWorker/scheduler/mapper; ViewModel
  enqueue + WorkInfo observe; IndexingSummary honest copy; Hilt bind; androidTest.
- **Automated verification and result:** Unit mapper/summary/ViewModel tests passed.
  Medium Phone MediaStoreDiscoveryWorkerAndroidTest **2 of 2** (drain, access-stop).
  Debug APK installed.
- **Emulator/manual verification and result:** On 2026-07-25 user confirmed on
  Medium Phone: indexed 0 items from permitted photo library; catalogue up to date;
  metadata-only honesty (no contents/memories claimed).
- **Failure/recovery paths verified:** Access-stopped fails unique work; UI maps to
  ACCESS_REVOKED. Other failures map to retryable Failed state.
- **Known limitation or follow-up:** OCR / image extract / semantic Memory remain later.
- **Documentation/traceability/ADR updates:** CONTINUE, CHANGELOG, ROADMAP Phase 1,
  this record.
- **Git commit:** (set after commit)
