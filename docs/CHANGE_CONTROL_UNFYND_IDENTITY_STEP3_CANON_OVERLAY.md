# Change control: UNFYND identity Step 3 — living-canon product-noun overlay

**Date:** 2026-08-23  
**Type:** Documentation / living-canon identity overlay  
**Decision guardrails:** Docs only. Product-as-subject Memora → UNFYND on listed
living canon. No Step 4 UI strings. No Kotlin, Gradle, applicationId, database,
or GitHub. No Grounded Answers or Event/Knowledge Memory implementation. No
rewrite of ADR-001–039 bodies. No `.docx` edits.

## Pre-work record

- **Requirement IDs:** P-01 (product behaves as a memory retrieval engine; identity
  overlay only); P-18 (scope control); A-02 (no new cloud path).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`,
  `CONTINUE`, `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS` (ADR-040), `ROADMAP`,
  `PRD_TRACEABILITY`, `LOCAL_AI_TECHNICAL_SPEC`,
  `UNFYND_IDENTITY_TRANSITION_PLAYBOOK` (Step 3 only),
  `CHANGE_CONTROL_UNFYND_IDENTITY_STEP2_ARCHITECTURE_REGISTRY`,
  `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** Working tree after Step 2 commit `43a9702`.
  No `MemoraApp/` identity edits. `applicationId` not opened. PDF slice spec does
  not name the product. Three `.docx` SHA-256 values left as registered.
- **Open ADRs / platform limitations checked:** ADR-040 binds identity and
  supersedes naming in ADR-001–039 without rewriting those bodies. Technical IDs
  stay deferred. Frozen architecture (invariants, pipelines, gates, Find, Evidence
  Package, Memory types) stays frozen.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Mechanical product-noun overlay + identity headers on
  touched constitutions/amendments; CONTINUE current-checkpoint product lines;
  P-01 / P-19 living product-name cells; registry hashes for overlayed hashed
  artifacts; this record; one Unreleased changelog bullet.
- **Acceptance criteria:**
  - Living constitutions’ product subject is UNFYND.
  - Memory / Find / Evidence Package / Memory Builder names unchanged.
  - ADR-001–039 wording preserved.
  - `.docx` hashes unchanged.
  - Markdown SHA-256 rows for overlayed hashed artifacts match Git blobs.
  - No `MemoraApp/` identity edits; local commit; no push.
- **Test and emulator verification plan:** Docs inspection and hash verification.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Classification applied

| Occurrence class | Action |
|---|---|
| Product subject (“Memora is retrieval-first”) | UNFYND |
| Memory, MemoryEvidence, MemoryAnchor, Asset, Find, Evidence Package, Memory Builder, Event/Knowledge Memory as concepts | KEEP |
| ADR-001–039 bodies | NOT rewritten |
| Changelog / prior change-control historical entries | PRESERVED |
| `Memora.docx` filename, registry historical rows, SHA-256 tables for `.docx` | KEEP |
| `applicationId`, `memora.db`, Keystore, MSAL host, GitHub repo, `MemoraApp/`, `libs.versions.toml` | NOT TOUCHED |

Ambiguous product-vs-Memory sentences were left in Memory language.

## Delivery record

- **Files/layers changed:**
  - `docs/GOVERNANCE.md`
  - `docs/PRODUCT_CONTRACT.md`
  - `docs/ARCHITECTURE.md`
  - `AGENTS.md`
  - `README.md` (product/vision copy; GitHub badge URL and clone path unchanged)
  - `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md`
  - `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`
  - `docs/GROUNDING_ARCHITECTURE.md` (lead invariant and product-is prose)
  - `docs/LOCAL_AI_TECHNICAL_SPEC.md` (identity sentences only; §9 carve-out substance unchanged)
  - `CONTINUE.md` (current-checkpoint product line / Step 3 pointer)
  - `docs/PRD_TRACEABILITY.md` (P-01 living requirement; P-19 “not UNFYND account”)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (one living product-subject sentence + SHA-256 rows for overlayed hashed artifacts)
  - `docs/CHANGELOG.md` (one Unreleased bullet)
  - `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP3_CANON_OVERLAY.md` (new)
- **Not overlayed:** `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` (does not name the product). `docs/ROADMAP.md` living summaries do not use the product noun.
- **Automated verification and result:** Docx hashes unchanged vs registry historical table. Overlayed governed-architecture Markdown SHA-256 recomputed from Git blobs after add.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:**
  - Playbook Step 4 user-visible Android brand copy not started.
  - Previously unstaged Grounded Answers pointer hunks in `docs/ARCHITECTURE.md`,
    `docs/PRODUCT_CONTRACT.md`, and `docs/PRD_TRACEABILITY.md` (G-01–G-08 rows) were
    already in the working tree from the Grounded Answers docs landing; they contain
    no product-noun rewrite and are included because those files were overlay
    targets.
  - Left unstaged: `docs/ROADMAP.md` Grounded Answers architecture-gate section
    (no product-noun overlay in living summary lines); `MemoraApp/gradle/libs.versions.toml`.
  - Historical CONTINUE diary, P-07 delivery notes, and ADR-021 traceability
    paragraphs still say Memora where they record shipped history.
- **Documentation/traceability/ADR updates:** Overlay + this record. No new ADR.
- **Git commit:** Local checkpoint after verification (no push).
