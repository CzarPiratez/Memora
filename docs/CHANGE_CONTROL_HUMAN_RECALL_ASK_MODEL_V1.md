# Change control: Human Recall Ask Model v1

**Date:** 2026-09-04  
**Type:** Docs only (holistic natural-recall language)  
**Authority:** `docs/HUMAN_RECALL_ASK_MODEL.md`  
**Decision guardrails:** Live/Dual N = 0. No Find code. No AVAILABLE. No
Grounded Answers runtime. No Vision / PERSON fabrication.

## Pre-work record

- **Requirement IDs:** ENGINEERING_CHARTER holistic scenarios; Product Contract
  retrieval (person/place/object/time/purpose/topic); Local AI §9; Grounding
  Architecture (Find as rail); MIG-03 TIME/TOPIC only; I22 typos; I8/I38 time;
  I11 person/place.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  PRODUCT_CONTRACT, MEANING_FIND bar, GROUNDED_ANSWERS_AMENDMENT,
  GROUNDING_ARCHITECTURE, MIG-03/07B honesty, RecallQueryContentTokens /
  classifier (current-code: bag-of-tokens + thin TIME; no fuzzy; no PERSON).
- **Privacy:** Docs only. Kinship/photo fixture must not imply face recognition
  without later ADR.
- **Smallest safe change:** Ceiling model so NL Find is not discovered in
  implementation. Binding fixture for photo + daughter + pool + last Saturday +
  typo. Waves W0–W5 so lists (W1) are not mistaken for that fixture.
- **Acceptance criteria:**
  - [x] Dimensions D1–D15, shape S1–S18, jobs J1–J17
  - [x] P-TYPO / P-TIME / P-VISUAL / P-AND vs list / P-ANSWER
  - [x] Fixture layered honesty (today cannot satisfy intended meaning)
  - [x] Ask impact: retrieval is the ceiling
  - [x] CONTINUE / changelog / bar / charter / recall index pointers
  - [x] **v1.1** D16 device/corpus scope (I10 / I34 now map to a dimension)
  - [x] **v1.1** §5b result and system classes R1–R8 (I16, I19, I27, I28, I29,
        I31, I32, I46 now map to a class)
  - [x] **v1.1** one-box decision registered (§2 + wave W1.5; I6 maps here)
  - [x] **v1.1** §11 anti-case must be a test, not a sentence
  - [ ] Founder acceptance of the model **and the v1.1 amendment** (this checkpoint)

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall — docs model only
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: Ask Model → waves W1+ inside Canonical Recall; Ask after retrieval
WHY THIS CONVERGES: One language model for Find and later Grounded Answers
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: screenshot-driven NL patches as the plan
EXTENDS LEGACY? no
```

## Delivery record

- **Files:** `HUMAN_RECALL_ASK_MODEL.md`; this change control; CONTINUE;
  CHANGELOG; MEANING_FIND bar pointer; ENGINEERING_CHARTER first application;
  RECALL_ENFORCEMENT_INDEX.
- **v1.1 amendment (2026-09-04):** `HUMAN_RECALL_ASK_MODEL.md` §2 one-box
  decision, §3 D16, §5b R1–R8, §10 W1.5, §11 anti-case-as-test. Evidence for
  why: `PROGRAM_STATE_AND_SEQUENCE_V1.md` §3.
- **Code:** none.
- **Git commit:** When requested
