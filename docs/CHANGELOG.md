# Change Log

## Unreleased

### CONTINUE status table honesty (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Find-by-meaning status row now reflects MIG-05 step 3
  evidence-level embedding ranking (not PdfPageEmbedding* for search).

### Escape-hatch audit + Live/Dual metric habit (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added `docs/ESCAPE_HATCH_AUDIT.md` (cadence, checklist,
  record template) and `docs/RECALL_ENFORCEMENT_INDEX.md` (Steps 1–7).
  Allowlist Audit cadence + CONTINUE Live/Dual = 7 one-liner. Enforcement
  program complete (docs); MIG code not started.
- **Change control:** `docs/CHANGE_CONTROL_ESCAPE_HATCH_AUDIT.md`.

### Canonical Recall shared result + Why contract (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added `docs/CANONICAL_RECALL_RESULT_CONTRACT.md` — DRAFT
  logical shared Find hit + Why fields (path labels KEYWORD|MEANING; no
  Kotlin). CONTINUE + `RECALL_CONVERGENCE_DONE` pointers. No app code; does
  not authorize MIG-06+.
- **Change control:** `docs/CHANGE_CONTROL_CANONICAL_RECALL_RESULT_CONTRACT.md`.

### MIG-05 + Recall convergence DONE checklists (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added MIG-05 FULL DONE retirement checklist (unchecked) to
  `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE`; new
  `docs/RECALL_CONVERGENCE_DONE.md` (Canonical Recall program exit; retirement
  mandatory). CONTINUE pointers only. No app code; checklists remain open;
  does not authorize MIG-05 step 4 / MIG-06+.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_RECALL_CONVERGENCE_DONE_CHECKLISTS.md`.

### Change-control architectural convergence (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Wired Find/Recall enforcement into change-control —
  architectural convergence block on `CHANGE_CONTROL_TEMPLATE`; new
  `LEGACY_EXTENSION_EXCEPTION.md` (mandatory sunset); GOVERNANCE pre-work
  pointers. No app code; does not authorize MIG-06+ or reopen Freeze.
- **Change control:** `docs/CHANGE_CONTROL_ARCHITECTURAL_CONVERGENCE_TEMPLATE.md`.

### Cursor architecture invariants rule (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added alwaysApply Cursor rule
  `.cursor/rules/unfynd-architecture-invariants.mdc` — Canonical Recall /
  Memory substrate invariants, false-positive guard, pre-code gate; links
  Freeze §3, ADR-049, `LEGACY_RECALL_SURFACE`. No app code; does not authorize
  MIG-05 step 4 / MIG-06+.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_ARCHITECTURE_INVARIANTS_RULE.md`.

### Legacy recall surface allowlist (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Introduced `docs/LEGACY_RECALL_SURFACE.md` — living
  shrink-only allowlist (L1–L8) for interim product Find paths; Live/Dual
  count = 7. Does not authorize MIG-05 step 4 / MIG-06+ or Cursor rules.
- **Change control:** `docs/CHANGE_CONTROL_LEGACY_RECALL_SURFACE.md`.

### ADR-049 Canonical Recall naming (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-049: App Find boundary name **Canonical Recall**;
  MIG-06 `SearchMemoryEvidence` = candidate generation into it; Grounding
  Retriever Option C (same pipeline); Spec `RecallRanker` / MIG-07B stages
  inside Canonical Recall. No MIG-06+ authorization; no app search code.
- **Change control:** `docs/CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md`.

### MIG-05 step 3 — meaning search cutover to evidence embeddings

- **Date:** 2026-08-29
- **Delivered:** `SearchAssetMemoriesByMeaning` ranks page/evidence hits from
  `MemoryEvidenceEmbeddingStore` + `MemoryEvidence` excerpt (page via
  `PdfPageEvidenceLocator`); no `SavedPdfPageTextSource` / no
  `PdfPageEmbeddingStore` on the search ranking path. Readiness
  `indexedCount` = summary + evidence stores. Cutover STALE targets only
  READY gap revisions (page embeddings without evidence embeddings for the
  active model); meaning-index drain includes STALE and restores READY after
  dual-write fill.
- **Truthfulness:** `IndexPdfPageEmbeddings` still dual-writes
  `PdfPageEmbedding*`. Full MIG-05 acceptance remains open (step 4 retirement
  + remaining Spec criteria). Room stays 14. MIG-06+, ranking redesign,
  AVAILABLE, Act, Grounded Answers code, Links, Event/Knowledge, VisionEngine,
  and package/db rename are not started. Hashed freeze/spec/amendment files
  unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (step 3 section).

### MIG-05 pre-step-3 — e2e dual-write device proof

- **Date:** 2026-08-29
- **Delivered:** `IndexPdfPageEmbeddingsDualWriteInstrumentedTest` proves on
  Medium Phone that dual-write persists both `pdf_page_embeddings` and
  `memory_evidence_embeddings` with real `e{n}` evidence ids (Room v14);
  unresolved path writes page store only. Search constructor assert confirms
  no cutover.
- **Truthfulness:** MIG-05 step 3 (Search cutover) not started. Room stays 14.
  Full MIG-05 acceptance remains open.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (pre-step-3 e2e dual-write section).

### MIG-05 step 2 — PDF page embedding dual-write to evidence store

- **Date:** 2026-08-29
- **Delivered:** `IndexPdfPageEmbeddings` dual-writes successful PDF page
  vectors into `MemoryEvidenceEmbeddingStore` keyed by
  `(revisionId, evidenceId, model)` when `MemoryEvidence.id` resolves via
  locator `pdf:page:N` (never the locator as id). Fingerprint-skip backfills
  the evidence store without mass `STALE_REINDEX`. Callers thread resolved
  evidence ids through `PdfPageEmbeddingCandidate`.
- **Truthfulness:** Search still reads `PdfPageEmbedding*` +
  `SavedPdfPageTextSource`. Full MIG-05 acceptance remains open. Room stays
  14. MIG-06+, ranking, AVAILABLE, Act, Grounded Answers code, Links,
  Event/Knowledge, VisionEngine, and package/db rename are not started.
  Hashed freeze/spec/amendment files unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (step 2 section).

### MIG-05 step 1 residual closed (Room 13→14 device-verified)

- **Date:** 2026-08-29
- **Delivered (docs only):** `MemoraDatabaseMigrationTest` passed **3/3** on
  the Medium Phone emulator (Android Studio Run of class). Room 13→14
  evidence-embedding store foundation is device-verified. No application
  code change in this residual close. MIG-05 step 2 not started.
- **Truthfulness:** Full MIG-05 acceptance remains open. Hashed
  freeze/spec/amendment files unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`.

### MIG-05 step 1 — evidence-level embedding store foundation

- **Date:** 2026-08-29
- **Delivered:** Additive `MemoryEvidenceEmbedding` domain + Room table/store
  (`memory_evidence_embeddings`, Room 13→14) keyed by
  `(revisionId, evidenceId, model)`. Hilt-bound; unused by Search/Index.
  **Dual-store interim:** `PdfPageEmbedding*` remains the live PDF meaning
  path. No `STALE_REINDEX_REQUIRED` marking in this step. No SQL remap of old
  page vectors onto evidenceIds. No search/index cutover.
- **Truthfulness:** Full MIG-05 acceptance remains open. MIG-06+, ranking,
  AVAILABLE, Act, Grounded Answers code, Links/Event/Knowledge, VisionEngine,
  and package/db rename are not started. Hashed freeze/spec/amendment files
  unchanged. `ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION` remains 5
  (conversion journal, not Room). Device residual later closed separately
  (see Unreleased residual entry above).
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`.

### Class A pack enrichment — status, examples, open-foundation docs (docs-only)

- **Date:** 2026-08-29
- **Delivered:** `public/unfynd-core/` enrichment: `SPEC-STATUS.md` (honest
  contract vs App table); synthetic `examples/` (valid Memory+evidence + reject
  cases; ADR-048); `PUBLIC_CHANGELOG.md`, `CONTRIBUTING.md`, `GOVERNANCE.md`;
  README pack revision + inspect/build-on/run section; ROADMAP-OPEN / SPEC /
  NOTICE / CITATIONS pointers. LICENSE remains Apache-2.0. Synced pack files
  only to https://github.com/CzarPiratez/unfynd-core. Private Memora not pushed.
  No App code, AI Packs, or personal fixtures.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_ENRICHMENT.md`; ADR-048.

### ADR-048 synthetic Class A reference samples (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-048: synthetic non-personal Class A examples under
  the same Apache-2.0 license as the pack; not a schema lock; no App open.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_ENRICHMENT.md`.

### Class A pack copy fix — App vs Core / no Android-only framing (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Public pack under `public/unfynd-core/` rewritten for honest
  App vs Core nouns (multiplatform App; Core = infrastructure). Removed
  Android-as-product / “reference application” framing from README, SPEC,
  SECURITY, ROADMAP-OPEN, and NOTICE. Optional line: commercial / enterprise
  licensing is separate from this Apache-2.0 contracts pack. LICENSE unchanged
  (Apache-2.0). Synced pack files only to https://github.com/CzarPiratez/unfynd-core.
  Private Memora monorepo not pushed.
- **Change control:** ADR-047 Class A pack (docs copy).

### Class A pack published to public GitHub (docs-only)

- **Date:** 2026-08-29
- **Delivered:** ADR-047 follow-up (c): Class A Apache-2.0 pack published to
  https://github.com/CzarPiratez/unfynd-core root only (`LICENSE`, `NOTICE`,
  `README.md`, `SPEC.md`, `ROADMAP-OPEN.md`, `SECURITY.md`, `CITATIONS.md`).
  Private Memora monorepo was not pushed.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.

### Root README / identity alignment — App vs Core (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Root `README.md` rewritten to site-aligned UNFYND App vs
  UNFYND Core voice (privacy-first, vision ladder as direction with Act/agents
  and AVAILABLE non-claims, Class A honesty, multiplatform App surfaces). Light
  App vs Core table fix in `public/unfynd-core/README.md`; light identity touch
  in `CONTINUE.md` / `AGENTS.md`. No MIG-*; no technical-ID renames; no private
  Memora push. Public Core GitHub publish tracked as Class A follow-up (c).
- **Change control:** `docs/CHANGE_CONTROL_README_APP_VS_CORE.md`.

### Class A NOTICE copyright attribution fix (docs-only)

- **Date:** 2026-08-29
- **Delivered:** `public/unfynd-core/NOTICE` copyright holder set to
  `Copyright 2026 UNFYND <czar.piratez@gmail.com>`. Pack scan: no other
  Memora / mir.m.hameedi copyright lines. Trademark and Apache NOTICE body
  unchanged.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.

### Class A pack files under `public/unfynd-core/` (docs-only)

- **Date:** 2026-08-29
- **Delivered:** ADR-047 follow-up (a): curated Class A pack landed at
  `public/unfynd-core/` under Apache-2.0 (`LICENSE`, `NOTICE`, `README.md`,
  `SPEC.md`, `ROADMAP-OPEN.md`, `SECURITY.md`, `CITATIONS.md`). Plain-language
  distillation of Core contracts (Memory/Evidence, evidence classes, capability
  seams including MemoryBuilder, local-first / truth-before-intelligence /
  retrieval-first). No hashed Freeze/Spec/Amendment/Grounding/Product Contract
  blob edits.
- **Truthfulness:** Root README App vs Core rewrite and public GitHub publish of
  **only** this pack were deferred at pack landing; publish completed as
  follow-up (c). No MIG-*, no private Memora push, no app source, no secrets.
  Act still out; no AVAILABLE / Act shipped claims.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.

### ADR-047 Class A UNFYND Core contracts under Apache-2.0 (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-047: Release Class A authorizes a curated
  **Public Specification / Contract** pack for UNFYND Core under **Apache
  License 2.0** (Memory/Evidence model, evidence classes, capability seams
  including MemoryBuilder, local-first / truth-before-intelligence /
  retrieval-first principles in plain public markdown). Cites ADR-046 Core
  noun; strategy §23 Class A; ADR-040 technical IDs unchanged. Product owner
  accepts Apache-2.0 residual legal risk without external counsel for this
  docs/contracts pack. Class A does not open the Android app or proprietary
  assets. Openness is product strategy (no grant-program framing).
- **Truthfulness:** Pack files (`public/unfynd-core/` or equivalent), root
  README Class A rewrite, and public GitHub repo publish are **not** created
  in this step — deferred follow-ups. No MIG-*, Grounded Answers code,
  Act/agents, Class B commercial licensing completion, or Architecture Freeze
  reopen. Hashed freeze/spec/amendment/grounding/contract/GA files unchanged.
  No `MemoraApp/` edits.
- **Change control:**
  `docs/CHANGE_CONTROL_ADR047_CLASS_A_CORE_CONTRACTS.md`.

### ADR-046 Core noun on-device memory and intelligence infrastructure (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-046: living-canon and public product language is
  **UNFYND Core — on-device memory and intelligence infrastructure**. Supersedes
  ADR-045 public wording (“on-device intelligence infrastructure”) only; ADR-045
  still stands for retired PKI/PII/grant metaphors, hashed PKI untouched,
  technical IDs unchanged, and non-authorization of Class A / app open / MIG /
  Act. Prefer “memory and intelligence” so Core is not read as a generic
  local-LLM stack without a Memory substrate.
- **Truthfulness:** No Class A pack, public GitHub, MIG-*, Grounded Answers
  code, Act/agents, or Architecture Freeze reopen. Hashed freeze/spec/amendment/
  grounding/contract/GA files unchanged. No `MemoraApp/` edits.
- **Change control:**
  `docs/CHANGE_CONTROL_ADR046_ON_DEVICE_MEMORY_AND_INTELLIGENCE_INFRASTRUCTURE.md`.

### ADR-045 UNFYND Core as on-device intelligence infrastructure (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-045: living-canon and public product language for
  the substrate is **UNFYND Core — on-device intelligence infrastructure**
  (durable multimodal memory next to data that cannot leave; local-first;
  evidence-backed). Retires “Personal Knowledge Infrastructure,” “Personal
  Intelligence Infrastructure,” and third-party grant metaphors from new
  living-canon / CONTINUE / AGENTS identity / registry interpretation lines.
  PKI remains a prior internal noun where hashed docs or ADR-043 still use it;
  ADR-043 substance stands. Technical IDs unchanged (`com.memora.app`,
  `memora.db`, Keystore/MSAL hosts, GitHub/`Memora` folder).
- **Truthfulness:** No Class A publish, app open, MIG-*, Grounded Answers code,
  Act/agents, or Architecture Freeze reopen. Hashed freeze/spec/amendment/
  grounding/contract/GA files unchanged. No `MemoraApp/` edits.
- **Change control:**
  `docs/CHANGE_CONTROL_ADR045_ON_DEVICE_INTELLIGENCE_INFRASTRUCTURE.md`.

### MIG-04 MemoryBuilder contract boundary

- **Date:** 2026-08-28
- **Delivered:** Spec §4 `MemoryBuilder` gains `assemble` returning a
  schema-validated Memory (or pure build outcome). Deterministic assembly is
  `DeterministicMemoryBuilder` (Available; no AI Pack). Production drain path is
  `RunPendingAssetMemoryAssembly` → `AssembleAssetMemoryFromExtractionFacts` →
  `MemoryBuilder` with empty local observations. Non-empty observations fail
  clearly (not silently dropped). Assembly schema remains
  `asset-memory-facts-v4`. No Room migration. No user-visible Find change.
- **Truthfulness:** VisionEngine / observation processing, MIG-05+ embeddings
  store, RecallRanker operate, ranking/Find/Why UI, Links, Grounded Answers
  code, and package rename are not started. Hashed freeze/spec/amendment files
  unchanged. ADR-044 unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG04_MEMORY_BUILDER_CONTRACT.md`.

### ADR-044 low-power equals event-driven Memory lifecycle (docs-only)

- **Date:** 2026-08-28
- **Delivered:** Accepted ADR-044: on-device power posture is the event-driven
  Memory lifecycle (sparse index → cheap recall → rare deep thought), not
  neuromorphic silicon or always-on sensing. Cites Spec §7 / §9 and Freeze
  retrieval-first / truth-before-intelligence. Paused indexing under
  battery/storage/thermal/WorkManager constraints is correct product behavior
  with honest UI. Interpretation only.
- **Truthfulness:** No application, database, Room, retrieval, UI, or MIG-*
  code. Does not reopen Architecture Freeze, authorize Grounded Answers
  implementation, always-on camera/mic, or cloud AI on the core path. Hashed
  freeze/spec/amendment/grounding/contract files unchanged. ADR-043 Act-out
  unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ADR044_LOW_POWER_MEMORY_LIFECYCLE.md`.

### MIG-03 TIME and TOPIC Memory Anchors

- **Date:** 2026-08-28
- **Delivered:** `AssembleAssetMemoryFromExtractionFacts` emits TIME anchors
  from surviving EXIF `Date taken:` facts (`exif:fields`) and TOPIC anchors
  from `pdf:title` / `note:title` SOURCE_METADATA, in addition to TEXT.
  PERSON / PLACE / OBJECT / ACTIVITY / PURPOSE are not fabricated. Evidence
  class remains DIRECT. Assembly schema bumped to `asset-memory-facts-v4`
  (non-forced reindex). No Room schema migration.
- **Truthfulness:** Ranking/Find/Why do not filter by anchor kind. Links,
  Grounded Answers code, package rename, and MIG-04+ are not started. Hashed
  freeze/spec/amendment files unchanged. MIG-01 residual already closed
  (Room 12→13 device-verified).
- **Change control:** `docs/CHANGE_CONTROL_MIG03_TIME_TOPIC_ANCHORS.md`.

### MIG-01 residual closed (Room 12→13 device-verified)

- **Date:** 2026-08-28
- **Delivered (docs only):** `MemoraDatabaseMigrationTest` passed **2/2** on
  the Medium Phone emulator. Room 12→13 evidence-class backfill is
  device-verified. No application code change in this residual close.
- **Truthfulness:** Hashed freeze/spec/amendment files unchanged. MIG-03+ not
  claimed here.
- **Change control:** `docs/CHANGE_CONTROL_MIG01_RESIDUAL_CLOSED.md`.

### MIG-02 Remove Artificial Evidence Item/Length Caps

- **Date:** 2026-08-28
- **Delivered:** Asset Memory assembly no longer truncates to 8 evidence items.
  Every usable deterministic fact becomes `MemoryEvidence` (`DIRECT`). Per-item
  character bound is a pathological guard at 8192 chars (aligned with the
  provisional PDF page write budget), documented as not a completeness policy.
  `MAX_SUMMARY_CHARS` ≤ 240 remains display-only. Assembly schema bumped to
  `asset-memory-facts-v3` so the next legitimate reassembly gets richer
  evidence; no forced mass reindex of existing memories.
- **Truthfulness:** Search paths, embeddings, typed anchors, Grounded Answers,
  ranking/Find/Why UI, Links, package rename, and MIG-03+ are not started in
  this entry. Hashed freeze/spec/amendment files unchanged. MIG-01 Room 12→13
  instrumentation residual later closed separately (see Unreleased residual
  entry above).
- **Change control:** `docs/CHANGE_CONTROL_MIG02_EVIDENCE_CAPS.md`.

### MIG-01 Evidence Class Taxonomy (Room 12→13)

- **Date:** 2026-08-24
- **Delivered:** Additive `MemoryEvidenceClass` on Asset Memory evidence
  (`DIRECT`, `VALIDATED_OBSERVATION`, `RETRIEVAL_SIGNAL`, `HYPOTHESIS`). All
  evidence currently assembled from deterministic extraction facts is tagged
  `DIRECT`. Existing `memory_evidence` rows backfill `DIRECT` via Room
  migration 12→13. Construction without an explicit class does not compile.
- **Truthfulness:** No ranking, Find, Why, or UI change. Class is not used
  for display or link eligibility. Links remain unimplemented. Hashed
  freeze/spec/amendment files unchanged. ADR-043 Act-out unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG01_EVIDENCE_CLASS.md`.

### ADR-043 PKI vision vs freeze; Act remains out (docs-only)

- **Date:** 2026-08-24
- **Delivered:** Accepted ADR-043: PKI is the north star; search is one
  capability; Android is the first reference implementation. Frozen architecture
  is suitable through See/Remember, staged Connect, retrieve-by-meaning, and
  Understand / converse-as-Q&A. Act remains out of current architecture. MIG-*
  not started. Hashed freeze/spec/amendment files unchanged.
- **Truthfulness:** No application, database, migration, retrieval, or package
  rename. Historical changelog entries unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ADR043_PKI_VISION_ALIGNMENT.md`.

### ADR-042 Freeze vs Grounding and GOVERNANCE order (docs-only)

- **Date:** 2026-08-24
- **Delivered:** Accepted ADR-042: Freeze §1 exclusivity is the Asset-Memory /
  PKI core; Grounding Architecture remains the sole Grounded Answers
  constitution; GOVERNANCE numbered order aligned with Freeze §2; Freeze §7
  §15/§7 deferral recorded as errata without rewriting hashed specs. MIG-*
  not started.
- **Truthfulness:** No application, database, migration, retrieval, or package
  rename. Historical changelog entries unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ADR042_FREEZE_GROUNDING.md`.

### Architecture Freeze v1.0 and Migration Spec V1 registration (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Registered `ARCHITECTURE_FREEZE_v1.0.md` and
  `ARCHITECTURAL_MIGRATION_SPEC_V1.md` with SHA-256 hashes; ADR-041 records
  registration and open authority conflicts. No MIG-* implementation.
- **Truthfulness:** No application, database, migration, retrieval, or package
  rename. Historical changelog Memora wording unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ARCHITECTURE_FREEZE_REGISTRATION.md`.

### UNFYND identity Step 5 — presentation/entry identifier rename

- **Date:** 2026-08-23
- **Delivered:** `UnfyndApplication`, `UnfyndTheme` / `Theme.Unfynd`, and
  Compose entry `UnfyndApp` / `UnfyndAppReady` / `UnfyndWelcomeScreen`. PascalCase
  Unfynd*, not UNFYND*.
- **Truthfulness:** No `applicationId`/namespace, database, MSAL host, Gradle folder,
  or user-visible string changes in this checkpoint. Emulator not claimed here.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP5_PRESENTATION_NAMES.md`.

### UNFYND identity Step 4 — user-visible Android product-noun copy

- **Date:** 2026-08-23
- **Delivered:** Launcher label and user-visible product-as-subject copy now UNFYND
  (strings, Compose/copy objects, notices header, debug SAF fixture title, matching
  unit copy tests). Resource IDs such as `clear_memora_index`, classes, packages,
  and `applicationId` unchanged.
- **Truthfulness:** No Gradle identity, database, MSAL host, Theme/Application
  rename (Step 5), or emulator claim in this checkpoint.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP4_ANDROID_COPY.md`.

### UNFYND identity Step 3 — living-canon product-noun overlay (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Overlay of product-as-subject Memora → UNFYND on living constitutions
  and amendments, with identity headers. Domain language (Memory, Find, Evidence
  Package) unchanged. Historical ADR-001–039 bodies and `.docx` hashes unchanged.
- **Truthfulness:** No Kotlin, XML strings, Gradle, package, database, or GitHub
  rename; no Grounded Answers or Event/Knowledge implementation.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP3_CANON_OVERLAY.md`.

### UNFYND identity Step 2 — architecture registry (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Inventoried and registered frozen architecture Markdown (hashes of
  Git blobs). Grounded Answers files now tracked; Experience Memory and Local-AI Spec
  hashes recorded; no competing Grounded Answers constitution.
- **Truthfulness:** No product-noun overlay (playbook Step 3); no package, database,
  Gradle, Kotlin, or UI-string rename; historical `.docx` hashes unchanged.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP2_ARCHITECTURE_REGISTRY.md`.

### UNFYND identity ADR-040 (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Accepted ADR-040: product/brand name is UNFYND (formerly Memora);
  PKI north star; Android is a milestone; domain language and technical IDs
  unchanged. Registry interpretation updated; historical `.docx` hashes unchanged.
- **Truthfulness:** No constitution overlay; no package, database, Gradle, Kotlin,
  or UI-string rename; no new architecture-file hashes.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_ADR040.md`.

### UNFYND identity transition playbook (docs-only)

- **Date:** 2026-08-22
- **Delivered:** Operating playbook for UNFYND product identity (formerly Memora),
  Personal Knowledge Infrastructure direction, Android as a milestone, branding
  overlay on frozen architecture, and one-step-at-a-time execution. No identity ADR.
- **Truthfulness:** No package, database, Gradle, Kotlin, or UI-string rename.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_PLAYBOOK.md`.

### Grounded Answers architecture constitution (docs-only)

- **Date:** 2026-08-05
- **Delivered:** Canonical `GROUNDING_ARCHITECTURE.md`; `GROUNDED_ANSWERS_AMENDMENT_V1.md`;
  PDF slice acceptance spec; ADR-033–039; G-01–G-08 traceability; Local-AI Spec §9
  carve-out; Experience Memory §10 status refresh; registry/CONTINUE/ROADMAP updates.
- **Truthfulness:** No ReasoningEngine, generative pack, or Ask UI code. Find remains
  non-generative. Implementation blocked on readiness gates in grounding architecture.
- **Change control:** `docs/CHANGE_CONTROL_GROUNDED_ANSWERS_ARCHITECTURE.md`.

### PDF local-reading progress counts

- **Date:** 2026-08-04
- **Delivered:** Pending PDF count at drain start; live “N of M” progress from
  WorkManager unit successes; completed copy includes drained count.
- **Truthfulness:** Aggregate integers only; no AVAILABLE flip.
- **Change control:** `docs/CHANGE_CONTROL_PDF_LOCAL_READING_PROGRESS.md`.

### Progress status refresh (README / CONTINUE / ROADMAP)

- **Date:** 2026-08-04
- **Delivered:** Honest current-status snapshot aligned to shipped meaning path,
  index progress/cap, M4 pending, and green CI — no AVAILABLE claim change.

### Fix: E5d boost unit-test fixtures (CI)

- **Date:** 2026-08-04
- **Delivered:** Corrected FOXTROT/mira page-vector fixtures so bounded token
  boost can flip ranking; GitHub Actions on `main` green (`970fcf9`).

### Meaning index progress + per-tap cap UI

- **Date:** 2026-08-04
- **Delivered:** Named ≤25 memories/tap batch; live summary/page progress copy;
  remaining READY → tap Build again; bottom Back on disclosure screen; unit tests.
- **Truthfulness:** Cap is per tap, not a library ceiling; no AVAILABLE flip.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_INDEX_PROGRESS_CAP.md`.

### Midrange meaning measurement gate (M4 plan)

- **Date:** 2026-08-04
- **Delivered:** M4 runbook + product AVAILABLE decision checklist; compatibility
  policy honesty for USE on emulator vs pending midrange.
- **Truthfulness:** No AVAILABLE flip; midrange row still ⬜ until device run.
- **Change control:** `docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md`.

### M3 USE meaning PDF page-recall baseline

- **Date:** 2026-08-04
- **Delivered:** Live USE scoring of `meaning-pdf-page-recall-v1` on
  `emulator_medium_phone` (prefer product install).
- **Outcome:** Cosine-only 2/3 (up from compact 0/3); boosted 3/3; keep E5d
  assist. No AVAILABLE UI flip.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_USE.md`.

### E4b Universal Sentence Encoder product embedder

- **Date:** 2026-08-04
- **Delivered:** Product model spec/download/store/disclosure switched to
  MediaPipe USE (ADR-032); legacy average-word cleanup; honesty copy; unit tests.
- **Smoke:** Emulator `mira` → Matched page 5 → Open Page 5 of 5 (accepted).
- **Truthfulness:** Candidate meaning path; not measured AVAILABLE; rebuild index
  after upgrade; E5d assist retained pending M3.
- **Change control:** `docs/CHANGE_CONTROL_E4B_UNIVERSAL_SENTENCE_ENCODER.md`.

### Fix: Done after local PDF reading returns to Welcome

- **Date:** 2026-08-04
- **Delivered:** Completed local reading Done navigates to Welcome instead of
  resetting to Start local reading; optional Read folder again.

### M2 on-device meaning PDF page-recall baseline

- **Date:** 2026-08-04
- **Delivered:** Live MediaPipe scoring of `meaning-pdf-page-recall-v1` on
  `emulator_medium_phone`; disposable model staging; unit + instrumentation.
- **Outcome:** Cosine-only 0/3; E5d-boosted 3/3; recommends E4b. No AVAILABLE
  UI flip (emulator tier ≠ midrange marketing).
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_ON_DEVICE.md`.

### M1 meaning PDF page-recall baseline (JVM harness)

- **Date:** 2026-08-04
- **Delivered:** Labeled corpus `meaning-pdf-page-recall-v1`;
  `MeasureMeaningPdfPageRecallBaseline` (cosine-only vs E5d boost page hit@1);
  `MeaningEvidenceTokenBoost` in domain; unit tests green.
- **Outcome:** Cosine-only below interim bar → E4b recommended for
  semantic-only; boosted path clears labeled @1. No product AVAILABLE flip.
- **Follow-up:** M2 on-device MediaPipe measurement.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_BASELINE.md`.

### E5d evidence-token boost for candidate meaning ranking

- **Date:** 2026-08-04
- **Delivered:** Disclosed bounded boost when a significant cue token appears in
  ranked evidence text; Why copy discloses assist; unit tests.
- **Truthfulness:** Hybrid candidate assist — not keyword Find alone, not
  measured AVAILABLE; E4b still the path for stronger semantic-only ranking.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_EVIDENCE_TOKEN_BOOST.md`.

### E5c index-time PDF page embeddings

- **Date:** 2026-08-04
- **Delivered:** Room v12 `pdf_page_embeddings`; Build meaning index also embeds
  capped PDF pages; Find by meaning ranks summary+page with per-asset dedup;
  hit shows Matched page N; Open uses ranked page; candidate honesty copy.
- **Verification:** Search page-prefer unit test + meaning copy tests (pending
  full suite / smoke).
- **Truthfulness:** Rebuild meaning index required; not measured AVAILABLE.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_EMBEDDINGS.md`.

### Enterprise completion — meaning PDF page recall (backlog)

- **Date:** 2026-08-04
- **Recorded:** Checklist for finishing enterprise-grade multi-page PDF meaning
  recall: E5c index-time page/chunk embeddings; measured ADR-024/025 gate;
  E4b if needed. E5b2e documented as interim only.
- **Change control:** `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md`.

### Meaning PDF cue-best page open (E5b2e)

- **Date:** 2026-08-04
- **Delivered:** On meaning Open original for PDFs, re-rank saved page texts
  against the cue with the on-device embedder; open cue-best page when it
  clearly beats the Memory cite; else keep E5b2d cite/fallback. Durable
  backlog for index-time page/chunk embeddings (E5c).
- **Verification:** ResolveMeaningPdfOpenPage unit tests; meaning copy/VM tests.
- **Truthfulness:** Open-time cue-best ≠ Find-by-meaning page ranking; not
  measured AVAILABLE.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_CUE_BEST_PAGE_OPEN.md`.

### Meaning PDF open uses cited page (E5b2d)

- **Date:** 2026-08-03
- **Delivered:** Meaning-search Open original for PDFs opens the page cited by
  the Memory summary evidence locator (`pdf:page:N`); honest page-1 fallback
  when no cite; Why / hit copy reflect the cited page.
- **Verification:** locator parser, open page resolve, meaning search cite
  flow, and copy unit tests.
- **Truthfulness:** Cited summary page ≠ query-best page ranking.

### Meaning-quality Asset Memory summaries (E5b2c)

- **Requirements:** A-05; ADR-029, ADR-031.
- **Delivered:** No dimension-only EXIF memories; OCR/text-first summaries
  (assembly schema v2); meaning index/lookups scoped to current schema.
- **Verification:** assembly unit tests; full debug unit suite.
- **Truthfulness:** Rebuild memories + meaning index required for existing
  devices; not a measured AVAILABLE claim.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_MEMORY_SUMMARY_QUALITY.md`.

### Local-AI embedding-first track E5b2b open original from meaning hits

- **Requirements:** A-05; ADR-031.
- **Delivered:** Open original on meaning hits (screenshot/photo in-app preview,
  PDF page 1 with honesty, note OneNote/browser launch).
- **Verification:** meaning open ViewModel + copy unit tests; full debug unit suite.
- **Truthfulness:** Ranking still on indexed memories; PDF page cite not invented.

### Local-AI MediaPipe 16 KB page-size fix

- **Requirements:** A-01; ADR-031.
- **Delivered:** Bump `tasks-text` 0.10.14 → 0.10.29 for 16 KB ELF alignment;
  load availability off the main thread after download.
- **Verification:** debug install on 16 KB emulator; unit suite still green.
- **Truthfulness:** Prior crash was native MediaPipe on 16 KB AVD after a
  successful model download — not a failed download.

### Local-AI embedding-first track E5b2 Find-by-meaning candidate recall

- **Requirements:** A-01, A-05; ADR-024, ADR-025, ADR-029, ADR-031.
- **Delivered:** Welcome → Find by meaning; cosine candidate ranking over indexed
  Asset Memories; Why-this-result cites stored summary + cue; readiness honesty.
- **Verification:** ranking + ViewModel + copy unit tests; full debug unit suite.
- **Truthfulness:** Compact candidate path only — not measured AVAILABLE; open
  original from meaning hits deferred.

### Local-AI embedding-first track E5b1 MediaPipe embedder + model download

- **Requirements:** A-01, A-02, A-07; ADR-029, ADR-031.
- **Delivered:** MediaPipe Text Embedder adapter; private model download after
  disclosure; build meaning index CTA; dependency review for tasks-text 0.10.14.
- **Verification:** full `:app:testDebugUnitTest` green; debug compile with
  MediaPipe (`createFromFile` private model path).
- **Truthfulness:** Compact model disclosed; Find-by-meaning UI not shipped yet;
  model not bundled in APK.

### Local-AI embedding-first track E5a embedding index foundation

- **Requirements:** A-01, A-02; ADR-029, ADR-030.
- **Delivered:** `EmbeddingEngine.embedText` contract; `memory_embeddings` Room
  v11; `IndexMemoryEmbeddings` refuses writes while Unavailable; cosine helper;
  Hilt still binds Unavailable embedding engine.
- **Verification:** embedding + index unit tests (engineering gate).
- **Truthfulness:** No meaning-search UI or AVAILABLE claim.

### Local-AI embedding-first track E4a offline pack-container activate

- **Requirements:** A-01, A-07; ADR-023, ADR-029, ADR-030.
- **Delivered:** Offline embedding pack-container fixture; private payload store;
  activate use case; Welcome disclosure CTA to verify/store; clear-index removes
  pack files. EmbeddingEngine stays Unavailable.
- **Verification:** activate + disclosure ViewModel unit tests (engineering gate).
- **Truthfulness:** ACTIVE install record ≠ meaning search AVAILABLE; no INTERNET.

### Local-AI embedding-first track E3 disclosure UI

- **Requirements:** A-01, A-03, A-07; ADR-023, ADR-029.
- **Delivered:** Welcome → About on-device meaning search; honesty screen with
  planned size/storage/license; affirmative acknowledge into Room ledger; no
  download or AVAILABLE claim.
- **Verification:** disclosure copy + ViewModel unit tests (engineering gate);
  optional user smoke on Welcome disclosure.
- **Truthfulness:** Meaning search stays off; keyword recall unchanged.

### Local-AI embedding-first track E2 Room install ledger

- **Requirements:** A-01, A-07; ADR-023, ADR-029.
- **Delivered:** Room v10 `ai_pack_install_ledger`; `MIGRATION_9_10`;
  `RoomAiPackInstallLedger`; Hilt `AiPackManager` = ledger-backed (empty ⇒
  NOT_INSTALLED).
- **Verification:** ledger unit tests; Room integration + migration on emulator.
- **Truthfulness:** No disclosure UI, download, AVAILABLE, or model vendor.

### Local-AI embedding-first track E0–E1 (ADR-029)

- **Requirements:** A-01, A-02, A-03, A-07; ADR-023, ADR-024, ADR-025, ADR-029.
- **Delivered:** Embeddings-first phase plan; domain AI Pack install ledger
  (disclosure → verify → ACTIVE/fail, retain prior known-good);
  `LedgerBackedAiPackManager`; planned `memora-embedding-pack-v1` id.
- **Verification:** `AiPackInstallLedgerTest` JVM (engineering gate).
- **Truthfulness:** No download, Room bind, AVAILABLE UI, or model vendor yet.

### Local-AI measured pack baselines L2 offline integrity + matrix honesty

- **Requirements:** A-01, A-03, A-05, A-07; ADR-023, ADR-024, ADR-025.
- **Delivered:** L2 harness — truncated-payload reject, prior known-good after
  corrupt update, `OFFLINE_CORE_PATH_OK`, emulator support-matrix rows remain
  UNSUPPORTED; compatibility policy draft notes. Product engines stay Unavailable.
- **Verification:** L2 unit + `SyntheticAiPackL2BaselineIntegrationTest` on
  emulator 2026-08-02 (engineering gate).
- **Truthfulness:** No AVAILABLE claim; midrange SUPPORTED rows deferred.

### Local-AI measured pack baselines L1 synthetic integrity harness

- **Requirements:** A-01, A-03, A-07; ADR-023, ADR-025.
- **Delivered:** SHA-256 pack payload verifier; synthetic fixture corpus;
  aggregate baseline measurement (size + integrity failure retained); emulator
  instrumentation staging under no-backup private storage. Product pack manager
  stays Unavailable.
- **Verification:** `:app:testDebugUnitTest` intelligence tests passed;
  `SyntheticAiPackBaselineIntegrationTest` passed on emulator 2026-08-02.
- **Truthfulness:** No AVAILABLE claim; no real model; Vision remains Unavailable.

### Local-AI measured pack baselines L0 opened

- **Requirements:** A-01, A-03, A-05, A-07, E-06; ADR-023, ADR-024, ADR-025.
- **Delivered:** Change-control opened with L0/L1/L2 phase plan; honesty gates;
  no pack/harness code.
- **Verification:** Docs-only; **accepted** 2026-08-02 (proceed to L1).
- **Truthfulness:** No AVAILABLE intelligence claim; Notes/keyword recall ≠ Local AI.

### Notes connector N7 Open original OneNote page

- **Requirements:** P-03, P-08, P-19; ADR-002, ADR-003.
- **Delivered:** Find saved note text → Open original note via Graph page
  `links`; prefers OneNote app when installed, else browser; MSAL WebView Connect;
  silent token refresh; IO timeout against ANR; search remains offline Room-only.
- **Verification:** Unit tests passed; emulator Open → OneDrive web **accepted**
  by user 2026-08-02 (no OneNote app on AVD).
- **Truthfulness:** Open may need network and Microsoft session; not in-app preview.

### Notes connector N6 Build-memories NOTE facts

- **Requirements:** P-03, P-08, P-19; ADR-003, ADR-007, ADR-019.
- **Delivered:** Build memories from saved OneNote page text (`NOTE_TEXT` /
  `note:page`, optional title metadata); pending drain includes notes with
  non-blank extracts; setup honesty updated.
- **Verification:** Unit tests passed; emulator **28** Asset Memories + OneNote
  copy **accepted** by user 2026-08-02.
- **Truthfulness:** Pre-AI cited facts only — not meaning-based ranking.

### Notes connector N5 Find saved note text

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Welcome **Find saved note text**; Room keyword search over
  saved OneNote page text; Why evidence; offline after extract; no Graph in search.
- **Verification:** Unit tests passed; emulator readiness 28 pages + `pass` →
  1 match + Why **accepted** by user 2026-08-02.
- **Truthfulness:** Keyword matching ≠ meaning-based Memory recall.

### Notes connector N4 page text extract

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Room `note_page_extractions` (v9); Graph HTML → plain text;
  user-started Extract drain; honesty “not searchable yet.”
- **Verification:** Unit tests passed; emulator Extract 29/29 for
  `mir.m@outlook.com` **accepted** by user 2026-08-01.
- **Truthfulness:** Extracted text becomes keyword-searchable in N5; still not
  meaning-based Memory recall.

### Notes connector N3 page discovery placeholders

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** User-started Discover OneNote pages via Graph (sections →
  per-section pages) into `AssetType.NOTE` placeholders; bounded pages; vaulted
  session + `ensureSession`; honest “not searchable yet.”
- **Verification:** Unit tests passed; emulator Connect + Discover (25 then 28
  placeholders) **accepted** by user 2026-08-01.
- **Truthfulness:** Placeholders ≠ extracted text ≠ searchable notes.

### Notes connector N2b MSAL Connect / Disconnect

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** MSAL 8.4.1; INTERNET for Microsoft source access; Connect /
  Disconnect OneNote; Keystore vault + MSAL cache clear; scopes Notes.Read,
  User.Read, offline_access. No Graph discovery yet (N3).
- **Verification:** Unit tests passed; emulator Connect (`mir.m@outlook.com`) +
  Disconnect accepted by user 2026-08-01.
- **Truthfulness:** Connected ≠ notes indexed/searchable.

### Notes connector N2a vault + registration gate

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Keystore-backed OneNote session vault; `BuildConfig` client ID /
  signature hash from `local.properties`; Notes status copy for registration
  required / disconnected / session-present; Disconnect + clear-index clears
  vault. Azure runbook + planned MSAL review. **No** MSAL, INTERNET, or Graph.
- **Verification:** Unit tests for copy, config, in-memory vault, secret absence.
  Emulator smoke pending user.
- **Truthfulness:** Does not claim notes are searchable; Connect not offered until
  N2b after Azure registration.

### Notes connector N1 honesty UI

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Welcome → About Notes indexing honesty screen; OneNote + network
  framing; explicit “not indexed yet”; no Connect / MSAL / Graph.
- **Verification:** `NotesConnectorHonestyCopyTest` passed; emulator UI + Back
  accepted by user 2026-08-01.
- **Truthfulness:** Does not claim notes are searchable or connected.

### Notes connector N0 opened (OneNote-class)

- **Requirements:** P-03, P-08, P-15, P-19; ADR-001, ADR-003, ADR-004.
- **Delivered:** `docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md` — phased plan
  N0–N5, network/token honesty, Azure app-registration as N2 external gate.
  Traceability P-08/P-19 and ADR-003 rule updated; CONTINUE points next code at N1.
- **Verification:** Docs-only; no Notes implementation code.
- **Truthfulness:** Do not claim phone-wide notes indexing; OneNote connector only.

### ADR-003 Notes strategy accepted

- **Requirements:** Notes MVP honesty; ADR-001; ADR-003.
- **Delivered:** ADR-003 accepted — OneNote-class read-only provider connector as
  the Notes path; Share-as-indexing and arbitrary note-app scanning rejected.
- **Verification:** Decision recorded; no Notes code in this slice.
- **Truthfulness:** Do not claim all phone notes are indexed until the connector
  ships under change control.

### Screenshot OCR open-original acceptance

- **Requirements:** P-01, P-06, P-11, P-13, P-17; A-01, A-02, A-05.
- **Delivered:** Documentation gate closed for capped read-only Open preview from
  screenshot OCR hits (feature already on `main`).
- **Verification:** Unit tests re-verified 2026-07-31; interactive emulator UI
  accepted same day (SwiftShader; user pass).
- **Truthfulness:** Search still uses saved OCR only; originals read-only.

### Evidence-backed Asset Memory persistence

- **Requirements:** P-02, P-09, P-11, P-14, P-17; A-02, A-04; E-04, E-05.
- **Delivered:** Room v8 persists immutable current-fingerprint Asset Memory revisions
  with normalized evidence, extraction schemas, TEXT anchors, and citation joins.
  An explicit bounded setup action assembles them only from saved PDF text/metadata,
  screenshot/photo OCR, and useful EXIF fields.
- **Verification:** `:app:testDebugUnitTest` passed on 2026-07-31, including
  deterministic assembler, revision-history, and normalized Room mapping tests.
  The v1→v8 migration test is updated; emulator execution remains pending.
- **Truthfulness:** No source reopen, network, Local-AI pack, embeddings, semantic
  ranking, natural-language recall, or confidence claim. Existing keyword search
  remains the interim recall UI.

### Photo OCR extract and keyword search

- **Requirements:** P-01, P-04, P-05, P-06, P-11, P-13, P-14, P-15, P-17;
  A-01, A-02, A-05, A-06; ADR-028.
- **Delivered:** Room v7 adds separate `photo_ocr_extractions`; explicit Read text
  from photos drains PHOTO assets through bundled on-device Latin OCR. Welcome →
  Find saved photo text provides current-fingerprint keyword search, readiness,
  cancel, clear, Why citation, and capped read-only Open original preview.
- **Verification:** `:app:testDebugUnitTest`, Android-test compilation, and debug
  assembly passed on 2026-07-31. Targeted migration execution was blocked before
  tests by emulator package-service `Broken pipe (32)`; visible verification pending.
- **Truthfulness:** PHOTO and SCREENSHOT tables/flows remain separate. Keyword
  matching is not natural-language or semantic Memory recall; no network or AI Pack.

### Public positioning and CI maturity

- **Requirements:** Docs / delivery hygiene; ADR-027.
- **Delivered:** README and GitHub About use the approved on-device personal AI
  memory positioning copy. Lightweight GitHub Actions CI runs
  `testDebugUnitTest` on `main` pushes and PRs.
- **Verification:** Workflow file present; first green run pending after push.
- **Truthfulness:** Public category examples do not expand MVP delivery scope
  (ADR-027).

### Screenshot OCR open-original preview

- **Requirements:** P-01, P-06, P-11, P-13, P-17; A-01, A-02, A-05.
- **Delivered:** From Find saved screenshot text hits, Open original shows a
  hard-capped (960px long edge) read-only in-app preview (or honest
  SourceUnavailable / CouldNotOpen). Search still uses saved OCR text only.
- **Verification:** Unit tests + install pending; emulator confirmation pending.
- **Truthfulness:** Read-only; no edit/upload; keyword search does not reopen
  images for matching; PHOTO out of scope.

### Screenshot OCR keyword search

- **Requirements:** P-01, P-06, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Welcome → Find saved screenshot text searches current-fingerprint
  `screenshot_ocr_extractions.full_text` (LIKE, ADR-022). Readiness, cancel, clear,
  Why citation, highlight. Open-original preview is a follow-on slice.
- **Verification:** Unit tests green; emulator confirmation 2026-07-29 (query
  `note` → 1 match with excerpt + Why on Medium Phone). Reconfirmed 2026-07-30
  after emulator recovery.
- **Truthfulness:** Keyword matching only; ordinary photos excluded.

### Screenshot OCR extract

- **Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** After photo catalogue + EXIF facts, Read text from screenshots
  drains `SCREENSHOT` assets via WorkManager, opens permitted URIs read-only for
  bundled ML Kit Latin OCR, persists `screenshot_ocr_extractions` (Room v6). Honest
  copy — no keyword/Memory claims; PHOTO OCR out of scope.
- **Verification:** Unit tests green; `installDebug` succeeded; emulator
  confirmation 2026-07-28 (3 catalogued; EXIF for 3; OCR text for 1 screenshot;
  honest OCR-only copy).
- **Truthfulness:** Discovery remains metadata-only (ADR-009); OCR is a separate
  explicit step (ADR-026). Keyword search over OCR remains later.

### MediaStore image EXIF extract

- **Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** After photo catalogue completes, Read photo facts drains PHOTO/
  SCREENSHOT assets via WorkManager, opens permitted URIs read-only for ExifInterface,
  persists `image_exif_extractions` (Room v5). Honest copy — no OCR/keyword/Memory claims.
- **Verification:** Unit tests green; `installDebug` succeeded; emulator confirmation
  2026-07-28 (3 fixture photos catalogued; Read photo facts saved basic facts;
  honest EXIF-only copy).
- **Truthfulness:** Discovery remains metadata-only (ADR-009); extract is a separate
  explicit step. OCR remains a later governed capability.

### Keyword search accessibility baseline

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05; Local-AI §11.
- **Delivered:** Headings, polite live regions for search/open statuses, merged
  progress announcements, richer preview image content description on keyword
  search + cited-page preview screens.
- **Verification:** Unit tests green (`PdfKeywordSearch*`); `installDebug` succeeded;
  emulator confirmation 2026-07-28 (user: all pass on sighted flow).
- **Truthfulness:** Keyword search + preview only; not a full-app TalkBack audit.

### Cancel in-flight keyword search

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Dedicated Cancel search while Searching (separate from Search);
  returns Idle keeping typed query; late completions ignored; search Job
  cancelled; short minimum Searching visibility (~700ms) on tiny indexes.
- **Verification:** Unit tests green (`PdfKeywordSearch*`); `installDebug` succeeded;
  emulator confirmation 2026-07-28 (user: pass) — dedicated Cancel search stops
  Searching; query kept; no results.
- **Truthfulness:** Interim keyword path only; does not cancel PDF preview Opening.

### Keyword search clear query

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Clear control on the query field when text is present; clears
  results/Why/open state; typed query also clears after Clear Memora index.
- **Verification:** Unit tests green (`PdfKeywordSearch*`); `installDebug` succeeded;
  emulator confirmation 2026-07-28 (user: all pass).
- **Truthfulness:** Interim keyword path only; does not cancel in-flight search.

### Open original PDF from keyword result (in-app cited-page preview)

- **Requirements:** P-01, P-11, P-13, P-17; A-02, A-05.
- **Delivered:** Per-hit Open original PDF opens a read-only in-app preview of the
  cited page via SAF + `PdfRenderer`; Opening / SourceUnavailable / CouldNotOpen
  feedback; Back returns to results. Search still uses stored text only.
- **Verification:** On 2026-07-28, unit tests passed; debug APK installed. User
  confirmed on Medium Phone (Documents/MemoraFixtures): keyword hit page labels,
  Open original in-app cited-page preview, Back to results, Why unchanged.
- **Truthfulness:** No external viewer page-jump promise; originals remain
  read-only; no AI/network.

### Keyword search IME gate + Searching progress

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Shared `canSubmitSearch` gate for button and keyboard Search;
  field read-only while searching; calm Searching progress copy with spinner.
- **Verification:** On 2026-07-27, search unit tests; debug APK installed. User
  confirmed on Medium Phone: blank keyboard Search blocked; Searching progress
  copy + locked field; results/Why unchanged.
- **Truthfulness:** Interim keyword path only; no AI/network.

### Keyword blank-query guidance + Search button gating

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Find saved PDF text disables Search while the query is blank and
  shows inline empty-query guidance in Idle; non-blank query unchanged.
- **Verification:** On 2026-07-27, search unit tests; debug APK installed. User
  confirmed on Medium Phone: blank → disabled Search + guidance; typed query →
  Search/Why still work.
- **Truthfulness:** Interim keyword path only; no AI/network.

### Keyword search corpus readiness

- **Requirements:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Find saved PDF text shows current-fingerprint page/document
  counts ready for keyword search; refreshes on open and after clear.
- **Verification:** On 2026-07-27, search readiness/copy/ViewModel unit tests;
  debug APK installed. User confirmed on Medium Phone: empty/searchable/clear
  readiness states all display honestly, and search/Why still work.
- **Truthfulness:** Interim keyword inventory only; not Memory recall; no AI/network.

### Keyword search clear invalidation

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Clear Memora index ack resets Find saved PDF text to Idle
  (invalidates in-flight search) so Results/Why cannot cite deleted excerpts.
- **Verification:** On 2026-07-27, ViewModel clear unit tests; debug APK
  installed. User confirmed on Medium Phone: clear drops stale Results/Why.
- **Truthfulness:** Interim keyword path; typed query may remain; no AI/network.

### Keyword excerpt match highlight

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** First case-insensitive query span bold/primary in each result
  excerpt; Why this result? unchanged.
- **Verification:** On 2026-07-27, support/highlight unit tests; debug APK
  installed. User confirmed on Medium Phone: meet highlighted in fixture excerpts.
- **Truthfulness:** Interim keyword path; first occurrence only; no AI/network.

### Clear-index UI recovery (PDF Index + Local reading + live DB)

- **Requirements:** P-04, P-05, P-14, P-15, P-17; A-02, A-06.
- **Delivered:** Ignore stale finished SAF discovery work after clear/reconnect so
  Index this folder returns; cancel Memora WorkManager tags on clear; Completed
  local reading shows Done; reset local-reading session on clear; extract/search
  use live MemoraDatabaseHandle (no closed-DB hang).
- **Verification:** On 2026-07-27, DocumentTree/local-reading/search unit tests;
  debug APK installed. User confirmed on Medium Phone: Index returns after
  clear+reconnect; Done after reading; search works.
- **Truthfulness:** Grants still survive clear; no AI/network.

### Keyword empty-corpus honesty + search hang recovery

- **Requirements:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Distinct Nothing saved for search yet vs true no-match; live DB
  resolution; SearchCouldNotFinish instead of endless spinner.
- **Verification:** Unit tests; user confirmed search after clear/rebuild on
  Medium Phone (2026-07-27).
- **Truthfulness:** Interim keyword path only.

### Keyword search submitted-query coherence

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Editing the query clears stale Results/Why; results summary and
  no-match copy name the submitted query; superseded in-flight searches ignored.
- **Verification:** On 2026-07-26, search ViewModel/copy unit tests; debug APK
  installed. User confirmed on Medium Phone: Results/Why for "meet mira", then for
  "meet" without citing the prior query.
- **Truthfulness:** Interim keyword path only; no AI/network.

### Keyword Why document-label provenance

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Why this result? cites the same saved document label shown on the
  result card, plus query/page/excerpt; still keyword-not-meaning.
- **Verification:** On 2026-07-26, unit copy tests; debug APK installed. User
  confirmed on Medium Phone: Why cites each fixture PDF name for meet mira.
- **Truthfulness:** Interim keyword path; label from Room metadata only; no PDF reopen.

### Keyword recall match-count / cap honesty

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Results summary with listed match count; honest at-most-20
  disclosure when the search cap is reached; Why this result? unchanged.
- **Verification:** On 2026-07-26, unit copy tests; debug APK installed. User
  confirmed on Medium Phone: meet mira → Showing 2 matches for two fixture PDFs.
- **Truthfulness:** Interim keyword path only; no total-corpus count query; no AI.

### Local-AI benchmark plan (architecture gate)

- **Requirements:** A-01, A-02, A-05, A-06, A-07, E-06; Local AI Technical Spec §11.
- **Delivered:** `docs/LOCAL_AI_BENCHMARK_PLAN.md` (ADR-025); domain benchmark
  metric/claim contracts forbidding unmeasured release promises; unit tests.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed. No new
  AI/OCR/network Gradle deps.
- **Truthfulness:** No latency/battery/storage SLA accepted; measured pack
  baselines still required before AVAILABLE claims.

### Local-AI compatibility/fallback policy (architecture gate)

- **Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §11/§12/§13.4.
- **Delivered:** `docs/LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY.md` (ADR-024); domain
  support tiers/default unsupported resolver; forbidden silent semantic fallbacks;
  unit tests.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed. No new
  AI/OCR/network Gradle deps.
- **Truthfulness:** Defaults all intelligence capabilities to UNSUPPORTED; no
  AVAILABLE claim from discovery/keyword paths alone.

### AI Pack delivery/security plan (architecture gate)

- **Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §6/§13.3.
- **Delivered:** `docs/AI_PACK_DELIVERY_SECURITY_PLAN.md` (ADR-023); domain
  AiPackManifest / install state / verification contracts; UnavailableAiPackManager
  stub; unit validation tests. No download, INTERNET, models, or install UI.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed (including
  AiPackContractsTest). No new AI/OCR/network Gradle deps.
- **Truthfulness:** Does not claim packs can install or that understanding is ready;
  compatibility/fallback policy and Local-AI benchmarks still open for gate exit.

### Local-AI capability interfaces (architecture gate)

- **Requirements:** A-01, A-02, A-03; Local AI Technical Spec §4.
- **Delivered:** Domain CapabilityAvailability/Id types; Spec §4 VisionEngine,
  OcrEngine, DocumentEngine, EmbeddingEngine, MemoryBuilder, RecallRanker;
  truthful unavailable stubs; unit availability tests. No models, OCR SDKs,
  embeddings, network, or AI Pack downloads.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed. No new
  AI/OCR/network Gradle deps.
- **Truthfulness:** Does not claim on-device understanding is ready; A-01/A-07 and
  full Local-AI gate exit remain open (packs, fallback matrix, benchmarks).

### Keyword search Why this result? (stored evidence)

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-01, A-02, A-05.
- **Delivered:** Per-hit Why this result? on Find saved PDF text; cites query,
  page, and stored excerpt; Results phase retains normalized query; honest
  keyword-not-meaning copy.
- **Verification:** On 2026-07-25, unit copy tests; debug APK installed. User
  confirmed Why this result? cites page + excerpt for meet mira on Medium Phone.
- **Truthfulness:** Interim keyword path only; no AI, confidence, Memory Explain,
  or PDF reopen.

### WorkManager MediaStore discovery drain

- **Requirements:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** MediaStoreDiscoveryWorker drains IndexMediaStoreImages pages under
  unique work; ViewModel enqueues on Start indexing; honest full/selected metadata
  copy; battery-not-low constraint.
- **Verification:** On 2026-07-25, unit mapper/summary/ViewModel tests; Medium Phone
  MediaStoreDiscoveryWorkerAndroidTest **2 of 2**; debug APK installed. User
  confirmed on Medium Phone: metadata-only completed copy (0 items / up to date).
- **Truthfulness:** Metadata catalogue only; no image bytes, OCR, AI, or network.

### WorkManager SAF PDF extract drain

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** SafPdfExtractWorker drains pending PDFs (no current extraction)
  via broker + isolated parser + persist; Local PDF reading Start enqueues unique
  work; honest multi-PDF copy; password skip / access-stop outcomes.
- **Verification:** On 2026-07-25, unit mapper/copy tests; Medium Phone extract
  androidTest + pending-asset Room test; debug APK installed. User confirmed on
  Medium Phone: Completed multi-PDF saved-for-search copy.
- **Truthfulness:** Explicit consent only; ADR-017 isolation; no AI/network.

### WorkManager SAF PDF discovery drain

- **Requirements:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Hilt SafPdfDiscoveryWorker drains bounded IndexSafPdfFolder
  pages under unique work per source; ViewModel enqueues on Index; honest metadata
  progress copy; battery-not-low constraint.
- **Verification:** On 2026-07-25, unit mapper/summary/ViewModel tests; Medium Phone
  WorkManager androidTest **3 of 3**; debug APK installed. User confirmed on
  Medium Phone: indexed 2 PDF items + up-to-date list copy (Local PDF reading still
  required for text search).
- **Truthfulness:** Discovery placeholders/checkpoints only; no PDF bytes, extract
  WM, AI, or network.

### On-device keyword search over saved PDF page text

- **Requirements:** P-01, P-07, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** `SearchPersistedPdfPageText`; DAO join on current Asset fingerprint;
  Find saved PDF text screen with page + excerpt hits; honest keyword copy.
- **Verification:** On 2026-07-25, unit support/copy tests; Medium Phone keyword
  search androidTest **2 of 2**; debug APK installed. User confirmed query → page
  + excerpt hit on Medium Phone.
- **Truthfulness:** Keyword/substring only; no PDF reopen, WorkManager, AI, or network.

### Persist searchable PDF text from Local PDF reading Start

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Client retains validated wire results; `PersistValidatedPdfLocalReading`
  maps → prepare → Room; Start path persists complete/no-text extractions; Completed
  copy states text was saved for search on this phone.
- **Verification:** On 2026-07-25, unit local-reading tests passed; Medium Phone
  persist integration **1 of 1** and client isolation **10 of 10**; debug APK
  installed. User confirmed Start → Completed saved-for-search copy on Medium Phone.
- **Truthfulness:** No WorkManager, AI, network, or search-results UI.

### Wired Local PDF reading status UI (foreground, status-only)

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** `RunPdfLocalReadingStatusCheck`; ViewModel Start/Retry/Resume bind
  the isolated parser for one indexed PDF; session Completed state; honest copy;
  first-PDF asset lookup on `AssetRepository`.
- **Verification:** On 2026-07-25, unit local-reading **9**; Room asset lookup
  androidTest **3 of 3**; debug APK installed on Medium Phone. User confirmed
  Start → Completed on an indexed folder (1 PDF).
- **Truthfulness:** Page text still discarded; no searchable Room persist,
  WorkManager, AI, or network.

### Verified real-source descriptor path (fingerprint + live SAF open)

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** ApprovedPdfFingerprintRevalidationContract; SafPdfDocumentFingerprint;
  broker observeFingerprint before open; StaleSource through parse/assemble/eligibility;
  ParseApprovedPdfWithIsolatedParserRealSourceIntegrationTest (user-approved tree,
  fixture PDF, live open, status-only, delete fixture).
- **Verification:** On 2026-07-25, Medium Phone emulator 11 focused broker/synthetic/
  real-source tests; unit fingerprint revalidation 3 of 3.
- **Truthfulness:** Does not wire production UI to real parse, persist searchable
  extraction text, or add WorkManager/AI/network.

### Verified visible PDF local-reading recovery UI (presentation-only)

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** PdfLocalReadingCopy/session/ViewModel and Local PDF reading card on
  the connected folder screen (explain, progress, pause, retry, unavailable).
- **Verification:** On 2026-07-25, unit copy 2 of 2 and session 5 of 5.
- **Truthfulness:** Does not open PDFs, call the parser for user documents, write
  Room extraction text, or add WorkManager/AI/network.

### Verified Binder protocol v3 session/chunk streaming

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** IIsolatedPdfParser begin/nextChunk/cancel; session message codec;
  isolated service streaming; ordinary client assembly via session assembler with
  status-only result.
- **Verification:** On 2026-07-25, Medium Phone emulator 25 focused isolation/handoff/
  process-death tests passed.
- **Truthfulness:** Synthetic descriptors only; no real PDF, Room/UI wiring,
  WorkManager, AI, or network.

### Verified pure PDF parser session/chunk assembler

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Session streaming plan plus IsolatedPdfParserSessionAssembler; unit tests cover ordered assembly, terminal headers, cancel, and rejection without partial text. Binder protocol v3 not implemented.
- **Verification:** On 2026-07-25, local IsolatedPdfParserSessionAssemblerTest 8 of 8.
- **Truthfulness:** No AIDL/service change, real PDF, Room/UI wiring, WorkManager, AI, or network.

### Verified PDF extraction write-path resource budgets

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** PdfExtractionWriteBudgets provisional hard limits enforced on Room writes; content-free write-path benchmark buckets; over-budget page count fails safely with zero rows.
- **Verification:** On 2026-07-25, unit 3 of 3 and Medium Phone emulator 5 of 5.
- **Truthfulness:** Synthetic fixtures only; no real PDF / WorkManager / AI / network / production UI wiring. ADR-017 remains.

### Accepted ADR-022 and verified synthetic PDF extraction Room persistence

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** ADR-022 accepted (retain superseded extrated as non-current provenance). Room schema v4 adds pdf extraction tables with additive migration 3 to 4. RoomPdfExtractionPersistencePort proves atomic write, idempotency, dual-fingerprint retention, conflict fail-safe, and delete-all. Not wired into production discovery/UI.
- **Verification:** On 2026-07-25, Medium Phone emulator persistence + migration 5 of 5; opener 3 of 3 on schema v4.
- **Truthfulness:** No real PDF parse, WorkManager, AI, network, or searchable UI. Measured write limits and ADR-017 real-source gates remain.

### Proposed ADR-022 superseded PDF extraction retention

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** ADR-020 gate progress updated (ADR-021 encryption prerequisite
  satisfied). ADR-022 proposed: retain superseded PDF extractions as non-current
  provenance (recommended) versus delete-on-supersede. No Room content write enabled.
- **Verification:** Documentation and decision records only.
- **Truthfulness:** Historical proposal; ADR-022 later accepted. See entry above.

### Verified encrypted conversion performance budget

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Content-free conversion timing buckets and provisional ceilings for
  synthetic schema-v3 sizes (`ConversionElapsedBuckets`,
  `ConversionPerformanceBenchmarkIntegrationTest`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_BENCHMARK_PLAN.md`).
- **Verification:** On 2026-07-25, unit **1 of 1** and Medium Phone emulator **4 of
  4** under ceilings (largest measured ~7.3s for 5_000 assets).
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Emulator battery deltas are informational only.

### Verified device-unlock deferred open and recovery copy guards

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Credential-unlock gate defers encrypted open without creating a
  second database or mutating files; handle exposes waiting state; calm unlock UI;
  rebuild + unlock copy jargon guards.
- **Verification:** On 2026-07-25, unit **2 of 2** and Medium Phone emulator
  `DeviceUnlockDeferredOpenIntegrationTest` **2 of 2**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Performance/battery budget remains open.

### Verified physical-device arm64 encrypted conversion

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Rollout proof #6 on Samsung Galaxy A15 5G (`SM-A156E`,
  `arm64-v8a`): native SQLCipher load, encrypted create/reopen, wrong-passphrase
  denial, production-named conversion, and production opener conversion.
  `StandardSqliteDatabaseProbe` copies before probing so OEM SQLite cleanup cannot
  delete live encrypted DB files.
- **Verification:** On 2026-07-25, device instrumentation **13 of 13 passed**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.

### Verified low-storage / interruption conversion denial

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `ConversionStorageGuard` preflight before encrypted candidate create;
  denial and mid-conversion IO failure mark `FAILED_SAFE` /
  `CONVERSION_VALIDATION_FAILED`, keep plaintext intact, and open plaintext for the
  session until storage allows a successful retry (`MemoraEncryptedDatabaseOpener`).
- **Verification:** On 2026-07-24, Medium Phone emulator
  `ConversionLowStorageDenialIntegrationTest` **2 of 2 passed**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Physical-device arm64 proof later verified separately.

### Verified live conversion process-death resume

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Debug secondary process `:conv_live_death` arms conversion at
  `ROWS_COPIED` / `SWITCH_PENDING`; instrumentation induces real `am crash`/kill;
  ordinary process cold-opens and completes with fixture rows
  (`ConversionLiveProcessDeathIntegrationTest`).
- **Verification:** On 2026-07-24, Medium Phone emulator **2 of 2 passed**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Live-death helper is debug-source only (not in release).

### Corrected process-death proof status (honesty)

- **Requirements:** P-05, P-14, P-15, P-17; governance truthfulness.
- **Delivered:** Documentation and ADR-021 status corrected so simulated prepare-stop
  resume is not claimed as the rollout **live crash/kill** gate. Pre-work record for
  the live-kill slice added in
  `docs/CHANGE_CONTROL_LIVE_CONVERSION_PROCESS_DEATH.md`.
- **Verification:** Documentation cross-check only; no production behavior change.
- **Truthfulness:** Superseded for the live-kill gate by the verified live suite above.
  Low-storage denial and physical-device arm64 proofs later verified separately.
  PDF content persistence remains blocked.

### Verified simulated conversion process-death resume

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `MemoraEncryptedDatabaseOpener` resume for interrupted `ROWS_COPIED`
  and `SWITCH_PENDING`, including mid-finalize layouts (plaintext retained; candidate
  already promoted). Instrumentation simulates death via prepare-stop hooks then
  resumes with `open()`.
- **Verification:** On 2026-07-24, Medium Phone emulator
  `ConversionProcessDeathResumeIntegrationTest`: **4 of 4 passed**.
- **Limitation:** This is **simulated** interrupt/resume only. The rollout **live
  crash/kill** process-death gate remains open.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.

### Verified Clear Memora derived data and rebuild recovery UX

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `MemoraDatabaseHandle` + `ClearMemoraDerivedData` close, delete only
  Memora-owned DB/wrapper/journal/Keystore wrap state, and reopen a fresh encrypted
  empty index. Welcome **Clear Memora index** confirm flow uses ADR-021 rebuild copy
  only. Repositories resolve DAOs through the live handle after recreate.
- **Verification:** On 2026-07-24, Medium Phone emulator
  `ClearMemoraDerivedDataIntegrationTest` **1 of 1 passed**; unit
  `ClearMemoraDerivedDataCopyTest` passed. Persistable URI grants unchanged by clear.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Clear does not revoke Android folder permissions.

### Wired live encrypted PersistenceModule open and Open-source notices

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `PersistenceModule` opens `memora.db` via
  `MemoraEncryptedDatabaseOpener` (SQLCipher `SupportOpenHelperFactory`, Keystore
  passphrase wrap, conversion journal). Fresh encrypted create and plaintext
  copy-and-validate rename finalize. Welcome → **Open-source licenses** ships
  SQLCipher Community BSD notice text plus SQLite/LibTomCrypt notices.
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `MemoraEncryptedDatabaseOpenerIntegrationTest`: **3 of 3 passed**. TearDown
  clears production identity files.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was
  added. Recovery UX still uses approved rebuild wording only.

### Verified production-named disposable conversion

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Instrumentation conversion using disposable `memora.db` /
  `memora.db.encrypted_candidate` names, production Keystore alias/journal files,
  and rename-finalize onto `memora.db` after validated encrypted reopen. TearDown
  deletes disposable files so live PersistenceModule identity is not stranded.
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `ProductionNamedConversionIntegrationTest`: **3 of 3 passed**.
- **Truthfulness (at delivery):** Live `PersistenceModule` still opened plaintext
  `memora.db` at that gate; later superseded by the encrypted-open checkpoint above.

### Promoted SQLCipher to production classpath (Slice 1)

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Re-verified `net.zetetic:sqlcipher-android:4.17.0` AAR SHA-256 and
  OSV (empty advisories), then moved SQLCipher and `androidx.sqlite:2.6.2` to
  `implementation`. Provenance review updated for classpath promotion.
- **Verification:** On 2026-07-24, `:app:assembleDebug` succeeded; emulator
  `EncryptedDatabasePocIntegrationTest` **7/7** and
  `PlaintextToEncryptedConversionIntegrationTest` **5/5**.
- **Truthfulness:** `PersistenceModule` still opens plaintext `memora.db`. No
  encrypted open switch, PDF content write, notices UI, WorkManager, AI, or network
  path was added. BSD notices remain required before any release that ships the
  library.

### Accepted encrypted-database production conversion rollout plan

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` defines
  PersistenceModule switch acceptance criteria, crash-resume rules, BSD attribution
  gate, rollback, staged release checklist, and remaining device/process-death
  proofs.
- **Status:** Design gate only. Production `memora.db` remains plaintext; no
  SQLCipher promotion, Room conversion switch, PDF content write, UI, worker, AI, or
  network behavior changed at the documentation gate.
- **Verification:** Documentation cross-check against verified synthetic PoC and
  conversion harness results on 2026-07-24.

### Verified synthetic plaintext-to-encrypted conversion harness

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Conversion journal/phases in `data/security`, DAO `findAll`/`count`
  helpers, and androidTest-only `PlaintextToEncryptedConversionHarness` that
  copy-and-validates schema-v3 fixture rows between separately named PoC databases,
  retains plaintext until explicit finalize, and fails safe without destructive
  overwrite.
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `PlaintextToEncryptedConversionIntegrationTest`: **5 of 5 passed**.
- **Truthfulness:** Production `PersistenceModule` remains plaintext `memora.db`. No
  PDF content persistence, UI, WorkManager, AI, or network path was added.

### Verified synthetic encrypted-database PoC

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Keystore AES-GCM passphrase wrapper in `data/security`, androidTest-
  only SQLCipher Room opener for `memora_encrypted_poc.db`, and
  `EncryptedDatabasePocIntegrationTest` covering native load, create/reopen round-
  trip, wrong passphrase, tampered wrapper, read-only plaintext probe, clear-text
  marker absence, no-INTERNET permission, and unchanged production `memora.db` name.
- **Dependencies:** `androidTestImplementation` only —
  `net.zetetic:sqlcipher-android:4.17.0` and `androidx.sqlite:sqlite:2.6.2` (resolves
  cleanly with Room `2.8.4`).
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `EncryptedDatabasePocIntegrationTest`: **7 of 7 passed**.
- **Truthfulness:** Production `PersistenceModule` remains plaintext. No PDF content
  persistence, UI, WorkManager, AI, or network path was added.

### Accepted ADR-021 encrypted-database direction with provenance and PoC plan

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Product owner accepted ADR-021. Frictionless UX and recovery-copy
  rules are recorded. `docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` accepts
  Maven Central `net.zetetic:sqlcipher-android:4.17.0` for PoC use only, with
  recorded AAR hashes and a point-in-time empty OSV result.
  `docs/ENCRYPTED_DATABASE_POC_PLAN.md` defines the synthetic-only next code gate.
- **Status:** Direction accepted; production `memora.db` remains plaintext; no
  production SQLCipher binding, Room conversion, PDF content write, UI, worker, AI,
  or network behavior changed at the documentation gate.
- **Verification:** Documentation and supply-chain inspection only on 2026-07-24.

### Proposed encrypted-database decision record

- **Delivered:** Added the SQLCipher-for-Android plus Android-Keystore recommendation,
  option comparison, licensing/provenance gate, key lifecycle, recovery,
  non-destructive conversion, diagnostics, and test/release requirements.
- **Status:** Superseded by ADR-021 acceptance above. Historical proposal retained for
  traceability.

### Accepted local-data backup and transfer exclusion

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** The accepted ADR-020 privacy posture now excludes all Memora-private
  databases, preferences, files, external app data, and app-root data from legacy
  Android backup plus Android 12+ cloud backup and device-to-device transfer.
  `allowBackup=false` is included as defence in depth. The design record remains the
  gate for future PDF extraction persistence.
- **Reason:** Android documents that `allowBackup=false` alone cannot reliably block
  device-to-device transfer on every manufacturer, so explicit exclusions are used
  for both transfer paths.
- **Verification:** On 2026-07-24, `:app:assembleDebug :app:installDebug` succeeded
  on the Medium Phone emulator. Package flags confirmed backup is disabled; packaged
  resources confirmed exclusions for all five private-data domains on both legacy and
  Android 12+ cloud/device-transfer paths.
- **Truthfulness:** This adds no Room schema/migration/write, source access, parser
  transport, UI, worker, AI, network, search, or real-source PDF capability.

### Verified content-free PDF extraction persistence eligibility

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure application policy now permits only an identity/fingerprint/
  schema-matching complete or explicit no-text extraction record to become eligible
  for a future atomic persistence transaction. Eligible facts are content-free:
  identity, fingerprint, schema, page count, coverage, integrity, lifecycle, and
  retry directive. Partial, inconsistent, failed, access-blocked, source-unavailable,
  and retryable outcomes stay explicitly ineligible.
- **Verification:** On 2026-07-23, `PrepareApprovedPdfExtractionPersistenceTest`
  passed **8 of 8** local unit tests.
- **Truthfulness:** This adds no Room entity, migration, or write; no source access,
  descriptor or parser-text transport, UI, WorkManager, semantic understanding/AI,
  or network path. It does not enable a real user PDF.

### Verified atomic PDF extraction persistence port

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A domain-layer repository port now accepts only a typed request
  built from an `EligibleForAtomicWrite` decision and its matching extraction record.
  The factory rejects mismatched identity, fingerprint, schema, page count, and
  partial/wrong coverage before a repository can receive a request. The port has
  explicit persisted, retryable, stale-reindex, and safe-failure outcomes.
- **Verification:** On 2026-07-23, `PdfExtractionPersistencePortContractTest` passed
  **5 of 5** local unit tests; the prerequisite eligibility suite remained **8 of 8**.
- **Truthfulness:** No repository implementation, Room schema/migration/write, source
  access, descriptor or parser transport, UI, WorkManager, AI, or network behavior
  was added.

### Verified PDF extraction persistence coordinator

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure application coordinator invokes the future persistence port
  only for an eligible, matching record. It preserves all four explicit port outcomes
  and prevents ineligible, missing, or mismatched record input from reaching the port.
- **Verification:** On 2026-07-23, `PersistApprovedPdfExtractionTest` passed
  **7 of 7** local unit tests.
- **Truthfulness:** The test uses only a fake port. No repository implementation,
  Room schema/migration/write, source access, parser/text transport, UI, WorkManager,
  semantic understanding/AI, or network behavior was added.

### Verified approved-parser and in-memory extraction assembly

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A small application consistency gate now joins an approved parser
  status with an already-created in-memory extraction outcome. It calls the synthetic
  extraction provider only after a successful parser status, requires exact Asset
  identity/fingerprint/schema binding plus matching page count and coverage, and
  rejects an inconsistent record. Parser transport failures remain distinct from
  post-parser extraction failures.
- **Verification:** On 2026-07-23, `AssembleApprovedPdfExtractionTest` passed
  **6 of 6** local unit tests. `ParseApprovedPdfWithIsolatedParserIntegrationTest`
  also passed **3 of 3** on the Medium Phone emulator.
- **Truthfulness:** The assembly has no descriptor/source/text transport API and
  writes no Room data. Its provider is synthetic and in-memory; this does not prove
  that page text has crossed the private service boundary or enable real-source
  parsing, search, UI, WorkManager, AI, or network.

### Verified validated-parser-result to domain-extraction handoff

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A small, synthetic-only data-to-domain mapper now accepts only an
  already validated isolated-parser result. It deterministically rejoins ordered
  chunks into complete page records, preserves explicit no-extractable-text, and maps
  protected or failed parser outcomes to truthful domain failures. It creates only an
  in-memory `PdfExtractionRecord` bound to the supplied Asset identity/fingerprint.
- **Verification:** On 2026-07-23, `ValidatedIsolatedPdfResultToExtractionMapperTest`
  passed **4 of 4** local unit tests. The existing private-parser emulator regression
  also passed **20 of 20** focused Android tests on the Medium Phone.
- **Truthfulness:** This mapper does not decode Binder data, open a descriptor or
  source, write Room, start WorkManager, invoke understanding/AI, expose UI, search,
  or use network. It has no partial-result transport and cannot enable real-source
  parsing or persistence.

### Verified bounded synthetic isolated-parser result transport

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** The existing private isolated parser now uses protocol version 2 to
  return a strict, bounded page/chunk envelope for repository-owned synthetic
  descriptors. The ordinary-process client validates the exact envelope and then
  deliberately discards chunks, retaining its content-free status summary. The
  temporary synthetic limits are 32 pages, four chunks per page, 8,192 UTF-16
  code-units per page, 65,536 total, and 2,048 per chunk.
- **Verification:** On 2026-07-23, the Medium Phone emulator passed **20 focused
  tests** across the service, client, end-to-end transport, and approved-broker
  handoff integration suites.
- **Truthfulness:** This does not enable a user source, extraction persistence,
  search, UI, WorkManager, understanding/AI, or network. The single bounded
  synthetic response is not the future real-source session/chunk streaming protocol;
  its limits are not production budgets.

### Verified synthetic approved-PDF parser handoff

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** An unbound application coordinator now connects the approved SAF
  custody broker to the private isolated parser only through a dedicated descriptor
  ownership adapter. The broker retains and closes its borrowed duplicate; the adapter
  duplicates it before transferring ownership to the existing parser client, which
  closes its own handle. The debug-only fixture was upgraded to a valid one-page,
  repository-owned selectable-text PDF.
- **Verification:** On 2026-07-23,
  `ParseApprovedPdfWithIsolatedParserIntegrationTest` completed on the Medium Phone
  emulator: **3 tests passed**. It proves the synthetic approved path reaches the
  private parser, a grant revoked immediately before opening prevents parser
  submission, and a retryable parser status is not presented as extraction.
- **Truthfulness:** The result is content-free and not persisted or searchable. This
  does not open a real user PDF or add UI, Hilt, WorkManager, Room extraction,
  semantic understanding/AI, or network behavior.

### Verified synthetic SAF PDF descriptor broker

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** An unbound data/platform broker, Android tree-membership/read-only
  adapter, and debug-only synthetic DocumentsProvider fixture, excluded from release
  builds. The broker owns original/duplicate descriptor closure and has no UI, Hilt,
  Room, worker, parser, AI, or network binding. API 26-28 returns an explicit safe
  unsupported-platform result and opens nothing.
- **Verification:** On 2026-07-23,
  `SafPdfDescriptorBrokerIntegrationTest` completed on the Medium Phone emulator:
  **6 tests passed**.
- **Truthfulness:** The test uses only a pipe-backed repository fixture. It does not
  request or use a real persisted user grant, and it does not make a user PDF
  eligible for opening or parsing.

### Canonical SAF PDF target boundary

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A SAF data/platform factory now derives an internal future opening
  target from the approved tree plus opaque source document ID. It does not trust a
  stored location as opening authority and rejects non-PDF, source-mismatch, invalid
  tree, and foreign-provider cases before any platform I/O.
- **Verification:** On 2026-07-23, the user ran
  `SafPdfCanonicalDocumentTargetFactoryIntegrationTest` on the Medium Phone emulator:
  **5 tests passed**.
- **Truthfulness:** The test uses five synthetic URI-only cases. This component
  performs no grant validation, provider query, descriptor open, parser call, source
  read, persistence, UI, WorkManager, AI, or network operation.

### Approved Android PDF descriptor-broker design

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** The platform custody plan defines exact SAF approval/grant ordering,
  opaque document-ID handling, Android subtree membership checks, read-only descriptor
  ownership, duplicate transfer to the isolated parser, and a synthetic-fixture test
  matrix.
- **Truthfulness:** Documentation and review only. No descriptor-opening code, source
  read, PDF parse, UI, persistence, WorkManager job, AI capability, permission,
  dependency, or network path was added. Real user-source parsing remains disabled
  under ADR-017.

### Verified approved PDF descriptor-custody gate

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure domain contract now binds a future PDF extraction request to
  the exact approved source ID and a freshly observed source-access state. Its only
  successful output contains the immutable Asset identity and fingerprint; it cannot
  carry a URI, descriptor, stream, source content, or parser handle inward.
- **Verification:** On 2026-07-23, focused Gradle and the user's Android Studio run
  both reported `ApprovedPdfDescriptorCustodyContractTest`: **5 tests passed**.
  The tests cover authorization plus mismatch, access-required, access-revoked, and
  source-unavailable denials.
- **Truthfulness:** No Android source, document, descriptor, parser, Room state, UI,
  worker, AI capability, network, or dependency was accessed or changed. This does
  not enable real-source PDF parsing; ADR-017's platform descriptor-broker gates
  remain required.

### Frozen trust, identity, and quality rules

- **Requirements:** P-09–P-13, A-01–A-05, E-04–E-06.
- **Delivered:** ADR-019 and Experience Memory Amendment v1.1 establish stable
  Memory identity/revisions, “truth before intelligence,” explicit integrity states,
  user-facing `Why this result?`, evidence-support classes, and calibration/
  overconfident-error evaluation requirements.
- **Truthfulness:** Documentation only. The current one-Asset `Memory` code does not
  yet implement Memory IDs, revisions, persistence, state presentation, confidence,
  evaluation, or `Why this result?` UI.
- **Verification:** Cross-reference review completed against the governing product
  documents, current Memory contract, architecture, ADRs, roadmap, and traceability.

### Governed Experience Memory direction and behavioral boundary

- **Requirements:** P-01, P-02, P-09 through P-13, P-18; A-01 through A-05; future
  architecture IDs E-01 through E-03.
- **Delivered:** User-approved `EXPERIENCE_MEMORY_AMENDMENT_V1`, ADR-018, an updated
  Local-AI specification, product capability map, roadmap, and traceability entries.
  They establish Personal Knowledge Infrastructure as Memora's long-term direction:
  Asset Memories remain the MVP foundation; future Event and Knowledge Memories use
  evidence-backed links rather than destructive grouping.
- **Truthfulness:** This is governance and behavioral specification only. It adds no
  source access, AI dependency, model, event detection, timeline, WhatsApp/audio
  access, storage schema, UI, or background work. P-18 exclusions remain in force.
- **Verification:** Documentation cross-reference review against the product source
  registry, Local-AI specification, product contract, architecture, ADRs, roadmap,
  traceability matrix, and current one-Asset `Memory` domain contract.

### Verified fresh SAF read-grant validation boundary

- **Requirements:** P-03, P-05, P-07, P-14, P-15, P-17; Local AI principles A-01,
  A-02, A-06.
- **Delivered:** A source-neutral `DocumentTreeAccessValidator` now checks
  Android's persisted permission list for the exact user-approved document-tree URI
  and a retained read permission. The existing metadata-only SAF discovery adapter
  depends on this new boundary; the metadata catalog no longer owns authorization.
- **Verification:** Kotlin, unit-test, and Android-test compilation passed. The
  focused matcher result contains **3 tests passed** for exact-tree acceptance and
  different-tree/readless-grant rejection. On 2026-07-23, after explicitly
  reconnecting an emulator Documents folder, the user ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator:
  **1 test passed**.
- **Truthfulness:** The Android adapter reads only the retained grant list. It does
  not query a provider, open a user document or descriptor, parse a PDF, persist an
  extraction, call the isolated service, invoke AI, or access a network. This does
  not enable real-source parsing.

### Verified live isolated PDF parser-process death recovery

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A test-only harness binds the existing private isolated parser service,
  holds a synthetic pipe request open, identifies exactly one package-matching process
  with an isolated UID, then induces an Android `am crash <pid>` for that PID alone.
  It adds no production service behavior, permission, Binder method, or debug kill
  switch.
- **Verification:** On 2026-07-23, the user ran
  `LiveIsolatedPdfParserProcessDeathIntegrationTest` on the Medium Phone emulator:
  **1 test passed**. The ordinary process returned a retryable content-free failure,
  closed its supplied descriptor, and marked the Binder connection unavailable.
- **Truthfulness:** This proves a narrow synthetic live-death recovery path only. It
  does not authorize real-source access or prove grant validation, result transport,
  resource limits, persistence, reconnection scheduling, or recovery UI.

### Verified offline synthetic PDF parser runtime

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A synthetic-only Android test now confirms that the release app
  requests no `INTERNET` permission, then permits the existing local PDF parser to
  run only after Android reports the emulator has no Internet-capable or validated
  network. It obtains network-state visibility through a temporary test-shell identity
  and drops that identity before parsing; neither app manifest gains a network
  permission or a network client.
- **Verification:** On 2026-07-22, the user ran
  `PdfParserOfflineRuntimeIntegrationTest` on the offline Medium Phone emulator:
  **2 tests passed**. It parsed only a repository-owned fixture and accessed no user
  source, descriptor, URI, SAF tree, Room data, service, UI, WorkManager, AI, or
  network.
- **Truthfulness:** This closes only the deterministic parser's narrow offline runtime
  check. It does not authorize real-source parsing or prove future source access,
  isolated-service behavior, persistence, or semantic understanding offline.

### Verified many-page synthetic PDF parser baseline

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A test-only benchmark plan and Android harness measure aggregate
  input bytes, page count, extracted-text UTF-16 code units, future-result Bundle
  size, and parser elapsed-time range after a warm-up and five measured runs. The
  expanded corpus generates small, medium, larger, and 32-page repository-owned PDFs
  entirely in memory, then materializes input bytes before timing begins. It contains
  no user source access, source text logging, service binding, Room, UI, WorkManager,
  or AI.
- **Verification:** On 2026-07-22, the connected Medium Phone emulator ran
  `PdfParserSyntheticBenchmarkIntegrationTest`: 3 of 3 tests passed. The new
  32-page fixture produced 65,536 text code units; all measurements are recorded in
  `docs/PDF_PARSER_BENCHMARK_PLAN.md` and remain harness evidence only, not a
  production policy.

### Verified strict parser-result Bundle codec

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A future-only Android `Bundle` decoder accepts exactly the approved
  version-one keys and field types, rejects unexpected or missing fields, then feeds
  the existing bounded-result validator. It returns no candidate text when decoding
  or validation fails.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserResultBundleCodecIntegrationTest` on the Medium Phone emulator:
  10 of 10 tests passed. The live isolated service still returns status only and this
  change opens no descriptor or source.

### Verified bounded parser-result contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A pure validator now defines the future versioned page-text result
  shape. It requires complete page/chunk coverage and an injected, explicit limit for
  page count, chunks per page, page-text UTF-16 code units, and total-text UTF-16
  code units. Rejection exposes no candidate content; a later transport must map it
  to a retryable content-free outcome.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserResultContractTest` in Android Studio: 10 of 10 tests passed.
  The current Binder service still returns status only; this change opens no
  descriptor or source and enables no PDF content return.

### Verified isolated parser end-to-end transport

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** One Android test connects the existing private Binder adapter to the
  existing ordinary-process client, then passes only a repository-owned pipe
  descriptor through the isolated service. It asserts a validated status-only result
  and ordinary-process descriptor closure.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserEndToEndIntegrationTest` on the Medium Phone emulator: 1 of 1
  test passed. This does not enable SAF access, real-source parsing, text chunks,
  Room persistence, UI, or AI.

### Verified isolated-parser client cancellation contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic ordinary-process client accepts an Android
  `CancellationSignal`. Cancellation before submission prevents a parser call;
  cancellation while waiting cancels the client-side future and returns a
  content-free retryable result after descriptor closure.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 10 of 10
  tests passed. This does not claim to terminate a live isolated process or enable
  real PDF access.

### Verified private parser-service binding contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A private Android binding adapter can expose only the existing
  isolated parser Binder. It reports connecting, available, or retryable-unavailable
  status and accepts no descriptor, URI, path, source identity, or parser request.
- **Verification:** On 2026-07-22, the user ran
  `AndroidIsolatedPdfParserConnectionIntegrationTest` on the Medium Phone emulator:
  3 of 3 tests passed. It proves a live private binding, explicit bind failure, and
  explicit disconnection callback without parser work.

### Verified malformed parser-response handling

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic ordinary-process client test now covers unknown
  outcomes, a false isolated-process claim, missing page-count data, and an invalid
  page count. Each must become a content-free retryable failure and close its
  descriptor.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 8 of 8
  tests passed. This remains a transport-validation step only; it does not enable
  real PDF access, Binder page/text chunks, or UI.

### Verified ordinary-process parser recovery contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A private, synthetic-only client contract maps bind failure,
  simulated Binder death, bounded timeout, interruption, and malformed transport
  responses to content-free retryable results. It returns only a validated outcome,
  retryability, and optional page count, and closes every supplied descriptor.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 4 of 4
  tests passed. This does not enable real PDF access, a live service connection,
  real process-death testing, cancellation, chunking, offline checks, persistence,
  or visible recovery UI.

### PDF parser isolation guardrail

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Decision:** ADR-017 requires all future real-PDF parsing to run in a private
  Android isolated service. The normal app verifies one persisted SAF read grant and
  passes one read-only descriptor; the parser service receives neither URI/path nor
  broad source access.
- **Delivered:** The documented threat model is now represented by a private,
  non-exported `android:isolatedProcess` service and a fixed-version descriptor-only
  Binder interface. The service receives no URI, path, source identity, metadata,
  extracted text, Room access, Hilt graph, UI, or network permission. It currently
  accepts only a repository-owned synthetic descriptor and returns a small parser
  status summary. No SAF source access, UI, Room change, worker, AI capability, or
  real user document path has been added.
- **Verification:** Android-test APK compilation passed on 2026-07-21. The user ran
  `IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: 2 of 2
  tests passed. They prove the private/isolated manifest configuration and a two-page
  synthetic descriptor parse. Offline verification, no-text/password/malformed inputs,
  descriptor cleanup under every failure, service death, cancellation, timeout
  reporting, chunk validation, source access, persistence, and visible
  progress/recovery are still required before real-source enablement.

### Expanded isolated PDF parser safety cases

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic-only service test now covers no extractable text,
  password protection, malformed input, an unsupported protocol version, and
  client-side descriptor cleanup after every Binder call. The worker now also closes
  its received descriptor if submission or execution fails. Its result is still a
  tiny status summary only: no source text, title, metadata, URI, path, or identity.
- **Verification:** Android-test APK compilation passed on 2026-07-21. On
  2026-07-22, the user ran the expanded class on the Medium Phone emulator: 6 of 6
  tests passed. This does not verify service death, cancellation, timeout, output
  chunking, offline operation, actual source access, persistence, or UI recovery.

### Local PDF parser and fixture decision

- **Requirements:** P-05, P-07, P-14, P-15, P-17; scanned-PDF OCR remains a future
  Local AI capability.
- **Decision:** ADR-016 selects PDFBox-Android 2.0.27.0 as the local parser behind
  the existing domain port, subject to explicit licensing, dependency, supply-chain,
  fixture, isolated-process, resource-measurement, offline, and emulator gates. Its
  vulnerable declared Bouncy Castle 1.72 dependencies are explicitly overridden with
  the reviewed 1.84 set.
- **Delivered:** The data-layer mapper and generated synthetic-only Android test
  fixtures exist. No SAF URI, connected folder, Room record, UI, worker, or real user
  document is wired to the mapper.
- **Verification:** Dependency graph, SBOM, OSV review, third-party notices, debug
  APK build-size baseline (`12,423,988` to `18,734,630` bytes), and Android-test APK
  compilation are complete. On 2026-07-21, a failed initial emulator run exposed
  accidental leading patch markers in the repository-owned Base64 fixture payloads;
  all four corrected payloads then passed independent Base64 validation. The user
  reran `PdfBoxPdfDocumentMapperIntegrationTest` on the Medium Phone emulator: 4 of
  4 tests passed. Command-line Android tools still could not see that emulator.
- **Truthfulness:** Text-layer PDFs can become complete page-level extraction records;
  scanned/image-only PDFs remain `NoExtractableText` until a separately governed local
  OCR capability is delivered. This records the P-07 gap rather than hiding it.
- **Verification:** Governing-document/current-code review, official Android API and
  compatibility review, and Git diff check. No source content, app behavior, or test
  suite changed in this documentation-only decision.

### Deterministic PDF extraction contract

- **Requirements:** P-05, P-07, P-14, P-15; this step does not implement Local AI.
- **Decision:** ADR-015 defines a versioned record that ties deterministic PDF facts
  to the exact source identity, fingerprint, and extraction schema. It distinguishes
  complete, partial, and no-text-layer coverage rather than silently treating a PDF as
  fully extracted.
- **Delivered:** Pure Kotlin PDF request, record, coverage, and recoverable outcome
  contracts, plus an inward-facing platform-extractor boundary. No data/platform
  adapter exists yet.
- **Privacy:** No document is opened, copied, uploaded, persisted, changed, or
  deleted. No parser, model, cloud path, background work, or UI behavior is added.
- **Verification:** On 2026-07-21, local Gradle passed `PdfExtractionTest`: 6 of 6
  tests cover PDF-only input, source-version binding, complete-page coverage, partial
  coverage, no-text truthfulness, and recoverable failure construction.
  Android-facing verification is not required for this pure domain contract because
  the app's runtime behavior is unchanged.
- **Known limitation:** The local PDF parser, privacy-safe fixtures, Room persistence,
  platform adapter, and emulator test remain separate steps.

### Verified explicit PDF-folder indexing control

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-16, P-17. This step does not
  implement Local AI, extraction, or a cloud path.
- **Decision:** ADR-014 restores the most recently approved private folder reference
  into the setup screen but never scans it automatically. The user explicitly starts
  one bounded metadata page and explicitly chooses any later page.
- **Delivered:** A Hilt ViewModel owns restored connection, indexing, completed,
  retryable-failure, and access-recovery state. Compose only renders that immutable
  state and forwards actions; it does not access Room, SAF, or PDF content.
- **Verification:** Local ViewModel and presentation-copy tests passed on 2026-07-21.
  Android Hilt/test compilation passed. The user then launched the app on the Medium
  Phone emulator, restored the connected folder, explicitly selected `Index this
  folder`, and observed the truthful completed `0 PDF items` state.
- **Known limitation:** This interim screen activates the most recently connected
  folder. A future source-management experience must let people view/select all
  independently connected folders. PDF extraction and background scheduling remain
  intentionally out of scope.

### Verified SAF descendant traversal

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17; no Local-AI requirement is
  implemented by this step.
- **Decision:** ADR-013 replaces root-only traversal with a resumable depth-first
  checkpoint strategy. One invocation reads children from exactly one pending folder;
  it never recurses unboundedly in a single call.
- **Delivered:** The SAF adapter's v2 checkpoint stores pending folder frames and
  their source-owned cursors, discovers declared PDF metadata in descendant folders,
  and reads a prior v1 root checkpoint safely. An empty provider page that claims more
  data becomes an explicit retryable failure.
- **Privacy:** The implementation reads metadata only and changes no source content.
  It does not open a PDF, read bytes/text, copy a document, request broader access,
  start automatically, or schedule background work.
- **Verification:** Local `SafPdfDiscoverySourceTest` passed on 2026-07-20, including
  deterministic nested traversal and v1-resume coverage. Android-test compilation
  passed. The user then reran `SafPdfDiscoverySourceIntegrationTest` on the Medium
  Phone emulator: 1 of 1 test passed after reconnecting the approved folder.
- **Known limitation:** The emulator is not assumed to contain a nested PDF fixture,
  so its live test confirms platform access/regression while the nested logic remains
  deterministically covered locally. Background scheduling and PDF extraction remain
  future work.

### Verified SAF PDF metadata page persistence

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17; A-01 and A-02 remain
  unaffected because this has no model, cloud, or network path.
- **Delivered:** `IndexSafPdfFolder` accepts one exact private approved-folder
  source ID, constructs a source-neutral adapter through an injected factory, and
  delegates one bounded result to the existing atomic discovery-page persistence
  boundary. Its immutable outcome distinguishes unconnected source, required/revoked
  access, retryable failure, and success.
- **Architecture:** The application layer depends only on domain contracts. The
  Android SAF catalog/factory is Hilt-bound in the data layer; no composable is
  changed and no layer opens a PDF.
- **Verification:** Local `IndexSafPdfFolderTest` and Android-test compilation passed
  on 2026-07-20. The user then ran `IndexSafPdfFolderIntegrationTest` on the Medium
  Phone emulator: 1 of 1 test passed against the already approved folder. It read one
  bounded metadata page and wrote only its placeholders and checkpoint to an isolated
  in-memory Room database.
- **Known limitation:** This is one explicit bounded metadata page per invocation.
  Descendant traversal is now handled through its source-owned checkpoint (ADR-013),
  but PDF bytes/text extraction, background scheduling, and UI initiation remain
  deliberately out of scope.

### Local-first engineering governance checkpoint

- **Delivered:** Preserved immutable, versioned repository copies of the original
  PRD and both accepted addenda in `docs/product-source/`, with SHA-256 values in
  `docs/PRODUCT_SOURCE_REGISTRY.md`.
- **Decision:** Added ADR-012 and `docs/LOCAL_AI_TECHNICAL_SPEC.md`. Normal memory
  creation, retrieval, ranking, and explanation are now governed as local-first and
  offline after required on-device capability installation. Cloud AI is optional and
  cannot become a core dependency.
- **Process:** Strengthened the mandatory pre-work gate. Every meaningful delivery
  must use the product registry, local-AI specification, current-code inspection,
  traceability IDs, and `docs/CHANGE_CONTROL_TEMPLATE.md`; conversational memory is
  not an authority.
- **Scope:** Documentation and source-artifact checkpoint only. No Android code,
  dependencies, permissions, source access, model, network client, or user-visible
  behaviour changed.

### Verified bounded SAF PDF metadata discovery

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17.
- **Decision:** ADR-011 records the bounded immediate-child implementation boundary,
  the explicit persisted-grant check, and the required future descendant-traversal
  work. It is not a claim that all nested PDFs are already discoverable.
- **Delivered:** A read-only SAF platform catalog and source adapter now verify the
  exact retained Android read grant for each connected source, query one bounded page
  of immediate-child metadata, emit declared PDFs as source-neutral Asset
  placeholders, and produce a private source-owned checkpoint. Revocation and
  provider errors are explicit outcomes rather than an empty folder.
- **Privacy:** The adapter does not request a permission, open a document, read PDF
  bytes or text, copy source data, persist a discovery page, start automatically, or
  schedule background work.
- **Verification:** On 2026-07-20, local `SafPdfDiscoverySourceTest` passed: 5 of 5
  tests. Android-test compilation passed. The user then ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator: 1 of 1 test
  passed against the already approved folder.
- **Known limitation:** It currently discovers immediate children only; resumable
  nested-folder traversal, persistence, extraction, and background scheduling remain
  future work.

### Verified user-approved SAF PDF-folder connection

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-16, P-17.
- **Decision:** ADR-010 establishes each approved SAF document tree as an
  independently resumable source with a SHA-256-derived source ID. Room database
  version 3 stores the private URI reference and approval time required by a later
  platform adapter.
- **Delivered:** The privacy-explained Compose setup screen launches Android's
  `ACTION_OPEN_DOCUMENT_TREE` picker. It persists only the chosen tree's read grant,
  then asks a Hilt ViewModel and application use case to save the private reference.
  The UI never accesses Room directly.
- **Privacy:** This flow does not enumerate a tree, open a PDF, copy content, request
  broad storage access, or start background work. The raw tree URI remains private
  database data and does not appear in source IDs.
- **Verification:** On 2026-07-20, `SafDocumentTreeSourceTest` passed in Android
  Studio: 3 of 3 tests. `RoomDocumentTreeApprovalRepositoryTest` then passed on the
  Medium Phone emulator: 2 of 2 tests. `MemoraDatabaseMigrationTest` passed: 1 of 1
  test verified the original Asset fixture survives the version-3 migration.
  `DocumentTreeSetupViewModelTest` passed: 4 of 4 tests, and
  `ApproveDocumentTreeTest` passed: 1 of 1. The user then selected an emulator folder
  through Android's live picker and observed the truthful connected state.
- **Known limitation:** No document enumeration or PDF discovery exists yet. The
  connected folder remains inert until the later, bounded read-only SAF adapter.

### Verified Compose setup and explicit MediaStore indexing control

- **Requirements:** P-04, P-05, P-14, P-15, P-16, P-17.
- **Delivered:** The Compose setup screen now reports Android permission results to
  the Hilt ViewModel and exposes `Start indexing` only after confirmed access. One
  user-initiated request indexes one bounded metadata-only page, then renders a
  truthful full-library or selected-photo completion message, or a recoverable error.
- **Privacy:** The Activity owns Android's permission launcher. The UI has no direct
  source, Room, or AI calls. The workflow does not open image bytes, modify originals,
  upload content, or schedule background work.
- **Verification:** On 2026-07-20, `IndexingSummaryTest` passed in Android Studio:
  3 of 3 tests passed. The Kotlin, Hilt, Android-test compilation, and local unit-test
  graphs compiled successfully. The user then verified the visible app flow on the
  Medium Phone emulator: it completed with 0 permitted items, a valid empty result.
- **Known limitation:** Android 17 (API 37.1) currently fails Compose
  instrumentation tests before test assertions due to the emulator/test bridge
  expecting the unavailable `InputManager.getInstance` method. This is recorded in
  `CONTINUE.md`; presentation copy is locally tested and the visible flow is manually
  verified until compatible tooling is available.

### Verified setup/indexing ViewModel state boundary

- **Requirements:** P-05, P-14, P-16, P-17.
- **Delivered:** A Hilt ViewModel with immutable state for photo access and one
  explicit indexing request. The ViewModel receives a permission result from the UI;
  it does not request Android permission. It blocks indexing without confirmed access,
  prevents concurrent duplicate requests, preserves selected-photo scope, and exposes
  recovery-safe access or failure outcomes.
- **Dependency note:** Added the official AndroidX Lifecycle ViewModel KTX runtime for
  `viewModelScope`, plus Kotlin coroutines test support for deterministic local state
  tests. Neither dependency adds a permission, network access, source scan, or user
  data collection.
- **Verification:** On 2026-07-20, `MediaStoreSetupViewModelTest` passed in Android
  Studio: 4 of 4 tests passed. The Hilt and Android-test compilation graph passed.
- **Known limitation:** This ViewModel starts one foreground, user-initiated page
  only. Background scheduling and extraction remain future work.

### Verified live MediaStore-to-Room indexing path

- **Requirements:** P-04, P-05, P-14, P-15, P-17.
- **Delivered:** An Android emulator integration test that executes one explicit,
  bounded `MediaStore -> discovery use case -> Room` path using a temporary in-memory
  database. It verifies the returned page's Asset placeholders and opaque checkpoint
  are saved together and that the reported access scope agrees with Android's live
  grant.
- **Privacy:** The test reads only the emulator's existing MediaStore metadata. It
  opens no image bytes, inserts no media, creates no thumbnail, changes no original,
  and leaves no derived records in the normal Memora database.
- **Verification:** On 2026-07-20,
  `IndexMediaStoreImagesIntegrationTest` passed in Android Studio on the Medium Phone
  emulator: 1 of 1 test passed after photo access was granted.
- **Known limitation:** The verified UI starts a foreground page only; background
  work remains intentionally unimplemented.

### Verified controlled MediaStore indexing boundary

- **Requirements:** P-04, P-05, P-14, P-15, P-17.
- **Delivered:** A Hilt-bound application use case that invokes the existing
  checkpoint-driven discovery flow for exactly one explicit, bounded MediaStore page.
  It returns an immutable outcome for a later ViewModel, including the truthful
  distinction between full-library and selected-photo access.
- **Privacy:** This boundary neither requests nor caches a permission, starts itself,
  schedules background work, opens image bytes, creates thumbnails, modifies original
  media, or changes the prototype UI. Without a future explicit caller it is inert.
- **Verification:** On 2026-07-19, `IndexMediaStoreImagesTest` passed in Android
  Studio: 5 of 5 tests passed. The Hilt application and Android-test graphs also
  compiled successfully.
- **Known limitation:** This use case has no user-facing ViewModel or UI caller yet.

### Verified checkpoint-driven discovery invocation

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application use case that checks a source's access state, loads
  only that source's saved opaque checkpoint, requests exactly one bounded page, then
  passes the outcome through the verified result coordinator. It rejects pages that
  claim a different source identity.
- **Privacy:** The use case is inactive until called explicitly, requests no Android
  permission, starts no background work, opens no original content, and has no UI.
- **Verification:** On 2026-07-19, `DiscoverSourcePageTest` passed in Android Studio:
  6 of 6 tests passed.
- **Known limitation:** The real MediaStore adapter is still not bound to this use
  case, so no actual source result is persisted by the running app.

### Verified discovery-result coordination

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application coordinator that sends only a successful discovery
  page to the atomic page-store use case. Missing access, revoked access, and source
  failures remain unchanged and cause no write. A storage exception becomes a
  recoverable, non-sensitive failure outcome.
- **Privacy:** The coordinator opens no source, requests no permission, starts no
  scan, and changes no UI. It does not expose internal storage errors to a user.
- **Verification:** On 2026-07-19, `ProcessDiscoveryResultTest` passed in Android
  Studio: 3 of 3 tests passed.
- **Known limitation:** No source calls this coordinator yet; no actual MediaStore
  discovery page is persisted by the running app.

### Verified atomic discovery-page persistence

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application `PersistDiscoveryPage` use case and a Room-backed
  atomic store. For every successful source-neutral page, it writes all Asset
  placeholders and that page's opaque source checkpoint in one database transaction.
  Unchanged versions retain their current indexing state; changed fingerprints are
  safely returned to `DISCOVERED`.
- **Privacy:** This component receives only source-neutral Asset metadata and opaque
  cursors. It opens no original content, requests no permission, performs no scan,
  starts no background work, and changes no UI.
- **Verification:** On 2026-07-19, local Gradle passed
  `PersistDiscoveryPageTest`: 1 of 1. `RoomDiscoveryPageStoreTest` then passed in
  Android Studio on the Medium Phone emulator: 3 of 3 tests passed.
- **Known limitation:** No platform source invokes this boundary yet; therefore no
  actual MediaStore result is persisted by the app, and restart behavior has not yet
  been verified end-to-end.

### Verified durable discovery checkpoints

- **Requirements:** P-04, P-14, P-17.
- **Delivered:** Room database version 2, a source-owned opaque checkpoint table,
  repository, Hilt binding, exported schema, and explicit migration from version 1.
- **Privacy:** A checkpoint contains only its source ID, opaque source cursor, and
  save time. It contains no original media, thumbnail, text, or semantic memory.
- **Verification:** On 2026-07-19, `RoomDiscoveryCheckpointRepositoryTest` passed in
  Android Studio on the Medium Phone emulator: 2 of 2 tests passed. The real
  `MemoraDatabaseMigrationTest` then passed: 1 of 1 test preserved a version-1 Asset
  across the migration.
- **Known limitation:** This repository is intentionally not yet connected to
  MediaStore discovery. The later atomic page-store boundary is tested, but no
  platform source invokes it yet.

### Verified MediaStore image/screenshot discovery adapter

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17.
- **Delivered:** A bounded, read-only `ContentResolver` query adapter for image and
  screenshot metadata; an opaque MediaStore version/watermark/ID checkpoint; and
  runtime access-state handling for full, selected, missing, and revoked access.
- **Privacy:** The adapter never opens image bytes, creates thumbnails, writes to
  MediaStore, requests location, persists scan results, or starts automatically.
- **Verification:** On 2026-07-19, local Gradle compiled the app and passed 5 of 5
  selected mapper/checkpoint unit tests. `MediaStoreImageDiscoverySourceTest` then
  passed in Android Studio on the Medium Phone emulator: 1 of 1 test passed after
  photo access was granted, proving a real read-only catalogue query.
- **Known limitation:** The adapter is intentionally not injected into a worker or
  UI and no cursor/Asset page is stored in Room yet.

### MediaStore adapter design approved

- **Requirements:** P-03, P-04, P-05, P-15, P-17.
- **Decision:** Query only Android-granted image metadata, distinguish full and
  selected-photo access, and use version/generation checkpoints (ADR-009).
- **Privacy:** No media location, image bytes, thumbnail, write operation, automatic
  scan, or user-content test fixture is permitted in this step.

### Verified incremental checkpoint correction

- **Requirements:** P-04, P-14.
- **Decision:** Completed and empty discovery pages retain a durable source checkpoint
  for the next incremental pass (ADR-008).
- **Scope:** Contract correction and unit tests only. No device media or permission is
  accessed.
- **Verification:** On 2026-07-18, `AssetDiscoverySourceTest` passed in Android
  Studio: 3 of 3 tests passed after the correction.

### Verified source discovery contract

- **Requirements:** P-03, P-04, P-05, P-14, P-15.
- **Decision:** Discovery returns bounded, source-specific Asset pages and explicit
  access/failure outcomes (ADR-008).
- **Scope:** Pure Kotlin contract and unit tests only. No device media, document,
  provider, permission, worker, or UI is accessed or changed.
- **Verification:** On 2026-07-18, `AssetDiscoverySourceTest` passed in Android
  Studio: 3 of 3 tests passed. The emulator remained healthy during the run.

### Verified Memory evidence contract

- **Requirements:** P-09, P-10, P-11, P-12, P-13.
- **Decision:** A Memory is versioned by its Asset and every summary/anchor cites
  stored evidence (ADR-007).
- **Scope:** Pure Kotlin domain model and unit tests only. No source is opened, no AI
  is called, no derived content is persisted, and no visible behavior is changed.
- **Verification:** On 2026-07-18, `:app:testDebugUnitTest` passed, including the
  Memory contract tests. The unchanged welcome screen then launched successfully on
  the Medium Phone emulator.

### Verified dependency-injection foundation

- **Requirement:** P-17.
- **Decision:** Hilt 2.60.1 owns application composition (ADR-006), using the existing
  Android legacy KAPT compatibility bridge. Java 17 is configured as required by the
  current official Hilt/Compose setup guidance.
- **Scope:** Application, Room database/DAO/repository bindings, and the Android
  entry point only. No source access, indexing work, permission, network behavior,
  or visible UI behavior is added.
- **Verification:** On 2026-07-18, `:app:testDebugUnitTest` completed successfully;
  Hilt's generated tasks compiled the application graph. The existing automated test
  task was current. The unchanged welcome screen then launched successfully on the
  Medium Phone emulator.

### Verified persistence boundary

- **Requirements:** P-04, P-05, P-09, P-14, P-17.
- **Decision:** Room 2.8.4 with exported schemas; Android legacy KAPT is used for
  Room generation under the current AGP built-in Kotlin toolchain (ADR-005).
- **Verification status:** Initial KSP configuration was rejected by AGP before
  compilation. The documented compatibility path uses legacy KAPT. The Room schema
  path is supplied explicitly to legacy KAPT because the Room Gradle plugin does not
  propagate it through this compatibility bridge.
- **Verification:** `:app:testDebugUnitTest` passed with the generated Room schema.
  On 2026-07-18, `RoomAssetRepositoryTest` passed on the Medium Phone emulator:
  2 of 2 tests passed. The tests verify record round-trip persistence and idempotent
  upsert behavior for a stable source identity.

### Source-neutral domain foundation

- **Requirements:** P-03, P-04, P-05, P-09, P-14.
- **Layers:** Domain only; no UI, Android platform API, data persistence, network, or
  AI dependency was added.
- **Delivered:** Asset types, immutable source identity/location/fingerprint values,
  read-only source capability contract, and validated recoverable indexing lifecycle.
- **Verification:** `:app:testDebugUnitTest` passed on 2026-07-17. Tests cover identity
  validation, source capability safety, valid indexing flow, retry behavior, and
  rejected invalid transitions.
- **Known limitation:** This does not discover, open, persist, or search real assets.
  The next phase is a Room persistence boundary; automatic existing-note access remains
  blocked by ADR-003.
