# Change control: Meaning PDF page-recall baseline (M1)

## Pre-work record

- **Requirement IDs:** ADR-024/025; enterprise completion §B; Spec §11
  measurement honesty.
- **Source documents read:** GOVERNANCE, CONTINUE, enterprise completion
  checklist, LOCAL_AI_BENCHMARK_PLAN, measured pack baselines L0–L2 pattern,
  E5c/E5d change controls, fixtures README.
- **Current-code evidence inspected:** E5d quality smoke passed with token
  boost; compact cosine alone failed mira→page-5; L1/L2
  `MeasureSyntheticAiPackBaseline` pattern for claims with
  `hasMeasuredEvidence`.
- **Open ADRs / limitations:** M1 is a **labeled-corpus ranking harness** with
  injected cosine scores (reproducible JVM). It does **not** authorize UI
  AVAILABLE. On-device MediaPipe end-to-end measurement is **M2** (follow-up).
  E4b only after M1/M2 show semantic-only targets fail without boost.
- **Privacy:** Fixture PDFs only (`memora-open-*.pdf`); no user content.
- **Smallest safe change:** Corpus + `MeasureMeaningPdfPageRecallBaseline` +
  unit tests comparing cosine-only vs E5d boost page-hit rate; docs; no UI
  AVAILABLE flip.
- **Acceptance criteria:**
  1. Corpus id + labeled cue→expected asset/page cases from open fixtures.
  2. Report records page hit-rate @1 for cosine-only and boosted paths.
  3. Claims with `hasMeasuredEvidence=true` for this fixture corpus / JVM tier
     do not change product copy.
  4. Decision note: boost recovers labeled mira/page cases under compact-failure
     cosine pattern; E4b still open for semantic-only without boost.
  5. Unit tests green.

## Delivery record

- **Status:** Accepted (JVM harness M1) — 2026-08-04
- **Delivered:**
  - Corpus `meaning-pdf-page-recall-v1` in
    `MeaningPdfPageRecallCorpus` (mira / boarding / invoice labeled cases;
    texts mirror `MemoraApp/fixtures/memora-open-*.pdf`).
  - `MeasureMeaningPdfPageRecallBaseline` — page hit@1 cosine-only vs E5d
    evidence-token boost; claims `RECALL_AT_K` + `EXPLANATION_COVERAGE` with
    `hasMeasuredEvidence=true` for `jvm_unit_test` tier only.
  - `MeaningEvidenceTokenBoost` moved to **domain** (architecture: application
    and harness share one pure helper).
  - Unit tests: `MeasureMeaningPdfPageRecallBaselineTest`,
    `MeaningEvidenceTokenBoostTest` — green.
- **Measured outcome (injected-cosine harness):** cosine-only hit@1 = 0/3
  (recommends **E4b** for semantic-only); boosted hit@1 = 3/3.
- **Explicitly not delivered:** Product UI AVAILABLE flip; on-device MediaPipe
  end-to-end measurement (**M2**); E4b vendor pack selection.
- **Git commit:** `b1c39d9` (`feat: M1 JVM meaning PDF page-recall baseline harness`)
- **Documentation/traceability:** CONTINUE, CHANGELOG, enterprise checklist §B
  partial, fixtures README, embedding-first track pointer.
