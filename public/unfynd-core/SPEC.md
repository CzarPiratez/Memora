# UNFYND Core - Public Specification (Class A)

**Status:** Public distillation for Release Class A (contracts / specs)  
**License:** Apache License 2.0  
**Noun:** UNFYND Core - on-device memory and intelligence infrastructure  
**Site:** [https://www.unfynd.com/core](https://www.unfynd.com/core)

This document is a plain-language public contract pack. It describes the
substrate we are building toward. Capability and industry applications -
[`APPLICATIONS.md`](APPLICATIONS.md). Synthetic sketches -
[`examples/`](examples/). Pack map - [`README.md`](README.md).

---

## 1. What UNFYND Core is

UNFYND Core is **on-device memory and intelligence infrastructure**: how
multimodal data becomes lasting, inspectable intelligence that can run where the
data already lives - phones, laptops, workstations, and controlled networks. It
is infrastructure you **ship, audit, and extend** when the corpus cannot be
treated as someone else’s training set.

**Core is the system underneath. You build the product.** Core provides ingest
and retention, recall and ranking, and answers tied to evidence you can open, at
the boundary you set (device, site, or air gap). Builders wire sources,
workflows, domain rules, and permissions - for clinics, field teams, enterprises,
research enclaves, consumer devices, and products we have not named yet.

UNFYND Core is **not** a personal AI or assistant. Assistants, companions, and
vertical tools are applications that can sit **on** Core. **UNFYND App** is one
multiplatform product surface that uses this substrate. This pack describes Core
contracts, not the private App implementation.

For the industry map and “how” primitives, see
[`APPLICATIONS.md`](APPLICATIONS.md).

### 1.1 Local-first / no cloud on the core path

After the required on-device capability is installed:

- Core memory creation, retrieval, ranking, and explanation run **locally**.
- The core path does **not** send source content, extracted facts, embeddings,
  prompts, queries, or Memories to a remote AI service.
- Optional cloud features (if ever added) need their own explicit product
  decision, consent, and data-handling contract. They are not the Core path.

---

## 2. Asset → Memory → evidence

Lifecycle (conceptual):

```text
Discover → Extract → Understand → Store → Recall → Explain
```

| Term | Meaning |
|---|---|
| **Asset** | A permitted source item (e.g. photo, screenshot, PDF, approved note). Discovery is permissioned; the user or organization does not manually feed each file as the primary workflow. |
| **Memory** | The searchable semantic representation of an Asset (or later, of linked context). It is **not** a copy, replacement, or owner of the original file. |
| **Evidence** | Bounded, provenance-cited support for what a Memory claims. Explanations cite stored evidence. |

**Originals stay with the user / OS (or approved provider).** UNFYND reads the
minimum needed after explicit access. Originals are **read-only**: Core must not
alter or delete source content.

### 2.1 Memory integrity (high level)

Every Memory has a **stable identity**. Fingerprint, evidence, summary, embedding,
and explanation may evolve through **traceable revisions**; identity does not
silently rewrite. Incomplete, failed, or unavailable derived content must not be
presented as a fully ready Memory.

Memory is a first-class runtime concern: ingest into a durable local record,
retain for continuity, and reason over a working store that stays inside the
boundary you set - not a profile uploaded to improve a remote model.

---

## 3. Evidence classes

Evidence classes describe **support and provenance**, not automatic truth.

| Class | Plain meaning | Can it alone justify a durable claim? |
|---|---|---|
| **DIRECT** | Bounded source-derived fact (metadata, OCR, PDF/note text, EXIF, etc.) | Yes, subject to provenance and validation |
| **VALIDATED_OBSERVATION** | Bounded, provenance-cited local model observation that a validator accepted | Only after validation |
| **RETRIEVAL_SIGNAL** | Similarity, embedding, or ranking information | **No** - candidate generation only |
| **HYPOTHESIS** | Tentative possible relationship | **No** - needs more evidence or user confirmation |

**Truth before intelligence:** no evidence means no assertion; uncalibrated
confidence means no precise confidence claim.

---

## 4. Frozen principles (plain language)

These are binding product rules for Core:

1. **Truth before intelligence** - do not fabricate certainty to appear smart.
2. **Evidence-first** - summaries, anchors, relationships, and shown confidence
   must be traceable to stored evidence.
3. **Retrieval-first** - ordinary recall surfaces stored, evidence-backed
   material; it does not invent unsupported answers.
4. **One memory / evidence substrate** - what Core “knows” about an Asset lives
   in Memory + evidence; explanations cite that store.
5. **Retrieval signals never independently justify truth** - embeddings may help
   find candidates; they do not, alone, make a durable claim true.
6. **Explain from stored evidence** - “Why this result?” uses stored Memory and
   evidence. Ordinary Find / recall does **not** reopen originals and does **not**
   run per-result generative reasoning.
7. **Local-first, user-controlled** - revoke access, remove sources, clear derived
   data; no future Core capability overrides that control.

---

## 5. Capability seams

Domain and application layers depend on **capability contracts**, not on a named
model vendor, HTTP client, or platform SDK.

| Seam | Role |
|---|---|
| **VisionEngine** | Structured, evidence-citable image observations (scene, objects, etc.) |
| **OcrEngine** | Text plus location/provenance suitable for citations |
| **DocumentEngine** | Permitted PDF/note understanding from deterministic extracted text; does not decide source access |
| **EmbeddingEngine** | Versioned vectors for Memory and evidence / query (retrieval signals) |
| **MemoryBuilder** | Combines deterministic extraction and optional local observations into a schema-validated Memory; **must not invent unsupported source facts** |
| **RecallRanker** | Ranks stored candidate Memories and returns the evidence used |

Each seam should expose availability, version identity, bounded limits,
recoverable failure, and cancellation.

**MemoryBuilder** exposes an assemble contract: facts + optional local
observations → schema-validated Memory (or an honest build outcome). Vision and
observation paths mature as Core capabilities advance through product phases.

---

## 6. Staged memory direction (future stages)

Approved long-term staging (phases of the Memory model):

```text
Original read-only Asset
        → Asset Memory
        → evidence-backed Link
        → Event Memory
        → Knowledge Memory
```

- **Asset Memory** - foundation: one permitted Asset version → evidence, anchors,
  evidence-cited summary, provenance.
- **Link** - “these memories appear related,” with inspectable evidence; not a
  silent merge; not embedding-only.
- **Event Memory** - cautious occurrence context referencing members.
- **Knowledge Memory** - evolving, evidence-backed subject object.

Links, Events, and Knowledge are **direction**. They arrive as Core and products
on Core mature.

---

## 7. Low-power posture

“Low power” for Core means the **event-driven Memory lifecycle**, not neuromorphic
hardware and not always-on sensing:

- Do expensive discovery / extraction / understanding **rarely**, under device
  health constraints (batch, battery/storage-aware background work).
- Store durable Memory, evidence, and embeddings.
- Stay quiet until a discovery, fingerprint change, user action, or pack-upgrade
  cue.
- **Recall cheaply** from stored rows.
- Run expensive reasoning when explicitly asked (e.g. future Grounded Answers over
  a frozen evidence package) - not on every ordinary recall result.

Paused indexing under device pressure is honest product behavior, not a silent
failure.

---

## 8. Privacy (high level)

- Originals are read-only.
- Ask for source access before discovery; access is revocable.
- Indexing state should be visible and recoverable.
- Disclose local processing, on-device model/pack storage, and any optional
  network action **before** the user or operator enables it.
- Core creation / retrieval / ranking / explanation do not depend on cloud AI.

---

## 9. Pack scope

This Class A pack opens **contracts and specifications** (and synthetic
examples) under Apache-2.0. It does not open the UNFYND App, proprietary AI
Packs, evaluation corpora, secrets, or Release Class B commercial packages.

Treat staged direction (Links, Events, Knowledge), Grounded Answers, and Act /
agentic action as **phases of the vision** described on the Core site and in
[`APPLICATIONS.md`](APPLICATIONS.md) - not as items bundled in this documentation
release. Marketing performance SLAs are product decisions outside this pack.

---

## 10. How this pack relates to private canon

Private monorepo hashed constitutions (Product Contract, Local AI Spec,
Experience Memory Amendment, Architecture Freeze, Grounding docs) remain
internal authority for implementers. This `SPEC.md` is a **curated public
distillation** for Class A transparency - not a verbatim dump of those files and
not a license to those private paths.
