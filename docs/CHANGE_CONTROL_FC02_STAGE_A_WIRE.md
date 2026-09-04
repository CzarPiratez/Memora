# Change control: FC-02 Stage A wire — CE RecallRanker inside Canonical Recall

**Date:** 2026-09-03  
**Type:** Application + data (Stage A semantic head wire)  
**Decision guardrails:** Live/Dual **N = 0**. No new Find path. No Stage B. No
ADR-052 UI. No AVAILABLE. Pack download remains interim/engineering until ADR-052.
S3 disposition **DEGRADED_EXPLICIT** effectivePool=20 (prefer seqLen 96).

## Pre-work record

- **Requirement IDs:** FC-02 Stage A; ADR-051; S1/S2/S3 PASS on SM-A156E.
- **Source documents read:** Charter, GOVERNANCE, CONTINUE, RECALL N=0, FC-02,
  S3 change control, model brief (wire after lexical filter).
- **Current-code evidence inspected:** `AnchorAwareMeaningRecallRanking` has no
  `RecallRanker`; `CanonicalRecall.searchByMeaning` calls it; Hilt has no
  RecallRanker binding; ORT androidTest-only; S3 DEGRADED_EXPLICIT pool 20.
- **Privacy:** Inference on stored excerpts only; model/vocab in no-backup when
  present; identity when pack absent.
- **Smallest safe change:** Promote ORT; `OnnxCrossEncoderRecallRanker` +
  no-backup pack files; wire after lexical filter; Identity when unavailable;
  unit tests; no ADR-052 onboarding UI.
- **Acceptance criteria:**
  - [x] ORT on `implementation`
  - [x] Ranking after lexical, before anchors; pool from latency policy
  - [x] Identity when model/vocab missing
  - [x] Existing ranking unit tests green with Identity (+ Stage A CE-order tests)
  - [x] No AVAILABLE claim
  - [x] S4 device smoke (CE present + identity absent) — 2026-09-04 SM-A156E

## Delivery record

- **Files:** ORT `implementation`; `NoBackupRecallRankPackStore`;
  `OnnxCrossEncoderRuntime`; `OnnxCrossEncoderRecallRanker`; `StageARecallRanker`;
  `AnchorAwareMeaningRecallRanking` + `CanonicalRecall` wire; main assets vocab;
  unit tests; this record; `FC02_STAGE_A_S4_WIRE_SMOKE_RUNBOOK.md`.
- **Disposition used:** S3 DEGRADED_EXPLICIT effectivePool=20 seqLen=96
- **Known limitation:** Rerank model still installed only via S1/S2 staging /
  adb push / future ADR-052 pack download — not silent auto-install. Studio
  **Run app** can wipe no-backup. Vocab interim in APK assets (~226 KiB) until
  pack bundles tokenizer. Live device-tier signals still hardcoded FULL in
  parts of `OnnxCrossEncoderRecallRanker`. Single-token cue ranks 2–10 may be
  noisy (lexical AND needs ≥2 tokens).
- **Verification:** JVM unit tests PASS; **S4 PASS** 2026-09-04 SM-A156E —
  CE present `silky` top hit spelling-list PDF (contains word); identity when
  model renamed away (no crash); model restored 23180880 bytes.
- **Next:** Device-tier hardening → ADR-052 unified onboarding UI.
- **Git commit:** When requested

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall (ADR-049) → RecallRanker (ADR-051 Stage A)
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: AnchorAwareMeaningRecallRanking → RecallRanker (CE or Identity)
WHY THIS CONVERGES: Measured Stage A head inside sole ranking stage
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: cosine+boost-only order without CE
EXTENDS LEGACY? no
```
