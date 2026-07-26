# Change Control: Keyword Search Clear Invalidation

**Date:** 2026-07-27
**Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** ADR-021 clear derived data; CONTINUE recall polish; no
AI/network/PDF reopen.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** CONTINUE, governance, clear-index recovery
  change-control, keyword search ViewModel/MainActivity clear-ack path.
- **Current-code evidence inspected:** Clear ack resets MediaStore, DocumentTree,
  PdfLocalReading; `PdfKeywordSearchViewModel` is not reset, so Results/Why can
  cite deleted excerpts after clear.
- **Dependencies added:** None.
- **Privacy:** Clears presentation of Memora-owned search evidence only.
- **Smallest safe change:** `onDerivedDataCleared` → Idle (invalidate in-flight);
  wire clear-ack; ViewModel unit test; user confirm.
- **Acceptance criteria:** After clear ack, no Results/Why; empty corpus search
  shows Nothing saved; rebuild+search still works.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchViewModel.onDerivedDataCleared`
  (Idle + bump `searchGeneration`); `MainActivity` clear-ack wires search VM;
  unit tests for results→clear and in-flight supersede.
- **Automated verification and result:** On 2026-07-27,
  `PdfKeywordSearchViewModelTest` (incl. clear cases) passed;
  `:app:installDebug` on Medium Phone API 17 succeeded.
- **Emulator/manual verification and result:** On 2026-07-27 the user confirmed
  on Medium Phone: search with results/Why, Clear Memora index → Find saved PDF
  text shows no stale cards/Why; rebuild+search still works.
- **Failure/recovery paths verified:** Clear after Results → Idle; clear during
  Searching → late Matches ignored (unit); manual clear with Why open.
- **Known limitation or follow-up:** Query field may retain typed text; phase must
  not keep cleared hits.
- **Documentation/traceability/ADR updates:** This change-control; CONTINUE;
  CHANGELOG.
- **Git commit:** _(filled after commit)_
