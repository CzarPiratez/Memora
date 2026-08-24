# ARCHITECTURAL_MIGRATION_SPEC_V1.md

**Status:** Canonical implementation specification, operating under Architecture Freeze v1.0
**Audience:** Engineers and coding agents (including Cursor) implementing against the frozen architecture
**Authority:** This document does not introduce or redesign architecture. Per `ARCHITECTURE_FREEZE_v1.0.md` §2, it holds sequencing and implementation-planning authority only, subordinate to `PRODUCT_CONTRACT.md`, `LOCAL_AI_TECHNICAL_SPEC.md`, and `EXPERIENCE_MEMORY_AMENDMENT_V1.md`. Where it appears to imply a rule not stated in those three documents, they govern.
**Scope boundary:** This spec governs the Asset Memory layer only — discovery, extraction, assembly, storage, and recall of Asset Memories. It does not specify Link, Event Memory, Knowledge Memory, or personalization. Those remain governed exclusively by `EXPERIENCE_MEMORY_AMENDMENT_V1.md`, ADR-018, and ADR-019, and are explicitly out of scope for every migration in this document.

---

## 0. Frozen foundations — never change

These elements of the current implementation already satisfy the accepted architecture. No migration in this document modifies them. Any future proposal to modify them requires a new architectural review, not an engineering ticket.

| Element | Location | Why it is frozen |
|---|---|---|
| Stable `MemoryId` / immutable `MemoryRevisionId` identity model | `domain/memory/Memory.kt` | This is the addressable node every future Link, Event Memory, and Knowledge Memory reference will attach to. It already matches `LOCAL_AI_TECHNICAL_SPEC.md` §2 and §8 exactly. |
| Evidence-required-by-construction invariants (`Memory`, `MemorySummary`, `MemoryAnchor` `init` blocks) | `domain/memory/Memory.kt` | This is the compiled form of "truth before intelligence." No summary or anchor can exist without cited evidence. Loosening this would break the binding principle in `LOCAL_AI_TECHNICAL_SPEC.md` §1. |
| `MemoryIntegrityState` lifecycle enum and its transition discipline | `domain/memory/Memory.kt` | Matches `PRODUCT_CONTRACT.md` and `LOCAL_AI_TECHNICAL_SPEC.md` §8 verbatim. |
| `AssetIdentity` / `AssetFingerprint` as source-neutral, stable dedup keys | `domain/asset/Asset.kt` | Already source-agnostic; every migration below extends what is built on top of it without touching it. |
| Domain-layer purity (`Memory.kt`, `Asset.kt`, `IndexingState.kt` have no Android imports) | `domain/**` | Preserves portability and testability independent of shell. No migration below introduces a platform import into `domain/`. |
| Encrypted single-file local database as the default persistence boundary | `data/security/MemoraEncryptedDatabaseOpener.kt`, `data/security/DatabaseEncryptionConversionJournal.kt` | This is product vision (`PRODUCT_CONTRACT.md` privacy contract), not an engineering decision open for revision inside this spec. |
| Isolated-process PDF parsing pattern (AIDL sandbox boundary) | `data/pdfbox/isolation/**` | Correct trust-boundary architecture. Migration 10 extends its *scope*; nothing below removes or weakens the isolation boundary itself. |
| `AiPackManager` / pack manifest verification model | `domain/intelligence/AiPackContracts.kt`, `data/local/AiPackInstallLedger*.kt` | Already matches `LOCAL_AI_TECHNICAL_SPEC.md` §6 in shape. Out of scope for this spec; no migration touches it. |

---

## 1. Migration ordering summary

| # | Migration ID | Title | Classification |
|---|---|---|---|
| 1 | MIG-01 | Introduce Evidence Class Taxonomy | Must complete before MVP |
| 2 | MIG-02 | Remove Artificial Evidence Item/Length Caps in Memory Assembly | Must complete before MVP |
| 3 | MIG-03 | Populate TIME and TOPIC Memory Anchors from Existing Deterministic Facts | Must complete before MVP |
| 4 | MIG-04 | Formalize and Freeze the MemoryBuilder Contract Boundary | Must complete before MVP |
| 5 | MIG-05 | Generalize the Embedding Store to Evidence-Level Granularity | Must complete before MVP |
| 6 | MIG-06 | Introduce a Unified Memory-Evidence Search Use Case (Additive) | Must complete before MVP |
| 7 | MIG-07 | Cut Over to Unified Search and Retire Parallel Keyword-Search Implementations | Must complete before MVP |
| 7B | MIG-07B | Implement Anchor-Aware Structured Recall | Must complete before MVP |
| 8 | MIG-08 | Collapse Per-Asset-Type Pending-Extraction Repository Methods into One Generic Query | Recommended before beta |
| 9 | MIG-09 | Add Nullable Device-Identity / Sync-Readiness Columns to the Memory Schema | Recommended before beta |
| 10 | MIG-10 | Generalize the Isolated Untrusted-Content Parsing Boundary | Recommended before beta |
| 11 | MIG-11 | Introduce an ANN-Ready Embedding Store Interface | Future architecture |

No migration in this table requires a later migration to complete first. Each is independently reviewable and mergeable, and the application remains fully functional — with no regression in existing search, indexing, or Explain Mode behavior — after every single merge.

Link, Event Memory, Knowledge Memory, and personalization are **future architecture**, staged behind their own behavioral-contract and quality-gate work per `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §8. They are not assigned migration IDs in this document.

---

## MIG-01 — Introduce Evidence Class Taxonomy

**Classification:** Must complete before MVP

**Objective:** Add the epistemic evidence-class taxonomy (`Direct`, `Validated observation`, `Retrieval signal`, `Hypothesis`) required by `LOCAL_AI_TECHNICAL_SPEC.md` §11 and `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §5, as a first-class field on stored evidence.

**Architectural Principle:** Truth before intelligence. Evidence must declare not only *what kind of source data* it is, but *what epistemic weight it is permitted to carry* — a distinction the current model does not make.

**Problem Being Solved:** `MemoryEvidenceKind` (`SOURCE_METADATA`, `OCR_TEXT`, `DOCUMENT_TEXT`, `NOTE_TEXT`, `VISUAL_OBSERVATION`) describes provenance format, not epistemic status. The accepted architecture requires a second, orthogonal classification — Direct / Validated observation / Retrieval signal / Hypothesis — that governs whether a piece of evidence can independently justify a durable claim. This distinction does not exist anywhere in the domain model today. Every future consumer of evidence class (Link proposal logic, confidence display, future `VisionEngine` output) has nothing to read.

**Current Implementation:** `domain/memory/Memory.kt` defines `MemoryEvidenceKind` only. `MemoryEvidence` has no field expressing whether an item is a bounded source-derived fact, a validated model observation, a retrieval signal, or a hypothesis. All current evidence is deterministic extraction, so this gap has produced no visible defect yet — but nothing in the type system would prevent a future `VisionEngine` observation from being silently treated with the same epistemic weight as a directly extracted EXIF field.

**Target Implementation:** Add a `MemoryEvidenceClass` enum (`DIRECT`, `VALIDATED_OBSERVATION`, `RETRIEVAL_SIGNAL`, `HYPOTHESIS`) to `domain/memory/Memory.kt` and a corresponding field on `MemoryEvidence`. All evidence produced by the current deterministic extraction path (`AssembleAssetMemoryFromExtractionFacts`) is classified `DIRECT`. The Room `MemoryEvidenceEntity` gains a matching non-null column with `DIRECT` as the backfill default for existing rows. No other behavior changes.

**Why this change is required:** `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §8 explicitly sequences "specify pure behavioral contracts for... confidence/uncertainty" *before* any model or storage work for correlation/linking begins. Evidence class is exactly that contract. Adding it now, while all evidence is trivially `DIRECT`, is a zero-risk additive change. Adding it later — after `VisionEngine` output or retrieval-signal-based candidates already exist untagged — requires a retroactive audit of every evidence row's epistemic status, which is materially more expensive and error-prone.

**Files / modules likely affected:**
- `domain/memory/Memory.kt` (new enum, new field)
- `data/local/MemoryEntities.kt` (new column)
- `data/local/MemoryRoomMapper.kt` (mapping)
- `application/memory/AssembleAssetMemoryFromExtractionFacts.kt` (tag all produced evidence `DIRECT`)
- Any test fixture that constructs `MemoryEvidence` directly (compilation-level updates only)

**Database migration required?** Yes. Additive, non-null column with a static default (`DIRECT`) applied to all existing rows. No data loss, no reinterpretation of existing rows required.

**Backward compatibility considerations:** Fully additive. No existing query, use case, or UI surface reads or depends on this field yet, so there is no behavior change to verify beyond successful migration and compilation.

**Acceptance criteria:**
- `MemoryEvidence` cannot be constructed without an explicit `MemoryEvidenceClass`.
- Existing Room schema migration test (`MemoraDatabaseMigrationTest`) passes with the new column present and defaulted correctly on upgrade.
- All evidence currently produced by `AssembleAssetMemoryFromExtractionFacts` is tagged `DIRECT`.
- No existing test's assertions on `Memory`/`MemoryEvidence` shape need behavioral changes beyond compilation.

**Verification strategy:** Unit tests on `AssembleAssetMemoryFromExtractionFacts` asserting evidence class; Room migration test confirming default backfill; full existing test suite run to confirm zero behavioral regression.

**Regression risks:** Minimal. The only risk is an incomplete backfill on the Room migration path, caught directly by the migration test.

**Dependencies:** None. This is the first migration in the sequence.

**Estimated implementation complexity:** Small.

**Out of scope:** Using evidence class to gate any behavior (ranking, display, link eligibility). This migration only introduces the field.

**Future follow-up:** A future `VisionEngine` implementation must tag its output `VALIDATED_OBSERVATION`. A future Context/Correlation Engine must tag embedding-derived candidates `RETRIEVAL_SIGNAL` and unconfirmed relationships `HYPOTHESIS`, per `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §5. That work is governed by the amendment, not this spec.

---

## MIG-02 — Remove Artificial Evidence Item/Length Caps in Memory Assembly

**Classification:** Must complete before MVP

**Objective:** Remove the fixed 8-item / 500-character evidence caps in `AssembleAssetMemoryFromExtractionFacts` so that a Memory's stored evidence can represent the full extracted content of its Asset, not an arbitrarily truncated preview.

**Architectural Principle:** Retrieval-first, evidence-first. Everything Memora can recall must be backed by evidence that is actually stored on the Memory it recalls. Search must never need to bypass the Memory to find something the Memory could have represented.

**Problem Being Solved:** `AssembleAssetMemoryFromExtractionFacts` currently truncates to `MAX_EVIDENCE_ITEMS = 8` and `MAX_EVIDENCE_CHARS = 500` per item. This cap is the direct cause of every other search-fragmentation problem addressed later in this spec: because a Memory cannot hold enough evidence to represent a multi-page PDF or a long OCR block, downstream code was forced to re-read raw extraction tables directly at query time instead of trusting the Memory's own evidence. Fixing the cap here removes the reason those bypasses were ever written.

**Current Implementation:** `application/memory/AssembleAssetMemoryFromExtractionFacts.kt` selects up to 8 extraction facts, truncates each to 500 characters, and discards the rest. A 40-page PDF's memory holds at most 8 short excerpts regardless of how much extracted text actually exists in `PdfExtractionDao`.

**Target Implementation:** Evidence storage is no longer capped by a fixed item count. Every deterministic extraction fact produced for an Asset becomes one `MemoryEvidence` row (one PDF page, one OCR block, one EXIF field, one note section), each still individually bounded to a sane per-item character limit to prevent a single pathological fact from dominating storage. The 240-character `MAX_SUMMARY_CHARS` truncation is retained, but is repositioned as a *display* concern only — it governs what is shown as the UI-facing summary, not what is persisted as searchable evidence.

**Why this change is required:** This is the root-cause fix for the "collection of specialized search engines" problem identified in prior architectural review. Every parallel search path that bypasses `Memory` (see MIG-06/MIG-07) exists because the evidence model was too lossy to search against directly. There is no way to converge search onto one substrate without first making that substrate complete enough to search.

**Files / modules likely affected:**
- `application/memory/AssembleAssetMemoryFromExtractionFacts.kt` (remove item cap; retain and clarify per-item character bound; separate summary truncation from evidence truncation)
- `application/memory/RunPendingAssetMemoryAssembly.kt` (no logic change expected; verify batching/timeout assumptions still hold with larger evidence sets)
- `data/local/MemoryDao.kt`, `data/local/MemoryEntities.kt` (verify no implicit row-count assumptions)
- Test fixtures in `application/memory/AssembleAssetMemoryFromExtractionFactsTest.kt`

**Database migration required?** No. This changes assembly policy, not schema. Existing `memory_evidence` rows are unaffected; only newly assembled or reassembled Memories store more evidence.

**Backward compatibility considerations:** Existing Memories with capped evidence remain valid and readable; they are not retroactively reassembled by this migration. A full reindex is not required for this change to be safe — evidence completeness improves going forward, matching the existing "new revision on next legitimate assembly trigger" model already defined by `MemoryAssemblySchemaVersion`. Bump `MemoryAssemblySchemaVersion` so that a stale-reindex path (already part of the accepted lifecycle) picks up richer evidence on next legitimate reprocessing, without requiring a forced mass reindex as part of this migration.

**Acceptance criteria:**
- A PDF with more than 8 extracted pages produces a Memory whose evidence includes more than 8 items when all are within the per-item character bound.
- The UI-facing summary remains ≤ 240 characters regardless of total evidence volume.
- No existing assembly test that asserted the old 8-item cap as *correct behavior* still asserts it as correct; those tests are updated to assert completeness instead.
- `MemoryAssemblySchemaVersion` is incremented to mark this as a new assembly policy.

**Verification strategy:** Unit tests against `AssembleAssetMemoryFromExtractionFactsTest` with fixture assets producing more than 8 facts; confirm all facts (within per-item bound) are represented as evidence; confirm summary length invariant is unaffected; run `RunPendingAssetMemoryAssembly`'s existing batching tests to confirm no timeout/throughput regression from larger per-Memory evidence sets.

**Regression risks:** Increased evidence volume per Memory increases Room row counts and per-Memory assembly/write time. Verify against `PdfExtractionWriteBudgets` and existing write-path benchmark tests (`PdfExtractionWritePathBenchmarkIntegrationTest`) to confirm no violation of established write budgets.

**Dependencies:** MIG-01 (evidence class tagging should exist so newly stored evidence is classified correctly at the point this migration changes what gets stored). Not a hard compilation dependency, but must land first to avoid re-touching the same function twice for unrelated reasons.

**Estimated implementation complexity:** Medium. Logic change is small; verification against write-budget and performance tests is the larger effort.

**Out of scope:** Retroactively reassembling existing Memories. Changing how evidence is searched (MIG-06/MIG-07). Changing embedding granularity (MIG-05).

**Future follow-up:** MIG-03 (anchor population) and MIG-05 (evidence-level embeddings) both depend on evidence completeness established here.

---

## MIG-03 — Populate TIME and TOPIC Memory Anchors from Existing Deterministic Facts

**Classification:** Must complete before MVP

**Objective:** Wire already-extracted deterministic facts (EXIF capture timestamp, PDF/note title) into `TIME` and `TOPIC` `MemoryAnchor` instances, instead of collapsing all recall signal into a single generic `TEXT` anchor.

**Architectural Principle:** The recall contract in `PRODUCT_CONTRACT.md` promises retrieval by "person, place, object, time, purpose, and topic." A future evidence-backed Link (`EXPERIENCE_MEMORY_AMENDMENT_V1.md` §2.2) explicitly requires "consistent permitted time, place, person, text" to propose a relationship. Neither promise can be honestly kept while the anchor taxonomy is unused.

**Problem Being Solved:** `MemoryAnchorKind` declares eight values (`PERSON`, `PLACE`, `OBJECT`, `TIME`, `ACTIVITY`, `PURPOSE`, `TOPIC`, `TEXT`). Only `TEXT` is ever instantiated by `AssembleAssetMemoryFromExtractionFacts`. This is not an AI capability gap — EXIF capture date and PDF/note titles are already extracted deterministically today and are simply flattened into `SOURCE_METADATA` evidence text rather than mapped to a typed anchor.

**Current Implementation:** `AssembleAssetMemoryFromExtractionFacts` produces exactly one `MemoryAnchor` of kind `TEXT`, derived from the primary evidence excerpt. EXIF date and title facts exist in the extraction fact list but are never distinguished from any other `SOURCE_METADATA` fact when anchors are built.

**Target Implementation:** When a `TIME`-eligible fact (EXIF capture timestamp) or `TOPIC`-eligible fact (PDF/note title) is present in the extraction fact list for an Asset, the assembler produces a corresponding typed `MemoryAnchor` citing that fact's evidence, in addition to the existing `TEXT` anchor. `PERSON`, `PLACE`, `OBJECT`, `ACTIVITY`, and `PURPOSE` anchors remain unpopulated — they require model-based observation (`VisionEngine`, future `MemoryBuilder` extensions) that does not exist yet, and this migration does not fabricate them.

**Why this change is required:** This is the cheapest possible increment toward the anchor taxonomy actually meaning what it claims to mean, using facts already on disk. Deferring it costs nothing today but costs materially more once Link-proposal logic (governed separately by the amendment) begins depending on anchors existing to correlate against — retrofitting anchor population after correlation logic is built means re-deriving historical Memories' anchors under time pressure instead of by design.

**Files / modules likely affected:**
- `application/memory/AssembleAssetMemoryFromExtractionFacts.kt` (anchor-building logic)
- Extraction fact shape consumed from `domain/memory/AssetMemoryFactSource.kt` / `data/local/RoomAssetMemoryFactSource.kt` (confirm EXIF timestamp and title facts are distinguishable by locator/kind, not just present in the generic fact list)
- Test fixtures in `AssembleAssetMemoryFromExtractionFactsTest.kt`

**Database migration required?** No. Anchors are already a modeled, persisted concept (`memory_anchor` table via `MemoryEntities.kt`); this migration only changes which anchor kinds get populated.

**Backward compatibility considerations:** Existing Memories retain their single `TEXT` anchor until next legitimate reassembly (same non-forced-reindex model as MIG-02). No consumer currently filters or ranks by anchor kind, so there is no behavior for existing anchors to break.

**Acceptance criteria:**
- An Asset with an EXIF capture timestamp produces a `TIME` anchor citing that evidence.
- A PDF or note Asset with an extracted title produces a `TOPIC` anchor citing that evidence.
- Assets without these facts continue to produce only the existing `TEXT` anchor; no anchor is fabricated in the absence of evidence.
- `MemorySignature`'s existing invariant (every anchor must cite evidence present in the same Memory) continues to hold without modification.

**Verification strategy:** Unit tests with fixture facts containing EXIF timestamps and titles, asserting the correct typed anchors are produced with correct evidence citation; fixture facts without these fields, asserting no anchor is fabricated.

**Regression risks:** Low. This is additive anchor production; no existing anchor consumer exists yet to regress.

**Dependencies:** MIG-01 (anchor evidence should be classified `DIRECT`), MIG-02 (uncapped evidence pool means the underlying EXIF/title facts are reliably present as evidence to cite, not competing against the old 8-item cap).

**Estimated implementation complexity:** Small.

**Out of scope:** `PERSON`, `PLACE`, `OBJECT`, `ACTIVITY`, `PURPOSE` anchors. Any ranking or filtering behavior based on anchor kind. Any correlation/linking logic.

**Future follow-up:** `PLACE` anchors become derivable once GPS EXIF is included in the extraction contract (already permitted by `PRODUCT_CONTRACT.md`'s "EXIF/GPS when available" language, not currently wired to an anchor) — flagged for a future migration, not included here to keep this migration's diff minimal and reviewable.

---

## MIG-04 — Formalize and Freeze the MemoryBuilder Contract Boundary

**Classification:** Must complete before MVP

**Objective:** Extract a `MemoryBuilder` interface matching the contract defined in `LOCAL_AI_TECHNICAL_SPEC.md` §4, and make the existing deterministic assembler its first concrete implementation — rather than leaving deterministic assembly and the declared `MemoryBuilder` capability interface as two disconnected things.

**Architectural Principle:** The Local Intelligence Layer contracts are the only sanctioned integration seam between deterministic logic and future on-device model output. A capability must never be bolted on beside the seam it was supposed to go through.

**Problem Being Solved:** `domain/intelligence/LocalIntelligenceEngines.kt` already declares a `MemoryBuilder` interface, but it has no operate method, and `AssembleAssetMemoryFromExtractionFacts` — the code that actually performs memory assembly today — does not implement or depend on it. This is not yet a defect, but it is an unresolved design question with a wrong-by-default outcome: if a future `VisionEngine`-driven assembly path is written before this seam is decided, the most likely result is a second, parallel assembly pipeline living beside `AssembleAssetMemoryFromExtractionFacts`, reproducing the exact "two systems doing the same job differently" failure mode this spec's search-related migrations (MIG-06/MIG-07) exist to eliminate on the retrieval side.

**Current Implementation:** `MemoryBuilder` is declared with capability-availability plumbing but no `assemble`/`build` method. `AssembleAssetMemoryFromExtractionFacts` is a standalone function with no relationship to that interface.

**Target Implementation:** `LocalIntelligenceEngines.kt`'s `MemoryBuilder` interface gains a method whose signature accepts deterministic extraction facts and optional validated local observations (matching `LOCAL_AI_TECHNICAL_SPEC.md` §4's description: "combines deterministic extraction and local observations into a schema-validated Memory; it may not invent unsupported source facts") and returns a schema-validated `Memory`. `AssembleAssetMemoryFromExtractionFacts`'s existing logic becomes the deterministic-only implementation of this interface — it is renamed or wrapped so that it *is* the `MemoryBuilder`, with the "local observations" input parameter accepted but always empty until a future `VisionEngine` exists to populate it. No assembly behavior changes; this migration only formalizes the existing function's relationship to the declared contract.

**Why this change is required:** `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §8 requires behavioral contracts to be specified "before storage or model work" begins on correlation/linking, and `LOCAL_AI_TECHNICAL_SPEC.md` §13 requires the exact capability contract to be documented and approved before any AI/model dependency is added. Doing this now, while `MemoryBuilder` has exactly one deterministic implementation and zero behavior to preserve across a risky refactor, is the cheapest point in the project's lifetime to do it. Doing it after a `VisionEngine` implementation exists means refactoring two live code paths simultaneously instead of one.

**Files / modules likely affected:**
- `domain/intelligence/LocalIntelligenceEngines.kt` (interface method addition)
- `application/memory/AssembleAssetMemoryFromExtractionFacts.kt` (implements or is wrapped by the interface; no logic change)
- `application/memory/RunPendingAssetMemoryAssembly.kt` (call site update to go through the interface, if not already structured to allow this)
- `data/di/LocalIntelligenceModule.kt` (dependency injection wiring for the concrete implementation)

**Database migration required?** No.

**Backward compatibility considerations:** Purely a code-shape change. Assembly output must be byte-for-byte identical to pre-migration output for the same inputs; this is directly verifiable by running the full existing `AssembleAssetMemoryFromExtractionFactsTest` suite unmodified against the new call path.

**Acceptance criteria:**
- `MemoryBuilder` has a defined operate method matching `LOCAL_AI_TECHNICAL_SPEC.md` §4's description.
- The deterministic assembler is wired as (or into) the concrete implementation used by `RunPendingAssetMemoryAssembly`.
- All existing assembly tests pass unmodified against the new call path, confirming zero behavior change.
- The interface accepts an optional local-observations input that is unused (empty) in the current concrete implementation, without requiring any caller to supply one.

**Verification strategy:** Full regression run of existing assembly and assembly-worker test suites with no expected assertion changes; a targeted test confirming the interface can be resolved and invoked through dependency injection exactly as the previous direct function call was.

**Regression risks:** Low; primarily a compile-time refactor. The main risk is accidental behavior drift introduced while relocating logic behind the interface, mitigated by the requirement that all existing tests pass unmodified.

**Dependencies:** MIG-01, MIG-02, MIG-03 — this migration freezes the contract shape around the assembler's *current* behavior, so it should land after that behavior has reached its MVP-target shape (uncapped evidence, evidence class, typed anchors), not before, to avoid freezing an interface around a signature that immediately needs revisiting.

**Estimated implementation complexity:** Small to medium.

**Out of scope:** Implementing `VisionEngine` or any model-backed local observation source. This migration only prepares the seam; it does not populate it.

**Future follow-up:** When `VisionEngine` is implemented (governed by its own acceptance-gate process per `LOCAL_AI_TECHNICAL_SPEC.md` §13), its output becomes the "local observations" input to this same `MemoryBuilder` contract, tagged `VALIDATED_OBSERVATION` per MIG-01's taxonomy — not a new parallel assembly path.

---

## MIG-05 — Generalize the Embedding Store to Evidence-Level Granularity

**Classification:** Must complete before MVP

**Objective:** Replace the PDF-page-specific embedding key (`revisionId + pageNumber + model`) with a generic evidence-level key (`revisionId + evidenceId + model`), so every asset type can be embedded at evidence granularity, not only PDFs.

**Architectural Principle:** One evidence substrate, one embedding substrate. The unit of semantic search should be "a piece of evidence," regardless of whether that evidence came from a PDF page, an OCR block, or a note section.

**Problem Being Solved:** `PdfPageEmbeddingEntity` and `PdfPageEmbeddingStore` are PDF-specific by construction — the primary key literally includes `pageNumber`, an integer with no meaning for a photo's OCR text or a note's body. This is why per-evidence semantic search exists only for PDFs today, and why `SearchAssetMemoriesByMeaning` has to read raw PDF page text live from `SavedPdfPageTextSource` instead of from stored evidence — there was nowhere generic to store or key that embedding for other asset types.

**Current Implementation:** `data/local/PdfPageEmbeddingEntity.kt` keys embeddings by `(revisionId, pageNumber, model)`. `application/intelligence/SearchAssetMemoriesByMeaning.kt` embeds one Memory-level summary vector plus, for PDFs only, a separate set of page vectors fetched via `SavedPdfPageTextSource.listCurrentVerifiedPages`, bypassing the Memory's own stored evidence at query time.

**Target Implementation:** `PdfPageEmbeddingEntity`/`PdfPageEmbeddingStore` are renamed and regeneralized to `MemoryEvidenceEmbeddingEntity`/`MemoryEvidenceEmbeddingStore`, keyed by `(revisionId, evidenceId, model)`. `IndexMemoryEmbeddings` (or its successor use case) embeds every stored `MemoryEvidence` item for a revision, not just PDF pages. `SearchAssetMemoriesByMeaning` reads exclusively from this store and from `MemoryEvidence`, and no longer depends on `SavedPdfPageTextSource` at query time.

**Why this change is required:** This is the schema change that makes MIG-06/MIG-07 (search unification) possible. Without evidence-level embeddings for every asset type, "one search substrate" is not achievable — PDFs would remain a structurally different, better-supported search experience than photos, screenshots, and notes, which is itself an architectural inconsistency this spec exists to remove.

**Files / modules likely affected:**
- `data/local/PdfPageEmbeddingEntity.kt` → generalized entity
- `data/local/RoomPdfPageEmbeddingStore.kt` → generalized store implementation
- `domain/intelligence/EmbeddingContracts.kt` (interface generalization if it references page-specific concepts)
- `application/intelligence/IndexMemoryEmbeddings.kt` / `application/intelligence/IndexPdfPageEmbeddings.kt` (consolidate into one evidence-embedding indexing use case)
- `application/intelligence/SearchAssetMemoriesByMeaning.kt` (remove `SavedPdfPageTextSource` dependency)
- `application/intelligence/ResolveMeaningPdfOpenPage.kt` (verify citation/open-original behavior still resolves correctly against the renamed store)
- Room schema migration and associated test (`MemoraDatabaseMigrationTest`)

**Database migration required?** Yes. The primary key shape changes from `(revisionId, pageNumber, model)` to `(revisionId, evidenceId, model)`. Existing PDF page embeddings are either migrated by mapping stored page numbers to their corresponding evidence rows' locators (`pdf:page:N`), or invalidated and scheduled for reindex via the existing stale-reindex mechanism (`STALE_REINDEX_REQUIRED`) rather than a blocking data migration. The latter is strongly preferred: embeddings are a derived, versioned artifact per `LOCAL_AI_TECHNICAL_SPEC.md` §8, and re-deriving them through the existing recoverable indexing pipeline is safer than a bespoke one-time data transform.

**Backward compatibility considerations:** Existing PDF meaning search continues to function throughout the migration window because the stale-reindex path already exists and is the accepted mechanism for "a schema/version change requires reprocessing" per `LOCAL_AI_TECHNICAL_SPEC.md` §8. No user-facing search gap occurs; PDFs already awaiting embedding reindex simply search via existing summary-level vectors until reindexed, exactly as any other asset type does today.

**Acceptance criteria:**
- Embeddings can be stored and retrieved for evidence originating from any `MemoryEvidenceKind`, not only `DOCUMENT_TEXT`.
- `SearchAssetMemoriesByMeaning` no longer imports or calls `SavedPdfPageTextSource`.
- Existing PDF citation/open-original behavior (`ResolveMeaningPdfOpenPage`) continues to resolve to the correct page after the key generalization.
- Room migration test passes; embeddings requiring reprocessing are marked via the existing stale-reindex mechanism, not silently dropped or silently left stale without a recoverable path.

**Verification strategy:** Migration test suite covering upgrade from the prior schema version; integration test embedding a non-PDF evidence item (e.g., an OCR block) and confirming it is retrievable via meaning search; regression test on existing PDF page-citation-and-open flow.

**Regression risks:** This is the highest-risk migration in the "must complete before MVP" tier, because it touches a live, user-facing search path (PDF meaning search) and a Room primary key. Mitigate by keeping the stale-reindex fallback as the migration strategy (no blocking data transform) and by not removing `PdfPageEmbeddingEntity` until the new store is verified in place.

**Dependencies:** MIG-02 (evidence must be complete/uncapped for evidence-level embedding to be meaningful for non-PDF asset types). MIG-01 is a soft dependency (embeddings are themselves a `RETRIEVAL_SIGNAL`-classified concept conceptually, though this migration does not yet wire that classification into ranking logic — that remains future work per MIG-01's "Future follow-up").

**Estimated implementation complexity:** Large. This is the largest single migration in the "must complete before MVP" tier.

**Out of scope:** Any ranking algorithm change. Any change to how embedding vectors are computed by `EmbeddingEngine` implementations — only the storage key and indexing scope generalize.

**Future follow-up:** MIG-11 (ANN-ready interface) builds directly on this generalized store.

---

## MIG-06 — Introduce a Unified Memory-Evidence Search Use Case (Additive)

**Classification:** Must complete before MVP

**Objective:** Add one `SearchMemoryEvidence` application use case that queries `MemoryEvidence` (via Room full-text/substring query) as the single literal-text search implementation, without yet removing or rewiring any existing per-asset-type keyword search class.

**Architectural Principle:** One evidence substrate, one search substrate. This migration is deliberately additive-only so that the larger cutover (MIG-07) can be reviewed and merged as a separate, smaller, lower-risk change.

**Problem Being Solved:** Keyword search today exists as four to five near-duplicate implementations — one per asset type — each querying that asset type's own raw extraction table directly (`SearchPersistedPdfPageText`/`PdfKeywordSearchSupport` and its screenshot/photo/note equivalents), explicitly bypassing `Memory`/`MemoryEvidence` entirely. `SearchPersistedPdfPageText`'s own code comment states plainly that this is *not* semantic recall — it is a separate, parallel retrieval system with its own ranking and explanation semantics, which will diverge further from the Memory-based meaning-search path the longer both exist.

**Current Implementation:** `PdfKeywordSearchSupport`, and its counterparts for screenshots, photos, and notes, each independently query their own Dao's raw extraction table (`PdfExtractionDao`, `ScreenshotOcrExtractionDao`, `PhotoOcrExtractionDao`, `NotePageExtractionDao`) via `LIKE`-style matching, entirely independent of `MemoryDao`/`MemoryEvidence`.

**Target Implementation:** A new `SearchMemoryEvidence` use case queries `memory_evidence.excerpt` directly (via `MemoryDao` or a new evidence-search-specific Dao method), returning matches with their owning `MemoryId`, `MemoryRevisionId`, evidence locator, and excerpt — the same shape of information the per-type implementations currently expose, but sourced from the unified substrate. This use case is built, unit-tested, and available for injection, but **no existing ViewModel or screen is rewired to use it in this migration.** The old per-type search paths continue operating unchanged.

**Why this change is required:** Because MIG-02 removed the evidence caps that made searching `MemoryEvidence` directly unviable for PDFs, and MIG-05 gave every asset type evidence-level embeddings, the technical blocker that originally forced keyword search to bypass `Memory` is gone. This migration proves the replacement works, in isolation, before anything depending on it is switched over — satisfying "leave the application in a working state after every migration" without coupling a large UI cutover to a large data-model change in the same reviewable unit.

**Files / modules likely affected:**
- New: `application/documents/SearchMemoryEvidence.kt` (or a location-neutral package if this use case is intended to be shared across future asset-type UIs)
- `data/local/MemoryDao.kt` (new query method for excerpt search, if not already sufficient)
- New unit test file for the use case

**Database migration required?** No. This migration only adds a query against the schema MIG-01/MIG-02 already established.

**Backward compatibility considerations:** None — this migration adds new, unused-by-UI code. It cannot regress any existing behavior because nothing existing calls it yet.

**Acceptance criteria:**
- `SearchMemoryEvidence` returns correct matches for a literal substring query against evidence excerpts across at least one instance of every current `MemoryEvidenceKind` (metadata, OCR text, document text, note text).
- Returned results carry sufficient locator/provenance information to support the existing "open original" and "why this result" UI affordances once wired in MIG-07.
- No existing search screen, ViewModel, or keyword-search class is modified in this migration.

**Verification strategy:** Unit tests against fixture `MemoryEvidence` rows covering every evidence kind; no integration or UI test changes are expected since no UI is touched.

**Regression risks:** None to existing behavior, by construction. The risk in this migration is entirely forward-looking: an incomplete or incorrectly shaped result contract here becomes expensive to fix once MIG-07 wires four to five screens against it. Reviewers should treat the use case's public contract as the primary review focus.

**Dependencies:** MIG-01, MIG-02, MIG-05.

**Estimated implementation complexity:** Medium.

**Out of scope:** Wiring any UI to this use case. Removing any existing keyword search class. Semantic (embedding-based) search — this migration is literal/keyword search only, matching the scope of what it is replacing.

**Future follow-up:** MIG-07 performs the cutover.

---

## MIG-07 — Cut Over to Unified Search and Retire Parallel Keyword-Search Implementations

**Classification:** Must complete before MVP

**Objective:** Rewire every asset-type-specific search screen/ViewModel to use `SearchMemoryEvidence` (MIG-06) instead of its bespoke per-type keyword search class, then delete the retired classes and their now-unused raw-extraction-table query paths.

**Architectural Principle:** Convergence over duplication. A user asking "why did this match?" should get the same kind of answer regardless of which asset type the result came from.

**Problem Being Solved:** This migration eliminates the parallel-search-engine problem directly. Once complete, there is exactly one literal-text search implementation and one semantic search implementation (`SearchAssetMemoriesByMeaning`, already Memory/evidence-based after MIG-05), both reading from `MemoryEvidence`, instead of five keyword implementations plus one meaning-search implementation with an asset-type-specific escape hatch.

**Current Implementation:** `PdfKeywordSearchViewModel`, `ScreenshotOcrKeywordSearchViewModel`, `PhotoOcrKeywordSearchViewModel`, `NotePageKeywordSearchViewModel` each depend on their respective per-type keyword search support class, independently of `Memory`.

**Target Implementation:** Each of these ViewModels depends on `SearchMemoryEvidence`, filtered by asset type at the query layer where the UI still needs an asset-type-scoped view (e.g., a "search my PDFs" screen), or consolidated into fewer, asset-type-agnostic search surfaces where the product experience allows it. This migration's *required* scope is the dependency cutover; UI/screen consolidation itself is optional and may be deferred to MIG-08 or later without blocking this migration's completion. `PdfKeywordSearchSupport`, `SearchPersistedPdfPageText`, and their screenshot/photo/note equivalents are deleted once every caller is migrated and verified.

**Why this change is required:** This is the migration that actually resolves the central finding of the prior architectural review: that the system was, in practice, a collection of specialized search engines rather than one evidence-first retrieval system. Everything before this migration (MIG-01 through MIG-06) exists to make this cutover safe; this migration is where the product-level behavior actually converges.

**Files / modules likely affected:**
- `ui/search/PdfKeywordSearchViewModel.kt`, `ui/search/ScreenshotOcrKeywordSearchViewModel.kt`, `ui/search/PhotoOcrKeywordSearchViewModel.kt`, `ui/search/NotePageKeywordSearchViewModel.kt` (dependency swap)
- Deletion: `application/documents/PdfKeywordSearchSupport.kt`, `application/documents/SearchPersistedPdfPageText.kt`, `application/images/ScreenshotOcrKeywordSearchSupport.kt`, `application/images/PhotoOcrKeywordSearchSupport.kt`, `application/notes/NotePageKeywordSearchSupport.kt`, and their corresponding raw-extraction-table read paths where no longer referenced elsewhere
- Corresponding ViewModel and Copy tests (`PdfKeywordSearchViewModelTest`, `PdfKeywordSearchCopyTest`, and per-type equivalents) updated to reflect the new dependency, not the underlying search behavior
- `data/di/*Module.kt` files providing the retired classes

**Database migration required?** No.

**Backward compatibility considerations:** This migration should be executed and merged **per asset type**, not as one large PR — e.g., cut PDF keyword search over first, verify in isolation, merge; then screenshots; then photos; then notes. This keeps each individual change small, independently reviewable, and immediately revertible without affecting the other asset types' still-unconverted search paths. The instruction that "every migration must leave the application in a working state" is satisfied at the sub-migration (per-asset-type) granularity within this single Migration ID.

**Acceptance criteria:**
- Every existing search screen produces equivalent or better result coverage against the same fixture corpus used by its retired predecessor (no regression in recall for literal substring queries).
- "Why this result" / explanation surfaces continue to show correct provenance after cutover.
- "Open original" continues to resolve to the correct source location/page after cutover.
- Zero references to the retired per-type search support classes remain in the codebase; they are deleted, not merely unused.
- Raw extraction tables (`PdfExtractionDao`, etc.) remain in place as write targets for extraction, but are no longer read by any search path.

**Verification strategy:** Before/after comparison against existing fixture-based search tests (`PdfKeywordSearchSupportTest` and equivalents, adapted to assert against the new use case); full existing ViewModel test suites per asset type; manual verification of "why this result" and "open original" flows per asset type as part of PR review, given these are the two product-trust surfaces most sensitive to a silent regression.

**Regression risks:** This is the second-highest-risk migration in the sequence, because it changes live, frequently used, user-facing search behavior across four screens. The primary mitigation is the per-asset-type sequencing described above, which bounds the blast radius of any single sub-change and allows independent rollback.

**Dependencies:** MIG-06 (hard dependency — the use case being cut over to must exist and be verified first).

**Estimated implementation complexity:** Large, but decomposed into four small, independently mergeable sub-changes (one per asset type) as described above.

**Out of scope:** Consolidating the four search screens into fewer UI surfaces. Changing ranking or explanation copy beyond what is required to preserve existing behavior against the new data path.

**Future follow-up:** Once complete, `SearchAssetMemoriesByMeaning` and `SearchMemoryEvidence` are the only two retrieval implementations in the codebase, both evidence-sourced. UI/UX consolidation of the search surfaces themselves, if desired, becomes a product decision independent of this architecture spec.

---

## MIG-07B — Implement Anchor-Aware Structured Recall

**Classification:** Must complete before MVP

**Objective:** Implement the structured-filter stage of the canonical recall pipeline, using the refined anchor-combination semantics frozen in `LOCAL_AI_TECHNICAL_SPEC.md` §9, so that queries expressing a time or topic cue actually use the `TIME`/`TOPIC` anchors MIG-03 populates.

**Architectural Principle:** One canonical recall pipeline. Truth before intelligence — structured filtering must never exclude a candidate on the basis of missing evidence, and every filtering decision must remain deterministic and explainable.

**Problem Being Solved:** `PRODUCT_CONTRACT.md`'s retrieval contract states that natural-language queries use remembered cues including time and topic. MIG-03 populates `TIME` and `TOPIC` anchors from deterministic facts; MIG-06/MIG-07 unify literal and semantic candidate generation onto one evidence substrate. No migration connects the two: today, a query implying a time or topic constraint has no code path that reads the anchors MIG-03 exists to produce. Until this migration, the Product Contract's retrieval promise is not yet met even after every other migration in this document lands.

**Current Implementation:** `SearchMemoryEvidence` (MIG-06) performs literal matching over evidence excerpts; `SearchAssetMemoriesByMeaning` performs similarity ranking over evidence embeddings (MIG-05). Neither reads `MemoryAnchor`. There is no structured-filter or anchor-aware ranking stage anywhere in the codebase.

**Target Implementation:** A structured-filter-and-rank stage sits downstream of both candidate-generation mechanisms, exactly as diagrammed in `LOCAL_AI_TECHNICAL_SPEC.md` §9: `literal-match candidates + vector candidates -> structured filters -> evidence-based ranking -> Memory cards`. Per the frozen semantics, a `TIME`/`TOPIC` anchor cue influences ranking by default; it becomes a mandatory, excluding filter only when the query's constraint interpretation resolves it to explicit rather than advisory, as a discrete decision produced by the same bounded local query encoder or intent classifier already permitted by §9 for semantic retrieval. A candidate missing an anchor — whether because that evidence wasn't extracted or because the anchor kind isn't populated anywhere yet — is never excluded on that basis; it contributes no signal in either direction. This behavior must remain stable as anchor coverage expands: a future anchor kind may only ever add ranking signal to previously unaffected queries, never retroactively convert an existing query from advisory to exclusionary.

**Why this change is required:** This is not new architecture — every rule this migration implements is already frozen text in `LOCAL_AI_TECHNICAL_SPEC.md` §9. It is the one already-frozen, MVP-level (not staged-future) recall requirement with no migration implementing it. Deferring it further would leave the Product Contract's retrieval promise unmet by the roadmap's own account.

**Files / modules likely affected:**
- New: a structured-filter/ranking use case sitting downstream of `SearchMemoryEvidence` and `SearchAssetMemoriesByMeaning`, or an extension of `SearchAssetMemoriesByMeaning`'s existing ranking step
- `domain/memory/Memory.kt` (read-only consumer of `MemoryAnchor`; no domain model change)
- The deterministic, bounded query-constraint classifier referenced in `LOCAL_AI_TECHNICAL_SPEC.md` §9 (new, narrowly scoped — resolves a query's time/topic cue to explicit-or-advisory only; not a general NLU component)
- Corresponding ViewModel(s) consuming unified search results

**Database migration required?** No. This migration only adds a query/ranking stage over data already produced by MIG-03 and MIG-05.

**Backward compatibility considerations:** Every existing query continues to return results. Because unpopulated or absent anchors are always treated as neutral, no existing query can regress to zero results because of this migration — at worst, a query gains no additional filtering signal until anchor coverage exists for it.

**Acceptance criteria:**
- A query expressing an explicit, high-confidence time or topic constraint excludes candidates that do not satisfy it.
- A query expressing an ambiguous or low-confidence time/topic cue only re-ranks candidates; it never excludes on that basis.
- A candidate lacking a relevant anchor is never excluded, regardless of whether the anchor kind exists elsewhere in the corpus.
- Adding a new anchor kind to a future migration provably does not change the filtering behavior of any query that previously had no cue mapping to that kind (regression test, not just code review).

**Verification strategy:** Fixture-based tests covering: an explicit-constraint query correctly excluding non-matching candidates; an advisory-constraint query correctly re-ranking without excluding; a query against a corpus where the relevant anchor kind is entirely unpopulated, confirming identical behavior to a corpus where it's populated-but-absent-on-this-item. A regression test asserting that introducing a new, previously-unused anchor kind does not change results for queries that don't reference it.

**Regression risks:** The primary risk is the explicit/advisory classification silently drifting toward exclusion-by-default over time as it's tuned, eroding the "advisory unless explicit and confident" guarantee. Mitigate by keeping the classification's output a discrete, tested enum (explicit or advisory) rather than a continuous threshold callers interpret themselves.

**Dependencies:** MIG-03 (anchors must exist), MIG-06 and MIG-07 (both candidate-generation mechanisms must already feed one substrate before a shared filter stage can sit downstream of them).

**Estimated implementation complexity:** Medium.

**Out of scope:** `PERSON`, `PLACE`, `OBJECT`, `ACTIVITY`, `PURPOSE` anchor filtering — these anchor kinds are not yet populated by any migration (MIG-03 covers only `TIME`/`TOPIC`), so there is nothing for this migration to filter on for them. Any model-backed `RecallRanker` implementation. Any change to Explain Mode's presentation of a filtered or ranked result.

**Future follow-up:** When a future migration populates additional anchor kinds, it inherits this migration's filter-and-rank stage without modification, per the stability requirement stated above.

---

## MIG-08 — Collapse Per-Asset-Type Pending-Extraction Repository Methods into One Generic Query

**Classification:** Recommended before beta

**Objective:** Replace `AssetRepository`'s five near-identical pending-extraction lookup methods (`findNextPdfPendingLocalReading`, `findNextImagePendingExifExtract`, `findNextScreenshotPendingOcrExtract`, `findNextPhotoPendingOcrExtract`, `findNextNotePendingPageExtract`) with one generic `findNextPendingExtraction(sourceId, assetType, extractionKind, schemaVersion, afterSourceAssetKey)` method.

**Architectural Principle:** Open for extension, closed for modification. Adding a new source type should mean writing a new extractor, not editing a shared domain-layer interface.

**Problem Being Solved:** Every one of the five existing methods differs only in which extraction kind and asset type it filters for; the underlying query shape is identical. This is the domain-layer instance of the same duplication pattern this spec removes from the search layer in MIG-06/MIG-07 — except here it is a repository contract, not a search implementation, so the cost of leaving it unconverged is a growing interface (one new method per future source type) rather than a growing set of parallel search engines.

**Current Implementation:** `domain/asset/AssetRepository.kt` declares five type-specific pending-lookup methods, each implemented separately in `data/local/RoomAssetRepository.kt`.

**Target Implementation:** One generic method parameterized by asset type and extraction kind replaces all five. Each existing worker (`RunPendingPdfLocalReading`, `RunPendingImageExifExtract`, `RunPendingScreenshotOcrExtract`, `RunPendingPhotoOcrExtract`, `RunPendingOneNotePageExtract`) calls the generic method with its specific parameters instead of a dedicated method.

**Why this change is required:** This is real leverage, but it is not user-facing and does not block correctness of the accepted architecture's retrieval/evidence contract, which is why it is classified below the MVP line. It is recommended before beta because Notes/OneNote extraction is still actively being built out, and every day this interface remains unconverged increases the number of call sites that need updating when it eventually is.

**Files / modules likely affected:**
- `domain/asset/AssetRepository.kt` (interface collapse)
- `data/local/RoomAssetRepository.kt` (implementation collapse)
- `application/documents/RunPendingPdfLocalReading.kt`, `application/images/RunPendingImageExifExtract.kt`, `application/images/RunPendingScreenshotOcrExtract.kt`, `application/images/RunPendingPhotoOcrExtract.kt`, `application/notes/RunPendingOneNotePageExtract.kt` (call-site updates)
- Corresponding tests in `data/local/RoomAssetRepositoryTest.kt` and per-use-case test files

**Database migration required?** No. This is an interface/query-shape change, not a schema change.

**Backward compatibility considerations:** Purely internal refactor; no external or persisted contract changes. Existing indexing behavior must be identical before and after.

**Acceptance criteria:**
- All five existing pending-extraction workers function identically after switching to the generic method, verified by existing worker-level tests.
- `AssetRepository` exposes one pending-extraction lookup method instead of five.
- Adding a hypothetical sixth extraction kind requires no interface change, only a new call with new parameters (demonstrated in a test, not shipped as a real sixth kind).

**Verification strategy:** Full regression run of existing per-worker tests (`RunPendingPdfLocalReading` and equivalents) with no expected assertion changes; a new test exercising the generic method directly with parameters matching each of the five prior specific methods, confirming identical query results.

**Regression risks:** Low to medium. The risk is a subtle behavioral difference between the five original hand-written queries (e.g., differing `afterSourceAssetKey` exclusivity handling) being lost in generalization. Mitigate by writing the new generic method's test suite from the existing five methods' documented behavior before deleting them, not after.

**Dependencies:** None from MIG-01–MIG-07 strictly, though sequencing it after the evidence/search convergence work is recommended to avoid two large refactors touching overlapping extraction-worker code concurrently.

**Estimated implementation complexity:** Medium.

**Out of scope:** Generalizing `AssetType` itself into an open registry/plugin model. That is a materially larger change than this migration and is not required to remove the duplication addressed here.

**Future follow-up:** If a source type requiring a genuinely new extraction *category* (not just another note provider) is planned, revisit whether `AssetType` itself should become an open model at that time — not before, per "prefer convergence over rewrites."

---

## MIG-09 — Add Nullable Device-Identity / Sync-Readiness Columns to the Memory Schema

**Classification:** Recommended before beta

**Objective:** Add nullable, currently-unused device-identity columns to `Memory`/`MemoryEvidence` persistence as cheap insurance against the cost of retrofitting them after years of single-writer assumptions accumulate, without implementing any sync behavior.

**Architectural Principle:** Local-first by default is product vision and is not being revisited by this migration. This migration only ensures that *if* a future, separately-approved product decision enables backup/sync (explicitly contemplated as a future option in `PRODUCT_CONTRACT.md`'s privacy contract and `LOCAL_AI_TECHNICAL_SPEC.md` §10), the schema is not actively hostile to it.

**Problem Being Solved:** `MemoryInsertResult.RevisionConflict` currently rejects conflicting writes outright — correct and required for a single-writer, single-device system. Nothing in the schema today records *which* writer produced a revision, because there has only ever been one. Adding a nullable device-identity concept now, while the schema is still on an actively-evolving migration sequence, is materially cheaper than adding it after a multi-year single-writer assumption is baked into every downstream query.

**Current Implementation:** No device-identity concept exists anywhere in `data/local/MemoryEntities.kt` or `domain/memory/Memory.kt`.

**Target Implementation:** A nullable `originDeviceId` (or equivalently named) column is added to the relevant Memory persistence tables. It is populated with a stable per-installation identifier already available from existing device/installation infrastructure, but is not read, compared, or acted upon by any code path. No merge logic, conflict-resolution logic, or sync transport is introduced.

**Why this change is required:** This is explicitly a hedge, not a feature. It is recommended, not mandatory, precisely because it has no functional payoff on its own — its entire value is in avoiding a more expensive schema change later, and that value only exists if the column is added before the schema stabilizes further. Bundling it into whichever migration in this spec is already touching the relevant tables (MIG-05, if timing allows) further minimizes its cost.

**Files / modules likely affected:**
- `data/local/MemoryEntities.kt` (new nullable column)
- `data/local/MemoryRoomMapper.kt` (mapping, populate-on-write only)
- Room schema migration and test

**Database migration required?** Yes. Purely additive, nullable column with no default-value complexity.

**Backward compatibility considerations:** None of substance. The column is unused by any read path, so there is no behavior to regress.

**Acceptance criteria:**
- New column exists and is populated on write with a stable per-installation identifier.
- No existing query, ranking, or conflict-resolution behavior reads or depends on this column.
- Migration test passes.

**Verification strategy:** Room migration test only. No functional test is meaningful yet since no behavior depends on this field.

**Regression risks:** Minimal.

**Dependencies:** None strictly, but should be bundled with MIG-05's schema migration if scheduling allows, to avoid a standalone low-value migration consuming its own review cycle.

**Estimated implementation complexity:** Small.

**Out of scope:** Any sync, merge, or multi-device logic. Any change to `MemoryInsertResult.RevisionConflict`'s reject-on-conflict behavior.

**Future follow-up:** If and when backup/sync becomes an approved product decision, this column is the starting point for a device-identity and merge-vector design — governed by its own future architectural review, not this spec.

---

## MIG-10 — Generalize the Isolated Untrusted-Content Parsing Boundary

**Classification:** Recommended before beta

**Objective:** Extract the process-isolation pattern currently implemented only for PDF parsing (`data/pdfbox/isolation/**`, AIDL-based) into a reusable "untrusted content sandbox" contract, and migrate OneNote HTML plain-text extraction into it.

**Architectural Principle:** Trust boundaries should be a property of "parsing content that did not originate from Memora," not a property of one specific file format.

**Problem Being Solved:** The isolated-process PDF parser is genuinely sophisticated trust-boundary engineering — untrusted binary content is parsed in a sandboxed process with descriptor handoff rather than raw file access. But it is PDF-specific by construction. `OneNoteHtmlPlainText.kt` parses third-party HTML retrieved from the Microsoft Graph API via in-process regex-based stripping, with no sandboxing boundary at all. This is a materially lower-risk operation than full PDF binary parsing (it is text stripping, not a complex parser executing over untrusted binary structure), but it is still untrusted external content processed in-process, and it is the only other place in the codebase where that category of risk currently exists.

**Current Implementation:** `IsolatedPdfParserService`/`IsolatedPdfParserClient` implement a PDF-specific AIDL sandbox. `OneNoteHtmlPlainText.kt` runs unsandboxed, in-process.

**Target Implementation:** A generic sandboxed-content-parsing contract is extracted from the existing PDF isolation implementation's shape (isolated process, descriptor/data handoff, bounded result contract, recoverable failure). The PDF parser is refactored to be the first implementation of this generic contract, with no behavior change. OneNote HTML stripping is evaluated against the same contract and migrated into it if the resulting complexity is justified by its lower-but-nonzero risk profile; if not justified, this migration documents that decision explicitly rather than leaving the gap unaddressed silently.

**Why this change is required:** This is the highest-leverage trust-model investment available in the codebase, and it is currently trapped in one vertical. Recommended, not mandatory, for beta because no new binary-content source type is imminent beyond the two that already exist — but every day it remains PDF-specific increases the chance that a future source type replicates unsandboxed parsing instead of the sandboxed pattern, simply because the sandboxed pattern isn't generic enough to reach for.

**Files / modules likely affected:**
- `data/pdfbox/isolation/**` (extract generic contract from existing PDF-specific implementation)
- `data/notes/OneNoteHtmlPlainText.kt` (migrate into the generic contract, or document why not)
- `app/src/main/aidl/com/memora/app/data/pdfbox/isolation/IIsolatedPdfParser.aidl` (generalize interface naming/shape if warranted, without breaking the existing PDF contract)

**Database migration required?** No.

**Backward compatibility considerations:** PDF parsing behavior must be identical before and after the extraction of the generic contract — this is a refactor of the PDF isolation layer's internal shape, not its external behavior. Existing PDF isolation integration tests (`IsolatedPdfParserEndToEndIntegrationTest`, `AndroidIsolatedPdfParserConnectionIntegrationTest`, `LiveIsolatedPdfParserProcessDeathIntegrationTest`) must continue passing unmodified.

**Acceptance criteria:**
- A generic sandboxed-content-parsing contract exists, independent of PDF-specific types.
- PDF parsing continues to pass all existing isolation, process-death, and benchmark integration tests unmodified.
- A documented decision exists on whether OneNote HTML extraction is migrated into the sandbox or explicitly deferred with rationale.

**Verification strategy:** Full existing PDF isolation integration test suite run unmodified; if OneNote migration is included, new integration tests covering malformed/hostile HTML input handled safely within the sandbox boundary.

**Regression risks:** Medium, concentrated entirely in the PDF isolation refactor — this subsystem has extensive process-death and live-conversion test coverage specifically because it is fragile under process lifecycle edge cases (`ConversionLiveProcessDeathIntegrationTest`, `LiveIsolatedPdfParserProcessDeathIntegrationTest`). Any generalization must be verified against this existing coverage, not just new unit tests.

**Dependencies:** None from MIG-01–MIG-09; this migration is independent of the evidence/search convergence work.

**Estimated implementation complexity:** Large, primarily due to the process-lifecycle test surface that must be preserved, not due to the contract extraction itself.

**Out of scope:** Sandboxing any future asset type's parser that does not yet exist. Changing PDF parsing behavior, performance characteristics, or AIDL wire format in any user-visible way.

**Future follow-up:** Any future binary or semi-structured content source (a new note provider with richer markup, a future document format) implements this contract from day one instead of choosing between "write a new sandbox" and "skip sandboxing."

---

## MIG-11 — Introduce an ANN-Ready Embedding Store Interface

**Classification:** Future architecture

**Objective:** Change `MemoryEvidenceEmbeddingStore`'s (post-MIG-05) query contract from "return every vector for brute-force scan" to a shape that permits an approximate-nearest-neighbor index to be introduced later without a further interface change.

**Architectural Principle:** An interface should not encode an implementation's current performance ceiling as part of its contract.

**Problem Being Solved:** `listForModel()` returns every stored vector for full in-memory cosine-similarity scan. This is correct and adequate at current and near-term data volumes. It is not adequate for a system whose stated ambition is personal memory infrastructure spanning years of accumulated photos, screenshots, PDFs, and notes — but building ANN indexing infrastructure now, before there is data at a scale that requires it, would be premature optimization contrary to this spec's "prefer convergence over rewrites, do not redesign" mandate.

**Current Implementation:** `domain/intelligence/EmbeddingContracts.kt` defines `listForModel()` returning the full candidate set; `SearchAssetMemoriesByMeaning` performs the cosine scan in application code.

**Target Implementation:** The store interface is reshaped to accept a query vector and a result-count bound directly (`findNearest(queryVector, model, limit)`), with the current brute-force implementation satisfying this contract identically to today's behavior. No ANN library or index structure is introduced. This migration changes only the *shape* of the contract so that a future ANN-backed implementation is a drop-in replacement rather than a breaking interface change.

**Why this change is required:** This is explicitly deferred to future architecture, not because it lacks value, but because it has no user-visible payoff until data volume actually justifies it, and doing it now would mean maintaining unused indexing infrastructure. It is listed in this spec rather than omitted so that the interface-shape decision — which is cheap and worth making early — is not lost or forgotten, even though the implementation behind it is correctly deferred.

**Files / modules likely affected:**
- `domain/intelligence/EmbeddingContracts.kt` (interface reshape)
- `data/local/RoomMemoryEmbeddingStore.kt` (post-MIG-05 generalized store; reimplement existing brute-force logic behind the new contract shape)
- `application/intelligence/SearchAssetMemoriesByMeaning.kt` (call-site update only; ranking logic unchanged)

**Database migration required?** No.

**Backward compatibility considerations:** Search result ordering and quality must be identical before and after, since the underlying algorithm does not change in this migration — only where the scan happens (inside the store implementation vs. in application code).

**Acceptance criteria:**
- Meaning search produces identical results before and after this migration for the same fixture corpus.
- The store interface no longer requires callers to retrieve the full candidate set to perform ranking.

**Verification strategy:** Existing `SearchAssetMemoriesByMeaningTest`/`SearchAssetMemoriesByMeaning` fixture-based tests re-run against the reshaped interface with no expected result changes.

**Regression risks:** Low, given no algorithmic change — risk is confined to the interface migration itself.

**Dependencies:** MIG-05 (requires the generalized evidence-level embedding store to exist first).

**Estimated implementation complexity:** Medium.

**Out of scope:** Selecting, integrating, or benchmarking any actual ANN library. Any change to ranking algorithm, evidence-token-boost logic, or result quality.

**Future follow-up:** When benchmark data justifies it (per `LOCAL_AI_TECHNICAL_SPEC.md` §11's requirement that no performance claim be made without benchmark evidence), a concrete ANN-backed implementation of this contract can be introduced as its own future migration, gated by measured need rather than speculative scale.

---

## 2. Explicitly out of scope for this entire specification

Per the governing scope boundary stated at the top of this document, and per `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §8's required delivery sequence, the following are **not** specified here and must not be started under this document's authority:

- Any `Link`, Event Memory, or Knowledge Memory implementation work.
- Any Context/Correlation Engine implementation.
- Any personalization or feedback-loop implementation.
- Any `VisionEngine`, `RecallRanker`, or other model-backed capability implementation (their integration *seam* is finalized in MIG-04; their implementation is not part of this spec). This is distinct from MIG-07B's deterministic structured-filter stage, which implements already-frozen non-model-backed recall behavior and does not constitute a `RecallRanker` implementation.
- Any change to the MVP asset-type list, source coverage, or PRD exclusions defined in `PRODUCT_CONTRACT.md`.
- Any cloud AI, backup, or sync feature (MIG-09 only reserves schema headroom; it does not implement or approve sync).

Work in these areas requires its own future architectural review and, where noted in `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §8, its own accepted quality/behavioral-contract gate before implementation begins.
