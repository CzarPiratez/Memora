# Change control: MIG-03 TIME and TOPIC Memory Anchors

**Date:** 2026-08-28  
**Type:** Application assembly policy (no Room schema change)  
**Decision guardrails:** Implement MIG-03 only. Do not start MIG-04–MIG-11 or
MIG-07B. Do not implement Grounded Answers, Links, Event/Knowledge, ranking or
Find/Why UI redesign, VisionEngine, embeddings (MIG-05), search-path cutover
(MIG-06/07), package rename, or ROADMAP/AGP toml. Do not rewrite hashed Product
Contract, Freeze, Migration Spec body, Local AI Spec, Grounding, Experience
Memory Amendment, or ADR-043 substance. No forced mass reindex of existing
memories. No push. Leave unrelated `docs/ROADMAP.md` / `libs.versions.toml`
unstaged.

## Pre-work record

- **Requirement IDs:** Migration Spec MIG-03 (canonical); Experience Memory
  Amendment §2.1 (searchable anchors on Asset Memory) and §2.2 (future Links
  need consistent permitted time among other cues); Product Contract recall by
  time and topic; Architecture Freeze §3 (evidence-first anchors; absence of
  an anchor is never a non-match — out of scope here to change ranking).
  Supporting: P-09 (normalized Memory structure), A-04 (assembly keyed by
  schema version).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC`,
  `EXPERIENCE_MEMORY_AMENDMENT_V1` §2, `ARCHITECTURE_FREEZE_v1.0` §3,
  `ARCHITECTURAL_MIGRATION_SPEC_V1` MIG-03 (canonical), ADR-040 / ADR-042 /
  ADR-043, `CHANGE_CONTROL_TEMPLATE`, prior MIG-01/MIG-02 change-control
  records.
- **Current-code evidence inspected:**
  `AssembleAssetMemoryFromExtractionFacts.kt` (TEXT-only anchors; schema v3;
  DIRECT evidence); `RoomAssetMemoryFactSource.kt` (`exif:fields` with
  `Date taken: …`; `pdf:title`; `note:title`);
  `AssembleAssetMemoryFromExtractionFactsTest.kt`; `Memory.kt`
  (`MemoryAnchorKind`, `MemorySignature` cite-evidence invariant).
- **Open ADRs / platform limitations checked:** ADR-043 Act remains out.
  User instruction authorizes MIG-03 over leftover “do not start MIG-03”
  governance lines. No Room migration required. MIG-01 residual already
  closed (emulator 2/2).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  No new source, permission, network, or AI path. Typed anchors appear only
  on next legitimate assembly for schema v4; originals remain read-only.
- **Smallest safe change:** Additive TIME/TOPIC anchor emission from existing
  deterministic facts; bump assembly schema to `asset-memory-facts-v4`;
  focused tests + governance docs for this checkpoint only.
- **Acceptance criteria:**
  - EXIF Date taken fact → TIME anchor cites that evidence; DIRECT.
  - `pdf:title` / `note:title` → TOPIC anchor cites that evidence.
  - Without those facts → no TIME/TOPIC fabricated; TEXT still present.
  - Existing >8 evidence / DIRECT / summary ≤240 tests remain green (schema
    string → v4).
  - No Room schema migration; no forced mass reindex.
- **Test and emulator verification plan:** Focused debug unit tests for the
  assembler. Manual: Build memories after schema v4 → expect TIME/TOPIC when
  EXIF/title facts exist.
- **User-visible quality/accessibility review plan:** N/A for UI redesign;
  anchor kinds are not yet consumed by Find/Why.

## Delivery record

- **Files/layers changed:**
  - Application: `AssembleAssetMemoryFromExtractionFacts` — `buildAnchors`
    adds TIME/TOPIC when eligible evidence survives sanitize;
    `ASSEMBLY_SCHEMA` → `asset-memory-facts-v4`
  - Tests: TIME, TOPIC (note + pdf), no-fabrication, camera-without-date;
    existing completeness/DIRECT/summary tests updated to v4
  - Docs: GOVERNANCE + registry authorize MIG-03 / block MIG-04+; CONTINUE
    checkpoint; CHANGELOG Unreleased; this record
  - Unchanged: Room schema, hashed blobs, ROADMAP, `libs.versions.toml`,
    ranking/Find/Why, Links, Grounded Answers code
- **Automated verification and result:**
  `:app:testDebugUnitTest` for
  `AssembleAssetMemoryFromExtractionFactsTest` (12 tests),
  `MemoryTest` (3), `MemoryRoomMapperTest` (1), and
  `AssetMemorySetupCopyTest` (2): **18/18 passed**, BUILD SUCCESSFUL.
- **Emulator/manual verification and result:** Not claimed in this
  checkpoint. When device works: Build memories and confirm TIME/TOPIC on
  assets with EXIF date / title facts under schema v4.
- **Failure/recovery paths verified:** Dimension-only EXIF still
  `NoUsableEvidence`; camera-without-date keeps EXIF evidence but no TIME
  anchor; assets without title/date keep TEXT only.
- **Known limitation or follow-up:** Existing v3 memories retain TEXT-only
  anchors until next legitimate reassembly under v4 (by design). Ranking/UI
  do not yet filter by anchor kind (MIG-07B / later). MIG-04+ not started.
- **Documentation/traceability/ADR updates:** GOVERNANCE,
  PRODUCT_SOURCE_REGISTRY, CONTINUE, CHANGELOG Unreleased, this record.
  Hashed specs unchanged. ADR-043 substance unchanged.
- **Git commit:** Local checkpoint after unit verification (no push).
