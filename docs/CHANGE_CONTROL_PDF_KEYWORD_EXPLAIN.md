# Change Control: Keyword Search Explain Mode (Stored Evidence)

**Date:** 2026-07-25
**Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-01, A-02, A-05.
**Decision guardrails:** Interim keyword path only; no AI/Memory/confidence; ADR-022
current fingerprint already enforced by search SQL.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-14, P-15, P-17; A-01, A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  keyword-search change-control.
- **Current-code evidence inspected:** `PdfKeywordSearchHit` has page + excerpt;
  results UI shows them without a Why affordance; query lives on
  `PdfKeywordSearchOutcome.Matches` but Results phase dropped it.
- **Open ADRs / limitations:** Not full Memory Explain Mode. No calibrated confidence.
  No PDF reopen / AI / network.
- **Dependencies added:** None.
- **Privacy:** Explains only from already-saved Room page text + user query; originals
  unchanged; nothing leaves the device.
- **Smallest safe change:** Per-hit Why this result? citing query, page, excerpt, and
  keyword match factor with honest non-meaning copy.
- **Acceptance criteria:** Why cites stored page + excerpt + keyword match on phone;
  no meaning/AI/confidence claims; unit copy tests; emulator user confirm.

## Delivery record

- **Files/layers changed:** PdfKeywordSearchCopy why helpers; Results(query,hits);
  PdfKeywordSearchScreen Why this result? expand; copy unit tests; docs.
- **Automated verification and result:** PdfKeywordSearchCopyTest; debug APK
  install.
- **Emulator/manual verification and result:** On 2026-07-25 user confirmed on
  Medium Phone: Why this result? cites Page 1 + stored excerpt for meet mira.
- **Failure/recovery paths verified:** N/A beyond existing blank/no-match search.
- **Known limitation or follow-up:** Full semantic Memory Explain Mode remains later.
- **Documentation/traceability/ADR updates:** CONTINUE, CHANGELOG, this record.
- **Git commit:** Pending after user confirmation.
