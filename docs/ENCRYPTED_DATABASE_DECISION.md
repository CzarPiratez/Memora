# Local Encrypted Database Decision Record

**Status:** Accepted product direction. Synthetic proof-of-concept is separately
planned; production conversion, release dependency pin, and content write remain
blocked until their gates pass.

**Date:** 2026-07-24  
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
**Approval:** Product owner accepted ADR-021 on 2026-07-24.

## Decision boundary

Memora's current Room database is `memora.db`, schema version 3. It holds source
references, discovery checkpoints, and Asset metadata, but no PDF page text or other
content-bearing extraction records. The accepted backup policy excludes private app
data from cloud backup and device-to-device transfer. Android app-private storage
does not, however, make Room an application-encrypted database.

This decision concerns Memora-owned data **at rest**. It does not authorize source
access, parser transport, semantic understanding, embeddings, search, a Memory graph,
network access, telemetry containing user data, or user-interface changes.

## Fixed invariants

1. Original assets remain Android-owned and read-only. Encryption protects only
   Memora-owned references and derived data; it never creates another source copy.
2. The database passphrase, Keystore key, page text, metadata, source references,
   and clear-text diagnostics remain local and are never logged, backed up, exported,
   or committed.
3. Encryption mitigates offline extraction from a copied database. It does not claim
   to protect a rooted/compromised device, an already-unlocked running process,
   process memory, or a visible screen.
4. A failed unlock, migration, or validation preserves the existing database and
   exposes a truthful recoverable state. It never silently resets, downgrades, or
   alters originals.
5. Database encryption is required before durable PDF extraction content, but does
   not lift ADR-017's real-source parsing gate or authorize PDF persistence.

## Options evaluated

| Option | Strength | Material gap/cost | Decision |
|---|---|---|---|
| App-private storage plus Android file-based encryption | No new dependency; sandbox/device storage protection. | No Memora-controlled whole-database encryption or protection of a copied database in the accepted threat model. | Reject as sole model. |
| Android Keystore alone | Non-exportable keys and restricted key use. | Stores keys; does not encrypt Room's SQLite database. | Required key protector, not a complete model. |
| Custom per-column encryption on standard Room | Can protect selected values. | SQLite indexes, journals, WAL, and metadata can leak; query, integrity, and migration complexity is high. | Reject. |
| SQLCipher for Android plus Keystore-protected secret | Whole-database encryption and Room open-helper integration. | Native dependency, attribution, key recovery, and a controlled plaintext conversion are required. | **Recommend, subject to approval and proof.** |

## Recommended architecture

Use the current supported SQLCipher Android API, `net.zetetic:sqlcipher-android`,
through Room's `openHelperFactory(...)` boundary rather than obsolete
`android-database-sqlcipher`. Zetetic documented version `4.17.0` when this record
was researched; implementation must re-check compatible version, transitive graph,
checksums/provenance, licence obligations, API 26, ABIs, emulator, and physical-device
compatibility before pinning any release.

Generate a random 256-bit database passphrase with `SecureRandom`. Never hard-code it,
derive it from a user password, store it clear-text, log it, or back it up. Wrap it
with a versioned, non-exportable Android Keystore AES-256-GCM key. Persist only a
private no-backup wrapper: alias/version, nonce, ciphertext, and wrapper-format
version. Keep the passphrase only long enough to open the database and clear mutable
buffers where APIs permit.

```text
Android Keystore AES-GCM key (non-exportable)
        wraps / unwraps
random database passphrase
        opens
SQLCipher-encrypted Room database
```

Constrain the Keystore key to AES/GCM/NoPadding, 256-bit, and encrypt/decrypt
purposes. Do **not** require biometric or lock-screen authentication on every normal
database use initially. That setting can permanently invalidate keys when device
security or biometric enrollment changes and would unexpectedly block user-approved
work. An optional biometric privacy mode is a future, separately approved feature.

## Licensing, provenance, and supply chain

SQLCipher Community Edition uses a BSD-style licence with attribution/reproduction
requirements. Before adoption, record the exact licence text, Maven origin, version,
dependency locks/checksums, native ABIs, third-party notices, vulnerability review,
release provenance, and a user-accessible licence notice. Community Edition carries no
vendor SLA. FIPS, commercial support, or enterprise licensing require a separate
procurement decision; no licence code may be committed or placed in the APK.

The proof-of-concept may use only the supported SQLCipher Android API and Room factory
boundary. It must not use the deprecated library, copied native binaries, reflection,
or a hand-written cipher layer.

## Key lifecycle and recovery

### Creation

1. Generate a new passphrase only when neither a database nor valid wrapper exists.
2. Create alias `memora.db.wrap.v1` before wrapping the passphrase.
3. Atomically persist the wrapper, then create/open the encrypted database.
4. A missing wrapper/key never permits creating a database over an existing one.

### Normal operation and device lock

Database access waits until Android makes credential-encrypted app data available to
the unlocked user profile. After reboot, future work must return a structured deferred
state until user unlock rather than prompt, create a second database, or fail noisily.
The default design has no biometric prompt for ordinary recall/indexing.

### Rotation

Alias and wrapper formats are versioned. Initial rotation only rewraps the unchanged
passphrase after validation. A database passphrase/cipher rekey or library-format
upgrade is a separate migration decision. Delete the old alias only after a new
wrapper/database open succeeds across process restart.

### Key loss or invalidation

Keystore loss/invalidation can follow uninstall, app-data clearing, device-security
changes, restore inconsistencies, or platform failure. If Memora cannot unwrap or
validate its database, it must stop access and show a plain-language private-index
recovery state. It preserves files until the user confirms **Clear Memora derived data
and re-index**. That action removes only Memora-owned data; it never changes original
photos, PDFs, or notes and may require Android source re-authorization. There is no
password recovery, cloud escrow, cross-device restore, or hidden back door in this
MVP direction.

## Conversion, rollback, and rollout

Changing Room's helper to SQLCipher is not an ordinary Room schema migration: a
plaintext SQLite file cannot simply be opened by the encrypted helper. A future
release must therefore use a staged, forward-only copy-and-validate conversion:

1. validate the known plaintext database and exported Room schema;
2. create a separately named encrypted candidate database;
3. copy Memora-owned rows in bounded transactions, preserving primary keys and source
   / checkpoint relationships;
4. validate counts, foreign keys, schema version, encrypted reopen, and a repository
   read;
5. make a controlled closed-database switch while retaining original database/WAL/SHM
   files until the first encrypted restart succeeds; then
6. delete plaintext files only after that verified switch, using a crash-resume state
   machine.

No destructive migration, fallback-to-destructive migration, automatic reset, or
in-place overwrite is allowed. Conversion failure keeps the old database and exposes
a safe retry state. Downgrade is not assumed safe: rollout needs staged cohorts,
content-free version telemetry, and a documented support/recovery procedure.

## Diagnostics and user experience

Privacy should usually be invisible. Ordinary use has no password to create or
remember, no biometric prompt for every search or index, and no encryption setup
screen. Memora simply works while protecting its private index locally.

If recovery is required, explain what happened, what remains safe, and what the user
action will change. Never show aliases, cipher details, paths, source names, text, or
raw exceptions. Never mention “SQLCipher,” “keys,” or “encryption failures” to users.
The only approved recovery wording is:

> Your private Memora index needs to be rebuilt. Your original photos, documents, and
> notes are unchanged.

Only coarse local categories are permitted: `KEY_UNAVAILABLE`, `WRAPPER_INVALID`,
`DATABASE_AUTH_FAILED`, `CONVERSION_VALIDATION_FAILED`, and
`USER_CONFIRMED_INDEX_CLEAR`. Logs may contain category, application/database schema
version, and elapsed-time bucket only; never SQL, keys, URIs, titles, metadata values,
page text, or parser output. Any support export is a separate consent decision.

## Mandatory proof before adoption

1. Exact dependency/version/provenance/licence/notice/vulnerability record and Gradle
   locking/ABI review are accepted.
2. Instrumentation tests create, reopen, rotate, reject tampered wrapper data, and
   safely handle inaccessible Keystore material without leaking a secret.
3. The encrypted database cannot be opened by standard SQLite/Room; SQLCipher opens
   it only with a valid wrapped passphrase; WAL/journal tests find no clear-text test
   content in artifacts.
4. Current schema v3 converts synthetic Asset/checkpoint/document-tree rows;
   empty-install, process death, low-storage, interruption, validation failure, and
   restart preserve the original or complete exactly once.
5. Clear-derived-data/source-removal affect only encrypted Memora records; originals
   and Android permissions are untouched.
6. Tests run offline after reboot/user unlock, across supported API/ABI configurations
   and at least one physical device before release.
7. Build, unit, instrumentation, migration, accessibility/recovery-copy, and manual
   recovery tests pass; performance/battery measurements set limits before PDF text
   persistence begins.

## Consequences and status

Room is not currently encrypted. ADR-021 acceptance plus
`docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` and
`docs/ENCRYPTED_DATABASE_POC_PLAN.md` authorize the synthetic SQLCipher/Keystore
proof-of-concept. `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` defines the
production PersistenceModule switch acceptance criteria. A separate implementation
gate is required for that switch, and another for PDF extraction persistence.

## Sources consulted

- [Android Keystore system](https://developer.android.com/privacy-and-security/keystore)
- [Android cryptography guidance](https://developer.android.com/privacy-and-security/cryptography)
- [Key authentication and invalidation behavior](https://developer.android.com/reference/android/security/keystore/KeyProtection.Builder)
- [SQLCipher Android migration and Room integration](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/)
- [SQLCipher Community Edition integration](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-community/)
- [SQLCipher licence information](https://www.zetetic.net/sqlcipher/license/)

## Pre-work and delivery record

- **Inspected:** product registry, Local AI Technical Specification, governance,
  continuation, product contract, architecture, decisions, PDF persistence
  design/contract/plan, traceability, current Room/Hilt module, Gradle catalogue, and
  backup configuration.
- **Current-code evidence:** `PersistenceModule` opens `memora.db` with standard
  `Room.databaseBuilder` and migrations 1-to-2/2-to-3. There is no encryption
  dependency or content-bearing PDF Room entity.
- **Affected layers:** documentation/future data-security boundary only.
- **Verification:** documentation cross-reference, code inspection, Maven Central
  artifact/POM/checksum review, and OSV query for `net.zetetic:sqlcipher-android:4.17.0`
  on 2026-07-24 (no listed advisories). No executable app code changed.
- **Follow-up docs:** `docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` and
  `docs/ENCRYPTED_DATABASE_POC_PLAN.md`.
- **Known limitation:** no encrypted database, content persistence, or security
  certification exists yet. Production conversion remains blocked until the synthetic
  PoC and later adoption gates pass.
