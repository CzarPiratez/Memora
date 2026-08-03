# MediaPipe tasks-text dependency review (embedding E5b1 / 16 KB fix)

**Date:** 2026-08-03  
**Artifact:** `com.google.mediapipe:tasks-text:0.10.29`  
**Change control:** `docs/CHANGE_CONTROL_LOCAL_AI_EMBEDDING_FIRST_TRACK.md`  
**ADR:** ADR-031  
**Supersedes:** `mediapipe-tasks-text-0.10.14-review.md` (kept for history)

## Purpose

On-device text embedding via MediaPipe Text Embedder for Spec §4
`EmbeddingEngine`. The TFLite model is **not** packaged in the APK; it is
downloaded to app-private no-backup storage after user disclosure and
affirmative action.

## Why 0.10.29 (vs 0.10.14)

Emulator smoke on a **16 KB page-size** AVD (`sdk_gphone16k_x86_64`) crashed the
process immediately after a successful model download when
`TextEmbedder.createFromFile` loaded `libmediapipe_tasks_text_jni.so`. MediaPipe
issue tracker confirms 16 KB ELF alignment landed in tasks packages around
0.10.26+; **0.10.29** includes aligned x86 / x86_64 libs needed for this
emulator.

## Alternatives considered

| Option | Why not chosen for this slice |
|---|---|
| Stay on 0.10.14 + 4 KB-only AVD | Fails Google Play 16 KB requirement for targetSdk 35+ |
| Bundle TFLite in APK assets | Conflicts with AI Pack / small-APK rule (Spec §6) |
| Cloud embedding API | Forbidden for core recall (Addendum 1 / ADR-012) |

## Privacy / security

- Existing `INTERNET` (Notes) may download **model bytes only** — never Memories,
  OCR, or source content.
- Inference runs on-device after install; no remote inference.
- Clear Memora index deletes the private model file.
- Product reports AVAILABLE only after the model file loads successfully.

## Licence / notices

MediaPipe Tasks are Apache-2.0. Attribution noted in `docs/THIRD_PARTY_NOTICES.md`.
The downloaded embedder model is subject to Google’s published MediaPipe model
terms for the chosen artifact URL.

## Follow-up

Bump only with a fresh review. Larger/higher-quality embedder packs remain E4b /
later measured slices.
