# UNFYND Core — Public Specification / Contract pack

**License:** [Apache License 2.0](LICENSE)  
**Product site:** [https://www.unfynd.com/](https://www.unfynd.com/)  
**Release class:** Class A — Public Specification / Contract (docs + synthetic examples)  
**Pack revision:** 2026-08-29

This repository is the curated **open, inspectable** contracts pack for
**UNFYND Core — on-device memory and intelligence infrastructure**.

It is **not** the UNFYND App, and it is **not** a claim that the full stack is
open source.

Commercial product and enterprise technology licensing (when offered) is
**separate** from this Apache-2.0 contracts pack.

---

## UNFYND App vs UNFYND Core

| | **UNFYND App** | **UNFYND Core** |
|---|---|---|
| What it is | The product people use across Android, Windows, iOS, and Mac | On-device **memory and intelligence infrastructure**: durable multimodal memory next to data that cannot leave; local-first; evidence-backed |
| Role | Multiplatform product surface | The substrate: Asset → Memory → evidence, capability seams, retrieval and explain contracts |
| Openness (today) | Private until a later decision | **This pack** opens contracts and specs under Apache-2.0 |

Search and Find are capabilities of the product. They are not the whole definition
of UNFYND Core.

---

## What this pack opens

Under Apache-2.0, this pack publishes plain-language contracts for:

- Core definition and local-first / no-cloud-core-path rules
- Asset → Memory → evidence model (originals stay with the user / OS)
- Evidence classes and truth-before-intelligence / retrieval-first principles
- Capability seams (Vision, OCR, Document, Embedding, MemoryBuilder, RecallRanker)
- Staged direction (Asset → Link → Event → Knowledge) as **future**, not shipped
- Low-power posture as an event-driven Memory lifecycle
- Explicit non-claims
- Honest **status** vs App today ([`SPEC-STATUS.md`](SPEC-STATUS.md))
- **Synthetic** illustrative examples ([`examples/`](examples/))

Start with [`SPEC.md`](SPEC.md). See also [`SPEC-STATUS.md`](SPEC-STATUS.md),
[`examples/`](examples/), [`ROADMAP-OPEN.md`](ROADMAP-OPEN.md),
[`SECURITY.md`](SECURITY.md), [`PUBLIC_CHANGELOG.md`](PUBLIC_CHANGELOG.md),
[`CONTRIBUTING.md`](CONTRIBUTING.md), and [`GOVERNANCE.md`](GOVERNANCE.md).

---

## How to inspect / build on / what’s next for run

| Mode | Today |
|---|---|
| **Inspect** | Read `SPEC.md`, `SPEC-STATUS.md`, and `examples/` under Apache-2.0. |
| **Build on** | Design and review against the seams and evidence rules; pair with your own tests and device budgets. Examples are sketches, not schema locks. |
| **Run** | No public runnable validator or Core runtime is shipped in this pack yet. A docs-aligned validator may come later under a separate decision. |

---

## What is not open yet

Until a later public decision (and typically a new ADR in the private monorepo):

- The UNFYND App and its private monorepo
- AI Pack weights, manifests, and proprietary configs
- Proprietary tuning, evaluation corpora, and quality fixtures
- Secrets, keystores, credentials, and machine-local config
- Commercial / enterprise technology packages (Release Class B — future)

Class A means **contracts and specs are inspectable**. It does **not** mean the
whole stack is open.

---

## Honesty on vision and status

We are **building toward** open on-device memory and intelligence.

- Do **not** read this pack as a claim that Converse, Act, agents, or marketing
  **AVAILABLE** semantic recall are shipped.
- Product direction may be described as See → Remember → Connect → Understand →
  Converse → Act. **Act** (agentic action on the user’s behalf) remains **out of
  current architecture** until an explicit later product-contract change.
- Grounded Answers (evidence-backed Q&A over stored evidence) is a governed
  future capability — architecture exists; generative implementation is not
  claimed here as shipped.
- For an honest App column, see [`SPEC-STATUS.md`](SPEC-STATUS.md).

---

## How to use these docs

**Builders** — Treat `SPEC.md` as the public contract surface for interoperability
and design review. Implement against the seams and evidence rules; do not invent
facts the evidence classes forbid. Pair with your own tests and device budgets.
Use `examples/` only as illustrations.

**Auditors** — Use this pack to check local-first boundaries, evidence honesty,
and what is *not* claimed. For private-monorepo maintainers, [`CITATIONS.md`](CITATIONS.md)
points at internal distillation sources (not required for external readers).

**Security reporters** — See [`SECURITY.md`](SECURITY.md). Do not send secrets
into public issues.

---

## Trademark

UNFYND® and UNFYND Core® are product marks. Apache-2.0 does not grant trademark
rights beyond reasonable attribution (see `NOTICE` and the License §6).
