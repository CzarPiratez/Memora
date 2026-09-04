# Roadmap and Quality Gates

## Local-AI architecture gate - required before AI work

**Goal:** Make the offline-first local-AI promise testable before any model, OCR,
embedding, vector, or WorkManager AI dependency is introduced.

**Deliverables:** approved Local AI Technical Specification, model capability
interfaces, AI Pack delivery/security plan, compatibility/fallback policy, benchmark
plan, and Local-AI traceability IDs.

**Status (2026-08-29):** Local-AI architecture gate **planning** deliverables are
complete (ADR-023/024/025). Measured pack baselines L0–L2 closed. **Embedding-first
track** E0–E5d + **E4b USE** + **M3** USE page-recall + **M4 plan + execute** +
**meaning-index progress/cap UI** shipped. Emulator and `midrange_arm64`
embedding/recall are DEGRADED_EXPLICIT candidate (not AVAILABLE). CI on `main`
is green. **Enterprise completion** awaits product AVAILABLE decision after M4:
`docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md`. **A-01 offline
end-to-end proof closed 2026-09-02** (`docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md`
— Samsung SM-A156E, build `bf511ca`); marketing AVAILABLE remains open.

**Exit gate:** an engineering review can prove that normal memory creation, recall,
ranking, and explanation have no cloud dependency and that unsupported devices receive
a truthful outcome rather than a hidden degraded claim. Planning docs and domain
contracts now support that review; measured pack proof is still required before
exit is claimed complete.

## Phase 0 - Foundation

**Goal:** Establish the source-neutral model before touching real device data.

**Deliverables:** domain Asset/Memory contracts, indexing-state model, unit tests,
dependency-injection skeleton, and this documentation.

**Exit gate:** an Asset can move through valid indexing states without Android APIs or
AI, and invalid transitions are rejected by tests.

## Phase 1 - Permissioned discovery

**Goal:** Discover source assets incrementally and persist placeholder records.

**Deliverables:** MediaStore image/screenshot adapter, PDF folder adapter, source
cursors, Room schema, WorkManager scheduling, progress/error UI.

**Status (2026-07-25):** SAF PDF **discovery** and MediaStore **discovery**
WorkManager drains are in place (metadata placeholders + checkpoints only).
SAF PDF extract WM also landed separately. OCR / image extract / semantic Memory
remain later phases.

**Exit gate:** restarting the app does not duplicate items; changed source assets are
re-queued; revoking access leaves existing derived records safe and clearly marked.

## Phase 2 - Deterministic extraction

**Goal:** Extract source facts before semantic processing.

**Deliverables:** local EXIF/OCR metadata, local PDF text/metadata extraction, test
fixtures, and extraction diagnostics.

**Exit gate:** fixtures produce expected structured extraction records offline.

## Phase 3 - Local understanding

**Goal:** Convert extracted facts to validated semantic memories.

**Deliverables:** Memory schema with stable identity and traceable revisions, local
intelligence interfaces, AI Pack/model registry, validated output, integrity-state
policy, retry policy, model-version policy, evidence-class policy, calibrated-
confidence evaluation plan, and privacy disclosure.

**Exit gate:** malformed output cannot corrupt the repository; every indexed memory
contains traceable evidence, a stable identity, a truthful integrity state, and no
unsupported confidence claim.

## Phase 4 - Recall and explanation

**Goal:** Search memories by natural-language recall cues and explain each match.

**Deliverables:** local query encoding where required, vector candidate retrieval,
evidence ranking, result screen, Explain Mode, and end-to-end tests with
representative assets.

**Exit gate:** a query returns the expected memory and an explanation based on stored
evidence rather than invented text.

## Phase 5 - Approved note connector

**Goal:** Implement the selected automatic source strategy for existing notes.

**Precondition:** ADR-003 is accepted and the required external setup is documented.

**Exit gate:** notes are re-indexed incrementally without manual per-note sharing.

## Post-MVP evolution - evidence-linked memory system

**Goal:** Build on a proven Asset-Memory retrieval foundation without losing source
provenance or making unsupported claims about a person's life or knowledge.

**Deliverables:** a versioned link model; deterministic and semantic candidate
generation; validated correlation decisions; Event Memory and Knowledge Memory
contracts; Memory Evolution/supersession policy; evidence-backed trust cards; and a
local, opt-in, reversible ranking-feedback design.

**Exit gate:** every proposed relationship is separately explainable, reversible, and
bounded by evidence. Asset Memories remain independently retrievable. No link or
derived memory is created solely from an embedding, and no excluded source is silently
introduced.

**Not a current-MVP gate:** this phase does not delay completion of the PRD MVP. It
does not make WhatsApp, audio, automatic timelines, or other P-18 exclusions current
features.

## Grounded Answers architecture gate — contracts before generative code

**Goal:** Freeze the Personal Information Engine answer path so Find stays the
source of truth while a replaceable on-device ReasoningEngine may answer from stored
evidence only — or abstain.

**Deliverables:** `docs/GROUNDING_ARCHITECTURE.md`,
`docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`, ADR-033–ADR-039, G-01–G-08 traceability,
`docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`, Local-AI Spec §9 carve-out, Experience
Memory status refresh.

**Status (2026-08-05):** Architecture gate **documentation complete**. No
ReasoningEngine adapter, generative pack, or Ask UI authorized. Remaining open gates:
reasoning model selection + pack lifecycle plan, PDF eval corpus, M4 measured
(or explicit unsupported policy for answer capability), adversarial pass, then first
PDF vertical slice per acceptance spec.

**Exit gate:** An engineering review can implement the PDF slice without ambiguity on
trust, completeness, abstention, cancellation, or Find coexistence. Measured device
proof and eval fixtures remain required before user-facing AVAILABLE for grounded
answers.

**Parallel workstream:** M4 midrange measure landed; enterprise AVAILABLE decision
and remaining retrieval quality work continue; do not pause for this gate.

## Deferred capabilities (planning backlog)

Named future options (RRF, cross-encoder rerank, ANN backends, SLM/runtime choices,
enterprise trust packaging) and explicit out-of-scope items are recorded in
`docs/FUTURE_CAPABILITY_BACKLOG.md`. That file does **not** authorize work; MIG-* and
Grounding gates remain authoritative.


## Mandatory quality gates for every phase

- The mandatory pre-work gate in `docs/GOVERNANCE.md` is completed before work starts.
- Requirement is mapped to `docs/PRODUCT_CONTRACT.md`.
- Permission and data-flow impact are documented.
- Tests pass.
- App builds and runs on the emulator.
- User verifies the visible step before the next user-facing feature.
