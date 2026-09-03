# FC-02 Stage A — S1 spike device runbook

**Host:** Samsung SM-A156E (`midrange_arm64`) preferred; any arm64 debug device OK  
**Status:** Engineering gate — not AVAILABLE  
**Authority:** `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md`, ADR-051, model brief  
**Does not:** wire Canonical Recall; ship ADR-052 onboarding; promote ORT to main APK

## Pre-work (already landed in tree)

- Domain: `RecallRankDevicePolicy`, `RecallRankCapabilitySupportPolicy`,
  `RecallRankAiPackTrack`, `OnnxMsMarcoMiniLmCrossEncoderSpec`
- Data: `AndroidRecallRankDeviceSignals`
- androidTest: `RecallRankStageASpikeIntegrationTest`, `OnnxCrossEncoderSpikeSupport`
- Gradle: `onnxruntime-android:1.28.0` on **`androidTestImplementation` only**
- JVM: `RecallRankDevicePolicyTest`

## Architectural boundary

```
ARCHITECTURAL BOUNDARY: Canonical Recall ranking (ADR-051 Stage A spike only)
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: future AnchorAwareMeaningRecallRanking → RecallRanker (not this run)
WHY THIS CONVERGES: Measured semantic head before product wire
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a (no new Find path)
EXTENDS LEGACY? no
```

## Steps

### 1. JVM unit tests (optional on this PC)

In Android Studio: run `RecallRankDevicePolicyTest` (module `:app`).

### 2. Connected instrumentation (required)

1. Unlock phone; USB debugging on; select the physical device.
2. Run only:
   `com.memora.app.domain.intelligence.RecallRankStageASpikeIntegrationTest`
3. Open Logcat; filter tag **`MemoraRecallRankS1`**.

### 3. What to record

From the tier test (always runs):

| Field | Example |
|---|---|
| abi | arm64-v8a |
| ramMb | (device) |
| executionTier | FULL / REDUCED / IDENTITY_ONLY |
| pool | 40 / 20 / 0 |

From the ONNX spike (skipped on IDENTITY_ONLY or if model cannot stage):

| Field | Example |
|---|---|
| pairs | 20 or 40 |
| totalMs | wall clock |
| avgPairMs | per pair |
| lastScore | any finite float |

Model staging (automatic if network allowed in instrumentation): HTTPS from
`OnnxMsMarcoMiniLmCrossEncoderSpec.DOWNLOAD_URL`, or push:

```text
adb push path\to\model.onnx /data/local/tmp/msmarco_minilm_l6_cross_encoder_qint8_v1.onnx
```

### 4. Pass / fail

| Result | Meaning |
|---|---|
| Tier log present + assertions green | Policy path PASS |
| ONNX spike green + avgPairMs recorded | Semantic-head load PASS (still not product wire) |
| ONNX assumeTrue skip (no model / identity-only) | Tier-only result — re-run with model staged before promoting ORT |
| Crash / native load failure | STOP — do not promote ORT; record in FC-02 delivery |

### 5. After PASS

1. Paste Logcat lines into FC-02 delivery record (or attach under `docs/artifacts/`).
2. Check model brief S1 box.
3. Next engineering: **S2 fixture** (`meaning-pdf-page-recall-v1`) — not ADR-052 UI.

### Device evidence — Samsung SM-A156E (2026-09-03) — S1 PASS

```
abi=arm64-v8a ramMb=7562 emulator=false executionTier=FULL pool=40 supportTier=SUPPORTED
onnxSpike download ok bytes=23180880
onnxSpike pairs=40 totalMs=1642 avgPairMs=41.05 inputs=[attention_mask, input_ids, token_type_ids] lastScore=-1.7289984
```

S3 ≤800 ms aggregate bar remains open (1642 ms measured on dummy tokens).

## Explicit non-claims

- Not marketing AVAILABLE
- Not Stage A product complete
- Not Stage B / evidence-native public claim
- Not smart-automatic download shipped (ADR-052 policy only)
