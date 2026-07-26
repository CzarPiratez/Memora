# Change Control: Keyword Empty-Corpus vs No-Match Honesty

**Date:** 2026-07-26
**Requirements:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
**Decision guardrails:** Interim keyword path; CONTINUE recall polish; no
AI/network/PDF reopen. Only current-fingerprint saved page text counts.

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, product contract, architecture, decisions, roadmap, traceability,
  query-coherence change-control (follow-up noted).
- **Current-code evidence inspected:** Empty `Matches` always maps to NoMatches
  copy that says words did not match; no existence check for searchable pages.
- **Open ADRs / limitations:** Discovery alone is not searchable; Local PDF reading
  must have saved text. Clear derived data can create empty corpus for manual test.
- **Dependencies added:** None.
- **Privacy:** EXISTS/count over Memora-owned Room rows only; no reopen.
- **Smallest safe change:** DAO existence for current searchable pages; distinct
  NothingSaved outcome/phase/copy; unit tests; user confirm both paths.
- **Acceptance criteria:** Empty corpus ≠ “no match for words”; true miss still
  names the query; hits path unchanged; coherence preserved.

## Delivery record

- **Files/layers changed:** `countCurrentSearchablePages`; NothingSavedToSearch
  outcome/phase/copy; SearchPersistedPdfPageText uses live DB handle; search
  failures map to SearchCouldNotFinish (no endless spinner); unit tests.
- **Automated verification and result:** On 2026-07-27, search unit tests passed
  with clear-index recovery suite.
- **Emulator/manual verification and result:** On 2026-07-27 user confirmed keyword
  search works after clear/rebuild (no endless spinner). Empty-corpus dedicated
  path covered by unit tests + NothingSaved copy.
- **Failure/recovery paths verified:** Closed-DB hang fixed via live handle;
  exception path leaves recoverable phase.
- **Known limitation or follow-up:** No document inventory counts in UI.
- **Documentation/traceability/ADR updates:** CONTINUE/CHANGELOG with clear-index
  recovery checkpoint.
- **Git commit:** `ad3aa54`
