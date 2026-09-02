# Post-MVP program (V1)

**Status:** Living program plan — **not hashed**  
**Opened:** 2026-09-01  
**Authority:** Informs sequencing and gates only. Does **not** authorize MIG-* steps,
Freeze reopen, Grounded Answers implementation, Connect, Act, new Find paths, or
marketing **AVAILABLE** by itself. Each phase slice still requires change control
and ADR where governance demands it.  
**Governing order:** `docs/GOVERNANCE.md` (Product Contract, Local AI Spec,
Experience Memory Amendment, Architecture Freeze, Migration Spec, Grounding
Architecture supersede this document on conflict).

---

## §0 Public narrative (do not duplicate here)

Product and Core positioning for visitors, investors, and builders lives on:

- [https://www.unfynd.com/](https://www.unfynd.com/)
- [https://www.unfynd.com/core](https://www.unfynd.com/core)
- Private monorepo root `README.md`
- Public Class A pack `public/unfynd-core/README.md`

This document is **engineering program authority** only. It does not replace
marketing copy or the public executive summaries already on those surfaces.

---

## §1 Purpose and scope

### What this document is

- The **single post-MVP program plan** for UNFYND Core + UNFYND App engineering.
- A **gate-driven** map from MVP exit through platform maturity (phases P1–P6).
- A **foundation checklist** — schema-safe work that must land before later
  phases to avoid rework.
- A **Core vs App ownership matrix** pointer and open-source ladder aligned with
  `docs/CORE_APP_SEPARATION_PLAN.md` and `public/unfynd-core/ROADMAP-OPEN.md`.

### What this document is not

- Not architectural authority (see Freeze, Spec, Amendment, Grounding).
- Not a shipping schedule or date plan.
- Not permission to implement deferred backlog items without change control.
- Not a grant, investor, or third-party application document (no such framing in
  the monorepo).

### Companion documents (read together)

| Document | Role |
|---|---|
| `docs/ROADMAP.md` | MVP phase gates through PRD MVP |
| `docs/UNFYND_VISION_ALIGNMENT.md` | Vision vs MVP boundary (not authority) |
| `docs/FUTURE_CAPABILITY_BACKLOG.md` | FC-* / FD-* / X-* option IDs |
| `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` | MIG-* sequencing when authorized |
| `docs/CORE_APP_SEPARATION_PLAN.md` | Open-source mechanics and Gradle phases |
| `docs/GROUNDING_ARCHITECTURE.md` | Converse / Grounded Answers constitution |
| `docs/MVP_EXIT_AUDIT.md` | Honest PASS/FAIL/PARTIAL inventory for MVP exit |
| `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` | Meaning recall enterprise checklist |
| `docs/RECALL_CONVERGENCE_DONE.md` | Canonical Recall program exit (complete) |
| `docs/LEGACY_RECALL_SURFACE.md` | Live/Dual allowlist (shrink-only) |

---

## §2 MVP exit gate (definition of “done before post-MVP”)

Post-MVP phases **P1+** open only after the MVP exit gate is **audited** and
**blockers are explicit**. Marketing **AVAILABLE** is a **separate** founder
decision after engineering gates pass — never inferred from this program doc.

### In scope for MVP exit (PRD MVP sources)

Asset types: **photos, screenshots, PDFs, notes** (OneNote-class connector).

### Required capabilities at exit

| Capability | Authority / evidence |
|---|---|
| Permissioned discovery for MVP asset types | `docs/ROADMAP.md` Phase 1–2 |
| Deterministic extraction + Memory build | Phase 2–3 exit gates |
| Keyword Find + Explain (Why) on saved Memories | Phase 4; `RECALL_CONVERGENCE_DONE` |
| Find by meaning on indexed corpus (honest degraded/candidate copy) | Local-AI track; not silent keyword-as-meaning |
| Corpus completeness honesty (indexed / pending / blocked) | FC-04 delivered |
| Canonical Recall as sole product Find boundary | `LEGACY_RECALL_SURFACE.md` Live/Dual **N = 0** |
| Read-only originals invariant | Product Contract |
| Offline core path after on-device pack installed | Local AI Spec; **A-01** |

### Open engineering gates (as of program open — see audit for current PASS/FAIL)

| Gate | Status pointer |
|---|---|
| **A-01** offline end-to-end proof | `docs/LOCAL_AI_BENCHMARK_PLAN.md`; `docs/ROADMAP.md` |
| **Product AVAILABLE decision** | `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` §B |
| Battery / latency budgets for index + query | Enterprise completion §C |
| Physical **midrange_arm64** re-validation after recent recall fixes | Audit + benchmark plan |

### Explicitly out of MVP exit (do not block exit on these)

| Item | Authority |
|---|---|
| Connect (Links / Event / Knowledge Memory) | `docs/UNFYND_VISION_ALIGNMENT.md` |
| Grounded Answers generative runtime | `docs/GROUNDING_ARCHITECTURE.md` |
| Act / agentic tooling | ADR-043 — out of current architecture |
| Audio / video / meeting Asset types | PRD exclusions until ADR per type |
| Cloud storage connectors (Drive, iCloud, NAS) | Post-MVP P5 |
| MCP server / full integration surface | Post-MVP P6 planning |
| Class B commercial SDK | Separate ADR |

**Audit record:** `docs/MVP_EXIT_AUDIT.md` (living; update when evidence changes).

---

## §3 North star (engineering framing)

**Evidence-backed life memory on device** — findable, explainable, optionally
answerable with abstain, eventually connectable and actionable — with **UNFYND
Core** as the portable, auditable substrate builders and institutions can ship
inside their boundary.

Product progression (staged, not all implemented):

```text
See → Remember → Connect → Understand → Converse → Act
```

Core lifecycle (construction and recall):

```text
Discover → Extract → Understand → Store → Recall → Explain
```

---

## §4 Program phases (gate-driven)

Phases open when **exit criteria** of the prior gate are met — not on calendar.

```text
MVP EXIT AUDIT complete (blockers named)
        │
        ▼
P1 Trust hardened
        │
        ▼
P2 Recall excellence
        │
        ▼
P3 Converse (governed)
        │
        ▼
P4 Connect (life)
        │
        ▼
P5 Multimodal + connected sources
        │
        ▼
P6 Platform (export, integration, Class B path)
        │
        └──► Act (separate ADR after P3 + P4 prove trust)
```

### Phase summary

| Phase | Name | User-visible outcome | Primary backlog IDs |
|---|---|---|---|
| **Exit** | MVP AVAILABLE (engineering) | Reliable Find + Why on MVP types; corpus honesty; offline proof | A-01, enterprise completion |
| **P1** | Trust hardened | Deeper Explain lineage; zero-egress checklist started | FC-06, FC-05 (candidate) |
| **P2** | Recall excellence | Meaning Find quality jump on real libraries | FC-02, FC-01, FC-03 |
| **P3** | Converse (governed) | Ask → cited answer or explicit abstain; Find remains safety rail | Grounding §14 gates; FD-05 later |
| **P4** | Connect | Evidence-backed cross-asset relationships; cautious Event/Knowledge | ADR-018; FD-07 UI later |
| **P5** | Multimodal + sources | Audio → video; connector-at-source (Drive, iCloud, …) | New Asset ADRs; OneNote template |
| **P6** | Platform | Evidence Package export/import; Core API; MCP adapter; federated sync design | FC-05; export contract; Class B ADR |
| **Act** | Governed action | Verifier-gated tool proposals; read-only originals preserved | ADR-043 successor ADR only |

### P1 — Trust hardened

**Opens when:** MVP exit audit complete; blockers for AVAILABLE named.

**Delivers:**

- FC-06: evidence lineage in Explain (model id, revision, extraction schema where applicable).
- FC-05 planning / checklist: zero-egress verification pack (no product feature required first).

**Does not authorize:** Grounded Answers UI, Connect, Act.

### P2 — Recall excellence

**Opens when:** P1 started **or** MVP exit audit shows recall **quality** as top user blocker; **FC-02 change control** authorized.

**Delivers:**

- FC-02 / **ADR-051:** Evidence-native on-device `RecallRanker` over top-k
  `MemoryEvidence` inside Canonical Recall — **Stage A** measured semantic head
  (bounded pool), then **Stage B** structured evidence signals. Public
  “evidence-native ranker” language only after Stage B measured green.
- FC-01: RRF fusion when authorized with MIG-07B convergence (ranking convergence complete per `RECALL_CONVERGENCE_DONE`; FC-01 still needs its own change control).
- FC-03: evidence chunking policy per asset type (stable passage boundaries) —
  **hard dependency before ADR-051 Stage B complete**.

**Architectural rule:** All ranking stays inside Canonical Recall. Live/Dual N must remain **0**.

### P3 — Converse (governed)

**Opens when:** MVP exit **PASS** on core Find paths; grounding domain interfaces landed; `GROUNDING_ARCHITECTURE.md` §14 gates satisfied for the chosen vertical slice.

**Delivers:**

- Grounded Answers **PDF + notes vertical slice** first (existing acceptance specs).
- Verifier gate; abstain UX as strength, not failure.
- `ReasoningEngine` behind existing seam — no chat-first product entry (`GROUNDING_ARCHITECTURE.md` §16).

**Does not authorize:** persistent chat personality, open-ended agent, Act.

### P4 — Connect (life)

**Opens when:** P3 demonstrates abstain on incomplete corpus; ADR-018 change control authorized.

**Delivers:**

- Evidence-backed Links (not embedding-only graph).
- Event Memory and Knowledge Memory per Experience Memory Amendment — cautious staging.
- Bi-temporal query design on claims (valid-time vs recorded-time) — productize revision lineage.

### P5 — Multimodal + connected sources

**Opens when:** Locator fields stable in domain; audio Asset ADR accepted; OneNote connector retained as connector template.

**Sequence inside P5:**

1. **Audio** Asset adapter (ADR per type) — temporal locators on evidence.
2. **Cloud storage connector** (index-at-source; network for source read only).
3. **Video** Asset adapter — after audio pipeline proven.

### P6 — Platform

**Opens when:** Export contract stable in Class A pack; integration ADR authorized.

**Delivers:**

- Evidence Package export/import (portable cited evidence — not raw Assets).
- Core local API (primary); thin MCP adapter (compatibility).
- Optional import bridges for external memory protocols as **candidate evidence**, not truth.
- Federated self-hosted sync **design** (encrypted replica mesh).
- Class B path per `OPEN_SOURCE_COMMERCIAL_STRATEGY` — separate ADR.

### Act (outside P1–P6 until re-authorized)

Deferred per ADR-043 until:

- Asset Memory quality proven on expanded corpus.
- Connect relationships evidence-backed.
- Grounded Answers abstains correctly.
- Action classes, consent, and audit defined in new ADR + Product Contract amendment.

---

## §5 Core vs App ownership matrix

Summary — full detail in `docs/CORE_APP_SEPARATION_PLAN.md` §5.

| Concern | Core (portable / open candidate) | App (private product) |
|---|---|---|
| Domain models (`Memory`, `Asset`, evidence classes) | Open candidate | Uses Core |
| Capability interfaces (`MemoryBuilder`, `EmbeddingEngine`, …) | Open candidate | DI wiring |
| `CanonicalRecall`, recall use cases | Open candidate after API semver | ViewModels |
| Room / MediaStore / SAF / MSAL | `core-android` or private initially | Today: `data/` |
| AI Pack store, weights, ledger | Private | Private |
| Compose UI, setup, product copy | Private | Private |
| OneNote / product connectors | Pattern in App; ports in domain | Private |
| Class A docs + synthetic examples | `public/unfynd-core/` | Distilled from private docs |

**Gradle target:** Phase 2 `:core-domain` module (not started). Do not publish runnable Core source until validator + API stability gates in §6 pass.

---

## §6 Open-source ladder (Class A → Class B)

Aligned with `public/unfynd-core/ROADMAP-OPEN.md`. **Grant-neutral** — no
third-party program language in repo.

| Step | Artifact | Gate |
|---|---|---|
| **Now** | Class A SPEC, APPLICATIONS, synthetic examples, governance | ADR-047 delivered |
| **Step 1** | JSON Schema + conformance validator + CI + `BUILDING.md` | Change control `CHANGE_CONTROL_CLASS_A_CONFORMANCE_VALIDATOR.md` |
| **Step 2** | `EXPORT_CONTRACT.md` + `INTEGRATION.md` planning sketches | Same validator release or follow-up pack PR |
| **Step 3** | Expanded synthetic fixtures (audio locator, export bundle) | ADR-048 synthetic only |
| **Step 4** | `:core-domain` Gradle module in private monorepo | Phase 2 `CORE_APP_SEPARATION_PLAN` |
| **Step 5** | Semver-stable `CanonicalRecall` + result types as public API | `RECALL_CONVERGENCE_DONE` + FC-02/other ranking stable |
| **Step 6** | Domain grounding types in public pack | Grounding interfaces stable |
| **Step 7** | Runnable Core source on public GitHub | Phase 4 ADR + strategy §23 gates |
| **Later** | Class B enterprise SDK / OEM | Separate ADR |

Publishing `public/unfynd-core/` to the public remote is **out of band** per
`ROADMAP-OPEN.md` — copy/subtree only; never push the private monorepo.

---

## §7 Foundation checklist (code before phase opens)

Land these **during MVP exit / early P1–P2** to avoid post-MVP schema churn.

| Foundation | Phase unlocked | Verification |
|---|---|---|
| **Locators on evidence slices** (page, span; fields for timecode/segment) | P5 audio/video | Domain types + persistence; examples in Class A |
| **`modelSignature` on stored embeddings** | Embedding engine swap (FD-01) | Room + domain; migration if needed |
| **`CorpusCoverage` in domain** (not UI-only) | P3 abstain with PARTIAL/UNKNOWN | FC-04 UI already; domain emission |
| **Revision ID on every Memory write** | P4 bi-temporal replay | Experience Memory Amendment |
| **Grounding domain interfaces** (`EvidencePackage`, `ReasoningTask`, `StructuredAnswer`) + fakes | P3 | Unit tests; no generative runtime |
| **`RecallRanker` port** (stub → FC-02 impl) | P2 | Behind Canonical Recall only |
| **Connector template** (discover → extract → MemoryBuilder → Find) | P5 | OneNote N0–N7 closed |
| **CanonicalRecall result contract frozen** | P6 public API | `CANONICAL_RECALL_RESULT_CONTRACT.md` |

---

## §8 Integration strategy (planning)

Layered exposure — **not MCP-only** (detail in `public/unfynd-core/INTEGRATION.md`).

1. **Core local API** — typed Evidence Package, Canonical Recall (primary for OEM / vertical).
2. **MCP adapter** — read-only Find + Evidence Package for external tools (compatibility).
3. **OS semantic hooks** — Android AppSearch donation / platform hooks (optional, later).
4. **Import bridges** — external memory protocol files land as **candidate evidence**, not truth.

Implementation waits for P6 authorization except planning sketches in Class A.

---

## §9 Success metrics per phase (examples)

| Phase | Engineering | User-visible |
|---|---|---|
| MVP exit | A-01 PASS; audit blockers closed or accepted | Find + Why on MVP types on physical device offline |
| P1 | Lineage fields in Why; zero-egress checklist draft | User sees model/revision in Explain |
| P2 | Recall@k lift on fixture corpus; latency budget | Meaning Find feels precise on multi-page PDFs |
| P3 | Abstain rate on incomplete corpus tests | “Not enough evidence” shown honestly |
| P4 | Link validation tests; no orphan claims | “These items relate” with citations |
| P5 | Audio ADR tests; connector E2E | Meeting recall demo (synthetic corpus) |
| P6 | Export round-trip validator; API semver | “Your Memory, your file” (cited slices only) |

---

## §10 Explicit non-goals

Recorded to prevent scope creep (`FUTURE_CAPABILITY_BACKLOG.md` Tier C aligned):

| ID | Non-goal |
|---|---|
| X-06 | Chat-first / opaque AI entry hiding Find |
| X-01 | Intelligent file routing (mutates sources) |
| X-02 | GraphRAG community summaries |
| X-03 | Multi-agent orchestration in Core |
| X-05 | Generic knowledge graph primitive |
| — | Act before P3 + P4 trust proof |
| — | New product Find paths (Live/Dual N > 0) |
| — | Marketing AVAILABLE before MVP exit audit + founder decision |
| — | Grant / third-party application language in monorepo |
| — | Full App or AI pack open source without Class B ADR |

---

## Appendix A — Backlog ID quick reference

| ID | Summary | Tier |
|---|---|---|
| FC-01 | RRF keyword + meaning fusion | A |
| FC-02 | Cross-encoder rerank | A |
| FC-03 | Evidence chunking policy | A |
| FC-04 | Corpus completeness honesty UI | **delivered** |
| FC-05 | Zero-egress verification pack | A |
| FC-06 | Evidence lineage in Explain | A |
| FD-01–07 | Deferred model/runtime/ANN/CRAG/graph UI | B |
| X-01–07 | Out of scope | C |

Full rows: `docs/FUTURE_CAPABILITY_BACKLOG.md`.

---

## Appendix B — Change control and MIG pointers

| Topic | Record |
|---|---|
| Post-MVP program open | `docs/CHANGE_CONTROL_POST_MVP_PROGRAM_V1.md` |
| MVP exit audit | `docs/CHANGE_CONTROL_MVP_EXIT_AUDIT.md` |
| Class A validator | `docs/CHANGE_CONTROL_CLASS_A_CONFORMANCE_VALIDATOR.md` |
| Canonical Recall exit | `docs/RECALL_CONVERGENCE_DONE.md` |
| MIG sequencing | `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` |
| Meaning search anchor fix | `docs/CHANGE_CONTROL_MEANING_SEARCH_ANCHOR_FIX.md` |

---

## Document history

| Date | Change |
|---|---|
| 2026-09-01 | Initial program V1 opened |
