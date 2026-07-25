# Change Control: Local-AI Compatibility / Fallback Policy

**Date:** 2026-07-25
**Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §6, §10, §11, §12, §13.4.
**Decision guardrails:** ADR-012, ADR-023; ROADMAP Local-AI architecture gate.

## Pre-work record

- **Requirement IDs:** A-01, A-03, A-07 (policy + domain support matrix contracts).
- **Source documents read:** AGENTS, product registry, Local AI spec, governance,
  CONTINUE, architecture, ADR-012/023, AI Pack plan, roadmap, traceability.
- **Current-code evidence inspected:** Capability interfaces + AiPack contracts
  (`ce73dbb`, `fe42088`); unavailable stubs only; no device support matrix yet.
- **Open ADRs / limitations:** Local-AI benchmark plan still required for gate exit.
- **Dependencies added:** None. No network/models.
- **Privacy:** No user content; policy defines honest unavailable outcomes only.
- **Smallest safe change:** Compatibility/fallback policy doc; domain support and
  fallback outcome types; unit tests; docs/ADR.
- **Acceptance criteria:** Policy forbids silent semantic→filename fallback;
  unsupported devices get truthful unavailable; domain types encode matrix rows;
  gate exit still needs benchmarks.

## Delivery record

- **Files/layers changed:** `docs/LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY.md`;
  ADR-024; domain `CapabilitySupportPolicy.kt`; unit tests; CHANGELOG; CONTINUE;
  ROADMAP; PRD_TRACEABILITY; this change-control; AI Pack change-control commit pin.
- **Automated verification and result:** On 2026-07-25,
  `:app:testDebugUnitTest --tests com.memora.app.domain.intelligence.*` passed
  (including CapabilitySupportPolicyTest). No new AI/OCR/network Gradle deps.
- **Emulator/manual verification and result:** Not required (docs + pure domain).
- **Failure/recovery paths verified:** Blank reasons rejected; default policy never
  upgrades to AVAILABLE; silent filename/keyword-as-semantic fallbacks forbidden.
- **Known limitation or follow-up:** Local-AI benchmark plan; concrete ABI/API/RAM
  rows when a pack is chosen.
- **Documentation/traceability/ADR updates:** ADR-024; ROADMAP/CONTINUE/CHANGELOG.
- **Git commit:** `1ecd7ee`
