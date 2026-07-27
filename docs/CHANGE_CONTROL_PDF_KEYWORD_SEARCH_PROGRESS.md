# Change Control: Keyword Search IME Gate + Searching Progress

**Date:** 2026-07-27
**Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path; CONTINUE recall polish; no
AI/network/PDF reopen. Completes blank-query gating without shortcuts.

## Pre-work record

- **Requirement IDs:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  blank-query guidance change-control.
- **Current-code evidence inspected:** Search button disables when query is blank,
  but `keyboardActions(onSearch)` still calls `onSearch()` unconditionally;
  Searching phase shows only a spinner with no progress copy.
- **Open ADRs / limitations:** Keyword path only; not meaning-based Memory recall.
- **Dependencies added:** None.
- **Privacy:** Presentation-only; no new data access.
- **Smallest safe change:** Gate keyboard Search the same as the button; show
  honest Searching progress copy + spinner; keep field read-only while searching;
  copy unit tests; user confirm.
- **Acceptance criteria:** Blank query cannot start search via keyboard; while
  searching, calm progress copy appears and input cannot change mid-flight;
  results/Why unchanged.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchUiState.canSubmitSearch`; Search
  button + keyboard IME share that gate; query field disabled while Searching;
  `SEARCHING_BODY` + spinner; copy/ViewModel unit tests.
- **Automated verification and result:** On 2026-07-27, `com.memora.app.ui.search.*`
  unit tests passed; `:app:installDebug` on Medium Phone succeeded.
- **Emulator/manual verification and result:** On 2026-07-27 the user confirmed
  on Medium Phone: blank keyboard Search does not start a search; Searching shows
  spinner + progress copy with locked field; results/Why still work.
- **Failure/recovery paths verified:** Blank query cannot submit (button + IME);
  Searching clears canSubmitSearch until completion.
- **Known limitation or follow-up:** None expected beyond interim keyword path.
- **Documentation/traceability/ADR updates:** This change-control; CONTINUE;
  CHANGELOG.
- **Git commit:** `57feb43`
