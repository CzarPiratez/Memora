# Change control: Meaning Find MF-1.1 — ask-shape wrappers + light plural

**Date:** 2026-09-04  
**Type:** Domain + unit tests (Canonical Recall meaning path; shared content tokens)  
**Bar:** `docs/MEANING_FIND_PRODUCT_SCENARIO_BAR.md` I1 / P0, T5  
**Decision guardrails:** Live/Dual N = 0. No second Find path. No AVAILABLE.
No I3 list-split. No I8 calendar. No synonym net. No ADR-052 UI.

## Pre-work record

- **Requirement IDs:** Intent Register I1 golden wrappers; charter holistic bar;
  MF-1.1 slice.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, MEANING_FIND bar v2 (P0 freeze).
- **Current-code evidence inspected:** `show`/`me` were not wrappers, so
  `Show me the files with swimming timetables` required those tokens AND
  exact `timetables` (evidence often `timetable`).
- **Privacy:** On-device; no new network.
- **Smallest safe change:** Closed ask-shape catalog on
  `RecallQueryContentTokens`; `EnglishRecallInflection` whole-word plural
  match in lexical + boost. Not I3. Not Porter stemmer.
- **Acceptance criteria:**
  - [x] `Show me the files with swimming timetables` → tokens `swimming`,
    `timetables`; evidence with `timetable` passes lexical
  - [x] `show me the files` → no content tokens (I5, not a bluff cue)
  - [x] `silky` does not match `silk` via inflection
  - [x] U1/U2/U3 unit tests still hold
  - [ ] Device MF-3 on SM-A156E (`show me` + swimming timetable PDF)

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning path (ADR-049)
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: MeaningRecallCue / RecallQueryContentTokens → lexical ≥1 → trusted list
WHY THIS CONVERGES: I1 paraphrases inside sole Find boundary
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: treating show/me as content tokens
EXTENDS LEGACY? no
```

## Delivery record

- **Files:** `RecallQueryContentTokens`, `EnglishRecallInflection`, lexical
  filter, token boost; unit tests; bar P0/P2 + MF-1.1 status.
- **Automated verification:** JVM tests listed in CONTINUE.
- **Git commit:** When requested
