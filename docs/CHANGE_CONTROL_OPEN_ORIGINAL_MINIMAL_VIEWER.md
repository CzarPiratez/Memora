# Change control — the opened original is the screen

**Date:** 2026-09-15
**Type:** User-visible presentation of an already-opened original
**Status:** Delivered in tree; founder device gate open

## Why this exists

The founder looked at the Open preview on the phone and said it was not
refined. He was right, and the reason was structural rather than cosmetic:
`OriginalPreviewScaffold` stacked a header, a filename, a page caption, a trust
paragraph, an Open button, a handoff paragraph and a zoom paragraph in one
`Column`, then gave the picture `weight(1f)`. The evidence — the thing the
person tapped Open to see — got roughly a third of the screen, below three
paragraphs explaining gestures every Android user already knows.

Two of those paragraphs taught pinch-to-zoom and "Back returns here". Text that
teaches a universal gesture is a sign the design does not feel native.

## Scope

- **In:** the preview is now full-bleed on a near-black canvas. Back, Share and
  info are 24 dp icons on 48 dp targets in translucent circles over a top
  scrim; **Open** is one pill at the bottom. Chrome fades out during a gesture
  and returns on a tap. Pinch and double-tap only — no visible zoom buttons.
- **In:** `Zoom in` / `Zoom out` / `Fit to screen` survive as TalkBack
  `CustomAccessibilityAction`s on the image.
- **In:** the trust line, the handoff explanation, the zoom explanation, the
  screen title and the filename move into an **About this preview** dialog
  behind the info icon.
- **In:** the filename shown there is run through `friendlyDisplayLabel`, so the
  40-character storage hash the founder saw is gone.
- **In:** page chip only when a document has more than one page.
- **Out:** ranking, Why, Canonical Recall, the notes path (no local file; still
  OneNote), the decode/OOM hardening, and `PreviewZoomPolicy` — the zoom and
  pan maths are untouched, so `PreviewZoomPolicyTest` still governs them.

## Convergence block

```
ARCHITECTURAL BOUNDARY: presentation of an already-opened original
CURRENT LEGACY PATH (L# or none): none
TARGET PATH: Canonical Recall remains the sole Find boundary; this screen is
  reached only after a Canonical Recall hit was tapped
WHY THIS CONVERGES: no retrieval, no ranking input, no new hit type, no new
  evidence source — layout, copy placement and accessibility only
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no — the UI still cannot produce a search hit
  without Canonical Recall
```

## Pre-work record

- **Requirement IDs:** P-16 (recognition-first UI, accessibility); P-11 (query
  answering still uses stored evidence); A-02 (no new cloud path).
- **Source documents read:** `ENGINEERING_CHARTER`, `GOVERNANCE` pre-work gate,
  `CONTINUE`, `PRODUCT_SOURCE_REGISTRY`, `LEGACY_RECALL_SURFACE` (N = 0),
  `CHANGE_CONTROL_OPEN_ORIGINAL_PINCH_ZOOM`,
  `CHANGE_CONTROL_OPEN_ORIGINAL_IN_ANOTHER_APP`, `CHANGE_CONTROL_TEMPLATE`,
  `unfynd-architecture-invariants`.
- **Current-code evidence inspected:** `OriginalPreviewScaffold` and
  `ZoomableOriginalImage`; all three call sites (`PdfOriginalPreviewScreen`,
  `PhotoOriginalPreviewScreen`, `ScreenshotOriginalPreviewScreen`) and the six
  routes that reach them (keyword + meaning); `PreviewZoomPolicy`;
  `CanonicalRecallWhyCopy.friendlyDisplayLabel`; the `Scaffold` at
  `MainActivity` that was insetting the preview.
- **Open ADRs / platform limitations checked:** ADR-053 / ADR-054 unchanged —
  same read-only URI, same two verbs. Not Act. Icons are local vector drawables,
  so **no new dependency** was added; `material-icons` was deliberately avoided.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  none. No permission, network, retention or URI-resolution change.
- **Smallest safe change:** rebuild one composable, add three drawables and one
  copy object, adjust one caption call site and the six routes' modifier.
- **Acceptance criteria:**
  - [x] Picture occupies the full screen; no explanatory paragraph above it
  - [x] Back / Share / info are icon-only with TalkBack names
  - [x] `Open` is the only labelled control
  - [x] Chrome fades during pinch/pan and returns on a single tap
  - [x] `Zoom in` / `Zoom out` / `Fit to screen` available to TalkBack
  - [x] Trust line still reachable, unchanged in wording
  - [x] Storage hash never shown
  - [x] Page chip suppressed on single-page documents
  - [x] Failures still speak, and are not hidden by the fade
- **Test and emulator verification plan:** copy tests plus the full unit suite;
  device pass on the founder's A15 (layout, insets, gestures, TalkBack).
- **User-visible quality/accessibility review plan:** icons must read on a white
  PDF page (translucent circles + scrim); 48 dp targets; error text light-red on
  a dark surface for contrast; polite live region retained.

## Design decisions worth recording

- **Open keeps a word; everything else is an icon.** No glyph reliably reads as
  "open this in another app" to someone who is not an engineer — `open_in_new`
  is developer vocabulary. Open is also the least discoverable action on the
  screen, so it is the one place a label earns its pixels.
  `OriginalPreviewChromeCopyTest` pins it to one word.
- **The zoom buttons became accessibility actions, not deletions.**
  `CHANGE_CONTROL_OPEN_ORIGINAL_PINCH_ZOOM` accepted "Zoom in / Zoom out / Fit
  (TalkBack)" as a criterion because a screen-reader user cannot pinch. Removing
  the visible chips without replacing that capability would have quietly broken
  an accepted criterion; `CustomAccessibilityAction` keeps it at zero pixels.
- **Failures sit outside the fade.** The gesture that triggers a failed sharper
  read is the same gesture that hides the chrome. If the message lived inside
  the fading overlay, the person would never see why the picture stopped
  improving.
- **A tap toggles chrome rather than opening the file.** Tap-to-open would
  collide with double-tap-to-zoom and would fire on every stray touch.
- **Translucent circles, not bare glyphs.** A PDF page is white. Bare white
  icons vanish on it and bare dark icons are harsh; a scrim plus per-icon circle
  is what makes the overlay legible on any content.
- **No icon dependency.** Three local vector drawables avoid adding
  `material-icons-*` and the dependency review that `AGENTS.md` requires.
- **The preview bypasses the app `Scaffold` padding.** It previously inherited
  `Modifier.padding(innerPadding)`, which cannot be full-bleed. The six routes
  now pass `Modifier.fillMaxSize()` and the preview insets only its own chrome
  with `statusBarsPadding` / `navigationBarsPadding`.
- **The system bars have to follow the canvas.** The first device build proved
  this: the rest of the app is light, so the window asks for dark system-bar
  icons, and over a near-black canvas that produced an unreadable clock and a
  pale grey navigation band directly under the Open pill. A `DisposableEffect`
  flips `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` off
  while the preview is on screen and restores the previous values on the way
  out — `WindowInsetsControllerCompat`, not the deprecated bar-colour setters,
  and scoped rather than global so the light app is unaffected.
- **The picture is fitted to width, not cropped.** A document viewer must never
  crop evidence to fill a tall screen, so black bands above and below a
  short page are correct and are what a phone photo viewer does.

## Delivery record

- **Files/layers changed:**
  - `ui/search/ZoomableOriginalPreview.kt` (rebuilt presentation; zoom/reload
    behaviour and the no-recycle / OOM-safe bitmap path preserved)
  - `ui/search/OriginalPreviewChromeCopy.kt` (new)
  - `ui/search/OriginalHandoffCopy.kt` (`OPEN_LABEL` → `Open`; hint names both
    actions)
  - `res/drawable/ic_preview_back.xml`, `ic_preview_share.xml`,
    `ic_preview_info.xml` (new)
  - `MainActivity.kt` (page caption only when `pageCount > 1`; six preview
    routes pass `Modifier.fillMaxSize()`)
  - `DarkSystemBarsWhileVisible` in the same file (system-bar icon appearance,
    scoped to this screen)
  - tests: `OriginalPreviewChromeCopyTest` (new), `OriginalHandoffCopyTest`
- **Not changed:** `PreviewZoomPolicy`, `ReloadOriginalPreview`,
  `application/handoff/**`, notes path, ranking, Why, permissions, manifest.
- **Automated verification and result:** `:app:compileDebugKotlin` BUILD
  SUCCESSFUL; `:app:testDebugUnitTest` **835 tests, 0 failures, 0 errors**
  (165 suites).
- **Emulator/manual verification and result:** founder's A15 (`SM-A156E`),
  2026-09-15, keyword PDF preview. First build: layout correct — page dominant on
  the dark canvas, icons legible in their circles over white paper, Open pill
  reachable, insets right — but the status-bar clock was dark-on-black and the
  navigation bar was a pale band. Fixed and re-verified on device: white clock,
  dark navigation bar. Screenshots taken through `adb screencap`. Remaining
  device items in the gate below are still open.
- **Failure/recovery paths verified:** unit-level only. `NoAppAvailable`,
  `SourceUnavailable`, `CouldNotOpen`, `CouldNotShare` and
  `COULD_NOT_SHARPEN_BODY` all still route to the status line, which is outside
  the fade and keeps `LiveRegionMode.Polite`.
- **Known limitation or follow-up:** TalkBack custom actions are verified by
  inspection, not by an instrumentation test — the repo has no Compose UI test
  harness for this screen. Copy is still hardcoded rather than in
  `strings.xml` (P-16 localization gap, unchanged).
- **Documentation/traceability/ADR updates:** this record; CHANGELOG; CONTINUE;
  amendment note in `CHANGE_CONTROL_OPEN_ORIGINAL_PINCH_ZOOM.md`.
- **Git commit:** after the founder's device pass.

## Device gate (founder)

1. ~~PDF preview: page fills the screen on a dark canvas; icons legible over
   white paper; **Open** pill at the bottom.~~ **Verified 2026-09-15**, plus
   system-bar contrast after the follow-up fix.
2. Pinch in / out and double-tap; chrome fades and returns on a tap.
3. Single-page PDF shows no page chip; a multi-page PDF does.
4. info icon → filename with no hash prefix, plus the trust line.
5. Open → default reader; Back returns to the preview.
6. Share → chooser.
7. Repeat on a photo and a screenshot.
8. TalkBack on: swipe to the image, open the actions menu, use Zoom in / Zoom
   out / Fit to screen.
