# Change Control: Keyword Search Accessibility Baseline

**Date:** 2026-07-28  
**Requirements:** P-01, P-14, P-15, P-17; A-02, A-05; Local-AI §11 accessible states.  
**Decision guardrails:** Keyword v1 exit item 4; interim keyword path; no AI/network.

## Pre-work record

- **Requirement IDs:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Sources read:** AGENTS, governance, CONTINUE, KEYWORD_PDF_RECALL_V1_EXIT,
  Local-AI §11, current `PdfKeywordSearchScreen` / preview (only preview image had
  a content description; progress and phase changes were silent for TalkBack).
- **Smallest safe change:** Keyword search + original-page preview only —
  screen title as heading; polite live regions for Searching / results / empty /
  error / Opening statuses; merge progress + status so TalkBack hears one calm
  announcement; richer preview image content description; Clear keeps its visible
  label (already exposed to services).
- **Acceptance:** Sighted flow unchanged (Search / Clear / Cancel / Why / Open).
  TalkBack (or equivalent) can find the screen heading, hear Searching and result
  status changes, activate Cancel and Clear by name, and hear a page preview
  description that names the document and page.

## Architecture layers

UI only (`MainActivity` keyword screens + `PdfKeywordSearchCopy`).

## Known limitation

Not a full-app TalkBack audit. Does not add custom focus traversal beyond
document order. Does not claim WCAG certification.

## Acceptance record

- **Unit:** `PdfKeywordSearchCopyTest` preview image description — passed.
- **Emulator (2026-07-28):** User confirmed all pass on Medium Phone for sighted
  flow — Find saved PDF text looks normal; Search / Why / Open / Cancel / Clear;
  preview Back to results.
- **Git:** `afdfb6b`.
