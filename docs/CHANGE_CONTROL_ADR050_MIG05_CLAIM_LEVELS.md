# Change control: ADR-050 MIG-05 claim levels (PDF slice vs Spec full)

**Date:** 2026-08-29  
**Type:** Documentation / claim-discipline decision  
**Decision guardrails:** Docs only. Do not rewrite hashed Product Contract,
Freeze, Migration Spec, Local AI Spec, Grounding, or Amendment blobs. Do not
authorize non-PDF evidence indexer implementation. Do not authorize MIG-06+.
Do not claim Canonical Recall exists in code. Do not change Live/Dual N or
app/search code. No push.

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-05; MIG-05 step 4 delivery;
  `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE` FULL DONE / non-PDF deferral;
  ADR-049 (Canonical Recall target-only).
- **Source documents read:** `CONTINUE` (MIG-05 step 4; N=6), ADR-049,
  `PRODUCT_SOURCE_REGISTRY`, MIG-05 FULL DONE checklist, `RECALL_ENFORCEMENT_INDEX`,
  `LEGACY_RECALL_SURFACE` (N=6), lead-locked claim levels A/B.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Hashed freeze/spec/amendment files not opened for rewrite. N=6 unchanged.
- **Open ADRs / platform limitations checked:** No ADR-050 collision (last
  accepted is ADR-049). Non-PDF indexer remains deferred; this ADR does not
  authorize it. Spec full vs PDF slice honesty gap is the decision being closed.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Accept ADR-050; registry / CONTINUE / FULL DONE /
  index pointer / changelog; this record.
- **Acceptance criteria:**
  - ADR-050 after ADR-049; Status Accepted.
  - Claim **A** (PDF delivery slice steps 1–4) COMPLETE for eng checkpoint language.
  - Claim **B** (Migration Spec MIG-05 full) STILL OPEN until non-PDF evidence
    indexing (or future scope ADR — not hashed Spec rewrite).
  - Grants/public/marketing must not say MIG-05/Spec MIG-05 complete without
    non-PDF-open honesty (or wait for B).
  - Hashed Spec / Freeze / Local AI Spec untouched.
  - Non-PDF indexer and MIG-06+ not authorized.
  - Zero app code in this change set.
- **Test and emulator verification plan:** Docs inspection only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: docs-only MIG-05 claim discipline (not Find path change)
CURRENT LEGACY PATH (L# or none): none (no code; N unchanged)
TARGET PATH: honest A vs B claims; Spec B still open
WHY THIS CONVERGES: prevents “MIG-05 complete” fiction while non-PDF deferred
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a (claim language only)
EXTENDS LEGACY? no
ESCAPE-HATCH AFTER CHANGE: yes — unchanged; Canonical Recall not in App; N=6
```

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-050)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (Authority item 11; MIG-05 claim note)
  - `CONTINUE.md` (Docs note ADR-050; checkpoint A/B clarify; DONE note)
  - `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md` (FULL DONE A vs B)
  - `docs/RECALL_ENFORCEMENT_INDEX.md` (one-line claim pointer)
  - `docs/CHANGELOG.md` (Unreleased)
  - `docs/CHANGE_CONTROL_ADR050_MIG05_CLAIM_LEVELS.md` (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed freeze/spec/amendment/contract files not in change set.
- **Emulator/manual verification and result:** Not required (docs-only).
- **Known limitation or follow-up:** B remains open until non-PDF indexer or a
  future scope-reinterpretation ADR. This ADR does not implement either.
- **Documentation/traceability/ADR updates:** ADR-050; this record.
- **Git commit:** Local checkpoint after verification (no push).
