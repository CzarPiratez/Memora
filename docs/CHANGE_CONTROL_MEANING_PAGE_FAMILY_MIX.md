# Change control — Meaning page named-word family mix (D-24)

**Date:** 2026-09-19
**Type:** Authorized ranking correction
**Status:** **Landed in tree 2026-09-19.** Keyword Find frozen. Pool 60 /
page cap 20 unchanged. Device re-test of `when are the swimming classes`
open; `passport` / `swimming timetable` must not regress.
**Ask Model:** **P-AND vs P-LIST**; **P-EVIDENCE**. Coverage is a boost and
a banner, not exclusive occupancy by one named-word family.
**Does not authorize:** type quotas, PDF seats, synonym nets
(`classes` ≠ `timetable`), FTS5, encoder swap, hybrid, one-box, keyword
Find, schema, raising pool 60, growing the shown page, marketing AVAILABLE

## Why this exists

D-22: timetable PDF `admittedRank=28` `rankedRank=21`
`diagnosis=in_pool_off_page`. D-23 mixed the shown 20 by named-token
**count**. Founder: still cannot see `Grade-2-Swimming-TT-2026.pdf`.

D-23 is a no-op when the files above the PDF are also **1-token**. Device
cards were class/classes images — `{classes}` — not `{swimming, classes}`.
Same depth. Cosine order then fills 20 with classes-only. The PDF is
`{swimming}` and stays 21st.

## Scope

| Step | What | Must not |
|---|---|---|
| **D-24 family mix** | Group in-band hits by matching token **set**. Round-robin those families (deeper sets lead). `{swimming}` and `{classes}` share the 20. | PDF seats; synonym; bigger page; encoder |

## Frozen (keyword Find)

Same freeze as D-20–D-23.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning shown-page occupancy.
CURRENT LEGACY PATH (L# or none): none — Live/Dual N = 0
TARGET PATH: MeaningTrustedHitPolicy band → MeaningNamedWordDepthPage
  families (token sets) → cap 20
WHY THIS CONVERGES: D-21 keep was already in the 60; the page must not
  let one named-word set occupy every seat
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** D-22 `in_pool_off_page`; D-23 device miss; Ask Model
  P-LIST.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (N = 0), D-23 CC.
- **Current-code evidence inspected:** `MeaningNamedWordDepthPage` grouped
  by `matchingTokens.size`; one depth → return incoming order.
- **Smallest safe change:** grouping key = token set.
- **Acceptance criteria:**
  - [x] 20 `{classes}` + in-band `{swimming}` timetable → timetable on
        the shown 20
  - [x] 20 two-token + timetable still on the page (D-23 case)
  - [x] `passport` unchanged; no type reorder
  - [ ] Founder sees `Grade-2-Swimming-TT-2026.pdf` on meaning
        `when are the swimming classes`
- **Holistic scenarios:**
  - User: class photos still appear; swimming-only neighbour can too
  - Anti: `classes` means `timetable`; PDF forcing
  - Technical: no `connectedDebugAndroidTest`
- **Limitation:** if many higher-cosine `{swimming}` photos exist, the PDF
  can still sit behind them in that family queue. That is cosine inside
  the family, not occupancy by `{classes}`.

## Alternatives considered

- **Assume D-23 was not installed** — possible, but the D-23 rule is still
  wrong for this library even with the new APK.
- **PDF seats / raise cap** — rejected.
- **Encoder** — rejected; file is in the 60 and in the band.

## Delivery record — family mix (2026-09-19)

- **Files/layers:** `MeaningNamedWordDepthPage` groups by token set.
  Keyword Find unchanged.
- **Automated verification:**
  `same_depth_families_share_the_page`;
  `same_depth_named_word_families_share_a_capped_page`;
  `searchByMeaning_shows_a_swimming_only_neighbour_among_classes_only_hits`;
  D-23 two-token tests kept.
- **Emulator/manual:** founder debug `when are the swimming classes`;
  confirm PDF card; `passport` / `swimming timetable`. Do **not** run
  `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.
