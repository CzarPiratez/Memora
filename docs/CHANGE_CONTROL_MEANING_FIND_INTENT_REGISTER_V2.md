# Change control: Meaning Find scenario bar v2 — Intent Register

**Date:** 2026-09-04  
**Type:** Docs only (holistic scenario planning)  
**Bar:** `docs/MEANING_FIND_PRODUCT_SCENARIO_BAR.md` (this change **is** the bar)  
**Decision guardrails:** Live/Dual N = 0. No Find code. No AVAILABLE. No Grounded
Answers. No ADR-052 UI.

## Pre-work record

- **Requirement IDs:** ENGINEERING_CHARTER holistic scenario planning; Product
  Contract retrieval cues (person/place/object/time/purpose/topic); Local AI
  Spec §9 structured filters; MIG-07B filter stage vs relative-time product;
  Grounded Answers blocked until readiness.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, PRODUCT_CONTRACT, LOCAL_AI_TECHNICAL_SPEC §9,
  ARCHITECTURAL_MIGRATION_SPEC MIG-03/MIG-07B, GROUNDED_ANSWERS_AMENDMENT_V1,
  GROUNDING_ARCHITECTURE §14, MVP_EXIT_AUDIT, MEANING_FIND bar v1, MF-1 change
  control; current `RecallQueryConstraintClassifier` / `MeaningRecallCue`.
- **Current-code evidence inspected:** TIME advisory is phrase-contains; no
  `yesterday`; no multi-cue split; MF-1 wrappers incomplete (`show me`).
- **Privacy:** Docs only.
- **Smallest safe change:** Expand the bar into an Intent Register with surface
  ownership (Find vs GA vs Act), I1–I34 / G* / ACT1, golden wrappers and
  list-vs-same-file rules, MIG-07B honesty, delivery slices MF-1.1 → MF-TIME.
- **Acceptance criteria:**
  - [x] v1 U1–U8 preserved
  - [x] Relative time owned by Find (I8), not Grounded Answers
  - [x] Multi-file list (I3) vs same-file (I2) vs hedge (I4) specified
  - [x] Answer-seeking misroute registered (I21) without authorizing Ask code
  - [x] CONTINUE / changelog / MVP audit honesty row updated
  - [ ] Founder review of register (this checkpoint)

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning path (ADR-049) — docs bar only
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: Intent Register → later MF-1.1+ / MF-TIME inside Canonical Recall
WHY THIS CONVERGES: Product scenarios owned before more ranking code
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: screenshot-only U1–U8 as the ceiling
EXTENDS LEGACY? no
```

## Delivery record

- **Files:** `MEANING_FIND_PRODUCT_SCENARIO_BAR.md` v2; this change control;
  CONTINUE; CHANGELOG; MVP_EXIT_AUDIT NL-intent row; RECALL_ENFORCEMENT_INDEX link.
- **Code:** none.
- **Git commit:** When requested
