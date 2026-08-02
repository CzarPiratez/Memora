# MediaPipe tasks-text dependency review (embedding E5b1)

**Date:** 2026-08-03  
**Artifact:** `com.google.mediapipe:tasks-text:0.10.14`  
**Change control:** `docs/CHANGE_CONTROL_LOCAL_AI_EMBEDDING_FIRST_TRACK.md`  
**ADR:** ADR-031

## Purpose

On-device text embedding via MediaPipe Text Embedder for Spec §4
`EmbeddingEngine`. The TFLite model is **not** packaged in the APK; it is
downloaded to app-private no-backup storage after user disclosure and
affirmative action.

## Alternatives considered

| Option | Why not chosen for this slice |
|---|---|
| Bundle TFLite in APK assets | Conflicts with AI Pack / small-APK rule (Spec §6) |
| Wait for custom Memora AI Pack vendor | Blocks meaning-search progress indefinitely |
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
