# Change control: PDF folder local-reading progress counts

## Pre-work record

- **Requirement IDs:** Spec §11 unbounded-work / progress honesty; enterprise
  completion §C follow-up (WorkManager PDF folder extract progress).
- **Source documents read:** GOVERNANCE, CONTINUE, meaning-index progress/cap CC,
  enterprise checklist, SafPdfExtractWorker, PdfLocalReading ViewModel/Copy.
- **Current-code evidence inspected:** Local PDF reading drain is one PDF per
  WorkManager unit with APPEND continuation; UI InProgress shows static body +
  Pause/Stop only — no “N of M” or drained count. Folder indexing already has
  Continue when `hasMore`; meaning index now has live progress.
- **Open ADRs / limitations:** Counts are aggregate only (no filenames/URIs).
  Pending total is snapshot at drain start (new PDFs indexed mid-drain may not
  inflate the denominator). No AVAILABLE flip.
- **Privacy:** Progress strings use integers only.
- **Smallest safe change:** Count pending PDFs at drain start; surface drained /
  pending progress from WorkInfo successes; completed copy includes drained
  count; unit tests; docs.
- **Acceptance criteria:**
  1. In-progress UI shows progress such as “Reading PDF 2 of 5…”.
  2. Completed copy includes how many PDFs this drain finished (or honest empty).
  3. Unit tests green; no AVAILABLE claim change.

## Delivery record

- **Status:** Accepted (engineering verification)
- **Date:** 2026-08-04
- **Delivered:** `countPdfPendingLocalReading`; worker `KEY_UNIT_FINISHED`;
  `PdfLocalReadingUiState` progress feedback + completed drained count; unit tests.
- **Verification:** Targeted `:app:testDebugUnitTest` green
- **Truthfulness:** Aggregate counts only; not marketing AVAILABLE
- **Git commit:** (filled at close)
