# UNFYND Core capability register (v1)

**Status:** Register — names and reserves capabilities; does **not** authorize building them
**Date:** 2026-09-04
**Process:** `docs/ENGINEERING_CHARTER.md` holistic scenario planning
**Companion:** `docs/PROGRAM_STATE_AND_SEQUENCE_V1.md` (state + sequence)

**Does not authorize:** marketing AVAILABLE, new Find paths, Grounded Answers
runtime, Act, VisionEngine, ADR-052 UI, multi-user, cloud, or any code.

---

## Why this document exists

Founder definition of UNFYND Core (2026-09-04):

> A persistent, multimodal, inspectable intelligence layer that lives beside the
> corpus, continuously builds usable knowledge from it, retrieves from it,
> grounds answers in evidence, and can operate inside the data's trust boundary.

Most of that definition is already carried by the substrate. Nine capabilities
are **not modelled anywhere** — not in the Freeze, not in the Grounding
Architecture, not in the Ask Model, not in code. They are not "unbuilt": unbuilt
is cheap. They are **unmodelled**, and each one is a decision that gets more
expensive the later it is made, because it touches the substrate rather than a
feature.

This register exists so that when Core is offered to a clinical, defence, or
enterprise integrator, the answer is *"here is the boundary we reserved for
that"* — never *"we did not think about it."*

**A register entry is not a commitment to build.** It is a commitment not to be
surprised. Each entry states the cost of deferring, so deferral stays a decision
rather than an accident.

---

## How to read the cost column

| Cost | Meaning |
|---|---|
| **Substrate** | Retrofitting touches Memory, evidence, recall, and answers at once. Most expensive class |
| **Path** | Touches one pipeline stage; contained but not local |
| **Surface** | UI, copy, or a single use case |

---

## Register

### CR-01 — Fact validity and supersession

| | |
|---|---|
| **What** | Evidence has a time. A **fact** has a period during which it is true. A quote is superseded by an invoice; an address changes; a policy is revised |
| **Definition clause** | *persistent* · *builds usable knowledge* |
| **Today** | `MemoryRevisionId` versions the **Memory**. Nothing models which **fact** is current. ADR-022 supersedes extraction rows, which is storage hygiene, not fact validity |
| **Symptom if deferred** | The system cannot distinguish "changed" from "contradictory". Old and current values compete as equals in ranking and in any future answer |
| **Cost** | **Substrate** |
| **Trigger to build** | Before Links/Events (CR-09), and before any answer that reports a value that can change |

### CR-02 — Conflict reconciliation (not refusal)

| | |
|---|---|
| **What** | Two pieces of evidence disagree. Today the only honest response available is silence |
| **Definition clause** | *grounds answers in evidence* |
| **Today** | `StructuredAnswerStatus.CONFLICTING_EVIDENCE` exists, but (a) nothing computes conflict, and (b) `StructuredAnswer` requires non-`ANSWERED` statuses to carry **zero claims** |
| **Symptom if deferred** | An advance plus a balance payment looks like a contradiction, so the system abstains on questions it could answer well. Safe, but not the product |
| **Cost** | **Path** (answer layer), **Substrate** once it needs CR-01 |
| **Trigger to build** | With the first Grounded Answers slice that can retrieve more than one supporting Memory |
| **Bar** | Reconciliation must **show its working**: every reconciled component individually cited, superseded values named rather than hidden |

### CR-03 — Audit seam

| | |
|---|---|
| **What** | A policy seam for recording who asked what, what was retrieved, and what was answered — set by the integrator, not hardcoded by Core |
| **Definition clause** | *inspectable* · *inside the data's trust boundary* |
| **Today** | Nothing. And the two requirements are in direct tension: Ask Model **R8** requires no durable prompt log on a consumer phone, while health, defence, and government require the opposite |
| **Symptom if deferred** | Core is silently a consumer product. A regulated integrator cannot adopt it, and adding audit later means re-opening recall, packaging, and answers together |
| **Cost** | **Substrate** |
| **Trigger to build** | Before any Core release aimed at a regulated boundary. The **seam** should be reserved much earlier than the implementation |
| **Bar** | Off by default. When on, the log stays inside the boundary and is itself inspectable |

### CR-04 — Permission-scoped retrieval

| | |
|---|---|
| **What** | Retrieval filtered by who is asking. Evidence a role may not see must not rank, must not appear in a package, and must not be inferable from an absence |
| **Definition clause** | *inside the data's trust boundary* |
| **Today** | The model assumes one person, one device, one corpus. Evidence is present or absent |
| **Symptom if deferred** | Every industry on the Core page (health, defence, government, enterprise) assumes roles and clearances. Without this, Core is a single-user product wearing infrastructure language |
| **Cost** | **Substrate** — recall path, evidence package, and audit trail simultaneously |
| **Trigger to build** | First multi-role deployment. Reserve the seam now |
| **Bar** | Scoping happens at **candidate generation**, never as a post-filter on results — a post-filter leaks existence through result counts and Why copy |
| **Public posture** | Stated as Core direction in the public pack from revision **2026-09-05** (`APPLICATIONS.md` primitive 6, "Scope inside the boundary"). Policy authorship stays with the builder; enforcement is Core's. The candidate-generation bar above is published, so a later post-filter shortcut would contradict a public contract |

### CR-05 — Re-derivation policy

| | |
|---|---|
| **What** | When a capability improves — a better embedder, a vision model arriving — which Memories are re-derived, in what order, at what battery and time cost, and what the user is told |
| **Definition clause** | *persistent* · *continuously builds* |
| **Today** | The identity half exists: stable `MemoryId`, `MemoryRevisionId` lineage, `STALE_REINDEX_REQUIRED`, `EmbeddingModelSignature`. The **trigger and policy** do not |
| **Symptom if deferred** | A library indexed today stays at today's intelligence forever. "Lasting memory" quietly becomes "memory frozen at ingest time" |
| **Cost** | **Path** (lifecycle + workers), rising to **Substrate** if capability versions were never recorded — they are, so this stays contained |
| **Trigger to build** | Before shipping the **second** version of any understanding model |
| **Bar** | Identity never changes because implementation improved (ADR-007). Re-derivation is event-driven and interruptible (ADR-044), never a device-melting full rebuild |

### CR-06 — Reproducibility contract

| | |
|---|---|
| **What** | Same corpus + same model signatures + same question ⇒ same answer; and the answer records the exact state that produced it |
| **Definition clause** | *inspectable* |
| **Today** | Every ingredient exists — `EmbeddingModelSignature`, extraction schema versions, `MemoryRevisionId`, evidence classes. **No stated guarantee and no test** |
| **Symptom if deferred** | "Inspectable" means only "you can open the citation", not "this result can be reproduced and defended". Most RAG systems cannot reproduce yesterday's answer; stating and testing this is a differentiator we can claim almost for free |
| **Cost** | **Surface** now (contract + test). **Path** later if non-determinism is introduced first and has to be removed |
| **Trigger to build** | Now-ish — it is cheap, and every day of non-deterministic code makes it harder |
| **Bar** | Determinism holds for retrieval and packaging. Where a probabilistic engine is involved, the **package** and the **citations** must still be reproducible even if prose is not |

### CR-07 — Aggregation trust

| | |
|---|---|
| **What** | Arithmetic over evidence: totals, counts, durations, comparisons |
| **Definition clause** | *grounds answers in evidence* |
| **Today** | `ReasoningTask` has `EXTRACT`, `LIST`, `COMPARE`. Nothing for aggregation |
| **Symptom if deferred** | Someone implements summing inside `EXTRACT`. A wrong total is a uniquely dangerous failure: confident, specific, checkable, and it **cites real evidence while being wrong** |
| **Cost** | **Path** |
| **Trigger to build** | Before any answer that reports a number the user did not read themselves |
| **Bar** | Two acceptable stances, and the decision must be recorded: (a) an `AGGREGATE` task where **every operand is individually cited** and the arithmetic is shown, or (b) explicit refusal to compute, listing the components instead. Silent arithmetic is forbidden either way |

### CR-08 — Selective deletion with proof of erasure

| | |
|---|---|
| **What** | Delete one source and everything derived from it — evidence, anchors, embeddings, cached packages — and demonstrate it is gone |
| **Definition clause** | *inside the data's trust boundary* |
| **Today** | All-or-nothing `ClearMemoraDerivedData`, plus a `REMOVED` integrity state |
| **Symptom if deferred** | No answer to subject-access deletion, clean-room teardown, or "this document was filed in error". Partial deletion that misses an embedding is worse than none, because the corpus can still surface it |
| **Cost** | **Path**, touching every derived store |
| **Trigger to build** | Before any deployment with a retention obligation, or any clean-room / engagement-scoped corpus |
| **Bar** | Cascade is enumerated, not assumed. Deletion is verifiable — a test proves nothing derived survives |

### CR-09 — Knowledge stages (Link → Event → Knowledge)

| | |
|---|---|
| **What** | Relationships between Memories, so several artefacts can be one remembered episode |
| **Definition clause** | *builds usable knowledge* — the clause the substrate currently does **not** satisfy |
| **Today** | Asset Memories only. Nothing links two Memories. The staged model is documented in `EXPERIENCE_MEMORY_AMENDMENT_V1.md` and deliberately deferred |
| **Symptom if deferred** | Every multi-source question is reconstructed at query time and thrown away. *"How much did I pay for painting my apartment"* — an invoice, a receipt photo, and a note — has no durable representation as one thing |
| **Cost** | **Substrate**, but the Freeze and the amendment already reserved the shape, so this is **planned expansion**, not retrofit |
| **Trigger to build** | After Find quality (W1–W3) and after CR-01, which Links depend on |
| **Bar** | Links are evidence-backed (ADR-018). No inferred relationship without citable support |

---

## Summary

| ID | Capability | Cost | Reserve now | Build trigger |
|---|---|---|---|---|
| CR-01 | Fact validity / supersession | Substrate | **Yes** | Before Links or value-reporting answers |
| CR-02 | Conflict reconciliation | Path → Substrate | **Yes** | First multi-Memory answer slice |
| CR-03 | Audit seam | Substrate | **Yes** | Before any regulated-boundary release |
| CR-04 | Permission-scoped retrieval | Substrate | **Yes** | First multi-role deployment |
| CR-05 | Re-derivation policy | Path | **Yes** | Before the second model version ships |
| CR-06 | Reproducibility contract | Surface | **Yes** | Soon — cheapest while code is still deterministic |
| CR-07 | Aggregation trust | Path | **Yes** | Before any computed number is shown |
| CR-08 | Selective deletion with proof | Path | **Yes** | Before any retention obligation |
| CR-09 | Knowledge stages | Substrate (planned) | Already reserved | After W1–W3 and CR-01 |

**Nothing here is authorized to be built by this document.** Each entry becomes an
ADR when its trigger fires.

---

## What the substrate already carries (do not lose these)

Recorded because they are the reason the capabilities above are *additions*
rather than *rewrites*:

- **Evidence classes** (`DIRECT`, `VALIDATED_OBSERVATION`, `RETRIEVAL_SIGNAL`,
  `HYPOTHESIS`) — the audit primitive regulated buyers need, and most systems do
  not have it at all
- **Abstain over fluency** as a lead invariant — cannot be retrofitted onto a
  system optimised to always speak
- **Locators generalised ahead of modality** — `AudioSegmentEvidenceLocator`
  exists before audio ingest does
- **`VISUAL_OBSERVATION`** already an evidence kind before any VisionEngine
- **Capability availability** that degrades honestly instead of pretending
- **Domain purity** — 74 files, zero Android imports, so portability off Android
  is mechanical

---

## Document history

| Date | Change |
|---|---|
| 2026-09-04 | v1: CR-01…CR-09 registered against the founder Core definition |
