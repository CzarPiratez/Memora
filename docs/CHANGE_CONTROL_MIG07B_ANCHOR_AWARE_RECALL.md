# Change control — MIG-07B anchor-aware structured recall

**Status:** Authorized — Slice 4 delivered (MIG-07B ranking convergence)  
**Opened:** 2026-08-30  
**Authority:** `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` MIG-07B;
`docs/LOCAL_AI_TECHNICAL_SPEC.md` §9; ADR-049 (Canonical Recall);
`docs/LEGACY_RECALL_SURFACE.md` (L7, L8 retire-by).  
**Does not authorize:** Grounded Answers, RecallRanker model impl, new Find
paths, Freeze reopen, or extending L7/L8 feature surface before cutover.

## Intent

Fold meaning Find (L8) and local ranking (L7) into **Canonical Recall** with
a deterministic structured-filter stage over `MemoryAnchor` (TIME/TOPIC). One
product-facing retrieval API; keyword and meaning remain **candidate generators**.

## Architectural convergence

| Field | Value |
|---|---|
| **ARCHITECTURAL BOUNDARY** | Canonical Recall (ADR-049) |
| **CURRENT LEGACY PATH** | L7 (meaning-local boost in `SearchAssetMemoriesByMeaning`), L8 (meaning Find not behind `CanonicalRecall`) |
| **TARGET PATH** | `CanonicalRecall` orchestrates keyword + meaning candidates → anchor structured filter → shared ranking |
| **WHY THIS CONVERGES** | Shrinks Live/Dual N from 2 → 0; meets Product Contract retrieval promise (time/topic cues) |
| **WHAT OLD PATH WILL EVENTUALLY BE RETIRED** | L7, L8 rows → Retired after ViewModels route meaning through `CanonicalRecall` |
| **EXTENDS LEGACY?** | No — defect fixes only on L7/L8 until cutover ships |

## Problem

Keyword Finds already enter `CanonicalRecall`. Meaning Find and anchor-aware
filtering do not. Queries with time/topic cues cannot use anchors MIG-03
populates. Enterprise recall is split across three surfaces.

## Sliced delivery (proposed)

### Slice 1 — Domain contracts (no UI cutover)

- `RecallQueryConstraint` enum: `Explicit` | `Advisory` | `None` per TIME/TOPIC cue
- `RecallQueryConstraintClassifier` (deterministic, bounded; not general NLU)
- `AnchorStructuredRecallFilter` pure function + fixture tests per Migration Spec
  acceptance criteria (explicit excludes; advisory re-ranks only; missing anchor neutral)

### Slice 2 — CanonicalRecall orchestration

- `CanonicalRecall` accepts recall mode or unified entry; delegates keyword to
  `SearchMemoryEvidence`, meaning to `SearchAssetMemoriesByMeaning` (candidate gen only)
- RRF fusion per `FUTURE_CAPABILITY_BACKLOG.md` FC-01 inside ranking stage
- L7 boost logic moves from meaning use case into shared ranker (L7 → Cutover)

### Slice 3 — UI cutover + L8 retirement

- Meaning Find ViewModels call `CanonicalRecall` only
- L8 → Retired; update `LEGACY_RECALL_SURFACE` header (N → 0 or 1)
- Escape-hatch audit recorded in `ESCAPE_HATCH_AUDIT.md`

## Verification (each slice)

- `scripts/check-legacy-recall-surface.sh` — N must not increase without ADR
- `CanonicalRecallTest` + new anchor-filter fixture tests
- `./gradlew :app:testDebugUnitTest`
- No new `application`→`data` violations (`check-application-layer-boundaries.sh`)

## Dependencies (satisfied)

- MIG-03 anchors (TIME/TOPIC in `DeterministicMemoryBuilder`)
- MIG-06 `SearchMemoryEvidence`
- MIG-07 keyword cutovers (L1–L4 Retired)
- MIG-05 evidence embeddings (meaning candidate gen)

## Out of scope (this change control)

- `PERSON` / `PLACE` / `OBJECT` anchor filtering (not populated)
- Cross-encoder rerank (FC-02)
- Grounded Answers
- Gradle module split (Phase 2 separate change control)

## Authorization

**Slice 1 authorized** via `CHANGE_CONTROL_DUAL_TRACK_CHECKPOINT_1.md` (2026-08-31).
**Slice 2 delivered** (2026-08-31): `CanonicalRecall.searchByMeaning`,
`searchHybrid` (RRF), `AnchorAwareMeaningRecallRanking`, `findSignatureAnchors`.
**Slice 3 delivered** (2026-08-31): `MeaningSearchViewModel` → `CanonicalRecall`;
L8 Retired; Live/Dual N=1.
**Slice 4 delivered** (2026-08-31): L7 token boost → shared ranker; L7 Retired;
Live/Dual N=0. MIG-07B ranking convergence complete; `RECALL_CONVERGENCE_DONE`
program exit still open.
