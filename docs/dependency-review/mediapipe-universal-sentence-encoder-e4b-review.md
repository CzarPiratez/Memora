# MediaPipe Universal Sentence Encoder model review (E4b / ADR-032)

**Date:** 2026-08-04  
**Artifact URL:**
`https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite`  
**Runtime:** `com.google.mediapipe:tasks-text:0.10.29` (unchanged)  
**Change control:** `docs/CHANGE_CONTROL_E4B_UNIVERSAL_SENTENCE_ENCODER.md`  
**ADR:** ADR-032

## Purpose

Product on-device `EmbeddingEngine` model for semantic meaning ranking. Replaces
compact `average_word_embedder` after M1/M2 showed cosine-only page-recall
failure on the compact model.

## Alternatives considered

| Option | Why not for E4b |
|---|---|
| Keep average-word + E5d boost only | Semantic-only quality fails measured interim bar |
| Embedding Gemma 300m | Larger/slower; separate change-control later |
| Cloud embedding API | Forbidden for core recall (ADR-012) |
| Bundle TFLite in APK | Conflicts with Spec §6 small-APK / pack download |

## Privacy / security

- INTERNET downloads **model bytes only** — never Memories or source content.
- Inference on-device after install.
- Clear Memora index deletes USE and legacy average-word files.
- Soft size ceiling 64 MiB; UI discloses ~40 MB upper bound.

## Licence / notices

MediaPipe Tasks Apache-2.0. Model subject to Google’s published MediaPipe model
terms (same pattern as average-word). See `docs/THIRD_PARTY_NOTICES.md`.

## Follow-up

M3: re-measure `meaning-pdf-page-recall-v1` with USE on emulator (and later
midrange). Decide whether E5d assist stays.
