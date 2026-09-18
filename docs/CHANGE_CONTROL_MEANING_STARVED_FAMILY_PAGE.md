# Change control — Meaning starved-family page lead (D-25)

**Date:** 2026-09-19
**Type:** Authorized ranking correction
**Status:** **Landed in tree 2026-09-19.** Keyword Find frozen. Cap 20 /
pool 60 unchanged.
**Ask Model:** **P-LIST**. When no file has every named word, the
under-represented family must not wait until 8th/14th.
**Does not authorize:** PDF seats, synonym nets, encoder, FTS, one-box,
raising the page, keyword Find, marketing AVAILABLE

## Why this exists

D-24 put swimming timetables **on** the page. Founder: they start at
**8th and 14th**. Round-robin still opened every round with `{classes}`
(cosine majority). Swimming-only sat on even seats; photos occupied 2/4/6;
PDFs at 8 then later.

## Scope

When **no** in-band hit matches every named word: count each family in the
cosine prefix of 20; the starved family takes its fair share
(`20 / familyCount`) first; remainder round-robins. When some hit is Exact
(every named word), keep D-23/D-24 deepest-first round-robin so
`swimming timetable` / `passport` do not become a mixed dump.

Not a type quota. Intra-family order stays cosine (swimming photos can
still sit above a weaker timetable in `{swimming}`).

## Frozen (keyword Find)

Same freeze as D-20–D-24.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning shown-page occupancy
CURRENT LEGACY PATH: none — Live/Dual N = 0
TARGET PATH: MeaningNamedWordDepthPage starved-family fair share when
  mixed Partial; deepest-first when Exact exists
WHY THIS CONVERGES: P-LIST occupancy inside Canonical Recall
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** D-24 device (timetables at 8 and 14); P-LIST.
- **Current-code evidence inspected:** D-24 round-robin; families ordered
  by set size then first-seen (classes first).
- **Acceptance criteria:**
  - [x] 20 `{classes}` + swimming photos + timetables → swimming family
        occupies the first fair-share seats; timetables before classes
  - [x] Exact two-token still leads
  - [x] One-word `passport` unchanged
  - [ ] Founder: timetables near the top of `when are the swimming classes`
- **Anti:** PDF seats; `classes` = `timetable`; page 40

## Delivery record (2026-09-19)

- **Files/layers:** `MeaningNamedWordDepthPage` starved-family fair share.
- **Automated verification:**
  `starved_family_takes_its_fair_share_before_the_cosine_majority`;
  `same_depth_families_share_the_page` (timetable first);
  `mixed_depths_round_robin_deepest_first` unchanged.
- **Emulator/manual:** founder re-test `when are the swimming classes`;
  `passport` / `swimming timetable`. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.
