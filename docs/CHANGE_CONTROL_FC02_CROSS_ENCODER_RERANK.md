# Change control — FC-02 on-device cross-encoder rerank

**Date opened:** 2026-09-02  
**Status:** **Authorized** — implementation slices below; not AVAILABLE / not shipped  
**Backlog:** `docs/FUTURE_CAPABILITY_BACKLOG.md` FC-02  
**Program:** `docs/POST_MVP_PROGRAM_V1.md` P2 Recall excellence  
**Decision guardrails:** Ranking stays inside **Canonical Recall** only. Live/Dual
**N** must remain **0**. No new Find path. No Grounded Answers runtime. No Act.
No marketing AVAILABLE claim from this slice.

## Pre-work record

- **Requirement IDs:** FC-02; Local AI Spec §9 retrieval signals; ADR-049 Canonical
  Recall; `RECALL_CONVERGENCE_DONE` (MIG-07B ranking convergence complete).
- **Source documents read:** `docs/FUTURE_CAPABILITY_BACKLOG.md`,
  `docs/POST_MVP_PROGRAM_V1.md`, `docs/GROUNDING_ARCHITECTURE.md` (Retriever note),
  `docs/LOCAL_AI_TECHNICAL_SPEC.md`, `docs/LEGACY_RECALL_SURFACE.md`,
  `docs/CANONICAL_RECALL_RESULT_CONTRACT.md`, `AnchorAwareMeaningRecallRanking.kt`,
  `SearchAssetMemoriesByMeaning.kt`, `LocalIntelligenceEngines.kt` (`RecallRanker`).
- **Current-code evidence inspected:** `RecallRanker` interface exists without
  `rank` operate method; meaning ranking is token boost + anchor filter in
  `AnchorAwareMeaningRecallRanking`; no cross-encoder runtime bound.
- **Open ADRs / platform limitations checked:** FC-02 gate “After MIG-07B” satisfied
  per `RECALL_CONVERGENCE_DONE`. Cross-encoder model pack not yet selected — slice 1
  lands port + identity ranker; slice 2 binds model when measured.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Rerank runs on **stored excerpt text** already in Room — no new source reads, no
  network. Additional on-device inference cost — must respect latency budget and
  degrade to identity ranking when pack unavailable.
- **Smallest safe change (slice 1):** Extend `RecallRanker` domain contract;
  `IdentityRecallRanker` (score-order pass-through); unit tests; no UI change.
- **Acceptance criteria (slice 1):**
  - [x] `RecallRanker.rank` domain API documented and tested
  - [x] `IdentityRecallRanker` preserves input order by score
  - [x] `UnavailableRecallRanker` returns honest unavailable rank result
  - [x] No wiring into `CanonicalRecall` until slice 2 change-control note
- **Acceptance criteria (slice 2 — cross-encoder):**
  - [ ] Bounded pool (default top **40** evidence passages, max **50**)
  - [ ] Invoked from `AnchorAwareMeaningRecallRanking` after token boost, before/with
        anchor filter (exact order documented in delivery record)
  - [ ] Measurable recall lift on `meaning-pdf-page-recall-v1` or successor fixture
  - [ ] Midrange latency within planning budget or explicit degraded copy
  - [ ] Falls back to identity ranking when reranker unavailable
- **Test and emulator verification plan:** JVM unit tests slice 1; slice 2 adds
  fixture corpus test + emulator smoke; midrange measurement before AVAILABLE tie-in.
- **User-visible quality/accessibility review plan:** Optional subtle copy only if
  rerank assist is disclosed (mirror E5d token-boost honesty pattern).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall (ADR-049)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (N=0)
TARGET PATH: AnchorAwareMeaningRecallRanking → RecallRanker port (FC-02)
WHY THIS CONVERGES: Industry-standard rerank stage inside single ranking boundary
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: ad-hoc per-hit score tweaks outside ranker
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — meaning ranking remains inside CanonicalRecall
```

## Problem

Meaning Find orders candidates by cosine similarity (+ token boost + anchor filter).
Production RAG and on-device memory stacks add a **cross-encoder rerank** over a
bounded top-k pool for large quality gains. UNFYND lacks the `RecallRanker` operate
path Spec §4 names; FC-02 cannot land without the port.

## Sliced delivery

### Slice 1 — RecallRanker port + identity implementation (delivered 2026-09-02)

- Domain: `RecallRankCandidate`, `RecallRankResult`, `RecallRanker.rank`
- `IdentityRecallRanker` — stable sort by score descending
- Tests; no CanonicalRecall wiring

### Slice 2 — Cross-encoder adapter (authorized after slice 1 green)

- Select on-device cross-encoder pack (ADR + measured choice — not in slice 1)
- `CrossEncoderRecallRanker` or adapter behind `RecallRanker`
- Wire into `AnchorAwareMeaningRecallRanking.applyTokenBoost` exit path
- Bounded pool from `MeaningSearchHit` evidence excerpts

### Slice 3 — Measurement gate (before AVAILABLE tie-in)

- Re-run M4-class device measurement or fixture harness with/without rerank
- Document in change-control delivery record

## Explicit non-goals

- Reranking keyword Find (unless separate ADR)
- New embedding index or second vector store
- Generative answers / Verifier
- Cloud rerank API

## Delivery record

- **Files/layers changed:** _Pending slice 1_
- **Automated verification and result:** _Pending_
- **Emulator/manual verification and result:** _Pending_
- **Known limitation or follow-up:** Cross-encoder model selection deferred to slice 2
- **Documentation/traceability/ADR updates:** This record; `POST_MVP_PROGRAM` P2;
  `MVP_EXIT_AUDIT` RecallRanker row when slice 1 lands
- **Git commit:** _Pending_
