# Change control: ADR-047 Class A UNFYND Core contracts under Apache-2.0

**Date:** 2026-08-29  
**Type:** Documentation / product openness decision  
**Decision guardrails:** Docs only. Authorize Release Class A for a curated
UNFYND Core Public Specification / Contract pack under Apache-2.0. Do **not**
create `public/unfynd-core/` or any pack files in this step. Do **not** rewrite
root README for Class A. Do **not** create or push a public GitHub repo. Do not
rewrite hashed Architecture Freeze, Local AI Spec, Experience Memory Amendment,
Grounding Architecture, Product Contract, or Grounded Answers amendment blobs.
Do not authorize MIG-*, Grounded Answers code, Act/agents, Architecture Freeze
reopen, or Class B commercial release. No application, database, Room,
retrieval, UI, or identity-ID change. No deploy. No push. Do not stage unrelated
dirty files (`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`).

## Pre-work record

- **Requirement IDs:** Openness / Release Class A decision only (strategy §23 /
  §24; ADR-046 Core noun; ADR-040 technical IDs; ADR-043 Act out). No new PRD
  capability IDs; no MIG-* authorization.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `DECISIONS` (ADR-040, ADR-043, ADR-044, ADR-045,
  ADR-046), `OPEN_SOURCE_COMMERCIAL_STRATEGY` (§§3–5, §7 open vs
  source-available, §8 candidates, §15 AI Pack/model IP, §16 no-cloud Core,
  §23 Release Class A vs B, §24 decisions before public release),
  `UNFYND_IDENTITY_TRANSITION_PLAYBOOK`, `CHANGE_CONTROL_TEMPLATE`,
  `CHANGELOG` Unreleased.
- **Current-code evidence inspected:** No `MemoraApp/` edits in this step.
  Hashed freeze/spec/amendment/grounding/contract/GA files are not opened for
  rewrite. Unrelated dirty files (`docs/ROADMAP.md`,
  `MemoraApp/gradle/libs.versions.toml`) are not staged. No
  `public/unfynd-core/` folder created.
- **Open ADRs / platform limitations checked:** ADR-045 / ADR-046 explicitly
  deferred Class A; this ADR closes that authorization gap for docs/contracts
  only. Strategy §24: Class A docs/contracts may proceed under artifact-specific
  license without full Model A vs B / Class B gates. User accepts Apache-2.0
  residual legal risk without external counsel for this Class A pack. CLA /
  contributor policy remains future work (strategy §13) before significant
  external code contributions.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. Class A pack must not include secrets, keystores, AI Pack
  weights/configs, evaluation corpora, or private app source.
- **Smallest safe change:** Accept ADR-047; update living pointers; optional
  strategy status one-liner; Unreleased changelog subsection; this record. Pack
  files explicitly not created.
- **Acceptance criteria:**
  - ADR-047 present after ADR-046; Status Accepted; binding points 1–10 as
    specified.
  - Class A = curated Core contracts/specs under Apache-2.0; whole stack not
    open.
  - No grant-program / foundation / RFP names in ADR, CONTINUE, or registry.
  - Technical IDs unchanged; Act out (ADR-043); honesty on unfinished
    capabilities.
  - CONTINUE, registry, changelog updated; optional strategy status line.
  - Change-control notes pack files not created in this commit.
  - No `public/unfynd-core/`, README Class A rewrite, public repo, MIG-*, or
    hashed architecture file edits; ROADMAP / libs.versions not staged.
- **Test and emulator verification plan:** Docs inspection only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-047)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (short ADR-047 pointer)
  - `CONTINUE.md` (short ADR-047 pointer)
  - `docs/CHANGELOG.md` (one new Unreleased docs-only subsection)
  - `docs/OPEN_SOURCE_COMMERCIAL_STRATEGY.md` (optional status one-liner:
    Class A authorized by ADR-047; pack implementation pending)
  - `docs/CHANGE_CONTROL_ADR047_CLASS_A_CORE_CONTRACTS.md` (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed freeze/spec/amendment/grounding/contract/GA files not
  in the change set. No `public/unfynd-core/` folder created.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Class A authorized; pack contents,
  README App vs Core rewrite, and separate public GitHub publish of **only**
  the pack are deferred follow-ups (ADR-047 §10). Class B / more Core source
  require later ADRs.
- **Documentation/traceability/ADR updates:** ADR-047; this record.
- **Git commit:** Local checkpoint after verification (no push).
