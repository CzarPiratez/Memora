# Change control: Midrange meaning measurement gate (M4 plan + execute)

## Pre-work record

- **Requirement IDs:** ADR-024/025; Spec §11; LOCAL_AI_BENCHMARK_PLAN
  `midrange_arm64`; enterprise completion §B midrange row.
- **Source documents read:** GOVERNANCE, CONTINUE, M1–M3 / E4b change controls,
  LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY, LOCAL_AI_BENCHMARK_PLAN, ADR-024/032.
- **Current-code evidence inspected:** Product USE embedder (ADR-032); M3
  emulator cosine-only **2/3** / boosted **3/3**; Find-by-meaning remains
  candidate copy; midrange harness
  `MeaningPdfPageRecallUseMidrangeBaselineIntegrationTest` (log
  `MemoraMeaningPdfM4`).
- **Open ADRs / limitations:** Emulator evidence must not authorize midrange
  marketing AVAILABLE. Physical `midrange_arm64` host is required for M4
  execute. This slice measures and records; it does **not** flip AVAILABLE UI,
  does not change Live/Dual **N**, and does not install EmbeddingGemma.
- **Privacy:** Same labeled corpus `meaning-pdf-page-recall-v1` (fixture texts);
  aggregate metrics only; no user content in logs.
- **Smallest safe change:** Run M4 instrumentation on physical midrange; fill
  delivery record; update honesty matrix + enterprise checklist; keep AVAILABLE
  **NO** until separate product decision.
- **Acceptance criteria:**
  1. M4 steps documented (install USE → run instrumentation or equivalent →
     record hit@1 / model / tier / wallMs).
  2. Compatibility policy distinguishes measured midrange candidate evidence vs
     marketing AVAILABLE.
  3. Explicit product decision checklist before any AVAILABLE copy change.
  4. Docs/CONTINUE point here; AVAILABLE remains gated.

## M4 runbook (when `midrange_arm64` device is available)

1. Install debug Memora; disclose → download USE; rebuild meaning index (or run
   `MeaningPdfPageRecallUseMidrangeBaselineIntegrationTest` preferring product
   USE / `/data/local/tmp` / HTTPS staging).
2. Record aggregates only (log tag `MemoraMeaningPdfM4`):
   model id@version, tier `midrange_arm64`, cosine-only hit@1, boosted hit@1,
   embed count, wallMs bucket.
3. Fill delivery record in this file; update enterprise checklist §B midrange.
4. Product decision (separate tip): whether UI may say measured AVAILABLE for
   embedding/recall on that tier — never invent latency/battery SLAs.

## Product decision checklist (before AVAILABLE copy)

- [x] Midrange M4 row recorded with `hasMeasuredEvidence=true` for RECALL_AT_K
      (and related) on `midrange_arm64` + corpus id.
- [x] Compatibility matrix row for EMBEDDING / RECALL_RANKER updated from
      pending to the honest tier (`DEGRADED_EXPLICIT` — measured candidate; not
      marketing AVAILABLE).
- [ ] UI copy reviewed against ADR-024 (no silent keyword fallback labeled as
      meaning; E5d assist still disclosed if enabled).
- [ ] Founder/product accepts AVAILABLE wording for that device class only.

## Delivery record — plan (2026-08-04)

- **Status:** Accepted (plan / honesty) — 2026-08-04
- **Delivered:** This runbook; compatibility policy USE/emulator honesty note;
  CONTINUE/ROADMAP/enterprise pointer. No AVAILABLE flip.
- **Not delivered (at plan tip):** Physical midrange measurement; AVAILABLE UI;
  EmbeddingGemma.
- **Git commit:** `6bb9c89`

## Delivery record — execute (2026-08-29)

- **Status:** Accepted (physical `midrange_arm64` M4 measure) — 2026-08-29
- **Host:** Samsung Galaxy A15 (`SM-A156E`), serial `RZCX12KZ6EN`, Android 16,
  ABI `arm64-v8a`. Only connected device (no emulator).
- **Harness:**
  `MeaningPdfPageRecallUseMidrangeBaselineIntegrationTest`
  (`measures_use_page_recall_on_midrange_arm64_tier`) — **PASS**
- **Measured outcome (log `MemoraMeaningPdfM4`):**
  - Model: `mediapipe-universal-sentence-encoder@float32-1`
  - Tier: `midrange_arm64`
  - Corpus: `meaning-pdf-page-recall-v1`
  - Cosine-only hit@1: **2/3** (matches M3 emulator USE)
  - E5d-boosted hit@1: **3/3**
  - `recommendsE4b=true` (interim semantic-only bar still flags further pack
    interest); **keep E5d disclosed assist** for labeled full recovery
  - Embeds: 14; wallMs: 448 (aggregate only; **not** a release latency SLA)
- **Log artifact:**
  `docs/artifacts/MemoraMeaningPdfM4-RZCX12KZ6EN.logcat.txt`
- **Decision this tip:** Do **not** flip marketing AVAILABLE. Do **not** remove
  E5d. Live/Dual **N** unchanged (**6**). EmbeddingGemma remains optional later.
- **Explicitly not delivered:** Product AVAILABLE UI; latency/battery SLAs;
  EmbeddingGemma; MIG-06+; Canonical Recall API.
- **Honesty:** Code path + physical midrange row ✅. Founder AVAILABLE wording ⬜
  (checklist items 3–4).
