# ML Kit text-recognition dependency review (screenshot OCR)

**Date:** 2026-07-28  
**Artifact:** `com.google.mlkit:text-recognition:16.0.1` (bundled Latin)  
**Change control:** `docs/CHANGE_CONTROL_SCREENSHOT_OCR_EXTRACT.md`  
**ADR:** ADR-026

## Purpose

On-device Latin OCR for catalogued MediaStore `SCREENSHOT` assets only. Bundled
model runs locally; Memora does not declare INTERNET and does not download models
or upload image bytes.

## Alternatives considered

| Option | Why not chosen for this slice |
|---|---|
| Play Services unbundled text recognition | May require model download / Play Services path; weaker offline story |
| Tesseract / other OCR | Heavier integration cost; no existing Memora precedent |
| Defer OCR until AI Pack | Blocks Phase 2 P-06 screenshot text facts unnecessarily |

## Privacy / security

- No INTERNET permission added.
- Image bytes opened only on the extract path (not discovery).
- OCR text stays in Memora-owned Room storage; clear-index deletes derived rows.
- Does not activate Spec §4 `OcrEngine` or AI Pack delivery.

## Licence / notices

ML Kit libraries are distributed under Google’s ML Kit terms / Apache-2.0 style
notices as published with the artifact. Attribution is listed in
`docs/THIRD_PARTY_NOTICES.md`.

## Follow-up

Regenerate this note and notices for any version bump. Non-Latin script packs and
PHOTO OCR need separate approval.
