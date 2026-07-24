# Change-control: Device-unlock deferred database open

## Pre-work record

- **Requirement IDs:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` proof #5,
  `docs/ENCRYPTED_DATABASE_DECISION.md` (normal operation and device lock),
  ADR-021, product contract / architecture / traceability (encryption path).
- **Current-code evidence:** `MemoraDatabaseHandle` eagerly opens encrypted DB at
  construction; opener has no `UserManager.isUserUnlocked` gate; no structured
  deferred state; no calm unlock UI. Clear / recovery copy already uses ADR-021
  rebuild wording only.
- **Open ADRs:** PDF persistence blocked (ADR-017/020). Performance budget and
  formal recovery-copy a11y review remain after this slice.
- **Privacy impact:** Defers CE-backed open until unlock; never creates a second DB,
  never destructive-resets on lock. No source mutation. No WorkManager/AI/network.
- **Smallest safe change:** Injectable unlock gate; opener refuses open while locked
  without mutating files; handle opens lazily and exposes waiting state; calm unlock
  UI; instrumentation proves deferred open + successful open after unlock.
- **Acceptance criteria:**
  1. Locked gate: no new DB/wrapper/candidate; existing files unchanged; category
     `WAITING_FOR_USER_UNLOCK`.
  2. No second database created while locked.
  3. After unlock (gate allows), open succeeds with prior fixture rows.
  4. UI shows plain-language wait copy (no SQLCipher/keys/encryption jargon).
  5. No PDF persistence, WorkManager, AI, or network.
- **Test plan:** Instrumentation on Medium Phone emulator with injectable locked
  then unlocked gate.

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Code:** `UserCredentialUnlockGate` / `WAITING_FOR_USER_UNLOCK`; opener refuses open
  while locked without mutating files; `MemoraDatabaseHandle` lazy open + availability;
  calm unlock UI; recovery/unlock copy guards.
- **Verification:** Unit `ClearMemoraDerivedDataCopyTest` **2 of 2**; Medium Phone
  emulator `DeviceUnlockDeferredOpenIntegrationTest` **2 of 2**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
- **Remaining after this slice:** Performance/battery budget (and any other open
  rollout checklist items).

