# Change-control: Low-storage conversion denial

## Pre-work record

- **Requirement IDs:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` proof #4, ADR-021, product
  contract / architecture / traceability (encryption release path only).
- **Current-code evidence:** `MemoraEncryptedDatabaseOpener` marks `FAILED_SAFE` on
  convert errors but does not yet preflight free space or fall back to plaintext
  open after storage denial; harness already maps validation failure to
  `CONVERSION_VALIDATION_FAILED`.
- **Open ADRs:** PDF persistence blocked (ADR-017/020). Physical-device proof remains
  after this slice. No WorkManager/AI/network.
- **Privacy impact:** None beyond existing Memora-owned DB files. No source mutation.
- **Smallest safe change:** Storage preflight (+ injectable denial) before encrypted
  candidate creation; on denial keep plaintext, journal `FAILED_SAFE`, expose
  `CONVERSION_VALIDATION_FAILED`, and open plaintext for the session. Optional IO
  interruption hook for the same fail-safe invariant.
- **Acceptance criteria:**
  1. Denied conversion leaves plaintext standard-SQLite-readable with fixture rows.
  2. Journal is `FAILED_SAFE`; last failure category is `CONVERSION_VALIDATION_FAILED`.
  3. Incomplete encrypted candidate is absent.
  4. When storage is allowed again, a later open converts successfully.
  5. No PDF persistence, WorkManager, AI, or network.
- **Test plan:** Medium Phone instrumentation for storage denial and IO interruption.

## Delivery record

- **Status:** Verified complete on 2026-07-24.
- **Code:** `ConversionStorageGuard` / `StatFsConversionStorageGuard`; opener preflight
  and IO-failure denial → `FAILED_SAFE` + `CONVERSION_VALIDATION_FAILED`, then
  plaintext session open when standard SQLite can still read `memora.db`.
- **Verification:** Medium Phone emulator
  `ConversionLowStorageDenialIntegrationTest` **2 of 2 passed** (storage denial +
  IO interruption; both leave plaintext fixtures intact and succeed on retry).
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
- **Remaining after this slice:** Physical-device `arm64-v8a` conversion proof later
  verified separately; other non-#4/#6 rollout items still open in the conversion
  rollout doc.
