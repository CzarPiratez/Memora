# Change control: FC-02 Stage A S2 — fixture quality harness

**Date:** 2026-09-03  
**Type:** Measurement / androidTest + domain tokenizer (no product Find wire)  
**Decision guardrails:** Live/Dual **N = 0**. No Canonical Recall wire. No ORT
`implementation` promote. No AVAILABLE. No ADR-052 UI. No Stage B.

## Pre-work record

- **Requirement IDs:** FC-02 Stage A S2; ADR-051; model brief measurement plan;
  `meaning-pdf-page-recall-v1` E5d-boosted baseline 3/3.
- **Source documents read:** `ENGINEERING_CHARTER`, `GOVERNANCE`, `CONTINUE`,
  `RECALL_ENFORCEMENT_INDEX` (N=0), FC-02 change control, Stage A model brief,
  `MeaningPdfPageRecallCorpus`, S1 PASS evidence.
- **Current-code evidence inspected:** S1 ONNX spike PASS on SM-A156E; no Find
  wire; corpus labeledCases() 3 cues; M4 E5d-boosted 3/3.
- **Open ADRs / limitations:** S1 aggregate 1642 ms > S3 ≤800 ms (deferred to S3).
  Tokenizer must match BertTokenizer (`do_lower_case=true`).
- **Privacy:** Fixture texts only; model download = bytes only; no source re-read.
- **Smallest safe change:** Domain WordPiece + fixture scorer API; JVM unit tests;
  androidTest S2 harness reusing staged QInt8 model + vocab asset.
- **Acceptance criteria:**
  - [x] `BertWordPieceTokenizer` encodes query–passage pairs (`input_ids` /
        `attention_mask` / `token_type_ids`)
  - [x] `ScoreMeaningPdfPageRecallWithCrossEncoder` reports hit@1 vs E5d 3/3 bar
  - [x] JVM unit tests for tokenizer + fake-scorer bar logic
  - [x] Founder device: `RecallRankStageAFixtureIntegrationTest` green
        (2026-09-03 SM-A156E; after `stageALabeledCases()` mira near-miss fix;
        prior raw `labeledCases` run was 2/3)
  - [x] No product wire / no ORT main promote in this step
- **Test plan:** JVM unit tests; connected androidTest on Samsung (model already
  cached from S1 under no-backup staging).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall ranking measurement (ADR-051 Stage A S2)
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: future AnchorAwareMeaningRecallRanking → RecallRanker (not this step)
WHY THIS CONVERGES: Measured fixture quality before product wire
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a
EXTENDS LEGACY? no
```

## Delivery record

- **Files:** `BertWordPieceTokenizer.kt`, `ScoreMeaningPdfPageRecallWithCrossEncoder.kt`,
  `MeaningPdfPageRecallCorpus.stageALabeledCases()`, unit tests,
  `RecallRankStageAFixtureIntegrationTest`, vocab assets, this record.
- **Automated verification:** JVM unit tests (Android Studio).
- **Device verification:** **S2 PASS** 2026-09-03 SM-A156E. First runs 2/3 on
  ambiguous mira distractors; PASS after `stageALabeledCases()` near-miss fix.
  Archive Logcat (`MemoraRecallRankS2`):

```
fixture hits@1=3/3 pairs=11 wallMs=438 meetsS2=true maxLen=128
case mira-5page-p5 hit=true top=memora-open-5page.pdf#5 score=2.4340556 expected=memora-open-5page.pdf#5
case boarding-3page-p2 hit=true top=memora-open-3page.pdf#2 score=-0.10968849 expected=memora-open-3page.pdf#2
case invoice-5page-p2 hit=true top=memora-open-5page.pdf#2 score=0.9890115 expected=memora-open-5page.pdf#2
```

- **Next:** S3 midrange wallMs / DEGRADED_EXPLICIT decision.
- **Git commit:** When requested
