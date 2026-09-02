# Change control — Canonical Recall quality follow-ups (F-01–F-03)

**Date:** 2026-09-02  
**Type:** Engineering — Canonical Recall quality  
**Status:** Delivered — pending unified device verification  
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

Operator runs `docs/A01_FOLLOWUP_DEVICE_VERIFICATION_RUNBOOK.md` (F-01–F-04 + A-01 regression)
on Samsung SM-A156E with `Documents/unfynd-test` corpus.

Unit suite: `testDebugUnitTest` in Android Studio on delivery build.
