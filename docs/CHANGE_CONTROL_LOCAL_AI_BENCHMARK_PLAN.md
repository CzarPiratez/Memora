# Change Control: Local-AI Benchmark Plan

**Date:** 2026-07-25
**Requirements:** A-01, A-02, A-05, A-06, A-07, E-06; Local AI Technical Spec §11, §13.6.
**Decision guardrails:** ADR-012, ADR-023, ADR-024; ROADMAP Local-AI architecture gate.

## Pre-work record

- **Requirement IDs:** A-01, A-05, E-06 (plan + metric claim contracts only).
- **Source documents read:** Local AI spec §11/§13, governance, CONTINUE, ADR-012,
  compatibility policy, AI Pack plan, roadmap, prior conversion benchmark pattern.
- **Current-code evidence inspected:** No Local-AI harness; conversion benchmark
  pattern exists for privacy-safe aggregate logging.
- **Dependencies added:** None. No models/network.
- **Privacy:** Synthetic fixtures only; no user content corpus in this plan.
- **Smallest safe change:** Benchmark plan document; domain metric/claim types;
  unit tests; docs/ADR.
- **Acceptance criteria:** Plan defines metrics and privacy rules; claims cannot
  publish without measured evidence; no inference harness in this slice.

## Delivery record

- **Files/layers changed:** `docs/LOCAL_AI_BENCHMARK_PLAN.md`; ADR-025; domain
  `LocalAiBenchmarkContracts.kt`; unit tests; CHANGELOG; CONTINUE; ROADMAP;
  PRD_TRACEABILITY; this change-control; companion compatibility/fallback docs.
- **Automated verification and result:** On 2026-07-25,
  `:app:testDebugUnitTest --tests com.memora.app.domain.intelligence.*` passed
  (including LocalAiBenchmarkContractsTest). No new AI/OCR/network Gradle deps.
- **Emulator/manual verification and result:** Not required (docs + pure domain).
- **Failure/recovery paths verified:** Unmeasured claims cannot publish as release
  promises; blank tier/corpus ids rejected.
- **Known limitation or follow-up:** Measured pack baselines require a future
  implementation slice; A-01 remains Planned.
- **Documentation/traceability/ADR updates:** ADR-025; ROADMAP planning complete.
- **Git commit:** _(pending)_
