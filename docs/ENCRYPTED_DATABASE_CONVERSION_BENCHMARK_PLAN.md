# Encrypted database conversion performance budget

**Status:** Initial emulator baseline captured 2026-07-25; provisional budgets set.  
**Date:** 2026-07-25  
**Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.  
**Governing documents:** ADR-021, `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md`
proof #9, `docs/ENCRYPTED_DATABASE_DECISION.md`.

## Purpose

Measure **content-free** elapsed-time buckets (and optional charge-counter deltas) for
plaintext → encrypted schema-v3 conversion at representative synthetic sizes. Set
provisional ceilings before PDF text persistence begins. This does **not** authorize
PDF persistence, WorkManager, AI, or network.

## Controlled boundary

The Android instrumentation harness:

- uses disposable benchmark database / Keystore / journal names only;
- seeds synthetic schema-v3 rows (assets, discovery checkpoints, document-tree
  approvals) with fixture markers — no user content, URIs that identify real files,
  or source text;
- runs the existing copy-and-validate + finalize harness;
- logs only aggregate metrics (`MemoraConversionBenchmark`);
- never logs fingerprints, locations, display names, SQL, keys, or exception text;
- deletes all benchmark files in tearDown.

## Size buckets

| Bucket ID | Assets | Checkpoints | Approvals | Provisional convert+finalize ceiling |
|---|---:|---:|---:|---|
| `empty` | 0 | 0 | 0 | 5_000 ms |
| `small_100` | 100 | 5 | 5 | 15_000 ms |
| `medium_1000` | 1_000 | 20 | 20 | 60_000 ms |
| `large_5000` | 5_000 | 50 | 50 | 180_000 ms |

Elapsed-time log buckets (coarse): `lt_1s`, `1s_to_5s`, `5s_to_15s`, `15s_to_60s`,
`60s_to_180s`, `gte_180s`.

## Measurements

After one unreported warm-up (empty convert), each size bucket is converted once on
the Medium Phone emulator. The harness emits one aggregate Logcat line with:

- bucket ID;
- row counts (assets / checkpoints / approvals);
- plaintext file bytes before convert;
- convert+finalize elapsed milliseconds and coarse bucket;
- optional `charge_counter_delta_uah` when Android exposes
  `BATTERY_PROPERTY_CHARGE_COUNTER` (otherwise `unavailable`).

Timing is an observed development budget, not a user-facing SLA. Battery deltas are
informational on emulators and must not alone justify product claims.

## How to capture

1. Start the Medium Phone emulator and keep it otherwise idle.
2. Run `ConversionPerformanceBenchmarkIntegrationTest`.
3. Confirm all bucket tests passed under the provisional ceilings.
4. Filter Logcat for `MemoraConversionBenchmark` and record aggregate lines in the
   delivery note.

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Verification:** Medium Phone emulator
  `ConversionPerformanceBenchmarkIntegrationTest` **4 of 4 passed**; unit
  `ConversionElapsedBucketsTest` **1 of 1**.
- **Measured aggregates (Logcat `MemoraConversionBenchmark`):**

| Bucket | Plaintext bytes | Elapsed ms | Coarse bucket | Charge delta µAh |
|---|---:|---:|---|---|
| `empty` | 49_152 | 4_207 | `1s_to_5s` | 0 |
| `small_100` | 61_440 | 3_221 | `1s_to_5s` | 0 |
| `medium_1000` | 258_048 | 4_084 | `1s_to_5s` | 0 |
| `large_5000` | 1_093_632 | 7_277 | `5s_to_15s` | 0 |

- **Provisional ceilings retained:** empty 5s, small 15s, medium 60s, large 180s
  (all measured times well under ceilings except empty near 5s due to Keystore /
  SQLCipher open overhead on a cold path).
- **Truthfulness:** Emulator charge-counter deltas are informational only. No PDF
  content write, WorkManager, AI, or network path was added.

