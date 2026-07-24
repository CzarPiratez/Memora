# Change-control: Physical-device arm64 conversion proof

## Pre-work record

- **Requirement IDs:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` proof #6, ADR-021, product
  contract / architecture / traceability (encryption release path only).
- **Current-code evidence:** Emulator suites already cover native SQLCipher load,
  PoC reopen, wrong-passphrase denial, production-named conversion, and
  `MemoraEncryptedDatabaseOpener` conversion. Rollout proof #6 still requires at
  least one physical `arm64-v8a` device.
- **Device under test:** Samsung Galaxy A15 5G (`SM-A156E`), serial `RZCX12KZ6EN`,
  ABI `arm64-v8a`, Android 16 / API 36.
- **Open ADRs:** PDF persistence blocked (ADR-017/020). Other rollout checklist
  items (device unlock, recovery copy review, performance budget) remain separate.
- **Privacy impact:** Instrumentation uses synthetic fixtures only; tearDown clears
  disposable DB/wrapper/journal files. No source mutation. No WorkManager/AI/network.
- **Smallest safe change:** Run existing instrumentation suites on the physical
  device; record results. Code changes only if a device-specific failure appears.
- **Acceptance criteria:**
  1. Native SQLCipher library loads on `arm64-v8a`.
  2. Encrypted create + reopen round-trip succeeds.
  3. Wrong passphrase denies with `DATABASE_AUTH_FAILED` and retains the DB file.
  4. Production-named / production-opener conversion succeeds on device.
  5. No PDF persistence, WorkManager, AI, or network.
- **Test plan:** Target serial `RZCX12KZ6EN` only:
  `EncryptedDatabasePocIntegrationTest`,
  `ProductionNamedConversionIntegrationTest`,
  `MemoraEncryptedDatabaseOpenerIntegrationTest`.

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Device:** Samsung Galaxy A15 5G (`SM-A156E`), serial `RZCX12KZ6EN`, ABI
  `arm64-v8a`, Android 16 / API 36.
- **Verification:** On-device instrumentation **13 of 13 passed**:
  `EncryptedDatabasePocIntegrationTest` (native load, reopen, wrong-passphrase,
  cleartext probe), `ProductionNamedConversionIntegrationTest`,
  `MemoraEncryptedDatabaseOpenerIntegrationTest`.
- **Harness fix:** `StandardSqliteDatabaseProbe` copies before probing. On this
  OEM/API build, opening an encrypted SQLCipher file with platform SQLite deleted
  the live path; probes must never risk Memora-owned DB files.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
- **Remaining after this slice:** Other rollout checklist items (device unlock,
  recovery copy review, performance budget, etc.) in
  `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md`.
