# UNFYND Local AI Technical Specification

**Product identity:** UNFYND (formerly Memora). Direction: Personal Knowledge Infrastructure. This document’s freeze is the technical invariants below. Naming is not an architectural invariant (ADR-040).

**Version:** 1.3
**Status:** Accepted engineering authority  
**Companion to:** `docs/product-source/Memora.docx`  
**Source basis:** `docs/product-source/Addendum 1.docx` and
`docs/product-source/Addendum 2 Engineering Reference.docx`

## 1. Binding principle

UNFYND remembers on the user's phone, not in the cloud.

**Truth before intelligence:** UNFYND must never fabricate certainty to appear
intelligent. No evidence means no assertion; uncalibrated confidence means no precise
confidence claim.

Everything required to create, store, retrieve, rank, and explain a Memory must
run locally after the required on-device capability is installed. Cloud features,
if introduced later, are explicit optional enhancements. They are never a
dependency for the MVP or normal recall.

This specification supersedes the original PRD only where an implementation detail
would otherwise require cloud AI or repeated source analysis during recall. It does
not weaken the PRD's product commitments, source coverage, read-only source rule,
or evidence-backed explanation rule.

## 2. Definitions

- **Asset:** a stable, source-neutral, read-only reference to original user content.
- **Extraction:** deterministic local facts obtained from an Asset version, such as
  metadata, OCR text, PDF text, or permitted note text.
- **Memory draft:** a private, recoverable intermediate record; it is not searchable.
- **Memory:** the validated, searchable semantic representation derived from one
  Asset version. It never owns or replaces the original Asset.
- **Memory identity:** a stable UNFYND-owned identifier for the same conceptual
  Memory. It survives valid reprocessing and revision.
- **Memory revision:** one immutable, provenance-bearing derived representation of a
  Memory identity. It records why it superseded a prior revision.
- **Evidence:** a source-derived fact or bounded extracted passage that supports a
  Memory field or an explanation.
- **AI Pack:** a locally installed, independently versioned set of model assets and
  metadata. It contains no original user content.
- **Local Intelligence Layer:** the application-facing interfaces that invoke
  eligible on-device capability implementations.

## 3. Canonical local lifecycle

```text
Discover -> Deterministic extract -> Local understanding -> Validate ->
Memory + evidence + embeddings -> Local store -> Recall -> Explain
```

The lifecycle is performed once per meaningful change, not once forever. A new
attempt is justified only by a changed Asset fingerprint, extraction schema,
understanding schema, model capability/version, or an explicit user reindex action.
Search must never reopen or reanalyse an original asset merely to answer a query.

## 4. Local Intelligence Layer contracts

The domain/application layers depend on capability contracts, never on a named
model, vendor SDK, HTTP client, or platform runtime:

- `VisionEngine`: produces structured, evidence-citable image observations such as
  scene, objects, activities, relationships, and a bounded summary.
- `OcrEngine`: produces text plus location/provenance suitable for citations.
- `DocumentEngine`: produces permitted PDF or note understanding from deterministic
  extracted text; it does not decide source access.
- `EmbeddingEngine`: encodes a Memory and a user query into compatible, versioned
  semantic vectors.
- `MemoryBuilder`: combines deterministic extraction and local observations into a
  schema-validated Memory; it may not invent unsupported source facts.
- `RecallRanker`: ranks stored candidate Memories and returns the evidence used.

Each contract must expose capability availability, model/version identity, bounded
input/output limits, recoverable failure, and cancellation. Implementations live in
data/platform adapters; interfaces and validated models live inward of them.

## 5. Source and data boundaries

1. Android or an approved provider remains owner of the original Asset.
2. UNFYND reads only the minimum source content needed after explicit source access.
3. Extraction and local inference operate on-device. No source content, extraction,
   embedding, prompt, query, or Memory is sent to a remote service in the core path.
4. Room persists only normalized UNFYND-owned state: references, fingerprints,
   extraction records, Memories, evidence, embedding/index metadata, model versions,
   and recoverable work state.
5. A user can revoke source access, remove a source, clear derived data, or clear
   all UNFYND data. “Memories live forever” means durable while the user retains
   them; it never overrides user control or platform revocation.

## 6. AI Pack and model registry

The base APK must stay small. Models are not hard-coded into UI or business logic.
Before inference, an `AiPackManager` must verify an approved pack manifest containing
at least capability, model ID, version, compatible application/schema versions,
download size, storage requirement, integrity hash, license, and installation state.

- Installation/update requires clear user disclosure and affirmative action.
- Downloads are permitted only for the AI Pack itself; they do not upload user data.
- Pack verification is atomic: an incomplete, incompatible, or failed verification
  leaves the prior known-good pack active or marks the capability unavailable.
- A model update never silently changes a completed Memory. Reprocessing is queued
  explicitly according to version policy and source availability.
- A system-managed local runtime may satisfy a capability when available. It must
  have a documented compatible fallback or a truthful unavailable state; UNFYND may
  not claim a feature works on unsupported devices.

## 7. Background execution and device health

All discovery, extraction, and understanding work executes outside Compose/UI.
WorkManager invokes bounded, idempotent use cases through a persistent queue. Work
must checkpoint after every safe batch and tolerate process death, cancellation,
access revocation, low storage, and a changed source.

Default policy for non-urgent indexing:

- batch work rather than wake the device per asset;
- require battery-not-low and storage-not-low;
- prefer charging and idle time for heavier local models;
- stop promptly when Android cancels work or constraints are lost;
- observe thermal and memory pressure where platform APIs make that practical;
- show an honest paused/retryable state rather than pretending indexing completed.

The UI may trigger a small, explicit foreground batch only when its privacy and
battery impact are explained. It still calls an application use case, never a model
or source adapter directly.

## 8. Memory and versioning requirements

Every searchable Memory must record:

- stable Memory identity and immutable revision identity;
- source-neutral Asset identity and immutable fingerprint;
- extraction schema/version and evidence provenance;
- understanding schema and capability/model version(s);
- structured attributes, summary, anchors, and uncertainty/failure state;
- compatible embedding version and index state;
- timestamps required for retry, reindex, and explanation.

The model must preserve prior revision provenance and a supersession reason. A new
extractor, model, embedding, or summary cannot silently overwrite history.

The internal integrity state is one of: `AWAITING_PERMISSION`, `QUEUED`, `INDEXING`,
`READY`, `STALE_REINDEX_REQUIRED`, `SOURCE_UNAVAILABLE`, `FAILED_SAFELY`, or
`REMOVED`. State transitions are explicit, recoverable where appropriate, and must
not make incomplete or failed derived content searchable.

An Asset is not marked searchable until its Memory passes structural validation and
its supporting evidence is persisted atomically. A malformed model output is a
recoverable indexing failure, never a partial truth.

## 9. Recall and Explain Mode

The normal local query path is:

```text
User recall cue -> optional local intent/query encoding -> vector candidates ->
structured filters -> evidence-based ranking -> Memory cards -> explanation
```

The phrase “no AI inference at search” means no original-asset analysis and no
per-result generative reasoning **on the ordinary Find / recall path**. A lightweight
local query encoder or deterministic intent classifier is allowed when needed for
semantic retrieval; it must be bounded, version-compatible with stored vectors, and
work without network access.

**Narrow carve-out — Grounded Answers:** Query-time generation is authorized only for
the separately governed Grounded Answers capability, and only over an immutable
Evidence Package of stored authorized evidence, with deterministic verification,
structured status/completeness, and no original reopen during package build or
reasoning. See `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`,
`docs/GROUNDING_ARCHITECTURE.md`, and ADR-033. This carve-out does not weaken
truth-before-intelligence, offline-first, or Find’s non-generative ranking path.

The user-facing name is **Why this result?**; Explain Mode remains the internal
product/engineering term. It uses the stored Memory and its evidence to present the
available matching cues, bounded evidence, safe provenance, uncertainty, freshness,
source availability, and material limitations. It never fabricates a reason, calls a
result an unsupported “answer,” or needs to reopen the original Asset for ordinary
recall. If an original is unavailable after access revocation, existing explanations
must say so truthfully. Grounded Answers use StructuredAnswer + citations as their
trust surface; they must not invent Find Why copy.

## 10. Offline and availability contract

“Offline-first” means that after a local AI capability is installed and a source is
authorized, discovery already permitted by Android, extraction, local understanding,
stored-memory recall, ranking, and existing explanations work without network access.

Connectivity may be used only for an explicitly user-approved AI Pack download or
update, future backup/sync, or future optional cloud enhancement. A third-party note
provider remains subject to its separate source-access contract; this specification
does not bypass Android app sandboxing or ADR-003.

## 11. Performance, quality, and accessibility

The following are planning budgets, not yet release promises: a typical stored-memory
search should target under 300 ms; all model packs, memory use, and per-asset
indexing time must be measured on a documented representative device tier before a
release target is accepted. No arbitrary pack-size, RAM, battery, or latency number
may be claimed without benchmark evidence.

Every implementation must define:

- supported-device/capability matrix and honest fallback copy;
- benchmark corpus, privacy-safe fixtures, and result-quality metrics;
- battery, storage, thermal, cancellation, and offline verification;
- accessible loading, paused, failure, retry, and completed states;
- polished, recognition-first language that never exposes raw implementation jargon
  where a user needs an understandable choice.

Any capability that displays confidence must additionally define a private,
representative evaluation corpus and track recall quality, unsupported-claim rate,
false-link rate where applicable, explanation coverage, calibration, and
overconfident-error rate. Evidence classes express support/provenance, not an
automatic truth guarantee:

- **Direct:** a bounded source-derived fact, such as permitted metadata, OCR, PDF
  text, note text, or EXIF;
- **Validated observation:** a bounded local model observation with provenance;
- **Retrieval signal:** similarity or ranking information that may propose a candidate
  but cannot prove a claim;
- **Hypothesis:** a clearly tentative possible relationship that needs more evidence
  or user confirmation.

## 12. Explicit prohibitions

- No OpenAI, Anthropic, Gemini cloud, or other remote AI API in the core path.
- No API keys, provider secrets, or long-lived access tokens in the app or Git.
- No manual per-asset upload/share workflow as a substitute for source indexing.
- No unbounded source scan, repeated asset analysis on every search, or UI-thread
  model inference.
- No silent fallback from semantic understanding to filename-only search.
- No unsupported explanation, mutation, deletion, renaming, movement, or copying of
  original source content.

## 13. Acceptance gates before AI implementation

Before adding any AI/model dependency or implementation, document and approve:

1. the exact capability and source types it serves;
2. local execution and data-flow proof;
3. licensing, model delivery, update, integrity, and rollback plan;
4. supported-device and fallback behaviour;
5. model input limits, output schema, validation, retry, and cancellation plan;
6. performance/battery/storage benchmark plan;
7. unit, integration, offline, and emulator acceptance tests;
8. affected PRD and Local-AI traceability IDs.

## 14. Cross-reference discipline

Before every meaningful delivery step, the engineer must consult
`docs/PRODUCT_SOURCE_REGISTRY.md`, this specification, `docs/GOVERNANCE.md`,
`CONTINUE.md`, the product contract, architecture, decisions, roadmap, and
traceability matrix. The delivery record must name the exact requirement IDs,
current-code evidence, decision/conflict check, acceptance criteria, and verification
result. No work proceeds from conversational memory alone.

## 15. Evidence-first memory evolution (accepted future direction)

The current unit of indexing is an **Asset Memory**: one validated, searchable
semantic representation of one Asset version. It is the durable foundation and is
never deleted or overwritten merely because later correlation finds a possible
relationship.

After Asset Memory creation, a separately governed local correlation capability may
propose an evidence-backed **link**. A link is not a claim that all linked items are
one thing. It records its relationship type, supporting evidence, confidence or
uncertainty, provenance/version, and lifecycle state. A link may be rejected, expire,
or be superseded without harming its member Asset Memories.

Future memory levels are additive:

```text
Asset Memory -> evidence-backed links -> Event Memory -> Knowledge Memory
```

- **Event Memory:** a tentative or confirmed representation of an occurrence such as
  a dinner, trip, meeting, or purchase. It references member Asset Memories and
  links; it never owns, replaces, or silently merges original Assets or their
  Asset Memories.
- **Knowledge Memory:** an evolving, evidence-backed representation of a subject such
  as a project, decision, idea, person, or place. It is not a generic chatbot answer
  or an untraceable summary.

Every relationship, Event Memory, and Knowledge Memory must remain explainable from
stored evidence. It must answer, in user-facing language: what evidence supports the
link, where that evidence came from, why it was considered relevant, and how certain
UNFYND is. "Reasoning" means inspectable matching factors and citations, not invented
facts or an unsupported disclosure of model-private reasoning.

Embeddings remain a versioned retrieval aid. They may suggest candidates but cannot,
on their own, create a durable link, Event Memory, Knowledge Memory, or explanation.
The Memory Builder is the authoritative validation boundary for memory construction;
future correlation/linking must use an equally validated boundary.

Memory is evolutionary, not immutable. A new evidence-backed version can supersede a
prior derived representation when new permitted evidence, a source change, or an
approved schema/model upgrade justifies it. Prior provenance, the supersession reason,
and user control must remain recoverable. A model update alone may not silently change
the user-facing meaning of a completed Memory.

This section is an architecture target only. It does not enable event detection,
timelines, personalization, WhatsApp, audio, or any other PRD-excluded MVP feature.
Its full behavior and staged adoption are governed by
`docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md`, ADR-018, and ADR-019.
