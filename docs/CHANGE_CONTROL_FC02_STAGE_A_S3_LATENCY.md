# Change control: FC-02 Stage A S3 — midrange latency disposition

**Date:** 2026-09-03  
**Type:** Measurement + domain latency policy (no product Find wire)  
**Decision guardrails:** Live/Dual **N = 0**. No Canonical Recall wire. No ORT
`implementation` promote until S3 disposition recorded. No AVAILABLE. No ADR-052 UI.

## Pre-work record

- **Requirement IDs:** FC-02 Stage A S3; ADR-051 §7 latency; model brief measurement
  plan (≤800 ms aggregate for pool 40 **or** `DEGRADED_EXPLICIT` + identity fallback).
- **Source documents read:** `ENGINEERING_CHARTER`, `GOVERNANCE` pre-work,
  `CONTINUE` (S1/S2 PASS), `RECALL_ENFORCEMENT_INDEX` (N=0), FC-02 change control,
  Stage A model brief, `RecallRankDevicePolicy`, S1 Logcat 1642 ms / 40 pairs.
- **Current-code evidence inspected:** S1 full-pool wall 1642 ms already above 800;
  S2 quality 3/3; no Find wire; `IdentityRecallRanker` available for fallback.
- **Open ADRs / limitations:** ADR-052 UI later; Stage B / FC-03 later.
- **Privacy:** Fixture/synthetic passages only; model bytes already staged from S1/S2.
- **Smallest safe change:** `RecallRankLatencyPolicy` + unit tests; androidTest S3
  harness measuring full40/reduced20 walls; change control + runbook; no Find wire.
- **Acceptance criteria:**
  - [x] Domain policy encodes WITHIN_BUDGET / DEGRADED_EXPLICIT / IDENTITY_FALLBACK
  - [x] Effective pool shrinks under DEGRADED_EXPLICIT; zero under IDENTITY_FALLBACK
  - [x] Identity fallback helper when wallMs > 800
  - [x] Device: `RecallRankStageALatencyIntegrationTest` Logcat `MemoraRecallRankS3`
  - [x] Disposition recorded from measured full + reduced walls

## Delivery record

- **Device verification:** **S3 PASS** 2026-09-03 SM-A156E:

```
latency label=full40_len128 pool=40 maxLen=128 wallMs=1517 targetMs=800
latency label=reduced20_len128 pool=20 maxLen=128 wallMs=729 targetMs=800
latency label=reduced20_len96 pool=20 maxLen=96 wallMs=584 targetMs=800
disposition=DEGRADED_EXPLICIT effectivePool=20 fullWallMs=1517 reduced20len96WallMs=584 tier=FULL meets800full=false
```

- **Binding disposition:** **DEGRADED_EXPLICIT** — Stage A wire uses effective
  pool **20** (prefer maxLen 96); identity fallback when pack absent or wall would
  exceed 800 ms.
- **Next:** Stage A product wire + ORT `implementation` promote.
- **Git commit:** When requested
