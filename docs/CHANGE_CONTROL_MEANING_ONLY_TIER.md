# Change control — Meaning-only tier for zero-overlap paraphrase

**Date:** 2026-09-15
**Type:** Canonical Recall meaning ranking (precision tier)
**Status:** Delivered in tree; founder device gate open
**Ask Model:** **P-MEANING-ONLY** (added this slice); **P-EVIDENCE**;
forbidden synonym net

## Why this exists

D-12 made lexical precision a tier so `swimming schedule` can reach a PDF
that says `swimming timetable`, and say that `schedule` was not found. D-15
made admission follow coverage depth so that partial answer can even enter
the pool.

When **no** named word appears in any stored excerpt, ranking still emptied
the list (`applyLexicalPrecisionTier`, `deepest == 0`). The on-device model
had already ranked a neighbour. Embeddings were again ordering results they
were not allowed to find — one level down from D-12. The film note
`pool timetable` is usually **Partial** (the file says `timetable`); the
remaining hole is true zero overlap: `kids water lessons` against that same
PDF.

## Scope

- **In:** `RecallPrecision.MeaningOnly`. When deepest coverage is 0, keep a
  short high-cosine band if the cue named **at least two** content words and
  the neighbour clears `MIN_COSINE` 0.32, gap 0.12, cap 3.
- **In:** banner copy that leads with the miss and says these files are
  closest by meaning, not because they contain those words.
- **Out:** synonym nets, one-word miss → neighbours, new Find path, CE/FC-02
  product wire, AVAILABLE, keyword Find.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning ranking (precision tier)
CURRENT LEGACY PATH (L# or none): none
TARGET PATH: SearchAssetMemoriesByMeaning (cosine candidates) →
  AnchorAwareMeaningRecallRanking (Exact / Partial / MeaningOnly) →
  MeaningTrustedHitPolicy → Meaning Find UI
WHY THIS CONVERGES: third precision tier inside the sole Find boundary; no
  new generator, no new ranker, no new hit type
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** P-16 (honest Find copy); Ask Model D1 / J1 /
  P-MEANING-ONLY; PROGRAM_STATE meaning-only tier.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (N = 0),
  HUMAN_RECALL_ASK_MODEL, MEANING_FIND_PRODUCT_SCENARIO_BAR,
  PROGRAM_STATE_AND_SEQUENCE_V1 D-12 / D-15, CHANGE_CONTROL_TEMPLATE.
- **Current-code evidence inspected:** `applyLexicalPrecisionTier` emptied
  on `deepest == 0`; `RecallPrecision` was Exact | Partial;
  `MeaningTrustedHitPolicy`; `MeaningSearchCopy.noMatchesBody`;
  `SearchAssetMemoriesByMeaning.selectCandidatePool` already admits cosine
  neighbours for zero overlap — ranking was the veto.
- **Open ADRs:** none new. Not Act. Not AVAILABLE.
- **Privacy:** on-device only; no new network.
- **Smallest safe change:** one policy object + one sealed variant + banner
  + tests. Cosine floor is an explicit constant, device-tunable.
- **Acceptance criteria:**
  - [x] `kids water lessons` against a swimming-timetable excerpt at ≥ 0.32
        is MeaningOnly, not empty, not Partial
  - [x] `pool timetable` against that excerpt is Partial (`timetable`)
  - [x] one-word `silky` with zero overlap stays empty even at high cosine
  - [x] weak cosine neighbour stays empty
  - [x] Exact still wins over Partial; Partial still wins over MeaningOnly
  - [x] copy does not claim a synonym or AVAILABLE
  - [ ] Device: `kids water lessons` and `pool timetable` on the A15 against
        the swimming-timetable PDF
- **Holistic scenarios (before implement):**
  - User: remembers the idea, not the wording (`kids water lessons`) — sees
    the timetable PDF under an honest miss banner
  - User: `pool timetable` when the file says timetable — Partial, missing
    `pool`
  - User: `silky` and nothing has silky — empty, not fashion-adjacent junk
  - User: two-word cue, only unrelated weak neighbours — empty
  - User: Why on a meaning-only card — Found by meaning; does not say the
    cue words appear
  - Anti: `schedule` is never asserted to mean `timetable`
  - Anti: `silky` ↛ `smooth`
  - Technical: candidate gen still cosine-only (L7); ranking still inside
    Canonical Recall
  - Edge: TIME/TOPIC anchor filter that empties the list still resets
    precision to Exact (existing)

## Alternatives considered

- **Show every cosine neighbour when deepest is 0** — rejected. Existing
  synthetic 0.9 “Bus rules” would become a hit. Prefer silence below a floor.
- **Synonym / WordNet expansion** — forbidden by Ask Model and by D-12
  (“this is not a synonym net”).
- **One-word meaning-only** — rejected. A missed `silky` is a keyword-shaped
  miss, not a paraphrase.
- **Raise candidate-gen `MIN_CANDIDATE_SCORE` instead** — rejected. That
  would starve Partial/Exact admission of weak-cosine exact-word hits (D-11).

## Delivery record

- **Files/layers:** `RecallPrecision.MeaningOnly`;
  `MeaningOnlyRecallPolicy`; `AnchorAwareMeaningRecallRanking`;
  `MeaningSearchCopy.meaningOnlyBody`; `MeaningSearchScreen` banner;
  `MeaningSearchOutcome.Matches` invariant; Ask Model P-MEANING-ONLY;
  PROGRAM_STATE D-15 note; tests.
- **Automated verification and result:** `:app:testDebugUnitTest` **842 tests,
  0 failures**.
- **Emulator/manual verification:** founder A15 — `kids water lessons` and
  `pool timetable` against the swimming-timetable PDF.
- **Failure/recovery:** below-floor → existing NoMatches copy. Engine
  unavailable unchanged.
- **Known limitation:** USE cosine is uncalibrated. A strong unrelated
  neighbour above 0.32 can still appear, with honesty. Floor is
  device-tunable. Not AVAILABLE.
- **Git commit:** after unit suite.
