# Change control — FC-02 on-device cross-encoder / Evidence-native RecallRanker

**Date opened:** 2026-09-02  
**Status:** **Authorized** — implementation slices below; not AVAILABLE / not shipped  
**Backlog:** `docs/FUTURE_CAPABILITY_BACKLOG.md` FC-02  
**Program:** `docs/POST_MVP_PROGRAM_V1.md` P2 Recall excellence  
**Governing ADR:** **ADR-051** (Evidence-native on-device RecallRanker)  
**Decision guardrails:** Ranking stays inside **Canonical Recall** only. Live/Dual
**N** must remain **0**. No new Find path. No Grounded Answers runtime. No Act.
No marketing AVAILABLE claim from this slice. Public “evidence-native ranker”
language requires ADR-051 **Stage B** measured green — not Stage A alone.

## Pre-work record

- **Requirement IDs:** FC-02; ADR-051; Local AI Spec §9 retrieval signals; ADR-049
  Canonical Recall; `RECALL_CONVERGENCE_DONE` (MIG-07B ranking convergence complete).
- **Source documents read:** `docs/FUTURE_CAPABILITY_BACKLOG.md`,
  `docs/POST_MVP_PROGRAM_V1.md`, `docs/GROUNDING_ARCHITECTURE.md` (Retriever note),
  `docs/LOCAL_AI_TECHNICAL_SPEC.md`, `docs/LEGACY_RECALL_SURFACE.md`,
  `docs/CANONICAL_RECALL_RESULT_CONTRACT.md`, `AnchorAwareMeaningRecallRanking.kt`,
  `SearchAssetMemoriesByMeaning.kt`, `LocalIntelligenceEngines.kt` (`RecallRanker`),
  ADR-051.
- **Current-code evidence inspected:** `RecallRanker.rank` + `IdentityRecallRanker`
  (slice 1); meaning ranking is token boost + lexical filter + anchor filter in
  `AnchorAwareMeaningRecallRanking`; no cross-encoder runtime bound.
- **Open ADRs / platform limitations checked:** FC-02 gate “After MIG-07B”
  satisfied. ADR-051 upgrades FC-02 to Stage A (semantic head) + Stage B
  (evidence-native signals). Cross-encoder / equivalent pack not yet selected —
  Stage A binds model when measured. FC-03 is hard dependency before Stage B
  complete.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Rerank runs on **stored excerpt text** (and Stage B structured fields already
  in Room) — no new source reads, no network for inference. Pack download =
  model bytes only. Degrade to identity ranking when pack unavailable.
- **Smallest safe change (slice 1):** Extend `RecallRanker` domain contract;
  `IdentityRecallRanker` (score-order pass-through); unit tests; no UI change.
- **Acceptance criteria (slice 1):**
  - [x] `RecallRanker.rank` domain API documented and tested
  - [x] `IdentityRecallRanker` preserves input order by score
  - [x] `UnavailableRecallRanker` returns honest unavailable rank result
  - [x] No wiring into `CanonicalRecall` until Stage A change-control note
- **Acceptance criteria (Stage A — semantic head / former “slice 2”):**
  - [ ] Bounded pool (default top **40** evidence passages, max **50**)
  - [ ] On-device relevance model (cross-encoder or measured equivalent) behind
        `RecallRanker` — pack chosen in delivery record with license / size /
        A15-class latency evidence (ADR-051 §7)
  - [ ] Invoked from `AnchorAwareMeaningRecallRanking` after token boost,
        before/with anchor filter (exact order documented in delivery record)
  - [ ] Measurable recall lift on `meaning-pdf-page-recall-v1` or successor fixture
  - [ ] Midrange latency within planning budget or explicit degraded copy
  - [ ] Falls back to identity ranking when reranker unavailable
- **Acceptance criteria (Stage B — evidence-native; ADR-051 differentiator):**
  - [ ] Candidates carry structured signals (locator, evidence kind/class,
        anchors when present, lexical completeness) into ranking
  - [ ] Ordering uses those signals (not text-only semantic score alone)
  - [ ] Why factors may cite inspectable signals without inventing evidence
  - [ ] FC-03 chunking/locators for in-scope MVP types green (or explicit
        interim limitation recorded) before Stage B “complete”
  - [ ] Measured lift vs Stage A-only on fixture / device corpus
- **Test and emulator verification plan:** JVM unit tests slice 1; Stage A adds
  fixture corpus test + emulator smoke; midrange measurement before AVAILABLE
  tie-in; Stage B adds signal-unit tests + regression vs Stage A.
- **User-visible quality/accessibility review plan:** Optional subtle copy only if
  rerank assist is disclosed (mirror E5d token-boost honesty pattern).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall (ADR-049)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (N=0)
TARGET PATH: AnchorAwareMeaningRecallRanking → RecallRanker (ADR-051 Stage A/B)
WHY THIS CONVERGES: Semantic head + evidence-native signals inside one ranking port
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: ad-hoc per-hit score tweaks outside ranker
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — meaning ranking remains inside CanonicalRecall
```

## Problem

Meaning Find orders candidates by cosine similarity (+ token boost + lexical /
anchor filters). Production stacks add a bounded semantic rerank. UNFYND also
stores structured `MemoryEvidence` that a text-only cross-encoder ignores.
FC-02 slice 1 landed the port; ADR-051 requires Stage A (measured semantic head)
then Stage B (evidence-native differentiator) so delivery does not stop at a
generic MiniLM wrapper.

## Sliced delivery

### Slice 1 — RecallRanker port + identity implementation (delivered 2026-09-02)

- Domain: `RecallRankCandidate`, `RecallRankResult`, `RecallRanker.rank`
- `IdentityRecallRanker` — stable sort by score descending
- Tests; no CanonicalRecall wiring

### Stage A — Semantic head (authorized; was “slice 2”)

- Select on-device pack via measured delivery record (ADR-051 §7)
- Adapter behind `RecallRanker`
- Wire into `AnchorAwareMeaningRecallRanking`
- Bounded pool from evidence excerpts
- Engineering checkpoint allowed; **not** full ADR-051 product claim

### Stage B — Evidence-native differentiator (authorized under ADR-051)

- Extend candidate / rank contracts for structured signals
- FC-03 dependency before Stage B complete
- Why factors when disclosed
- Required before public “evidence-native ranker” language

### Measurement gate (before AVAILABLE tie-in)

- Re-run M4-class device measurement or fixture harness with/without Stage A
- Stage B vs Stage A comparison before marketing language

## Explicit non-goals

- Reranking keyword Find (unless separate ADR)
- New embedding index or second vector store
- Generative answers / Verifier
- Cloud rerank API
- SLM as primary Find ranker
- Personalization weights (later ADR; Experience Memory §7)

## Delivery record

- **Files/layers changed:** Slice 1 delivered; Stage A/B pending
- **Automated verification and result:** Slice 1 unit tests green (prior checkpoint)
- **Emulator/manual verification and result:** Pending Stage A
- **Known limitation or follow-up:** Model selection deferred to Stage A delivery
  record; Stage B awaits FC-03 sequencing
- **Documentation/traceability/ADR updates:** ADR-051;
  `CHANGE_CONTROL_ADR051_EVIDENCE_NATIVE_RECALL_RANKER.md`
- **Git commit:** _Pending Stage A_
