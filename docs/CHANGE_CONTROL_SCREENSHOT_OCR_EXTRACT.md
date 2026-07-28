# Change Control: Screenshot OCR Extract

**Date:** 2026-07-28  
**Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.  
**Decision guardrails:** Phase 2 deterministic extraction for `SCREENSHOT` only;
ADR-009 discovery stays metadata-only; extract may open permitted image bytes
read-only on-device; bundled ML Kit Latin text recognition (no INTERNET permission,
no AI Pack download, no embeddings, no Memory ranking, no photo OCR yet).

## Pre-work record

- **Requirement IDs:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.
- **Sources read:** AGENTS, governance, CONTINUE, ROADMAP Phase 2, ADR-009,
  MediaStore EXIF extract change-control, Local AI Spec §4 (OCR capability remains
  unavailable until Local-AI pack path; this slice is deterministic extract only),
  THIRD_PARTY_NOTICES pattern.
- **Code inspected:** `RunPendingImageExifExtract` / WM / Room v5 EXIF table;
  MediaStore `AssetType.SCREENSHOT` classification; PrivacyScreen EXIF UI;
  ClearMemoraDerivedData tags.
- **Smallest safe change:** Room `screenshot_ocr_extractions` (v6); pending drain of
  SCREENSHOT only; platform OCR reader behind interface; WorkManager extract;
  honest setup UI after EXIF facts; cancel tag on clear index; Latin bundled ML Kit
  dependency review note.
- **Acceptance:** After catalogue + photo facts, user can start screenshot text
  reading; OCR text persisted when engine returns text (empty text is a valid
  completed extract); originals untouched; copy never claims keyword/Memory search
  for screenshots yet; photos are not OCR’d in this slice; clear index drops
  derived OCR.

## Architecture layers

UI → ViewModel → WorkManager scheduler → Worker → application pending reader →
platform OCR adapter + AssetRepository + Room persistence port.

## Known limitation

- PHOTO assets are not OCR’d here (screenshots only).
- No keyword search / Explain Mode over OCR text yet (separate follow-on).
- Latin-script bundled model only; other scripts need a later approved dependency.
- Does not activate Spec §4 `OcrEngine` Local-AI capability or AI Pack delivery.

## Acceptance record

- **Unit:** mapper, IndexingSummary OCR honesty, ScreenshotOcrExtraction,
  MediaStoreSetupViewModel — passed.
- **Emulator (2026-07-28):** User confirmed catalogue of 3 items, EXIF for 3,
  then OCR text saved for 1 screenshot with honest OCR-only copy (not keyword/
  Memory). Follow-up fix: MediaStore URI load via `InputImage.fromFilePath`,
  no false access-revoked dead end, retry kept catalogue/EXIF.
- **Git:** (filled after local commit)
