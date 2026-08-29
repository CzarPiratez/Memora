# Change control: MIG-05 + Recall convergence DONE checklists

**Date:** 2026-08-29  
**Type:** Documentation / program-exit definitions (docs-only)  
**Decision guardrails:** Docs only. Do not modify `MemoraApp/**`. Do not
implement MIG-05 step 4 or MIG-06+. Do not mark DONE boxes completed. Do not
claim Canonical Recall exists in code. Do not rewrite Freeze / Spec /
Grounding hashed blobs. No push.

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; Migration Spec MIG-05 / MIG-06 /
  MIG-07 / MIG-07B; `LEGACY_RECALL_SURFACE` L1–L8; Step 4 change-control
  convergence template (prefer committed first).
- **Source documents read:** `LEGACY_RECALL_SURFACE`,
  `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE` (step 3 residuals / step 4),
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-05/06/07/07B, ADR-049, `CONTINUE`
  next eng defaults, Step 4 `CHANGE_CONTROL_ARCHITECTURAL_CONVERGENCE_TEMPLATE`.
- **Current-code evidence inspected:** None required (no app change).
- **Open ADRs / platform limitations checked:** ADR-049 accepted; Step 4
  committed (`0fa6407`). Prefer that commit before this delivery.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Append MIG-05 retirement checklist; add Recall
  convergence DONE file; CONTINUE pointers; CHANGELOG Unreleased; this record.
- **Acceptance criteria:** Both checklists exist and emphasize retirement;
  CONTINUE points to them; zero app code; stop before Step 6 (shared
  result/Why contract sketch); checklists remain open / unchecked.
- **Test and emulator verification plan:** N/A (docs-only).
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Architectural convergence

`N/A — not a Find/Recall change` (docs/process only; defines exit criteria;
does not touch search paths).

## Delivery record

- **Files/layers changed:**
  - `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md` (MIG-05 FULL DONE
    retirement checklist + “Already true after step 3” note)
  - `docs/RECALL_CONVERGENCE_DONE.md` (new; program exit)
  - `CONTINUE.md` (docs note + next eng default pointers)
  - `docs/CHANGELOG.md` (Unreleased)
  - `docs/CHANGE_CONTROL_MIG05_RECALL_CONVERGENCE_DONE_CHECKLISTS.md` (this
    file)
- **Automated verification and result:** N/A (docs-only; no app compile).
- **Emulator/manual verification and result:** N/A.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** MIG-05 FULL DONE still open (step 4
  retirement mandatory). Recall convergence DONE still open. Step 6 (shared
  result/Why contract sketch) not started. MIG-05 step 4 / MIG-06+ not
  authorized by this delivery.
- **Documentation/traceability/ADR updates:** CONTINUE + CHANGELOG pointers;
  no new ADR.
- **Git commit:** Local only when requested; no push.
