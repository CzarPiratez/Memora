# Change control: architectural convergence in change-control template

**Date:** 2026-08-29  
**Type:** Documentation / change-control enforcement (docs-only)  
**Decision guardrails:** Docs only. Do not modify `MemoraApp/**`. Do not
implement MIG-05 step 4 / MIG-06+. Do not add `LEGACY_RECALL_SURFACE` rows.
Do not rewrite Freeze / Spec / Grounding hashed blobs. Do not claim Canonical
Recall exists in code. No push.

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; Migration Spec sequencing;
  `LEGACY_RECALL_SURFACE` Rules + escape-hatch; Cursor invariants operational
  gate (Step 4).
- **Source documents read:** `CHANGE_CONTROL_TEMPLATE`,
  `LEGACY_RECALL_SURFACE` (Rules + escape-hatch),
  `.cursor/rules/unfynd-architecture-invariants.mdc` (Operational gate),
  `GOVERNANCE` (mandatory pre-work gate), ADR-049, `CONTINUE`, CHANGELOG.
- **Current-code evidence inspected:** None required (no app change).
- **Open ADRs / platform limitations checked:** ADR-049 accepted; Step 3
  Cursor rule committed (`02bbb30`). Prefer that commit before this delivery.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Template convergence section + exception form +
  GOVERNANCE pointers + CONTINUE/CHANGELOG + this record.
- **Acceptance criteria:** Template has convergence block; exception form
  exists with mandatory sunset; GOVERNANCE points at them; CONTINUE +
  CHANGELOG updated; stop before Step 5 (MIG retirement DONE checklists).
- **Test and emulator verification plan:** N/A (docs-only).
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Architectural convergence

`N/A — not a Find/Recall change` (docs/process only; no search path).

## Delivery record

- **Files/layers changed:**
  - `docs/CHANGE_CONTROL_TEMPLATE.md` (Architectural convergence section)
  - `docs/LEGACY_EXTENSION_EXCEPTION.md` (new)
  - `docs/GOVERNANCE.md` (pre-work gate bullets 7–9)
  - `CONTINUE.md` (one-line docs note)
  - `docs/CHANGELOG.md` (Unreleased one-liner)
  - `docs/CHANGE_CONTROL_ARCHITECTURAL_CONVERGENCE_TEMPLATE.md` (this file)
- **Automated verification and result:** N/A (docs-only; no app compile).
- **Emulator/manual verification and result:** N/A.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Step 5 (MIG retirement DONE checklists)
  not started. MIG-05 step 4 / MIG-06+ not authorized. Does not reopen Freeze.
- **Documentation/traceability/ADR updates:** CONTINUE + CHANGELOG +
  GOVERNANCE pointers; no new ADR (enforcement of ADR-049 / Freeze §3 /
  allowlist / Cursor rule).
- **Git commit:** Local only when requested; no push.
