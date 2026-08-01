# Change Control — Local-AI measured pack baselines

**Date opened:** 2026-08-02  
**Status:** **L0 opened** (docs-only phase plan). Awaiting user acceptance before L1 code.  
**Requirements:** A-01, A-03, A-05, A-07, E-06; Local AI Technical Spec §6, §10, §11, §13.  
**Decision guardrails:** ADR-012, ADR-023, ADR-024, ADR-025. Notes connector N0–N7
is closed and is **not** a substitute for on-device intelligence readiness.

## Pre-work record

- **Sources read:** Product registry, Local AI Technical Specification, GOVERNANCE,
  CONTINUE, ROADMAP, LOCAL_AI_BENCHMARK_PLAN, AI Pack delivery/security plan,
  compatibility/fallback policy, ADR-023/024/025, Notes change-control close.
- **Current-code evidence inspected:** Domain intelligence contracts under
  `domain/intelligence/` (engines, `AiPackManager`, benchmark claim rules,
  `Unavailable*` stubs). No pack bytes, download UI, inference harness, or Hilt
  engine binds for real models.
- **Open limitations:** A-01 offline end-to-end intelligence remains Planned.
  No user-facing `AVAILABLE` claim is authorized by this open record.
- **Privacy / network / offline / retention:**
  - Synthetic or licensed test fixtures only — never personal photos/PDFs/notes.
  - Logs are aggregate metrics only (counts, durations, coarse buckets).
  - Core memory create/retrieve/rank/explain must remain local after a pack is
    installed; this track must not introduce cloud inference for core paths.
  - Failed pack verify must not invent ACTIVE/AVAILABLE.
- **Smallest safe change for this open record:** documentation only — phase plan,
  honesty gates, L1 acceptance criteria. No model bytes, INTERNET for packs, or
  inference code in L0.
- **Acceptance for opening L0:** User accepts the phase plan; CONTINUE points here;
  next code is L1 only after that acceptance.

## Honest product framing (must stay true)

- Keyword recall (PDF / screenshot / photo / note) and pre-AI Asset Memories are
  **not** Local Intelligence AVAILABLE.
- Planning targets (e.g. latency under 300 ms) are **not** release promises until
  measured with fixture + tier IDs (`LocalAiBenchmarkRules`).
- Emulator baselines do not authorize midrange-device marketing claims.

## Phased delivery (approve each phase before code)

| Phase | Goal | Ships code? | Hard gate |
|-------|------|-------------|-----------|
| **L0** (this) | Open change-control + phase plan | Docs only | User accepts plan |
| **L1** | Synthetic pack fixture + aggregate size/integrity harness on `emulator_medium_phone` | Yes — test fixtures + instrumented verify path; engines remain Unavailable | No AVAILABLE claim; synthetic only; privacy-safe aggregates |
| **L2** | Broader offline/integrity checks on emulator; draft compatibility notes | Yes — still no user-facing AVAILABLE | Emulator-only evidence; midrange claims deferred |
| **Later** | User-facing AVAILABLE + midrange measured claims | Separate change-control | ADR-024 matrix + A-01 proof |

**Recommended next code slice after L0 approval:** **L1 only**.

## L1 sketch (not authorized until L0 accepted)

- Disposable synthetic pack bytes + complete manifest fields owned for testing.
- Integrity success / hash-failure outcomes recorded as aggregates.
- Engines and support policy stay Unavailable / unsupported by default.
- Record results in this change-control; no product UI claiming AI is ready.

## Acceptance record — L0 (pending user pass)

- **Delivered:** This file; CONTINUE / CHANGELOG / ROADMAP / benchmark-plan pointer
  updated; Notes track closed in CONTINUE.
- **Not delivered:** Pack bytes, harness, download UI, inference, AVAILABLE UI.
