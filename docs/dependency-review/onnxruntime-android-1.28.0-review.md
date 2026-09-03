# ONNX Runtime Android dependency review (FC-02 Stage A spike)

**Date:** 2026-09-02  
**Artifact:** `com.microsoft.onnxruntime:onnxruntime-android:1.28.0`  
**Change control:** `docs/CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md` (Stage A S1)  
**ADR:** ADR-051

## Purpose

Load and score **MS MARCO MiniLM-L6 cross-encoder** QInt8 ONNX on-device for
ADR-051 Stage A semantic head. Classpath scope: **`implementation`** after
S1–S3 measured green (2026-09-03); androidTest keeps the same artifact for
harnesses.

## Why 1.28.0

Latest stable `onnxruntime-android` on Maven Central at review time. Ships
native libs for **arm64-v8a**, **armeabi-v7a**, **x86**, **x86_64** — supports
midrange phones, legacy 32-bit ARM, and emulators without a separate artifact.

## Alternatives considered

| Option | Why not default |
|---|---|
| MediaPipe TextEmbedder | Bi-encoder only — not cross-encoder rerank |
| LiteRT float32 ~87 MiB TFLite | Exceeds rerank pack size budget |
| Bundle ONNX in APK | Conflicts with AI Pack / small-APK pattern (ADR-052: auto after consent, not in APK) |
| Cloud rerank API | Forbidden for core recall (ADR-012) |
| `implementation` on first merge | APK/native cost before measured spike — defer |

## Broader phone coverage (ADR-051 / ADR-052)

- **Rerank not required for meaning Find** — USE embedding path works without this dependency.
- **Smart automatic install** — on FULL/REDUCED tiers, unified onboarding auto-downloads
  rerank pack after one Continue (ADR-052); IDENTITY_ONLY skips rerank bytes.
- **Device tier policy** — `RecallRankDevicePolicy` maps RAM/ABI → pool 40 / 20 / identity-only.
- **ARM artifact** — Stage A uses CPU QInt8 ONNX validated on **arm64-v8a**, not x86-only AVX512 builds.

## Privacy / security

- Download **model bytes only** after disclosure — never Memories or source content.
- Inference on stored evidence excerpts on-device.
- Clear index / pack removal drops private model file.
- Spike logs aggregate latency only (`MemoraRecallRankS1` tag).

## Licence / notices

ONNX Runtime: **MIT**. Cross-encoder weights: **Apache-2.0** (upstream
`cross-encoder/ms-marco-MiniLM-L-6-v2`). Record both in `THIRD_PARTY_NOTICES.md`
when promoted to `implementation`.

## Release follow-up

When promoting to release builds with R8, add ProGuard keep rule per ONNX docs:

```
-keep class ai.onnxruntime.** { *; }
```

## Follow-up

- S1 spike on `midrange_arm64` (Samsung SM-A156E) — record pair latency + fixture lift
- Pin model integrity hash in rerank AI Pack manifest after spike
- Promote dependency to `implementation` only after FC-02 Stage A wire slice
