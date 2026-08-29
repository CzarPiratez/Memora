# Change control: Escape-hatch audit + Live/Dual metric habit

**Date:** 2026-08-29  
**Type:** Documentation / enforcement cadence (docs-only)  
**Decision guardrails:** Docs only. Do not modify `MemoraApp/**`. Do not add CI
grep gates. Do not authorize MIG-05 step 4 / MIG-06+. Do not claim Recall
convergence DONE. No push.

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; `LEGACY_RECALL_SURFACE` escape-hatch
  test; `RECALL_CONVERGENCE_DONE` retirement / escape-hatch = NO; Cursor rule
  success metric (Live/Dual shrink).
- **Source documents read:** `LEGACY_RECALL_SURFACE`; `RECALL_CONVERGENCE_DONE`;
  `unfynd-architecture-invariants.mdc`; `CONTINUE.md`; Steps 1–6 change-controls.
- **Current-code evidence inspected:** None required (procedure docs; N=7
  restated from allowlist header).
- **Open ADRs / platform limitations checked:** ADR-049 accepted; Step 6
  committed (`d2b6056`). Prefer that commit before this delivery.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** New audit procedure + enforcement index; allowlist
  Audit cadence section; CONTINUE metric one-liner + Steps 1–7 landed note;
  CHANGELOG Unreleased; this record.
- **Acceptance criteria:** `ESCAPE_HATCH_AUDIT.md` with procedure + template;
  CONTINUE shows Live/Dual N + audit pointer; enforcement index discoverable;
  zero app code; stop (no MIG implementation).
- **Test and emulator verification plan:** N/A (docs-only).
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: docs-only audit cadence for Find bypass measurement
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none (no code)
TARGET PATH: Canonical Recall sole product Find after MIG-07 (escape-hatch NO)
WHY THIS CHANGE CONVERGES: makes Live/Dual N auditable on every Find/MIG
  checkpoint so convergence cannot be claimed without evidence
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L1–L8 Live/Dual per allowlist
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset (MIG/step/date): N/A
LEGACY SURFACE DELTA: unchanged (N=7); Audit cadence section + index pointer only
ESCAPE-HATCH AFTER CHANGE: yes — unchanged; enabling L1–L4, L5, L7, L8
```

### Escape-hatch audit record (this delivery)

```
Date: 2026-08-29
Live/Dual N: 7
Enabling L#s: L1, L2, L3, L4, L5, L7, L8
Escape-hatch YES/NO: YES
Delta since last audit: procedure + baseline record landed (N unchanged)
Auditor: docs Step 7
```

## Delivery record

- **Files/layers changed:**
  - `docs/ESCAPE_HATCH_AUDIT.md` (new)
  - `docs/RECALL_ENFORCEMENT_INDEX.md` (new; Steps 1–7 map)
  - `docs/LEGACY_RECALL_SURFACE.md` (Audit cadence; index pointer; metric reminder)
  - `CONTINUE.md` (Live/Dual one-liner + Steps 1–7 landed note)
  - `docs/CHANGELOG.md` (Unreleased)
  - this change-control
- **Verification evidence:** Docs-only; N=7 matches allowlist header.
- **Residual risks / follow-ups:** CI grep enforcement deferred until after
  MIG-07 cutover. MIG-05 step 4 / MIG-06+ still unauthorized.
- **User confirmation:** Pending (docs Step 7 acceptance).
