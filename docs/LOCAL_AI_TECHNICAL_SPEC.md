# Memora Local AI Technical Specification

**Version:** 1.0  
**Status:** Accepted engineering authority  
**Companion to:** `docs/product-source/Memora.docx`  
**Source basis:** `docs/product-source/Addendum 1.docx` and
`docs/product-source/Addendum 2 Engineering Reference.docx`

## 1. Binding principle

Memora remembers on the user's phone, not in the cloud.

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
2. Memora reads only the minimum source content needed after explicit source access.
3. Extraction and local inference operate on-device. No source content, extraction,
   embedding, prompt, query, or Memory is sent to a remote service in the core path.
4. Room persists only normalized Memora-owned state: references, fingerprints,
   extraction records, Memories, evidence, embedding/index metadata, model versions,
   and recoverable work state.
5. A user can revoke source access, remove a source, clear derived data, or clear
   all Memora data. “Memories live forever” means durable while the user retains
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
  have a documented compatible fallback or a truthful unavailable state; Memora may
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

- source-neutral Asset identity and immutable fingerprint;
- extraction schema/version and evidence provenance;
- understanding schema and capability/model version(s);
- structured attributes, summary, anchors, and uncertainty/failure state;
- compatible embedding version and index state;
- timestamps required for retry, reindex, and explanation.

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
per-result generative reasoning. A lightweight local query encoder or deterministic
intent classifier is allowed when needed for semantic retrieval; it must be bounded,
version-compatible with stored vectors, and work without network access.

Explain Mode uses the stored Memory and its evidence. It never fabricates a reason,
and it never needs to reopen the original Asset for ordinary recall. If an original
is unavailable after access revocation, existing explanations must say so truthfully.

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
