# Change Control: Cancel In-Flight Keyword Search

**Date:** 2026-07-28  
**Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.  
**Decision guardrails:** Interim keyword path; Keyword v1 exit item 3; no AI/network.

## Pre-work record

- **Requirement IDs:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Sources read:** AGENTS, governance, CONTINUE, KEYWORD_PDF_RECALL_V1_EXIT,
  clear-query change-control, current search ViewModel (generation supersede;
  Clear disabled while Searching).
- **Smallest safe change:** Explicit Cancel while Searching; bump search
  generation, cancel the search Job, return to Idle keeping the typed query;
  do not treat coroutine cancellation as SearchCouldNotFinish; keep Searching
  visible for a short minimum (~700ms) on tiny indexes so Cancel is tappable
  (does not invent progress; slow searches are not delayed further); keep Search
  and Cancel as two always-mounted buttons (Cancel enabled only while Searching)
  so a Cancel tap cannot re-fire as Search; only apply outcomes while phase is
  still Searching.
- **Acceptance:** During Searching, Cancel stops the spinner and returns Idle
  with the query still typed; a late search completion cannot show results;
  Search again / Clear / Why / Open still work afterward; Cancel remains
  visible long enough to tap on a small fixture index.

## Architecture layers

UI → ViewModel only (no new application/data ports).

## Known limitation

Does not cancel Opening a PDF preview (separate open generation). Accessibility
baseline remains Keyword v1 exit item 4.

## Acceptance record

- **Unit:** Cancel returns Idle and ignores late completion; minimum Searching
  hold; `PdfKeywordSearchCopyTest` Cancel label — passed.
- **Emulator (2026-07-28):** User confirmed pass on Medium Phone — dedicated
  Cancel search stops Searching; query kept; no results; Search/Why/Open still
  work afterward.
