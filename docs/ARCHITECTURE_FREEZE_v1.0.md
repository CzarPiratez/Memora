# Architecture Freeze v1.0

**Status:** ARCHITECTURE FROZEN
**Type:** Governance declaration only. This document introduces no new architecture,
principles, or requirements. It records that the architecture defined by the
canonical documents below is closed for open decision-making and certifies the
process by which it may change in the future.

---

## 1. Canonical document list

✓ `PRODUCT_CONTRACT.md`
✓ `LOCAL_AI_TECHNICAL_SPEC.md`
✓ `EXPERIENCE_MEMORY_AMENDMENT_V1.md`
✓ `ARCHITECTURAL_MIGRATION_SPEC_V1.md`

These four documents, together, are the complete and exclusive architectural
authority for Memora as of this freeze. No other document, comment, prior draft, or
conversation record carries architectural authority, regardless of when it was
written.

## 2. Authority hierarchy

1. **`PRODUCT_CONTRACT.md`** — foundational product and privacy commitments. Nothing
   below may contradict it.
2. **`LOCAL_AI_TECHNICAL_SPEC.md`** — accepted engineering authority. Supersedes the
   original PRD only where an implementation detail would otherwise require
   contradicting on-device, evidence-first execution. Subordinate to
   `PRODUCT_CONTRACT.md`.
3. **`EXPERIENCE_MEMORY_AMENDMENT_V1.md`** — binding amendment governing the staged
   Asset → Link → Event → Knowledge memory model, evidence classes, and the
   Explain/Trust contract. Subordinate to `PRODUCT_CONTRACT.md` and
   `LOCAL_AI_TECHNICAL_SPEC.md`; where it defines behavior not otherwise specified by
   either, it is authoritative.
4. **`ARCHITECTURAL_MIGRATION_SPEC_V1.md`** — sequencing and implementation-planning
   authority only. It does not define architecture; it defines the order in which the
   architecture defined by documents 1–3 is realized. Where it appears to imply a
   rule not stated in documents 1–3, documents 1–3 govern.

No two documents in this hierarchy define the same rule differently. Where this
freeze found and corrected an instance of that (see the audit below), the correction
is already reflected in the published documents.

## 3. Frozen architectural principles

The following principles are closed. They are not subject to revision without a
documented architectural flaw (§5) and the change process in §6:

- **Truth before intelligence** — no evidence means no assertion; uncalibrated
  confidence means no precise confidence claim.
- **Evidence-first** — every summary, anchor, relationship, and displayed confidence
  value must be traceable to stored evidence; nothing is asserted that isn't backed.
- **Retrieval-first** — recall surfaces stored, evidence-backed material; it does not
  generate unsupported answers.
- **One memory substrate** — `Memory` and its evidence are the sole representation of
  what Memora knows about an Asset.
- **One evidence substrate** — all search, ranking, and explanation read from
  `MemoryEvidence`; no parallel raw-extraction search path is permitted.
- **One canonical recall pipeline** — literal and semantic matching are
  candidate-generation mechanisms feeding one structured-filter-and-ranking stage,
  not separate pipelines.
- **Confidence is derived, never stored** — computed at recall/explain time from
  evidence class, anchor coverage, and integrity/freshness state; `RecallRanker`
  never emits it directly.
- **Retrieval signals never independently justify truth** — an item classified as a
  retrieval signal may generate candidates only; it can never by itself justify a
  durable claim, a confidence value, or an explanation element.
- **Mandatory sandboxing of untrusted parsing** — any parsing of external byte
  content or markup not decoded through a platform-guaranteed-safe API must execute
  inside the isolated-process sandbox boundary.
- **Anchor-based filtering is advisory by default** — structured constraints become
  mandatory, excluding filters only when a query's constraint interpretation resolves
  a cue to explicit rather than advisory; absence of an anchor is never treated as a
  non-match; new anchor coverage may only add ranking signal, never retroactively
  convert an existing query from advisory to exclusionary.
- **Explainability is a first-class trust surface** — "Why this result?" must state,
  per constraint, whether it was satisfied, unmatched, or unavailable, and must never
  imply an unconfirmed cue was confirmed.
- **Determinism and recoverability** — capability contracts expose availability,
  version identity, bounded limits, recoverable failure, and cancellation; no
  capability may silently degrade into an unbounded or irrecoverable state.
- **Local-first, user-controlled data** — the user can revoke source access, remove a
  source, clear derived data, or clear all Memora data, and no future capability may
  override that control.

## 4. Change policy after freeze

From this point forward:

- No new canonical architectural specification is created unless it is proven
  indispensable by a genuine architectural flaw discovered during implementation
  (§5). The bar is proof, not convenience or perceived incompleteness.
- No existing canonical document is revised to introduce a new principle. Documents
  may only be corrected for a demonstrated flaw, per the process in §6.
- Implementation questions — data structures, algorithms, libraries, performance
  tuning, UI composition, ranking weights, and similar decisions — are never
  architectural questions and are never grounds to reopen this freeze.
- Future capabilities explicitly staged as future architecture (Link, Event Memory,
  Knowledge Memory, personalization, multi-device sync, ANN-backed retrieval) remain
  governed exactly as already specified in the frozen documents. Beginning their
  implementation does not itself constitute a change to this freeze; it is executing
  architecture that is already written.

## 5. Definition of an architectural flaw

An architectural flaw is a discovery, made during implementation, that satisfies
**all** of the following:

1. It is a case where the frozen documents are silent, self-contradictory, or
   demonstrably force two competent engineers to build materially different, mutually
   incompatible behavior from the same text.
2. It cannot be resolved by choosing an implementation detail — the ambiguity exists
   at the level of a principle or contract, not a technique.
3. It was not already deliberately deferred as future architecture, safe-to-defer
   implementation work, or intentionally out of scope by a prior review.

The following are explicitly **not** architectural flaws and must not be used to
justify reopening this freeze:

- A missing feature.
- A desire to improve retrieval quality, performance, or user experience.
- A new product idea, however good.
- An implementation being harder or slower than expected.
- A capability that is correctly staged as future architecture not yet being built.

## 6. Rules for future architectural changes

1. A claimed architectural flaw must be documented in writing: the exact clause or
   silence in the frozen documents, the two (or more) incompatible implementations it
   permits, and why the incompatibility cannot be resolved as an implementation
   detail.
2. The correction must be the smallest edit that resolves the flaw — an amendment to
   the existing document and section where the flaw was found, not a new document,
   unless the flaw's resolution genuinely cannot be expressed as an amendment to any
   existing canonical document.
3. The correction is recorded in `CHANGELOG.md` with the flaw, the reasoning, and the
   resolution, in the same historical-traceability style as every entry preceding it.
4. Architecture Freeze remains in effect throughout this process. A documented flaw
   and its correction do not reopen the rest of the frozen architecture to revision.

---

## 7. Final cross-document consistency audit

Performed across all four canonical documents, limited to: contradictory
terminology, conflicting authority, inconsistent sequencing, duplicated
responsibility, unresolved ambiguity, and architectural circular dependencies.
Implementation, optimization, future capabilities, and product ideas were excluded
from scope, as instructed.

**Findings and corrections:**

- **Duplicated responsibility (found and corrected):** Prior to this freeze,
  `LOCAL_AI_TECHNICAL_SPEC.md` §15 and `EXPERIENCE_MEMORY_AMENDMENT_V1.md` §7 each
  independently stated a version of the retrieval-signal rule. After this freeze's
  amendment generalized §7 to explicitly cover confidence values and explanation
  elements, §15's narrower restatement would have left the two documents asserting
  different scopes for the same rule. **Correction applied:** §15 now states the
  principle once, in summary, and defers to §7 as the single authoritative statement,
  eliminating the duplication.

- **Terminology variance (reviewed, no correction required):** `MemoryBuilder`
  (interface identifier) versus "Memory Builder" (prose name for the same subsystem),
  and "Context/Correlation Engine" (shorthand used in the migration spec) versus
  "Context and Correlation Engine" (`EXPERIENCE_MEMORY_AMENDMENT_V1.md`'s full name
  for the same not-yet-built subsystem). Both are consistent in meaning and referent
  across all four documents; the variance is typographic register (code identifier
  vs. prose), not conflicting terminology. No correction applied.

- **Authority hierarchy:** Confirmed one-directional. `ARCHITECTURAL_MIGRATION_SPEC_V1.md`
  cites `LOCAL_AI_TECHNICAL_SPEC.md` and `EXPERIENCE_MEMORY_AMENDMENT_V1.md` as
  authority throughout; neither of those documents references the migration spec as
  authority for any rule. No circular dependency found.

- **Sequencing:** `ARCHITECTURAL_MIGRATION_SPEC_V1.md`'s migration order was checked
  against every rule this freeze added or amended; no migration in that document
  assumes a rule the amended documents no longer state, and no migration's stated
  dependency contradicts another's. No inconsistency found.

- **Unresolved ambiguity:** The six items resolved across the closure review and this
  freeze (confidence derivation, `RecallRanker`'s output boundary, the generalized
  retrieval-signal invariant, the unified recall pipeline, mandatory sandboxing
  threshold, and refined anchor semantics) were the last remaining ambiguities
  identified across both review passes. No further ambiguity was found in this final
  audit.

**Certification:**

> The Memora architecture is internally consistent and Architecture Freeze v1.0 is
> approved.

---

*This declaration is governance only. It introduces no new architecture. Continued
validity depends on the change process in §6 being followed for any future
correction.*
