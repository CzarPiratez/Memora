# Change Control: MediaStore Image EXIF Extract

**Date:** 2026-07-28  
**Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.  
**Decision guardrails:** Phase 2 deterministic image facts; ADR-009 discovery stays
metadata-only; extract may open permitted image bytes read-only; no OCR / AI Pack /
network / embeddings / Memory ranking.

## Pre-work record

- **Requirement IDs:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.
- **Sources read:** AGENTS, governance, CONTINUE, ROADMAP Phase 2, ADR-009,
  MediaStore discovery change-control, SAF PDF extract WM pattern.
- **Code inspected:** MediaStore discovery WM + Asset PHOTO/SCREENSHOT placeholders;
  SafPdfExtractWorker / RunPendingPdfLocalReading / pdf_extractions; ClearMemoraDerivedData
  tags; MediaStoreSetupViewModel.
- **Smallest safe change:** Room `image_exif_extractions` (v5); pending drain of
  PHOTO/SCREENSHOT; ExifInterface reader; WorkManager extract; honest setup UI after
  discovery; cancel tag on clear index.
- **Acceptance:** User can start photo-fact reading after catalogue; EXIF/datetime
  stored when present; originals untouched; copy never claims OCR/keyword/Memory
  search for photos; clear index drops derived facts.

## Architecture layers

UI → ViewModel → WorkManager scheduler → Worker → application pending reader →
platform EXIF adapter + AssetRepository + Room persistence port.

## Known limitation

No OCR, no keyword search over photos, no GPS/privacy surface beyond stored EXIF
strings already on the file. Screenshot vs photo remains discovery classification.

## Acceptance record

- **Unit:** mapper, IndexingSummary EXIF honesty, MediaStoreSetupViewModel — passed.
- **Emulator (2026-07-28):** User confirmed catalogue of 3 fixture photos then
  Read photo facts completed with honest EXIF-only copy (not OCR/search).
- **Git:** `05c866a`.
