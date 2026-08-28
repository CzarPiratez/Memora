# Change control: MIG-02 Remove Artificial Evidence Item/Length Caps

**Date:** 2026-08-28  
**Type:** Application assembly policy (no Room schema change)  
**Decision guardrails:** Implement MIG-02 only. Do not start MIG-03–MIG-11 or
MIG-07B. Do not implement Grounded Answers, Links, Event/Knowledge, ranking or
Find/Why UI redesign, embeddings (MIG-05), search-path cutover (MIG-06/07),
package rename, or ROADMAP/AGP toml. Do not rewrite hashed Product Contract,
Freeze, Migration Spec body, Local AI Spec, Grounding, Experience Memory
Amendment, or ADR-043 substance. No forced mass reindex of existing memories.
No push. Leave unrelated `docs/ROADMAP.md` / `libs.versions.toml` unstaged.

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-02 (canonical); Experience Memory
  Amendment §2.1 (Asset Memory holds source-derived evidence; summary is
  concise and evidence-cited); Architecture Freeze §3 (one evidence substrate;
  retrieval-first; truth before intelligence); Product Contract lifecycle
  Understand → Store; Local AI Spec (store evidence for recall without
  re-reading originals). Supporting: P-09 (normalized Memory structure),
  A-04 (assembly keyed by schema version).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC`,
  `EXPERIENCE_MEMORY_AMENDMENT_V1` §2.1, `ARCHITECTURE_FREEZE_v1.0` §3,
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-02 (canonical), ADR-040 / ADR-042 /
  ADR-043, `CHANGE_CONTROL_TEMPLATE`, prior `CHANGE_CONTROL_MIG01_EVIDENCE_CLASS`.
- **Current-code evidence inspected:**
  `AssembleAssetMemoryFromExtractionFacts.kt` (`MAX_EVIDENCE_ITEMS = 8`,
  `MAX_EVIDENCE_CHARS = 500`, `MAX_SUMMARY_CHARS = 240`, schema
  `asset-memory-facts-v2`); `AssembleAssetMemoryFromExtractionFactsTest.kt`
  (no test asserted “8 is correct”); `RunPendingAssetMemoryAssembly.kt`
  (batch limit only; no item-cap coupling); `MemoryAssemblySchemaVersion`
  usages (pending assembly keyed by current schema); `PdfExtractionWriteBudgets`
  (`MAX_CHARS_PER_PAGE = 8192`).
- **Open ADRs / platform limitations checked:** ADR-043 Act remains out.
  User instruction authorizes MIG-02 over leftover “do not start MIG-02”
  governance lines. No Room migration required. MIG-01 residual:
  `MemoraDatabaseMigrationTest` not emulator-verified (adb/connect); do not
  block MIG-02 on fixing emulator in this change; do not claim instrumentation
  green.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new source, permission, network, or AI path. Richer evidence is stored
  only on next legitimate assembly for schema v3; originals remain read-only.
- **Smallest safe change:** Remove fixed item-count take; replace 500-char cap
  with documented 8192 pathological per-item guard; keep summary ≤240 as
  display-only; bump assembly schema to `asset-memory-facts-v3`; tests +
  governance docs for this checkpoint only.
- **Acceptance criteria:**
  - Fixture with >8 facts → Memory evidence count >8 (all within per-item
    bound).
  - Summary still ≤ 240 chars.
  - No test asserts “8 is correct”; completeness asserted instead.
  - `MemoryAssemblySchemaVersion` incremented; no forced mass reindex.
  - `RunPendingAssetMemoryAssembly` logic unchanged (batching assumptions hold).
- **Test and emulator verification plan:** Focused debug unit tests for the
  assembler. Manual: Build memories on emulator when device works (v3 pending
  assets reassemble with richer evidence). Do not claim MIG-01 instrumentation
  green.
- **User-visible quality/accessibility review plan:** N/A for UI redesign;
  Build-memories path may take longer per Memory with large fact sets — batch
  limit (25) unchanged.

## Delivery record

- **Files/layers changed:**
  - Application: `AssembleAssetMemoryFromExtractionFacts` — remove
    `MAX_EVIDENCE_ITEMS`; `MAX_EVIDENCE_CHARS_PER_ITEM = 8192` with rationale;
    `ASSEMBLY_SCHEMA` → `asset-memory-facts-v3`; summary KDoc clarifies
    display-only role
  - Tests: completeness (>8 facts), summary bound, per-item truncation
  - Docs: GOVERNANCE + registry authorize MIG-02 / block MIG-03+; CONTINUE
    checkpoint; CHANGELOG Unreleased; this record
  - Unchanged: `RunPendingAssetMemoryAssembly`, Room schema, hashed blobs,
    ROADMAP, `libs.versions.toml`
- **Automated verification and result:**
  `:app:testDebugUnitTest` for
  `AssembleAssetMemoryFromExtractionFactsTest` (9 tests),
  `MemoryRoomMapperTest` (1), `MemoryTest` (3), and
  `AssetMemorySetupCopyTest` (2): **15/15 passed**, BUILD SUCCESSFUL
  (~28m cold Gradle/JDK25 first run). No existing
  `RunPendingAssetMemoryAssembly` unit suite to re-run; drain code unchanged.
- **Emulator/manual verification and result:** Not claimed in this
  checkpoint. When device works: Build memories and confirm multi-page PDF
  memories can hold >8 evidence items. MIG-01 `MemoraDatabaseMigrationTest`
  still not run (adb/emulator residual); do not treat Room 12→13 as
  instrumentation-green.
- **Failure/recovery paths verified:** Empty/unusable facts still yield
  `NoUsableEvidence`. Pathological single-fact length still bounded at 8192.
- **Known limitation or follow-up:** Existing v2 memories remain capped until
  next legitimate reassembly under v3 (by design). Search still bypasses
  Memory for some paths (MIG-06/07). MIG-03+ not started. MIG-01 Room 12→13
  instrumentation gap remains open.
- **Documentation/traceability/ADR updates:** GOVERNANCE, PRODUCT_SOURCE_REGISTRY,
  CONTINUE, CHANGELOG Unreleased, this record. Hashed specs unchanged.
  ADR-043 substance unchanged.
- **Git commit:** Local checkpoint after unit verification (no push).
