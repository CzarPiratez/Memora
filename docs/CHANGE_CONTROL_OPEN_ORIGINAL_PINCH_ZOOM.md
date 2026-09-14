# Change control: Open-original pinch-zoom with on-device re-render

**Date:** 2026-09-14  
**Type:** Open-original presentation (not Find / ranking)  
**Closes:** Demo-prep slice 4 — pinch-zoom of the opened original  
**Does not authorize:** share/Act, Coil/Glide, a full PDF reader, in-app note
viewer, disk cache of user pixels, AVAILABLE, a new Find path, or Live/Dual growth

## Pre-work record

- **Requirement IDs:** P-16 (recognition-first UI); P-11 / LOCAL_AI §3 (search
  must not reopen originals *to answer a query*); P-15 (originals stay
  read-only); P-17 (UI → application → data). Canonical Recall result
  contract §2 open-original hints.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work gate,
  CONTINUE, PRODUCT_SOURCE_REGISTRY, LOCAL_AI_TECHNICAL_SPEC §3,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (Live/Dual **N = 0**),
  CANONICAL_RECALL_RESULT_CONTRACT, CHANGE_CONTROL_TEMPLATE,
  CHANGE_CONTROL_PDF_KEYWORD_OPEN_ORIGINAL, CHANGE_CONTROL_FIND_RESULT_THUMBNAILS.
- **Current-code evidence inspected:** PDF / photo / screenshot Open preview
  screens were `Column` + `verticalScroll` + `Image` `FillWidth` of a 960 px
  (image) / 1440 px (PDF) ARGB buffer. No `detectTransformGestures`. PDF
  renderer already accepted `maxEdgePx`; photo/screenshot Open hard-coded 960.
  Notes Open launches OneNote URLs — no in-app page image. Keyword and meaning
  already share the three preview screens.
- **Open ADRs / platform:** no new ADR. No schema change. Decode cap 2048 px
  (~16 MB ARGB) chosen to stay inside a phone-class heap; not native resolution.
- **Privacy:** Zoom re-opens the same original Open already had a grant to
  read. Pixels stay in process (ViewModel keeps the first-paint buffer;
  sharper frames live in the preview composable). No upload. No disk cache of
  user pixels. Ranking still uses stored Memory evidence only.
- **Smallest safe change:** One `PreviewZoomPolicy` + `ReloadOriginalPreview`
  over the existing Open use cases; one `OriginalPreviewScaffold` on PDF /
  photo / screenshot preview (keyword + meaning). Notes unchanged.
- **Acceptance criteria:**
  - [x] Pinch and Zoom in / Zoom out / Fit buttons (TalkBack)
  - [x] Scale above ~1.2 re-renders from the original at a stepped edge ≤ 2048
  - [x] First paint stays the existing Open budget (960 / 1440)
  - [x] Failed re-render keeps the last sharp-enough image; preview stays open
  - [x] Note Open still goes to OneNote; no invented in-app note viewer
  - [x] Meaning PDF still opens the matched page, then zooms that page
  - [x] No new Find path; Live/Dual unchanged
  - [ ] Device: pinch a PDF page and a photo; Fit returns to full page; revoke
        grant mid-zoom keeps the last image
- **Holistic scenarios (before implement):**
  - User: pinch in on a cited PDF page — text stays readable, not a blur of 1440
  - User: pinch out / Fit — back to the first-paint buffer
  - User: pan a zoomed photo; pan a tall screenshot at fit
  - User: TalkBack Zoom in / Zoom out / Fit without pinch
  - User: pinch quickly — debounce + cancel in-flight reopen
  - User: permission revoked after Open — last image stays; no crash
  - User: OneNote hit — still leaves the app; no fake zoom viewer
  - Technical: zoomed pixels are not written into ViewModel StateFlow
  - Edge: process death closes preview (existing)
- **Alternatives considered:**
  - `graphicsLayer` scale of the 960/1440 buffer only — rejected; that is the
    defect this slice exists to fix.
  - Full PDF reader / tiled `SubsamplingScaleImageView` — too large for a
    demo-prep slice; annotations are out of scope.
  - Decode native resolution on first Open — OOM risk on a 12 MP photo.
  - Coil/Glide — undeclared dependency; would invite a disk cache of user
    pixels.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Open-original presentation (pinch-zoom re-render)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (Live/Dual N = 0)
TARGET PATH: same OpenPersistedPdf/Photo/ScreenshotForViewing use cases;
  Canonical Recall stays the sole Find boundary
WHY THIS CHANGE CONVERGES: sharper view of an already-opened original, not a
  new Find path
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: stretch-the-Open-buffer preview
  (this change)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

LOCAL_AI §3 / P-11: query answering still uses stored evidence. Reopening the
original here is Open-class read-only display after a tap, never a ranking
input.

## Delivery record

- **Files/layers:** application `PreviewZoomPolicy`, `ReloadOriginalPreview`;
  Open use cases accept `maxEdgePx`; UI `OriginalPreviewScaffold` + zoom copy;
  CompositionLocal reloader from `OriginalPreviewReloadViewModel`; preview UI
  models carry source identity for reload. Notes unchanged.
- **Automated verification:** `PreviewZoomPolicyTest`,
  `ReloadOriginalPreviewTest`, `OriginalPreviewZoomCopyTest`, sample-size /
  scale tests, ViewModel identity after Open. Full
  `:app:testDebugUnitTest` **809 tests, 0 failures** (2026-09-14).
- **Emulator/manual:** pending founder device pass with Why + I3 + thumbnails
  + zoom together.
- **Failure/recovery:** reload Unavailable keeps last pixels; Opening path
  unchanged; clear-index still drops the preview.
- **Known limitation:** not a full PDF reader (no annotations, no page
  swipe). Decode cap 2048 px. Share is a separate record
  (`CHANGE_CONTROL_OPEN_ORIGINAL_SHARE`). First paint is still sampled;
  sharpness arrives after pinch / Zoom in.
- **Git commit:** pending local checkpoint (not pushed).

## Defect fixes after review (2026-09-14)

Found by code review of this slice before the next one started. No feature
change, no new decode budget, no new Find path.

- **Z-1 recycled bitmap.** `PreviewBitmapImage` recycled its `Bitmap` in an
  `onDispose` when a sharper read replaced it. Compose can still be drawing
  the previous frame, which is the `Canvas: trying to use a recycled bitmap`
  crash class — and most likely on the exact gesture this slice exists for.
  The preview now holds an `ImageBitmap` and never recycles;
  `FindResultThumbnail` already worked this way, so the two paths agree again.
- **Z-2 `OutOfMemoryError`.** A 2048 px ARGB reload is ~16 MB of `IntArray`
  plus ~16 MB of `Bitmap` while the previous buffer is still live.
  `OutOfMemoryError` is an `Error`, so the existing `catch (Exception)` in
  `ReloadOriginalPreview` could not see it and no other code in the app
  handled it. It is now caught at that one decode boundary and mapped to
  `Unavailable`. The `Bitmap` allocation also moved off the composition into
  the reload coroutine so the same failure is recoverable there.
- **Z-3 silent failure.** `Unavailable` cleared the spinner and left a blurry
  picture with no explanation, while Share already had honest error copy.
  `OriginalPreviewZoomCopy.COULD_NOT_SHARPEN_BODY` now states that UNFYND
  could not read a sharper view on this phone and that the picture has not
  changed, as a polite live region.

- **Verification:** `ReloadOriginalPreviewTest.out_of_memory_keeps_the_preview_instead_of_crashing`,
  `OriginalPreviewZoomCopyTest.a_failed_sharper_read_says_the_picture_did_not_change`.
  Full `:app:testDebugUnitTest` **818 tests, 0 failures** (2026-09-14).
- **Still open:** founder device pass. Z-1 and Z-2 are crash classes that a
  unit suite cannot prove absent; the device pass should pinch a large PDF
  page and a large photo repeatedly.
