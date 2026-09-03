# FC-02 Stage A — on-device recall-ranker model brief

**Date:** 2026-09-02  
**Status:** **Recommendation for approval** — no App wiring in this document  
**Governing ADR:** ADR-051 (Evidence-native on-device RecallRanker); **ADR-052** (launch onboarding)  
**Change control:** `docs/CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md` (Stage A)  
**Program:** `docs/POST_MVP_PROGRAM_V1.md` P2  
**Does not authorize:** marketing AVAILABLE, Stage B, or a second Find path.

## Purpose

Select a **measured default** semantic head for ADR-051 **Stage A**: an on-device
relevance model behind `RecallRanker` that reranks a bounded pool of stored
`MemoryEvidence` excerpts inside Canonical Recall. This brief upgrades FC-02
from “generic cross-encoder” to an explicit, benchmark-gated pack choice for
Galaxy A15–class hardware (`midrange_arm64`).

## Decision summary (recommended)

| Role | Choice |
|---|---|
| **Default (Stage A)** | **ONNX Runtime Mobile** + **MS MARCO MiniLM-L6 cross-encoder** (QInt8, ~23 MiB weights) |
| **Fallback A** | Same architecture, float32 ONNX (~87 MiB) — only if QInt8 quality fails fixture bar |
| **Fallback B** | `IdentityRecallRanker` — required when pack missing, corrupt, or latency budget exceeded |

**Do not select for Stage A:** MediaPipe TextEmbedder (wrong architecture — bi-encoder,
not cross-encoder), cloud rerank APIs, SLM/LLM listwise rankers, BGE-reranker-base
or larger cross-encoders, bundled-in-APK weights (conflicts with Spec §6 small-APK /
AI Pack pattern).

**Founder approval:** **Accepted 2026-09-02** (broader-coverage path). S1 spike
**PASS** on SM-A156E (2026-09-03).

---

## Launch-ready install (ADR-052 — smart automatic)

At **marketing AVAILABLE**, users do **not** hunt separate “download meaning model” /
“download rerank pack” tabs. One **unified onboarding** step:

1. Combined disclosure (total size, network = model bytes only, licenses).
2. User taps **Continue** once.
3. App auto-downloads **meaning pack (USE)** on capable phones.
4. App auto-downloads **rerank pack** when tier is FULL or REDUCED (same flow).
5. App auto-starts meaning index build with progress (batch limits may show
   “continue indexing” later — not a separate engineer-only screen).

**Interim** `AiPackDisclosure` multi-tap flow remains for engineering proof only.

---

## Broader phone coverage (binding for Stage A)

Meaning Find **never requires** rerank weights. Coverage is tiered:

| RAM (device) | Primary ABI | Execution tier | Pool | Rerank at launch |
|---|---|---|---|---|
| ≥ 6 GiB | arm64-v8a | FULL | 40 | Auto after unified Continue |
| 4–6 GiB | arm64-v8a | REDUCED | 20 | Auto after unified Continue |
| < 4 GiB | any supported | IDENTITY_ONLY | 0 | Skipped — meaning may still install |
| ≥ 4 GiB | armeabi-v7a | REDUCED | 20 | Auto (slower) |
| Unsupported | riscv / unknown | IDENTITY_ONLY | 0 | Skipped |

Code: `RecallRankDevicePolicy`, `RecallRankCapabilitySupportPolicy`,
`AndroidRecallRankDeviceSignals`, `RecallRankAiPackTrack`.

ONNX Runtime ships **arm64-v8a + armeabi-v7a + x86*** — emulator and legacy 32-bit
ARM included; identity-only tier protects very low-RAM hosts.

---

## Evaluation criteria (from ADR-051 / FC-02)

| Criterion | Bar |
|---|---|
| License | Apache-2.0 (or equivalent documentable OSS); upstream + runtime notices |
| Pack size | Target **≤ 32 MiB** on disk (weights + tokenizer assets); hard ceiling **64 MiB** (same as USE) |
| Offline | Inference on stored excerpts only; download = model bytes only |
| Latency | Rerank default pool **40** on `midrange_arm64`; target **≤ 800 ms** aggregate rerank stage or honest degraded copy / identity fallback |
| Quality | Measurable lift on `meaning-pdf-page-recall-v1` (or successor) vs Stage A identity-only on same corpus |
| Architecture | Behind existing `RecallRanker.rank`; wire in `AnchorAwareMeaningRecallRanking`; Live/Dual **N** unchanged |
| Fallback | `IdentityRecallRanker` when pack unavailable |

---

## Candidates considered

### C1 — ONNX Runtime Mobile + MS MARCO MiniLM-L6 QInt8 (**recommended default**)

| Attribute | Value |
|---|---|
| Upstream | `cross-encoder/ms-marco-MiniLM-L-6-v2` (Apache-2.0) |
| Artifact | QInt8 ONNX ~22–23 MiB (e.g. upstream `onnx/model_qint8_avx512_vnni.onnx` or community `temsa/ms-marco-MiniLM-L-6-v2-onnx-cpu-qint8`) |
| Runtime | `com.microsoft.onnxruntime:onnxruntime-android` (new dependency — requires `docs/dependency-review/` record before merge) |
| Tokenizer | WordPiece (BERT-style); ship `vocab.txt` + config in rerank AI Pack (~1–2 MiB) |
| Inputs | `(query, passage)` pair; recommend **max_length 128** for mobile (excerpts are page-scale, not documents) |
| Pros | Industry-standard reranker; fits size ceiling; strong MS MARCO retrieval benchmarks; separates cleanly from USE embedding pack |
| Cons | New runtime dependency; pure-Kotlin (or bundled) tokenizer work; per-pair latency must be spike-measured on A15 |
| Stage A fit | **Best balance** of quality, size, and license |

### C2 — LiteRT / TFLite float32 MiniLM-L6 (community export)

| Attribute | Value |
|---|---|
| Artifact | e.g. `Bombek1/ms-marco-MiniLM-L-6-v2-litert` (~87 MiB float32 `.tflite`) |
| Runtime | LiteRT interpreter or TFLite (no MediaPipe task wrapper for cross-encoder) |
| Pros | Stays in TFLite family; no ONNX dependency |
| Cons | **Exceeds practical size budget** for a second pack on midrange phones; community conversion provenance weaker than upstream ONNX; still needs custom tokenizer |
| Stage A fit | **Fallback A only** if ONNX path blocked (policy/legal), not default |

### C3 — MediaPipe TextEmbedder / USE “rerank by similarity”

| Attribute | Value |
|---|---|
| Idea | Re-embed query+passage or compare embeddings creatively |
| Pros | Reuses installed USE pack |
| Cons | **Wrong architecture** — bi-encoder cannot score joint query–passage relevance; ADR-051 Stage A requires a true relevance head; would not meet FC-02 acceptance |
| Stage A fit | **Reject** |

### C4 — Larger cross-encoder (MiniLM-L12, BGE-reranker-base, GTE-reranker)

| Attribute | Value |
|---|---|
| Pros | Higher benchmark NDCG on desktop |
| Cons | 2–4× size and latency; poor fit for 40-wide pool on A15; violates smallest-safe-change |
| Stage A fit | **Reject** for phone MVP (revisit for desktop Core surface under FD-06 only) |

### C5 — Defer Stage A (identity-only)

| Attribute | Value |
|---|---|
| Pros | Zero new pack/runtime |
| Cons | No FC-02 Stage A delivery; cosine + lexical + anchors only — misses planned P2 quality jump |
| Stage A fit | **Fallback B** when pack missing (already required), not the product path |

---

## Recommended pack identity (proposed)

| Field | Proposed value |
|---|---|
| `modelId` | `onnx-msmarco-minilm-l6-cross-encoder` |
| `version` | `qint8-v1` (until measured artifact hash frozen) |
| Pack track | **Separate rerank AI Pack** from embedding USE pack (mirrors ADR-039 separation — user may have embeddings without rerank) |
| Disclosure ceiling | ~30 MiB weights + ~2 MiB tokenizer assets; **64 MiB** hard reject |
| Pool | Default **40**, max **50** per FC-02; spike may justify **20** pre-rerank cap with identity order for tail if latency fails |

---

## Privacy / security / license

- **Privacy:** Scores already-stored `MemoryEvidence` excerpts in Room — no new source
  reads; no content egress; aggregate benchmark logs only (per `LOCAL_AI_BENCHMARK_PLAN`).
- **Download:** HTTPS model bytes only; integrity hash in AI Pack manifest (same pattern
  as USE).
- **License:** Upstream `cross-encoder/ms-marco-MiniLM-L-6-v2` is Apache-2.0; ONNX
  Runtime Mobile is MIT. Record both in `docs/THIRD_PARTY_NOTICES.md` at implementation.
- **Clear data:** Clearing index / uninstalling rerank pack removes weights; Find
  falls back to `IdentityRecallRanker` + existing meaning stages.

---

## Implementation implications (next slice — not in this brief)

1. **Dependency:** Add `onnxruntime-android` with `docs/dependency-review/onnxruntime-android-*-review.md`.
2. **Domain:** `OnnxCrossEncoderRecallRanker` (or similar) implementing `RecallRanker`.
3. **Pack:** `RecallRankAiPackTrack` + disclosure + ledger entry (reuse embedding-first
   patterns; separate pack id).
4. **Tokenizer:** Minimal WordPiece for `cross-encoder/ms-marco-MiniLM-L-6-v2` vocab
   (JVM unit tests with golden token ids from HF reference).
5. **Wiring:** `AnchorAwareMeaningRecallRanking` after token boost, before lexical filter
   **or** after lexical filter — **spike must pick one** (recommend **after lexical
   filter** so rerank pool is smaller and higher precision).
6. **Concurrency:** Synchronized inference (mirror `MediaPipeEmbeddingEngine` lock).
7. **Stage B:** This brief does **not** implement evidence-native signals — Stage A only.

---

## Measurement plan (before merge to product path)

| Step | Host | Pass bar |
|---|---|---|
| **S1 Spike** | JVM + optional `androidTest` on A15 | Load QInt8 ONNX; score 40 synthetic pairs; no crash; p50 pair latency recorded |
| **S2 Fixture** | JVM harness | `meaning-pdf-page-recall-v1` hit@1 **≥** E5d-boosted baseline (3/3) with rerank enabled |
| **S3 Midrange** | Samsung SM-A156E (`midrange_arm64`) | Aggregate rerank wallMs for 40 passages; ≤ 800 ms or document `DEGRADED_EXPLICIT` + identity fallback path |
| **S4 Regression** | Unit + device | A-01 offline Find still PASS; identity fallback when pack absent |

Existing M4 baseline (USE, no rerank): cosine-only **2/3**, E5d-boosted **3/3** on
`meaning-pdf-page-recall-v1`. Stage A should aim to match or beat boosted **3/3**
**without** relying on rerank to fix broken chunking.

---

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| 40 × cross-encoder too slow on A15 | Cap `max_length` 128; batch if ONNX graph supports; reduce pool to 20; async rank with progress only if product accepts |
| QInt8 ranking drift vs float | Compare top-1 on fixture; fall back to float32 pack only if bar missed |
| New ONNX native lib size | Measure APK + install size in change control; NNAPI optional later (FD-03) |
| Tokenizer bugs | Golden tests vs Hugging Face reference tokens for fixture pairs |
| User confusion (two packs) | ADR-052 unified onboarding — one Continue, combined disclosure |

---

## Launch onboarding (ADR-052)

See `docs/CHANGE_CONTROL_ADR052_SMART_AUTOMATIC_AI_PACK_ONBOARDING.md`. Rerank is
not a second product journey at launch.

---

## Alternatives explicitly deferred

| Option | Gate |
|---|---|
| Embedding swap (BGE/E5/GTE) | FD-01 — separate from rerank |
| FC-01 RRF | Own change control |
| FC-03 chunking | Hard dependency for Stage B; may run parallel with Stage A spike |
| Stage B evidence-native features | After Stage A green |
| Distilled custom reranker on fixture corpus | Post–Stage A innovation ADR |

---

## Approval checklist

- [x] Founder accepts **C1 (ONNX + MiniLM-L6 QInt8)** as Stage A default (2026-09-02)
- [x] Broader coverage: ADR-052 smart automatic install on eligible tiers +
      `RecallRankDevicePolicy`
- [x] **S1 spike** green on `midrange_arm64` (2026-09-03 SM-A156E: FULL/40;
      pairs=40 totalMs=1642 avgPairMs=41.05; download 23180880 bytes)
- [x] Engineering opened **S2 fixture** (tokenizer + harness)
- [x] S2 device hit@1 **3/3** on SM-A156E (2026-09-03; `stageALabeledCases`)
- [x] Engineering opened **S3 latency** harness + `RecallRankLatencyPolicy`
- [x] S3 device disposition recorded (`MemoraRecallRankS3`): **DEGRADED_EXPLICIT**
      effectivePool=20 (full40=1517 ms; reduced20@96=584 ms)
- [ ] Dependency promoted from `androidTest` to `implementation` with Stage A wire

**After S3 disposition:** Stage A product wire (effective pool 20 under
DEGRADED_EXPLICIT) → ADR-052 unified onboarding UI (after wire).
