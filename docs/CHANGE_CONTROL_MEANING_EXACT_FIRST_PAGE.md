# Change control — Meaning Exact-first page (D-26)

**Date:** 2026-09-19
**Type:** Authorized ranking correction
**Status:** **Landed in tree 2026-09-19.** Keyword Find frozen. Cap 20 /
pool 60 unchanged. Mixed Partial (wifi / scan silky) unchanged.
**Ask Model:** **P-AND vs P-LIST**. Exact must not *delete* neighbours
(D-20). Exact must still **lead** the shown page.
**Does not authorize:** PDF seats, synonym nets, encoder, FTS, one-box,
raising the page, keyword Find, marketing AVAILABLE

## Why this exists

D-25 stop-gate on founder meaning Find:

| Cue | Result |
|---|---|
| `passport` | Good |
| `wifi password` | Wifi shots then password note/image; no file has both |
| `scan silky` | Scan shot, scan PDF, silky PDF |
| `swimming timetable` | Exact PDFs at **8, 12, 13, 14** |

Wifi / silky are mixed Partial — keep D-25. `swimming timetable` has files
that contain every named word. D-23–D-25 still **round-robined** Exact
with one-word neighbours, so neighbours took seats ahead of Exact.

D-20: coverage is a boost and a banner, not a subset that **deletes**
Partial. It is not permission for Partial to occupy 1–7 on an Exact cue.

## Scope

When an in-band family matches **every** named word: emit that family
first (incoming / cosine order, any asset type). Remaining named-word
hits follow for leftover seats under the cap of 20. If Exact already
fills 20, neighbours stay in the 60 and miss the page.

Mixed Partial path (no Exact family) is unchanged (D-25 starved-family
fair share).

## Frozen (keyword Find)

Same freeze as D-20–D-25.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning shown-page occupancy
CURRENT LEGACY PATH: none — Live/Dual N = 0
TARGET PATH: MeaningNamedWordDepthPage Exact-first when Exact exists;
  D-25 starved mix when it does not
WHY THIS CONVERGES: Exact lead without deleting the neighbour generator
  or adding type quotas
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** D-25 device miss on Exact; D-20 keep-not-subset.
- **Acceptance criteria:**
  - [x] 3 Exact + 1 neighbour → Exact then neighbour
  - [x] 20 Exact + neighbour → page is Exact only
  - [x] 12 Exact + neighbour → neighbour after Exact, still on page
  - [x] 20 `{classes}` + `{swimming}` still starved-family (wifi class)
  - [x] Founder: `swimming timetable` Exact PDFs moved to 4/7/8/9
        (from 8/12/13/14); `passport` / `wifi password` / classes hold
  - [x] Founder miss: `scan silky` first 12 = app screenshots; silky 17th
        — Exact-first undid D-14 (see D-27)

## Delivery record (2026-09-19)

- **Files/layers:** `MeaningNamedWordDepthPage` Exact-first concatenate.
- **Automated verification:** `:app:testDebugUnitTest` for
  `MeaningNamedWordDepthPageTest`, `MeaningTrustedHitPolicyTest`,
  `CanonicalRecallMeaningTest` — BUILD SUCCESSFUL (2026-09-19). D-25
  starved-family tests kept.
- **Emulator/manual:** `swimming timetable`; confirm wifi / silky /
  passport. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.
