# Synthetic PDF Parser Benchmark Plan

**Status:** Initial, expanded, and many-page emulator baselines captured; not a
production-limit decision.
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

- reads only repository-owned synthetic fixtures: existing Base64 fixtures plus
  deterministic PDFs generated entirely in memory for the test;
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
| `generated_text_small` | Generated in memory: one page with 1,024 ASCII UTF-16 code units | One page and at least 1,024 extracted text code units. |
| `generated_text_medium` | Generated in memory: four pages with 2,048 ASCII UTF-16 code units per page | Four pages and at least 8,192 extracted text code units. |
| `generated_text_larger` | Generated in memory: eight pages with 4,096 ASCII UTF-16 code units per page | Eight pages and at least 32,768 extracted text code units. |
| `generated_text_many_pages` | Generated in memory: 32 pages with 2,048 ASCII UTF-16 code units per page | 32 pages and at least 65,536 extracted text code units. |

The generated fixtures are materialized before parser timing begins. They prove a
repeatable progression of input/result sizes without timing Base64 decoding or PDF
generation. They are still not a representative corpus and cannot justify production
limits by themselves.

## Measurements

After one unreported warm-up, each fixture is parsed five times on the emulator. The
harness emits one aggregate-only Logcat line containing:

- fixture ID and measured-run count;
- in-memory input PDF byte count;
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
4. Confirm that **3 tests passed**.
5. Open **Logcat**, filter for `MemoraPdfBenchmark`, and send the six aggregate log
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

## Offline-runtime verification harness

The prepared Android instrumentation class
`PdfParserOfflineRuntimeIntegrationTest` closes the narrow “offline runtime” evidence
gap without broadening the production application. It has exactly two checks:

1. It inspects the installed **release-app** manifest and fails if that manifest
   requests `android.permission.INTERNET`.
2. It asks Android whether the **emulator** has an Internet-capable or validated
   network. Only when neither capability is present does it parse the existing
   repository-owned two-page synthetic fixture and assert its page/text facts.

The production and test-APK manifests are not changed for this test and declare
neither `INTERNET` nor `ACCESS_NETWORK_STATE`. The instrumentation test temporarily
adopts Android's test-shell identity for `ACCESS_NETWORK_STATE` solely while it reads
the emulator's connectivity capabilities, then drops it before parsing. That gives
neither the test nor the app Internet access, and the harness has no network client.
This avoids a less trustworthy user-supplied “offline” flag and avoids expanding
either installed package's capabilities.

The harness does not open a user PDF, read a descriptor/URI/SAF source, bind or change
the isolated service, persist data, touch Room/UI/WorkManager, invoke AI, or log PDF
text or source metadata. If the emulator is connected, it fails **before** invoking
the parser and gives an explicit instruction; that is deliberate, because a passing
test on a connected emulator would not constitute offline evidence.

### How to verify offline runtime

1. In the Medium Phone emulator, open Android Quick Settings by dragging down from
   the top of the phone screen. Turn off **Wi-Fi** and, if it is available, **mobile
   data**. Wait until the status bar no longer shows an active connection.
2. In Android Studio, open `PdfParserOfflineRuntimeIntegrationTest` and click the
   green arrow beside the class name, then choose **Run**.
3. Confirm **2 tests passed**. The parser test is not allowed to run until Android
   reports the offline precondition.
4. Restore Wi-Fi/mobile data afterwards for normal emulator use.

This is a controlled synthetic-only runtime check, not a claim that the full product
is complete offline. No real user PDF becomes eligible for parsing until all remaining
ADR-017 gates and the representative measurement policy are accepted.

## Verified offline-runtime evidence

On 2026-07-22, the Medium Phone emulator ran
`PdfParserOfflineRuntimeIntegrationTest` with Wi-Fi/mobile connectivity disabled:
**2 of 2 tests passed**. The first test confirmed that the installed release app does
not request `android.permission.INTERNET`. The second used Android's temporary
test-shell network-state identity to confirm that no Internet-capable or validated
network was present, dropped that identity, and then parsed only the repository-owned
two-page synthetic fixture successfully.

No real PDF, descriptor, URI, SAF source, Room data, service request, UI action,
WorkManager work, AI call, or network request occurred. This confirms the narrow
offline-runtime gate for the existing deterministic parser; it does not establish
offline behavior for future real-source access, isolated-service parsing, persistence,
or semantic understanding.

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

## Verified expanded emulator baseline

On 2026-07-22, the connected Medium Phone emulator ran the updated
`PdfParserSyntheticBenchmarkIntegrationTest`: **3 of 3 tests passed**. This capture
materialized each test fixture before timing parser work and emitted six
aggregate-only Logcat lines:

| Fixture | Runs | Input PDF bytes | Pages | Text code units | Result Bundle bytes | Parser elapsed time (min / median / max) |
|---|---:|---:|---:|---:|---:|---:|
| `two_page_selectable` | 5 | 23,102 | 2 | 94 | 804 | 35 / 41 / 130 ms |
| `image_only` | 5 | 3,302 | 1 | 0 | 308 | 5 / 6 / 14 ms |
| `generated_text_small` | 5 | 873 | 1 | 1,024 | 2,500 | 58 / 79 / 89 ms |
| `generated_text_medium` | 5 | 2,216 | 4 | 8,192 | 17,328 | 377 / 492 / 679 ms |
| `generated_text_larger` | 5 | 4,106 | 8 | 32,768 | 67,136 | 493 / 547 / 1,139 ms |
| `generated_text_many_pages` | 5 | 14,814 | 32 | 65,536 | 136,608 | 981 / 1,692 / 2,067 ms |

The generated PDFs are test fixtures, not user content; their 1/4/8/32-page and
1,024/2,048/4,096-code-unit shapes are test cases rather than proposed production
caps. The earlier two-fixture baseline used a different timing method and has no
input-byte figures, so it must not be numerically compared with this capture. Neither
capture establishes memory, battery, thermal, process-isolation, offline, or
production-limit evidence. The many-page generator also guarantees a non-whitespace
final character on each synthetic page because deterministic extraction trims source
text; this keeps the intended fixture text length stable without changing parser
behavior.

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
- **Smallest safe change:** document this measurement plan and add synthetic-only
  Android harnesses. The offline harness temporarily adopts and drops Android's
  test-shell `ACCESS_NETWORK_STATE` identity so it can reject a connected emulator;
  both manifests, the service, source access, Room, UI, WorkManager, AI, and
  dependency graph remain unchanged.
- **Acceptance criteria:** the harness reports only the defined aggregate metrics,
  verifies stable non-timing facts over repeated runs, and passes on the Medium Phone
  emulator without access to any user source.
- **Verification result:** the connected Medium Phone emulator completed the initial
  2 of 2 tests, then the latest expanded 3 of 3 tests on 2026-07-22. Both runs emitted
  only the aggregate metrics recorded above; the latest expanded run materialized
  inputs before timing and verified progressive input/text/result-size growth through
  a 32-page, 65,536-code-unit fixture. The Medium Phone emulator then completed the
  dedicated offline harness with 2 of 2 tests passing after its network was disabled.
