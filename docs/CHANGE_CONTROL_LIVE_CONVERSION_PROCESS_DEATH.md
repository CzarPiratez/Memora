# Change-control: Live conversion process-death (next slice)

## Pre-work record

- **Requirement IDs:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `docs/PRODUCT_SOURCE_REGISTRY.md`,
  `docs/LOCAL_AI_TECHNICAL_SPEC.md`, `docs/GOVERNANCE.md`, `CONTINUE.md`,
  `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` (ADR-017,
  ADR-020, ADR-021), `docs/ROADMAP.md`, `docs/PRD_TRACEABILITY.md`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md`,
  `docs/CHANGE_CONTROL_TEMPLATE.md`.
- **Current-code evidence inspected:** `MemoraEncryptedDatabaseOpener` (prepare-stop
  hooks + finalize resume), `ConversionProcessDeathResumeIntegrationTest` (simulated
  only), `MemoraDatabaseHandle` / clear path, production identity files.
- **Open ADRs / platform limitations checked:** ADR-017 / ADR-020 keep PDF text
  persistence blocked. ADR-021 production open has landed; live kill / low-storage /
  physical-device proofs remain. ADR-003 notes connector still open (out of scope).
  Phase 1 WorkManager is a roadmap deliverable but CONTINUE forbids enabling it now.
- **Privacy, source-access, dependency, offline, and data-retention impact:** No new
  permission, network, or source mutation. Test must not delete user originals or
  revoke Android grants. Content-free diagnostics only.
- **Smallest safe change:** Add instrumentation that seeds plaintext conversion,
  reaches `ROWS_COPIED` or `SWITCH_PENDING`, **kills the test process** (or equivalent
  real process death), then a fresh process opens via `MemoraEncryptedDatabaseOpener`
  and asserts plaintext preserved or conversion completed exactly once with fixture
  rows. Prefer emulator-automatable kill; no UI change.
- **Acceptance criteria:**
  1. Death occurs after journal shows `ROWS_COPIED` or `SWITCH_PENDING` (real kill,
     not prepare-stop return).
  2. Next process open completes to `COMPLETED` with fixture rows, or safely retries
     from plaintext without data loss.
  3. TearDown clears production identity files.
  4. Docs mark rollout proof #3 complete only after this passes.
  5. No PDF persistence, WorkManager, AI, or network.
- **Test and emulator verification plan:** Medium Phone
  `connectedDebugAndroidTest` for the new live-kill suite; keep simulated suite as
  regression.
- **User-visible quality/accessibility review plan:** None (no UI). User confirmation
  after green tests before claiming the gate closed.

## Delivery record

- **Status:** Pre-work only. Implementation not started pending product-owner OK.
- **Files/layers changed:** honesty corrections in CONTINUE, CHANGELOG, rollout,
  PRD_TRACEABILITY, ADR-021 status (this checkpoint).
- **Known limitation or follow-up:** After live kill, still need low-storage and
  physical-device proofs before treating encryption release gates as complete.
