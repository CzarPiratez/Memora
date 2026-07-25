# Change Control: Persist Searchable PDF Text from Local Reading Start

**Date:** 2026-07-25
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrails:** ADR-017, ADR-020, ADR-022.

## Pre-work record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** `AGENTS.md`, product registry, Local AI spec,
  governance, CONTINUE, product contract, architecture, decisions
  (ADR-017/020/022), roadmap, traceability, persistence contract/design,
  write-path budget, visible recovery plan, local-reading status UI change
  control, change-control template.
- **Current-code evidence inspected:** `IsolatedPdfParserClient` discarded wire
  text via `toClientSummary`; mapper/prepare/persist/Room port existed unbound;
  `RunPdfLocalReadingStatusCheck` was status-only.
- **Open ADRs / limitations:** No WorkManager, AI, or network. Ordinary client may
  retain validated text only after session assembly acceptance. Budgets and
  superseded retention already enforced in Room port.
- **Privacy impact:** One indexed PDF from the user-approved folder; derived text
  stored only in encrypted Memora Room; originals unchanged; nothing leaves device.
- **Smallest safe change:** Retain validated wire result on completed client
  sessions; on Local PDF reading Start, map → prepare → atomic Room persist;
  truthful Completed copy; no WM/AI/network.
- **Acceptance criteria:**
  1. Successful Start persists complete/no-text extraction for the first indexed
     PDF when within budgets.
  2. Password/access/stale/cancel/budget/reject → no saved-for-search claim;
     existing recovery states.
  3. Completed copy states text was saved for search on this phone when persist
     succeeds; never when it fails.
  4. Emulator proof that Room holds page text after approved-tree fixture parse.

## Delivery record

- **Files/layers changed:** `IsolatedPdfParserClient` retains validated wire;
  `PersistValidatedPdfLocalReading`; `RunPdfLocalReadingStatusCheck` persist path;
  Local PDF reading copy; targeted `PdfExtractionDao.deleteHeader`; persist
  androidTest; CONTINUE/CHANGELOG/ADR-020 notes/this record.
- **Automated verification and result:** unit local-reading passed; Medium Phone
  `PersistValidatedPdfLocalReadingIntegrationTest` **1 of 1**;
  `IsolatedPdfParserClientIntegrationTest` **10 of 10**; `:app:installDebug` OK.
- **Emulator/manual verification and result:** APK installed; user confirmed
  Start → Completed saved-for-search wording on Medium Phone (2026-07-25).
- **Failure/recovery paths verified:** missing validated wire / persist failure →
  RetryableProblem (no saved claim); password/access broker paths unchanged.
- **Known limitation or follow-up:** Still one PDF per Start; no search UI;
  WorkManager not scheduled.
- **Documentation/traceability/ADR updates:** CONTINUE next step after confirmation;
  CHANGELOG; ADR-020 production persist note.
- **Git commit:** 0832c70
