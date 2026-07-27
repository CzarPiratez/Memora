# Change Control: Keyword Search Corpus Readiness

**Date:** 2026-07-27
**Requirements:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path; CONTINUE recall polish; ADR-022
current-fingerprint only; no AI/network/PDF reopen.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  empty-corpus change-control (inventory follow-up).
- **Current-code evidence inspected:** Find saved PDF text has scope copy but no
  live page/document counts; empty corpus only revealed after Search; DAO already
  has `countCurrentSearchablePages`.
- **Open ADRs / limitations:** Still keyword-not-meaning; discovery alone is not
  searchable; Local PDF reading must have saved text.
- **Dependencies added:** None.
- **Privacy:** Counts Memora-owned current-fingerprint Room rows only; no reopen.
- **Smallest safe change:** Distinct document + page counts; readiness line on
  search screen; refresh on open and after clear; copy + unit tests; user confirm.
- **Acceptance criteria:** With saved pages, screen shows honest page/document
  counts before search; after clear, readiness shows nothing saved; search/Why
  unchanged; no meaning/AI claims.

## Delivery record

- **Files/layers changed:** DAO `countCurrentSearchableCorpus`;
  `LoadPersistedPdfKeywordSearchReadiness`; search ViewModel readiness state +
  refresh on open/clear; readiness copy + Find saved PDF text UI line; unit tests.
- **Automated verification and result:** On 2026-07-27, `com.memora.app.ui.search.*`
  unit tests passed; `:app:installDebug` on Medium Phone API 17 succeeded.
- **Emulator/manual verification and result:** On 2026-07-27 the user confirmed
  on Medium Phone: empty index shows nothing saved; after indexing + Local PDF
  reading, Find saved PDF text shows honest saved page/document counts; search
  and Why still work; clear returns readiness to empty.
- **Failure/recovery paths verified (automated):** Readiness load failure →
  CouldNotLoad; clear refreshes empty inventory; screen-visible refresh.
- **Known limitation or follow-up:** Counts are current-fingerprint keyword corpus
  only; not Memory inventory.
- **Documentation/traceability/ADR updates:** This change-control; CONTINUE;
  CHANGELOG.
- **Git commit:** _(filled after commit)_
