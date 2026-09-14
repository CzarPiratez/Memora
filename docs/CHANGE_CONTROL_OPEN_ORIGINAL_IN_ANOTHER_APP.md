# Change control — Open the whole original in another app

**Date:** 2026-09-14
**Decision:** ADR-054 (extends ADR-053)
**Status:** Delivered in tree; founder device gate open

## Why this exists

Open showed a rendered picture of the page that justified the hit. That is the
right thing for *evidence* — it proves why UNFYND surfaced the file — but it is
not the file. A person who recognises their document wants to scroll it, search
inside it, and use it the way they always do, then come back.

Before this slice the only way out of the preview was Share, which asks the
person to pick a destination for a file they only wanted to read.

## Scope

- **In:** a **Open full file** tap on the PDF / photo / screenshot Open preview
  starts `ACTION_VIEW` with a read-only grant of the stored content URI. The
  viewer opens inside UNFYND's task, so Back returns to the preview.
- **In:** honest outcomes — no reader installed, source unreachable, and could
  not open are three different messages.
- **Out:** notes (no local file; Open still goes to the OneNote app). Editing,
  annotating, printing, any write grant. Replacing the in-app preview. Any
  card-level "open in app" action. Act of any kind.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Open-adjacent handoff of an already-opened original
CURRENT LEGACY PATH (L# or none): none
TARGET PATH: Canonical Recall remains the sole Find boundary; this is a
  post-tap display action over a URI the Open path already resolved
WHY THIS CONVERGES: no retrieval, no ranking input, no new hit type — the same
  URI Share already hands out, sent with a different verb
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no — the UI still cannot produce a search hit
  without Canonical Recall
```

LOCAL_AI §3 / P-11: query answering still uses stored evidence. Handing the
original to a viewer after a tap is Open-class display, never a ranking input.

## Design decisions worth recording

- **One resolver, two verbs.** Share and Open need the identical thing: a
  read-only content URI plus MIME for a stored original. That resolver is now
  `application/handoff/PrepareOriginalHandoff` instead of a class called
  `PrepareShareOriginal`, because Open is not a share. Pure rename plus package
  move; no behaviour change to the share sheet. `ShareOriginalChooser`
  (`ACTION_SEND`) and `ExternalOriginalViewer` (`ACTION_VIEW`) are separate
  ports over that one result.
- **The viewer starts from the Activity, not the application context.**
  `FLAG_ACTIVITY_NEW_TASK` from the application context can surface the
  viewer's *existing* task — showing a different document and losing the way
  back to UNFYND. `ForegroundActivityTracker` (an
  `Application.ActivityLifecycleCallbacks` holding a weak reference to the
  resumed Activity) lets the viewer start inside UNFYND's task instead. When no
  Activity is on screen the old `NEW_TASK` path is still the fallback. This is
  why the copy can promise "Back brings you here".
- **No forced chooser.** `ACTION_VIEW` without `createChooser` opens the
  person's default reader, which is what they expect for their own files.
  Android raises its own picker when they have no default.
- **`NoAppAvailable` is a distinct outcome.** `ActivityNotFoundException` means
  the phone has no reader, which the person can fix. Folding it into "could not
  open" would blame their file for their app list.
- **The preview is not replaced.** The cited page is the evidence and works
  offline with no third-party app. The full file is the escape hatch.

## Delivery record

- **Files/layers:** application `handoff` package (`OriginalHandoffRequest`,
  `PreparedOriginalHandoff`, `PrepareOriginalHandoff`, `OpenOriginalInAnotherApp`,
  ports); data `AndroidExternalOriginalViewer` + `ForegroundActivityTracker`
  (registered in `UnfyndApplication`), `AndroidHandoffPdfUriAccess`,
  `AndroidHandoffImageUriCandidates`, `OriginalHandoffModule`; UI
  `OriginalHandoffViewModel`, `OriginalHandoffCopy`, Open button and combined
  handoff feedback in `OriginalPreviewScaffold`.
- **Automated verification:** `OpenOriginalInAnotherAppTest` (ready handoff
  reaches the viewer with its MIME, no-reader is its own outcome, an
  unreachable original never reaches the viewer, failed handoff reads as could
  not open), `PrepareOriginalHandoffTest` (unchanged behaviour under new
  names), `OriginalHandoffCopyTest`. Full `:app:testDebugUnitTest`
  **828 tests, 0 failures** (2026-09-14). `:app:assembleDebug` succeeded.
- **Failure/recovery:** source unreachable, no reader, and could-not-open each
  have their own polite live-region line. A failed open leaves the preview
  exactly as it was.
- **Privacy:** read grant for one intent. No copy into UNFYND storage, no
  FileProvider cache of user pixels, no upload, no persisted grant handed to
  the other app.
- **Known limitation:** notes have no local file, so Open full file is not
  offered for them. A viewer that ignores `ACTION_VIEW` MIME types may still
  refuse a SAF tree-document URI; that surfaces as could-not-open. Task
  affinity depends on the viewer's own manifest, so a reader declaring
  `singleInstance` can still take its own task.
- **Device gate (open):** founder taps Open full file on a PDF, a photo, and a
  screenshot; confirms the real app opens the whole file and Back returns to
  the UNFYND preview.
- **Git commit:** pending local checkpoint (not pushed).
