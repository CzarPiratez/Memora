# Change control: Canonical Recall shared result + Why contract

**Date:** 2026-08-29  
**Type:** Documentation / DRAFT contract (docs-only)  
**Decision guardrails:** Docs only. Do not modify `MemoraApp/**`. Do not
implement MIG-06+. Do not add Kotlin types. Do not add or extend
`LEGACY_RECALL_SURFACE` rows. Do not implement Grounded Answers / Evidence
Package / `RecallRanker`. Do not check RECALL_CONVERGENCE_DONE boxes complete.
No push.

## Pre-work record

- **Requirement IDs:** Freeze §3 (evidence substrate; explainability); ADR-049;
  ADR-024; `RECALL_CONVERGENCE_DONE` shared result/Why boxes; Grounding §6–7
  (labeled candidates; package hints only).
- **Source documents read:** ADR-049; `RECALL_CONVERGENCE_DONE`;
  `ARCHITECTURE_FREEZE_v1.0` §3; ADR-024; `GROUNDING_ARCHITECTURE` §6–7;
  `LEGACY_RECALL_SURFACE`; interim hit DTOs (read-only gap map).
- **Current-code evidence inspected (read-only):** `MeaningSearchHit`;
  `PdfKeywordSearchHit`; `PhotoOcrKeywordSearchHit`;
  `ScreenshotOcrKeywordSearchHit`; `NotePageKeywordSearchHit`; per-screen Why
  copy (keyword honesty in strings, not structured path fields).
- **Open ADRs / platform limitations checked:** ADR-049 accepted; Step 5
  committed (`c8c92f3`). Prefer that commit before this delivery.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** New contract doc; CONTINUE pointer;
  RECALL_CONVERGENCE_DONE one-line sketch pointers; CHANGELOG Unreleased; this
  record.
- **Acceptance criteria:** Contract exists with fields + Why + path labels +
  non-goals + today’s-hit mapping; CONTINUE + RECALL_CONVERGENCE_DONE point to
  it; zero app code; stop before Step 7 (escape-hatch audit + metric cadence).
- **Test and emulator verification plan:** N/A (docs-only).
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: docs-only DRAFT for future Canonical Recall results
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (no code)
TARGET PATH: Canonical Recall shared result + Why (MIG-06/07 when authorized)
WHY THIS CHANGE CONVERGES: freezes one hit/Why dialect so MIG-06/07 do not
  invent four Explain shapes; maps today’s DTOs to shared fields
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L1–L4 keyword hit DTOs + interim
  MeaningSearchHit as product bind targets (per LEGACY_RECALL_SURFACE / MIG-07)
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: yes — unchanged; Canonical Recall still not in App;
  L1–L4, L5, L7, L8 still enable hits without Canonical Recall
```

## Delivery record

- **Files/layers changed:**
  - `docs/CANONICAL_RECALL_RESULT_CONTRACT.md` (new)
  - `docs/RECALL_CONVERGENCE_DONE.md` (contract sketch pointers under shared
    result/Why boxes)
  - `CONTINUE.md` (docs note)
  - `docs/CHANGELOG.md` (Unreleased)
  - `docs/CHANGE_CONTROL_CANONICAL_RECALL_RESULT_CONTRACT.md` (this file)
- **Automated verification and result:** N/A (docs-only; no app compile).
- **Emulator/manual verification and result:** N/A.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Contract is DRAFT / not implemented.
  RECALL_CONVERGENCE_DONE shared result/Why boxes remain unchecked. Step 7
  (escape-hatch audit procedure + metric cadence) not started. MIG-06+ not
  authorized.
- **Documentation/traceability/ADR updates:** CONTINUE + CHANGELOG +
  RECALL_CONVERGENCE_DONE pointers; no new ADR.
- **Git commit:** Local only when requested; no push.
