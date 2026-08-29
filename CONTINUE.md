# Continue Here

## Current checkpoint

**Project:** UNFYND App (Android surface in this monorepo)  
**Project folder path (deferred; ADR-040):** `MemoraApp/` — not the product name  
**Updated:** 2026-08-30

**Docs note:** Root `README.md` is site-aligned product/App vs Core voice
([unfynd.com](https://www.unfynd.com/)); engineering status stays here only.

**Checkpoint (MIG-07 note keyword cutover):** Note Find ViewModel
binds `SearchMemoryEvidence` with `AssetType.NOTE`;
`SearchPersistedNotePageText` + `NotePageKeywordSearchSupport` deleted;
readiness counts READY note Memory evidence. **L4 Retired**; Live/Dual
**N = 2** (L7, L8). PDF + screenshot + photo cutovers remain delivered
(**L1–L3 Retired**). **MIG-07 keyword L1–L4 cutovers complete** — this is
**not** full MIG-07 / Recall DONE. Canonical Recall still **not** a live
single App API (**ADR-049**). L7/L8, MIG-07B, shared ranking, and
`RECALL_CONVERGENCE_DONE` boxes remain open. Per **ADR-050:** claim **A**
(PDF evidence-embedding delivery slice steps 1–4) **COMPLETE**; claim **B**
(Migration Spec MIG-05 full) remains **open** — non-PDF evidence indexer
**deferred**. **MIG-04** MemoryBuilder seam remains live. Ranking,
Find/Why UI redesign, Links, Event/Knowledge, Grounded Answers code, package
rename, VisionEngine, and MIG-07B are not started. Hashed
freeze/spec/amendment files unchanged. ADR-043 unchanged (Act still out).
ADR-044 unchanged (interpretation only).
`ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION` remains 5 (conversion
journal, not Room). Identity playbook steps 0–5 remain complete.

See `CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER`. Photo cutover:
`CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER`. Screenshot cutover:
`CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER`. PDF cutover:
`CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER`. MIG-06 step 1 additive use case
remains the shared substrate (`CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE`).
MIG-05 step 4 retirement remains delivered
(`CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE`).

**Docs note (ADR-044):** Low-power product behavior = event-driven Memory
lifecycle (interpretation only; cites Spec §7 / §9). No code.

**Docs note (ADR-045 / ADR-046):** Living product language for the substrate is
**UNFYND Core — on-device memory and intelligence infrastructure** (ADR-046
refines ADR-045 wording). Do not use “Personal Knowledge Infrastructure,”
“Personal Intelligence Infrastructure,” or third-party grant metaphors in new
living-canon lines. PKI is a prior internal noun where hashed docs / ADR-043
still say it; ADR-043 substance stands. No MIG-* / Grounded Answers code / Act
from those ADRs alone.

**Docs note (ADR-047):** Class A authorized — curated UNFYND Core Public
Specification / Contract pack under Apache-2.0 (essential open, inspectable
element of Core). **Pack files landed** under `public/unfynd-core/` (Apache-2.0
distillation; follow-up a). **Root README App vs Core rewrite landed**
(follow-up b; site-aligned). **Class A pack published** at
https://github.com/CzarPiratez/unfynd-core (follow-up c; Apache-2.0 root
LICENSE/NOTICE/README/SPEC/ROADMAP-OPEN/SECURITY/CITATIONS only). **Pack copy
fix:** App vs Core softened; Android-as-product / “reference application”
framing removed from public pack; commercial licensing called out as separate
from Apache contracts (no fake license limits). LICENSE remains Apache-2.0.
**Pack enrichment (2026-08-29):** `SPEC-STATUS.md`, synthetic `examples/`
(ADR-048), PUBLIC_CHANGELOG / CONTRIBUTING / GOVERNANCE, README revision stamp
and inspect/build-on/run honesty. Synced pack-only to public unfynd-core.
**Class A status refreshed (2026-08-30):** SPEC-STATUS aligned to
Memory-evidence Find convergence + M4 midrange measurement (contracts
unchanged; AVAILABLE / Canonical Recall as live API still not claimed);
synced pack-only to public unfynd-core.
UNFYND App and proprietary assets stay private until a later ADR. No MIG-* /
Freeze reopen / Act from ADR-047 / ADR-048.

**Docs note (ADR-048):** Synthetic, non-personal Class A reference samples under
the same Apache-2.0 license as the pack are authorized. Not a schema lock; no
App/fixture dump; no App open / Class B / Act from this ADR alone.

**Docs note (ADR-049):** Canonical Recall naming accepted — sole App
product-facing retrieval boundary after MIG-07 cutover (not yet one API in
code). MIG-06 `SearchMemoryEvidence` = keyword/literal **candidate-generation**;
MIG-07 PDF + screenshot + photo + note cutovers wire those Finds (L1–L4
Retired). Grounding Retriever = Option C same pipeline (not a competing
Find). Does not authorize MIG-07B from ADR-049 alone; does not change
MIG-05 step 4 / ADR-050 acceptance.

**Docs note (ADR-050):** MIG-05 claim levels — **A** PDF delivery slice
(steps 1–4) COMPLETE for eng checkpoints; **B** Spec MIG-05 full STILL OPEN
(non-PDF evidence indexing deferred). Grants/public must not say “MIG-05
complete” without stating B open. Does not authorize the non-PDF indexer.
Does not rewrite hashed Spec/Freeze. MIG-07 note cutover is separately
authorized in GOVERNANCE /
`CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER`.

**Legacy recall surface:** Live/Dual = **2** (see `docs/LEGACY_RECALL_SURFACE.md`;
audit: `docs/ESCAPE_HATCH_AUDIT.md` — MIG-07 note checkpoint; L4 Retired).

**Status truth check 2026-08-30:** CONTINUE + LEGACY + code agree; N=2
(L7, L8 Live; L1–L6 Retired; Room 15; evidence-only index;
`SearchAssetMemoriesByMeaning` on `MemoryEvidenceEmbeddingStore`; PDF +
screenshot + photo + note keyword Finds on `SearchMemoryEvidence`; ADR-049
Canonical Recall not yet one API; full MIG-07 / Recall DONE **open** —
keyword L1–L4 cutovers complete ≠ program DONE).

**Docs note (Recall enforcement program Steps 1–7):** Landed (docs only).
**Operator entry:** `docs/RECALL_ENFORCEMENT_INDEX.md` (60s checklist) +
LEGACY Live/Dual **N=2**. Cursor rule:
`.cursor/rules/unfynd-architecture-invariants.mdc` (alwaysApply).
Change-control convergence block + `docs/LEGACY_EXTENSION_EXCEPTION.md`.
**MIG-07B still requires separate authorization.** **Machine check:**
`scripts/check-legacy-recall-surface.sh` + CI job `Legacy recall surface guard`
(see RECALL_ENFORCEMENT_INDEX; MIG-07 PDF + screenshot + photo + note allowlist).

**Docs note (Recall / MIG-05 DONE checklists):** Program-exit definitions only.
Recall program exit: `docs/RECALL_CONVERGENCE_DONE.md` (still open — L7/L8,
Canonical Recall facade, MIG-07B, shared ranking). MIG-05 FULL DONE checklist
in `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md` — per **ADR-050**,
claim **A** (PDF slice) may be treated complete; claim **B** / Spec full and
the non-PDF box remain **open**. MIG-07 keyword L1–L4 cutovers alone do
**not** satisfy Recall convergence DONE.

**Docs note (Canonical Recall result + Why contract):** DRAFT logical shared
hit/Why shape for MIG-06/07 — `docs/CANONICAL_RECALL_RESULT_CONTRACT.md`.
MIG-06 step 1 hit type carries Memory/evidence/asset provenance fields toward
that draft; Canonical Recall API itself is **not** implemented.
### Where we are (honest)

UNFYND runs on the Medium Phone emulator as a **local-first memory retrieval**
app: permissioned discovery, deterministic extract, Room persistence, keyword
Find, evidence-backed Asset Memories, and a **candidate** Find-by-meaning path.

| Area | Status |
|---|---|
| Photos / screenshots / PDF folders | Discover → extract → save facts (bounded, read-only) |
| Keyword Find (PDF / OCR / photo text) | Shipped; Why evidence; open original / page where built |
| Asset Memory build | Shipped (≤25 READY facts per tap); evidence uncapped per MIG-02 |
| Evidence class (MIG-01) | On Asset Memory evidence; all current assembly is DIRECT; not used for ranking/UI/links. Links still not implemented. Room 12→13 **device-verified** (`MemoraDatabaseMigrationTest` 2/2 on Medium Phone emulator). |
| Evidence completeness (MIG-02) | No fixed 8-item cap; per-item 8192-char pathological guard; summary ≤240 display-only |
| Typed anchors (MIG-03) | TIME from EXIF date-taken; TOPIC from `pdf:title` / `note:title`; TEXT always; no fabricated PERSON/PLACE/OBJECT/ACTIVITY/PURPOSE; schema `asset-memory-facts-v4` |
| MemoryBuilder contract (MIG-04) | `assemble` seam live; `DeterministicMemoryBuilder` Available; observations empty; drain → use case → MemoryBuilder; no VisionEngine |
| Evidence embeddings (MIG-05 step 4) | PdfPageEmbedding* retired (Room 15); evidence-only index writer; search on evidence store; **ADR-050 A** PDF slice complete; **B** Spec full open (non-PDF deferred) |
| Unified evidence keyword search (MIG-06/07 L1–L4) | `SearchMemoryEvidence` live for PDF + screenshot + photo + note Finds; L1–L4 Retired; L7/L8 still Live |
| On-device meaning model | **USE** (Universal Sentence Encoder); disclosure + download |
| Find by meaning | Candidate recall; evidence-level embeddings (MIG-05 step 3+); summary embeddings remain; E5d token assist; PdfPageEmbedding* dual-write **gone** (step 4) |
| Meaning index UX | ≤25 memories/tap + live progress + remaining-tap honesty |
| PDF local reading UX | Aggregate N of M progress during WorkManager drain |
| Measured quality | M1–M3 closed (emulator); **M4 midrange execute** closed (Galaxy A15 `midrange_arm64`: USE cosine **2/3** / boosted **3/3**) |
| Marketing AVAILABLE / SLA | **Not claimed** — M4 measured; product AVAILABLE decision still open |
| Notes (OneNote etc.) | OneNote connector N0–N7 accepted (keyword Find; not all phone notes) |
| **Grounded Answers architecture** | **Docs accepted** (ADR-033–039); **no generative code** |
| CI (`main`) | Green after E5d boost fixture fix (`970fcf9`) |

**Authoritative backlog (retrieval):**  
`docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md`

**Registered freeze / sequencing (ADR-041; conflicts decided by ADR-042):**  
`docs/ARCHITECTURE_FREEZE_v1.0.md`  
`docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md`

**Authoritative constitution (grounded answers; not unseated by Freeze §1):**  
`docs/GROUNDING_ARCHITECTURE.md`  
`docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`  
`docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`

**Next eng default (parallel workstreams):**

1. **MIG-07B / Canonical Recall wiring (when authorized)** — fold L8
   meaning Find + L7 local ranking into Canonical Recall; shared result/Why.
   Keyword L1–L4 cutovers **done**. Recall program exit gate:
   `docs/RECALL_CONVERGENCE_DONE.md` (still open; keyword cutovers ≠
   program complete).
2. **MIG-05 remaining / deferral (ADR-050 claim B)** — non-PDF evidence
   indexer when authorized, or accept PDF-only vectors via a future ADR.
   Do not claim Spec MIG-05 / claim **B** complete while non-PDF remains
   deferred (claim **A** PDF slice is already complete). Full MIG-05 Spec
   close gate: `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE` retirement
   checklist (open — non-PDF deferred).
3. **Retrieval:** **M4 execute landed** (2026-08-29). Product AVAILABLE
   decision + UI copy review remain open — do **not** flip AVAILABLE from
   measurement alone. Keep E5d disclosed assist.
4. **Grounded Answers:** **no ReasoningEngine / Ask UI code** until readiness
   gates in `GROUNDING_ARCHITECTURE.md` §14 pass (model lifecycle for chosen
   reasoner, eval corpus, M4 measured or explicit device policy, adversarial
   pass).

Experience Memory Amendment §10 is hashed and was not edited; evidence-class
and completeness status for this checkpoint are recorded here and in
`docs/CHANGELOG.md`.

### Early build diary (July 2026)

The long chronological notes below record how foundation slices landed
(discovery, SAF, PDFBox, OCR, memories). Prefer the table above for “what is
true now.” Do not treat early “no AI / no Memory persisted” lines as current.

## Read in this order

1. `AGENTS.md`
2. `docs/PRODUCT_SOURCE_REGISTRY.md` (ADR-040 identity; ADR-046 UNFYND Core noun; ADR-047 Class A contracts; ADR-048 synthetic Class A samples; ADR-043 vision vs freeze; hashed architecture artifacts)
3. `docs/GOVERNANCE.md` (ADR-042 governing order; ADR-043 Act remains out; aligned with Freeze §2)
4. `docs/PRODUCT_CONTRACT.md`
5. `docs/LOCAL_AI_TECHNICAL_SPEC.md`
6. `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` (UNFYND Core / on-device memory and intelligence infrastructure; Asset Memory freeze; hashed text may still say PKI)
7. `docs/ARCHITECTURE_FREEZE_v1.0.md` (freeze/change-control for that core; §1 exclusivity scoped by ADR-042)
8. `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` (sequencing only; MIG-04 is this delivery; do not start MIG-05+)
9. `docs/GROUNDING_ARCHITECTURE.md` (sole Grounded Answers constitution)
10. `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`
11. `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`
12. `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md` and `docs/DECISIONS.md` (ADR-040 / ADR-041 / ADR-042 / ADR-043 / ADR-045 / ADR-046 / ADR-047)
13. `docs/ARCHITECTURE.md`
14. `docs/ROADMAP.md`
15. `docs/PRD_TRACEABILITY.md`

No implementation begins until this read gate is complete and the next change is
mapped to its requirements, acceptance criteria, risks, and verification plan.

## Frozen product-direction concepts

ADR-018 and `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` now freeze the following
direction: offline-first; Asset Memories; evidence-first construction; Memory
Builder; Explain/Trust; a future Memory Graph; and future Event/Knowledge Memories.
The immediate work remains the reliable Asset-Memory MVP. Do not redesign this
direction casually. Define and test behavior before future correlation, graph, or AI
implementation, and do not represent future capabilities as already shipped.

ADR-019 strengthens this freeze with stable Memory identity/revisions, the “truth
before intelligence” rule, explicit integrity states, user-facing `Why this result?`,
evidence-strength classes, and false-confidence evaluation. Treat these as binding
acceptance criteria for all future Memory, recall, explanation, and AI work.

**ADR-033–039 and `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`** freeze Grounded Answers:
retrieval-first; Evidence Package → ReasoningEngine → Verifier → StructuredAnswer;
Find remains source of truth; no chatbot; PDF saved-text first slice; **generative
implementation blocked** until `docs/GROUNDING_ARCHITECTURE.md` readiness gates pass.
Do not wire a ReasoningEngine or Ask UI before those gates close.

**ADR-043** confirms the vision against the freeze (Android = reference shell;
search ≠ product definition; Act out; vision ladder is direction only).
**ADR-045** retires PKI / PII living nouns; **ADR-046** sets living product
language to **UNFYND Core — on-device memory and intelligence infrastructure**.
**ADR-047** authorizes Class A curated Core contracts under Apache-2.0; pack
implementation and public publish remain follow-ups. PKI is a prior internal
noun where older text still uses it. Act / agentic Personal AI remains out of
current architecture until a later ADR and product-contract change. Do not
skip Asset-Memory quality to jump to Links, Events, Grounded Answers, or
conversation UI. Optional map: `docs/UNFYND_VISION_ALIGNMENT.md` (not hashed;
not architectural authority).

## Last verified behavior

- Gradle sync completed in Android Studio.
- The emulator starts and the app installs.
- `:app:testDebugUnitTest` passed after the source-neutral domain foundation was added.
- `:app:testDebugUnitTest` passed after adding Room persistence and exported schema
  version 1.
- `RoomAssetRepositoryTest` passed on the Medium Phone Android emulator on
  2026-07-18: 2 of 2 tests passed. It verifies record round-trip persistence and
  idempotent update behavior for a stable source identity.
- Hilt's generated dependency graph compiled successfully with
  `:app:testDebugUnitTest` on 2026-07-18. The existing automated test task was
  current, and the unchanged welcome screen then launched successfully on the Medium
  Phone emulator.
- The Memory/evidence domain tests passed with `:app:testDebugUnitTest` on
  2026-07-18. The unchanged welcome screen launched successfully afterward on the
  Medium Phone emulator.
- `AssetDiscoverySourceTest` passed in Android Studio on 2026-07-18: 3 of 3 tests
  passed. It verifies bounded discovery, source-owned cursors, and rejection of mixed
  sources or duplicate identities. The emulator remained healthy during the run.
- The corrected `AssetDiscoverySourceTest` passed in Android Studio on 2026-07-18:
  3 of 3 tests passed. Completed and empty pages now retain a durable checkpoint for
  the next incremental pass.
- On 2026-07-19, the local Gradle task passed for the MediaStore metadata mapper and
  checkpoint codec: 5 of 5 selected unit tests passed. The adapter is metadata-only,
  bounded, and inactive.
- `MediaStoreImageDiscoverySourceTest` passed in Android Studio on the Medium Phone
  emulator on 2026-07-19: 1 of 1 test passed after the user granted photo access. It
  verifies a real, read-only bounded MediaStore query; it does not persist or index
  any result.
- `RoomDiscoveryCheckpointRepositoryTest` passed in Android Studio on the Medium
  Phone emulator on 2026-07-19: 2 of 2 tests passed. It verifies source-owned cursor
  round-trip storage and idempotent replacement.
- `MemoraDatabaseMigrationTest` passed in Android Studio on the Medium Phone emulator
  on 2026-07-19: 1 of 1 test passed. It verifies an existing version-1 Asset record
  survives the non-destructive migration to database version 2.
- On 2026-07-19, the local `PersistDiscoveryPageTest` passed: 1 of 1 test verified
  that the application use case delegates the complete source-neutral page to its
  atomic storage boundary.
- `RoomDiscoveryPageStoreTest` passed in Android Studio on the Medium Phone emulator
  on 2026-07-19: 3 of 3 tests passed. It verifies that a page saves its placeholders
  and checkpoint together, unchanged asset versions retain their current indexing
  state, changed versions return to `DISCOVERED`, and an empty completed page advances
  its checkpoint.
- `ProcessDiscoveryResultTest` passed in Android Studio on 2026-07-19: 3 of 3 tests
  passed. It verifies that a successful page is committed, explicit access and source
  failure outcomes are returned without a write, and a persistence exception becomes
  a safe retryable failure.
- `DiscoverSourcePageTest` passed in Android Studio on 2026-07-19: 6 of 6 tests
  passed. It verifies saved-cursor resumption, first-pass discovery without a cursor,
  explicit access handling, source failure handling, exception recovery, and rejection
  of a page from an unexpected source.
- `IndexMediaStoreImagesTest` passed in Android Studio on 2026-07-19: 5 of 5 tests
  passed. It verifies one bounded, explicitly requested page is persisted, full and
  selected-photo access remain distinct, missing access performs no query or write,
  and revoked/failure outcomes remain safe for a later ViewModel.
- `IndexMediaStoreImagesIntegrationTest` passed in Android Studio on the Medium Phone
  emulator on 2026-07-20: 1 of 1 test passed after photo access was granted. It
  verifies a real bounded MediaStore page and its source-owned checkpoint persist
  atomically into an isolated in-memory Room database, without opening or altering
  original media.
- `MediaStoreSetupViewModelTest` passed in Android Studio on 2026-07-20: 4 of 4
  tests passed. It verifies indexing is blocked until both an explicit permission
  result and an explicit user request, selected-photo access stays distinct, recovery
  states remain explicit, and duplicate requests are ignored while work is active.
- `IndexingSummaryTest` passed in Android Studio on 2026-07-20: 3 of 3 tests passed.
  It verifies the completion message remains truthful for full-library access,
  selected-photo access, an incomplete page, and an empty completed page.
- On 2026-07-20, the user ran the app on the Medium Phone emulator, granted access,
  explicitly started indexing, and observed the completed `0 items` full-library
  message. This was expected for the emulator's permitted catalogue.
- Android 17 (API 37.1) currently fails Compose instrumentation tests before their
  assertions because its test environment lacks `InputManager.getInstance`. The
  failure is in the emulator/test-tool bridge, not Memora. The blocked Compose test
  was removed; pure presentation copy is locally tested, and the visible flow is
  manually verified on the emulator. Revisit when the emulator image or Compose test
  tooling is compatible.
- `SafDocumentTreeSourceTest` passed in Android Studio on 2026-07-20: 3 of 3 tests
  passed. It verifies stable, distinct hashed source identities and keeps the raw
  tree URI out of the source ID.
- `RoomDocumentTreeApprovalRepositoryTest` passed on the Medium Phone emulator on
  2026-07-20: 2 of 2 tests passed. It verifies temporary in-memory persistence of
  independent document-tree approval records without accessing a real tree.
- `MemoraDatabaseMigrationTest` passed on the Medium Phone emulator on 2026-07-20:
  1 of 1 test passed after Room database version 3 added the document-tree approval
  table without destructively resetting the version-1 Asset fixture.
- `DocumentTreeSetupViewModelTest` passed in Android Studio on 2026-07-20: 4 of 4
  tests passed. It verifies Android's persisted read grant is the only path that can
  save a folder reference, and platform or persistence failures remain recoverable.
- `ApproveDocumentTreeTest` passed in Android Studio on 2026-07-20: 1 of 1 test
  passed. It verifies the application boundary saves one private approval only after
  it receives a persisted tree URI.
- On 2026-07-20 the user opened the live Android folder picker, selected an emulator
  folder, and observed `PDF folder connected. Memora has not opened or indexed any
  document yet.`
- `SafPdfDiscoverySourceTest` passed locally on 2026-07-20: 5 of 5 tests verify
  fresh-grant revocation, bounded metadata pages, PDF-only filtering, source-owned
  checkpoint rejection, and retryable provider failure handling.
- Android-test compilation passed on 2026-07-20 after the SAF adapter was added.
- `SafPdfDiscoverySourceIntegrationTest` passed in Android Studio on the Medium Phone
  emulator on 2026-07-20: 1 of 1 test queried the existing user-approved folder's
  bounded metadata page. It did not open, modify, copy, extract, or index any PDF.
- `IndexSafPdfFolderTest` passed locally on 2026-07-20. Android-test compilation also
  passed. The user then ran `IndexSafPdfFolderIntegrationTest` on the Medium Phone
  emulator: 1 of 1 test passed. It verifies one real, bounded, read-only SAF PDF
  metadata page persists its placeholders and source-owned checkpoint atomically into
  an isolated in-memory Room database, without opening, copying, extracting, changing,
  or deleting a PDF.
- `SafPdfDiscoverySourceTest` passed locally on 2026-07-20 after descendant traversal
  was added. It verifies depth-first nested-folder resumption, bounded folder pages,
  v1 root-checkpoint compatibility, access revocation, and safe failure behavior.
  The user then reran `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone
  emulator: 1 of 1 test passed after reconnecting the approved folder. It confirms
  the changed adapter remains metadata-only under a live persisted Android grant.
- On 2026-07-21, local Gradle ran `PdfExtractionTest`: 6 of 6 tests passed. It
  verifies PDF-only requests, asset/fingerprint/schema binding, complete coverage,
  partial coverage, no-text-layer truthfulness, and recoverable failure construction.
  The app has no changed Android-facing behavior in this domain-only step.

## Next approved engineering step

The synthetic isolated-service smoke test is verified. On 2026-07-21, the user ran
`IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: **2 tests
passed**. It bound the private, non-exported `android:isolatedProcess` service and
parsed a repository-owned two-page PDF sent through a pipe, with no URI, path, SAF
folder, real user PDF, Room record, or UI action.

The expanded isolated-service test is verified. On 2026-07-22, the user ran
`IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: **6 tests
passed**. It verifies the private isolated manifest, selectable-text success,
image-only/no-text truthfulness, password-protected output, malformed-input
retryability, unsupported-protocol rejection, and caller-side descriptor cleanup after
each request. It has no real PDF, folder, URI, source identity, Room, UI, WorkManager,
or AI dependency.

## Next approved engineering step

The synthetic cancellation suite is verified. On 2026-07-22, the user ran
`IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: **10 tests
passed**. It proves that an already-cancelled request never reaches the parser and
that cancelling while the ordinary process waits returns a content-free retryable
failure and closes the descriptor. It does not claim to hard-kill the isolated
service, and it opens no real source.

## Next approved engineering step

The synthetic end-to-end transport test is verified. On 2026-07-22, the user ran
`IsolatedPdfParserEndToEndIntegrationTest` on the Medium Phone emulator: **1 test
passed**. It explicitly bound `AndroidIsolatedPdfParserConnection`, gave
`IsolatedPdfParserClient` only a repository-owned pipe descriptor, and verified the
private service returned a validated status-only result with caller-side descriptor
closure. It used no SAF URI, user PDF, path, source identity, Room, UI, WorkManager,
or AI.

## Next approved engineering step

The pure bounded-result contract is verified. On 2026-07-22, the user ran
`IsolatedPdfParserResultContractTest` in Android Studio: **10 tests passed**. It
verifies the future page-text response contract: known schema version, complete page
coverage, final chunk sequence, injected maximum page/chunk/page-text/total-text
limits, no-text truthfulness, and content-free rejection of malformed data. It uses
only synthetic strings and has no Binder service change, PDF descriptor, SAF URI,
user source, Room, UI, WorkManager, or AI.

## Next approved engineering step

The strict future result codec is verified. On 2026-07-22, the user ran
`IsolatedPdfParserResultBundleCodecIntegrationTest` on the Medium Phone emulator:
**10 tests passed**. It translates exactly-versioned synthetic `Bundle` fields into
the already-verified pure result contract, rejecting unknown, missing, malformed, and
over-limit data without exposing candidate text. It does not change the isolated
service, open a descriptor, access a SAF URI or user source, persist Room data, alter
UI, schedule WorkManager, or invoke AI.

## Next approved engineering step

The synthetic-only benchmark plan and Android harness are verified. On 2026-07-22,
`PdfParserSyntheticBenchmarkIntegrationTest` completed on the connected Medium Phone
emulator: **2 tests passed**. It measured repository fixtures after one warm-up and
five runs, logging only page count, text code-unit count, serialized future-result
size, and parser elapsed time. The initial values are recorded in
`docs/PDF_PARSER_BENCHMARK_PLAN.md`. It logged no text and opened no descriptor, URI,
SAF or user source. It did not alter the service protocol, source access, Room, UI,
WorkManager, or AI. This is a measurement baseline, not a production limit or
user-facing performance promise. A live in-flight parser-process-death check,
offline check, fresh-grant verification, source access, persistence, and user-visible
recovery remain separate ADR-017 gates.

## Next approved engineering step

The expanded synthetic PDF corpus is verified. On 2026-07-22, the user ran
`PdfParserSyntheticBenchmarkIntegrationTest` on the Medium Phone emulator: **3 tests
passed**. The corpus deterministically generated only three repository-owned PDFs in
memory: one 1,024-code-unit page, four 2,048-code-unit pages, and eight
4,096-code-unit pages. The benchmark materialized every fixture before timing parser
work, emitted only aggregate input-byte/page/text/result-size/elapsed-time metrics,
and verified the expected growth relationship. Recorded measurements are in
`docs/PDF_PARSER_BENCHMARK_PLAN.md`. It did not bind or change the isolated service,
open a descriptor/URI/SAF or user source, persist Room data, alter UI, schedule
WorkManager, or invoke AI. These test shapes are not production limits and do not
close ADR-017's memory, battery, thermal, offline, live-process-death, fresh-grant,
persistence, or recovery gates.

## Next approved engineering step

The many-page corpus addition is verified. On 2026-07-22, the user reran
`PdfParserSyntheticBenchmarkIntegrationTest` on the Medium Phone emulator: **3 tests
passed**. It now includes a repository-owned, in-memory 32-page PDF with 2,048
deterministic ASCII code-units per page. Its parser result contained 65,536 text
code-units, and its measured aggregate values are recorded in
`docs/PDF_PARSER_BENCHMARK_PLAN.md`. It did not open a user PDF, bind or change the
isolated service, use a descriptor/URI/SAF source, persist Room data, alter UI,
schedule WorkManager, invoke AI, use network, or claim a production limit. The
remaining blank/no-text, password, malformed, cancellation, timeout, device-health,
and offline benchmark evidence remains separately required.

## Next approved engineering step

The synthetic-only offline verification harness is verified. On 2026-07-22, the user
ran `PdfParserOfflineRuntimeIntegrationTest` after disabling network access on the
Medium Phone emulator: **2 tests passed**. It confirmed that the installed release
app requests no `INTERNET` permission and that the existing deterministic parser runs
only after Android reports no Internet-capable or validated network. The test
temporarily adopted and dropped Android's test-shell `ACCESS_NETWORK_STATE` identity
solely to inspect that condition; both manifests remain without
`ACCESS_NETWORK_STATE` or `INTERNET`. It opened only a repository-owned synthetic PDF
and did not bind or change the isolated service, use a descriptor/URI/SAF source,
persist Room data, alter UI, schedule WorkManager, invoke AI, or make a network
request. This verifies only the narrow parser offline-runtime gate.

## Next approved engineering step

The live isolated-parser process-death harness is verified. On 2026-07-23, the user
ran `LiveIsolatedPdfParserProcessDeathIntegrationTest` on the Medium Phone emulator:
**1 test passed**. It retained a test-only synthetic pipe request in flight, refused
to proceed until Android exposed exactly one package-matching process with an isolated
UID, and used test-shell `am crash <pid>` for that PID alone. The ordinary process
returned a retryable content-free failure, closed its descriptor, and marked the
connection unavailable. It changed no production Binder method, service behavior,
app permission, or user source.

## Next approved engineering step

The source-neutral fresh SAF read-grant boundary is verified. On 2026-07-23, the
user reran `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator
after explicitly reconnecting an emulator Documents folder: **1 test passed**. The
new `DocumentTreeAccessValidator` checks only Android's persisted grant list for
the exact private tree reference and retained read permission. The matcher's 3
focused local tests and Kotlin/unit-test/Android-test compilation passed. The gate
does not query a provider, open a document or descriptor, parse a PDF, persist an
extraction, call the service, invoke AI, or access a network.

## Next approved engineering step

The source-neutral **approved-PDF descriptor custody contract** is verified. On
2026-07-23, `ApprovedPdfDescriptorCustodyContractTest` passed in Android Studio:
**5 tests passed**. The pure domain gate binds the future request Asset to the exact
approved source and a just-observed read-grant state, returns only immutable identity
and fingerprint facts when authorized, and denies mismatch, required, revoked, and
unavailable access without retaining a URI, descriptor, stream, or content. It opens
no URI, calls no parser, reads no user PDF, persists no extraction, and does not make
real-source parsing eligible.

## Next approved engineering step

The Android platform custody boundary is now specified in
`docs/PDF_PLATFORM_DESCRIPTOR_BROKER_PLAN.md`. It makes the broker use the approved
tree plus opaque source document ID rather than a raw stored location, requires two
fresh retained-grant checks, uses Android subtree membership validation, permits only
one `"r"` descriptor followed by one duplicate to the private parser client, and
defines closure/failure tests. This is a design and acceptance plan only: no
descriptor-opening code exists and no document has been opened.

## Next approved engineering step

Implement the broker only against a repository-owned synthetic Android
DocumentsProvider fixture in the debug source set. The first code change must test
exact grant sequencing, tree-derived target construction, read-only opening,
descriptor ownership/closure, and safe denial paths. It must not touch user PDFs,
UI, WorkManager, Room extraction persistence, semantic understanding, AI, or the
network.

The first canonical-target component is verified. On 2026-07-23, the user ran
`SafPdfCanonicalDocumentTargetFactoryIntegrationTest` on the Medium Phone emulator:
**5 tests passed**. It accepts a PDF only from the exact approved source, derives its
internal Android target from the approved tree plus opaque source document ID, and
rejects raw foreign locations without opening them. The test has five synthetic-only
cases and does not include a provider fixture, retained-grant sequencing, descriptor
opening, parser invocation, persistence, or visible app behavior.

The synthetic descriptor broker is verified. On 2026-07-23,
`SafPdfDescriptorBrokerIntegrationTest` completed on the Medium Phone emulator:
**6 tests passed**. It uses a debug-only (release-excluded) provider with one
pipe-backed repository fixture. The broker verifies exact approval, rechecks the
grant immediately before opening, verifies API 29+ tree membership, permits only a
read-only descriptor, duplicates and owns cleanup of the consumer descriptor, and
denies every tested unsafe path without opening. It has no Hilt, UI, user source,
account, parser, Room persistence, worker, AI, or network path.

## Next approved engineering step

The synthetic broker-to-parser handoff is verified. On 2026-07-23,
`ParseApprovedPdfWithIsolatedParserIntegrationTest` completed on the Medium Phone
emulator: **3 tests passed**. The debug-only fixture is a valid repository-owned,
one-page selectable-text PDF. The unbound coordinator first applies exact approval,
fresh-grant, canonical-target, subtree-membership, and read-only descriptor custody;
an ownership adapter then duplicates the broker-borrowed descriptor before the
existing isolated parser client takes and closes its transferred handle. The test also
proves last-moment grant revocation reaches no parser and that retryable parser status
cannot become an extraction result. It has no real folder or user PDF, UI,
WorkManager, Room extraction persistence, semantic understanding/AI, or network
path; its status-only result is not searchable.

## Verified engineering checkpoint

The synthetic parser-result transport is verified. On 2026-07-23, the Medium Phone
emulator completed **20 focused tests** across
`IsolatedPdfParserServiceIntegrationTest`,
`IsolatedPdfParserClientIntegrationTest`,
`IsolatedPdfParserEndToEndIntegrationTest`, and
`ParseApprovedPdfWithIsolatedParserIntegrationTest`. Protocol version 2 of the
private isolated service returns a strict bounded page/chunk envelope for
repository-owned synthetic descriptors. It validates complete page coverage; the
ordinary-process client validates the envelope and then discards every chunk, keeping
only its content-free status result. The temporary synthetic limits are 32 pages,
four chunks per page, 8,192 UTF-16 code-units per page, 65,536 total, and 2,048 per
chunk.

This does not enable a user source, source identity, Room extraction record, search,
UI behavior, WorkManager, semantic understanding/AI, or network. It is not the
required real-source session/chunk streaming protocol, and its limits are not
production budgets. ADR-017 still blocks real-user PDF parsing.

## Verified engineering checkpoint

The synthetic typed parser-to-domain handoff is verified. On 2026-07-23,
`ValidatedIsolatedPdfResultToExtractionMapperTest` passed **4 of 4** local unit
tests. It accepts only an already validated bounded parser result and creates the
existing in-memory `PdfExtractionOutcome`: complete chunks are sorted/rejoined per
page, no-text remains explicit, and protected/failed outcomes remain content-free
domain failures. The supplied Asset identity/fingerprint and schema version bind any
created record. The Medium Phone emulator also reran the related private-parser
regression: **20 of 20** Android tests passed.

This mapper does not decode Binder data, open a descriptor or source, create partial
coverage, invent metadata/title/text, write Room, expose UI/search, schedule
WorkManager, invoke semantic understanding/AI, use network, or enable a real user
PDF. It remains a synthetic-only proof of the next inward architecture boundary.

## Verified engineering checkpoint

The synthetic approved-parser and extraction assembly is verified. On 2026-07-23,
`AssembleApprovedPdfExtractionTest` passed **6 of 6** local unit tests. It invokes a
parser-status port before an in-memory extraction provider, calls the provider only
after successful parser status, and accepts a record only when Asset identity,
fingerprint, schema, page count, and coverage agree. Transport failure, protected
status, access state, cancellation, extraction failure, and inconsistent record all
remain separate outcomes. The existing approved synthetic descriptor handoff also
passed **3 of 3** emulator tests on the Medium Phone after the port refactor.

The provider is synthetic and in-memory. This does not establish a live page-text
transfer from the private service, source opening, persistence, retrieval, UI,
WorkManager, semantic understanding/AI, or network operation. It cannot enable real
user PDFs and does not weaken ADR-017.

## Next approved engineering step

The pure content-free extraction persistence contract is verified. On 2026-07-23,
`PrepareApprovedPdfExtractionPersistenceTest` passed **8 of 8** local unit tests. It
accepts a request and the already assembled in-memory PDF extraction outcome, then
permits only a matching complete or explicit no-text record to become eligible for a
future atomic write. Its facts contain only identity, fingerprint, schema, positive
page count, coverage, verified integrity, lifecycle, and retry direction. Partial,
inconsistent, mismatched, failed, access-blocked, unavailable, and retryable outcomes
are ineligible with a truthful recovery state. No Room entity/migration/write, source
access, text transport, UI, worker, semantic understanding/AI, or network behavior
was added.

The pure atomic persistence-port contract is also verified. On 2026-07-23,
`PdfExtractionPersistencePortContractTest` passed **5 of 5** local unit tests, and
the prerequisite eligibility suite remained **8 of 8**. The domain port accepts only
a private-constructor request created from `EligibleForAtomicWrite` plus a matching
record; it rechecks identity, fingerprint, schema, page count, and complete/no-text
coverage. Its only future outcomes are persisted, retryable, stale-reindex, or safe
failure. No repository implementation, Room schema/migration/write, source access,
text transport, UI, worker, semantic understanding/AI, or network behavior was added.

The pure application persistence coordinator is verified. On 2026-07-23,
`PersistApprovedPdfExtractionTest` passed **7 of 7** local unit tests. It invokes the
future port only for a valid eligible decision and matching record, maps every
persisted/retryable/stale/safe-failure port result explicitly, and proves ineligible,
missing, and mismatched input never calls the port. The tests use a fake port only;
no repository implementation, Room schema/migration/write, source access, parser
transport, UI, worker, semantic understanding/AI, or network behavior was added.

## Verified engineering checkpoint

The user accepted ADR-020's privacy posture. The backup/device-transfer foundation
is verified: the debug app assembled and installed on the Medium Phone, and its
installed manifest plus packaged resources confirm that all Memora-private data is
excluded from legacy backup and Android 12+ cloud/device-transfer paths.

The detailed encrypted-database decision record was prepared in
`docs/ENCRYPTED_DATABASE_DECISION.md`. ADR-021 was then proposed for approval with
SQLCipher for Android, a randomly generated database passphrase wrapped by a
versioned Android Keystore AES-GCM key, dependency licensing/provenance, key
lifecycle, non-destructive conversion, device-unlock behavior, diagnostics, and
test/release gates.

## Verified engineering checkpoint

The product-owner approval gate for ADR-021 is complete. The earlier ask-to-approve
step is closed by the accepted checkpoint below; it no longer blocks work.

## Verified engineering checkpoint

ADR-021 is accepted. On 2026-07-24 the product owner approved the SQLCipher-for-
Android plus Android-Keystore direction with frictionless ordinary use and the
approved recovery wording that never mentions SQLCipher, keys, or encryption
failures. `docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` records Maven Central
candidate `net.zetetic:sqlcipher-android:4.17.0`, AAR hashes, the declared
`androidx.sqlite:sqlite:2.6.2` runtime dependency, BSD-style attribution obligations,
and a point-in-time OSV query with no listed advisories for that version.
`docs/ENCRYPTED_DATABASE_POC_PLAN.md` defines the synthetic-only next code gate.
Production `PersistenceModule` still opens plaintext `memora.db`. No production
SQLCipher binding, database conversion, PDF content write, UI, WorkManager, AI, or
network path was added at that documentation gate.

## Verified engineering checkpoint

The synthetic encrypted-database PoC is verified. On 2026-07-24,
`EncryptedDatabasePocIntegrationTest` completed on the Medium Phone emulator:
**7 tests passed**. It loads native SQLCipher, creates `memora_encrypted_poc.db`
with a Keystore-wrapped random passphrase, round-trips synthetic schema-v3 Asset/
checkpoint/document-tree rows across close/reopen, denies wrong-passphrase and
tampered-wrapper access without deleting the database, rejects a read-only standard
SQLite probe, finds no fixture marker in clear-text artifacts, and confirms the app
still requests no `INTERNET` permission while production `PersistenceModule` still
targets plaintext `memora.db`. SQLCipher is `androidTestImplementation` only.
Resolved test classpath versions: `net.zetetic:sqlcipher-android:4.17.0`,
`androidx.sqlite:sqlite:2.6.2`, `androidx.room:room-runtime:2.8.4`.

## Verified engineering checkpoint

The synthetic plaintext-to-encrypted conversion harness is verified. On 2026-07-24,
`PlaintextToEncryptedConversionIntegrationTest` completed on the Medium Phone
emulator: **5 tests passed**. It copies schema-v3 Asset/checkpoint/document-tree
fixture rows from `memora_plaintext_conversion_poc.db` into
`memora_encrypted_conversion_poc.db`, validates counts and row equality, proves
encrypted reopen while retaining plaintext at `SWITCH_PENDING`, deletes plaintext
only after explicit finalize, converts an empty database, keeps plaintext and drops
a bad candidate on validation failure, and retries safely from an interrupted
`ROWS_COPIED` journal state. Production `PersistenceModule` remains plaintext
`memora.db`. No PDF content persistence, UI, WorkManager, AI, or network path was
added.

## Verified engineering checkpoint

The production conversion rollout acceptance document is in place. On 2026-07-24,
`docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` recorded PersistenceModule switch
criteria, crash-resume rules, BSD attribution gate, rollback procedure, staged
release checklist, and remaining physical-device/process-death/notices proofs.
No production database opening, SQLCipher promotion, PDF content write, UI,
WorkManager, AI, or network path was changed.

## Verified engineering checkpoint

SQLCipher production classpath promotion (Slice 1) is verified. On 2026-07-24,
Maven Central AAR SHA-256 still matched
`44fc40c33d1de597c8339072a71fa0ff20e12d01ab352d6abe4ad5df668ead94`, OSV returned
no advisories for `net.zetetic:sqlcipher-android:4.17.0`, and the coordinate was
moved to `implementation` with `androidx.sqlite:sqlite:2.6.2`.
`:app:assembleDebug` succeeded. Emulator re-ran
`EncryptedDatabasePocIntegrationTest` (**7/7**) and
`PlaintextToEncryptedConversionIntegrationTest` (**5/5**). Production
`PersistenceModule` still opens plaintext `memora.db` without
`openHelperFactory`. BSD notices remain a release gate; PDF content persistence
remains blocked.

## Verified engineering checkpoint

Production-named disposable conversion is verified. On 2026-07-24,
`ProductionNamedConversionIntegrationTest` completed on the Medium Phone emulator:
**3 tests passed**. It converts schema-v3 fixtures from disposable `memora.db` into
`memora.db.encrypted_candidate`, renames the encrypted file onto `memora.db` only
after validated reopen, rejects standard SQLite against the production name, keeps
plaintext on validation failure, and deletes disposable files in tearDown so live
`PersistenceModule` is not left on an encrypted file. No PersistenceModule encrypted
open switch, PDF content write, notices UI, WorkManager, AI, or network path was
added.

## Verified engineering checkpoint

Live encrypted `PersistenceModule` open and Open-source notices are verified. On
2026-07-24, production startup opens `memora.db` through
`MemoraEncryptedDatabaseOpener` (SQLCipher + Keystore-wrapped passphrase +
conversion journal). Fresh installs create an encrypted database; existing
plaintext `memora.db` is converted with copy-and-validate rename finalize.
Welcome screen exposes user-accessible **Open-source licenses** with the SQLCipher
Community Edition BSD text (ZETETIC copyright), plus SQLite and LibTomCrypt public-
domain notices from `res/raw/open_source_notices.txt`. Emulator instrumentation:
`MemoraEncryptedDatabaseOpenerIntegrationTest` **3 of 3 passed** on Medium Phone
(fresh create, plaintext convert, PersistenceModule name), with tearDown clearing
production DB/wrapper/journal files. No PDF content persistence, WorkManager, AI, or
network path was added. Recovery copy still never mentions SQLCipher, keys, or
encryption failures.

## Verified engineering checkpoint

User-confirmed Clear Memora derived data is verified. On 2026-07-24,
`MemoraDatabaseHandle` closes the live Room helper, deletes only Memora-owned
`memora.db`/candidate/retained sidecars plus Keystore wrap alias and conversion
journal, then reopens a fresh encrypted empty index without process kill.
Welcome → **Clear Memora index** requires confirm and shows only the ADR-021 rebuild
wording. Instrumentation: `ClearMemoraDerivedDataIntegrationTest` **1 of 1 passed**;
unit copy guard `ClearMemoraDerivedDataCopyTest` passed. Persistable URI grant count
is unchanged by clear. No PDF content persistence, WorkManager, AI, or network path
was added.

## Verified engineering checkpoint

Conversion **simulated** process-death resume is verified. On 2026-07-24,
`ConversionProcessDeathResumeIntegrationTest` completed on the Medium Phone emulator:
**4 of 4 passed**. It proves resume after **instrumentation-stopped** conversion at
`ROWS_COPIED` (plaintext retained, incomplete candidate dropped, conversion completes
once), clean `SWITCH_PENDING`, mid-finalize after plaintext retained, and mid-finalize
after encrypted candidate promotion onto `memora.db`. `MemoraEncryptedDatabaseOpener`
finalize handles those interrupted file layouts.

## Verified engineering checkpoint

Conversion **live** process-death resume is verified. On 2026-07-24,
`ConversionLiveProcessDeathIntegrationTest` completed on the Medium Phone emulator:
**2 of 2 passed**. A debug-only secondary process (`:conv_live_death`) arms conversion
at `ROWS_COPIED` or `SWITCH_PENDING`; instrumentation induces a real `am crash` /
kill; the ordinary process then opens via `MemoraEncryptedDatabaseOpener` and
completes with fixture rows. No PDF content persistence, WorkManager, AI, or network
path was added.

## Verified engineering checkpoint

Low-storage / interruption conversion denial is verified. On 2026-07-24,
`ConversionLowStorageDenialIntegrationTest` completed on the Medium Phone emulator:
**2 of 2 passed**. Storage preflight denial and forced mid-conversion IO failure each
leave plaintext `memora.db` standard-SQLite-readable with fixture rows, journal
`FAILED_SAFE`, category `CONVERSION_VALIDATION_FAILED`, no incomplete encrypted
candidate; a later open with storage allowed completes conversion. No PDF content
persistence, WorkManager, AI, or network path was added.

## Verified engineering checkpoint

Physical-device `arm64-v8a` encrypted conversion proof is verified. On 2026-07-25,
Samsung Galaxy A15 5G (`SM-A156E`, ABI `arm64-v8a`, Android 16) ran
`EncryptedDatabasePocIntegrationTest`, `ProductionNamedConversionIntegrationTest`,
and `MemoraEncryptedDatabaseOpenerIntegrationTest`: **13 of 13 passed** (native
SQLCipher load, create/reopen, wrong-passphrase denial, production-named and
production-opener conversion). Standard SQLite probes now copy before open so OEM
cleanup cannot delete live encrypted files. No PDF content persistence, WorkManager,
AI, or network path was added.

## Verified engineering checkpoint

Device-unlock deferred database open and recovery-copy guards are verified. On
2026-07-25, locked-gate open refuses without creating or mutating DB/wrapper files
(`WAITING_FOR_USER_UNLOCK`), then succeeds after unlock with prior fixture rows
(`DeviceUnlockDeferredOpenIntegrationTest` **2 of 2**). Welcome path shows calm
unlock copy; rebuild + unlock strings have unit jargon guards
(`ClearMemoraDerivedDataCopyTest` **2 of 2**). No PDF content persistence,
WorkManager, AI, or network path was added.

## Verified engineering checkpoint

Encrypted conversion performance/battery budget is verified. On 2026-07-25,
`ConversionPerformanceBenchmarkIntegrationTest` completed on the Medium Phone
emulator: **4 of 4 passed** under provisional ceilings (empty / 100 / 1_000 /
5_000 synthetic schema-v3 rows). Aggregate Logcat tag `MemoraConversionBenchmark`
records content-free timing buckets and optional charge-counter deltas. Plan:
`docs/ENCRYPTED_DATABASE_CONVERSION_BENCHMARK_PLAN.md`. No PDF content persistence,
WorkManager, AI, or network path was added.

## Verified engineering checkpoint

ADR-022 accepted (non-current provenance retention). On 2026-07-25, Room schema v4
(`pdf_extractions` / pages / metadata) with additive migration 3→4 and synthetic
`RoomPdfExtractionPersistencePort` passed on the Medium Phone emulator
(`RoomPdfExtractionPersistenceIntegrationTest` + `MemoraDatabaseMigrationTest`
**5 of 5**; opener **3 of 3** on schema v4). Production discovery/UI is not wired to
persist PDF text. No real-source parse, WorkManager, AI, or network path was added.

## Verified engineering checkpoint

PDF extraction write-path resource budgets are verified. On 2026-07-25,
`PdfExtractionWriteBudgets` hard limits are enforced in
`RoomPdfExtractionPersistencePort`; Medium Phone
`PdfExtractionWritePathBenchmarkIntegrationTest` **5 of 5** and unit **3 of 3**
passed. Overflow page counts fail safely with zero rows. Plan:
`docs/PDF_EXTRACTION_WRITE_PATH_BUDGET.md`. No real-source parse, production UI
wiring, WorkManager, AI, or network path was added.

## Verified engineering checkpoint

The pure PDF parser session/chunk assembler is verified. On 2026-07-25,
IsolatedPdfParserSessionAssemblerTest passed **8 of 8** local unit tests. Plan:
docs/PDF_PARSER_SESSION_STREAMING_PLAN.md. The assembler accepts a content-free
header, accumulates ordered bounded chunks, finalizes only through the existing
result contract, and rejects or cancels without exposing partial text. No AIDL,
service protocol v3, client streaming, real-source open, Room/UI wiring,
WorkManager, AI, or network path was added.

## Verified engineering checkpoint

Binder protocol v3 session/chunk streaming is verified (synthetic descriptors only).
On 2026-07-25, Medium Phone emulator passed **25** focused tests across the private
service (including one-chunk-per-Binder pull), ordinary client, end-to-end transport,
binding adapter, approved-broker handoff, and live process-death suites. `begin` /
`nextChunk` / `cancel` replace the single-envelope `parse`; the client feeds the
session assembler and still returns status only. Plan:
`docs/PDF_PARSER_SESSION_STREAMING_PLAN.md`. No real-source open, Room/UI wiring,
WorkManager, AI, or network path was added.

## Verified engineering checkpoint

ADR-017 visible foreground PDF local-reading recovery flow is verified. On
2026-07-25, `PdfLocalReadingCopyTest` **2 of 2** and
`PdfLocalReadingSessionTest` **5 of 5** passed, and the user confirmed the
Medium Phone emulator flow: Continue → Start → Pause → Resume → Stop; Show
example problem → Try again; Text reading not enabled yet. Plan:
`docs/PDF_EXTRACTION_VISIBLE_RECOVERY_PLAN.md`. No PDF was opened; no
isolated-parser user-document call, Room extraction write, WorkManager, AI, or
network path was added.

## Verified engineering checkpoint

Real-source descriptor path is verified. On 2026-07-25, Medium Phone emulator
passed **11** focused broker/synthetic/real-source tests and fingerprint
revalidation **3 of 3**. Broker revalidates the approved-tree fingerprint before
open; stale/unavailable sources stay status-only; the real-source integration test
creates a fixture under the user-approved SAF tree, opens via live descriptor +
isolated parser, and deletes the fixture. Change control:
`docs/CHANGE_CONTROL_PDF_REAL_SOURCE_DESCRIPTOR.md`. No production UI parse wire,
searchable Room extraction persist, WorkManager, AI, or network path was added.

## Verified engineering checkpoint

Foreground Local PDF reading status UI is wired (status-only). On 2026-07-25,
unit tests for session/copy/status mapping passed (**9**), Room asset lookup
androidTest **3 of 3** on Medium Phone, and debug APK installed. Start/Retry/Resume
run one broker + isolated-parser status check for the first indexed PDF in the
connected folder; page text is still discarded; no searchable Room write,
WorkManager, AI, or network. Change control:
`docs/CHANGE_CONTROL_PDF_LOCAL_READING_STATUS_UI.md`. On 2026-07-25 the user
confirmed on Medium Phone: indexed folder (1 PDF) → Continue → Start → Completed
(“finished a local reading check… Text was not saved for search yet”).

## Verified engineering checkpoint

Searchable PDF extraction persist from Local PDF reading Start is verified. On
2026-07-25, unit local-reading copy/session/mapping passed; Medium Phone
`PersistValidatedPdfLocalReadingIntegrationTest` **1 of 1** persisted non-blank
page text from an approved-tree fixture through broker + isolated parser + Room.
Client retains validated wire results on completed sessions; Start maps → prepare →
atomic Room write; Completed copy states text was saved for search on this phone.
Change control: `docs/CHANGE_CONTROL_PDF_LOCAL_READING_PERSIST.md`. No WorkManager,
AI, network, or search-results UI was added. On 2026-07-25 the user confirmed on
Medium Phone: Start → Completed (“finished reading one PDF and saved its text for
search on this phone”).

## Verified engineering checkpoint

On-device keyword search over saved PDF page text is verified. On 2026-07-25,
`PdfKeywordSearchSupportTest` / copy tests passed and Medium Phone
`PdfExtractionKeywordSearchIntegrationTest` **2 of 2** proved current-fingerprint
matches with page excerpts and superseded rows ignored. Welcome opens **Find saved
PDF text**; copy states keyword matching, not meaning-based recall. Change control:
`docs/CHANGE_CONTROL_PDF_KEYWORD_SEARCH.md`. No WorkManager, AI, network, or
semantic Memory ranking was added. On 2026-07-25 the user confirmed on Medium
Phone: Find saved PDF text → query → page + excerpt hit.

## Verified engineering checkpoint

WorkManager SAF PDF **discovery drain** is verified (metadata placeholders only).
On 2026-07-25, unit mapper/summary/ViewModel tests passed and Medium Phone
SafPdfDiscoveryWorkerAndroidTest **3 of 3** proved unique-work page drain,
re-enqueue after complete, and access-stopped failure. Explicit **Index this folder**
enqueues background discovery; copy states folder metadata listing, not PDF text
search. Change control: docs/CHANGE_CONTROL_PDF_DISCOVERY_WORKMANAGER.md.
No extract WorkManager, AI, or network. On 2026-07-25 the user confirmed on
Medium Phone: folder connected → indexed **2 PDF items** → list up to date with
copy that text search still needs Local PDF reading; Local PDF reading then
Completed (saved for search).

## Verified engineering checkpoint

WorkManager SAF PDF **extract drain** is implemented (ADR-017 broker + isolated
parser; PersistValidatedPdfLocalReading). Explicit Local PDF reading Start enqueues
unique extract work: one pending PDF per doWork, continue while pending remain,
password skips ahead, access-stop fails. Unit mapper/copy tests and Medium Phone
SafPdfExtractWorkerAndroidTest + RoomAssetRepository pending selector passed on
2026-07-25. Change control: docs/CHANGE_CONTROL_PDF_EXTRACT_WORKMANAGER.md.
No AI/network. On 2026-07-25 the user confirmed on Medium Phone: Local PDF
reading Completed copy — finished reading PDFs from this folder and saved text
for search where reading succeeded.

## Verified engineering checkpoint

WorkManager MediaStore **discovery drain** is implemented (photo/screenshot
metadata only). Explicit Start indexing enqueues unique work that pages
IndexMediaStoreImages until complete or access fails; copy keeps full-vs-selected
honesty and never claims OCR/Memory readiness. Change control:
docs/CHANGE_CONTROL_MEDIASTORE_DISCOVERY_WORKMANAGER.md. No OCR/AI/network.
On 2026-07-25 the user confirmed on Medium Phone: Start indexing completed with
honest metadata-only copy (0 items from permitted photo library; catalogue up to
date; no photo contents / searchable memories claimed).

## Verified engineering checkpoint

Keyword search **Why this result?** Explain Mode is implemented for saved PDF
page text only. Each hit can show a citation of the query, page, and stored
excerpt with honest keyword-not-meaning copy. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_EXPLAIN.md. No AI, Memory ranking, confidence
scores, or PDF reopen. On 2026-07-25 the user confirmed on Medium Phone:
Find saved PDF text -> meet mira -> Why this result? cites Page 1 + stored excerpt
and keyword-not-meaning copy.

## Verified engineering checkpoint

Local-AI **capability interfaces** (architecture gate slice) are in domain
`com.memora.app.domain.intelligence`: CapabilityAvailability/Id types, Spec §4
VisionEngine / OcrEngine / DocumentEngine / EmbeddingEngine / MemoryBuilder /
RecallRanker, and truthful unavailable stubs with unit tests. Change control:
docs/CHANGE_CONTROL_LOCAL_AI_CAPABILITY_INTERFACES.md. No models, OCR SDKs,
embeddings, network, AI Pack download, or Hilt binds. On 2026-07-25, domain
intelligence unit tests passed. Docs do not claim on-device understanding is
ready; A-01/A-07 and full gate exit (packs, fallback matrix, benchmarks) remain
open.

## Verified engineering checkpoint

**AI Pack delivery/security plan** (architecture gate slice) is accepted:
docs/AI_PACK_DELIVERY_SECURITY_PLAN.md (ADR-023) plus domain AiPackManifest /
install-state / verification contracts and UnavailableAiPackManager stub with
unit tests. Change control:
docs/CHANGE_CONTROL_AI_PACK_DELIVERY_SECURITY_PLAN.md. No download, INTERNET,
models, or install UI. On 2026-07-25, domain intelligence unit tests passed.
A-07 notes plan progress; compatibility/fallback policy and Local-AI benchmark
plan remain for gate exit.

## Verified engineering checkpoint

**Local-AI compatibility/fallback policy** is accepted:
docs/LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY.md (ADR-024) plus domain support
decisions, default unsupported resolver, and forbidden silent semantic fallbacks
with unit tests. Change control:
docs/CHANGE_CONTROL_LOCAL_AI_COMPATIBILITY_FALLBACK.md. No models/network.

## Verified engineering checkpoint

**Local-AI benchmark plan** is accepted: docs/LOCAL_AI_BENCHMARK_PLAN.md
(ADR-025) plus domain metric/claim contracts that forbid unmeasured release
promises. Change control: docs/CHANGE_CONTROL_LOCAL_AI_BENCHMARK_PLAN.md.
No inference harness or pack baseline yet.

Local-AI architecture gate **planning** deliverables are complete. Measured pack
proof and A-01 offline end-to-end intelligence remain open.

## Verified engineering checkpoint

Keyword recall **match-count / cap honesty** is implemented. Results show how many
matches are listed; when the 20-hit cap is reached, copy states Memora lists at
most 20 and more saved pages may match. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_RECALL_CAP.md. No AI/network. On 2026-07-26 the
user confirmed on Medium Phone: Find saved PDF text → meet mira → Showing 2
matches for two fixture PDFs; Why this result? still keyword-not-meaning.

## Verified engineering checkpoint

Keyword **Why this result?** now cites the saved document label (same name as the
result card) with query, page, and excerpt. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_WHY_LABEL.md. No AI/network/PDF reopen. On
2026-07-26 the user confirmed on Medium Phone: Why cites
memora-persist-fixture.pdf and memora-real-source-fixture.pdf for meet mira.

## Verified engineering checkpoint

Keyword search **submitted-query coherence** is implemented. Editing the words
field clears stale results/Why; summary says Results for "…"; Why matches the
submitted query; superseded in-flight searches cannot overwrite. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_QUERY_COHERENCE.md. No AI/network. On 2026-07-26
the user confirmed on Medium Phone: Results/Why for meet mira, then for meet
without citing the prior query.

## Verified engineering checkpoint

**Clear-index UI recovery** is implemented: after Clear Memora index + reconnect,
Index this folder returns (stale finished WorkManager results ignored; Memora WM
tags cancelled on clear); Local PDF reading finished shows Done; local-reading
session resets on clear; extract/search resolve the live database handle.
Companion keyword empty-corpus honesty + no endless spinner. Change control:
docs/CHANGE_CONTROL_CLEAR_INDEX_UI_RECOVERY.md,
docs/CHANGE_CONTROL_PDF_KEYWORD_EMPTY_CORPUS.md. No AI/network. On 2026-07-27
the user confirmed on Medium Phone: Index returns after clear+reconnect; Done
after reading; search works.

## Verified engineering checkpoint

Keyword **excerpt match highlight** is implemented. Result cards emphasize the
first case-insensitive match of the submitted query in the saved excerpt. Change
control: docs/CHANGE_CONTROL_PDF_KEYWORD_MATCH_HIGHLIGHT.md. No AI/network. On
2026-07-27 the user confirmed on Medium Phone: search meet highlights meet in
both fixture excerpts; Why this result? still works.

## Verified engineering checkpoint

Keyword **search clear invalidation** is implemented. After Clear Memora index,
Find saved PDF text returns to Idle (typed query may remain) and ignores late
in-flight search so Results/Why cannot cite deleted excerpts. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_CLEAR_INVALIDATION.md. No AI/network. On
2026-07-27 the user confirmed on Medium Phone: clear drops stale Results/Why;
rebuild+search still works.

## Verified engineering checkpoint

Keyword **search corpus readiness** is implemented. Find saved PDF text shows
honest current-fingerprint saved page/document counts before search, refreshes
on open, and returns to empty after clear. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_SEARCH_READINESS.md. No AI/network. On
2026-07-27 the user confirmed on Medium Phone: empty/searchable/clear
readiness states display honestly, and search/Why still work.

## Verified engineering checkpoint

Keyword **blank-query guidance + seamless Search button** is implemented. Find
saved PDF text disables Search while the query is blank and shows inline
guidance; non-blank query restores prior search/Why behavior. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_QUERY_INPUT_GUIDANCE.md. No AI/network. On
2026-07-27 the user confirmed on Medium Phone: blank → disabled Search +
guidance; typed query → Search works as before.

## Verified engineering checkpoint

Keyword **IME gate + Searching progress** is implemented. Button and keyboard
Search share `canSubmitSearch`; the query field is read-only while searching;
Searching shows calm progress copy with the spinner. Change control:
docs/CHANGE_CONTROL_PDF_KEYWORD_SEARCH_PROGRESS.md. No AI/network. On
2026-07-27 the user confirmed on Medium Phone: blank keyboard Search blocked;
Searching progress + locked field; results/Why unchanged.

## Verified engineering checkpoint

Keyword **Open original PDF (in-app cited-page preview)** is implemented. Per-hit
Open shows Opening then a read-only preview of the cited page via SAF +
PdfRenderer; SourceUnavailable / CouldNotOpen feedback; Back to results; Why
unchanged. Change control: docs/CHANGE_CONTROL_PDF_KEYWORD_OPEN_ORIGINAL.md.
Fixtures: MemoraApp/fixtures/. No AI/network. On 2026-07-28 the user confirmed
on Medium Phone: all pass.

## Keyword search clear query (2026-07-28)

Clear control empties the query field and results/Why/open state; Clear Memora
index also blanks typed text. Verified on Medium Phone (user: all pass).
Change control: `docs/CHANGE_CONTROL_PDF_KEYWORD_CLEAR_QUERY.md`.
Keyword v1 exit item 2 Done.

## Keyword search cancel in-flight (2026-07-28)

Cancel search stops Searching, keeps typed query, ignores late results. Search and
Cancel are separate always-mounted buttons. Verified on Medium Phone (user: pass).
Change control: `docs/CHANGE_CONTROL_PDF_KEYWORD_CANCEL_SEARCH.md`.
Keyword v1 exit item 3 Done.

## Keyword search accessibility baseline (2026-07-28)

Headings, polite live regions, merged progress announcements, and richer preview
image descriptions on keyword search + cited-page preview. Sighted flow verified
on Medium Phone (user: all pass). Change control:
`docs/CHANGE_CONTROL_PDF_KEYWORD_ACCESSIBILITY.md`. Keyword v1 exit item 4 Done.

## MediaStore image EXIF extract (2026-07-28)

After photo catalogue, Read photo facts drains PHOTO/SCREENSHOT via WorkManager,
opens permitted URIs read-only for ExifInterface, persists `image_exif_extractions`
(Room v5). Honest EXIF-only copy. Verified on Medium Phone with 3 fixtures.
Change control: `docs/CHANGE_CONTROL_MEDIASTORE_IMAGE_EXIF_EXTRACT.md`.

## Screenshot OCR extract (2026-07-28)

After EXIF facts, Read text from screenshots drains SCREENSHOT assets via
WorkManager, opens permitted URIs read-only for bundled ML Kit Latin OCR,
persists `screenshot_ocr_extractions` (Room v6). Honest OCR-only copy; keyword
search landed as a follow-on. ADR-026. Change control:
`docs/CHANGE_CONTROL_SCREENSHOT_OCR_EXTRACT.md`.

## Screenshot OCR keyword search (2026-07-29)

Welcome → Find saved screenshot text searches saved OCR text on-device (keyword /
substring, current fingerprint only). Change control:
`docs/CHANGE_CONTROL_SCREENSHOT_OCR_KEYWORD_SEARCH.md`.

## Next approved engineering step

**Notes connector N0–N7 accepted** 2026-08-02
(`docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md`): Connect → Discover → Extract
→ Find saved note text → Build-memories NOTE facts → Open original. Interim
keyword recall covers PDF / screenshot / photo / note.

**Local-AI measured pack baselines L0–L2 closed** 2026-08-02
(`docs/CHANGE_CONTROL_LOCAL_AI_MEASURED_PACK_BASELINES.md`): synthetic integrity
only; engines remain Unavailable.

**Local-AI embedding-first track E0–E5d quality accepted** 2026-08-04:
E5c page index + E5d disclosed evidence-token boost. Emulator: `mira` →
Matched page 5 → Open Page 5 of 5 with assist disclosed in Why. Still not
measured AVAILABLE.

**M1 meaning PDF page-recall baseline (JVM) accepted** 2026-08-04:
Corpus `meaning-pdf-page-recall-v1`; cosine-only hit@1 fails interim bar
(recommends E4b); E5d boost recovers labeled @1. Does not flip AVAILABLE UI.
Change control: `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_BASELINE.md`.

**M2 on-device MediaPipe page-recall baseline accepted** 2026-08-04:
`emulator_medium_phone` + compact average-word embedder: cosine-only **0/3**,
boosted **3/3**, recommends E4b. Still not marketing AVAILABLE (emulator tier).
Change control: `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_ON_DEVICE.md`.

**E4b Universal Sentence Encoder product embedder accepted** 2026-08-04:
Product download/store/disclosure switched to MediaPipe USE (ADR-032). Legacy
average-word does not count as installed; rebuild meaning index after upgrade.
E5d assist retained until M3 USE re-measure. Still not marketing AVAILABLE.
Change control: `docs/CHANGE_CONTROL_E4B_UNIVERSAL_SENTENCE_ENCODER.md`.

**E4b emulator smoke accepted** 2026-08-04 (user): USE download + rebuild index
(Indexed 3 / Pages 10) → Find by meaning `mira` → `memora-open-5page.pdf`
**Matched page 5** → Open **Page 5 of 5** (“JULIET meet mira closing”).

**M3 USE page-recall baseline accepted** 2026-08-04:
`emulator_medium_phone` + USE: cosine-only **2/3** (vs M2 compact 0/3), boosted
**3/3**. Keep E5d disclosed assist. Still not marketing AVAILABLE.
Change control: `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_USE.md`.

**Midrange meaning measurement gate (M4 plan) accepted** 2026-08-04:
Runbook + compatibility honesty rows for `midrange_arm64` (pending physical
device at plan tip). Emulator embedding/recall = DEGRADED_EXPLICIT candidate —
not AVAILABLE.
Change control: `docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md`.

**M4 midrange USE page-recall execute accepted** 2026-08-29:
Galaxy A15 `SM-A156E` serial `RZCX12KZ6EN` (`midrange_arm64`) + USE: cosine-only
**2/3**, boosted **3/3**, embeds 14, wallMs 448 (not an SLA). Keep E5d
disclosed assist. Compatibility EMBEDDING/RECALL_RANKER midrange =
DEGRADED_EXPLICIT. Marketing AVAILABLE still **NO**. Live/Dual **N=6**
unchanged. Log `MemoraMeaningPdfM4`. Change control:
`docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md`.

**Meaning index progress + per-tap cap UI accepted** 2026-08-04:
Build meaning index batches ≤25 READY memories/tap with live progress copy and
honest “tap again for remaining” feedback; bottom Back on disclosure screen.
Change control: `docs/CHANGE_CONTROL_MEANING_INDEX_PROGRESS_CAP.md`.

**PDF local-reading progress counts accepted** 2026-08-04:
Local PDF reading drain shows aggregate “N of M” progress and completed drained
count (WorkManager unit successes). Change control:
`docs/CHANGE_CONTROL_PDF_LOCAL_READING_PROGRESS.md`.
**Next eng default:** product AVAILABLE decision after M4 (UI copy + founder
accept), or optional EmbeddingGemma / MIG-05 claim B when authorized.

**Enterprise completion (durable):**
`docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` — E5c/E5d + M1–M4 +
E4b USE smoke + meaning-index progress/cap + PDF reading progress; AVAILABLE
decision remains open.

## Screenshot OCR open-original (closed 2026-07-31)

From Find saved screenshot text hits: Open original loads a **capped** read-only
in-app preview (max edge 960px) via MediaStore URI. Search still uses saved OCR
only. Change control: `docs/CHANGE_CONTROL_SCREENSHOT_OCR_OPEN_ORIGINAL.md`.
Unit tests re-verified 2026-07-31. Interactive emulator UI **accepted** same day
on SwiftShader cold-boot Medium Phone (`note` → preview); user confirmed pass.

## Public positioning and GitHub CI (2026-07-31)

README and GitHub About use the approved on-device personal AI memory positioning
copy. ADR-027 keeps that messaging separate from MVP delivery scope. Lightweight
GitHub Actions CI runs `MemoraApp` `testDebugUnitTest` on `main` pushes/PRs
(first push run completed successfully).

## Photo OCR extract and keyword search (2026-07-31)

ADR-028 extends bundled Latin OCR to ordinary `PHOTO` assets through a separate
user-started WorkManager drain and Room v7 `photo_ocr_extractions`. Welcome →
**Find saved photo text** searches only saved current-fingerprint PHOTO OCR by
keyword, provides Why evidence, and can open a capped read-only preview. PHOTO and
SCREENSHOT storage/search remain separate. No network, AI Pack, semantic Memory
ranking, or natural-language claim. `:app:testDebugUnitTest` passed on 2026-07-31.
Emulator extract + **Find saved photo text** (`cafe`) **accepted** same day on
SwiftShader cold-boot; user confirmed pass.

## Evidence-backed Asset Memory persistence (2026-07-31)

Room v8 now stores immutable Asset Memory revisions with stable identity,
current-fingerprint provenance, normalized deterministic evidence, extraction-schema
versions, TEXT anchors, and explicit summary/anchor citations. The user-started
**Build memories from saved facts** action drains at most 25 eligible assets per tap
from already-saved PDF text/metadata, screenshot/photo OCR, and useful EXIF fields.
It never reopens a source, invokes AI, downloads a pack, or uses network.

This is the Phase 3 storage foundation, not semantic recall. Existing PDF,
screenshot, and photo keyword search remains the interim recall UI. There are no
embeddings, natural-language ranking, confidence claims, Notes, or model-backed
understanding. Change control:
`docs/CHANGE_CONTROL_ASSET_MEMORY_PERSISTENCE.md`. `:app:testDebugUnitTest` passed
on 2026-07-31. Emulator **Build memories from saved facts** (Built 3 / 3 current)
**accepted** same day; user confirmed pass. v1→v8 Room migration instrumentation
remains optional follow-up if needed.

## ADR-003 Notes strategy (accepted 2026-07-31)

Accepted: one narrowly scoped, read-only provider connector (first target: OneNote)
as explicit source authorization — not Memora accounts, not Share-as-indexing, not
arbitrary note-app scanning. Change-control **opened** 2026-07-31:
`docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md` (N0 docs; N1 is the first code
slice after plan acceptance). Do not claim all phone notes are indexed.

## Notes connector N0 (opened 2026-07-31)

Phased OneNote-class plan with explicit network/token honesty and Azure app
registration as an external N2 gate. No Notes implementation code in N0.
