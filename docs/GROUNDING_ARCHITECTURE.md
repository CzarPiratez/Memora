# Grounding Architecture

**Product identity:** UNFYND (formerly Memora). Direction: Personal Knowledge Infrastructure. This document’s freeze is the technical invariants below. Naming is not an architectural invariant (ADR-040).

**Status:** Canonical engineering reference for Grounded Answers  
**Authority:** `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`, ADR-033–ADR-039  
**Updated:** 2026-08-05  

This document is the **constitution** for UNFYND’s grounded-answer system. Future
features are measured against it. It merges adversarial reviews of the current
codebase, operational readiness, and long-term product semantics.

**Implementation of generative reasoning is blocked** until §14 readiness gates pass.

---

## 1. Lead invariant

> **UNFYND is a retrieval-first system. Every generated statement must be grounded
> in retrieved evidence, or the system must explicitly abstain.**

Secondary invariant:

> **Silent incompleteness is a trust failure.** Prefer `PARTIAL` / `UNKNOWN`
> completeness and abstention over a fluent answer that pretends the corpus was fully
> considered.

---

## 2. What UNFYND is becoming

| Layer | Role |
|---|---|
| Personal Information Engine | Product category |
| Retrieval Engine | Source of truth for candidates |
| Evidence Package | Model-agnostic contract (primary IP surface) |
| ReasoningEngine | Replaceable probabilistic box |
| Verifier | Deterministic structural gate (not a truth oracle) |
| StructuredAnswer | Only narrative source for grounded answers |
| Find | Forever safety rail and browsing path |

AI/models are **replaceable infrastructure**. They are never the product’s center of
gravity.

---

## 3. Canonical pipeline

```text
Question + ReasoningTask
        │
        ▼
   Retriever  ──────────────────────────► labeled candidates
        │                                   (keyword / meaning / page)
        ▼
Evidence Package Builder ───────────────► immutable EvidencePackage
        │                                   (task, evidence, budget,
        │                                    coverage, versions)
        ▼
 ReasoningEngine ───────────────────────► untrusted ReasoningResult
        │                                   (proposed claims)
        ▼
    Verifier ───────────────────────────► accept OR abstain/reject
        │
        ▼
 StructuredAnswer ──────────────────────► domain result
        │                                   (status, completeness,
        │                                    citations, limitations)
        ▼
   UI mapping ──────────────────────────► presentation + open original
```

**Logical modules** (dependency arrows now; Gradle split later if needed):

```text
domain-grounding
application-grounding
data-retrieval
data-evidence
data-reasoning-runtime
data-verification
ui-answer
```

Domain contracts must not import Android, Room, Compose, URI, PDF SDK, or model SDK
types.

---

## 4. Trust invariants by component

| Component | Must never | Must always |
|---|---|---|
| Retriever | Invent candidates; reopen originals for ranking | Label retrieval path; return references + scores as signals only |
| Evidence Package Builder | Call a model; inject unseen text; reopen sources | Bound budget; record omissions; preserve locators/versions |
| ReasoningEngine | Access Room/Android/network/UI; exceed package | Emit claim-shaped proposals with intended citations |
| Verifier | Call a model; invent facts | Enforce citation∈package; every claim cited; limits honored |
| StructuredAnswer assembler | Hide status/limitations/citations | Surface completeness and next actions |
| UI | Run retrieval/model/DB logic; invent Why prose | Map domain → presentation; open cited locator |

---

## 5. ReasoningTask

Domain concept (not a subsystem). Selects packaging and answer shape.

| Value | v1 |
|---|---|
| `ANSWER_QUESTION` | Allowed |
| `LIST` / `EXTRACT` / `COMPARE` / `SUMMARIZE` / `TIMELINE` / `CLASSIFY` | Reserved |

v1 PDF slice may hard-code `ANSWER_QUESTION`. Automatic task routing is deferred.

**Deferred (do not block, do not build now):** goal graphs (“renew passport”),
multi-step planners, unified single entry that hides Find vs Answer.

---

## 6. Retriever

**Port**, not EmbeddingEngine alone.

Responsibilities:

- Accept `Question` + retrieval policy (+ task hints).
- Return **labeled** candidates from existing stored indexes:
  - keyword PDF page hits
  - meaning Memory / page hits
  - future note/OCR corpora when authorized
- Never silently relabel keyword as meaning (ADR-024).
- Never reopen originals for candidate generation.

Today’s code has parallel Find use cases and unused Spec `RecallRanker`. New work
must introduce a Retriever boundary that can feed Grounded Answers without deleting
Find screens.

**Open-original rule:** Cited locator in the StructuredAnswer is authoritative for
open. Post-answer page re-score must not silently change the cited page.

**Scale note:** Contracts must allow future approximate indexes; v1 may scan stored
vectors within measured limits, but must not freeze “load all embeddings into RAM”
as the permanent API.

---

## 7. Evidence Package (center of gravity)

Prefer the name **Evidence Package** (not “prompt”). Any future model, SLM, rules
engine, or platform runtime consumes the same package.

### 7.1 Required contents

- Immutable identities: Asset / Memory / revision / extraction schema versions
- Exact permitted excerpts, pages, or spans with locators
- Selection order and selection reason
- `ReasoningTask` + constraints (e.g. prefer chronology)
- Source-availability / freshness / integrity signals
- Package schema version + builder policy version
- Compatible reasoner capability/model version constraints
- Hard limits: max assets, excerpts/pages per asset, total bytes/tokens, citations
- Stable package identity (checksum / content hash)
- **Coverage:** what was considered vs omitted, and why (budget, rank cutoff,
  incomplete index, unauthorized, duplicate)
- Retrieval-path labels per candidate (keyword vs meaning, etc.)

### 7.2 Rules

- Built only from **stored** authorized extracts / Memory evidence — not full PDF
  bytes, original images, or entire note accounts during recall.
- If Memory evidence excerpts are insufficient for the question, the builder may
  include **persisted page/OCR text rows** already in Room for those candidates,
  still within budget — and must mark coverage accordingly.
- Ephemeral by default: not written to durable user-facing history in v1.
- Same inputs + same builder policy version ⇒ same package (deterministic).

### 7.3 Completeness hints from packaging

Builder emits signals the answer layer maps to completeness:

| Signal | Typical completeness |
|---|---|
| All high-rank candidates included; budget not hit | May allow `COMPLETE` only if retrieval policy also claims corpus-complete for the query class |
| Budget truncated or deep pages unindexed | `PARTIAL` |
| Unknown corpus coverage / weak retrieval | `UNKNOWN` |

Never claim corpus-complete for open-ended “every X” queries unless retrieval
explicitly supports exhaustive enumeration (usually it does not → `PARTIAL` or
`UNKNOWN` + abstain or list-with-caveat).

---

## 8. ReasoningEngine

New Local Intelligence **capability** (do not overload `DocumentEngine` or
index-time `MemoryBuilder`).

### 8.1 Contract shape

```text
reason(package: EvidencePackage, task: ReasoningTask) -> ReasoningResult
```

`ReasoningResult` is **untrusted**: proposed claims with intended citation IDs,
plus raw abstain proposal if the model declines.

Must not return Compose UI types or a bare `String` as the product answer.

### 8.2 v1 answer forms

Prefer **constrained** claim shapes (short statements, dates, lists of cited
excerpts) over free-form essays. Free-form paraphrase increases faithfulness risk.

### 8.3 Runtime ownership

- Dedicated lifecycle-aware runtime owner (load, serial access, cleanup).
- Not loaded from Compose; not an accidental unmanaged global singleton.
- Single-flight inference for v1 (likely one session).
- Cancellation propagates; never swallow `CancellationException` as generic failure.
- Pack install follows ADR-023 patterns; reasoning packs are **separate** from
  embedding packs (ADR-039).

### 8.4 Sensitive domains

May surface what a document says with citations. Must not present as medical,
legal, or financial advice. Limitations copy is mandatory for such queries when
detected by policy heuristics (v1 may use conservative keyword/task policy).

---

## 9. Verifier

Deterministic. No second LLM.

### 9.1 Must prove structurally

- Each citation exists in the package
- Cited excerpt belongs to the selected package
- Every final claim has ≥1 citation
- Package limits and source/version rules honored
- No evidence ⇒ no `ANSWERED`
- Conflicting cited spans ⇒ `CONFLICTING_EVIDENCE` (do not silently pick a winner)

### 9.2 Must not claim

- Semantic equivalence of paraphrase to source
- “Hallucination-proof”
- Calibrated numeric confidence without E-06 evaluation

Failed verification → abstain / reject to `INSUFFICIENT_EVIDENCE` (or conflicting),
never a partially unverified prose answer.

---

## 10. StructuredAnswer

Domain object only. UI maps afterward.

### 10.1 Status

| Status | Meaning |
|---|---|
| `ANSWERED` | Claims accepted with citations |
| `INSUFFICIENT_EVIDENCE` | Cannot support an answer |
| `CONFLICTING_EVIDENCE` | Sources disagree |
| `CAPABILITY_UNAVAILABLE` | Pack/device/runtime unavailable |
| `CANCELLED` | User/system cancelled |
| `FAILED_SAFELY` | Error without corrupting store |
| `SHOW_CANDIDATES_ONLY` | Goal best served by Find-like cards, not prose |

### 10.2 Completeness (required)

| Value | Meaning |
|---|---|
| `COMPLETE` | Policy asserts the answer considers the full applicable evidence set for this query class |
| `PARTIAL` | Known truncation, incomplete index, or non-exhaustive retrieval |
| `UNKNOWN` | System cannot honestly assert completeness |

No numeric confidence in v1.

### 10.3 Other required fields

- Bounded claims with per-claim citations
- Direct evidence excerpts + safe provenance pointers
- Source availability / freshness
- Limitations
- Suggested next actions (e.g. open Find, open page) — optional, never invent knowledge
- Versions: retrieval policy, package, reasoner model, verifier rules

---

## 11. Android lifecycle and concurrency

| Topic | Rule |
|---|---|
| Interactive answer | Foreground cancellable coroutine flow — **not** WorkManager default |
| Identity | Request ID + Evidence Package ID; stale results must not overwrite UI |
| Room | Snapshot immutably; **do not** hold a transaction across inference |
| Process death | In-flight answer is ephemeral; show “did not finish—try again”; do not restore partial prose |
| Cancellation | No partial durable answer; release native resources promptly |
| WorkManager | Discovery, extract, embed/index, pack download, reindex, benchmarks only |
| Long background answer | Only with explicit UX, progress, constraints, and a separate design — not v1 |
| Native crash / OOM | Must not corrupt Room; consider isolated process only after measurement spike |
| Device unlock | Encrypted DB unavailable ⇒ safe failure; no partial writes |

---

## 12. Privacy and threat model (summary)

Threat surfaces to address before ship:

- Evidence Package contents in RAM (sensitive multi-doc context)
- Vendor runtime logs / crash dumps
- Screenshots of answer UI
- Model files in no-backup storage
- Derived extracts after source revocation (disclose; do not pretend original is open)
- No user content upload in pack download
- No durable prompt logging in release builds

Full threat notes live with ADR-039 and the reasoning-pack delivery plan when a
model is chosen.

---

## 13. Evaluation harness (forever)

Every release that enables Grounded Answers must track:

1. Retrieval quality  
2. Citation / grounding coverage  
3. Claim support / unsupported-claim rate  
4. Abstention quality  
5. Conflicting-evidence handling  
6. Completeness honesty (no false `COMPLETE`)  
7. Latency, RAM, battery, thermal on representative devices  

Fixtures are independently authored. Model-generated “expected answers” are not
ground truth.

---

## 14. Readiness gates (implementation block)

Before production ReasoningEngine / Ask UI code:

1. Experience Memory status kept truthful (`EXPERIENCE_MEMORY_AMENDMENT_V1.md` §10).
2. This architecture doc + `GROUNDED_ANSWERS_AMENDMENT_V1.md` accepted.
3. ADR-033–ADR-039 recorded.
4. Evidence Package contract reviewed.
5. ReasoningEngine + capability ID contract reviewed.
6. Verifier + abstention contract reviewed.
7. StructuredAnswer + completeness contract reviewed.
8. Context limits + privacy / threat notes accepted.
9. Reasoning model lifecycle plan (separate from embeddings) accepted for the chosen
   pack **before** that pack is wired — may remain “TBD vendor” until selection.
10. Evaluation corpus plan for PDF slice opened.
11. Interactive cancellation / process-death design accepted (this §11).
12. M4 midrange execute **or** explicit unsupported/degraded policy for the answer
    capability on target tiers.
13. `GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` accepted.
14. Adversarial pass on: wrong retrieval, incomplete package, conflict, revocation,
    model swap, native crash.

**Docs in this change close gates 1–8, 11, 13 (design). Gates 9–10, 12, 14 remain
open until a model is chosen and M4 / eval work proceeds.**

---

## 15. Mapping to current codebase (honest)

| Proposed | Today |
|---|---|
| Retriever | Parallel keyword + meaning use cases; no unified port |
| Evidence Package | MemoryEvidence + keyword hit DTOs; no package type |
| ReasoningEngine | Absent |
| Verifier | Pack integrity only (`AiPackPayloadVerifier`) |
| StructuredAnswer | UI phase + Why copy strings |
| Spec MemoryBuilder / RecallRanker | Availability stubs; real assembly/ranking elsewhere |

Grounded Answers is a **new architecture slice**, not a rename of Find by meaning.

---

## 16. Explicit deferrals

Do not implement under this architecture’s v1:

- Chat history, agents, personalities, cloud core path
- Goal orchestration / multi-step life tasks
- Hiding Find behind a single opaque “AI” entry
- Event/Knowledge Memories as answer substrate
- Numeric confidence UI
- Premature Gradle module split (logical boundaries only)
- Isolated inference process (spike later)

---

## 17. Change-control record

- **Requirement IDs:** G-01–G-08; P-01, P-11–P-13; A-01–A-05, A-07; E-05, E-06.
- **Sources:** Adversarial architecture review; Codex readiness review; semantics
  review; Local-AI Spec; ADR-012/018/019/023–025/029–032; CONTINUE.
- **Smallest safe change:** Documentation constitution only.
- **Verification:** Cross-linked amendment, ADRs, acceptance spec, registry,
  CONTINUE, ROADMAP, CHANGELOG, traceability.
