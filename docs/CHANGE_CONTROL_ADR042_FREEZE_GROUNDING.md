# Change control: ADR-042 Freeze vs Grounding and GOVERNANCE order

**Date:** 2026-08-24  
**Type:** Documentation / governance decision  
**Decision guardrails:** Docs only. Do not rewrite Freeze §1. Do not retire
Grounding. Do not edit hashed `LOCAL_AI_TECHNICAL_SPEC.md` or Experience
Memory §7. Do not change the Architecture Freeze blob hash. Do not start
MIG-01–MIG-11 or MIG-07B. No application, database, migration, retrieval,
OCR, MemoryBuilder, RecallRanker, Grounded Answers, Event/Knowledge/Links,
package, or identity change. Do not use
`docs/PHASE_A_IMPLEMENTATION_PLAN_V1.md` as permission to implement. No
deploy. No push.

## Pre-work record

- **Requirement IDs:** P-01 / P-18 (scope control); A-02 (no new cloud path);
  G-01–G-08 (Grounded Answers remain architecture, not implementation);
  E-01–E-06 (PKI staged).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `DECISIONS` (ADR-040 / ADR-041), `ARCHITECTURE`,
  `GROUNDING_ARCHITECTURE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC`,
  `EXPERIENCE_MEMORY_AMENDMENT_V1`, `ARCHITECTURAL_MIGRATION_SPEC_V1`,
  `ARCHITECTURE_FREEZE_v1.0`, `CHANGELOG`, `CHANGE_CONTROL_TEMPLATE`,
  `CHANGE_CONTROL_ARCHITECTURE_FREEZE_REGISTRATION`.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Freeze and Migration Spec SHA-256 values still match the registry rows
  committed with ADR-041. Hashed Local AI Spec §15 and Experience Memory §7
  do not contain the Freeze §7 claimed correction. PHASE_A plan and commercial
  strategy remain unhashed non-canon attachments.
- **Open ADRs / platform limitations checked:** ADR-041 conflicts 1–3 decided
  here with the user-chosen winners. ADR-033–039 Grounded Answers remain
  architecture. Technical IDs stay deferred.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Accept ADR-042; align GOVERNANCE numbered order
  with Freeze §2; update registry/CONTINUE pointers; one Unreleased changelog
  bullet; this record. Do not rewrite changelog history or hashed constitutions.
- **Acceptance criteria:**
  - Conflict 1 winner is option (a); Grounding not retired; Freeze §1 unedited.
  - Conflict 2 winner is GOVERNANCE aligned with Freeze §2, including the
    scoped Local AI Spec win rule.
  - Conflict 3 is errata; hashed Spec, Amendment, and Freeze blobs unchanged.
  - One new Unreleased changelog bullet; prior Unreleased entries preserved.
  - MIG-* still unstarted; PHASE_A is not permission to implement.
  - No `MemoraApp/` or schema/migration file edits.
- **Test and emulator verification plan:** Docs inspection and hash
  verification only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (ADR-041 status pointer; append ADR-042)
  - `docs/GOVERNANCE.md` (numbered order aligned with Freeze §2)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (conflicts resolved; hashes unchanged)
  - `CONTINUE.md` (checkpoint, pointers, read-order)
  - `docs/CHANGELOG.md` (one new Unreleased entry; prior entries untouched)
  - `docs/CHANGE_CONTROL_ADR042_FREEZE_GROUNDING.md` (this file)
- **Automated verification and result:** Freeze and Migration Spec SHA-256
  still match registry rows. Local AI Spec and Experience Memory Amendment
  hashes not recomputed because those files were not edited.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Freeze §7 claimed Spec/Amendment
  correction remains deferred until a later hashed change-control. MIG-*
  remains unstarted.
- **Documentation/traceability/ADR updates:** ADR-042; this record.
- **Git commit:** Local checkpoint after verification (no push).
