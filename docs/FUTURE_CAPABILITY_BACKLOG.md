# Future capability backlog

**Status:** Deferred options and explicit out-of-scope items (not authorized work)  
**Updated:** 2026-08-30  
**Authority:** Informs planning only. Does **not** authorize MIG-* steps, Freeze
reopen, Grounded Answers code, new Find paths, or ADRs by itself.  
**Companion:** Adjacent landscape review (StratoSort / enterprise retrieval
patterns) — recorded here for later gates, not as product commitments.

When an item graduates to authorized work, record it in change control / an ADR
and remove or update its row here.

---

## How to use this file

| Column | Meaning |
|---|---|
| **Layer** | `Core` (substrate) · `App` (product surface) · `Both` |
| **Status** | `candidate` (may build when gate passes) · `deferred` (benchmark/ADR first) · `out-of-scope` (do not pursue without new ADR) |
| **Gate** | What must be true before implementation |

**Already authoritative elsewhere — do not duplicate as new backlog:**

- MIG-07B — hybrid recall inside `CanonicalRecall`
- MIG-05 claim B — non-PDF evidence embeddings
- MIG-11 — ANN-ready embedding store interface shape
- Grounded Answers — `GROUNDING_ARCHITECTURE.md` + readiness §14
- Links / Event / Knowledge — ADR-018 + Experience Memory Amendment
- Embedding swap policy — `LOCAL_AI_TECHNICAL_SPEC.md` §11 + AI Pack ledger

---

## Tier A — High value candidates (likely build)

| ID | Capability | Layer | Status | Gate | Notes |
|---|---|---|---|---|---|
| FC-01 | **Reciprocal Rank Fusion (RRF)** for keyword + meaning lists | Core | candidate | MIG-07B authorized | Named fusion inside Canonical Recall ranking; no parallel Find path |
| FC-02 | **Evidence-native on-device RecallRanker** (ADR-051): Stage A semantic head + Stage B structured signals over top-k `MemoryEvidence` | Core | candidate | After MIG-07B; ADR-051; `RecallRanker` port (slice 1 done) | Bounded (40–50); Stage A ≠ full product claim; Stage B needs FC-03 |
| FC-03 | **Evidence chunking policy** per asset type (PDF page, OCR block, note section) | Core | candidate | Before MIG-05 B wide rollout; Grounded Answers eval corpus; **before ADR-051 Stage B complete** | Stable passage boundaries at index time; anchors on every evidence slice |
| FC-04 | **Corpus completeness honesty** in UI (indexed / pending / blocked counts) | App | **delivered** (2026-08-31) | App UX milestone; permission/disclosure patterns exist | `CHANGE_CONTROL_FC04_CORPUS_COMPLETENESS_HONESTY.md` |
| FC-05 | **Zero-egress / air-gap verification pack** (checklist + optional CI assertion) | Core | candidate | Class A / commercial readiness; no product feature required first | Provable core path never egresses; supports regulated buyers |
| FC-06 | **Evidence lineage in Explain** (extraction schema, model id/version, memory revision) | Both | candidate | After recall convergence; Explain MVP enhancement | Substrate largely exists; surface in App |

---

## Tier B — Deferred options (benchmark- or ADR-gated)

| ID | Option | Layer | Status | Gate | Notes |
|---|---|---|---|---|---|
| FD-01 | **Embedding models:** BGE-small, GTE-small, E5-small, multilingual E5 | Core | **BGE wins bake-off** (ADR-055 slice 3) | Slice 4 disclosed swap + reindex | Founder device 2026-09-21: BGE gold@60=11 vs USE 0. Plan: `CHANGE_CONTROL_MEANING_RETRIEVAL_STACK.md` |
| FD-02 | **On-device SLM for Ask:** Gemma 2B, Phi-3-mini, Llama 3.2 1B–3B | Core | deferred | `GROUNDING_ARCHITECTURE.md` §14 gates 9–10, 12, 14 | ReasoningEngine selection; not MVP chat |
| FD-03 | **Inference runtime:** ONNX Runtime Mobile, ExecuTorch, platform NNAPI/QNN/Core ML | Core | deferred | After FD-02 selection + adversarial eval | Avoid defaulting to llama.cpp for all capabilities |
| FD-04 | **ANN backends:** sqlite-vec/vss, USearch, hnswlib, ObjectBox vector | Core | deferred | MIG-11 interface reshape; M4-style scale measurement | Brute-force correct until benchmarks prove need |
| FD-05 | **CRAG / Self-RAG-style corrective retrieval** (re-retrieve on low grade) | Core | deferred | Grounded Answers v1 shipped; Verifier extension only | Inside Verifier phase; not generic RAG loop |
| FD-06 | **ColBERT / late interaction retrieval** | Core | deferred | Desktop / high-resource Core surface only | Too heavy for phone MVP |
| FD-07 | **Evidence-linked graph visualization** | App | deferred | ADR-018 Connect authorized | UI for Links/Event/Knowledge — not a Core graph engine |

---

## Tier C — Explicit out of scope (record to prevent scope creep)

| ID | Item | Reason |
|---|---|---|
| X-01 | StratoSort-style **intelligent file routing** | Violates read-only Asset invariant |
| X-02 | **GraphRAG** (Microsoft-style community summaries) | Different problem than evidence-backed Links (ADR-018) |
| X-03 | **Multi-agent orchestration** in Core | Assistants/companions are apps on Core (Class A / ADR-046) |
| X-04 | **SCAN / neuromorphic / spiking / psychometric** research tracks | No product intent; ADR-044 covers event-driven lifecycle only |
| X-05 | **Generic knowledge graph** as Core primitive | Memory + Evidence + future Links suffice |
| X-06 | **Chat-first / opaque “AI” entry** hiding Find | `GROUNDING_ARCHITECTURE.md` §16; Find remains safety rail |
| X-07 | **StratoSort / SCAN as architecture templates** | Adjacent reference only — retrieval UX and air-gap ops, not substrate design |

---

## Adjacent landscape (reference only)

**StratoSort** (privacy-first desktop: semantic search, RAG, graph viz, file routing):

- **Worth studying:** hybrid retrieval UX, air-gapped deployment discipline, honest
  partial-result presentation.
- **Do not import:** file routing, desktop-first assumptions, unconstrained RAG chat.

**Enterprise retrieval patterns** (Elastic, Vespa, hybrid search):

- **Worth studying:** RRF, cross-encoder rerank stages, citation-first answer UX.
- **Implement via:** Canonical Recall convergence (MIG-07B), not parallel pipelines.

**UNFYND differentiator (keep):** Evidence Package → Verifier → abstain; immutable
evidence lineage; zero-egress core path; open Class A contracts.

---

## Suggested sequencing (when gates open)

```text
Now (authorized lanes)     → Dual-track Checkpoint 1 (MIG-05 B photo/screenshot + MIG-07B Slice 1)
Next checkpoints         → MIG-07B Slice 2, FC-04, MIG-07B Slice 3, notes MIG-05 B
Next candidates (Tier A)   → FC-02 Stage A (ADR-051), FC-03, FC-02 Stage B, FC-01 RRF
Trust packaging            → FC-05 zero-egress, FC-04 corpus honesty, FC-06 lineage
Grounded Answers           → existing §14 gates; FD-02/03 after model choice
Scale                      → MIG-11 → FD-04 ANN when measured
Connect / graph UI         → ADR-018 → FD-07 only
```

---

## References

- `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` (MIG-05, MIG-07B, MIG-11)
- `docs/GROUNDING_ARCHITECTURE.md`
- `docs/LOCAL_AI_TECHNICAL_SPEC.md` §11
- `docs/CORE_APP_SEPARATION_PLAN.md`
- `docs/UNFYND_VISION_ALIGNMENT.md`
- `docs/DECISIONS.md` ADR-018, ADR-043, ADR-046, ADR-050
- `public/unfynd-core/APPLICATIONS.md`
