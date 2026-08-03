# Change control: Evidence-token boost for candidate meaning ranking

## Pre-work record

- **Requirement IDs:** Recall quality; enterprise checklist quality smoke;
  ADR-024 honesty (no silent keyword-as-meaning; no AVAILABLE).
- **Source documents read:** GOVERNANCE, CONTINUE, enterprise completion
  checklist, E5c change control, ADR-024/025/031.
- **Current-code evidence inspected:** E5c page index works; compact MediaPipe
  cosine ranks `mira` to screenshot / FOXTROT page 1 instead of pages that
  contain `mira`. Keyword Find already proves those pages exist.
- **Open ADRs / limitations:** This is a **disclosed candidate hybrid**, not
  measured AVAILABLE and not a substitute for E4b larger embedder. Must not
  label keyword-only search as meaning.
- **Privacy / offline:** Uses already-loaded evidence text at rank time; no
  network; no original rewrite.
- **Smallest safe change:** When scoring an indexed page (or summary) hit, if
  a significant cue token appears in that evidence text, add a bounded boost
  to cosine; Why/copy disclose when boost applied; unit tests; no AVAILABLE.
- **Acceptance criteria:**
  1. Cue `mira` ranks a PDF page whose saved text contains `mira` above
     FOXTROT page 1 / screenshot when those lack the token.
  2. Copy discloses evidence-token assist; never claims measured AVAILABLE.
  3. Blank/short tokens do not invent matches; caps on boost.
  4. Unit tests for pure boost helper + search ordering.
- **Not in this slice:** E4b vendor pack; measured ADR-024/025 corpus; changing
  MediaPipe model.

## Delivery record

- **Status:** Code complete; emulator quality retest pending.
- **Files/layers changed:** `MeaningEvidenceTokenBoost`; wired into
  `SearchAssetMemoriesByMeaning`; Why disclosure; unit tests; CONTINUE/
  CHANGELOG/enterprise checklist pointer.
- **Automated verification and result:** Boost + search ordering + copy unit
  tests green; debug APK installed.
- **Emulator/manual verification and result:** **Accepted** 2026-08-04 —
  `mira` → top PDF Matched page 5 with evidence assist in Why → Open Page 5
  of 5 (`JULIET meet mira closing`).
- **Known limitation or follow-up:** Still not measured AVAILABLE; E4b for
  stronger semantic-only quality without token assist.
- **Git commit:** (acceptance docs)
