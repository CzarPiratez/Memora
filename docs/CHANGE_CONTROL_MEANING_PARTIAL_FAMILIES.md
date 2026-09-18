# Change control — Meaning Partial families (D-21)

**Date:** 2026-09-18
**Type:** Decision plan; ranking code is a **later** Agent ticket
**Status:** **Delivered in tree 2026-09-18.** Ranking keep rule landed.
Keyword Find frozen. Device re-test of `wifi password` / `scan silky` open.
**Ask Model:** **P-AND vs P-LIST** (keep named-word neighbours already in the
admitted pool); **P-EVIDENCE**; honesty banners may remain; they must not
delete neighbours
**Does not authorize:** I3 grouped piles, synonym nets, FTS5, encoder swap,
hybrid/RRF, ANN, one-box Find, keyword Find changes, schema migration,
cosine-only pool seating, growing the shown page, type quotas, raising the
pool of 60, marketing AVAILABLE

## Why this exists

D-20 Phase 1 stopped Exact from **replacing** the list. That landed. Founder
device: `swimming timetable`, `swimming schedule`, `passport`, `Aadhaar`,
`silky` wrappers — good.

When **no** file in the admitted pool contains every named word, ranking
still subsets to **one** matched-word set: the first deepest hit’s tokens.
The other named word’s files, already in the 60, are deleted so one banner
can be uniform. That is a second lexical veto. Phase 0 measured it.

This is **not** I3 (grouped piles / `passport and id`). It is the same keep
rule Phase 1 already uses when Exact exists: every admitted hit that carries
**at least one** named word stays; coverage remains a boost and a banner.

This is **not** paraphrase and **not** an encoder ticket. `Kids water
classes` is Partial because `water` / `classes` exist. MeaningOnly never
runs. Gold at rank hundreds still cannot enter a 60-pool; D-21 does not
raise the pool.

## Founder Phase 0 traces (A15, 2026-09-18)

All four: `scanned=3030 floor=3030 collapse=1603 admitted=60
poolTruncated=true`. The 0.05 floor is not the leak.

| Cue | Tokens | Tier | droppedByTier | shown | Reading |
|---|---|---|---|---|---|
| `silky scan` | silky, scan | Partial | **0** | 18, cap truncated | Other family can miss the **60**, so the tier has nothing to drop |
| `wifi password` | wifi, password | Partial | **48** | 11 | One-word pile owns the page; 48 neighbours already in the 60 were deleted |
| `when are the swimming classes` | swimming, classes | Partial | **26** | 20, cap truncated | `classes` pile fills the page; first dropped labels were UNFYND screenshots; timetable not in the 8 printed names |
| `Kids water classes` | kids, water, classes | Partial | **58** | 2 | Not MeaningOnly. 58 of 60 stripped. `Spell.pdf` among dropped |

`droppedByTier=0` is not “nothing wrong.”

## Scope

### In this record (docs)

- Defect **D-21**, freeze, gates, what is **not** this slice.
- Hand-off from D-20: ranking sequence there is **stopped** (Phase 1 gate
  met for Exact-neighbour + exact-token). Residual is this ticket.

### Authorized next code (one Agent ticket; do not combine with encoder / I3)

| Step | What | Must not |
|---|---|---|
| **D-21 keep** | In `applyLexicalPrecisionTier`, when `deepest > 0`, keep every hit with a non-empty matching token set — **whether or not Exact exists**. Same keep as today’s `hasExact` branch. Update `refineAfterTrustedTrim` so a cosine-band page cannot re-subset to one word family. Banner stays Partial when any named word is missing; must not claim nothing saved says a word a remaining hit has (`exactHitsPresent` / mixed copy already exists). `droppedByTier` then counts only zero-overlap drops. | I3 grouped UI; synonym (`classes` = `timetable`); encoder; FTS; type quotas; pool 60; page cap 20; keyword Find; cosine pool seating; retuning `+0.35` / `0.22` unless a survival test cannot pass (then raw cosine only, as D-20) |

### Out (not authorized here)

- I3 grouped piles (`passport and id` as separate stacks)
- I4 hedge (`silk or silky`)
- Encoder / FTS / hybrid / ANN / schema
- Raising the candidate pool so rank-800 gold can enter
- Keyword LIKE/AND → OR
- Specimen seats (PDF / modifier-first / reserved Partial)

## Decision gates (after D-21 code)

Measure on the founder library. Keep exact-token in the set.

| Device result | Next |
|---|---|
| `wifi password` shows password **and** wifi-named files that were in the 60; banner names the miss; `passport` / `silky` / `swimming timetable` still look like themselves | Stop. Do not start I3 or encoder. |
| Other family still missing and Phase 0 said `droppedByTier=0` / `poolTruncated=true` | Pool seating — **not** this ticket; do not raise pool without a new CC |
| Exact-token dumps junk | Revert; do not ship |
| Banner says nothing saved says a word a remaining card has | Copy / `exactHitsPresent` bug; ranking keep is not done |

## Frozen (keyword Find)

Same freeze as D-20: `SearchMemoryEvidence`, four keyword ViewModels,
`CanonicalRecall.invoke`, Room schema, MemoryBuilder. Meaning ranking
objects + Partial banner copy only.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning ranking (lexical precision
  tier). Keyword candidate gen unchanged. Pool seating unchanged (D-15).
CURRENT LEGACY PATH (L# or none): none — Live/Dual N = 0
TARGET PATH: SearchAssetMemoriesByMeaning (pool 60) →
  AnchorAwareMeaningRecallRanking (any named-word hit kept; no one-family
  subset) → MeaningTrustedHitPolicy (raw cosine, up to 20) → Meaning Find UI
WHY THIS CONVERGES: removes a ranking veto inside the sole Find boundary;
  no new generator, no new product Find, no Live/Dual growth
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none (keyword Find stays)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** Ask Model P-AND vs P-LIST; P-EVIDENCE; D-20 Phase 1
  “coverage is a boost and a banner, not a subset” applied when Exact is
  absent; scenario bar I2 vs I3 (this slice is **not** I3).
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (N = 0),
  HUMAN_RECALL_ASK_MODEL, MEANING_FIND_PRODUCT_SCENARIO_BAR,
  CHANGE_CONTROL_TEMPLATE, CHANGE_CONTROL_MEANING_RANKING_LEXICAL_VETO,
  PROGRAM_STATE D-20.
- **Current-code evidence inspected:** `applyLexicalPrecisionTier` keeps all
  ≥1-token hits only when `hasExact`; else `scored.first { depth == deepest }`
  defines the sole token set. `refineAfterTrustedTrim` repeats that subset
  after the trusted page. Phase 0 `MeaningSearchTrace` on device (table
  above). `selectCandidatePool` token-count seating, pool 60.
- **Open ADRs:** none new. Not Act. Not AVAILABLE. Not FC-02 complete.
- **Privacy:** no new logging in the ranking ticket; Phase 0 debug Logcat
  already exists (R8: debug, not durable, not uploaded).
- **Smallest safe change (code ticket):** one keep rule + trim refine +
  invert the Phase 0 “one family dropped” tests; do not touch keyword or
  pool size.
- **Acceptance criteria (this docs slice):**
  - [x] D-20 stop gate recorded from founder traces
  - [x] D-21 keep rule named; I3 / encoder / FTS / quotas / pool raise out
  - [x] Ranking code delivered (later ticket)
- **Holistic scenarios (before implement):**
  - User: `wifi password` with no file that has both — password **and**
    wifi-named files that made the 60 can appear; banner says what is missing
  - User: `scan silky` / `silky scan` — if both families made the 60, both
    can appear; if `droppedByTier=0` and one family is absent, D-21 cannot
    invent it
  - User: `when are the swimming classes` — swimming-only **and**
    classes-only from the 60; not a claim that `classes` means `timetable`
  - User: `swimming timetable` / `passport` / `silky` — must not regress
  - User: `Kids water classes` — Partial on named words that exist; not
    MeaningOnly; timetable only if it was in the 60
  - Anti: grouped I3 UI; PDF seats; synonym net; bigger dump than 20
  - Technical: Live/Dual N = 0; `connectedDebugAndroidTest` that `clearAll`
    the live DB is forbidden
  - Edge: one-word miss stays empty; MeaningOnly only when deepest is 0

## Alternatives considered

- **Treat this as D-20 Phase 1e** — rejected. Phase 1 explicitly kept the
  deepest-set subset when Exact was absent so one banner stayed uniform.
  Changing that is a new contract.
- **I3 grouped piles this week** — rejected. Scenario bar I3 is OPEN;
  founder miss is “the other word’s files in the 60 were deleted,” not “I
  need two stacks.”
- **Encoder because `Kids water classes` is not a timetable** — rejected.
  Trace is Partial, not MeaningOnly. Probe already had `kids water lessons`
  at rank 841, in neither pool. Encoder stays behind D-20’s thousands gate.
- **Raise pool to 200** — rejected as *this* fix. `silky scan` with
  `droppedByTier=0` is a seating/reachability issue; mixing it into D-21
  would hide whether the keep rule worked.
- **Keep deepest ≥ 2 subset, only relax when deepest == 1** — rejected as
  the primary rule. Phase 1 already keeps shallower Partial beside Exact.
  A two-word-deep hit must not delete a one-word neighbour that still
  carries a word the person named. Coverage stays a boost.

## Delivery record (plan docs, 2026-09-18)

- **Files/layers:** this change control; `CONTINUE.md`; `PROGRAM_STATE`
  D-21; `CHANGELOG`; D-20 status closed at stop gate; recall index link.
- **Automated verification:** docs only.
- **Emulator/manual:** n/a this slice. Ranking code is the next ticket.
- **Known limitation:** files outside the token-seated 60 still cannot
  appear after D-21.
- **Git commit:** local; do not push.

## Delivery record — keep named-word families (2026-09-18)

- **Files/layers:** `applyLexicalPrecisionTier` and `refineAfterTrustedTrim`
  keep every ≥1-token hit; `RecallPrecision.Partial.mixedNamedWordFamilies`;
  mixed banner copy. Keyword Find unchanged. Pool 60 / page 20 unchanged.
- **Automated verification:**
  `apply_keeps_both_named_word_families_when_no_file_has_every_word`;
  `apply_keeps_a_shallower_named_word_neighbour_beside_a_deeper_partial`;
  `refineAfterTrustedTrim_does_not_drop_the_other_named_word_family`;
  `CanonicalRecallMeaningTest.searchByMeaning_records_pool_counts_and_partial_tier_drops`;
  D-20 Exact+Partial tests unchanged; MeaningOnly / zero-overlap tests
  unchanged; mixed-family copy test.
- **Emulator/manual:** founder debug meaning `wifi password`, `scan silky`,
  `when are the swimming classes`; confirm `passport` / `silky` /
  `swimming timetable` do not regress. Logcat `MeaningSearchTrace`
  `droppedByTier` should count zero-overlap only. Do **not** run
  `connectedDebugAndroidTest`.
- **Known limitation:** families that never entered the 60 still cannot
  appear. Not I3 groups. Not encoder.
- **Git commit:** local; do not push.
