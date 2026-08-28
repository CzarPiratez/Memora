# Change control: Class A pack files under `public/unfynd-core/`

**Date:** 2026-08-29  
**Type:** Documentation / Class A pack implementation (ADR-047 follow-up a)  
**Decision guardrails:** Docs/curated public markdown only. Create
`public/unfynd-core/` Apache-2.0 contracts pack. Do **not** create a public
GitHub repository. Do **not** push. Do **not** rewrite root README (ADR-047
follow-up b). Do **not** start MIG-*. Do **not** edit hashed Freeze / Spec /
Amendment / Grounding / Product Contract blobs — distill into **new** public
markdown. No secrets, keystores, app source, corpora, or grant-program framing.
Do not stage unrelated dirty files (`docs/ROADMAP.md`,
`MemoraApp/gradle/libs.versions.toml`).

## Pre-work record

- **Requirement IDs:** ADR-047 follow-up (a); strategy §23 Class A; ADR-046 Core
  noun. No MIG-* authorization.
- **Source documents read:** ADR-046, ADR-047, ADR-043, ADR-044;
  `OPEN_SOURCE_COMMERCIAL_STRATEGY` §23; `PRODUCT_CONTRACT` (definition /
  privacy / memory integrity high level); `LOCAL_AI_TECHNICAL_SPEC` §4 + §9;
  `EXPERIENCE_MEMORY_AMENDMENT_V1` §2 + §5; `ARCHITECTURE_FREEZE_v1.0` §3;
  CONTINUE MIG-04 MemoryBuilder seam summary; `CHANGE_CONTROL_TEMPLATE`;
  `CHANGE_CONTROL_ADR047_CLASS_A_CORE_CONTRACTS.md`.
- **Current-code evidence inspected:** No `MemoraApp/` edits. Hashed
  freeze/spec/amendment/grounding/contract blobs not opened for rewrite.
  Unrelated dirty files not staged.
- **Open ADRs / platform limitations checked:** ADR-047 authorizes Class A
  boundary; this step implements pack folder only. Public GitHub publish and
  root README rewrite remain deferred (follow-ups b/c).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. Pack contains no secrets or app source.
- **Smallest safe change:** Add `public/unfynd-core/` pack files; short CONTINUE
  + CHANGELOG + registry pointer; this record.
- **Acceptance criteria:**
  - `LICENSE` (Apache-2.0), `NOTICE`, `README.md`, `SPEC.md`,
    `ROADMAP-OPEN.md`, `SECURITY.md`, optional `CITATIONS.md` present.
  - Distillation only; no hashed blob edits; no MIG-*; no push; no public repo.
  - Honesty: no AVAILABLE / Act shipped claims; Act out; App vs Core clear.
  - Copyright from existing git author identity; UNFYND® / UNFYND Core® marks
    noted; no grant metaphors.
- **Test and emulator verification plan:** Docs inspection / tree listing only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `public/unfynd-core/LICENSE`
  - `public/unfynd-core/NOTICE`
  - `public/unfynd-core/README.md`
  - `public/unfynd-core/SPEC.md`
  - `public/unfynd-core/ROADMAP-OPEN.md`
  - `public/unfynd-core/SECURITY.md`
  - `public/unfynd-core/CITATIONS.md`
  - `CONTINUE.md` (short Class A pack note)
  - `docs/CHANGELOG.md` (Unreleased subsection)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (pack-landed pointer)
  - `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md` (this file)
- **Automated verification and result:** Docs inspection. No Kotlin/XML/Gradle/
  schema edits. Hashed architecture files not in the change set.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Root README App vs Core rewrite (ADR-047
  b) landed in `CHANGE_CONTROL_README_APP_VS_CORE.md`. Separate public GitHub
  publish of **only** this pack (ADR-047 c) remains deferred. Class B requires
  a later ADR.
- **Documentation/traceability/ADR updates:** This record; CONTINUE; changelog;
  registry pointer. ADR-047 text unchanged (authorization already accepted).
- **Git commit:** Local checkpoint after verification (no push).
- **Follow-up (2026-08-29):** NOTICE copyright corrected to
  `Copyright 2026 UNFYND <czar.piratez@gmail.com>`; pack rescanned — no other
  Memora / mir.m.hameedi copyright lines.
