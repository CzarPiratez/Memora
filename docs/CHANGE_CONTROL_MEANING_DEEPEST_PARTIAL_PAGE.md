# Change control — Meaning deepest-partial page (D-28)

**Date:** 2026-09-19
**Type:** Authorized ranking correction
**Status:** **Landed in tree 2026-09-19. Occupancy path CLOSED (ADR-055).**
Do not open a further mixer ticket. Keyword Find frozen. Cap 20 / pool 60
unchanged. Same-depth mix (wifi / scan silky) unchanged.
**Ask Model:** **P-AND**. Stacked attributes of one original
(`swimming classes for grade 2`) are not a list of `{grade}` files.
**Does not authorize:** PDF seats, synonym nets (`classes` ≠ `timetable`),
encoder, FTS, one-box, raising the page, keyword Find, marketing AVAILABLE

## Why this exists

D-27 device (founder meaning Find):

| Cue | Result |
|---|---|
| `scan silky` | Restored — originals lead, app screenshots last |
| `swimming timetable` | Exact images at 1–3; PDFs at 4/8/9/10 |
| `wifi password` / `passport` / `when are the swimming classes` | Hold |
| `when are the swimming classes for grade 2` | **Miss.** Irrelevant results |

`for` is already a wrapper. Tokens are `swimming`, `classes`, `grade`
(the digit `2` is below [RecallQueryContentTokens.MIN_TOKEN_LENGTH]).
D-25 then gave every family — including `{grade}`-only school files — a
fair share. A qualifier turned a working swimming page into a grade dump.

D-22-style band: one-word files can also set a high cosine and drop a
two-word swimming hit (gap > 0.22).

## Scope

1. Eligible set: when some hit matches **two or more** named words, those
   deepest hits stay page-eligible even if shallower files set the cosine
   ceiling. One-word cues still use the 0.22 band alone.
2. Occupancy: starved-family mix (D-25) runs only among families at that
   deepest depth. Shallower families fill leftover seats (D-20 keep).
   Exact (every named word) is the same rule.

Not a type quota. A real timetable screenshot that names the words can
still sit with the PDF.

## Frozen (keyword Find)

Same freeze as D-20–D-27.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning shown-page occupancy
CURRENT LEGACY PATH: none — Live/Dual N = 0
TARGET PATH: MeaningTrustedHitPolicy + MeaningNamedWordDepthPage
  deepest-partial eligibility and occupancy
WHY THIS CONVERGES: P-AND qualifiers must not invent a parallel
  one-word Find; P-LIST same-depth mix is unchanged
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** D-27 device miss on `… for grade 2`; P-AND J2.
- **Holistic scenarios:** three-word qualifier leads with two-word
  swimming hits; `when are the swimming classes` / wifi / silky /
  passport / timetable Exact hold; 1-word far cosine still drops;
  undetected chrome still last (D-27).
- **Acceptance criteria:**
  - [x] 20 `{grade}` + 1 `{swimming, grade}` → gold first
  - [x] two depth-2 families starve among themselves; `{grade}` last
  - [x] gold cosine 0.60 vs shallower 0.90 still eligible
  - [x] D-25 / D-26 / D-27 tests kept
  - [x] Occupancy path closed without a further mixer; next is ADR-055
        (grade-2 cue is a retrieval-stack job, not D-29)

## Delivery record (2026-09-19)

- **Files/layers:** `MeaningTrustedHitPolicy.eligibleHits`;
  `MeaningNamedWordDepthPage` deepest-tier mix.
- **Automated verification:** `:app:testDebugUnitTest` for
  `MeaningNamedWordDepthPageTest`, `MeaningTrustedHitPolicyTest`,
  `CanonicalRecallMeaningTest`, `MeaningRecallCueTest` — BUILD SUCCESSFUL
  (2026-09-19).
- **Emulator/manual:** the grade-2 cue; confirm classes / timetable /
  wifi / silky / passport. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.
