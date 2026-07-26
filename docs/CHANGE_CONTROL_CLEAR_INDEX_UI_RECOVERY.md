# Change Control: Clear-Index UI Recovery (PDF Index + Local Reading)

**Date:** 2026-07-27
**Requirements:** P-04, P-05, P-14, P-15, P-17; A-02, A-06.
**Decision guardrails:** ADR-021 clear derived data; grants survive; no AI/network.

## Pre-work record

- **Requirement IDs:** P-04, P-05, P-14, P-15, P-17; A-02, A-06.
- **Source documents read:** CONTINUE, governance, clear-derived change history,
  DocumentTree/PdfLocalReading ViewModels, WorkManager schedulers, PersistenceModule.
- **Current-code evidence inspected:** After clear+reconnect, DocumentTree observes
  stale finished unique work and maps to COMPLETED (hides Index). Local reading
  Completed UI still shows Stop. Clear ack does not reset PdfLocalReadingViewModel.
  Long-lived extract use cases inject captured MemoraDatabase (stale after clear).
- **Dependencies added:** None.
- **Privacy:** Clear still Memora-owned DB only; cancels Memora WM tags only.
- **Smallest safe change:** Ignore finished WM unless indexing in progress; cancel
  Memora work tags on clear; Completed→Done; reset local-reading VM on clear;
  resolve Room via MemoraDatabaseHandle in extract persist path.
- **Acceptance criteria:** After clear+reconnect, Index this folder returns; after
  reading finishes, Done (not Stop); clear resets local-reading session; search/
  extract use live DB.

## Delivery record

- **Files/layers changed:** DocumentTreeSetupViewModel ignores stale finished WM
  unless IN_PROGRESS; ClearMemoraDerivedData cancels Memora WM tags; Completed
  local reading shows Done; PdfLocalReadingViewModel.onDerivedDataCleared;
  extract/search resolve live MemoraDatabaseHandle; related unit/androidTest
  updates; this change-control; CONTINUE; CHANGELOG. Companion empty-corpus
  search honesty + hang recovery in keyword search files.
- **Automated verification and result:** On 2026-07-27, DocumentTreeSetupViewModelTest,
  PdfLocalReadingSessionTest, and search unit tests passed; `:app:installDebug` OK.
- **Emulator/manual verification and result:** On 2026-07-27 user confirmed on
  Medium Phone: after clear+reconnect Index this folder returns; local reading
  finished shows Done; keyword search works (no endless spinner).
- **Failure/recovery paths verified:** Stale finished discovery ignored until
  explicit index; clear cancels discovery/extract/MediaStore tags; extract persist
  uses live DB.
- **Known limitation or follow-up:** Empty-corpus vs no-match copy remains available
  for dedicated empty-index search check if needed later.
- **Documentation/traceability/ADR updates:** CONTINUE checkpoint; CHANGELOG.
- **Git commit:** _(pending)_
