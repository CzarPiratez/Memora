# Change control — Welcome tagline is the privacy promise

**Date:** 2026-09-14
**Type:** User-visible copy
**Status:** Delivered in tree; founder device check open

## Why this exists

Welcome led with "What are you trying to remember?" — a recall prompt — and
then repeated a near-identical privacy line underneath. The product's first
screen should state the privacy promise, not ask a search question before
anyone has indexed a file.

## Scope

- **In:** Welcome headline stays UNFYND. The title under it is
  "Your privacy first, on-device AI". The duplicate body line is removed. The
  permission card ("Your privacy comes first") stays. Meaning Find's field
  label is "A short recall cue" so the retired prompt is not still on that
  screen.
- **Out:** Ranking, Why, Open, Share, permissions, identity ADRs.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Welcome copy; Meaning Find field label only
CURRENT LEGACY PATH (L# or none): none
TARGET PATH: Canonical Recall unchanged; no new Find path
WHY THIS CONVERGES: label swap on an existing Canonical Recall entry field
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** P-16 (recognition-first UI); A-02 (local-first, no new
  cloud path).
- **Source documents read:** `ENGINEERING_CHARTER`, `GOVERNANCE` pre-work,
  `CONTINUE`, `PRODUCT_SOURCE_REGISTRY`, `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** `UnfyndWelcomeScreen` in
  `MainActivity.kt`; `MeaningSearchCopy.QUERY_LABEL`; `UnfyndSelfCapture`
  chrome list (retired prompt kept so older screenshots still demote).
- **Open ADRs / platform limitations checked:** none for copy. Not Act.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Copy only. No permission or network change.
- **Smallest safe change:** Welcome tagline + meaning field label + chrome
  marker for the new tagline + tests + this record.
- **Acceptance criteria:** Welcome does not show "What are you trying to
  remember?". Tagline is "Your privacy first, on-device AI". Meaning Find
  field is not the retired prompt. Self-capture still matches old chrome.
- **Test and emulator verification plan:** `WelcomeCopyTest`,
  `MeaningSearchCopyTest`, `UnfyndSelfCaptureTest`; unit suite. Device: open
  Welcome after install.
- **User-visible quality/accessibility review plan:** TalkBack reads UNFYND
  then the privacy tagline. Permission card still explains access before
  indexing.

## Delivery record

- **Files/layers changed:** `WelcomeCopy`; `UnfyndWelcomeScreen`;
  `MeaningSearchCopy.QUERY_LABEL`; `UnfyndSelfCapture` chrome; tests;
  CHANGELOG; CONTINUE; this record.
- **Automated verification and result:** `:app:testDebugUnitTest` for
  `WelcomeCopyTest`, `MeaningSearchCopyTest`, `UnfyndSelfCaptureTest` —
  **BUILD SUCCESSFUL**.
- **Emulator/manual verification and result:** founder device after install.
- **Failure/recovery paths verified:** N/A (copy).
- **Known limitation or follow-up:** localization still hardcoded in `*Copy.kt`
  (P-16 open).
- **Documentation/traceability/ADR updates:** this record; CHANGELOG; CONTINUE.
- **Git commit:** not in this step unless requested.
