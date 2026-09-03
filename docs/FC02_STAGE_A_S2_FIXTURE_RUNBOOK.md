# FC-02 Stage A — S2 fixture device runbook

**Host:** Samsung SM-A156E  
**Status:** Engineering gate after S1 PASS — not AVAILABLE  
**Authority:** `CHANGE_CONTROL_FC02_STAGE_A_S2_FIXTURE.md`, Stage A model brief  
**Bar:** Cross-encoder hit@1 **3/3** on `meaning-pdf-page-recall-v1` (≥ E5d-boosted)

## Pre-flight (docs)

- Live/Dual **N = 0** — no new Find path
- S1 PASS already recorded (model likely cached on phone)
- This step does **not** wire Find or ship ADR-052 UI

## Steps

1. Unlock phone; USB debugging; select **SM-A156E**.
2. Optional JVM (Android Studio): run `BertWordPieceTokenizerTest` +
   `ScoreMeaningPdfPageRecallWithCrossEncoderTest`.
3. Run androidTest class:
   `com.memora.app.domain.intelligence.RecallRankStageAFixtureIntegrationTest`
   (green ▶ on the **class**).
4. Logcat filter: **`MemoraRecallRankS2`**
5. Expect a line like:
   `fixture hits@1=3/3 pairs=… wallMs=… meetsS2=true maxLen=128`

## Pass / fail

| Result | Meaning |
|---|---|
| hits@1=3/3 + green | **S2 PASS** → open S3 |
| hits@1&lt;3/3 | Investigate scoring/tokenizer; do not wire Find |
| skip (no model) | Re-run S1 ONNX once to restage model |

## Explicit non-claims

Not AVAILABLE. Not Stage A product wire. Not Stage B. Not ADR-052 auto-download.
