# Change control: ADR-051 Evidence-native on-device RecallRanker

**Date:** 2026-09-02  
**Type:** Architecture / ranking decision (docs)  
**Decision guardrails:** Docs + program alignment only in this step. Do not
rewrite hashed Freeze / Migration Spec / Local AI Spec / Grounding / Experience
Memory / Product Contract blobs. Do not claim marketing AVAILABLE. Do not bind a
concrete model file or start App wiring in this step. Live/Dual **N** unchanged.
No Grounded Answers, Act, Connect, or cloud rerank. No new Find path.

## Pre-work record

- **Requirement IDs:** Spec `RecallRanker`; ADR-049 Canonical Recall; FC-02;
  Local AI Spec §4 / §9 retrieval signals; Freeze §3 one ranking boundary;
  `POST_MVP_PROGRAM_V1` P2; Experience Memory Amendment §7 (personalization
  deferred).
- **Source documents read:** `ENGINEERING_CHARTER`, `GOVERNANCE` pre-work gate,
  `CONTINUE`, `DECISIONS` (ADR-049 / ADR-050), `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK`,
  `FUTURE_CAPABILITY_BACKLOG` FC-02/FC-03, `POST_MVP_PROGRAM_V1` P2,
  `RecallRankContracts.kt`, `IdentityRecallRanker.kt`, `LocalIntelligenceEngines.kt`.
- **Current-code evidence inspected:** `RecallRanker.rank` + `IdentityRecallRanker`
  landed (FC-02 slice 1); no cross-encoder runtime; meaning ranking still token
  boost + lexical filter + anchors in `AnchorAwareMeaningRecallRanking`.
- **Open ADRs / platform limitations checked:** ADR-049 places `RecallRanker`
  inside Canonical Recall. FC-02 authorized but text-only industry baseline is
  insufficient for UNFYND’s evidence-first product. Model pack not selected —
  Stage A remains measured choice under FC-02.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Docs only this step. Future stages: on-device inference on stored excerpts;
  pack download = model bytes only; no source re-read at rank time.
- **Smallest safe change:** Accept ADR-051; update FC-02 / P2 / backlog /
  registry / CONTINUE / changelog / this record. Zero App code.
- **Acceptance criteria:**
  - ADR-051 Accepted after ADR-050
  - Evidence-native thesis + Stage A / Stage B binding
  - FC-03 hard dependency before Stage B complete
  - Type-agnostic ranker / type-specific ingress rule
  - Explicit non-authorization of AVAILABLE, cloud rerank, SLM-as-Find, hashed
    rewrites, App wiring in this step
  - FC-02 change control reinterprets slice 2 as Stage A and records Stage B
- **Test and emulator verification plan:** Docs inspection; no Kotlin/XML/
  Gradle/schema in this change set.
- **User-visible quality/accessibility review plan:** N/A (no UI this step).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall (ADR-049) → RecallRanker (ADR-051)
CURRENT LEGACY PATH (L# or none): none (N=0 for product Find after convergence)
TARGET PATH: AnchorAwareMeaningRecallRanking → Evidence-native RecallRanker
WHY THIS CONVERGES: One ranking port; semantic head + structured evidence signals
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: ad-hoc score tweaks outside RecallRanker
EXTENDS LEGACY? no
```

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (ADR-051)
  - `docs/CHANGE_CONTROL_ADR051_EVIDENCE_NATIVE_RECALL_RANKER.md` (this file)
  - `docs/CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md` (Stage A/B under ADR-051)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (authority item 12)
  - `docs/POST_MVP_PROGRAM_V1.md` (P2 cite ADR-051)
  - `docs/FUTURE_CAPABILITY_BACKLOG.md` (FC-02 note)
  - `docs/MVP_EXIT_AUDIT.md` (next actions)
  - `CONTINUE.md`
  - `docs/CHANGELOG.md`
- **Automated verification and result:** Docs inspection; no App code.
- **Emulator/manual verification and result:** Not required (docs-only).
- **Known limitation or follow-up:** Next engineering step = Stage A model
  options brief + measured pack choice under FC-02; Stage B after / with FC-03.
- **Documentation/traceability/ADR updates:** ADR-051; this record.
- **Git commit:** Local checkpoint when requested (no push).
