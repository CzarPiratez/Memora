# Change control: ADR-043 PKI vision vs freeze; Act remains out

**Date:** 2026-08-24  
**Type:** Documentation / product-direction decision  
**Decision guardrails:** Docs only. Do not rewrite hashed Product Contract,
Freeze, Spec, or Amendment blobs. Do not reopen Architecture Freeze v1.0. Do
not start MIG-01–MIG-11 or MIG-07B. No application, database, migration,
retrieval, OCR, MemoryBuilder, RecallRanker, Grounded Answers,
Event/Knowledge/Links, package, or identity change. Do not skip Asset-Memory
quality to jump to Links, Events, Grounded Answers, or conversation UI. No
deploy. No push.

## Pre-work record

- **Requirement IDs:** P-01 (product definition; this ADR records that the
  one-line “retrieval engine” lead is narrower than the PKI north star and
  does not rewrite it); P-18 (MVP exclusions stay out unless an explicit
  contract/ADR adds a source); E-01–E-06 (staged Asset → Link → Event →
  Knowledge); A-02 (no new cloud path); G-01–G-08 (Grounded Answers remain
  architecture, not implementation).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `PRODUCT_CONTRACT` (definition, long-term
  direction, Grounded Answers, MVP exclusions), `EXPERIENCE_MEMORY_AMENDMENT_V1`
  §§1–2 and §§8–9, `ARCHITECTURE_FREEZE_v1.0` §§3–4, `GROUNDING_ARCHITECTURE`
  §16, `DECISIONS` (ADR-040 / ADR-042), `CHANGE_CONTROL_TEMPLATE`,
  `CHANGELOG` Unreleased.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Hashed freeze/spec/amendment files are not opened for rewrite. Unrelated
  dirty files (`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`) are
  not staged.
- **Open ADRs / platform limitations checked:** ADR-040 identity and PKI north
  star remain binding. ADR-042 Freeze vs Grounding decisions remain in force.
  GROUNDING_ARCHITECTURE.md §16 already defers chat history, agents,
  personalities, and goal orchestration. Act is recorded here as out of
  current architecture.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. Originals remain read-only. No new source, permission,
  or cloud path.
- **Smallest safe change:** Accept ADR-043; registry/CONTINUE/GOVERNANCE
  pointers; one Unreleased changelog bullet; optional non-canon vision map;
  this record. Do not rewrite changelog history or hashed constitutions.
- **Acceptance criteria:**
  - ADR-043 present after ADR-042; older ADRs not restyled.
  - Frozen architecture accepted as suitable through See/Remember, staged
    Connect, retrieve-by-meaning, and Understand / converse-as-Q&A.
  - Act / agentic Personal AI / mutating originals recorded as out until a
    later ADR and product-contract change.
  - Connector expansions are not a new Memory Core; P-18 not silently in-scope.
  - Product Contract one-line definition not rewritten; hashed blobs unchanged.
  - Architecture Freeze not reopened; MIG-* still unstarted.
  - One new Unreleased changelog bullet; prior Unreleased entries preserved.
  - No `MemoraApp/` or schema/migration file edits.
- **Test and emulator verification plan:** Docs inspection only. Confirm no
  Kotlin/XML/Gradle/schema edits. Confirm hashed freeze/spec/amendment files
  are untouched.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-043)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (Authority and interpretation item 6)
  - `docs/GOVERNANCE.md` (single ADR-043 cross-reference; governing order
    unchanged)
  - `CONTINUE.md` (checkpoint, read-order, frozen-direction pointer)
  - `docs/CHANGELOG.md` (one new Unreleased entry; prior entries untouched)
  - `docs/UNFYND_VISION_ALIGNMENT.md` (optional; not hashed; not architectural
    authority)
  - `docs/CHANGE_CONTROL_ADR043_PKI_VISION_ALIGNMENT.md` (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed freeze/spec/amendment files not in the change set.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Product Contract still leads with
  “personal memory retrieval engine”; a later overlay/ADR may align that
  one-line definition. Act remains out. MIG-* remains unstarted. Do not skip
  Asset-Memory quality.
- **Documentation/traceability/ADR updates:** ADR-043; this record.
- **Git commit:** Local checkpoint after verification (no push).
