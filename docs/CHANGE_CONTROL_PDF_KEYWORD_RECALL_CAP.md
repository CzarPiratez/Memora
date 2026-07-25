# Change Control: Keyword Recall Match-Cap Honesty

**Date:** 2026-07-25
**Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path only; ADR-024 keyword disclosure;
CONTINUE recall polish (default non-AI fork). No AI/network/PDF reopen.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  keyword search/explain change-control, compatibility/fallback policy.
- **Current-code evidence inspected:** `PdfKeywordSearchSupport.MAX_RESULTS = 20`;
  Results UI shows static RESULTS_HINT with no count or cap disclosure; Why path
  unchanged and user-confirmed.
- **Open ADRs / limitations:** Not semantic Memory recall; E-05 integrity states
  still later; no AI Pack implementation.
- **Dependencies added:** None.
- **Privacy:** Same Room-only keyword path; no new data access.
- **Smallest safe change:** Results summary with match count and honest “at most
  20 / more may exist” when the limit is reached; unit copy tests; user confirms
  on emulator.
- **Acceptance criteria:** Results show how many matches are listed; when 20 hits
  return, copy states the cap and that more pages may match; no meaning/AI claims;
  Why this result? unchanged in behavior.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchOutcome.Matches.limitReached`;
  `PdfKeywordSearchPhase.Results.limitReached`; `PdfKeywordSearchCopy.resultsSummary`;
  Results UI uses summary; copy unit tests; this change-control; CONTINUE; CHANGELOG.
- **Automated verification and result:** On 2026-07-25/26,
  `PdfKeywordSearchCopyTest` passed; `:app:installDebug` OK.
- **Emulator/manual verification and result:** On 2026-07-26 user confirmed on
  Medium Phone: Find saved PDF text → meet mira → “Showing 2 matches.” for two
  fixture PDFs; Why this result? still cites page + excerpt + keyword-not-meaning.
- **Failure/recovery paths verified:** Under-cap count honesty verified; full 20-cap
  disclosure covered by unit tests (user corpus had only 2 hits).
- **Known limitation or follow-up:** Still interim keyword path; does not query a
  total match count beyond the listed page; semantic Memory recall remains later.
- **Documentation/traceability/ADR updates:** CONTINUE checkpoint; CHANGELOG.
- **Git commit:** `5117c3e`
