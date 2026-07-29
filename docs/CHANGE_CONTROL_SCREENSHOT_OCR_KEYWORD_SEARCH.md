# Change Control: Keyword Search of Saved Screenshot OCR Text

**Date:** 2026-07-29  
**Requirements:** P-01, P-06, P-11, P-14, P-15, P-17; A-01, A-02, A-06.  
**Decision guardrails:** ADR-022 (current fingerprint only); ADR-026 (screenshot OCR
extract already landed); interim keyword path only — no AI Pack, network,
embeddings, PHOTO OCR, Memory ranking, or open-original preview.

## Pre-work record

- **Requirement IDs:** P-01, P-06, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
- **Sources read:** AGENTS, governance, CONTINUE, ADR-022/026, PDF keyword search
  change-control, screenshot OCR extract change-control.
- **Code inspected:** `SearchPersistedPdfPageText`, `PdfKeywordSearchViewModel` /
  Copy / Screen; `screenshot_ocr_extractions` + DAO (no LIKE yet).
- **Smallest safe change:** Room LIKE over current-fingerprint non-empty
  `full_text`; Welcome entry + search UI with readiness, cancel, clear, Why
  citation, highlight; honest keyword-not-meaning copy; unit tests.
- **Acceptance:** After screenshot OCR extract, user can Find saved screenshot
  text, query a known OCR word (e.g. from fixture), see label + excerpt + Why;
  empty corpus / no matches / blank query are distinct; superseded fingerprints
  ignored; no claim of Memory or PHOTO OCR.

## Architecture layers

UI → ViewModel → application search/readiness use cases → Room DAO on
`screenshot_ocr_extractions` ⋈ `assets`.

## Known limitation

- No open-original / MediaStore preview from a hit (later slice).
- PHOTO assets are not searched.
- Not semantic Memory recall; Latin OCR corpus only as already extracted.

## Acceptance record

- **Unit:** ScreenshotOcrKeywordSearchCopy/ViewModel + IndexingSummary — passed.
- **Emulator (2026-07-29):** User confirmed Find saved screenshot text → query
  `note` → 1 match (`Screenshot_memora_note.png`) with highlighted excerpt and
  Why this result? citation (keyword-not-meaning).
- **Git:** `091bd67`.
