# Change control: On-device USE PDF page-recall baseline (M3)

## Pre-work record

- **Requirement IDs:** ADR-024/025; ADR-032; enterprise completion §B/§C; Spec §11.
- **Source documents read:** GOVERNANCE, CONTINUE, M1/M2/E4b change controls,
  enterprise checklist, MediaPipe USE product path.
- **Current-code evidence inspected:** M2 measured compact average-word
  cosine-only **0/3** / boosted **3/3**; E4b smoke `mira` → page 5 on USE;
  product store installs USE; M2 harness stages average-word into disposable dir.
- **Open ADRs / limitations:** Emulator tier ≠ midrange AVAILABLE. M3 measures
  USE on the same labeled corpus. E5d boost may remain or become optional based
  on cosine-only rates.
- **Privacy:** Fixture corpus texts only; aggregate logs; disposable staging or
  product USE file — never upload user content.
- **Smallest safe change:** Instrumentation scores `meaning-pdf-page-recall-v1`
  with live USE cosines; report hit@1 cosine-only vs boost; docs; no AVAILABLE
  flip; no EmbeddingGemma.
- **Acceptance criteria:**
  1. Live USE embeddings (model id `mediapipe-universal-sentence-encoder`).
  2. Page hit@1 cosine-only and boosted recorded for `emulator_medium_phone`.
  3. Decision note: whether E5d assist still required for labeled @1.
  4. Unit path still green; instrumentation green; delivery record filled.
  5. No product AVAILABLE UI change.

## Delivery record

- **Status:** Accepted (emulator_medium_phone M3) — 2026-08-04
- **Delivered:**
  - `MeaningPdfPageRecallUseBaselineIntegrationTest` — stages product USE (or
    download) into disposable dir; measures live hit@1.
  - Prefer product `universal_sentence_encoder_float32_1.tflite` (E4b install).
- **Measured outcome (log `MemoraMeaningPdfM3`):**
  - Model: `mediapipe-universal-sentence-encoder@float32-1`
  - Tier: `emulator_medium_phone`
  - Corpus: `meaning-pdf-page-recall-v1`
  - Cosine-only hit@1: **2/3** (improved vs M2 compact **0/3**)
  - E5d-boosted hit@1: **3/3**
  - Interim semantic-only bar (<0.67) still flags further pack interest;
    **keep E5d disclosed assist** for labeled full recovery.
  - Embeds: 14; wallMs: 5039 (aggregate only; not a release latency SLA)
- **Decision:** Do not remove E5d. Do not flip AVAILABLE. EmbeddingGemma (or
  other larger pack) remains a separate optional follow-up — not a silent
  scope cut.
- **Explicitly not delivered:** Product AVAILABLE; midrange_arm64; remove boost;
  EmbeddingGemma install.
- **Git commit:** (filled at close)
- **Documentation/traceability:** CONTINUE, CHANGELOG, enterprise checklist,
  embedding-first track, ROADMAP.
