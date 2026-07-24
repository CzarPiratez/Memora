# SQLCipher Dependency and Provenance Review

**Status:** Accepted. SQLCipher is on the production classpath; `PersistenceModule`
opens encrypted `memora.db` via `MemoraEncryptedDatabaseOpener`; Open-source notices
ship the Community Edition BSD text. This does **not** authorize PDF content
persistence.

**Date:** 2026-07-24  
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.  
**Governing decisions:** ADR-020 (accepted privacy posture), ADR-021 (accepted
encrypted-database direction), `docs/ENCRYPTED_DATABASE_DECISION.md`,
`docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md`.

## Purpose and boundary

This review records the exact candidate SQLCipher Android artifact Memora may use in
a later, tightly scoped synthetic proof-of-concept. It answers supply-chain,
licence, ABI, and API-floor questions before any Gradle dependency is added.

It does **not**:

- convert `memora.db` or change `PersistenceModule` encrypted open;
- open a user source, transport page text, schedule WorkManager, invoke AI, or use
  the network;
- claim a permanent vulnerability guarantee or enterprise SLA;
- satisfy the BSD notices release gate by itself.

Classpath note (2026-07-24): the reviewed coordinate is `implementation` in
`MemoraApp/app/build.gradle.kts`. Encrypted open and notices UI shipped the same day
after conversion instrumentation gates.

## Provenance re-check (2026-07-24, Slice 1)

| Check | Result |
|---|---|
| AAR SHA-256 | Unchanged: `44fc40c33d1de597c8339072a71fa0ff20e12d01ab352d6abe4ad5df668ead94` |
| OSV `net.zetetic:sqlcipher-android:4.17.0` | Empty vulnerability set (`{}`) |
| Decision | Promote to production classpath; do not switch Room open helper yet |

## Candidate artifact

| Field | Value |
|---|---|
| Coordinates | `net.zetetic:sqlcipher-android:4.17.0` |
| Packaging | AAR (`@aar`) |
| Maven origin | Maven Central |
| Artifact URL | `https://repo.maven.apache.org/maven2/net/zetetic/sqlcipher-android/4.17.0/` |
| Published | 2026-07-08 |
| Project URL | https://www.zetetic.net/sqlcipher |
| Source repository | https://github.com/sqlcipher/sqlcipher-android |
| Release note | https://www.zetetic.net/blog/2026/07/08/sqlcipher-4.17.0-release/ |
| Deprecated alternative rejected | `net.zetetic:android-database-sqlcipher` |

### Integrity hashes recorded on 2026-07-24

| Artifact | Size (bytes) | SHA-256 | SHA-1 | MD5 |
|---|---:|---|---|---|
| `sqlcipher-android-4.17.0.aar` | 4,007,507 | `44fc40c33d1de597c8339072a71fa0ff20e12d01ab352d6abe4ad5df668ead94` | `08d276dfa786834efe4bc299ccd63b4af921d301` | `c8e961a985bbf467f0953febfb9cfa6e` |
| `sqlcipher-android-4.17.0-sources.jar` | 117,707 | `c0ddfac43ffa592ca62805e54fc6276b2236522f6ee769dbd2c9a9684f14df50` | `2e77d1d543797576d20713053608a094ad860a34` | `da6fe5a400f20e3f99bf2edbff7bfc1f` |
| `sqlcipher-android-4.17.0-javadoc.jar` | 529,786 | `d9fa3c2b143dec15bade0605a636533965f72f98a869a8fd3d77984642397e13` | `0087d34bf6979971ba3cdc0344c0ebebf1cff2f8` | `92b9401fe313cec86e132dfc61c9dcbf` |

AAR SHA-512 (Gradle module metadata):  
`f3d2e7d949c35f392e5705f4afb385ac97f0e1a4dafee4ff6ff053a35777f5407aba77f0323aa4f6d8444c5dd5f46b4f965a8cc4d6a652a37fb881f5e33d6ac3`

Before any future pin, re-download the AAR and verify these hashes. Do not trust a
cached copy if hashes diverge.

## Declared runtime dependency

The published POM declares exactly one runtime dependency:

- `androidx.sqlite:sqlite:2.6.2` (runtime scope)

Memora currently uses Room `2.8.4` via `MemoraApp/gradle/libs.versions.toml`. The
synthetic PoC must resolve the final `androidx.sqlite` graph, refuse an unresolved
conflict, and record the resolved versions. It must not silently force an incompatible
downgrade of Room's SQLite support libraries.

## Supported product fit

| Concern | Finding |
|---|---|
| Memora `minSdk` | App `minSdk = 26`. SQLCipher Android Community Edition is used widely at this floor; PoC must prove open/create on API 26 emulator and at least one higher API used by the team. |
| Emulator ABI | Medium Phone emulator uses `x86_64`. The AAR is expected to ship multiple ABIs including emulator-compatible ones; PoC must prove native `System.loadLibrary("sqlcipher")` succeeds on the emulator before any conversion work. |
| Device ABI | Future physical-device gate must cover `arm64-v8a` (and any other ABI Memora ships). |
| Room integration | Supported path is `net.zetetic.database.sqlcipher.SupportOpenHelperFactory` with Room `openHelperFactory(...)` after `System.loadLibrary("sqlcipher")`. |
| Cryptographic provider (Community Android) | LibTomCrypt-based Community build (non-FIPS), per Zetetic 4.17.0 release notes. |
| Network | Artifact adds no INTERNET requirement. Memora manifests remain without `INTERNET`. |

## Licence and attribution obligations

Edition selected: **SQLCipher Community Edition**.

- Licence style: BSD-style, per https://www.zetetic.net/sqlcipher/license/
- Obligation: reproduce copyright notice, licence conditions, and disclaimer in a
  **user-accessible** location (in-app About/Open-source notices and/or linked product
  documentation).
- Copyright line expected by Zetetic community guidance:  
  `Copyright (c) 2008-2024, ZETETIC, LLC`
- No endorsement using the Zetetic/SQLCipher names without permission.
- Community Edition has **no vendor SLA**. FIPS, commercial support, or enterprise
  licensing require a separate procurement decision and must not put a licence key in
  the repository or APK.
- Related notices that may apply through the Android Community stack include SQLite
  (public domain) and LibTomCrypt (public domain). Exact notice text for the shipped
  AAR must be captured from the AAR/`NOTICE` contents during the PoC packaging step
  and placed in Memora's future notices screen—not in user-facing recovery copy.

User-facing recovery copy remains governed by ADR-021 and must never mention
SQLCipher, keys, or encryption failures.

## Vulnerability review (point-in-time)

On 2026-07-24:

1. OSV query for Maven package `net.zetetic:sqlcipher-android` version `4.17.0`
   returned an empty vulnerability set (`{}` / no listed advisories).
2. OSV web search for `sqlcipher-android` under Maven returned no listed advisories.
3. Zetetic's 4.17.0 release notes state the release updates the SQLite baseline to
   incorporate fixes for upstream CVEs that affected earlier versions.

This is useful evidence for the PoC gate, **not** a permanent guarantee. Re-run OSV
and vendor advisories before any production pin or release that ships the library.

## Explicitly rejected approaches

| Approach | Why rejected for Memora |
|---|---|
| `android-database-sqlcipher` | Deprecated API; superseded by `sqlcipher-android`. |
| Copied local native binaries outside Maven provenance | Breaks checksum/provenance review and licence tracking. |
| Hand-written cipher layer or reflection into SQLite internals | Unsupported, untestable against ADR-021 proofs. |
| Cloud key escrow / user password / biometric-every-open default | Conflicts with accepted frictionless local-only recovery model. |

## Acceptance for classpath promotion (Slice 1)

This review **accepts** `net.zetetic:sqlcipher-android:4.17.0` on the production
runtime classpath, provided that:

1. `PersistenceModule` continues to open plaintext `memora.db` until the conversion
   switch gate;
2. AAR hashes are re-verified at promotion time (done 2026-07-24);
3. resolved `androidx.sqlite` / Room versions remain recorded;
4. synthetic PoC/conversion instrumentation suites remain green;
5. no PDF content entity, source access, UI encryption setup screen, WorkManager, AI,
   or network path is added by the promotion;
6. BSD attribution notices ship before any store or sideload release that includes
   the native library.

## Pre-work and delivery record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Sources read:** product registry, Local AI Technical Specification, governance,
  CONTINUE, product contract, architecture, decisions, encrypted-database decision,
  PRD traceability, changelog.
- **Current-code evidence:** `PersistenceModule` opens plaintext `memora.db` with
  Room migrations 1→2 and 2→3; schema version 3; Room `2.8.4`; `minSdk = 26`; no
  SQLCipher coordinate in `libs.versions.toml`.
- **Verification:** Maven Central artifact listing, POM/module metadata, published
  checksums, OSV API query for `4.17.0`, Zetetic licence and 4.17.0 release notes.
- **Known limitation:** No executable PoC exists yet. Hashes and advisories are
  point-in-time and must be rechecked before production adoption.
