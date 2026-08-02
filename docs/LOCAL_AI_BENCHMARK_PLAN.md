# Local-AI Benchmark Plan

**Status:** Accepted for Local-AI architecture gate planning (2026-07-25).  
**Requirements:** A-01, A-02, A-03, A-05, A-06, A-07, E-06; Local AI Technical Spec
§11, §13.6.  
**Governing decisions:** ADR-012, ADR-023, ADR-024, ADR-025.  
**Does not authorize:** shipping model packs, claiming latency/battery/storage
numbers, or marking on-device understanding ready.

## Purpose

Define how Memora will measure Local Intelligence quality, performance, battery,
storage, cancellation, and offline behaviour **before** any release claim. No
arbitrary pack-size, RAM, battery, or latency number may be accepted without
evidence from this plan (Spec §11).

## Privacy-safe corpus rules

- Fixtures are synthetic or explicitly licensed sample assets owned for testing.
- No real user photos, PDFs, notes, or Memories from a personal device catalogue.
- Logs record aggregate metrics only (counts, durations, coarse buckets). Never log
  source text, filenames that identify people, URIs, fingerprints, or exception
  text that embeds paths.
- Benchmark databases and pack staging use disposable names and are deleted in
  tearDown (same pattern as encrypted conversion benchmarks).

## Device tiers under test

| Tier ID | Host | Role |
|---|---|---|
| `emulator_medium_phone` | Medium Phone emulator | Development smoke / CI when applicable |
| `midrange_arm64` | Physical arm64 midrange | Primary claim host once packs exist |
| `low_ram` | Constrained physical or emulator config | Proves UNAVAILABLE / degraded honesty under pressure |

A metric is not a release promise until it has been measured on the claimed tier.

## Metric families

### Quality (when a capability is AVAILABLE)

| Metric | Notes |
|---|---|
| Recall@k for stored Memories | Against fixture queries |
| Unsupported-claim rate | Explain Mode invents a reason → fail |
| False-link rate | Future Event/Knowledge links only |
| Explanation coverage | Result has citable evidence |
| Calibration / overconfident-error rate | Required if confidence is shown (E-06) |

### Performance / device health

| Metric | Notes |
|---|---|
| Stored-memory search latency | Spec planning target under 300 ms — not accepted until measured |
| Per-asset understanding elapsed | Coarse buckets only in logs |
| Pack download size / on-disk size | Compared to disclosed manifest fields |
| Peak RSS / low-memory outcomes | Must not thrash; prefer UNAVAILABLE |
| Battery charge-counter delta when available | Informational; emulator deltas alone are not product claims |
| Thermal / cancellation | Work stops when Android cancels; UI shows paused/retryable |

### Offline / integrity

| Check | Expected |
|---|---|
| Airplane mode after pack installed | Create/retrieve/rank/explain still work |
| Failed pack hash | Prior known-good retained or UNAVAILABLE |
| No network in core path | No remote inference calls |

## Evidence classes under evaluation

Benchmarks must distinguish Direct facts, Validated observations, Retrieval signals,
and Hypotheses (Spec §11). A retrieval signal must never be scored as proof of a
Direct claim.

## When to run

1. **Gate planning (this document):** define metrics and privacy rules only.
2. **First pack implementation slice:** capture baseline on `emulator_medium_phone`
   with synthetic fixtures; record aggregates in change-control.
3. **Before any user-facing AVAILABLE claim:** repeat on `midrange_arm64`; fill
   compatibility matrix rows (ADR-024).
4. **Before release latency/battery/storage promises:** accept only measured values
   with fixture IDs and tier IDs recorded.

## Explicit non-goals

- Publishing marketing SLAs from planning targets
- Using personal device content as a corpus
- Running inference in this documentation slice

## Acceptance for this plan slice

1. This document is recorded and linked from ROADMAP / CONTINUE / CHANGELOG / ADR.
2. Domain benchmark-metric identifiers exist for planned families (no harness yet).
3. No model/OCR/network dependency lands in the same change.
4. Traceability notes progress; A-01 remains incomplete until offline end-to-end
   proof with a real capability exists.

## Gate exit checklist (Local-AI architecture)

| Deliverable | Status |
|---|---|
| Local AI Technical Specification | Accepted |
| Capability interfaces | Landed |
| AI Pack delivery/security plan | Accepted (ADR-023) |
| Compatibility/fallback policy | Accepted (ADR-024) |
| Local-AI benchmark plan | Accepted (this doc / ADR-025) |
| Measured pack baseline + AVAILABLE claim | Baseline **L1 verified** 2026-08-02 (synthetic integrity/size on emulator); AVAILABLE still a later slice — `docs/CHANGE_CONTROL_LOCAL_AI_MEASURED_PACK_BASELINES.md` |
