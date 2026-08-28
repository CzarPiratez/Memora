# Change control: ADR-046 Core noun on-device memory and intelligence infrastructure

**Date:** 2026-08-29  
**Type:** Documentation / product-language refinement  
**Decision guardrails:** Docs only. Noun refinement of ADR-045 public wording
only. Do not rewrite hashed Architecture Freeze, Local AI Spec, Experience
Memory Amendment, Grounding Architecture, Product Contract, or Grounded
Answers amendment blobs. Do not authorize Class A publish, opening the Android
app, MIG-*, Grounded Answers code, Act/agents, or Architecture Freeze reopen.
No application, database, Room, retrieval, UI, or identity-ID change. No
public GitHub repo setup. No deploy. No push. Do not stage unrelated dirty
files (`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`).

## Pre-work record

- **Requirement IDs:** Product-language overlay only (ADR-045 stands except
  public noun wording; ADR-043 / ADR-044 unchanged). No new PRD capability IDs.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `CONTINUE`, `DECISIONS` (ADR-045), `CHANGE_CONTROL_ADR045_*`, `CHANGELOG`
  Unreleased.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Hashed freeze/spec/amendment/grounding/contract/GA files are not opened for
  rewrite. Unrelated dirty files (`docs/ROADMAP.md`,
  `MemoraApp/gradle/libs.versions.toml`) are not staged.
- **Open ADRs / platform limitations checked:** ADR-045 retirement of PKI /
  PII / grant metaphors remains binding. Only “on-device intelligence
  infrastructure” → “on-device memory and intelligence infrastructure” is
  refined. No Class A / MIG-* / Grounded Answers code authorization.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Accept ADR-046; update living pointers; one-line
  note under ADR-045; ADR-045 change-control follow-up pointer; Unreleased
  changelog subsection; this record.
- **Acceptance criteria:**
  - ADR-046 present after ADR-045; ADR-045 body not restyled beyond the
    one-line “Public noun refined by ADR-046.” note.
  - Living noun is UNFYND Core — on-device memory and intelligence
    infrastructure.
  - ADR-045 substance (retired phrases, hashed PKI untouched, technical IDs,
    non-authorization) still stands.
  - CONTINUE, registry, AGENTS, ADR-045 change-control pointers updated.
  - One new Unreleased changelog subsection; prior Unreleased entries
    preserved.
  - No `MemoraApp/` or hashed architecture file edits; ROADMAP / libs.versions
    not staged.
- **Test and emulator verification plan:** Docs inspection only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (ADR-045 one-line note; append ADR-046)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (living noun pointers)
  - `AGENTS.md` (identity / direction line)
  - `CONTINUE.md` (living noun pointers)
  - `docs/CHANGE_CONTROL_ADR045_ON_DEVICE_INTELLIGENCE_INFRASTRUCTURE.md`
    (follow-up pointer to ADR-046)
  - `docs/CHANGELOG.md` (one new Unreleased docs-only subsection)
  - `docs/CHANGE_CONTROL_ADR046_ON_DEVICE_MEMORY_AND_INTELLIGENCE_INFRASTRUCTURE.md`
    (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed freeze/spec/amendment/grounding/contract/GA files not
  in the change set.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Noun refinement only. Does not start
  Class A, public pack, MIG-*, Grounded Answers code, or Act.
- **Documentation/traceability/ADR updates:** ADR-046; this record.
- **Git commit:** Local checkpoint after verification (no push).
