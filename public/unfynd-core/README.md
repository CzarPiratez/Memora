# UNFYND Core: Public Specification / Contract pack

**License:** [Apache License 2.0](LICENSE)  
**Product site:** [https://www.unfynd.com/](https://www.unfynd.com/) · [Core](https://www.unfynd.com/core)  
**Release class:** Class A: Public Specification / Contract (docs + synthetic examples)  
**Pack revision:** 2026-09-05 (scope-aware retrieval direction)

**Quick start (validate synthetic examples):**

```bash
python tools/validate_class_a_examples.py --self-test
```

See [`BUILDING.md`](BUILDING.md).

**UNFYND Core®** is on-device **memory and intelligence infrastructure**: how
memory and intelligence become something you can ship, audit, and extend in
places where the data has to stay put.

This repository publishes curated, inspectable foundations under Apache-2.0. It
is **not** the UNFYND App, and it is **not** a claim that the full stack is open
source. Commercial product and enterprise technology licensing (when offered) is
**separate** from this Apache-2.0 contracts pack.

---

## Vision

UNFYND Core turns multimodal data into lasting, inspectable intelligence that
can run where the data already lives: on phones, laptops, workstations, and
controlled networks. It is infrastructure you build on when the corpus cannot
be treated as someone else’s training set.

Remotely hosted models are easy to call. What remains hard is durable memory and
grounded reasoning that do not require shipping the corpus to a third party.
Anywhere the data is sensitive, sovereign, disconnected, or simply too valuable
to upload by default, on-device and site-local intelligence is the rational
architecture. Core is built for that architecture.

**Core is the system underneath. You build the product.**

| | |
|---|---|
| **What Core provides** | On-device ingest and retention for multimodal data. Recall and ranking across that store. Answers tied to evidence you can open. Runs where you set the boundary: device, site, or air gap. |
| **What you build** | The app, the workflow, the domain rules. Wire new sources. Specialize retrieval and ranking. Author the permission policy for your environment. Ship to clinics, field teams, enterprises, or consumer devices. Same Core, your product on top. |

UNFYND Core is **not** a personal AI or assistant product. Assistants,
companions, and vertical tools are applications that can be built **on** Core.
**UNFYND App** is one multiplatform product surface that uses this substrate.

---

## What Core enables (capability and industries)

When memory and intelligence can stay with the data, whole categories of product
become buildable. The trust envelope changes; the machinery does not invent a
new category for each vertical.

**How (primitives):** ingest and retain multimodal material as a durable local
record → recall what is relevant → rank competing candidates → produce outputs
tied to evidence you can open, at the boundary you set.

**Applications Core is designed to unlock** (direction we are building toward;
not a claim that every vertical product ships in this pack):

See [`APPLICATIONS.md`](APPLICATIONS.md) for the full industry map (Health,
Defence and public safety, Government, Enterprise and SMB, Research and
education, Industrial and field operations, Accessibility, Emergency response,
Aging / companion care).

---

## Where Core is on this path

Core is already carrying real work. UNFYND App runs on this substrate on Android
today: permissioned discovery, deterministic extraction (OCR, document and note
text, image metadata), Memory assembly through a single construction seam,
on-device embeddings, and recall converging on one canonical Find boundary that
explains results from stored evidence.

The current build is deepening that foundation - evidence identity carried
through every retrieval path, so any output can name the stored evidence it
rests on; ranking that composes meaning with the constraints people actually
speak, starting with time and type; and the reranking seam moving to an
on-device cross-encoder.

Next on the path: evidence-tied answers over that ranked set, scope-aware
retrieval for shared and regulated corpora, the staged memory model beyond a
single Asset (Asset → Link → Event → Knowledge), and further product surfaces
beyond the first mobile one.

The contracts in this pack are written for where Core is going, deliberately.
They are the target implementations are held to, including ours.

---

## What this pack opens now

Under Apache-2.0, this Class A pack publishes:

- Core vision, local-first rules, and builder model ([`SPEC.md`](SPEC.md))
- Capability and industry applications ([`APPLICATIONS.md`](APPLICATIONS.md))
- Asset → Memory → evidence model and evidence classes
- Capability seams (Vision, OCR, Document, Embedding, MemoryBuilder, RecallRanker)
- Truth-before-intelligence / retrieval-first principles
- Staged memory direction (Asset → Link → Event → Knowledge) as future stages
- Low-power posture as an event-driven Memory lifecycle
- Synthetic illustrative examples ([`examples/`](examples/))
- Openness phases ([`ROADMAP-OPEN.md`](ROADMAP-OPEN.md))

**UNFYND App** and proprietary assets stay private until a later decision.

| Mode | This pack |
|---|---|
| **Inspect** | Read `SPEC.md`, `APPLICATIONS.md`, and `examples/`. |
| **Build on** | Design against the seams and evidence rules; run `tools/validate_class_a_examples.py` on sketches. |
| **Later phases** | Domain reference library and further open foundations arrive as pack updates. |

---

## Pack map

| File | Role |
|---|---|
| [`SPEC.md`](SPEC.md) | Public contracts (definition, evidence, seams, principles) |
| [`APPLICATIONS.md`](APPLICATIONS.md) | Capability and industry applications |
| [`examples/`](examples/) | Synthetic Memory / evidence sketches |
| [`BUILDING.md`](BUILDING.md) | Run the conformance validator |
| [`schema/`](schema/) | JSON Schema sketch for examples |
| [`tools/`](tools/) | Validator CLI |
| [`EXPORT_CONTRACT.md`](EXPORT_CONTRACT.md) | Evidence Package export planning sketch |
| [`INTEGRATION.md`](INTEGRATION.md) | Integration surface planning sketch |
| [`ROADMAP-OPEN.md`](ROADMAP-OPEN.md) | What is open now vs later openness phases |
| [`PUBLIC_CHANGELOG.md`](PUBLIC_CHANGELOG.md) | Pack revision history |
| [`SECURITY.md`](SECURITY.md) | Security reporting |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) / [`GOVERNANCE.md`](GOVERNANCE.md) | How this public repo is maintained |

---

## How to use these docs

**Builders:** Treat `SPEC.md` as the public contract surface. Implement against
the seams and evidence rules; do not invent facts the evidence classes forbid.
Use `APPLICATIONS.md` for the capability picture and `examples/` as illustrations.

**Auditors:** Use this pack to check local-first boundaries, evidence honesty,
and pack scope (contracts vs full stack). For private-monorepo maintainers,
[`CITATIONS.md`](CITATIONS.md) points at internal distillation sources (not
required for external readers).

**Security reporters:** See [`SECURITY.md`](SECURITY.md). Do not send secrets
into public issues.

---

## Trademark

UNFYND® and UNFYND Core® are product marks. Apache-2.0 does not grant trademark
rights beyond reasonable attribution (see `NOTICE` and the License §6).
