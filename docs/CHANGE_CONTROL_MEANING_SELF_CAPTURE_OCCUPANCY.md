# Change control — Meaning self-capture occupancy (D-27)

**Date:** 2026-09-19
**Type:** Authorized ranking correction
**Status:** **Landed in tree 2026-09-19.** Keyword Find frozen. Cap 20 /
pool 60 unchanged. D-25 mixed Partial and D-26 Exact-first among
**originals** unchanged.
**Ask Model:** **P-AND vs P-LIST**. D-14 still stands: a picture of
UNFYND is not the original file.
**Does not authorize:** PDF seats, synonym nets, encoder, FTS, one-box,
raising the page, keyword Find, marketing AVAILABLE

## Why this exists

D-26 device (founder meaning Find):

| Cue | Result |
|---|---|
| `swimming timetable` | Exact PDFs at **4/7/8/9** (was 8/12/13/14) |
| `when are the swimming classes` | PDFs at 5/8/9/10 — hold |
| `wifi password` | Hold |
| `passport` | Hold |
| `scan silky` | **Regressed.** First 12 = app screenshots; scan PDF later; silky 17th |

D-14 already demotes pictures of UNFYND after ranking. D-26 Exact-first
re-reads the close cosine band and treats any file that OCRs every named
word as Exact. A Find screenshot repeats the cue in chrome, so it becomes
the Exact family and takes the page.

## Scope

[MeaningNamedWordDepthPage] partitions [UnfyndSelfCapture] hits out of
family occupancy and appends them last. Exact-first and starved-family
mix run on originals only.

Not a type quota. A real timetable screenshot that is not UNFYND chrome
can still sit in Exact.

## Frozen (keyword Find)

Same freeze as D-20–D-26.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning shown-page occupancy
CURRENT LEGACY PATH: none — Live/Dual N = 0
TARGET PATH: MeaningNamedWordDepthPage Exact-first / D-25 mix among
  originals; D-14 self-captures stay last
WHY THIS CONVERGES: occupancy must not undo D-14 or invent a new Find
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** D-26 device miss on `scan silky`; D-14 demotion.
- **Holistic scenarios:** `scan silky` originals lead; `swimming
  timetable` Exact originals still lead; `wifi password` / `passport` /
  `when are the swimming classes` hold; undetected chrome is a later
  lever (neighbour reservation), not this step.
- **Acceptance criteria:**
  - [x] 12 Exact self-captures + scan + silky → originals first, self last
  - [x] Exact originals still lead neighbours (D-26)
  - [x] Mixed Partial starved-family tests kept
  - [x] Founder: `scan silky` restored; `swimming timetable` Exact images
        at 1–3, PDFs at 4/8/9/10; passport / wifi / classes hold
  - [x] Founder miss: `when are the swimming classes for grade 2`
        irrelevant — `{grade}` family share (see D-28)

## Delivery record (2026-09-19)

- **Files/layers:** `MeaningNamedWordDepthPage` self-capture partition.
- **Automated verification:** `:app:testDebugUnitTest` for
  `MeaningNamedWordDepthPageTest`, `MeaningTrustedHitPolicyTest`,
  `CanonicalRecallMeaningTest` — BUILD SUCCESSFUL (2026-09-19).
- **Emulator/manual:** `scan silky`, `swimming timetable`, confirm wifi /
  classes / passport. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.
