# Grounded Answers Amendment v1.0

**Product identity:** UNFYND (formerly Memora). Direction: Personal Knowledge Infrastructure. This document’s freeze is the technical invariants below. Naming is not an architectural invariant (ADR-040).

**Status:** Accepted product-direction amendment; architecture and contracts first —
implementation blocked until readiness gates in `docs/GROUNDING_ARCHITECTURE.md`
pass  
**Decisions:** ADR-033 through ADR-039  
**Governing baseline:** `docs/product-source/Memora.docx`, Local-AI addenda in
`docs/PRODUCT_SOURCE_REGISTRY.md`, `docs/LOCAL_AI_TECHNICAL_SPEC.md`,
`docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md`, ADR-012, ADR-018, ADR-019

## 1. Purpose

UNFYND is a **Personal Information Engine**: private, local-first, retrieval-first.

This amendment authorizes a governed **Grounded Answers** capability:

> Answer questions about the user’s own indexed information using only retrieved,
> stored, authorized evidence — or explicitly abstain.

It does **not** authorize a chatbot, personal assistant, cloud reasoning, phone-wide
AI, chat history, personalities, or unsupported confidence claims.

## 2. Product promise (frozen)

| Promise | Meaning |
|---|---|
| Find remains the source of truth | Candidates, evidence, and originals stay inspectable |
| Grounded answers accelerate the goal | When evidence supports a claim, UNFYND may return a structured, cited answer |
| Retrieval quality ≥ answer quality | Weak retrieval must not be papered over by fluent generation |
| AI is replaceable infrastructure | Models may change; Evidence Package and trust rules must not |
| Completeness honesty | Silent incompleteness is a trust failure worse than visible abstention |

**User-facing name** (“Ask”, “Find out”, “What does it say?”, etc.) is **not frozen**.
Architecture and capability identity are frozen; copy follows user research.

## 3. Spec carve-out (narrow)

`LOCAL_AI_TECHNICAL_SPEC.md` §9 forbids per-result generative reasoning on the
ordinary Find/search path. This amendment **narrowly authorizes** query-time
generation **only** when all of the following hold:

1. The user explicitly invokes the Grounded Answers capability (not silent Find).
2. Generation consumes only an immutable **Evidence Package** built from stored,
   authorized extracts/Memories — never freshly reopened originals for ranking or
   answering.
3. A deterministic **Verifier** rejects unsupported claims or forces abstention.
4. The UI presents status, citations, limitations, and **completeness** — never a
   bare prose “answer” without evidence.
5. Capability availability follows ADR-024 honesty (Unavailable / degraded labels).

Ordinary Find, keyword recall, and meaning candidate recall remain non-generative
unless separately amended.

## 4. Canonical pipeline

```text
Question + ReasoningTask
  -> Retriever
  -> Evidence Package Builder
  -> ReasoningEngine
  -> Verifier
  -> StructuredAnswer
  -> UI mapping
  -> Evidence / open original
```

Trust invariants (non-negotiable):

| Component | Must never |
|---|---|
| Retriever | Invent candidates |
| Evidence Package Builder | Inject unseen information or call a model |
| ReasoningEngine | Answer beyond the package; touch Room/Android/UI/network |
| Verifier | Guess missing facts or call a model |
| StructuredAnswer / UI | Hide uncertainty, incompleteness, or citations |

Lead invariant:

> UNFYND is retrieval-first. Every generated statement must be grounded in retrieved
> evidence, or the system must explicitly abstain.

## 5. ReasoningTask (domain concept)

The unit of reasoning is a **task**, not only free-form chat.

| Task | v1 |
|---|---|
| `ANSWER_QUESTION` | In scope for first slice |
| `LIST`, `EXTRACT`, `COMPARE`, `SUMMARIZE`, `TIMELINE`, `CLASSIFY` | Reserved; deferred |

Task selection may later be automatic; v1 may fix `ANSWER_QUESTION` for the PDF
slice. Goal graphs (“renew passport”) are explicitly deferred — do not block them
in contracts, do not implement them now.

## 6. First implementation slice

Until a later amendment:

- **Corpus:** saved, current-fingerprint **PDF text** only
- **Offline** after required packs are installed
- **No** source reopen during package build or reasoning
- **No** medical/legal/financial advice framing — surface what documents say
- **Abstain** on insufficient, conflicting, truncated, or unauthorized evidence
- **No** chat history, agents, personalities, web search, or cloud dependency

Acceptance detail: `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`.

## 7. Relationship to Find and Explain

- **Find** remains forever as the safety rail and browsing path.
- **Why this result?** remains the trust surface for Find hits.
- Grounded Answers must not silently replace Find or invent Why text in Compose copy
  objects; StructuredAnswer owns answer narrative for this capability.
- A future single entry field may route by intent; underlying Find vs Answer modes
  must remain distinguishable in architecture and honesty labels.

## 8. Non-goals

- General chatbot or phone-wide assistant
- Cloud AI in the core path
- Replacing Asset Memories with generated mega-summaries
- Numeric confidence without calibrated evaluation (ADR-019 / E-06)
- Claiming “hallucination-proof”
- Expanding MVP sources beyond existing PRD approvals via this amendment alone

## 9. Readiness before production code

Implementation of ReasoningEngine adapters, generative packs, or Ask UI is **blocked**
until the gates in `docs/GROUNDING_ARCHITECTURE.md` §Readiness gates are accepted.
M4 midrange retrieval work continues in parallel and is not paused by this amendment.

## 10. Change-control record

- **Requirement IDs:** P-01, P-11–P-13, P-18, A-01–A-05, A-07; E-05, E-06; G-01–G-08.
- **Source documents read:** product registry, Local-AI Spec, governance, CONTINUE,
  Experience Memory amendment, architecture adversarial reviews (Cursor/Codex/
  semantics), ADRs through ADR-032.
- **Current-code evidence:** Dual Find paths; UI Why copy; USE embeddings; no
  generative reasoner; Spec §9 non-generative Find path.
- **Open limitations:** M4 AVAILABLE open; notes/scanned PDF gaps; Spec carve-out
  must stay narrow.
- **Privacy/offline:** Local-only core; Evidence Package ephemeral by default;
  separate reasoning-pack disclosure from embeddings.
- **Smallest safe change:** Governed docs + ADRs only; no production reasoning code.
- **Acceptance criteria:** Product promise, carve-out, pipeline, first-slice bounds,
  and implementation block are explicit.
- **Verification:** Documentation cross-check; no emulator change required for this
  amendment alone.
