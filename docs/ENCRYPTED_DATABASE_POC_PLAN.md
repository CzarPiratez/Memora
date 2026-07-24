# Encrypted Database Synthetic Proof-of-Concept Plan

**Status:** Synthetic PoC verified on the Medium Phone emulator (2026-07-24).
Production conversion and PDF content persistence remain blocked.
**Date:** 2026-07-24  
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.  
**Governing documents:** ADR-021, `docs/ENCRYPTED_DATABASE_DECISION.md`,
`docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md`.

## Purpose

Define the smallest safe code step that proves Memora can create and reopen a
**synthetic** SQLCipher-encrypted Room database with a Keystore-wrapped passphrase,
without converting live `memora.db`, writing PDF text, or changing user-visible
behavior.

## Non-goals

This PoC must **not**:

- replace or convert the production `memora.db` opened by `PersistenceModule`;
- add PDF extraction entities, content transport, or searchable text;
- change setup/indexing Compose screens;
- schedule WorkManager;
- invoke semantic understanding/AI;
- request network permission or call a remote service;
- introduce a user password, biometric prompt, or encryption setup UI;
- mention SQLCipher, keys, or encryption in any user-visible string.

## Candidate dependency

Use only the reviewed coordinate:

- `net.zetetic:sqlcipher-android:4.17.0` (AAR)
- declared companion: `androidx.sqlite:sqlite:2.6.2`, subject to Room graph resolution
  recorded during the PoC

Verify AAR SHA-256
`44fc40c33d1de597c8339072a71fa0ff20e12d01ab352d6abe4ad5df668ead94`
before treating the download as trusted.

## Architecture boundary

Keep dependencies flowing inward. New types live in `data` / platform adapters:

```text
Synthetic PoC instrumentation test
  -> temporary encrypted Room open helper (test/debug scoped)
  -> Keystore passphrase wrapper (data security helper)
  -> SQLCipher SupportOpenHelperFactory
```

Production `PersistenceModule` continues to open plaintext `memora.db` until a later,
separately approved conversion gate.

Preferred isolation for the first code change:

1. add the SQLCipher coordinate only where the PoC needs it (instrumentation /
   debug-scoped dependency if practical; otherwise an explicit version-catalog entry
   that production code does not reference yet);
2. use a **separately named** database file such as `memora_encrypted_poc.db`;
3. use a minimal Room database type that mirrors only the current schema-v3 tables
   needed for synthetic row round-trips, or reuse `MemoraDatabase` against the
   separate file name without changing the production provider.

## Exact PoC algorithm

All steps run offline on the emulator.

1. Generate a 256-bit passphrase with `SecureRandom`.
2. Create Keystore alias `memora.db.wrap.v1` (or a PoC-specific alias such as
   `memora.poc.db.wrap.v1` if isolation from a future production alias is safer).
3. Wrap the passphrase with AES-256-GCM; persist only alias/version, nonce,
   ciphertext, and wrapper-format version in no-backup private storage.
4. Clear passphrase buffers after the open helper has consumed them where APIs allow.
5. Call `System.loadLibrary("sqlcipher")`.
6. Open `memora_encrypted_poc.db` through Room `openHelperFactory(SupportOpenHelperFactory(...))`.
7. Insert synthetic Asset, discovery-checkpoint, and document-tree-approval rows that
   contain only fixture metadata (no user content, no page text).
8. Close the database, kill/recreate the process boundary as the test harness allows,
   unwrap the passphrase, and reopen.
9. Assert round-trip equality for the synthetic rows.
10. Attempt open with a wrong passphrase and assert failure without deleting files.
11. Tamper with the wrapper blob and assert a content-free recovery category such as
    `WRAPPER_INVALID` or `KEY_UNAVAILABLE`—never a silent reset.
12. Confirm standard SQLite/Room without the passphrase cannot read fixture markers
    from the encrypted file/WAL artifacts.

## Mandatory test corpus

| Test | Acceptance |
|---|---|
| Native load | `System.loadLibrary("sqlcipher")` succeeds on Medium Phone emulator |
| Create + reopen | Synthetic rows survive close/reopen with the wrapped passphrase |
| Wrong passphrase | Open fails; files retained; no destructive fallback |
| Tampered wrapper | Structured denial; no new database created over the old one |
| Plaintext probe | Unencrypted SQLite open cannot read fixture marker strings |
| Offline | Device/emulator has no validated Internet capability for the run, or the test
  asserts the app still declares no `INTERNET` permission |
| Process boundary | Reopen works after test-controlled process/DB close |
| Dependency graph | Resolved `sqlcipher-android`, `androidx.sqlite`, and Room versions are logged
  content-free and recorded in the change-control note |

## User-visible copy rule (binding even though PoC has no UI)

If any future recovery surface is stubbed for tests, use only:

> Your private Memora index needs to be rebuilt. Your original photos, documents, and
> notes are unchanged.

Never expose SQLCipher, Keystore aliases, cipher names, paths, or exception text.

## Exit gate for this PoC

The PoC is complete only when:

1. instrumentation tests on the Medium Phone emulator pass the corpus above;
2. production `PersistenceModule` still opens plaintext `memora.db` unchanged;
3. no PDF content persistence path exists;
4. provenance hashes and resolved dependency versions are recorded in the delivery
   note;
5. `CONTINUE.md` names the next separately approved step (likely conversion design
   harness or production adoption proofs)—not silent enablement.

### Delivery record (2026-07-24)

- `EncryptedDatabasePocIntegrationTest`: **7 of 7 passed** on Medium Phone emulator.
- Production `PersistenceModule` still opens `memora.db` without SQLCipher.
- SQLCipher scope: `androidTestImplementation` only.
- Resolved versions: `net.zetetic:sqlcipher-android:4.17.0`,
  `androidx.sqlite:sqlite:2.6.2`, `androidx.room:room-runtime:2.8.4`.
- Next step recorded in `CONTINUE.md`: synthetic plaintext-to-encrypted
  copy-and-validate conversion harness.

## Explicitly deferred to later gates

- plaintext `memora.db` → encrypted copy-and-validate conversion;
- production Hilt binding switch;
- physical-device ABI/performance/battery budgets;
- release Open-source notices screen with BSD attribution;
- PDF extraction Room entities and atomic content writes;
- ADR-017 real-user PDF parsing.

## Pre-work record for the future code step

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Smallest safe change:** synthetic encrypted Room open/reopen tests only.
- **Acceptance criteria:** corpus in this plan; production database path untouched.
- **Verification plan:** Medium Phone emulator instrumentation; offline/no-INTERNET
  assertion; content-free logs only.
- **Rollback:** remove PoC dependency/test database files; production path unchanged.
