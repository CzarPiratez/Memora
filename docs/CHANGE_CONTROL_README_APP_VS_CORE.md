# Change control: Root README App vs Core (site voice)

**Date:** 2026-08-29  
**Type:** Documentation / identity (ADR-047 follow-up b)  
**Decision guardrails:** Docs only. Rewrite root README to UNFYND App vs Core
site-aligned voice. Light-align Class A pack App vs Core table and living
identity lines. Do **not** create a public GitHub repository. Do **not** push.
Do **not** start MIG-*. Do **not** rename `MemoraApp/`, `applicationId`,
`memora.db`, or GitHub repo name (ADR-040 deferred). Do not stage unrelated
dirty files (`docs/ROADMAP.md`, `MemoraApp/gradle/libs.versions.toml`).

## Pre-work record

- **Requirement IDs:** ADR-047 follow-up (b); ADR-040 / ADR-046 / ADR-047;
  lead decisions for this checkpoint (root README product/site-facing; App vs
  Core; multiplatform surfaces; no engineering status dump).
- **Source documents read:** https://www.unfynd.com/;
  `public/unfynd-core/README.md`; ADR-040, ADR-046, ADR-047; CONTINUE status
  table (awareness only); AGENTS identity lines; current root README;
  `CHANGE_CONTROL_TEMPLATE`; `CHANGE_CONTROL_CLASS_A_PACK_V1.md`.
- **Current-code evidence inspected:** No `MemoraApp/` application edits.
  Unrelated dirty files not staged.
- **Open ADRs / platform limitations checked:** ADR-047 follow-up b authorizes
  root README App vs Core rewrite. Public GitHub publish (follow-up c) remains
  deferred. Act out (ADR-043); no AVAILABLE SLA claim.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** Root README rewrite; Class A pack table-only App vs
  Core alignment; light CONTINUE / AGENTS / registry / changelog / this record.
- **Acceptance criteria:**
  - Root README: UNFYND + site link; App vs Core; privacy-first; vision ladder
    as direction with explicit non-claims; Class A honesty; practical Android
    open path with `MemoraApp/` as deferred path only; guidance links; ADR-040
    note; CI badge kept; no MIG/M4/status dump.
  - Class A pack: App vs Core table only — multiplatform App, Core unchanged.
  - No technical-ID renames; no push; no public repo.
- **Test and emulator verification plan:** Docs inspection only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `README.md`
  - `public/unfynd-core/README.md` (App vs Core table only)
  - `CONTINUE.md` (identity + site-aligned README note; ADR-047 follow-up b)
  - `AGENTS.md` (identity blurb)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (follow-up b landed)
  - `docs/CHANGELOG.md` (Unreleased)
  - `docs/CHANGE_CONTROL_README_APP_VS_CORE.md` (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. No technical-ID renames.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Public GitHub publish of **only** the
  Class A pack (ADR-047 follow-up c) remains deferred. Class B requires a later
  ADR.
- **Documentation/traceability/ADR updates:** This record; CONTINUE; changelog;
  registry pointer. ADR text unchanged.
- **Git commit:** Local checkpoint after verification (no push).
