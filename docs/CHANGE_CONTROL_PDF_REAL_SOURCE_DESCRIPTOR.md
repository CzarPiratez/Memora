# Change Control: Real-Source SAF PDF Descriptor Path

**Date:** 2026-07-25
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrail:** ADR-017.

## Delivered

- `ApprovedPdfFingerprintRevalidationContract` + broker pre-open check.
- `StaleSource` outcomes through parsing/assembly/persistence eligibility.
- Live `ParseApprovedPdfWithIsolatedParserRealSourceIntegrationTest` under the
  user-approved SAF tree (create fixture → open → status-only → delete).

## Not delivered

- Production UI parse button wiring, searchable Room writes from this path,
  WorkManager, AI, network.

## Verification

- Unit: fingerprint contract **3 of 3**.
- Medium Phone emulator: broker + synthetic parse + real-source **11 of 11**.
