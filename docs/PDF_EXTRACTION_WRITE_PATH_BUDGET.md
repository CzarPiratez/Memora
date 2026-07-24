# PDF extraction write-path resource budget

**Status:** Initial emulator baseline captured 2026-07-25; provisional hard limits set.  
**Date:** 2026-07-25  
**Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.  
**Governing documents:** ADR-020, ADR-022, `PDF_EXTRACTION_DATA_PERSISTENCE_DESIGN.md`.

## Purpose

Measure content-free write-path cost for durable PDF extraction rows and set
provisional hard limits with a deterministic overflow outcome (`FailedSafely`, no
partial rows). This does **not** authorize real-source parsing, production UI wiring,
WorkManager, AI, or network.

## Provisional hard limits (`PdfExtractionWriteBudgets`)

| Limit | Value |
|---|---:|
| Max pages | 32 |
| Max chars per page | 8_192 |
| Max total chars | 65_536 |
| Max metadata entries | 32 |
| Max metadata name / value chars | 128 / 2_048 |
| Max title chars | 512 |
| Max single persist elapsed (emulator ceiling) | 15_000 ms |

These match the temporary synthetic parser envelope until a later measured production
policy revises them.

## Size buckets measured

| Bucket | Shape |
|---|---|
| `no_text` | 4 pages, no page rows, `NO_EXTRACTABLE_TEXT` |
| `blank_pages` | 8 complete blank pages + 1 metadata entry |
| `dense_text` | 8 pages × 4_096 chars + 2 metadata entries |
| `max_envelope` | 32 pages filling total-char budget + 32 metadata entries |
| `overflow_pages` | page count over limit → `FailedSafely`, zero rows |

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Verification:** Unit `PdfExtractionWriteBudgetsTest` **3 of 3**; Medium Phone
  `PdfExtractionWritePathBenchmarkIntegrationTest` **5 of 5**.
- **Measured aggregates (Logcat `MemoraPdfWriteBenchmark`):**

| Bucket | Pages | Chars/page | Elapsed ms | Coarse bucket |
|---|---:|---:|---:|---|
| `no_text` | 4 | 0 | 304 | `lt_1s` |
| `blank_pages` | 8 | 0 | 281 | `lt_1s` |
| `dense_text` | 8 | 4_096 | 862 | `lt_1s` |
| `max_envelope` | 32 | 2_048 | 255 | `lt_1s` |

Overflow page-count case returned `FailedSafely` with zero rows. Emulator
`db_bytes_*` on the main file can under-report while WAL is active; elapsed and
heap deltas are the primary content-free signals here.
- **Truthfulness:** No real PDF parse, production UI wiring, WorkManager, AI, or
  network. ADR-017 real-source gates remain.
