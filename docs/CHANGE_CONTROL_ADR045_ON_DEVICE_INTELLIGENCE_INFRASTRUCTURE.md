# Change control: ADR-045 UNFYND Core as on-device intelligence infrastructure

**Date:** 2026-08-29  
**Type:** Documentation / product-language decision  
**Decision guardrails:** Docs only. Do not rewrite hashed Architecture Freeze,
Local AI Spec, Experience Memory Amendment, Grounding Architecture, Product
Contract, or Grounded Answers amendment blobs. Do not authorize Class A
publish, opening the Android app, MIG-*, Grounded Answers code, Act/agents, or
Architecture Freeze reopen. No application, database, Room, retrieval, UI, or
identity-ID change. No README Class A rewrite. No public GitHub repo setup.
No deploy. No push. Do not stage unrelated dirty files
(`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`).

## Pre-work record

- **Requirement IDs:** Product-language overlay only (ADR-040 identity;
  ADR-043 architectural substance retained; ADR-044 low-power interpretation
  unchanged). No new PRD capability IDs.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `DECISIONS` (ADR-040 / ADR-043 / ADR-044),
  `UNFYND_IDENTITY_TRANSITION_PLAYBOOK`, `CHANGE_CONTROL_TEMPLATE`,
  `CHANGELOG` Unreleased.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Hashed freeze/spec/amendment/grounding/contract/GA files are not opened for
  rewrite. Unrelated dirty files (`docs/ROADMAP.md`,
  `MemoraApp/gradle/libs.versions.toml`) are not staged.
- **Open ADRs / platform limitations checked:** ADR-040 identity and deferred
  technical IDs remain binding. ADR-043 substance (Android reference shell;
  search ≠ product definition; Act out; vision ladder direction-only) stands;
  PKI becomes a prior internal noun. ADR-044 interpretation-only posture
  unchanged. No Class A / MIG-* / Grounded Answers code authorization.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. No new source, permission, cloud path, or grant
  metaphor.
- **Smallest safe change:** Accept ADR-045; registry interpretation item;
  AGENTS identity blurb; CONTINUE living noun pointers; one Unreleased
  changelog subsection; this record.
- **Acceptance criteria:**
  - ADR-045 present after ADR-044; older ADRs not restyled.
  - Living noun is UNFYND Core — on-device intelligence infrastructure.
  - Retired living phrases recorded (PKI / Personal Knowledge Infrastructure /
    Personal Intelligence Infrastructure / third-party grant metaphors).
  - Hashed blobs and ADR-043 body left unchanged; PKI = prior internal noun.
  - Technical IDs unchanged per ADR-040 / playbook.
  - Explicit non-authorization of Class A, app open, MIG-*, Grounded Answers
    code, Act/agents, Freeze reopen.
  - One new Unreleased changelog subsection; prior Unreleased entries
    preserved.
  - No `MemoraApp/` or hashed architecture file edits; ROADMAP / libs.versions
    not staged.
- **Test and emulator verification plan:** Docs inspection only. Confirm no
  Kotlin/XML/Gradle/schema edits. Confirm hashed architecture files unchanged.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-045)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (Authority and interpretation item for
    ADR-045 noun; living item 1 language)
  - `AGENTS.md` (lead identity / direction line only)
  - `CONTINUE.md` (living PKI / Personal Knowledge Infrastructure checkpoint
    nouns → on-device intelligence infrastructure / UNFYND Core; historical
    diary untouched)
  - `docs/CHANGELOG.md` (one new Unreleased docs-only subsection)
  - `docs/CHANGE_CONTROL_ADR045_ON_DEVICE_INTELLIGENCE_INFRASTRUCTURE.md`
    (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed freeze/spec/amendment/grounding/contract/GA files not
  in the change set.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Living-canon noun only. Does not start
  Class A, ADR-046, MIG-*, Grounded Answers code, or Act. Historical ADRs and
  hashed documents may still say PKI; substance of ADR-043 stands.
- **Documentation/traceability/ADR updates:** ADR-045; this record.
- **Git commit:** Local checkpoint after verification (no push).
