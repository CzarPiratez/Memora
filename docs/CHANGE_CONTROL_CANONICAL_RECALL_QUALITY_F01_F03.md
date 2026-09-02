# Change control — Canonical Recall quality follow-ups (F-01–F-03)

**Date:** 2026-09-02  
**Type:** Engineering — Canonical Recall quality  
**Status:** Delivered — **device verified PASS** (Samsung SM-A156E, 2026-09-02)  
**Requirement IDs:** F-01, F-02, F-03 from `docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md`  
**Runbook:** `docs/A01_FOLLOWUP_DEVICE_VERIFICATION_RUNBOOK.md`

Does **not** authorize marketing **AVAILABLE**.

## Pre-work record

- **Source documents read:** `docs/GOVERNANCE.md`, `docs/RECALL_ENFORCEMENT_INDEX.md`,
  `docs/LEGACY_RECALL_SURFACE.md` (N = 0), `docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md`,
  `docs/ENGINEERING_CHARTER.md`, `CONTINUE.md`.
- **Current-code evidence inspected:** `SearchMemoryEvidence`, `AnchorAwareMeaningRecallRanking`,
  `CanonicalRecallWhyCopy`, `MeaningEvidenceTokenBoost`.
- **Smallest safe change:** Quality fixes inside Canonical Recall only; no new Find pipeline.

## ARCHITECTURAL BOUNDARY

```
CURRENT LEGACY PATH: none
TARGET PATH: Canonical Recall keyword + meaning ranking
WHY THIS CONVERGES: per-asset AND keyword + lexical meaning filter + consumer Why copy
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a
EXTENDS LEGACY? no
```

## Delivery

| ID | Change | Files |
|---|---|---|
| F-01 | Multi-token keyword AND per asset (cross-page tokens count) | `SearchMemoryEvidence.kt`, `MemoryEvidenceLiteralSearchSupport.kt` |
| F-02 | Lexical AND filter on meaning hits when ≥2 content tokens | `MeaningEvidenceLexicalFilter.kt`, `AnchorAwareMeaningRecallRanking.kt` |
| F-03 | Consumer Why copy (no similarity/score engineering jargon) | `CanonicalRecallWhyCopy.kt` |

## Tests

- `SearchMemoryEvidenceTest` — multi-word AND per asset
- `MeaningEvidenceLexicalFilterTest` — stop-word handling + AND gate
- `AnchorAwareMeaningRecallRankingTest` — lexical filter excludes partial-token hits
- `CanonicalRecallWhyCopyTest`, `MeaningSearchCopyTest` — updated Why assertions

## Verification gate

**Device PASS (2026-09-02)** on Samsung SM-A156E (`Documents/unfynd-test` corpus;
later expanded folder for F-04). Operator recorded:

| ID | Verdict | Notes |
|---|---|---|
| F-01 | **PASS** | `silky wreck` / `silky, wreck` / `silky + blink` hit when both words in same PDF; cross-file AND correctly empty |
| F-02 | **PASS** | Natural cue with both content tokens returned the matching PDF only |
| F-03 | **PASS** | Multiline consumer Why; no similarity/score jargon |
| F-04 | **PASS** | Rescan + **Check for new PDFs** first when folder already listed |
| A-01 regression | **PASS** | Offline keyword + meaning after cold start |

Unit suite: **483** `testDebugUnitTest` passed on delivery build.
