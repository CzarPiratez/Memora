# Change control: Docs track check — Stage A gate alignment

**Date:** 2026-09-03  
**Type:** Documentation / operator truth sync  
**Decision guardrails:** Docs only. No App product wire. No AVAILABLE claim.
Does not promote ONNX Runtime to main. Does not ship ADR-052 UI.

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; ADR-051; ADR-052; FC-02; GOVERNANCE
  pre-work; LEGACY Live/Dual metric habit.
- **Source documents read:** `ENGINEERING_CHARTER`, `GOVERNANCE`, `CONTINUE`,
  `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`, `RECALL_CONVERGENCE_DONE`,
  `ESCAPE_HATCH_AUDIT`, ADR-051/052, FC-02 change control, Stage A model brief,
  `PRODUCT_SOURCE_REGISTRY`, `MVP_EXIT_AUDIT`, S1 spike sources.
- **Current-code evidence inspected:** `AnchorAwareMeaningRecallRanking` (no
  `RecallRanker` call); `IdentityRecallRanker`; S1 androidTest spike + device
  policy; ORT on androidTest only.
- **Open ADRs / platform limitations checked:** ADR-052 accepted (UI later);
  Stage A wire blocked on S1 device proof; Stage B blocked on FC-03.
- **Privacy / offline / dependency:** Docs only this step.
- **Smallest safe change:** Fix operator docs drift; publish S1 runbook; align
  next-action lists to S1 → S2 → wire → ADR-052 UI.
- **Acceptance criteria:**
  - `RECALL_ENFORCEMENT_INDEX` heartbeat matches LEGACY **N = 0**
  - GOVERNANCE / registry no longer imply L7/L8 Live or MIG-07-PDF-only
  - FC-02 / MVP / CONTINUE name S1 runbook as next gate
  - ADR-052 explicitly sequenced after Stage A wire
- **Test plan:** Docs inspection; JVM unit tests when local JDK available
  (agent shell JBR path incomplete — founder runs in Android Studio).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall (docs sync only)
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: FC-02 Stage A behind RecallRanker inside AnchorAwareMeaningRecallRanking
WHY THIS CONVERGES: Operator docs must match delivered escape-hatch NO
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a
EXTENDS LEGACY? no
```

## Findings (track verdict)

**On track.** Correct sequence remains:

1. S1 device spike (now) — `docs/FC02_STAGE_A_S1_SPIKE_RUNBOOK.md`
2. S2 fixture quality on `meaning-pdf-page-recall-v1`
3. S3 midrange wallMs
4. Stage A product wire + ORT `implementation`
5. ADR-052 unified auto-download onboarding (launch slice)
6. Stage B only after FC-03

**Do not jump** to automatic download UI or product wire before S1 green.

## Delivery record

- **Files:** `RECALL_ENFORCEMENT_INDEX`, `GOVERNANCE`, `PRODUCT_SOURCE_REGISTRY`,
  FC-02 change control, model brief, ORT review, MVP audit, CONTINUE, CHANGELOG,
  ADR-052 CC, this file, `FC02_STAGE_A_S1_SPIKE_RUNBOOK.md`
- **Verification:** Docs inspection
- **Follow-up:** Founder runs S1 on Samsung; paste Logcat `MemoraRecallRankS1`
- **Git commit:** When requested
