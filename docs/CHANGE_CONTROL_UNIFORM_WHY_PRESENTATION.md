# Change control: Uniform Why presentation (one dialect, one panel)

**Date:** 2026-09-14  
**Type:** Find presentation only (Why). No retrieval, ranking, or index change.  
**Closes:** Demo-prep slice 1 — uniform “Why this result?”  
**Does not authorize:** I3 meaning-index worker, thumbnails, zoom, share/Act, synonym
nets, AVAILABLE, a new Find path, or Live/Dual growth.

## Pre-work record

- **Requirement IDs:** Product Contract Why trust surface; Freeze explainability;
  `CANONICAL_RECALL_RESULT_CONTRACT.md` §3 (cite stored evidence, honest path,
  one dialect); scenario bar U5 (do not duplicate the card); defect D-17
  (Why is not a word inventory); device runbook F-03 (no score jargon);
  ADR-024 (no silent keyword-as-meaning).
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE pre-work gate,
  CONTINUE, RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (Live/Dual **N = 0**),
  CANONICAL_RECALL_RESULT_CONTRACT, MEANING_FIND_PRODUCT_SCENARIO_BAR,
  HUMAN_RECALL_ASK_MODEL (ceiling; this slice does not add NL), CHANGE_CONTROL_TEMPLATE.
- **Current-code evidence inspected:** Five Find cards each expanded Why
  differently. Keyword Why was a five-line block that repeated query, type,
  filename, page, and the excerpt the card already highlighted. Meaning Why
  was a relevance sentence plus a cited line, sometimes with a different
  colour treatment. `CanonicalRecallWhyCopy` already existed as the named
  assembler; per-screen copy objects still asserted the old keyword dialect.
- **Open ADRs / platform:** no new ADR. No schema change. No new dependency.
- **Privacy:** Why still quotes only stored evidence already on device. No
  originals re-read. No network.
- **Smallest safe change:** One `WhyPresentation` shape (relevance, optional
  cited line, how-found) and one `WhyDisclosure` composable. Keyword leaves
  cited-line empty because the card already shows the excerpt. Meaning quotes
  the justifying stored span because the card does not. Per-screen Why
  strings remain thin delegates for existing tests.
- **Acceptance criteria:**
  - [x] Same slots, order, and typography on PDF / photo / screenshot / note
        and on both KEYWORD and MEANING
  - [x] Keyword Why does not repeat filename, page, or card excerpt
  - [x] Meaning Why does not inventory “Has / Does not have” on the card
  - [x] Path labelled in consumer words (ADR-024 / F-03)
  - [x] Shape-contract unit test covers every `AssetType` × both paths
  - [ ] Device: expand Why on one keyword PDF hit and one meaning hit — same
        panel, no repeated excerpt on keyword
- **Holistic scenarios:** empty/blank query rejected; self-capture meaning hit
  still names the screenshot of UNFYND and does not quote chrome; partial
  meaning list still uses the list banner, not per-card missing-word copy;
  TalkBack reads `spokenText`; no new permission.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall Why presentation (`ui/search`)
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none
TARGET PATH: CanonicalRecallWhyCopy → WhyPresentation → WhyDisclosure
  on every product Find card. Retrieval stays CanonicalRecall.
WHY THIS CHANGE CONVERGES: contract §3.4 one dialect; no second Explain path
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: per-screen Why string builders
  (already delegates)
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): n/a
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no — UI still cannot produce a search hit without
  Canonical Recall
```

## Delivery record

- **Files/layers changed:** `WhyPresentation`, `WhyDisclosure`,
  `CanonicalRecallWhyCopy`, `MeaningWhy`; keyword copy delegates and results
  hints; unit tests; `CANONICAL_RECALL_RESULT_CONTRACT.md` §3.1 / §3.4;
  CONTINUE / CHANGELOG.
- **Automated verification and result:** `:app:testDebugUnitTest` **761 tests,
  0 failures** (2026-09-14).
- **Emulator/manual verification and result:** pending device — expand Why on
  keyword PDF and meaning Find.
- **Failure/recovery paths verified:** blank query still throws at the
  assembler; blank Canonical Recall label still rejected by the result type.
- **Known limitation or follow-up:** demo slices 2–5 (I3, thumbnails, zoom,
  share) are not this change. Find quality (`pool timetable`) is unchanged.
- **Documentation/traceability/ADR updates:** this record; contract §3;
  CONTINUE checkpoint.
- **Git commit:** local, this checkpoint.
