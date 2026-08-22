# Change control: UNFYND identity transition playbook (Step 0)

**Date:** 2026-08-22  
**Type:** Documentation / operating procedure  
**Decision guardrails:** No identity ADR; no constitution retitles; no Kotlin,
Gradle, `applicationId`, database, or UI-string changes; no push.

## Pre-work record

- **Requirement IDs:** P-01 (product identity as retrieval engine remains; this
  step does not change behavior); P-18 (scope control — no silent expansion);
  A-02 (no new cloud dependency). Identity overlay itself is not yet a PRD row.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`,
  `CONTINUE`, `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS`, `ROADMAP`,
  `PRD_TRACEABILITY`, `LOCAL_AI_TECHNICAL_SPEC`, `CHANGE_CONTROL_TEMPLATE`,
  Unreleased `CHANGELOG`.
- **Current-code evidence inspected:** Repository grep found **no** `UNFYND` /
  `Unfynd` / `unfynd` before this step. App identity remains Memora:
  `applicationId = "com.memora.app"` (`MemoraApp/app/build.gradle.kts`),
  `rootProject.name = "Memora"` (`settings.gradle.kts`), `DATABASE_NAME = "memora.db"`
  (`ProductionDatabaseIdentity.kt`), `app_name` = Memora (`strings.xml`).
- **Open ADRs / platform limitations checked:** No identity ADR exists yet (Step 1).
  ADR-018 PKI north star and ADR-033–039 grounding freeze remain architecture, not
  brand. `applicationId` / `memora.db` / Keystore stay deferred per playbook §4.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. No permission, source, network, or retention change.
- **Smallest safe change:** Land the playbook, this record, and one Unreleased
  changelog bullet.
- **Acceptance criteria:**
  - `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md` exists with agreed Step 0 text.
  - Matching change-control record exists.
  - Unreleased changelog bullet exists.
  - No identity ADR; constitutions not retitled.
  - Git status shows no app identity changes (Kotlin/Gradle/`applicationId`/DB/UI).
- **Test and emulator verification plan:** Docs inspection only; no Gradle or
  emulator run required.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md` (new)
  - `docs/CHANGE_CONTROL_UNFYND_IDENTITY_PLAYBOOK.md` (new)
  - `docs/CHANGELOG.md` (Unreleased bullet only)
- **Automated verification and result:** Docs-inspection only; no Gradle run.
- **Emulator/manual verification and result:** Not required for this change.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:**
  - Product identity is not yet an accepted ADR (Step 1).
  - Living constitutions still say Memora (Step 3).
  - CONTINUE read-order not updated in Step 0 (playbook default: playbook file only).
  - User-facing Android brand still Memora (Step 4).
- **Documentation/traceability/ADR updates:** This record; no ADR; no registry
  rewrite; no P-row added.
- **Git commit:** Local checkpoint after verification (no push).
