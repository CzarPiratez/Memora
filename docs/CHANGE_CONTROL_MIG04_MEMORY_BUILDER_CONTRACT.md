# Change control: MIG-04 MemoryBuilder Contract Boundary

**Date:** 2026-08-28  
**Type:** Domain/application contract seam (no Room schema change; no assembly
behavior change)  
**Decision guardrails:** Implement MIG-04 only. Do not start MIG-05–MIG-11 or
MIG-07B. Do not implement VisionEngine, observation processing, RecallRanker
operate, embeddings store (MIG-05), search-path cutover (MIG-06/07), Grounded
Answers, Links, Event/Knowledge, ranking or Find/Why UI redesign, package
rename, or ROADMAP/AGP toml. Do not rewrite hashed Product Contract, Freeze,
Migration Spec body, Local AI Spec, Grounding, Experience Memory Amendment, or
ADR-043 / ADR-044 substance. No assembly schema bump (remain
`asset-memory-facts-v4`). No push. Leave unrelated `docs/ROADMAP.md` /
`libs.versions.toml` unstaged.

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-04 (canonical); Local AI Spec §4
  (`MemoryBuilder` combines deterministic extraction + local observations into
  a schema-validated Memory; may not invent unsupported source facts) and §13
  (capability contract before AI/model dependency); Experience Memory Amendment
  §3 (Memory Builder validates extraction + permitted observations) and §8
  (behavioral contracts before storage/model work on correlation); Architecture
  Freeze §3 (evidence-first construction). Supporting: A-03 (replaceable
  capability interfaces).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC` §4 /
  §13, `EXPERIENCE_MEMORY_AMENDMENT_V1` §3 / §8, `ARCHITECTURE_FREEZE_v1.0` §3,
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-04 (canonical), ADR-043 / ADR-044,
  `CHANGE_CONTROL_TEMPLATE`, prior MIG-01–03 change-control records.
- **Current-code evidence inspected:**
  `LocalIntelligenceEngines.kt` (MemoryBuilder availability-only);
  `UnavailableLocalIntelligence.kt` (UnavailableMemoryBuilder);
  `CapabilityAvailability` / `EmbeddingEngine.embedText` pattern;
  `AssembleAssetMemoryFromExtractionFacts.kt`;
  `RunPendingAssetMemoryAssembly.kt`; `LocalIntelligenceModule.kt`;
  `AssembleAssetMemoryFromExtractionFactsTest.kt`.
- **Open ADRs / platform limitations checked:** ADR-043 Act remains out.
  ADR-044 interpretation-only (unchanged). User authorizes MIG-04 over leftover
  “do not start MIG-04” governance lines. No Room migration. No schema bump.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new source, permission, network, or AI path. Observations list accepted
  empty; non-empty rejected. Originals remain read-only.
- **Smallest safe change:** Add `assemble` on MemoryBuilder; extract
  DeterministicMemoryBuilder; wire DI + use case; keep assembly output
  identical; focused builder tests; governance docs for this checkpoint only.
- **Acceptance criteria:**
  - MemoryBuilder has operate/assemble matching Spec §4.
  - Deterministic assembler is the concrete Available MemoryBuilder.
  - Drain path goes through MemoryBuilder (via existing use case).
  - Existing AssembleAssetMemoryFromExtractionFactsTest assertions pass
    unmodified in substance (construction may wire MemoryBuilder).
  - Empty observations default; non-empty rejected clearly.
  - UnavailableMemoryBuilder does not invent Memories.
  - DI binds DeterministicMemoryBuilder, not UnavailableMemoryBuilder.
- **Test and emulator verification plan:** Focused debug unit tests for
  assembler + DeterministicMemoryBuilder / UnavailableMemoryBuilder.
- **User-visible quality/accessibility review plan:** N/A — Build memories
  behavior unchanged; no Find/Why UI change.

## Delivery record

- **Files/layers changed:**
  - Domain: `MemoryBuilder.assemble` + `LocalObservation` + `MemoryBuildResult`;
    `DeterministicMemoryBuilder`; `UnavailableMemoryBuilder.assemble`
  - Application: `AssembleAssetMemoryFromExtractionFacts` orchestrates and
    injects MemoryBuilder (empty observations)
  - DI: `LocalIntelligenceModule` binds DeterministicMemoryBuilder as
    MemoryBuilder
  - Tests: existing assembler suite (wiring only); new
    `DeterministicMemoryBuilderTest` / Unavailable assemble honesty
  - Docs: GOVERNANCE + registry authorize MIG-04 / block MIG-05+; CONTINUE
    checkpoint; CHANGELOG Unreleased; this record
  - Unchanged: Room schema, assembly schema v4, hashed blobs, ROADMAP,
    `libs.versions.toml`, VisionEngine, ranking/Find/Why, Links, Grounded
    Answers code
- **Automated verification and result:**
  `:app:testDebugUnitTest` focused suites: `AssembleAssetMemoryFromExtractionFactsTest`
  (12), `DeterministicMemoryBuilderTest` (3), `UnavailableMemoryBuilderAssembleTest`
  (1), `UnavailableLocalIntelligenceTest` (3), `MemoryTest` (3),
  `MemoryRoomMapperTest` (1): **23/23 passed**, BUILD SUCCESSFUL.
- **Emulator/manual verification and result:** Not claimed in this
  checkpoint. Build memories path should behave identically (deterministic
  facts → same Memory shape under schema v4).
- **Failure/recovery paths verified:** Empty facts → NoUsableEvidence;
  non-empty observations → ObservationsUnsupported / FailedSafely at use case;
  UnavailableMemoryBuilder → Unavailable (no invented Memory).
- **Known limitation or follow-up:** VisionEngine must feed
  VALIDATED_OBSERVATION on this same seam (not a parallel assembler). MIG-05+
  not started. ADR-044 unchanged.
- **Documentation/traceability/ADR updates:** GOVERNANCE,
  PRODUCT_SOURCE_REGISTRY, CONTINUE, CHANGELOG Unreleased, this record.
  Hashed specs unchanged. ADR-043 / ADR-044 substance unchanged.
- **Git commit:** Local checkpoint after unit verification (no push).
