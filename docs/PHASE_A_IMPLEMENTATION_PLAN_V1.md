> **Repository filing:** Not hashed in `docs/PRODUCT_SOURCE_REGISTRY.md`. Historical/delivery only; not architectural authority. `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` still governs sequence. `docs/ARCHITECTURE_FREEZE_v1.0.md` still governs architecture.

# Memora — Implementation Execution Plan v1.1

**Status:** Execution plan for the current repository state
**Supersedes:** `MEMORA_IMPLEMENTATION_EXECUTION_PLAN_V1.md` — corrected per the review below, not a redesign
**Purpose:** Guide Cursor through the remaining implementation work without reopening the frozen architecture

**What changed from v1.0, and why:**
1. MIG-01, MIG-02, and MIG-03's descriptions (§10–§12) were factually wrong — they didn't match `ARCHITECTURAL_MIGRATION_SPEC_V1.md`. Corrected to the actual canonical text.
2. The authority hierarchy (§2) ranked `ARCHITECTURE_FREEZE_v1.0.md` as a flat #1 content authority above `PRODUCT_CONTRACT.md`. Reframed — it's the governance/change-control layer over the other four documents, not a competing content authority.
3. A standing rule was added (§2A) making explicit what was previously only implicit in Phase 0 and Step B: this plan describes intent; the current repository determines exact implementation location. This generalizes a specific catch from the Phase A review to every phase and every migration, not just Phase 1.
4. Phase 1 items P1.2–P1.6 (§8) now carry the specific files, classes, and existing tests found during the platform audit and Phase A review, explicitly marked as needing re-verification against the repository before use, per §2A.
5. P1.7 now carries the two specific feasibility questions from the Phase A review instead of a generic "defer if broad" instruction.

Everything else — the non-goals list, execution model, stop protocol, and checkpoint discipline — was already sound and is preserved as written.

---

# 1. Purpose

Memora has completed its architecture/product specification and Android platform-capability audit, but the canonical architecture migration roadmap has **not yet been implemented**.

This document is therefore an **execution wrapper**, not a replacement for the canonical architecture or migration documents.

It answers one practical question:

> **What should Cursor do next, in what order, starting from the repository's actual current state?**

The objective is to move the current implementation toward the frozen Memora architecture and MVP acceptance state while using Android/device capabilities wherever they already satisfy a required capability.

---

# 2. Authority hierarchy

This document is subordinate to the canonical Memora documents.

**Content authority, in order:**

1. `PRODUCT_CONTRACT.md`
2. `LOCAL_AI_TECHNICAL_SPEC.md`
3. `EXPERIENCE_MEMORY_AMENDMENT_V1.md`
4. `ARCHITECTURAL_MIGRATION_SPEC_V1.md` — sequencing and implementation-planning authority only; where it appears to imply a rule not stated in 1–3, they govern.
5. Repository governance/source-registry documents (e.g. `CONTINUE.md`, ADRs)
6. This execution plan
7. Current implementation details

`ARCHITECTURE_FREEZE_v1.0.md` sits above all of this as the **governance and change-control layer**, not as an additional content authority competing with `PRODUCT_CONTRACT.md`. It defines the canonical document set, certifies their internal consistency, and governs the process by which any of documents 1–4 may ever be changed. Nothing in this execution plan may be used to justify a change to a canonical document without going through that process.

This document must **not** silently override a canonical requirement.

If the repository contradicts a canonical document, stop and report the contradiction rather than inventing a new interpretation.

---

# 2A. Standing rule — the repository is the ground truth for implementation location

This plan, the migration specification's "files/modules affected" notes, and every finding in the Android platform capability audit describe **intent**, not a guarantee of current file state. The repository can change between when a document was written and when it's acted on — including by Cursor itself in an earlier session.

> **The plan describes the intended change. The current repository determines the exact implementation location.**

Concretely: before touching any file named in this document, confirm the file still exists at that path, the class/function named still has that name and shape, and the described current behavior still matches. If it doesn't, stop and report the discrepancy rather than proceeding on the plan's stale description or silently improvising a fix. This is not a one-time check for Phase 1 — it applies identically to every migration in Phase 2, and is why Step B ("Inspect") in §18 exists as its own mandatory step before Step C ("Plan") for every migration, not just the first one.

---

# 3. Current project state

The architecture is frozen.

The Android platform audit is complete.

The repository already uses Android/platform capabilities in many appropriate areas, including:

- MediaStore;
- AndroidX ExifInterface;
- ML Kit OCR;
- MediaPipe embeddings;
- WorkManager;
- Android Keystore;
- Android isolated-process PDF handling.

However:

> **MIG-01 through MIG-07B have not yet been implemented as the completed migration sequence.**

Therefore the repository must be treated as a **pre-migration implementation**.

Do not assume that the current code already satisfies the target architecture merely because the target architecture is documented.

The migration roadmap is the mechanism for moving the repository from its current state to the frozen target state.

---

# 4. Strategic implementation principle

The new Android-first direction does **not** require a new architecture.

The intended implementation principle is:

> **Use Android/device capabilities where they already satisfy a Memora capability; use Memora-owned infrastructure where the platform does not provide the required memory, evidence, retrieval, provenance, or explanation capability.**

Examples already present in the repository:

```text
Android / platform
    ├── MediaStore
    ├── Storage Access Framework
    ├── ExifInterface
    ├── ML Kit OCR
    ├── WorkManager
    ├── Keystore
    └── isolated process
             ↓
        Memora adapters
             ↓
        Memora Core
```

Do not replace this with a vendor-specific AI architecture.

---

# 5. Absolute non-goals

Cursor must NOT:

- reopen the architecture freeze;
- create Architecture v2;
- create a second Memory substrate;
- create a second evidence substrate;
- create a parallel recall pipeline;
- introduce a cloud AI dependency;
- send user content, embeddings, queries, prompts or Memories to a remote AI service;
- introduce Gemini Nano/AICore merely because it is available;
- introduce MNN merely because it is available;
- introduce llama.cpp merely because it is available;
- replace the current MediaPipe/USE embedding implementation unless a canonical migration explicitly requires it;
- introduce a new embedding model during the migration unless the canonical migration requires it;
- create an independent FTS migration;
- create a thermal-management project before measurement demonstrates a need;
- migrate AI Pack delivery to Google Play as part of this work;
- add reverse geocoding for EXIF GPS;
- create duplicate versions of canonical documents;
- modify original source files;
- perform unrelated refactors.

If a proposed change is not necessary to satisfy the current migration task, defer it.

---

# 6. Execution model

Work in **small, verified, sequential checkpoints**.

The overall sequence is:

```text
PHASE 0
Repository preflight + migration-state confirmation
        ↓
PHASE 1
Small verified pre-migration corrections
        ↓
MIG-01
Introduce Evidence Class Taxonomy
        ↓
MIG-02
Remove Artificial Evidence Item/Length Caps in Memory Assembly
        ↓
MIG-03
Populate TIME and TOPIC Memory Anchors from Existing Deterministic Facts
        ↓
MIG-04
Formalize and Freeze the MemoryBuilder Contract Boundary
        ↓
MIG-05
Generalize the Embedding Store to Evidence-Level Granularity
        ↓
MIG-06
Introduce a Unified Memory-Evidence Search Use Case
        ↓
MIG-07
Cut Over to Unified Search / Retire Parallel Keyword-Search Implementations
        ↓
MIG-07B
Anchor-Aware Structured Recall
        ↓
MVP migration verification
        ↓
MIG-08 / MIG-09 / MIG-10 as appropriate
```

Do not skip directly to MIG-07 because a later component appears easier.

Do not implement multiple migrations simultaneously.

---

# 7. PHASE 0 — Repository preflight

Before any code change:

1. Read repository governance/instruction files.
2. Read the canonical architecture/product/Local AI documents.
3. Read the complete `ARCHITECTURAL_MIGRATION_SPEC_V1.md`.
4. Read `CONTINUE.md`, current roadmap and current migration state.
5. Inspect the actual repository.
6. Confirm which migration-related files already exist.
7. Determine which migration acceptance tests already exist.
8. Confirm current branch and working-tree state.
9. Confirm there are no unrelated changes that will be mixed into the work.

### Required report

Cursor must report:

- current branch;
- current commit;
- working-tree state;
- canonical documents read;
- current migration state;
- existing implementation relevant to MIG-01;
- existing tests relevant to MIG-01;
- discrepancies between documents and repository (per §2A).

Then STOP.

Do not implement MIG-01 in the same turn as preflight unless explicitly instructed after the preflight report has been reviewed.

---

# 8. PHASE 1 — Small verified pre-migration corrections

These are corrections identified by the Android platform audit that are sufficiently independent of the migration architecture to be addressed before or alongside the migration.

They must not become a second migration program.

**Every file, class, and test name below must be reconfirmed against the current repository before use, per §2A.** They reflect the state found during the platform capability audit and Phase A review, not a guarantee of current state.

## P1.1 — AI model integrity verification

*(Source: `LOCAL_AI_TECHNICAL_SPEC.md` §6's AI Pack manifest integrity-hash requirement — not a finding from the platform capability audit. Confirm this is not already satisfied by an existing mechanism before treating it as new work.)*

### Objective

Ensure the on-device embedding model is cryptographically verified against the approved AI Pack manifest integrity hash before activation.

### Required flow

```text
download
   ↓
HTTP/status validation
   ↓
size validation
   ↓
cryptographic hash
   ↓
approved manifest hash comparison
   ↓
PASS → atomic activation
FAIL → reject artifact
        preserve prior known-good model
```

### Requirements

- Use the existing AI Pack manifest/registry/verification boundary.
- Do not hard-code the hash in a downloader.
- Do not create a second model registry.
- Do not activate an unverified model.
- Do not destroy a known-good model when an update fails.

### Tests

At minimum:

- valid bytes + expected hash;
- modified bytes + expected hash;
- failed verification;
- existing known-good model survives failed update;
- temporary artifact cleanup;
- successful activation.

If the existing architecture already has an equivalent integrity mechanism that is merely difficult to locate, do not duplicate it. Reconcile the implementation with the existing mechanism.

---

## P1.2 — Photo OCR WorkManager constraint

**File:** `work/MediaStorePhotoOcrExtractWorkScheduler.kt` — at time of review, builds its `WorkRequest` with `Constraints.Builder().build()` (no constraints), while sibling schedulers (`MediaStoreDiscoveryWorkScheduler.kt`, `MediaStoreImageExifExtractWorkScheduler.kt`, `SafPdfDiscoveryWorkScheduler.kt`, `SafPdfExtractWorkScheduler.kt`) all set `.setRequiresBatteryNotLow(true)`.

**Verify first, per §2A** — confirm this is still the case before changing anything.

Apply the established `BatteryNotLow` policy to match the sibling schedulers.

**Tests:** No dedicated scheduler-level test existed for this class at time of review (confirmed by repository-wide search for `*Scheduler*Test*` — zero results anywhere in the module). If still true, add a new unit test asserting the built `WorkRequest.workSpec.constraints.requiresBatteryNotLow == true`. Plain JUnit against the built request; no instrumentation needed.

Do not modify already-correct sibling schedulers.

---

## P1.3 — Screenshot OCR WorkManager constraint

**File:** `work/MediaStoreScreenshotOcrExtractWorkScheduler.kt` — at time of review, also builds its `WorkRequest` with no constraints, same gap as P1.2. (Note: an earlier pass at this audit incorrectly stated this scheduler already had the constraint — re-verified twice independently and corrected; both P1.2 and P1.3 are real gaps, not one.)

**Verify first, per §2A.**

Apply the same established `BatteryNotLow` policy.

**Tests:** Same situation as P1.2 — no existing scheduler-level test found; add one following the same pattern.

---

## P1.4 — EXIF GPS

**Files, at time of review:**
- `domain/extraction/ImageExifExtraction.kt` — `ImageExifReadResult.Facts` has no GPS fields; `ImageExifSchemaVersion` has only `V1`.
- `data/mediastore/ContentResolverImageExifReader.kt` — already depends on `androidx.exifinterface.media.ExifInterface`; add a read of `ExifInterface.getLatLong(FloatArray)` (no new dependency).
- `data/local/ImageExifExtractionEntity.kt` — add nullable `latitude`/`longitude` columns.
- `data/local/MemoraDatabaseMigrations.kt`, `data/local/MemoraDatabase.kt` — current schema version at time of review was 12; a GPS column addition is a new additive migration (13), nullable, no backfill required.

**Verify first, per §2A** — in particular reconfirm the current schema version before assuming it's still 12.

Use the existing ExifInterface capability to expose GPS information when present.

Constraints:

- deterministic;
- local;
- no network;
- no reverse geocoding;
- no named-place assertion unless another existing source already provides it.

This item produces facts only — it does **not** create a `PLACE` anchor. Anchor population remains MIG-03's and future work's scope.

**Tests:** No dedicated unit test existed for `ContentResolverImageExifReader` at time of review (only a WorkDecisionMapper test existed, covering a different class). Add one covering: GPS present, GPS absent (should produce `null`, not a failure), and `getLatLong` throwing (should follow this class's existing exception-handling pattern). Extend `MemoraDatabaseMigrationTest.kt` with the new version's migration case, following its existing pattern.

---

## P1.5 — Screenshot classification

**File, at time of review:** `data/mediastore/MediaStoreImageMapper.kt` — `isScreenshot()` uses a substring match against `relativePath`/`displayName` containing "screenshot."

**Verify first, per §2A.**

Improve deterministic screenshot detection using Android-supported screenshot conventions plus the existing conservative fallback — specifically, check `Environment.DIRECTORY_SCREENSHOTS` (available API 31+) as an additional signal, gated by SDK version following the same branching pattern already used elsewhere in this module, keeping the substring match as the fallback for older API levels and OEM variance.

Do not add ML.

**Tests:** `test/java/com/memora/app/data/mediastore/MediaStoreImageMapperTest.kt` exists and covers this function at time of review — extend it, don't replace it. Add cases for false-positive and false-negative behavior at the API-level boundary.

---

## P1.6 — Selected-photo access

Inspect the current source-access implementation.

**At time of review, the domain contract already correctly models this** — `domain/discovery/ImageLibraryAccessScope` (`FULL_LIBRARY`, `SELECTED_PHOTOS`) and `ImageLibraryDiscoverySource`, with `MediaStoreImageDiscoverySource.accessScope()` already correctly detecting and mapping `MediaStoreAccess.FULL`/`.SELECTED`/`.REQUIRED`. **Verify this is still the case per §2A before assuming it.**

If confirmed present: do not rebuild this detection logic. The gap, at time of review, was narrower — no code anywhere invoked the Photo Picker UI itself (`PickVisualMedia`/`ACTION_PICK_IMAGES`; confirmed absent by repository-wide search). If still absent, implement the missing adapter: a thin wrapper around `ActivityResultContracts.PickVisualMedia` (or `PickMultipleVisualMedia`), plus a new result-handling method on `ui/setup/MediaStoreSetupViewModel.kt` following the same pattern as its existing `onPhotoPermissionResult(isGranted: Boolean)`.

### Critical rule

Do not replace legitimate full-library MediaStore access with Photo Picker.

Photo Picker is an appropriate mechanism for selected access, not a forced replacement for full access. Concretely: the picker should be offered only when current access is already `SELECTED_PHOTOS` (to expand or change the selection), never presented as a required or default step when `FULL_LIBRARY` access is already granted. `MediaStoreSetupViewModel`'s `PhotoAccessState.COMPLETED.accessScope` already carries the information needed to make this conditional correctly.

Verify behavior on a supported Android configuration.

**Tests:** `androidTest/java/com/memora/app/data/mediastore/MediaStoreImageDiscoverySourceTest.kt` exists at time of review and is the natural home for confirming `accessScope()` still reports correctly after a picker-driven selection change — extend it. Add a new unit test for the ViewModel's new result-handling method following its existing test patterns.

---

## P1.7 — Model-delivery boundary

Inspect the current embedding-model downloader and AI Pack manager (`domain/intelligence/AiPackContracts.kt`, `data/local/AiPackInstallLedger*.kt`, `data/intelligence/NoBackupAiPackPayloadStore.kt` at time of review).

This is a **feasibility question, not implementation work**, per the earlier Phase A review. Before making any change, resolve:

1. Does Google Play's AI-pack delivery mechanism support Memora's model/format requirements outside Play Store distribution during development?
2. Does it support the license and redistribution-rights metadata Memora's AI Pack manifest already requires?

If both are confirmed favorable and a small, well-scoped change would move HTTP responsibility behind the existing platform/data capability boundary without a broad refactor, do so. If it requires a broad refactor, or either question above can't be resolved with confidence, defer it and report the finding — this is explicitly separate future work, not part of this migration (see §5, §22).

---

# 9. PHASE 1 completion gate

Before MIG-01:

- all Phase 1 changes must pass their focused tests;
- no canonical contract may be changed;
- no network data-egress behavior may be introduced;
- current embedding/retrieval behavior must remain functional;
- working-tree diff must contain only intended changes.

If a Phase 1 item is not necessary for safe migration and would cause meaningful churn, document it as deferred and proceed to MIG-01.

Do not allow Phase 1 to become an architecture redesign.

---

# 10. MIG-01 — Introduce Evidence Class Taxonomy

Use `ARCHITECTURAL_MIGRATION_SPEC_V1.md`'s MIG-01 section as the authoritative specification — reproduced in summary below; the canonical document's full text governs over this summary in any conflict.

### Objective

Add the epistemic evidence-class taxonomy (`DIRECT`, `VALIDATED_OBSERVATION`, `RETRIEVAL_SIGNAL`, `HYPOTHESIS`) to stored evidence, as required by `LOCAL_AI_TECHNICAL_SPEC.md` §11 and `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §5. All evidence currently produced by deterministic extraction is tagged `DIRECT`.

### Cursor requirements

Before coding:

- inspect `domain/memory/Memory.kt`'s current `MemoryEvidence`/`MemoryEvidenceKind` shape;
- confirm this is purely additive — no existing behavior should change beyond the new field and its default;
- identify the Room migration this requires and its target version number, confirmed against the current schema, per §2A.

### Constraints

- purely additive; no existing query, use case, or UI surface should need to change;
- do not conflate this with `MemoryEvidenceKind` (source-format taxonomy) — this is a separate, orthogonal epistemic-status taxonomy.

### Verification

Run the MIG-01 acceptance tests (per the canonical spec) and the existing Room migration test suite.

Then stop.

---

# 11. MIG-02 — Remove Artificial Evidence Item/Length Caps in Memory Assembly

Use `ARCHITECTURAL_MIGRATION_SPEC_V1.md`'s MIG-02 section as the authoritative specification.

### Objective

Remove the fixed evidence-item-count and per-item-length caps in the deterministic Memory assembler, so a Memory's stored evidence can represent the full extracted content of its Asset rather than an arbitrarily truncated preview. The UI-facing summary length limit is retained as a display concern, separated from what's persisted as searchable evidence.

### Cursor requirements

Before coding:

- inspect the current assembly function's cap constants and confirm their current values, per §2A;
- confirm the separation between "evidence persisted" and "summary displayed" doesn't already exist in some other form before assuming it needs to be built from scratch;
- check current write-path performance benchmarks so the change can be verified against them, not just against correctness tests.

### Constraints

- this is a root-cause fix for search fragmentation (per the completeness/closure review) — it is a prerequisite for MIG-05/06/07, not optional polish;
- do not retroactively reassemble existing Memories as part of this migration; rely on the existing stale-reindex mechanism.

### Verification

Run the MIG-02 acceptance tests and the existing write-path benchmark tests.

Then stop.

---

# 12. MIG-03 — Populate TIME and TOPIC Memory Anchors from Existing Deterministic Facts

Use `ARCHITECTURAL_MIGRATION_SPEC_V1.md`'s MIG-03 section as the authoritative specification.

### Objective

Wire already-extracted deterministic facts (EXIF capture timestamp, PDF/note title) into `TIME` and `TOPIC` `MemoryAnchor` instances, instead of collapsing all recall signal into a single generic `TEXT` anchor. `PERSON`, `PLACE`, `OBJECT`, `ACTIVITY`, `PURPOSE` anchors remain unpopulated — this migration does not fabricate them.

### Cursor requirements

Before coding:

- confirm which extraction facts are currently distinguishable as EXIF-timestamp or title facts in the fact list consumed by the assembler, per §2A;
- if P1.4 (EXIF GPS) has already landed, confirm it hasn't changed the fact shape in a way that affects this migration's anchor-building logic.

### Constraints

- do not fabricate an anchor in the absence of the underlying fact;
- do not implement any ranking or filtering behavior based on anchor kind — that is MIG-07B's scope, not this one;
- do not implement `PLACE`, `PERSON`, `OBJECT`, `ACTIVITY`, or `PURPOSE` anchors here.

### Verification

Run the MIG-03 acceptance tests, confirming both the positive case (fact present → anchor produced, citing the correct evidence) and the negative case (fact absent → no anchor fabricated).

Then stop.

---

# 13. MIG-04 — MemoryBuilder convergence

Use the canonical migration specification (`ARCHITECTURAL_MIGRATION_SPEC_V1.md`, MIG-04 — "Formalize and Freeze the MemoryBuilder Contract Boundary").

The target is the single MemoryBuilder seam.

### Critical rule

Do not create a second AI pipeline.

Local AI capabilities such as OCR, embeddings or future vision models are capability providers to MemoryBuilder.

They are not separate memory systems.

A deterministic-only implementation may remain valid where the current contract permits it.

### Verification

Run:

- MemoryBuilder tests;
- evidence-link tests;
- deterministic extraction tests;
- existing retrieval regressions.

Then stop.

---

# 14. MIG-05 — Generalize the Embedding Store to Evidence-Level Granularity

This migration is especially important.

The target is generic evidence-level indexing rather than a collection of asset-specific embedding paths.

### Required principle

```text
MemoryEvidence
      ↓
embedding
      ↓
meaning index
```

The embedding runtime remains replaceable.

The current MediaPipe/USE implementation should remain in place unless the canonical migration explicitly requires a different model/runtime.

### Do not

- introduce Gemini Nano;
- introduce a local LLM;
- change model families merely because newer Android AI exists;
- create a second vector/index substrate.

### Android-first rule

Use the existing local embedding capability/runtime through the established capability contract.

### Verification

Run MIG-05 acceptance tests, including:

- deterministic evidence identity;
- idempotent indexing;
- stale/revision behavior;
- local-only execution;
- retrieval regression.

Then stop.

---

# 15. MIG-06 — Unified Memory-Evidence search

This migration creates the canonical unified search use case.

### Important implementation decision

This is the appropriate stage to evaluate whether SQLite FTS/FTS5 or another existing local database search facility is beneficial for literal evidence search.

**Do not treat this as settled.** Per the Phase A review, `pdf_extraction_pages` currently has a composite primary key with no natural `rowid`, and the existing literal-search query is a three-way join enforcing current-fingerprint filtering (ADR-022) — this does not map cleanly onto Room's FTS support without restructuring, independent of whether SQLCipher's build even has FTS4/FTS5 compiled in. Resolve both questions (SQLCipher module availability; whether the join-heavy, composite-key shape can support an FTS entity without a larger restructure) as a short spike **before** committing to FTS as this migration's literal-search mechanism. If either answer is unfavorable, implement literal search without FTS and record the finding — do not force the FTS approach to preserve a previous recommendation.

Do not create a separate FTS project before MIG-06.

### Required architecture

Search should converge on the canonical Memory/Evidence substrate rather than retaining parallel asset-specific search paths.

### Search categories

Preserve the canonical distinction between:

- literal search;
- semantic search;
- source/type constraints;
- evidence-backed results.

### Android-first rule

Use SQLite/Room capabilities where they satisfy the requirement.

Do not introduce another search engine without a concrete requirement.

### Verification

Run MIG-06 acceptance tests and existing search regressions.

Then stop.

---

# 16. MIG-07 — Recall cutover

This is the migration from legacy/per-asset search paths to the canonical recall path.

### Critical rule

Do not maintain two competing production recall systems after the migration's cutover point.

The canonical recall pipeline must become the sole production path as specified by MIG-07.

### Required verification

Verify:

- search behavior;
- ranking;
- evidence;
- provenance;
- result explanation;
- source access;
- failure/recovery behavior.

Only retire legacy search implementations after acceptance criteria are satisfied.

Then stop.

---

# 17. MIG-07B — Anchor-aware structured recall

Follow the canonical migration specification.

This stage introduces the structured-filter/anchor-aware recall layer, using the refined, frozen anchor-combination semantics in `LOCAL_AI_TECHNICAL_SPEC.md` §9: structured signals are advisory (influence ranking) by default; a constraint becomes mandatory/excluding only when the query's constraint interpretation resolves it to explicit; absence of an anchor is always neutral, never exclusionary; new anchor coverage may only ever add ranking signal to previously unaffected queries, never retroactively convert an existing query from advisory to exclusionary.

At this stage, only `TIME` and `TOPIC` anchors exist (per MIG-03) — `PERSON`, `PLACE`, and other anchor kinds are out of scope until a future migration populates them.

### Critical rule

Do not turn this into a general-purpose LLM agent.

Structured recall should remain deterministic and evidence-backed wherever the canonical specification requires it.

A local generative model is not a prerequisite.

### Verification

Run:

- structured recall tests, including the explicit-vs-advisory distinction and the absence-is-neutral rule;
- anchor tests;
- regression tests;
- evidence/provenance tests;
- local-only tests.

Then stop.

---

# 18. Migration-by-migration execution protocol

For **every** migration:

### Step A — Read

Read the complete migration section in `ARCHITECTURAL_MIGRATION_SPEC_V1.md`.

### Step B — Inspect

Inspect actual current code and tests. This step exists specifically because plans and summaries — including this document's §10–§17 — describe intent, not guaranteed current state (§2A). Do not skip this step because a summary above seems sufficient.

### Step C — Plan

Produce a short repository-grounded implementation plan.

### Step D — Implement

Implement only that migration.

### Step E — Test

Run the migration's acceptance tests.

### Step F — Regression

Run relevant existing tests.

### Step G — Review

Inspect:

- git diff;
- dependencies;
- package boundaries;
- data flow;
- privacy/network behavior.

### Step H — Report

Report:

- files changed;
- tests run;
- test results;
- behavioral changes;
- migration acceptance status;
- remaining limitations.

### Step I — Stop

Do not begin the next migration until the current one is verified.

---

# 19. Privacy/network rule

The migration must preserve the local-first privacy contract.

Network access may exist only for explicitly permitted functions such as:

- approved provider/source access;
- approved model artifact downloads.

It must not be used for:

- source-content AI inference;
- embeddings;
- Memories;
- Evidence;
- user queries;
- prompts;
- recall;
- explanations.

If a new network request appears during implementation, STOP and report it.

---

# 20. Performance rule

Do not prematurely optimize based on assumptions.

Measure first.

For relevant migration stages record where appropriate:

- indexing time;
- retrieval latency;
- memory/storage growth;
- model load time;
- battery impact;
- thermal behavior;
- failure/retry behavior.

Thermal-management implementation is deferred until measurement demonstrates a need.

---

# 21. AI/model rule

Memora is **not** being redesigned around an LLM.

The current objective is to build:

> an on-device memory/retrieval infrastructure that uses local intelligence where useful.

The current frozen path does not require a generative LLM.

If a migration appears to require an LLM, stop and identify the concrete requirement before adding one.

Do not introduce a model/runtime because it is newer, faster, or fashionable.

---

# 22. AI Pack / model delivery rule

The current AI Pack abstraction remains the authority.

Do not replace it with Google Play for On-device AI during this migration.

A future feasibility spike (see P1.7) may evaluate Play-delivered AI packs, but that is separate work, gated on the two questions stated there.

The current migration should make model delivery:

- local;
- versioned;
- verified;
- capability-driven;
- replaceable.

---

# 23. Documentation protocol

Do not rewrite canonical architecture documents simply because implementation changes.

Update only documents explicitly required by the migration's change-control rules.

Maintain traceability between:

```text
Product requirement
      ↓
Canonical architecture contract
      ↓
Migration
      ↓
Implementation
      ↓
Test
```

Do not create competing "architecture" or "roadmap" documents.

This execution plan is only an execution wrapper.

---

# 24. Git/checkpoint protocol

Use small reviewable checkpoints.

Preferred:

```text
Phase 1 verified → checkpoint
MIG-01 verified  → checkpoint
MIG-02 verified  → checkpoint
MIG-03 verified  → checkpoint
MIG-04 verified  → checkpoint
MIG-05 verified  → checkpoint
MIG-06 verified  → checkpoint
MIG-07 verified  → checkpoint
MIG-07B verified → checkpoint
```

Do not combine multiple migrations into one large unreviewed change.

Do not commit or push unless explicitly instructed.

---

# 25. Failure/stop protocol

STOP immediately if:

- a canonical contract conflicts with the implementation;
- a migration requires changing the architecture freeze;
- user data would leave the device unexpectedly;
- source ownership becomes ambiguous;
- evidence provenance becomes unverifiable;
- a migration would create a second production pipeline;
- existing data could be lost;
- a migration cannot be rolled back safely;
- tests contradict the canonical acceptance criteria;
- this document's description of a migration or file conflicts with what Step B (Inspect) actually finds in the repository (§2A) — report the discrepancy rather than silently trusting either source.

Report the exact problem before proceeding.

Do not solve architectural uncertainty by improvisation.

---

# 26. MVP migration completion gate

MIG-01 through MIG-07B are complete only when:

- the canonical Memory/Evidence substrate is authoritative;
- MemoryBuilder is converged;
- generic evidence-level semantic indexing is operational;
- unified Memory-Evidence search is operational;
- canonical recall is the production recall path;
- anchor-aware structured recall is operational where required;
- legacy parallel paths specified for retirement have actually been retired;
- evidence/provenance is preserved;
- local-only core execution remains intact;
- regression tests pass;
- Android platform capabilities continue to be used through appropriate adapters;
- no new cloud AI dependency has been introduced.

At that point, assess the remaining roadmap items:

- MIG-08;
- MIG-09;
- MIG-10.

Do not automatically implement later migrations without checking their prerequisites and current MVP need.

---

# 27. Cursor master instruction

Use this document as the execution wrapper around the canonical Memora migration specification.

**First action: Phase 0 only.**

Do not write code until Phase 0 is complete and the current repository state has been reported.

After Phase 0:

1. complete Phase 1 only where the verified current code requires it;
2. verify Phase 1;
3. implement MIG-01;
4. verify MIG-01;
5. implement MIG-02;
6. verify MIG-02;
7. continue sequentially through MIG-07B.

For every step:

- inspect current code first (§2A) — do not rely on this document's summaries as a substitute;
- use the canonical documents as authority;
- make the smallest change that satisfies the requirement;
- prefer Android/platform capabilities where they satisfy the contract;
- preserve capability abstractions;
- add/maintain tests;
- run verification;
- inspect the diff;
- report the result;
- stop before the next step.

**Never redesign the architecture to solve an implementation problem.**

If you believe an architecture change is necessary, stop and provide evidence that the existing frozen architecture cannot satisfy the requirement.

Do not commit or push without explicit instruction.

---

# 28. Final position

Memora is now moving from:

```text
architecture/design
```

to:

```text
controlled implementation and migration
```

The architecture is frozen.

The Android platform audit has informed implementation choices but has **not** replaced the migration roadmap.

The migration roadmap remains the path to the target architecture.

The correct strategy is:

```text
Use Android capabilities
        +
Preserve Memora capability contracts
        +
Migrate toward the canonical Memory/Evidence architecture
        +
Verify every migration independently against the actual repository, not against a prior description of it
        +
Avoid unnecessary AI/runtime dependencies
        +
Keep the core local
```

**Do not create another architecture. Build the architecture that has already been specified.**
