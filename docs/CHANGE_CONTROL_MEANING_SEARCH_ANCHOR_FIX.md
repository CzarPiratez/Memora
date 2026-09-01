# Change control: Find-by-meaning — anchor load crash + enterprise hardening

**Date:** 2026-09-01  
**Type:** Defect fix + verification hardening (Canonical Recall MEANING path)  
**Decision guardrails:** No new Find path. No Live/Dual growth (N stays 0). No
AVAILABLE claim. No Room schema bump. No legacy extension.

```
ARCHITECTURAL BOUNDARY: Canonical Recall → searchByMeaning →
  SearchAssetMemoriesByMeaning + AnchorAwareMeaningRecallRanking
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: CanonicalRecall.searchByMeaning (MIG-07B)
WHY THIS CONVERGES: fixes production crash on advisory TOPIC queries; adds
  persistence proof + failure-path tests without widening Find surface
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a — defect on canonical path
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged (N = 0)
```

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; MIG-07B anchor structured filter;
  P-01 evidence-backed recall; Local AI Spec on-device embed safety.
- **Root cause:** `RoomMemoryRepository.findSignatureAnchors` constructed
  `MemoryAnchor` with `evidenceIds = emptySet()`, violating domain `require`
  and throwing during ranking for typical queries ≥4 chars (ADVISORY TOPIC),
  surfaced as UI `SearchCouldNotFinish`.
- **Secondary risk:** concurrent `TextEmbedder.embed` during index build +
  Find-by-meaning (IO + Default dispatchers) — serialized via engine lock.
- **Smallest safe change:** load `memory_anchor_evidence` join; skip invalid
  rows; add diagnostics logging; test parity with keyword Find failure paths.

## Delivery record

### Production (meaning path only)

| File | Change |
|------|--------|
| `RoomMemoryRepository.kt` | `findSignatureAnchors` loads cited evidence |
| `MemoryDao.kt` | `findAnchorEvidenceForRevisions` |
| `MediaPipeEmbeddingEngine.kt` | synchronized embedder access |
| `SearchAssetMemoriesByMeaning.kt` | try/catch, label trim, drop diagnostics |
| `MeaningSearchCandidateDiagnostics.kt` | pure-Kotlin operator signal |
| `MeaningSearchViewModel.kt` | Logcat on search/readiness failures |

### Verification

| File | Purpose |
|------|---------|
| `RoomMemoryRepositorySignatureAnchorsIntegrationTest.kt` | Room regression for anchor crash |
| `CanonicalRecallMeaningTest.kt` | Facade wiring + trim + anchor rank |
| `SearchAssetMemoriesByMeaningTest.kt` | Failed + dimension mismatch |
| `MeaningSearchViewModelTest.kt` | Failure phase parity with keyword Find |
| `MeaningSearchCandidateDiagnosticsTest.kt` | Drop stats log message |
| `AnchorAwareMeaningRecallRankingTest.kt` | Advisory topic + anchors |

### Not in scope

- New user-visible “index corrupt” phase (future UX)
- Hybrid Find UI wiring
- Keyword Find changes
- Room migration / schema version bump

## Acceptance

1. Unit: `:app:testDebugUnitTest` — meaning-search tests green.
2. Instrumented: `RoomMemoryRepositorySignatureAnchorsIntegrationTest` green.
3. Emulator: meaning index built → Find `invoice` / `mira` returns results or
   honest NoMatches — not `SearchCouldNotFinish`.
4. Smoke: keyword Find + Build memories unchanged.

## Privacy

On-device only. Diagnostics logs contain query text and drop counts — no
network, no user file content in logs.
