# Change control: UNFYND architecture invariants Cursor rule

**Date:** 2026-08-29  
**Type:** Documentation / agent enforcement (docs-only)  
**Decision guardrails:** Docs + `.cursor/rules` only. Do not modify
`MemoraApp/**`. Do not implement MIG-05 step 4 / MIG-06+. Do not expand
`LEGACY_RECALL_SURFACE` rows. Do not rewrite Freeze / Spec / Grounding hashed
blobs. Do not claim Canonical Recall exists in code. No push.

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; Migration Spec sequencing;
  MIG-05 step 3 checkpoint (Cursor-rule step).
- **Source documents read:** `CONTINUE` (MIG-05 step 3; ADR-049; legacy
  surface N=7), `DECISIONS` ADR-049, `LEGACY_RECALL_SURFACE` (full),
  `ARCHITECTURE_FREEZE_v1.0` §3, `.cursor/rules/git-checkpoint-commits.mdc`
  (frontmatter style), CHANGELOG.
- **Current-code evidence inspected:** None required (no app change).
- **Open ADRs / platform limitations checked:** ADR-049 accepted; Canonical
  Recall naming only. Prefer Step 2 (`LEGACY_RECALL_SURFACE`) committed
  first; this step does not reopen allowlist rows.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation / Cursor rule only.
- **Smallest safe change:** Thin alwaysApply rule + CONTINUE one-liner +
  CHANGELOG + this record.
- **Acceptance criteria:** Rule exists with `alwaysApply: true`; references
  ADR-049 + `LEGACY_RECALL_SURFACE`; false-positive guard; pre-code gate;
  CONTINUE + CHANGELOG updated; stop before Step 4 (change-control template).
- **Test and emulator verification plan:** N/A (docs-only).
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `.cursor/rules/unfynd-architecture-invariants.mdc` (new)
  - `CONTINUE.md` (Cursor rule pointer under legacy surface note)
  - `docs/CHANGELOG.md` (Unreleased one-liner)
  - `docs/CHANGE_CONTROL_UNFYND_ARCHITECTURE_INVARIANTS_RULE.md` (this file)
- **Automated verification and result:** N/A (docs-only; no app compile).
- **Emulator/manual verification and result:** N/A.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Step 4 (change-control template) not
  started. MIG-05 step 4 / MIG-06+ not authorized. Rule does not replace
  `git-checkpoint-commits.mdc`.
- **Documentation/traceability/ADR updates:** CONTINUE + CHANGELOG; no new
  ADR (enforcement of ADR-049 / Freeze §3 / allowlist).
- **Git commit:** Local only when requested; no push.
