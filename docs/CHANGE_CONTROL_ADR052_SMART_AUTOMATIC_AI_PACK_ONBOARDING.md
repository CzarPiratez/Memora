# Change control: ADR-052 Smart automatic AI pack onboarding

**Date:** 2026-09-03  
**Type:** Product UX / AI Pack policy (docs)  
**Decision guardrails:** Docs only. Does not ship unified onboarding UI. Does not
claim AVAILABLE. Does not bundle packs in APK. Does not authorize silent install.

## Pre-work record

- **Requirement IDs:** AI Pack delivery plan; ADR-012 local-first; ADR-051 rerank;
  ADR-029–032 embedding track; founder launch UX direction (2026-09-03).
- **Source documents read:** `AI_PACK_DELIVERY_SECURITY_PLAN.md`,
  `AiPackDisclosureCopy.kt`, `RecallRankDevicePolicy.kt`, ADR-051, FC-02 model
  brief, `MVP_EXIT_AUDIT.md`.
- **Current-code evidence inspected:** Interim multi-step `AiPackDisclosure`
  flow (acknowledge → download → build index); no unified onboarding composable.
- **Smallest safe change:** Accept ADR-052; update registry, model brief, CONTINUE,
  changelog; this record.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: AI Pack install (Spec §6) — not Canonical Recall
CURRENT PATH: Interim AiPackDisclosure multi-tap engineering flow
TARGET PATH: Unified onboarding — one Continue → device-aware auto install
WHY THIS CONVERGES: Launch UX without violating explicit user-approved download
EXTENDS LEGACY? no
```

## Delivery record

- **Files/layers changed:** ADR-052; registry; model brief; CONTINUE; CHANGELOG;
  this file; `RecallRankDevicePolicy` / `RecallRankAiPackTrack` doc comments.
- **Automated verification:** Docs inspection only.
- **Known limitation:** UI implementation is a future launch slice **after**
  FC-02 Stage A product wire; engineering path remains until then. Do not start
  ADR-052 UI before S1 → S2 → Stage A wire.
