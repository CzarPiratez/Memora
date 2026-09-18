# Change control — Meaning live gold-in-pool measure (D-22)

**Date:** 2026-09-18
**Type:** Authorized measure slice (debug live path only)
**Status:** **Measure landed 2026-09-18.** Device line recorded
(`diagnosis=in_pool_off_page`). Ranking among the 60 is **D-23**. Keyword
Find frozen.
**Ask Model:** **P-EVIDENCE**; measure before the next ranking or seating
change. Honesty banners may remain; they must not delete neighbours.
**Does not authorize:** pool seating change, ranking change, type quotas,
PDF seats, synonym nets (`classes` ≠ `timetable`), FTS5, encoder swap,
hybrid/RRF, ANN, one-box Find, keyword Find changes, schema migration,
raising the pool of 60, growing the shown page, marketing AVAILABLE

## Why this exists

D-21 keeps every named-word family **already in the admitted 60**. Founder
device after D-21: `when are the swimming classes` is Partial, `droppedByTier=0`,
shown 20, mostly class/classes/timetable **images and screenshots**, one note,
**no PDF**. Keyword PDF Find `swimming timetable` still returns the correct
PDFs.

That is **not** two products. Keyword and meaning are two **candidate
generators** inside Canonical Recall. The next code change is either seating
or ranking — **not both**, and not until this measure says which.

Two remaining leaks, and they need different patches:

1. **Out of the 60.** Token-count seating (D-15) admits deeper named-token
   hits first. A timetable PDF that has `swimming` but not `classes` loses
   seats to 60 two-word class photos. Ranking cannot recover it.
2. **In the 60, off the page.** The PDF is admitted; trusted trim / cosine
   order still hide it under the cap of 20.

Phase 0 counts cannot tell those apart. The encoder probe used a diagnostic
pool of 30 and is not the live product path (pool 60 → rank → page 20).

## Scope

### In this record

- Defect **D-22**: live gold membership on `MeaningSearchTrace`.
- Same gold needles and **label/key** match as `MeaningEncoderProbeCues`
  (not a synonym table; not OCR-body gold).
- Diagnosis strings that name the next authorized ticket.

### Authorized code (this Agent ticket; do not combine)

| Step | What | Must not |
|---|---|---|
| **D-22 measure** | Debug Logcat on `CanonicalRecall.searchByMeaning`: best matching gold filename (short), asset type, 1-based `collapseRank` / `admittedRank` / `rankedRank` / `shownRank`, collapse cosine, `diagnosis`. Reuse probe cues. No ranking policy. | Seating change; ranking change; PDF seats; raising pool 60; page cap 20; keyword Find; encoder; FTS; one-box |

### Out (not authorized here)

- Mixed-depth pool seating (next **only if** `diagnosis=out_of_pool`)
- Ranking among the 60 without type quotas (next **only if**
  `diagnosis=in_pool_off_page`)
- I3 grouped piles, encoder, FTS, hybrid, one-box Find
- Treating keyword vs meaning as the user-facing solution

## Diagnosis → next (after founder device line)

Registered cue for this defect: `when are the swimming classes`. Gold
needles stay `timetable` / `swimming-tt` / `swimming_tt` / `swimming tt` on
**display label or source asset key**, same as the encoder probe.

| `diagnosis` | Meaning | Next ticket |
|---|---|---|
| `out_of_pool` | Gold survived collapse; not in the admitted 60 | Mixed-depth seating so a 1-token swimming neighbour can still enter when 2-token class photos fill the board. **Not** PDF seats. **Not** a bigger dump. New CC. |
| `in_pool_off_page` | Gold is in the 60 and survived the lexical tier; not on the shown page | Rank among those 60 without type quotas. New CC. |
| `dropped_by_tier` | Gold was admitted; lexical/anchor drop removed it | Inspect D-21 keep; do not start seating. |
| `on_page` | Gold filename is already in the shown 20 | Matcher vs what the person is looking at; not seating. |
| `not_in_collapse` | No label/key gold after the 0.05 floor | Index/encoder path. Still not FTS unless gold chunk rank is thousands (D-20 gate). |
| `no_gold_cue` | Query is not in `MeaningEncoderProbeCues` | Type the registered cue. |

Do **not** run `connectedDebugAndroidTest` (wipes the live DB).

## Frozen (keyword Find)

Same freeze as D-20 / D-21: `SearchMemoryEvidence`, four keyword ViewModels,
`CanonicalRecall.invoke`, Room schema, MemoryBuilder. Meaning live debug
trace only.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning live debug trace only.
  Candidate generation, ranking, and keyword Find unchanged.
CURRENT LEGACY PATH (L# or none): none — Live/Dual N = 0
TARGET PATH: SearchAssetMemoriesByMeaning (pool 60, D-15 seating unchanged)
  → AnchorAwareMeaningRecallRanking (D-21 keep unchanged)
  → MeaningTrustedHitPolicy (raw cosine, up to 20)
  → MeaningSearchTrace gold ranks (debug)
WHY THIS CONVERGES: the next seating or ranking change is chosen from live
  membership, not from a specimen or a second product Find
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none (keyword Find stays)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** Ask Model P-EVIDENCE; D-20 “separate measure from
  ranking”; D-21 known limitation (families outside the 60 cannot appear);
  scenario bar: NL cue must still retrieve the artefact, without synonym
  nets or type quotas.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE,
  PRODUCT_SOURCE_REGISTRY, CONTINUE, RECALL_ENFORCEMENT_INDEX,
  LEGACY_RECALL_SURFACE (N = 0), HUMAN_RECALL_ASK_MODEL,
  MEANING_FIND_PRODUCT_SCENARIO_BAR, CHANGE_CONTROL_TEMPLATE,
  CHANGE_CONTROL_MEANING_RANKING_LEXICAL_VETO,
  CHANGE_CONTROL_MEANING_PARTIAL_FAMILIES, PROGRAM_STATE D-20/D-21.
- **Current-code evidence inspected:** `MeaningSearchTrace` Phase 0 fields
  (no gold ranks); `selectCandidatePool` token-count-first `take(60)`;
  D-21 keep; `MeaningEncoderProbeCues` swimming needles; live path
  `CanonicalRecall.searchByMeaning` pool 60 then page 20.
- **Open ADRs:** none new. Not Act. Not AVAILABLE. Not FC-02 complete.
- **Privacy:** debug Logcat only (Ask Model R8). Short filename + type +
  ranks. No excerpts, no Room, no upload. Asset identity used for matching
  is not written to the log line.
- **Smallest safe change:** gold membership on the existing debug line.
  Do not touch seating or ranking in this commit.
- **Acceptance criteria:**
  - [x] Registered cue `when are the swimming classes` can report
        `out_of_pool` vs `in_pool_off_page` vs `on_page` in unit tests
  - [x] Unregistered cue (`scan silky`) is `no_gold_cue`; `classes` is not
        treated as `timetable`
  - [x] About hint names gold ranks; “Does not change Find”
  - [x] Founder debug search pastes the `gold=` / `diagnosis=` fields
- **Holistic scenarios (before implement):**
  - User: meaning `when are the swimming classes` — one Logcat line says
    whether the timetable PDF is out of the 60 or in the 60 but off the
    page
  - User: keyword PDF `swimming timetable` — unchanged
  - User: `passport` / `swimming timetable` / `scan silky` — ranking
    unchanged; extra gold fields only when the query is a registered cue
  - Anti: PDF seats; synonym net; raising the dump; asking the person to
    pick keyword vs meaning as the product
  - Technical: Live/Dual N = 0; `connectedDebugAndroidTest` that `clearAll`
    the live DB is forbidden
  - Edge: empty content tokens; gold on source key not only label; no
    registered cue; several gold filenames — best cosine after collapse
    wins, same as the probe

## Alternatives considered

- **Change seating in this commit because the hypothesis is strong** —
  rejected. D-20 forbids combining measure and ranking. A wrong seating
  patch would hide whether D-21 already left the PDF in the 60.
- **PDF type seats** — rejected. Specimen drift. User already refused.
- **Tell the user to use keyword Find for this cue** — rejected as the
  product answer. Generators stay distinct until a later one-box CC.
- **Raise the pool so rank-380 gold can enter** — rejected as *this*
  slice. Measure first; seating mix of depths is the candidate if
  `out_of_pool`.
- **Match gold on OCR/summary haystack** — rejected. Probe gold is
  label/key. Matching body text would count a photo of a timetable as the
  gold PDF.

## Delivery record — live gold membership (2026-09-18)

- **Files/layers:** `MeaningSearchGoldLocator`; `MeaningSearchTrace` gold
  fields; `SearchAssetMemoriesByMeaning.withPool` passes collapse hits;
  About hint. Keyword Find unchanged. Pool 60 / page 20 / D-21 keep
  unchanged.
- **Automated verification:** `MeaningSearchGoldLocatorTest`;
  `MeaningSearchTraceTest` out-of-pool and in-pool-off-page;
  `CanonicalRecallMeaningTest.searchByMeaning_records_gold_membership_for_a_registered_cue`;
  About copy test for gold ranks.
- **Emulator/manual:** founder A15 2026-09-18 23:44
  `when are the swimming classes`:
  `gold=Grade-2-Swimming-TT-2026.pdf goldType=PDF collapseRank=689
  admittedRank=28 rankedRank=21 shownRank=- goldCosine=0.675
  diagnosis=in_pool_off_page`. Seating is not the leak. Ranking among the
  60 is **D-23**. Do **not** run `connectedDebugAndroidTest`.
- **Known limitation:** gold is label/key substring, not excerpt body.
  Unregistered cues log `diagnosis=no_gold_cue`.
- **Git commit:** local; do not push.
