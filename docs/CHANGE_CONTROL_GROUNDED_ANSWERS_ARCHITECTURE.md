# Change control: Grounded Answers architecture constitution

**Date:** 2026-08-05  
**Type:** Documentation / product-direction / architecture gate  
**Decision guardrails:** No generative ReasoningEngine code; no Ask UI; no new SDK;
Find remains non-generative; M4 retrieval work continues in parallel.

## Pre-work record

- **Requirement IDs:** P-01, P-11–P-13, P-18; A-01–A-05, A-07; E-05, E-06; G-01–G-08.
- **Source documents read:** `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`, `CONTINUE`,
  `PRODUCT_CONTRACT`, `ARCHITECTURE`, `DECISIONS`, `ROADMAP`, `PRD_TRACEABILITY`,
  `LOCAL_AI_TECHNICAL_SPEC`, `EXPERIENCE_MEMORY_AMENDMENT_V1`, adversarial architecture
  reviews (Cursor/Codex/semantics).
- **Current-code evidence inspected:** Dual Find paths (keyword + meaning); UI Why
  copy; `MemoryEvidence` domain; Spec `MemoryBuilder`/`RecallRanker` stubs; no
  generative reasoner; USE embedding path; M4 still open.
- **Open ADRs / limitations:** ADR-003 notes; M4 midrange; reasoning model vendor TBD;
  Spec §9 required narrow carve-out; Experience Memory §10 was stale (corrected).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only. Future grounded answers remain local-first; Evidence Package
  ephemeral by default; reasoning packs separate from embedding packs (ADR-039).
- **Smallest safe change:** Governed docs + ADRs + traceability; no app code.
- **Acceptance criteria:**
  - Canonical architecture doc with trust invariants and readiness gates.
  - Product amendment with Spec carve-out and first-slice bounds.
  - ADR-033–039 recorded after ADR-032.
  - G-01–G-08 in traceability matrix.
  - PDF slice acceptance spec.
  - Registry, CONTINUE, ROADMAP, CHANGELOG updated.
  - Implementation explicitly blocked until open readiness gates pass.
- **Test and emulator verification plan:** Documentation cross-check only; no emulator
  run required for this slice.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/GROUNDING_ARCHITECTURE.md` (new)
  - `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` (new)
  - `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` (new)
  - `docs/CHANGE_CONTROL_GROUNDED_ANSWERS_ARCHITECTURE.md` (new)
  - `docs/DECISIONS.md` (ADR-033–039)
  - `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` (§10 status refresh)
  - `docs/LOCAL_AI_TECHNICAL_SPEC.md` (v1.3, §9 carve-out)
  - `docs/PRODUCT_SOURCE_REGISTRY.md`
  - `docs/ARCHITECTURE.md`
  - `docs/PRD_TRACEABILITY.md` (G-01–G-08)
  - `docs/ROADMAP.md`
  - `CONTINUE.md`
  - `docs/CHANGELOG.md`
- **Automated verification and result:** Documentation-only; no Gradle run.
- **Emulator/manual verification and result:** Not required for this change.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:**
  - Reasoning model vendor and pack lifecycle plan (ADR-039 follow-up ADR when chosen).
  - PDF eval corpus fixtures not yet authored.
  - M4 midrange execute still pending.
  - Adversarial pass on implemented slice remains future gate.
  - Domain Kotlin contracts (EvidencePackage, Retriever port, etc.) not yet coded.
- **Documentation/traceability/ADR updates:** This record.
- **Git commit:** Pending user request.

## What this enables next (still blocked)

1. Choose on-device reasoner + record pack ADR.
2. Author PDF eval fixtures.
3. Implement domain contracts + fakes (no vendor SDK until pack ADR).
4. Close M4 or explicit degraded policy for answer capability.
5. Build PDF vertical slice per acceptance spec.

## What this forbids

- ReasoningEngine production wiring before readiness gates close.
- Chatbot UX, chat history, cloud core reasoning.
- Marketing grounded answers as AVAILABLE without measurement.
- Replacing Find or hiding dual-path honesty labels.
