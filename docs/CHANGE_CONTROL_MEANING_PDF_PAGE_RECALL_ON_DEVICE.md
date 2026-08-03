# Change control: On-device meaning PDF page-recall baseline (M2)

## Pre-work record

- **Requirement IDs:** ADR-024/025; enterprise completion §B; Spec §11;
  LOCAL_AI_BENCHMARK_PLAN device tier `emulator_medium_phone`.
- **Source documents read:** GOVERNANCE, CONTINUE, M1 change control, enterprise
  checklist, embedding-first track, MediaPipeAverageWordEmbedderSpec,
  MediaPipeEmbeddingEngine, L1/L2 instrumentation pattern.
- **Current-code evidence inspected:** M1 JVM harness with injected cosines;
  product embedder is compact average-word MediaPipe; E5d boost recovers labeled
  smoke; product model lives under `memora_embedding_models`.
- **Open ADRs / limitations:** M2 measures compact MediaPipe on the labeled
  fixture corpus. Emulator tier does **not** authorize midrange marketing
  AVAILABLE. E4b remains a separate vendor-pack slice.
- **Privacy:** Fixture texts only (corpus strings mirroring open PDFs). Aggregate
  logs only (hit counts, latency buckets, model id). Disposable model staging —
  never delete the user's product model install.
- **Smallest safe change:** Score labeled corpus via EmbeddingEngine → reuse M1
  measure helper; unit test with fake engine; androidTest stages compact model
  (tmp / product copy / HTTPS) into a **throwaway** dir, runs MediaPipe, records
  aggregates; docs; no UI AVAILABLE flip.
- **Acceptance criteria:**
  1. Live cosines from on-device embedder (not injected failure pattern).
  2. Report records page hit@1 cosine-only and boosted; model id/version; embed
     wall-time aggregate.
  3. Claims `hasMeasuredEvidence=true` for `emulator_medium_phone` + corpus id.
  4. Does not change product copy / AVAILABLE.
  5. Unit + instrumentation green; delivery record filled with measured rates.

## Delivery record

- **Status:** Accepted (emulator_medium_phone M2) — 2026-08-04
- **Delivered:**
  - `MeaningPdfPageRecallCorpus.labeledCases()` text corpus shared with M1.
  - `ScoreMeaningPdfPageRecallCorpus` + `MeasureOnDeviceMeaningPdfPageRecallBaseline`.
  - Unit tests: `ScoreMeaningPdfPageRecallCorpusTest` green.
  - Instrumentation:
    `MeaningPdfPageRecallOnDeviceBaselineIntegrationTest` green on
    Medium Phone (`emulator-5554`).
- **Measured outcome (log `MemoraMeaningPdfM2`):**
  - Model: `mediapipe-average-word-embedder@float32-1`
  - Tier: `emulator_medium_phone`
  - Corpus: `meaning-pdf-page-recall-v1`
  - Cosine-only hit@1: **0/3**
  - E5d-boosted hit@1: **3/3**
  - `recommendsE4bForSemanticOnly`: **true**
  - Embeds: 14; wallMs: 6031 (aggregate only; not a release latency SLA)
- **Explicitly not delivered:** Product UI AVAILABLE flip; midrange_arm64 row;
  E4b vendor pack selection/download.
- **Git commit:** (filled at close)
- **Documentation/traceability:** CONTINUE, CHANGELOG, enterprise checklist §B,
  embedding-first track, ROADMAP.
