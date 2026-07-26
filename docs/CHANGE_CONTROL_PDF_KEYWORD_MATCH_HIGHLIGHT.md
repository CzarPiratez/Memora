# Change Control: Keyword Excerpt Match Highlight

**Date:** 2026-07-27
**Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path; CONTINUE recall polish; no
AI/network/PDF reopen/SQL changes.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** CONTINUE, governance, Local AI spec (no AI), prior
  keyword recall change-controls.
- **Current-code evidence inspected:** Result cards show plain `hit.excerpt`;
  `excerptAroundMatch` already centers the first case-insensitive needle; Why
  path unchanged and user-confirmed.
- **Dependencies added:** None.
- **Privacy:** Highlights already-saved excerpt text only.
- **Smallest safe change:** Pure first-match span helper; AnnotatedString excerpt
  in Results cards; unit tests; user confirm.
- **Acceptance criteria:** Submitted query span is bold/primary in excerpt when
  present; missing span stays plain; Why unchanged; coherence preserved.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchSupport.firstMatchSpan`;
  `PdfKeywordSearchHighlight.annotatedExcerpt`; Results card excerpt uses
  AnnotatedString; support + highlight unit tests; this change-control;
  CONTINUE; CHANGELOG.
- **Automated verification and result:** On 2026-07-27,
  PdfKeywordSearchSupportTest + PdfKeywordSearchHighlightTest passed;
  `:app:installDebug` OK.
- **Emulator/manual verification and result:** On 2026-07-27 user confirmed on
  Medium Phone: search meet highlights meet in both fixture excerpts; Why still
  works.
- **Failure/recovery paths verified:** Absent span stays plain (unit-tested).
- **Known limitation or follow-up:** First occurrence only; no SQL/ranking change.
- **Documentation/traceability/ADR updates:** CONTINUE checkpoint; CHANGELOG.
- **Git commit:** _(pending)_
