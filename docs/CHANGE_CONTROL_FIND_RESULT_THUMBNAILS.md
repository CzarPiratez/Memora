# Change control: Find result thumbnails (matched page, memory-only)

**Date:** 2026-09-14  
**Type:** Find presentation (visual recognition on Canonical Recall cards)  
**Closes:** Demo-prep slice 3 — thumbnails on Find result cards  
**Does not authorize:** pinch-zoom re-render, share/Act, Coil/Glide, disk cache of
user pixels, AVAILABLE, a new Find path, or Live/Dual growth

## Pre-work record

- **Requirement IDs:** P-16 (recognition-first UI); P-11 / LOCAL_AI §3 (search
  must not reopen originals *to answer a query*); P-15 (originals stay
  read-only); P-17 (UI → application → data). Canonical Recall result
  contract §2 open-original hints.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work gate,
  CONTINUE, PRODUCT_SOURCE_REGISTRY, LOCAL_AI_TECHNICAL_SPEC §3, RECALL_ENFORCEMENT_INDEX,
  LEGACY_RECALL_SURFACE (Live/Dual **N = 0**), CANONICAL_RECALL_RESULT_CONTRACT,
  CHANGE_CONTROL_TEMPLATE, CHANGE_CONTROL_UNIFORM_WHY_PRESENTATION.
- **Current-code evidence inspected:** Five Find cards were filename + excerpt +
  Why + Open, with no visual of the original. Open already reopens
  MediaStore / SAF via `OpenPersistedPhoto/Screenshot/PdfForViewing` at 960 /
  1440 px after a tap. `AndroidPdfPagePreviewRenderer` always used the Open
  edge. Screenshot Open already walks URI candidates after emulator cold boot.
  No Coil/Glide. Result lists are `Column`+scroll (all hits compose at once).
- **Open ADRs / platform:** no new ADR. `ContentResolver.loadThumbnail` is API 29;
  minSdk is 26, so API 26–28 uses sampled `BitmapFactory`. OneNote notes have
  no local page image.
- **Privacy:** Thumbnails are in-process LRU only (24 entries, 128 px). Clear-index
  evicts the cache. No disk write of user pixels. Originals are not edited.
  Ranking still uses stored Memory evidence only.
- **Smallest safe change:** One `LoadFindResultThumbnail` use case; one
  `FindResultThumbnail` composable on every product Find card. PDF renders the
  cited/matched page (page 1 only when no page is known, labelled as first page).
  Notes show an honest “Note” glyph.
- **Acceptance criteria:**
  - [x] Keyword PDF / photo / screenshot / note and meaning cards share one
        thumbnail composable
  - [x] PDF uses ranked/cited page, not an invented page
  - [x] Photo/screenshot decode at 128 px, not the 960 px Open buffer
  - [x] Notes do not reopen Graph or invent a page image
  - [x] Failed preview leaves the hit in place
  - [x] Unavailable results are not cached (permission grant can retry)
  - [x] Clear-index clears the memory cache
  - [x] No new Find path; Live/Dual unchanged
  - [ ] Device: keyword PDF matched page, photo, screenshot, note glyph,
        meaning mixed types, revoke access → placeholder
- **Holistic scenarios (before implement):**
  - User: ten-hit Find — recognise the photo/PDF page without opening
  - User: PDF hit on page 9 — thumbnail is page 9, not the cover
  - User: meaning hit with no PDF page — first-page preview, spoken as first page
  - User: OneNote hit — “Note” glyph, not a fake document photo
  - User: permission revoked / file gone — em dash placeholder; Open still
    explains; hit remains
  - User: clear index — thumbnails cannot leak from the previous library
  - Technical: all ten hits compose at once (existing lists) — decode gated to
    two concurrent opens
  - Technical: search ranking never reads thumbnail pixels
  - Edge: API 26 sampled decode; API 29+ `loadThumbnail`
- **Alternatives considered:**
  - Coil with disk cache — rejected; GOVERNANCE forbids undeclared dependencies
    and a plaintext thumbnail cache of user content.
  - Always page 1 for PDFs (industry default) — rejected; the matched page is
    the recognition value of this product.
  - Decode the existing 960 px Open preview into the card — rejected; a ten-hit
    list would allocate Open-sized buffers.
  - Skip notes entirely (empty slot) — rejected; a reserved glyph keeps layout
    stable and stays honest.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Find result presentation (thumbnails on Canonical Recall cards)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (Live/Dual N = 0)
TARGET PATH: same Canonical Recall hits; UI binds a read-only visual of the
  stored original / cited page. Retrieval stays CanonicalRecall.
WHY THIS CHANGE CONVERGES: recognition of an existing hit, not a second Find
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: text-only cards (this change)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

LOCAL_AI §3 / P-11: query answering still uses stored evidence. Reopening the
original here is the same class of work as Open (read-only visual), at list
size, and never a ranking input.

## Delivery record

- **Files/layers:** application `LoadFindResultThumbnail` + memory LRU;
  data `AndroidImageThumbnailLoader`, `AndroidMediaStoreImageUriResolver`;
  `PdfPreviewScale` + renderer `maxEdgePx`; `FindResultThumbnail` on PDF /
  photo / screenshot / note / meaning cards; Hilt bind; clear-index eviction.
  Screenshot Open now shares the URI resolver.
- **Automated verification:** `FindThumbnailPolicyTest`,
  `MemoryFindThumbnailCacheTest`, `LoadFindResultThumbnailTest`,
  `PdfPreviewScaleTest`, `FindThumbnailCopyTest`. Full
  `:app:testDebugUnitTest` **800 tests, 0 failures** (2026-09-14).
- **Emulator/manual:** pending founder device pass with Why + I3 + thumbnails
  together.
- **Failure/recovery:** missing grant / missing file → glyph; hit stays;
  failures not cached.
- **Known limitation:** lists are not lazy; decode concurrency is the backstop.
  No foreground prefetch beyond visible composition. Open pinch-zoom is a
  separate record (`CHANGE_CONTROL_OPEN_ORIGINAL_PINCH_ZOOM`).
- **Git commit:** pending local checkpoint (not pushed).
