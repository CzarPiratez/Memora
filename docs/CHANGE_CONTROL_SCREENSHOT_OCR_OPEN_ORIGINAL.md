# Change Control: Open original screenshot (in-app read-only preview)

**Date:** 2026-07-30  
**Requirements:** P-01, P-06, P-11, P-13, P-17; A-01, A-02, A-05.  
**Decision guardrails:** Originals read-only; search stays on stored OCR text; no
AI/network; ADR-009; ADR-026; SCREENSHOT-only. Prior open-original WIP was
discarded after emulator GPU ANRs; this slice re-lands with a **hard-capped**
scaled preview so decode/display stays bounded.

## Pre-work record

- **Requirement IDs:** P-01, P-06, P-11, P-13, P-17; A-01, A-02, A-05.
- **Sources read:** AGENTS, governance, CONTINUE, PDF open-original change-control,
  screenshot OCR keyword search change-control; prior WIP lessons (full-res decode
  + emulator RenderThread SIGSEGV).
- **Code inspected:** `ScreenshotOcrKeywordSearch*` at `ddf3cee`; PDF open/preview
  ViewModel paths; MediaStore access helper.
- **Platform note:** In-app scaled bitmap only (no external viewer). Long edge
  capped (~960px) so phone screenshots do not rebuild full-res bitmaps on the UI
  thread. Emulator may need software GPU when host graphics are unstable.
- **Smallest safe change:** Open use case + ViewModel open feedback/preview +
  per-hit Open + preview screen; unit tests; docs. No PHOTO OCR/search/edit.
- **Acceptance:** From a screenshot OCR keyword hit, Open shows Opening then an
  in-app image (or honest SourceUnavailable / CouldNotOpen). Why/search still use
  saved OCR only. Typing still works after open/close.

## Architecture layers

UI → ViewModel → `OpenPersistedScreenshotForViewing` → AssetRepository +
MediaStore grant check + ContentResolver read-only capped decode (ARGB8888).

## Known limitation

Scaled read-only preview only — not a gallery editor. Photo access must remain
valid. PHOTO assets out of scope. After emulator cold boot / MediaStore rescan,
stored content URIs can go stale while OCR rows remain searchable; Open falls
back to resolving the current MediaStore URI by display name. Re-running Start
indexing refreshes durable Asset locations.

## Acceptance record

- **Unit (re-verified 2026-07-31):** `OpenPersistedScreenshotForViewingTest`
  (incl. sample-size cap), open ViewModel paths,
  `ScreenshotOcrKeywordSearchCopyTest` open/preview honesty — passed under JDK 21.
- **Emulator (accepted 2026-07-31):** Cold-boot Medium Phone with
  `-gpu swiftshader_indirect`. Query `note` → hit → **Open original** showed
  read-only **Original screenshot** preview (`Screenshot_memora_note.png`) with
  ImageView and no SourceUnavailable. User confirmed pass.
- **Git:** open/preview feature and URI harden already on `main`; this record
  closes the interactive UI gate.
