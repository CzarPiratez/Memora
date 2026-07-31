# Change Control: Photo OCR Extract

**Date:** 2026-07-31  
**Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.  
**Decision guardrails:** ADR-028; deterministic `PHOTO` extraction only; bundled
ML Kit Latin OCR; explicit user-started WorkManager drain; no network, AI Pack,
semantic Memory, or screenshot-table reuse.

## Pre-work record

- **Sources read:** Product registry, Local AI specification, governance, product
  contract, architecture, decisions, roadmap, traceability, CONTINUE, ADR-026, and
  the screenshot OCR implementation/tests.
- **Smallest safe change:** separate Room v7 `photo_ocr_extractions`, PHOTO-only
  pending selector and reader, bounded worker drain, setup state/copy, clear-index
  cancellation, and focused unit tests.
- **Acceptance:** PHOTO records cannot accept SCREENSHOT assets; current fingerprint
  and schema determine pending work; originals are opened read-only; empty OCR is a
  completed extract; no photo text enters `screenshot_ocr_extractions`.

## Architecture and privacy

UI → ViewModel → scheduler/worker → application use case → PHOTO-only platform
reader + Room persistence. OCR runs on-device with the existing bundled Latin model.
No `INTERNET` permission, upload, source mutation, or Local-AI availability claim.

## Verification

- `:app:testDebugUnitTest`: passed on 2026-07-31.
- `:app:compileDebugAndroidTestKotlin :app:assembleDebug`: passed.
- Targeted `MemoraDatabaseMigrationTest` could not start because emulator package
  installation failed with Android service `Broken pipe (32)`; rerun after emulator
  recovery. Visible PHOTO OCR verification also remains required.
- **Git:** `e5668d3`.
