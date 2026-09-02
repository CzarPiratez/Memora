# Change control — Post-MVP domain foundations (slice 1)

**Date:** 2026-09-02  
**Status:** Authorized — domain-only; no Grounded Answers runtime  
**Program:** `docs/POST_MVP_PROGRAM_V1.md` §7 foundation checklist  
**Decision guardrails:** Pure domain + tests. No UI, Room migration, or Canonical
Recall wiring in this slice. No AVAILABLE claim.

## Pre-work record

- **Requirement IDs:** POST_MVP §7 (locators, model signature, grounding interfaces).
- **Source documents read:** `docs/GROUNDING_ARCHITECTURE.md` §5–10,
  `docs/POST_MVP_PROGRAM_V1.md`, `PdfPageEvidenceLocator.kt`,
  `CorpusCompleteness.kt`, `LocalIntelligenceEngines.kt`.
- **Current-code evidence inspected:** `CorpusCompleteness` already in domain;
  `ModelVersionIdentity` used for embeddings; `EvidenceLocator` string wrapper;
  no `domain.grounding` package; `RecallRanker` without operate method.
- **Smallest safe change:** Audio segment locator; `EmbeddingModelSignature` alias;
  grounding domain contracts (immutable types + tests); RecallRanker slice 1 per
  `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md`.
- **Acceptance criteria:**
  - [x] `AudioSegmentEvidenceLocator` parse/format round-trip tests
  - [x] Grounding types compile with domain purity (no Android imports)
  - [x] `IdentityRecallRanker` unit tests added
  - [ ] `testDebugUnitTest` green locally (JAVA_HOME unavailable in agent shell; CI gate)

## Architectural convergence

N/A — not a Find/Recall product path change (port definition only).

## Delivery record

- **Files/layers changed:** _See git diff_
- **Git commit:** _Pending_
