# Change control: MIG-01 Evidence Class Taxonomy

**Date:** 2026-08-24  
**Type:** Domain + Room persistence (additive)  
**Decision guardrails:** Implement MIG-01 only. Do not start MIG-02–MIG-11 or
MIG-07B. Do not implement Grounded Answers, Links, Event/Knowledge, ranking or
UI on class, package rename, or ROADMAP/AGP toml. Do not rewrite hashed
Product Contract, Freeze, Migration Spec body, Local AI Spec, Grounding, or
ADR-043 substance. `ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION` stays
5. Experience Memory Amendment is hashed; status is recorded in CONTINUE and
changelog, not in that file. No push.

## Pre-work record

- **Requirement IDs:** Experience Memory evidence classes
  (`docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` §5); Local AI Spec §11; Migration
  Spec MIG-01. Supporting: P-09 (normalized Memory structure); E-02 / E-05
  (explainable evidence/provenance; class is stored now, not yet used for
  ranking or Why display).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC` §11,
  `EXPERIENCE_MEMORY_AMENDMENT_V1` §5 and §10, `ARCHITECTURE_FREEZE_v1.0` §3
  (retrieval signals never independently justify truth),
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-01 (canonical), ADR-040 / ADR-042 /
  ADR-043, `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** `Memory.kt` (`MemoryEvidence` had
  id/kind/locator/excerpt; `MemoryEvidenceKind` unchanged);
  `MemoryEntities.kt` / `MemoryRoomMapper.kt`;
  `AssembleAssetMemoryFromExtractionFacts.kt`; `MemoraDatabase.kt` version 12;
  `MemoraDatabaseMigrations.kt` through `MIGRATION_11_12`;
  `MemoraEncryptedDatabaseOpener` both `addMigrations` lists;
  schema `12.json` `memory_evidence` DDL; construction sites and instrumentation
  lists named in the delivery instruction. Grep after edits:
  `MemoryEvidence(` / `MemoryEvidenceEntity(` only at assembler, mapper, entity,
  domain, and the two unit fixtures.
- **Open ADRs / platform limitations checked:** ADR-043 Act remains out; that
  ADR is not rewritten. MIG-01 is authorized by the user's latest instruction
  over leftover “Do not start MIG-*” lines. Room additive `ALTER TABLE`
  backfill is SQLite-compatible. Conversion journal schema version is
  independent of Room 13.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new source, permission, network, or AI path. Existing evidence rows are
  classified `DIRECT` without reopening originals. Class is not shown to users.
- **Smallest safe change:** Required `MemoryEvidenceClass` field; assembler
  tags `DIRECT`; Room `evidence_class` TEXT NOT NULL; migration 12→13 default
  `'DIRECT'`; tests and governance docs for this checkpoint only.
- **Acceptance criteria:**
  - `MemoryEvidence` cannot be constructed without an explicit class (no
    Kotlin default).
  - All evidence from `AssembleAssetMemoryFromExtractionFacts` is `DIRECT`.
  - Mapper round-trips `evidenceClass`, including a non-`DIRECT` fixture.
  - Existing v12 `memory_evidence` rows backfill `DIRECT`; database version
    becomes 13.
  - No ranking/Find/Why/UI consumer of class.
- **Test and emulator verification plan:** Focused debug unit tests; Room
  kapt export of `13.json`. `MemoraDatabaseMigrationTest` on emulator if adb
  is available; otherwise record the gap. Manual install: existing index
  should open; no UI change.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - Domain: `MemoryEvidenceClass`; required `MemoryEvidence.evidenceClass`
  - Application: assembler tags `DIRECT`
  - Data: `MemoryEvidenceEntity.evidenceClass`; mapper `enum.name` /
    `valueOf`; Room version 13; `MIGRATION_12_13`; both opener migration lists
  - Tests: assembler, mapper, domain fixtures; migration 1→13 chain plus
    focused v12 evidence-row backfill; instrumentation lists gain
    `MIGRATION_12_13`
  - Schema export: `app/schemas/.../13.json` (Room-generated)
  - Docs: GOVERNANCE, registry, CONTINUE, CHANGELOG Unreleased, this record
- **Automated verification and result:**
  `:app:testDebugUnitTest` for
  `AssembleAssetMemoryFromExtractionFactsTest` (6 tests),
  `MemoryRoomMapperTest` (1 test), and `MemoryTest` (3 tests): **10/10 passed**,
  BUILD SUCCESSFUL. Room kapt wrote `13.json` with `evidence_class` TEXT NOT
  NULL. `EXPECTED_SCHEMA_VERSION` remains 5.
- **Emulator/manual verification and result:**
  `MemoraDatabaseMigrationTest` **did not run**. adb could not start a daemon
  in this environment (`cannot connect to daemon`). Do not treat the 12→13
  backfill as emulator-verified.
- **Failure/recovery paths verified:** Unit construction and mapping only.
  Missing-class construction is a compile error. Legacy rows rely on SQL
  `DEFAULT 'DIRECT'` (instrumentation pending).
- **Known limitation or follow-up:** Class is unused for ranking, display, or
  link eligibility (MIG-01 out of scope). MIG-02+ not started. Run
  `MemoraDatabaseMigrationTest` on a connected emulator when adb is available.
  Experience Memory Amendment §10 was not edited (hashed).
- **Documentation/traceability/ADR updates:** GOVERNANCE and registry MIG-01
  exception; CONTINUE checkpoint; CHANGELOG Unreleased; this record. ADR-043
  substance unchanged.
- **Git commit:** Local checkpoint after unit verification (no push).
