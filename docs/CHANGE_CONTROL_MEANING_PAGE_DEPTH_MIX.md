# Change control — Meaning page named-word depth mix (D-23)

**Date:** 2026-09-18
**Type:** Authorized ranking slice (shown page occupancy)
**Status:** **Landed in tree 2026-09-18.** Keyword Find frozen. Pool 60 and
page cap 20 unchanged. Device re-test of
`when are the swimming classes` open; `passport` /
`swimming timetable` must not regress.
**Ask Model:** **P-AND vs P-LIST**; **P-EVIDENCE**. Coverage is a boost and
a banner, not exclusive occupancy of the shown 20.
**Does not authorize:** type quotas, PDF seats, synonym nets
(`classes` ≠ `timetable`), FTS5, encoder swap, hybrid/RRF, ANN, one-box
Find, keyword Find changes, schema migration, raising the pool of 60,
growing the shown page, cosine-only pool seating, marketing AVAILABLE

## Why this exists

D-22 live trace on founder A15 (2026-09-18 23:44):

```
q=when are the swimming classes tokens=swimming,classes scanned=3030
floor=3030 collapse=1603 admitted=60 poolTruncated=true
admittedTopCosine=0.840 tier=Partial droppedByTier=0 dropped=[]
shown=20 shownTopCosine=0.840 capTruncated=true gold=Grade-2-Swimming-TT-2026.pdf
goldType=PDF collapseRank=689 admittedRank=28 rankedRank=21 shownRank=-
goldCosine=0.675 diagnosis=in_pool_off_page
```

Token seating **worked** (`admittedRank=28`). D-21 **worked**
(`droppedByTier=0`). USE found the file (`collapseRank=689`, not thousands).
The 0.22 raw-cosine band still includes it (0.840 − 0.675 = 0.165). The
shown cap of 20 then took the first 20 of boosted-then-cosine order.
Token boost saturates at 1.0, so those 20 were the closer 2-token class
photos. The PDF was **21st**. Growing the page to 21 would be a dump, not
ranking.

## Scope

### Authorized code (this Agent ticket)

| Step | What | Must not |
|---|---|---|
| **D-23 page mix** | When the close band exceeds the cap **and** the cue has two or more named words **and** more than one named-token depth is present, occupy the 20 by round-robin across depths (deepest leads each round; incoming order inside a depth). One-word cues and a single depth stay as today. | PDF / type seats; raising cap 20; raising pool 60; encoder; FTS; synonym; keyword Find |

### Out

- Mixed-depth **pool seating** (`out_of_pool`) — not this diagnosis
- I3 grouped piles
- Encoder / FTS / hybrid / one-box
- Specimen 1b–1d quotas (already reversed)

## Frozen (keyword Find)

Same freeze as D-20 / D-21 / D-22.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning shown-page occupancy.
  Candidate pool seating (D-15) unchanged. Keyword Find unchanged.
CURRENT LEGACY PATH (L# or none): none — Live/Dual N = 0
TARGET PATH: SearchAssetMemoriesByMeaning (pool 60)
  → AnchorAwareMeaningRecallRanking (D-21 keep)
  → MeaningTrustedHitPolicy band 0.22 + MeaningNamedWordDepthPage mix
  → cap 20
WHY THIS CONVERGES: ranking inside the sole Find boundary so an admitted
  1-token neighbour in the close band can appear; no new generator
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none (keyword Find stays)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** D-22 `diagnosis=in_pool_off_page`; Ask Model P-LIST
  (named-word neighbours already retrieved must not be hidden by dump
  occupancy); scenario bar: retrieve the artefact from NL without type
  quotas.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (N = 0),
  HUMAN_RECALL_ASK_MODEL, MEANING_FIND_PRODUCT_SCENARIO_BAR,
  CHANGE_CONTROL_TEMPLATE, CHANGE_CONTROL_MEANING_GOLD_IN_POOL,
  CHANGE_CONTROL_MEANING_PARTIAL_FAMILIES,
  CHANGE_CONTROL_MEANING_RANKING_LEXICAL_VETO.
- **Current-code evidence inspected:** `MeaningTrustedHitPolicy.page` filters
  0.22 then `take(20)` in incoming (boosted, stable cosine) order;
  `MeaningEvidenceTokenBoost` +0.35 coerce 1.0; D-15 pool seating kept the
  PDF at admitted 28; D-21 droppedByTier=0.
- **Open ADRs:** none new. Not Act. Not AVAILABLE. Not FC-02 complete.
- **Privacy:** no new logging.
- **Smallest safe change:** mix named-word depths on the capped page only.
- **Acceptance criteria:**
  - [x] 20 two-token class files + in-band 1-token timetable → timetable
        on the shown 20; deepest still leads
  - [x] `passport` (one word) capped page order unchanged
  - [x] Same-depth Exact list does not reorder by asset type
  - [x] Far cosine still drops; thin band still not padded
  - [ ] Founder re-test `when are the swimming classes` (`diagnosis=on_page`
        or `shownRank` set); `passport` / `swimming timetable` still
        look like themselves
- **Holistic scenarios:**
  - User: meaning `when are the swimming classes` — timetable PDF can
    appear among class photos; banner still Partial; not a claim that
    `classes` means `timetable`
  - User: `passport` / `swimming timetable` — must not become a mixed dump
  - Anti: PDF seats; page 21; encoder swap this ticket
  - Technical: Live/Dual N = 0; no `connectedDebugAndroidTest`

## Alternatives considered

- **Raise the page to 21 / 40** — rejected. Cap is the contract; corpus
  must improve ranking, not dump.
- **PDF type seats** — rejected. Specimen. User already refused.
- **Encoder swap because collapseRank=689** — rejected. File is in the 60
  and in the band. D-20 thousands gate not met.
- **Pool seating mix** — rejected. `admittedRank=28`; seating is not the leak.
- **Per-token boost so 2-token ranks higher** — rejected. Would make the
  occupancy bug worse.
- **50/50 reserved Partial seats (D-20 1b)** — rejected. Depth mix is
  type-agnostic and only runs when the cap truncates a multi-depth band.

## Delivery record — named-word depth page mix (2026-09-18)

- **Files/layers:** `MeaningNamedWordDepthPage`; `MeaningTrustedHitPolicy.page`
  orders in-band hits before `take(20)`; `CanonicalRecall.trimMeaningMatches`
  passes the query. Keyword Find unchanged. Pool 60 / cap 20 / D-21 keep /
  D-15 seating unchanged.
- **Automated verification:** `MeaningNamedWordDepthPageTest`;
  `MeaningTrustedHitPolicyTest.mixed_named_word_depths_share_a_capped_page`;
  `one_word_cue_does_not_reorder_a_capped_page`;
  `CanonicalRecallMeaningTest.searchByMeaning_shows_a_shallower_named_word_neighbour_on_a_capped_page`;
  existing band / Exact-neighbour / type-order tests unchanged.
- **Emulator/manual:** founder 2026-09-19: still cannot see
  `Grade-2-Swimming-TT-2026.pdf` after D-23. Same-depth `{classes}` leak;
  occupancy key corrected in **D-24**.
- **Known limitation:** gold still needs to be in the 0.22 band. Files
  outside the 60 still cannot appear. Same-depth families were not mixed
  (D-24).
- **Git commit:** local; do not push.
