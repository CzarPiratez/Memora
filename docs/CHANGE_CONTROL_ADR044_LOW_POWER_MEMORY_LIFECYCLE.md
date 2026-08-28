# Change control: ADR-044 low-power equals event-driven Memory lifecycle

**Date:** 2026-08-28  
**Type:** Documentation / product-direction interpretation  
**Decision guardrails:** Docs only. Do not rewrite hashed Product Contract,
Freeze, Spec, Grounding, or Amendment blobs. Do not reopen Architecture Freeze
v1.0. Do not authorize MIG-*, Grounded Answers implementation, always-on
camera/mic, or cloud AI on the core path. No application, database, Room,
retrieval, UI, or neuromorphic requirements. No performance number claims
without benchmarks. No deploy. No push.

## Pre-work record

- **Requirement IDs:** Spec §7 (WorkManager / battery / charging / idle /
  thermal; honest paused state); Spec §9 (ordinary Find: no original reopen /
  no per-result generative reasoning; Grounded Answers carve-out); Freeze §3
  (truth before intelligence, retrieval-first, evidence-first); ADR-043 (PKI /
  Core direction; Act out; Android reference shell).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `PRODUCT_CONTRACT`, `LOCAL_AI_TECHNICAL_SPEC` §7
  and §9, `ARCHITECTURE_FREEZE_v1.0` §3, `DECISIONS` (ADR-043 tone/structure),
  `CHANGE_CONTROL_TEMPLATE`, `CHANGELOG` Unreleased.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Hashed freeze/spec/amendment/grounding/contract files are not opened for
  rewrite. Unrelated dirty files (`docs/ROADMAP.md`,
  `MemoraApp/gradle/libs.versions.toml`, MIG-03 application paths) are not
  staged. Concurrent MIG-03 docs lines left untouched.
- **Open ADRs / platform limitations checked:** ADR-043 PKI / Act-out remains
  binding. Spec §7 already requires battery/storage/thermal honesty. Spec §9
  already separates ordinary Find from the Grounded Answers carve-out. This
  ADR adds interpretation and team language only.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. No new source, permission, always-on capture, or cloud
  path.
- **Smallest safe change:** Accept ADR-044; registry/CONTINUE pointers; one
  Unreleased changelog subsection; this record.
- **Acceptance criteria:**
  - ADR-044 present after ADR-043; older ADRs not restyled.
  - Decision states low-power = event-driven Memory lifecycle (interpretation).
  - Explicit non-authorization of MIG-*, Grounded Answers code, always-on
    sensing, cloud AI core path, and Freeze reopen.
  - Paused/deferred indexing under device-health constraints recorded as
    correct product behavior with honest UI.
  - ADR-043 cited; Act remains out; Android remains a reference shell.
  - One new Unreleased changelog subsection; prior Unreleased entries
    preserved.
  - No `MemoraApp/` or hashed architecture file edits.
- **Test and emulator verification plan:** Docs inspection only. Confirm no
  Kotlin/XML/Gradle/schema edits. Confirm hashed architecture files unchanged.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-044)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (Authority and interpretation pointer)
  - `CONTINUE.md` (short checkpoint / next-eng pointer)
  - `docs/CHANGELOG.md` (one new Unreleased docs-only subsection)
  - `docs/CHANGE_CONTROL_ADR044_LOW_POWER_MEMORY_LIFECYCLE.md` (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed freeze/spec/amendment/grounding/contract files not in
  the change set.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Interpretation only. Does not implement
  new WorkManager policy, UI copy, or Grounded Answers. Concurrent MIG-*
  authorization lines remain owned by their own change control.
- **Documentation/traceability/ADR updates:** ADR-044; this record.
- **Git commit:** Local checkpoint after verification (no push).
