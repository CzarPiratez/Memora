# Change Control: Keyword Search Submitted-Query Coherence

**Date:** 2026-07-26
**Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path; CONTINUE recall polish; ADR-019
Explain honesty. No AI/network/PDF reopen.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  prior keyword recall change-controls.
- **Current-code evidence inspected:** After Results/NoMatches/EmptyQuery, editing
  the field keeps stale phase/Why while `phase.query` can disagree with the text
  field; concurrent `onSearch` launches are not generation-guarded.
- **Open ADRs / limitations:** Still interim keyword path; not semantic Memory.
- **Dependencies added:** None.
- **Privacy:** Same Room-only search; no new data access.
- **Smallest safe change:** Clear terminal/searching phases on query edit; name
  submitted query in results summary; ignore superseded search completions;
  ViewModel + copy unit tests; user confirm.
- **Acceptance criteria:** Edit after results clears hits/Why; summary says Results
  for "…"; Why matches submitted query; stale in-flight search cannot overwrite;
  no AI/network.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchViewModel` clears phase on query edit
  and ignores superseded in-flight searches; `NoMatches` carries submitted query;
  `resultsSummary` / `noMatchesBody` name the query; UI wiring; ViewModel + copy
  unit tests; this change-control; CONTINUE; CHANGELOG.
- **Automated verification and result:** On 2026-07-26,
  `com.memora.app.ui.search.*` unit tests passed; `:app:installDebug` OK.
- **Emulator/manual verification and result:** On 2026-07-26 user confirmed on
  Medium Phone: Results for "meet mira" / Why cites meet mira; after re-search,
  Results for "meet" / Why cites meet (not meet mira).
- **Failure/recovery paths verified:** Unit tests cover clear-on-edit, blank query,
  no-matches with query, and superseded in-flight ignore.
- **Known limitation or follow-up:** Empty-corpus vs no-match distinction still later.
- **Documentation/traceability/ADR updates:** CONTINUE checkpoint; CHANGELOG.
- **Git commit:** _(pending)_
