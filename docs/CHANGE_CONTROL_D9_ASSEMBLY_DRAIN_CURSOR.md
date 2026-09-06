# Change control: D-9 — assembly drain cursor advances on terminal per-asset outcomes

**Date:** 2026-09-06  
**Type:** Domain + application + Room (Memory assembly construction; not Find)  
**Closes:** PROGRAM_STATE D-9 / backlog A8  
**Does not authorize:** I2/I3 workers, meaning-index automation, AVAILABLE

## Pre-work record

- **Requirement IDs:** P-09 / P-11 (deterministic Asset Memory from saved facts);
  charter reliability (no unbounded work); PROGRAM_STATE D-9.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  PROGRAM_STATE_AND_SEQUENCE_V1 §2 D-9 / §8b A8 / Batch I, CHANGE_CONTROL_TEMPLATE.
- **Current-code evidence inspected:** `RunPendingAssetMemoryAssembly` aborted
  the drain on `NoUsableEvidence` / `AssetMissing` / `RevisionConflict` /
  `FailedSafely`; `findNextPendingAsset` is `ORDER BY … LIMIT 1` with no skip;
  PDF extract drain advances with `afterSourceAssetKey` + persisted status.
- **Open ADRs / platform:** no new ADR. Additive Room 15→16. Non-destructive.
- **Privacy:** on-device skip metadata only (identity + fingerprint + reason +
  fact digest). No originals. Cleared with derived-data wipe (DB recreate).
- **Smallest safe change:** persist terminal skips; exclude from pending
  select/count; clear skip when new facts land; abort only on infrastructure
  `FailedSafely`. No workers.
- **Acceptance criteria:**
  - [x] `NoUsableEvidence` records a skip and the next Asset in the batch assembles
  - [x] A second drain does not retry the same skipped Asset
  - [x] `FailedSafely` aborts and does **not** skip the failing Asset
  - [x] `AssetMissing` is skipped so the drain can continue
  - [x] Pending count and select share the same skip exclusion
  - [ ] Device: one unusable Asset no longer blocks Continue building (manual)
- **Holistic scenarios (before implement):**
  - User: tap Build; 1 unusable + 24 good → 24 built, 1 skipped, Continue if more
  - User: all-skip batch → Completed with skipped count, not Failed
  - User: later OCR arrives → skip cleared, Asset pending again
  - Technical: disk-full insert → FailedSafely, retry same Asset next tap
  - Technical: worker must not exist yet (I2 after this)
  - Edge: empty batch, already-present, revision conflict, missing Asset race

## Architectural convergence

```
N/A — not a Find/Recall change
```

## Delivery record

- **Files/layers:** domain `AssemblyFactsDigest` + `AssetMemoryAssemblyOutcomeStore`;
  Room `memory_assembly_skips` + migration 15→16; pending SQL exclude;
  persist ports clear skip on new facts; `RunPendingAssetMemoryAssembly`;
  setup copy; Hilt bind; `MemoraDatabaseMigrations.ALL`.
- **Automated verification:** `RunPendingAssetMemoryAssemblyTest`,
  `AssemblyFactsDigestTest`, `AssetMemorySetupCopyTest`. Full
  `:app:testDebugUnitTest` **722 tests, 0 failures** (2026-09-06).
- **Emulator/manual:** Room 15→16 on existing installs via
  `MemoraDatabaseMigrationTest` (assert version 16 + table exists). Device
  tap-through of a known-unusable Asset is a follow-up, not this gate.
- **Failure/recovery:** infrastructure abort unchanged; skip cleared on new
  facts; derived-data clear drops the table with the DB.
- **Known limitation:** I1 is delivered. I2–I4 still open. Skip is
  construction-time, not corpus-scope (D-14 self-capture remains Find-time).
- **Git commit:** recorded after JVM green.
