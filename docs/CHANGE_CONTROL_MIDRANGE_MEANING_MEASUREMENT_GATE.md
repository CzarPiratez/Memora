# Change control: Midrange meaning measurement gate (M4 plan)

## Pre-work record

- **Requirement IDs:** ADR-024/025; Spec §11; LOCAL_AI_BENCHMARK_PLAN
  `midrange_arm64`; enterprise completion §B midrange row.
- **Source documents read:** GOVERNANCE, CONTINUE, M1–M3 / E4b change controls,
  LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY, LOCAL_AI_BENCHMARK_PLAN, ADR-024/032.
- **Current-code evidence inspected:** Product USE embedder (ADR-032); M3
  emulator cosine-only **2/3** / boosted **3/3**; Find-by-meaning remains
  candidate copy; no midrange device row exists.
- **Open ADRs / limitations:** Emulator evidence must not authorize midrange
  marketing AVAILABLE. Physical `midrange_arm64` host is required for M4 code
  execution. This slice is **plan + honesty matrix update only** — no AVAILABLE
  UI flip, no EmbeddingGemma.
- **Privacy:** Same labeled corpus `meaning-pdf-page-recall-v1` (fixture texts);
  aggregate metrics only; no user content in logs.
- **Smallest safe change:** Durable M4 runbook + compatibility matrix honesty
  rows; CONTINUE/ROADMAP/enterprise checklist pointers. No product code.
- **Acceptance criteria:**
  1. M4 steps documented (install USE → run instrumentation or equivalent →
     record hit@1 / model / tier / wallMs).
  2. Compatibility policy distinguishes emulator candidate evidence vs pending
     midrange SUPPORTED/AVAILABLE.
  3. Explicit product decision checklist before any AVAILABLE copy change.
  4. Docs/CONTINUE point here; NOW remains “await midrange host or choose
     next wow slice.”

## M4 runbook (when `midrange_arm64` device is available)

1. Install debug Memora; disclose → download USE; rebuild meaning index (or run
   `MeaningPdfPageRecallUseBaselineIntegrationTest` preferring product USE).
2. Record aggregates only (log tag `MemoraMeaningPdfM3` / M4 successor):
   model id@version, tier `midrange_arm64`, cosine-only hit@1, boosted hit@1,
   embed count, wallMs bucket.
3. Fill delivery record in this file; update enterprise checklist §B midrange.
4. Product decision (separate tip): whether UI may say measured AVAILABLE for
   embedding/recall on that tier — never invent latency/battery SLAs.

## Product decision checklist (before AVAILABLE copy)

- [ ] Midrange M4 row recorded with `hasMeasuredEvidence=true` for RECALL_AT_K
      (and related) on `midrange_arm64` + corpus id.
- [ ] Compatibility matrix row for EMBEDDING / RECALL_RANKER updated from
      pending to the honest tier (SUPPORTED or DEGRADED_EXPLICIT with named
      limitation).
- [ ] UI copy reviewed against ADR-024 (no silent keyword fallback labeled as
      meaning; E5d assist still disclosed if enabled).
- [ ] Founder/product accepts AVAILABLE wording for that device class only.

## Delivery record

- **Status:** Accepted (plan / honesty) — 2026-08-04
- **Delivered:** This runbook; compatibility policy USE/emulator honesty note;
  CONTINUE/ROADMAP/enterprise pointer. No AVAILABLE flip.
- **Not delivered:** Physical midrange measurement; AVAILABLE UI; EmbeddingGemma.
- **Git commit:** (filled at close)
- **Honesty:** Code/emulator path ✅ (M1–M3). Founder midrange device row ⬜
  until M4 executes.
