# Change Control: Keyword Search of Saved Photo OCR Text

**Date:** 2026-07-31  
**Requirements:** P-01, P-06, P-11, P-13, P-14, P-15, P-17; A-02, A-05, A-06.  
**Decision guardrails:** current-fingerprint `photo_ocr_extractions` only; keyword
substring matching, not semantic recall; no network, AI Pack, or screenshot corpus.

## Pre-work record

- **Code inspected:** screenshot OCR search/readiness/ViewModel/copy/screen/open
  pipeline and capped read-only MediaStore preview.
- **Smallest safe change:** PHOTO-only DAO search/readiness, application use cases,
  search UI with clear/cancel/Why, welcome entry, clear invalidation, and
  `OpenPersistedPhotoForViewing`.
- **Acceptance:** results cite the submitted query, saved OCR excerpt, and photo
  label; search never reopens photos; Open original is a separate user action and
  caps the decoded preview to a 960px long edge.

## Known limitations

- Exact keyword/substring search only; no natural-language or Memory ranking.
- Bundled Latin OCR corpus only.
- A stale MediaStore link or revoked access produces an honest unavailable result.

## Verification

- `:app:testDebugUnitTest`: passed on 2026-07-31.
- Android-test compilation and debug assembly passed. Emulator search/open remains
  pending because the available emulator failed APK installation with Android
  service `Broken pipe (32)`.
