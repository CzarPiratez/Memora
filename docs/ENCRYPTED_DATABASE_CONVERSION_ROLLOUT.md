# Encrypted Database Production Conversion Rollout

**Status:** Accepted design gate. This document authorizes **no** production
`PersistenceModule` change, dependency promotion, or PDF content persistence.

**Date:** 2026-07-24  
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.  
**Governing documents:** ADR-020, ADR-021, `docs/ENCRYPTED_DATABASE_DECISION.md`,
`docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md`,
`docs/ENCRYPTED_DATABASE_POC_PLAN.md`.

## Purpose and boundary

Define the acceptance criteria, crash-resume rules, BSD attribution obligations, and
rollback procedure required before Memora may switch production Room opening from
plaintext `memora.db` to a SQLCipher-encrypted database with a Keystore-wrapped
passphrase.

This rollout plan is deliberately separate from implementation. A later, explicitly
approved engineering step must implement the switch. Until then:

- `PersistenceModule` continues to open plaintext `memora.db`;
- SQLCipher remains `androidTestImplementation` only (or an equally non-production
  scope until the switch step);
- no PDF extraction content is written to Room;
- no encryption setup UI, password, or biometric-every-open prompt is introduced;
- user-facing recovery copy never mentions SQLCipher, keys, or encryption failures.

## Evidence already verified

| Gate | Evidence (2026-07-24) |
|---|---|
| Direction | ADR-021 accepted; frictionless UX and recovery wording recorded |
| Provenance | `net.zetetic:sqlcipher-android:4.17.0` hashes + point-in-time OSV empty set |
| Synthetic open/reopen | `EncryptedDatabasePocIntegrationTest` **7/7** on Medium Phone |
| Synthetic conversion | `PlaintextToEncryptedConversionIntegrationTest` **5/5** on Medium Phone |
| Production path untouched | `PersistenceModule` still targets `memora.db` without SQLCipher |
| Offline permission | App still requests no `INTERNET` |

## Remaining proofs before production switch

These are **blocking** for any release that opens encrypted `memora.db` by default:

1. **Dependency promotion plan:** move SQLCipher from test-only scope to the production
   runtime classpath with re-verified AAR SHA-256, resolved Room/`androidx.sqlite`
   versions, and a fresh OSV/advisory check on the pin date.
2. **Production-named conversion harness:** same copy-and-validate algorithm against
   disposable instrumentation databases that use the real production file naming
   strategy (`memora.db` / encrypted candidate / retained plaintext sidecar), still
   without changing the live Hilt provider default until its own step.
3. **Live process-death:** crash or kill while `ROWS_COPIED` or `SWITCH_PENDING` and
   prove resume preserves plaintext or completes exactly once.
   - **Partial (2026-07-24):** simulated prepare-stop + resume covered by
     `ConversionProcessDeathResumeIntegrationTest` **4/4** (including mid-finalize
     file layouts). Opener finalize resume paths landed.
   - **Still open:** real process kill/crash during conversion, then cold resume.
4. **Low-storage / interruption:** conversion denial leaves plaintext intact and
   exposes `CONVERSION_VALIDATION_FAILED` (content-free).
5. **Device unlock / credential-encrypted storage:** after reboot, deferred open until
   user unlock; no second database, no destructive reset.
6. **Physical device:** at least one `arm64-v8a` device proves native load, conversion,
   reopen, and wrong-passphrase denial.
7. **BSD attribution UI/docs:** user-accessible Open-source notices include the
   required Zetetic copyright, licence text, and disclaimer before any store or
   sideload release that ships SQLCipher.
8. **Recovery copy review:** accessibility and plain-language review of the approved
   rebuild message only.
9. **Performance/battery budget:** content-free timing buckets for conversion of
   representative schema-v3 sizes; set limits before PDF text persistence begins.
10. **Clear-derived-data:** ~~user-confirmed clear removes only Memora-owned encrypted
    data and wrapper/journal state; originals and Android grants remain untouched.~~
    **done 2026-07-24** (`ClearMemoraDerivedData` + welcome confirm UI;
    `ClearMemoraDerivedDataIntegrationTest` 1/1).

## Production `PersistenceModule` switch acceptance criteria

The switch is accepted only when **all** of the following are true:

### A. Open path

1. Production `provideMemoraDatabase` loads `sqlcipher` once, unwraps the Keystore-
   protected passphrase, and opens through `SupportOpenHelperFactory`.
2. Fresh installs create the encrypted database directly (no plaintext `memora.db`).
3. Upgrades with an existing plaintext `memora.db` run the conversion state machine
   before serving repository traffic.
4. No Compose screen, setup wizard, or permission dialog is added for encryption.

### B. Conversion algorithm (binding)

Use the staged algorithm already proven synthetically:

```text
NOT_STARTED
  -> PLAINTEXT_VALIDATED
  -> CANDIDATE_CREATED
  -> ROWS_COPIED
  -> CANDIDATE_VALIDATED
  -> SWITCH_PENDING   // plaintext retained; encrypted reopen proven
  -> COMPLETED        // plaintext DB/WAL/SHM deleted only here
```

On any failure: `FAILED_SAFE`, keep plaintext, delete incomplete encrypted candidate,
allow retry. Never use Room `fallbackToDestructiveMigration` for this conversion.

Required validations before `SWITCH_PENDING`:

- Room schema version matches the exported schema (currently **3**);
- asset / discovery-checkpoint / document-tree-approval counts match;
- full row equality for those tables (or an equivalent deterministic checksum plan
  approved before implementation if row volume makes full equality impractical);
- encrypted reopen succeeds after a closed-database boundary;
- standard SQLite read-only open of the encrypted file fails without rewriting it.

Plaintext files are deleted only in `finalize` after `SWITCH_PENDING` and a successful
encrypted reopen. Until then, the app may fall back to plaintext if the encrypted
candidate is missing or invalid **only while journal phase is before `COMPLETED`**.
After `COMPLETED`, missing/invalid encrypted access becomes a recovery state, not a
silent plaintext recreate over user data.

### C. File and identity rules

| Role | Rule |
|---|---|
| Production DB name | Remain `memora.db` after `COMPLETED` (encrypted file behind that name, or an atomic rename strategy documented in the implementing change) |
| Candidate name | Distinct temporary name during conversion; never dual-write user traffic to both |
| Key alias | Versioned production alias (for example `memora.db.wrap.v1`), separate from PoC aliases |
| Wrapper storage | No-backup private storage only; never backup/transfer |
| Journal | Content-free phase + schema version only |

Exact rename-versus-open-helper strategy is an implementation detail of the switch
step, but it must preserve the “plaintext retained until validated encrypted reopen”
invariant.

### D. Observability

Logs may include only: conversion phase, schema version, coarse elapsed-time bucket,
and `DatabaseSecretFailureCategory`. Never log SQL, keys, URIs, titles, metadata
values, page text, or exception strings that could contain path/content.

## Crash-resume rules

| Journal phase at process start | Required behavior |
|---|---|
| `NOT_STARTED` / `PLAINTEXT_VALIDATED` | Open plaintext if present; begin or continue conversion when idle/safe |
| `CANDIDATE_CREATED` / `ROWS_COPIED` | Delete incomplete encrypted candidate; reset to `NOT_STARTED` or re-enter from `PLAINTEXT_VALIDATED`; plaintext untouched |
| `CANDIDATE_VALIDATED` / `SWITCH_PENDING` | Reopen encrypted; re-validate; finalize plaintext deletion only after success |
| `COMPLETED` | Open encrypted only; never recreate plaintext `memora.db` automatically |
| `FAILED_SAFE` | Open plaintext if present; expose retryable content-free state; do not delete plaintext |

Concurrent writers are forbidden during conversion. WorkManager indexing (when later
enabled) must observe a “database unavailable / conversion in progress” gate rather
than opening a second helper.

## BSD attribution gate

Before any release APK that includes `sqlcipher-android`:

1. Ship a user-accessible **Open-source notices** surface (Settings/About or
   equivalent) that includes:
   - `Copyright (c) 2008-2024, ZETETIC, LLC`
   - the SQLCipher Community Edition licence text and disclaimer
   - required notices for bundled cryptographic/SQLite components as shipped in the
     AAR
2. Keep attribution out of recovery/error copy. Recovery remains:

   > Your private Memora index needs to be rebuilt. Your original photos, documents,
   > and notes are unchanged.

3. Do not use Zetetic/SQLCipher names to endorse Memora.
4. Re-check licence text against the pinned AAR at release time; commit the reviewed
   notice text or a generated notices artifact under version control.

This gate is independent of conversion success: shipping the native library without
notices is a release blocker even if encryption works.

## Rollback procedure

### Before `COMPLETED`

- Leave plaintext authoritative.
- Delete incomplete encrypted candidate and, if needed, reset journal to
  `FAILED_SAFE` or `NOT_STARTED`.
- No store rollback is required; the device remains on plaintext.

### After `COMPLETED` (encrypted is authoritative)

- App downgrade to a plaintext-only build is **not safe** and is unsupported for the
  same install that completed conversion.
- Supported recovery is user-confirmed **Clear Memora derived data and re-index**,
  which removes Memora-owned DB/wrapper/journal state only.
- Release rollout should use staged cohorts and a kill-switch only if it can disable
  *new* conversion for upgrades still on plaintext. It must not attempt to decrypt
  and rewrite an already-`COMPLETED` database to plaintext in the field.

### Engineering rollback during the switch PR

- Revert the `PersistenceModule` change.
- Keep synthetic PoC/conversion tests green.
- Do not delete provenance, ADR, or rollout documentation.

## Staged release checklist

1. Internal emulator + physical-device conversion matrix green.
2. Notices screen reviewed and present in release builds.
3. Staged rollout cohort (small → wider).
4. Content-free conversion success/failure counters only (no user content).
5. Support playbook uses the approved recovery wording exclusively.
6. Separate product approval still required before PDF page-text persistence
   (ADR-020 / ADR-017 remain binding).

## Explicit non-goals of this document

- Changing `PersistenceModule` encrypted open in this documentation step
- Enabling PDF extraction Room entities or searchable stored text
- Enabling WorkManager, AI, network, or cloud key escrow
- Claiming FIPS, commercial SQLCipher, or enterprise SLA coverage

Note (2026-07-24): Slice 1 promoted SQLCipher to the production classpath while
keeping plaintext `PersistenceModule` open. Encrypted open and notices UI were later
gates (completed the same day).

## Next approved engineering step after this document

Implement production-named synthetic conversion against disposable files, then wire
`PersistenceModule` encrypted open + conversion journal, and ship notices before
release:

1. ~~promote SQLCipher with re-verified provenance for the switch change set;~~
   **done 2026-07-24 (classpath only; plaintext open retained)**
2. ~~production-named instrumentation conversion against disposable files;~~
   **done 2026-07-24 (`ProductionNamedConversionIntegrationTest` 3/3)**
3. ~~wire `PersistenceModule` to encrypted open + conversion journal;~~
   **done 2026-07-24 (`MemoraEncryptedDatabaseOpener` + `PersistenceModule`)**
4. ~~ship notices surface before any release that includes the native library;~~
   **done 2026-07-24 (welcome → Open-source licenses / `res/raw/open_source_notices.txt`)**

Remaining proofs: **live** process-death (real kill/crash), physical-device conversion,
and low-storage failure. Simulated process-death instrumentation and clear-derived-data
are done (2026-07-24). PDF content persistence stays blocked until its own
privacy/resource gates pass.

## Pre-work and delivery record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Sources read:** product registry, Local AI Technical Specification, governance,
  CONTINUE, product contract, architecture, decisions, encrypted-database decision,
  provenance review, PoC plan, conversion harness results, PRD traceability.
- **Current-code evidence (updated 2026-07-24):** `PersistenceModule` opens encrypted
  `memora.db` via `MemoraDatabaseHandle` / `MemoraEncryptedDatabaseOpener`; notices UI
  and Clear Memora index recovery UX ship; opener, clear, and **simulated**
  process-death resume instrumentation cover the path.
- **Affected layers:** data/security, DI, welcome UI notices/clear, documentation.
- **Verification:** opener + clear + simulated process-death instrumentation; notices
  text committed under version control from Zetetic Community Edition licence URL.
- **Known limitation:** live crash/kill, physical-device, and low-storage proofs remain
  open; PDF content persistence remains blocked.
- **Honesty correction (2026-07-24):** earlier “done” marking for live process-death
  was overstated; corrected to partial/simulated only.
