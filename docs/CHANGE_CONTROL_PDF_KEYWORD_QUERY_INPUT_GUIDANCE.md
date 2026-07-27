# Change Control: Blank-query Guidance + Seamless Search Button

**Date:** 2026-07-27
**Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path; CONTINUE recall polish; no
AI/network/PDF reopen. This change must not invent meaning-based recall.

## Pre-work record

- **Requirement IDs:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec,
  governance, CONTINUE, product contract, architecture, decisions,
  PRD traceability (keyword recall slices).
- **Current-code evidence inspected:** `PdfKeywordSearchScreen` always enables
  the Search button when not actively searching, even when the typed query is
  blank. Idle phase renders no inline guidance when query is blank.
- **Open ADRs / limitations:** Keyword path only; must keep “not meaning-based
  recall yet” framing.
- **Dependencies added:** None.
- **Smallest safe change:** UI-only polish:
  - Disable the “Search on this phone” button while the query is blank.
  - When phase is `Idle` and query is blank, show `PdfKeywordSearchCopy.EMPTY_QUERY_BODY`.
- **Acceptance criteria:** With blank query: Search button disabled and inline
  guidance appears. With non-blank query: behavior unchanged. No new claims.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchScreen` disables Search while the
  query is blank; Idle + blank query shows `EMPTY_QUERY_BODY` inline guidance.
- **Automated verification and result:** On 2026-07-27, `com.memora.app.ui.search.*`
  unit tests passed via `:app:testDebugUnitTest --tests com.memora.app.ui.search.*`.
- **Emulator/manual verification and result:** On 2026-07-27 the user confirmed
  on Medium Phone: blank query → Search disabled + guidance; non-blank query →
  Search enabled and search/Why unchanged.
- **Known limitation or follow-up:** None expected.
- **Documentation/traceability/ADR updates:** This change-control; CONTINUE;
  CHANGELOG.
- **Git commit:** `36e96df`

