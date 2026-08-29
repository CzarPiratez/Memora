# Change control: ADR-049 Canonical Recall naming

**Date:** 2026-08-29  
**Type:** Documentation / naming decision  
**Decision guardrails:** Docs only. Do not rewrite hashed Product Contract,
Freeze, Spec, Grounding, or Amendment blobs. Do not reopen Architecture Freeze
v1.0. Do not authorize MIG-06, MIG-07, MIG-07B, or MIG-05 step 4. Do not claim
Canonical Recall exists in code. Do not retire legacy keyword Find or
`PdfPageEmbedding*` dual-write. Do not change MIG-05 step 3 acceptance or
search implementation. Do not implement Grounded Answers, VisionEngine, Act,
package rename, `LEGACY_RECALL_SURFACE`, or Cursor rules. No application,
database, Room, retrieval, or UI code. No deploy. No push.

## Pre-work record

- **Requirement IDs:** Freeze §3 (one evidence substrate; one canonical recall
  pipeline — literal + semantic as candidate generation into one
  structured-filter-and-ranking stage); Spec `RecallRanker` capability;
  Migration Spec MIG-06 / MIG-07 / MIG-07B; Grounding §6 Retriever; ADR-035
  Retriever port.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE` (MIG-05 step 3 checkpoint), `ARCHITECTURE_FREEZE_v1.0`
  §3, `ARCHITECTURAL_MIGRATION_SPEC_V1` (MIG-06/07/07B), `GROUNDING_ARCHITECTURE`
  §6, `LOCAL_AI_TECHNICAL_SPEC` (RecallRanker), `DECISIONS` (ADR-048 tone;
  ADR-035), `CHANGE_CONTROL_TEMPLATE`, `CHANGELOG` Unreleased.
- **Current-code evidence inspected:** No `MemoraApp/` application/search edits
  in this step. Unrelated dirty MIG-05 / gradle / ROADMAP files left unstaged.
  Hashed freeze/spec/amendment/grounding files not opened for rewrite.
- **Open ADRs / platform limitations checked:** ADR-035 Retriever remains the
  Grounding-facing port name. Freeze §3 already requires one canonical recall
  pipeline — locked Option C / Canonical Recall naming does not contradict it.
  No ADR-049 collision (last accepted is ADR-048). MIG-05 step 3 remains the
  live search-cutover checkpoint; this ADR does not reopen it.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. No new source, permission, or cloud path.
- **Smallest safe change:** Accept ADR-049; registry / CONTINUE / changelog
  pointers; this record. No GOVERNANCE MIG-06+ authorization line.
- **Acceptance criteria:**
  - ADR-049 present after ADR-048; Status Accepted.
  - Frozen App boundary name: **Canonical Recall**.
  - Option C recorded (Retriever = same pipeline, not competing Find).
  - `SearchMemoryEvidence` documented as candidate generation into Canonical
    Recall.
  - Spec `RecallRanker` and MIG-07B filter recorded as stages inside Canonical
    Recall.
  - False-positive guard present (extraction → MemoryBuilder allowed;
    extraction DAO → user-visible search outside Canonical Recall forbidden
    after cutover / allowlist).
  - Explicit non-authorization of MIG-06+, MIG-05 step 4, code claims, dual-write
    retirement, hashed-blob rewrites.
  - Zero `MemoraApp/` search/production code changes in this step.
- **Test and emulator verification plan:** Docs inspection only. Confirm no
  Kotlin/XML/Gradle/schema edits in the ADR-049 change set. Emulator code
  verification not required beyond “no app code changed.”
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-049)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (Authority and interpretation item 10)
  - `CONTINUE.md` (short Docs note under Current checkpoint; MIG-05 step 3
    status text undisturbed beyond ADR-049 cross-reference in the note)
  - `docs/CHANGELOG.md` (one new Unreleased docs-only subsection)
  - `docs/CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md` (this file)
  - `docs/GOVERNANCE.md` — not modified (no MIG-06+ authorization; recent ADR
    pattern does not require a governance line for naming-only ADRs)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits in this change set. Hashed freeze/spec/amendment/grounding/
  contract files not in the change set.
- **Emulator/manual verification and result:** Not required (docs-only; no app
  code changed).
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Naming only. Canonical Recall API,
  MIG-06/07/07B, legacy surface inventory, and Cursor rules remain later
  authorized steps. MIG-05 step 3 acceptance unchanged.
- **Documentation/traceability/ADR updates:** ADR-049; this record.
- **Git commit:** Local checkpoint after verification when requested (no push).
