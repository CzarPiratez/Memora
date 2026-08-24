# Change control: Architecture Freeze v1.0 and Migration Spec V1 registration

**Date:** 2026-08-23  
**Type:** Documentation / governance registration  
**Decision guardrails:** Docs only. Do not rewrite freeze or migration-spec
substance. Do not start MIG-01–MIG-11 or MIG-07B. No application, database,
migration, retrieval, OCR, MemoryBuilder, RecallRanker, Grounded Answers,
Event/Knowledge/Links, package, or identity change. No deploy. No push. No
commit in this checkpoint unless the user later asks.

## Pre-work record

- **Requirement IDs:** P-01 / P-18 (scope control); A-02 (no new cloud path);
  G-01–G-08 (Grounded Answers remain architecture, not implementation);
  E-01–E-06 (PKI staged).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `DECISIONS`, `ARCHITECTURE`,
  `GROUNDING_ARCHITECTURE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC`,
  `EXPERIENCE_MEMORY_AMENDMENT_V1`, `ARCHITECTURAL_MIGRATION_SPEC_V1`,
  `ARCHITECTURE_FREEZE_v1.0`, `CHANGELOG`, `CHANGE_CONTROL_TEMPLATE`,
  identity playbook Step 2, prior architecture-registry change-control.
- **Current-code evidence inspected:** OneNote connector exists under
  `MemoraApp/app/src/main/java/com/memora/app/` (notes application, work, UI,
  domain). CONTINUE Notes row was stale (“strategy only — not implemented”).
  No `MemoraApp/` edits in this step. Existing registry SHA-256 rows verified
  against `git show HEAD:<path>` blob bytes. New freeze/spec files are LF on
  disk and were not previously in Git.
- **Open ADRs / platform limitations checked:** ADR-040 identity unchanged.
  ADR-033–039 Grounded Answers remain architecture. ADR-041 records
  registration plus open conflicts. Technical IDs stay deferred.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Place/keep the two supplied files in `docs/`, hash
  them, register them, record ADR-041 without resolving conflicts, truthful
  CONTINUE/governance pointers, one Unreleased changelog bullet, this record.
- **Acceptance criteria:**
  - Both files at `docs/ARCHITECTURE_FREEZE_v1.0.md` and
    `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` without substantive rewrite.
  - SHA-256 of Git-blob/file bytes recorded in the existing registry table.
  - Existing hashed-constitution rows unchanged.
  - Conflicts reported; no silent winner; no MIG-* start.
  - Notes status in CONTINUE matches N0–N7 connector implementation.
  - No `MemoraApp/` or schema/migration file edits.
- **Test and emulator verification plan:** Docs inspection and hash
  verification only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/ARCHITECTURE_FREEZE_v1.0.md` (add as-is)
  - `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` (add as-is)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (registration prose + two hash rows)
  - `docs/GOVERNANCE.md` (pointer that numbered order is not rewritten)
  - `docs/DECISIONS.md` (ADR-041)
  - `CONTINUE.md` (checkpoint, Notes honesty, read-order pointers)
  - `docs/CHANGELOG.md` (one new Unreleased entry; prior entries untouched)
  - `docs/CHANGE_CONTROL_ARCHITECTURE_FREEZE_REGISTRATION.md` (this file)
- **Automated verification and result:** SHA-256 of new files computed from
  on-disk bytes (LF). Existing constitution hashes match HEAD blobs.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** ADR-041 conflicts 1–3 must be decided by
  a human before Freeze exclusivity, GOVERNANCE order alignment, or claimed
  Spec/Amendment §7/§15 corrections can be applied. MIG-* remains unstarted.
- **Documentation/traceability/ADR updates:** ADR-041; this record.
- **Git commit:** Not created in this checkpoint (user instruction).
