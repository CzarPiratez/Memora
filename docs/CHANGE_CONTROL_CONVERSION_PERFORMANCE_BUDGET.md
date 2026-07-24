# Change-control: Encrypted conversion performance budget

## Pre-work record

- **Requirement IDs:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` proof #9,
  `docs/ENCRYPTED_DATABASE_DECISION.md`, PDF benchmark plan (pattern only), ADR-021.
- **Current-code evidence:** Conversion harness and production opener are verified;
  no conversion timing/battery budget instrumentation or recorded ceilings yet.
- **Open ADRs:** PDF persistence blocked (ADR-017/020). No WorkManager/AI/network.
- **Privacy impact:** Synthetic fixture rows only; aggregate Logcat metrics; tearDown
  deletes benchmark DB/wrapper/journal state.
- **Smallest safe change:** Disposable benchmark harness + plan + provisional ceilings;
  no production open-path behavior change.
- **Acceptance criteria:**
  1. Four size buckets convert+finalize successfully.
  2. Content-free aggregate logs only.
  3. Elapsed time under provisional ceilings.
  4. Docs record plan, ceilings, and measured results.
  5. No PDF persistence, WorkManager, AI, or network.
- **Test plan:** Medium Phone `ConversionPerformanceBenchmarkIntegrationTest`.

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Code:** `ConversionElapsedBuckets`; disposable
  `ConversionPerformanceBenchmarkIntegrationTest` (four schema-v3 size buckets).
- **Verification:** Unit **1/1**; Medium Phone emulator benchmark **4/4** under
  provisional ceilings (see benchmark plan measured table).
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
- **Remaining after this slice:** Encrypted-database conversion rollout proof #9 is
  closed. PDF text persistence remains blocked (ADR-017 / ADR-020).

