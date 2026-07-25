# Change Control: Keyword Why Document Label Provenance

**Date:** 2026-07-26
**Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path only; CONTINUE recall polish; no
AI/network/PDF reopen. Label already on the hit card from saved Room metadata.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  keyword search/explain/cap change-control.
- **Current-code evidence inspected:** `whyThisResultBody` cites query/page/excerpt
  only; `PdfKeywordSearchHit.label` shown on card but unused in Why; multi-doc
  “Showing N matches” makes document provenance in Why useful.
- **Open ADRs / limitations:** Not full Memory Explain / E-05 integrity states;
  label is displayName/title fallback, not a reopened file path.
- **Dependencies added:** None.
- **Privacy:** Uses already-persisted hit label only; originals unchanged.
- **Smallest safe change:** Pass hit.label into Why copy; unit tests; user confirm.
- **Acceptance criteria:** Why cites the same document label as the card plus
  query/page/excerpt; still keyword-not-meaning; no AI/confidence.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchCopy.whyThisResultBody` now requires
  `documentLabel`; Results Why call passes `hit.label`; copy unit tests; this
  change-control; CONTINUE; CHANGELOG.
- **Automated verification and result:** On 2026-07-26,
  `PdfKeywordSearchCopyTest` passed; `:app:installDebug` OK.
- **Emulator/manual verification and result:** On 2026-07-26 user confirmed on
  Medium Phone: Why this result? cites `memora-persist-fixture.pdf` and
  `memora-real-source-fixture.pdf` respectively for meet mira.
- **Failure/recovery paths verified:** Blank document label rejected by unit test.
- **Known limitation or follow-up:** Label may be generic “PDF document” when
  display name/title were blank at discovery; still interim keyword path.
- **Documentation/traceability/ADR updates:** CONTINUE checkpoint; CHANGELOG.
- **Git commit:** _(pending)_
