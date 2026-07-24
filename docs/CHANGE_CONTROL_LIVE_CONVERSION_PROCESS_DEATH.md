# Change-control: Live conversion process-death

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
  persistence blocked. ADR-021 production open has landed; low-storage denial and
  physical-device arm64 proof have since landed (see
  `CHANGE_CONTROL_LOW_STORAGE_CONVERSION.md`,
  `CHANGE_CONTROL_PHYSICAL_DEVICE_CONVERSION.md`). ADR-003 notes connector still open
  (out of scope). Phase 1 WorkManager is a roadmap deliverable but CONTINUE forbids
  enabling it now.
- **Privacy, source-access, dependency, offline, and data-retention impact:** No new
  permission, network, or source mutation. Test must not delete user originals or
  revoke Android grants. Content-free diagnostics only. Live-death worker is
  debug-source only.
- **Smallest safe change:** Secondary debug process arms conversion at journal phase;
  instrumentation induces real `am crash`/kill; ordinary process opens and asserts
  completed encrypted rows.
- **Acceptance criteria:** Met — see delivery record.
- **Test and emulator verification plan:** Medium Phone
  `ConversionLiveProcessDeathIntegrationTest`.
- **User-visible quality/accessibility review plan:** None (no UI).

## Delivery record

- **Status:** Verified and committed.
- **Files/layers changed:** debug `ConversionLiveDeathWorkerService` + marker;
  `ConversionLiveProcessDeathIntegrationTest`; CONTINUE/CHANGELOG/rollout/
  PRD_TRACEABILITY/ADR-021 updates.
- **Automated verification and result:** Medium Phone emulator **2 of 2 passed**
  (`ROWS_COPIED` and `SWITCH_PENDING` live crash/kill + cold resume).
- **Emulator/manual verification and result:** Instrumentation only; no UI change.
- **Failure/recovery paths verified:** Journal phase survives kill; opener completes
  to `COMPLETED` with fixture rows; standard SQLite probe fails on production DB.
- **Known limitation or follow-up:** Device-unlock / recovery-copy / performance
  checklist items remain (physical arm64 and low-storage verified separately).
- **Documentation/traceability/ADR updates:** Yes.
- **Git commit:** (this checkpoint)
