# Change Control: WorkManager SAF PDF Extract Drain

**Date:** 2026-07-25
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrails:** ADR-017 isolation; ADR-020/022 persistence; discovery WM
pattern; no AI/network.

## Pre-work record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions (ADR-017/020/022), roadmap,
  traceability, discovery WM change-control, local-reading persist change-control.
- **Current-code evidence inspected:** Discovery `SafPdfDiscoveryWorker` drain;
  `RunPdfLocalReadingStatusCheck` opens first PDF only via
  `findFirstBySourceAndType`; `PersistValidatedPdfLocalReading` + broker + isolated
  client already persist searchable text on explicit Start.
- **Open ADRs / limitations:** Extract must use descriptor broker + isolated
  parser only (ADR-017). No auto-start from discovery complete. No AI/network.
- **Dependencies added:** None new (reuse WorkManager + Hilt Worker already present).
- **Privacy:** Read-only descriptors for user-approved folder PDFs; derived text only
  in encrypted Room; originals unchanged; nothing leaves the device.
- **Smallest safe change:** Explicit Local PDF reading Start enqueues unique extract
  work per source; one pending PDF per `doWork`; continue while pending remain;
  reuse persist path; honest multi-PDF progress copy.
- **Acceptance criteria:**
  1. One Start tap drains pending PDFs (no current extraction for asset fingerprint)
     until none remain or access fails—without per-PDF Start taps.
  2. Worker never parses in-process; uses broker + isolated service + persist.
  3. Password-protected PDF skips to next pending; access revoke stops and asks reconnect.
  4. Unit mapper tests; focused WorkManager androidTest; emulator user confirmation.

## Delivery record

- **Files/layers changed:** Pending-PDF selector; RunPendingPdfLocalReading;
  extract worker/scheduler/mapper; Local PDF reading ViewModel enqueue + copy;
  Hilt bind for PendingPdfLocalReader.
- **Automated verification and result:** Unit mapper/copy passed; Medium Phone
  SafPdfExtractWorkerAndroidTest and RoomAssetRepositoryTest pending selector
  passed; debug APK installed.
- **Emulator/manual verification and result:** On 2026-07-25 user confirmed on
  Medium Phone: Local PDF reading Completed multi-PDF saved-for-search copy.
- **Failure/recovery paths verified:** Access-stopped fails unique work; password
  skip continues; retryable maps to Result.retry.
- **Known limitation or follow-up:** MediaStore extract WM and auto-start after
  discovery remain out of scope.
- **Documentation/traceability/ADR updates:** CONTINUE, CHANGELOG, this record.
- **Git commit:** Pending after user confirmation.
