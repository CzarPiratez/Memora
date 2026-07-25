# Change Control: WorkManager SAF PDF Discovery Drain

**Date:** 2026-07-25
**Requirements:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrails:** Phase 1 WorkManager; ADR-017 extract deferred.

## Pre-work record

- **Requirement IDs:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  change-control template.
- **Current-code evidence inspected:** `IndexSafPdfFolder` one page; no WorkManager
  dependency; `DocumentTreeSetupViewModel` foreground-only; checkpoints persist.
- **Open ADRs:** Extract WM deferred (ADR-017). No AI/network/notes.
- **Dependencies added:** `androidx.work:work-runtime-ktx` (bounded discovery
  batches), `androidx.hilt:hilt-work` (inject indexer into worker),
  `androidx.work:work-testing` (instrumentation). No network/AI SDKs.
- **Privacy:** Discovery metadata only; no PDF bytes; no cloud.
- **Smallest safe change:** One Index tap enqueues unique discovery work that drains
  pages via existing indexer/checkpoints until done or access fails.
- **Acceptance criteria:** Drain without per-page taps; checkpoint resume; revoke
  stops work; never opens PDF/extract; unit + WorkManager androidTest; honest copy.

## Delivery record

- **Files/layers changed:** WorkManager + Hilt Worker deps; MemoraApplication
  Configuration.Provider; SafPdfDiscoveryWorker / scheduler / outcome mapper;
  ViewModel enqueue + WorkInfo observation; honest PDF-folder metadata copy;
  countBySourceAndType; WorkManager androidTest.
- **Automated verification and result:** Unit mapper + summary + ViewModel tests
  passed. Medium Phone SafPdfDiscoveryWorkerAndroidTest **3 of 3** (drain,
  resume/re-enqueue, access-stopped). Debug APK installed.
- **Emulator/manual verification and result:** On 2026-07-25 user confirmed on
  Medium Phone: indexed 2 PDF items; PDF list up to date; copy states text search
  still needs Local PDF reading (not claiming all PDFs searchable from discovery).
- **Failure/recovery paths verified:** Access-stopped fails unique work; UI maps to
  reconnect. Provider failure maps to retryable Failed indexing state.
- **Known limitation or follow-up:** Extract WorkManager remains a later slice.
  Discovery does not open PDF bytes or save searchable text.
- **Documentation/traceability/ADR updates:** CONTINUE, CHANGELOG, ROADMAP Phase 1
  note, ADR-020 Phase-1 discovery-WM note, PRD_TRACEABILITY P-14/P-17/A-06.
- **Git commit:** Pending explicit user request (local checkpoint not created yet).
