# Synthetic PDF Parser Benchmark Plan

**Status:** Initial emulator baseline captured; not a production-limit decision.  
**Date:** 2026-07-22  
**Requirements:** P-07, P-14, P-15, P-17; Local-AI principles A-01, A-02, A-06.

## Purpose

This plan provides the first reproducible measurement baseline required before the
private isolated PDF parser may return page text. It does **not** authorize a service
protocol change, a production parser-result limit, real-PDF access, persistence, or
user-visible indexing. Its results are development evidence only.

The governing safety requirement is ADR-017: input/output, elapsed-time, memory,
battery, and thermal budgets must be measured on a representative device tier before
Memora opens a real user PDF. The Local AI Technical Specification also prohibits
unsubstantiated performance claims and requires privacy-safe fixtures and result
quality metrics.

## Controlled benchmark boundary

The Android instrumentation harness:

- reads only repository-owned, Base64-encoded synthetic `InputStream` fixtures;
- invokes the existing deterministic PDF parser in the test process;
- constructs an in-memory, future version-one result `Bundle` solely to measure its
  serialized size;
- writes no file, database row, cache, source reference, telemetry event, or
  benchmark result to persistent storage;
- does not bind the isolated service, open a descriptor, access a SAF URI, request a
  permission, invoke AI, or use the network; and
- logs only aggregate metrics. It never logs text, title, metadata, URI, path,
  password, asset/source identifier, or parser exception.

The harness is deliberately separate from the current status-only Binder service. A
passing benchmark does not enable page-text transport.

## Current fixture baseline

| Fixture ID | Repository-owned input | Expected aggregate facts |
|---|---|---|
| `two_page_selectable` | Existing two-page selectable-text test PDF | Two pages and a non-zero text code-unit count. |
| `image_only` | Existing image-only test PDF | One page and zero extracted text code units. |

These tiny fixtures prove the measurement path and no-text handling. They are not a
representative corpus, and they cannot justify production limits by themselves.

## Measurements

After one unreported warm-up, each fixture is parsed five times on the emulator. The
harness emits one aggregate-only Logcat line containing:

- fixture ID and measured-run count;
- page count;
- extracted text length in UTF-16 code units;
- serialized byte count of the in-memory future result `Bundle`; and
- local parser elapsed-time median, minimum, and maximum in milliseconds.

The harness asserts that page count, text length, and serialized result size are
stable across measured runs. Timing is intentionally reported as an observed range,
not an SLA.

## How to capture the baseline

1. Start the Medium Phone emulator and keep it otherwise idle.
2. In Android Studio, open `PdfParserSyntheticBenchmarkIntegrationTest`.
3. Click the green arrow beside the class name, then choose **Run**.
4. Confirm that **2 tests passed**.
5. Open **Logcat**, filter for `MemoraPdfBenchmark`, and send the two aggregate log
   lines to the engineering record. Do not share any raw PDF content because the
   harness is designed not to emit it.

## Evidence still required before a production policy

Before the result contract receives production values or the service returns page
text, Memora must add a representative, privacy-safe corpus and record measurements
for the approved device tier. That later corpus must cover progressively larger valid
text PDFs, many-page PDFs, blank/no-text PDFs, malformed input, password-protected
input, cancellation, timeout behavior, memory pressure, battery/thermal behavior,
and offline runtime. It must also define the accepted input-size, page-count,
per-page-text, total-text, chunk, elapsed-time, and recovery policy with evidence.

The separate ADR-017 gates for live in-flight process death, offline runtime,
fresh-grant validation, real source access, atomic persistence, and user-visible
pause/retry/recovery remain open.

## Verified initial emulator baseline

On 2026-07-22, `PdfParserSyntheticBenchmarkIntegrationTest` completed on the
connected Medium Phone emulator (Android 17): **2 of 2 tests passed**. The
aggregate-only Logcat output was:

| Fixture | Runs | Pages | Text code units | Result Bundle bytes | Parser elapsed time (min / median / max) |
|---|---:|---:|---:|---:|---:|
| `two_page_selectable` | 5 | 2 | 94 | 804 | 170 / 340 / 576 ms |
| `image_only` | 5 | 1 | 0 | 308 | 116 / 175 / 268 ms |

This is a reproducible sanity baseline for the measurement harness only. It does not
measure file-size limits, larger/many-page documents, memory, battery, thermal
impact, process-isolation overhead, or offline behavior. It must not be converted
into a production cap, an SLA, or user-facing copy.

## Change-control record

- **Source documents read:** product registry, Local AI Technical Specification,
  governance, continuation record, product contract, architecture, decisions,
  roadmap, traceability, PDF extraction plan, parser-isolation review, and
  change-control template.
- **Current-code evidence:** the parser accepts only an `InputStream`; synthetic PDF
  fixtures live exclusively in Android tests; the private service is status-only;
  the future result contract and strict Bundle decoder already reject malformed data.
- **Open decisions and platform limitations:** ADR-003 is unrelated and remains
  open. No measured production limit exists. Image-only PDF OCR remains a separate
  local-AI capability.
- **Smallest safe change:** document this measurement plan and add a synthetic-only
  Android harness. No service, source, Room, UI, WorkManager, AI, dependency, or
  manifest change is permitted.
- **Acceptance criteria:** the harness reports only the defined aggregate metrics,
  verifies stable non-timing facts over repeated runs, and passes on the Medium Phone
  emulator without access to any user source.
- **Verification result:** the connected Medium Phone emulator completed 2 of 2
  tests on 2026-07-22 and emitted only the aggregate metrics recorded above.
