# Change control: Open-original share sheet (stored URI, not Act)

**Date:** 2026-09-14  
**Type:** Open-adjacent presentation + platform URI handoff  
**Closes:** Demo-prep slice 5 — share via stored URI  
**Does not authorize:** Act product, reminders, FileProvider cache of user
pixels, Coil/Glide, note file share, AVAILABLE, a new Find path, or Live/Dual growth

## Pre-work record

- **Requirement IDs:** P-15 (originals read-only); P-11 / LOCAL_AI §3 (search
  does not reopen originals *to answer a query*); P-17 (UI → application →
  data). HUMAN_RECALL J17; MEANING_FIND ACT1. ADR-043 (Act out) interpreted by
  ADR-053.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work gate,
  CONTINUE, PRODUCT_SOURCE_REGISTRY, PRODUCT_CONTRACT, LOCAL_AI_TECHNICAL_SPEC,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (Live/Dual **N = 0**),
  HUMAN_RECALL_ASK_MODEL, MEANING_FIND_PRODUCT_SCENARIO_BAR, ADR-043, ADR-053,
  CHANGE_CONTROL_TEMPLATE, CHANGE_CONTROL_OPEN_ORIGINAL_PINCH_ZOOM,
  CHANGE_CONTROL_PDF_KEYWORD_OPEN_ORIGINAL.
- **Current-code evidence inspected:** Preview screens had Back + zoom, no
  Share. No `ACTION_SEND` / FileProvider. PDF Open uses the SAF broker’s
  canonical tree-document URI, not `Asset.location` as authority. Screenshot
  Open walks MediaStore URI candidates after cold boot. Notes Open launches
  OneNote URLs (no local file).
- **Open ADRs / platform:** ADR-053 accepted in this slice. SAF targets may
  refuse a tree-document URI; that is an honest failure, not a cache-copy
  workaround. Chooser needs `ClipData` + `FLAG_GRANT_READ_URI_PERMISSION` or
  the grant often does not reach the chosen app.
- **Privacy:** Read grant only. No write. No upload. No durable copy of user
  pixels into UNFYND. Ranking still uses stored Memory evidence only.
- **Smallest safe change:** `PrepareShareOriginal` + Android chooser; Share
  on the existing Open preview (PDF / photo / screenshot; keyword and meaning).
  Notes unchanged (Open in OneNote).
- **Acceptance criteria:**
  - [x] Share on Open preview for PDF, photo, screenshot
  - [x] PDF URI is the canonical tree-document URI, not a trusted `Asset.location`
  - [x] Photo/screenshot walk Open’s URI candidates; first readable wins
  - [x] Read grant only; no write flag
  - [x] Honest SourceUnavailable / CouldNotShare copy
  - [x] Notes have no Share (no local file)
  - [x] No new Find path; Live/Dual unchanged; Act not claimed
  - [ ] Device: Share a PDF to Files/Drive; share a photo; revoke grant → honest
        failure; note Open still leaves for OneNote
- **Holistic scenarios (before implement):**
  - User: Open a cited PDF page, tap Share — the whole PDF goes to the app they
    pick, not a screenshot of the page
  - User: Share a photo after emulator cold boot — candidate walk still finds it
  - User: TalkBack Share
  - User: tap Share twice quickly — second tap disabled while presenting
  - User: folder grant revoked — “cannot share right now”
  - User: OneNote hit — still Open only
  - Technical: search / ranking never reads the share URI
- **Alternatives considered:**
  - FileProvider cache copy so more apps accept the file — rejected this slice
    (CONTINUE: stored URI; copies user pixels into app cache).
  - Share from every Find card — extra surface; preview is after the person
    confirmed the original.
  - `ACTION_VIEW` instead of `ACTION_SEND` — historically flaky on emulator
    Files; share sheet is the handoff the person asked for.
  - Treat this as Act product — rejected; ADR-053.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Open-original presentation (share sheet of stored URI)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (Live/Dual N = 0)
TARGET PATH: same Open identity; PrepareShareOriginal resolves a read-only URI;
  Canonical Recall stays the sole Find boundary
WHY THIS CHANGE CONVERGES: handoff of an already-opened original, not a new Find
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: Open-only preview (this change adds Share)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

LOCAL_AI §3 / P-11: query answering still uses stored evidence. This reopen is
Open-class read-only display/handoff after a tap, never a ranking input.

## Delivery record

- **Files/layers:** application `PrepareShareOriginal`; data SAF canonical PDF
  URI + MediaStore readable probe + `ACTION_SEND` chooser; UI Share on
  `OriginalPreviewScaffold`; ADR-053.
- **Automated verification:** `PrepareShareOriginalTest`, `ShareOriginalCopyTest`.
  Full `:app:testDebugUnitTest` **816 tests, 0 failures** (2026-09-14).
- **Emulator/manual:** pending founder device pass with Why + I3 + thumbnails +
  zoom + share together.
- **Failure/recovery:** missing grant / unreadable URI → honest copy; preview
  stays open.
- **Known limitation:** not Act. Notes have no local file share. Some apps may
  refuse a SAF URI; no cache-copy fallback in this slice. Decode/zoom unchanged.
- **Git commit:** pending local checkpoint (not pushed).
