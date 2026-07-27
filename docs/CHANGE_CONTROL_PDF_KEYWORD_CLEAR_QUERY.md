# Change Control: Keyword Search Clear Query

**Date:** 2026-07-28  
**Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.  
**Decision guardrails:** Interim keyword path; Keyword v1 exit item 2; no AI/network.

## Pre-work record

- **Requirement IDs:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Sources read:** AGENTS, governance, CONTINUE, KEYWORD_PDF_RECALL_V1_EXIT,
  current search ViewModel/UI (clear already drops Results but keeps typed query).
- **Smallest safe change:** Explicit Clear control when query is non-blank (blocked
  while Searching/Opening); `onQueryCleared` resets query + Idle + open state;
  `onDerivedDataCleared` also clears typed query so a rebuilt index cannot leave
  a stale phrase in the field.
- **Acceptance:** Clear empties the field and returns Idle/empty-query guidance;
  after Clear Memora index + Done, Find saved PDF text shows a blank query;
  Search/Why/Open unchanged otherwise.

## Architecture layers

UI → ViewModel only (no new application/data ports).

## Known limitation

Does not cancel an in-flight search (v1 exit item 3). Clear is disabled while
Searching or Opening so state stays coherent.

## Acceptance record

- **Unit:** `PdfKeywordSearchViewModelTest` clear-query / clear-on-index-clear
  paths; `PdfKeywordSearchCopyTest` Clear label — passed.
- **Emulator (2026-07-28):** User confirmed all pass on Medium Phone — Clear
  empties field + results; Clear resets after Why/Open; Clear Memora index blanks
  typed query; Search/Why/Open still work after Clear (not after index wipe).
