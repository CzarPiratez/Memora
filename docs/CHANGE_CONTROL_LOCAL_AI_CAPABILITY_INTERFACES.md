# Change Control: Local-AI Capability Interfaces

**Date:** 2026-07-25
**Requirements:** A-01, A-02, A-03; Local AI Technical Spec §4.
**Decision guardrails:** ADR-012; ROADMAP Local-AI architecture gate; no models/SDKs.

## Pre-work record

- **Requirement IDs:** A-01, A-02, A-03 (interfaces only; packs/benchmarks later).
- **Source documents read:** AGENTS, product registry, Local AI spec §4–6/§10,
  governance, CONTINUE, product contract, architecture, ADR-012, roadmap,
  PRD_TRACEABILITY.
- **Current-code evidence inspected:** No `VisionEngine` / `OcrEngine` / etc. in
  Kotlin; domain Memory contract exists separately; no AI Gradle deps.
- **Open ADRs / limitations:** AI Pack plan, compatibility matrix, and Local-AI
  benchmarks remain missing for full gate exit. No inference in this slice.
- **Dependencies added:** None.
- **Privacy:** No user content processed; stubs only report unavailable.
- **Smallest safe change:** Domain capability availability types, spec-named
  interfaces, unavailable stubs, unit tests proving truthful unavailability.
- **Acceptance criteria:** Spec interfaces exist; stubs never invent facts; no new
  AI/OCR/network deps; docs do not claim understanding is ready; A-03 notes
  interfaces landed without marking A-01/A-07 done.

## Delivery record

- **Files/layers changed:** Domain package
  `com.memora.app.domain.intelligence`: CapabilityId, ModelVersionIdentity,
  CapabilityAvailability, CapabilityLimits, LocalCapability; Spec §4 interfaces
  VisionEngine, OcrEngine, DocumentEngine, EmbeddingEngine, MemoryBuilder,
  RecallRanker; Unavailable* stubs + UnavailableLocalIntelligence factory; unit
  tests. Change-control this file; CONTINUE; CHANGELOG; PRD_TRACEABILITY A-03.
- **Automated verification and result:** On 2026-07-25,
  `:app:testDebugUnitTest --tests com.memora.app.domain.intelligence.*` passed
  (CapabilityAvailabilityTest + UnavailableLocalIntelligenceTest). No new AI/OCR/
  network Gradle dependencies; domain package has no Android/SDK imports.
- **Emulator/manual verification and result:** Not required for this slice (pure
  domain contracts; no UI or on-device inference).
- **Failure/recovery paths verified:** Unavailable stubs always return
  Unavailable with non-blank reason and null limits; blank reasons/model versions
  rejected.
- **Known limitation or follow-up:** AI Pack delivery/security plan, compatibility
  fallback policy, and Local-AI benchmark plan still required for gate exit. No
  Hilt binds yet (nothing consumes these contracts in production paths).
- **Documentation/traceability/ADR updates:** CONTINUE checkpoint; CHANGELOG
  Unreleased; PRD_TRACEABILITY A-03 notes interfaces landed without marking
  A-01/A-07 done.
- **Git commit:** `ce73dbb`
