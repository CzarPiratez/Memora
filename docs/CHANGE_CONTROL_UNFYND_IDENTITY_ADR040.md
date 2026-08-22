# Change control: UNFYND identity ADR-040 (Step 1)

**Date:** 2026-08-23  
**Type:** Documentation / product-identity decision  
**Decision guardrails:** Docs only. No constitution overlay. No hashing of new
architecture files. No Kotlin, Gradle, strings, package, DB, GitHub, or push.
No PKI-stage implementation.

## Pre-work record

- **Requirement IDs:** P-01 / P-18 (product definition and scope control; identity
  overlay does not change shipped behavior); A-02 (no new cloud path).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`,
  `CONTINUE`, `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS`, `ROADMAP`,
  `PRD_TRACEABILITY`, `LOCAL_AI_TECHNICAL_SPEC`, `UNFYND_IDENTITY_TRANSITION_PLAYBOOK`
  (Step 1), `CHANGE_CONTROL_TEMPLATE`.
- **Current-code evidence inspected:** Latest accepted ADR in the working
  `DECISIONS.md` is ADR-039. `applicationId` remains `com.memora.app`. Docx
  SHA-256 values match the registry table. UNFYND is absent from app code.
- **Open ADRs / platform limitations checked:** No prior identity ADR. Technical
  IDs (`com.memora.app`, `memora.db`, Keystore, MSAL, GitHub, `MemoraApp/`) stay
  deferred. ADR-018 PKI and ADR-033–039 grounding remain architecture, not brand.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** ADR-040 + registry interpretation + this record + one
  Unreleased changelog bullet.
- **Acceptance criteria:**
  - ADR-040 present after ADR-039; ADR-001–039 bodies not rewritten.
  - Registry current identity is UNFYND; `Memora.docx` remains historical baseline.
  - Three `.docx` SHA-256 rows unchanged; hashes verified against files.
  - No SHA-256 for new architecture files (Step 2).
  - UNFYND still absent from app code; no `MemoraApp/` identity edits this step.
- **Test and emulator verification plan:** Docs inspection only.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/DECISIONS.md` (append ADR-040 only as this step’s intent)
  - `docs/PRODUCT_SOURCE_REGISTRY.md` (interpretation only)
  - `docs/CHANGE_CONTROL_UNFYND_IDENTITY_ADR040.md` (new)
  - `docs/CHANGELOG.md` (one Unreleased bullet)
- **Automated verification and result:** Docs-inspection; docx hashes match table.
- **Emulator/manual verification and result:** Not required.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:**
  - Constitution overlay is playbook Step 3 (not done).
  - New architecture file inventory/hashes are Step 2 (not done).
  - CONTINUE one-line skipped: `CONTINUE.md` already has unrelated dirty hunks.
  - Working-tree extra: ADR-033–039 and the Grounded Answers registry amendment
    row were already uncommitted; this step reports them and does not treat them
    as Step 1 deliverables.
- **Documentation/traceability/ADR updates:** ADR-040; this record.
- **Git commit:** Local checkpoint after verification (no push).
