# AI Pack Delivery and Security Plan

**Status:** Accepted for Local-AI architecture gate planning (2026-07-25).  
**Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §6, §10, §13.3.  
**Governing decisions:** ADR-012.  
**Does not authorize:** model files in the APK, network permission, pack download UI,
OCR/embedding SDKs, WorkManager AI jobs, or any claim that on-device understanding
is ready.

## Purpose

Define how Memora will deliver, verify, update, and roll back on-device AI Packs so
that Spec §6 and A-07 are testable **before** any download or inference code lands.
This plan is a gate deliverable. Implementation of downloads remains a later,
separately change-controlled slice.

## Product constraints

- Core memory creation, retrieval, ranking, and explanation stay local after a
  required capability is installed (Addendum 1 / ADR-012).
- Connectivity may be used only for an explicitly user-approved AI Pack download or
  update (Spec §10). Pack downloads must not upload user content.
- Domain and application code depend on capability interfaces and pack contracts,
  never on a named vendor SDK or HTTP client (Spec §4 / §6).
- Unsupported or unverified packs must surface a truthful unavailable state; Memora
  must not claim a capability works.

## Pack artifact model

An **AI Pack** is a versioned, installable set of model assets plus an approved
manifest. The base APK stays small; packs are not hard-coded into UI or business
logic.

### Required manifest fields (Spec §6)

| Field | Role |
|---|---|
| `packId` | Stable pack identity (not a marketing name alone) |
| `capability` | Which Spec §4 capability this pack satisfies |
| `modelId` / `modelVersion` | Versioned model identity used in Memory provenance |
| `compatibleAppVersions` | App version range that may activate the pack |
| `compatibleSchemaVersions` | Understanding / embedding schema compatibility |
| `downloadSizeBytes` | Disclosed download size before user affirmative action |
| `storageRequirementBytes` | Disclosed on-device storage need before install |
| `integrityHash` | Cryptographic hash of pack payload for atomic verification |
| `license` | SPDX or plain license string shown in disclosure |
| `installationState` | Not installed / verifying / active / failed / rolled back |

Domain contracts in `com.memora.app.domain.intelligence` encode these fields and
reject blank or non-positive values. They do not perform I/O.

## Delivery flow (future implementation contract)

```text
User sees disclosure (size, storage, license, capabilities)
  -> affirmative action
  -> download pack bytes only (no user content upload)
  -> atomic verify (hash + compatibility)
  -> activate new pack OR keep prior known-good / mark unavailable
```

Rules:

1. **Disclosure first.** Installation or update requires clear user disclosure and
   affirmative action. Silent background pack install is prohibited.
2. **Pack-only network.** When network is later approved, the only allowed payload
   direction for this feature is pack download. No telemetry of Memories, Assets,
   extracted text, or embeddings.
3. **Atomic verification.** Incomplete, incompatible, or failed verification must
   leave the prior known-good pack active, or mark the capability unavailable if no
   known-good pack exists.
4. **No silent Memory rewrite.** A model update never silently changes a completed
   Memory. Reprocessing is queued explicitly per version policy and source
   availability (Spec §6 / §8).
5. **System runtime option.** A system-managed local runtime may satisfy a capability
   when available; it still needs a documented compatible fallback or truthful
   unavailable state (compatibility/fallback policy is a separate gate deliverable).

## Integrity and rollback

| Event | Required outcome |
|---|---|
| Hash mismatch | Reject payload; retain prior known-good; capability stays available only if prior pack remains active |
| Incompatible app/schema | Do not activate; truthful unavailable or keep prior pack |
| Partial download / process death | Treat as not installed; retryable; never mark AVAILABLE |
| Verification crash | Same as failed verification |
| User clears Memora data | Pack install state cleared with derived data policy |
| Rollback after bad update | Reactivate last known-good pack; queue explicit reprocess policy if needed |

Integrity verification is a pure check against the declared `integrityHash` and
compatibility ranges. This plan does not select a specific hash algorithm in code
yet; the first download implementation must record the algorithm in the manifest
schema and in change-control before shipping.

## Security boundaries

- No API keys, provider secrets, or long-lived access tokens in the APK or Git.
- No remote inference API in the core path.
- Pack storage lives in app-private storage; packs are not world-readable.
- Manifest and install state are Memora-owned metadata, separate from original
  user source files (sources remain read-only).
- Logging must never include user content, pack private keys (none expected), or
  raw exception text that embeds source paths.

## `AiPackManager` responsibility (not implemented yet)

When implemented behind a platform adapter, `AiPackManager` must:

1. expose install/disclosure state without inventing AVAILABLE;
2. verify an approved pack manifest before any inference call;
3. map verified packs onto Spec §4 capability engines;
4. refuse work when verification fails and report unavailable reasons to callers.

Domain interfaces for capability engines already exist; pack management must bind to
those availability contracts rather than bypassing them.

## Explicit non-goals for this gate document

- Choosing a commercial model vendor or committing a model binary
- Adding `INTERNET` permission or download WorkManager
- Building install UI copy beyond the disclosure requirements above
- Defining the supported-device matrix (separate compatibility/fallback policy)
- Setting latency/battery/storage release numbers (separate benchmark plan)

## Acceptance for this plan slice

1. This document is recorded and linked from ROADMAP / CONTINUE / CHANGELOG.
2. Domain manifest and verification types exist with unit tests that reject
   incomplete packs and never invent an ACTIVE install from blank data.
3. No Gradle AI/OCR/network dependency and no pack download code land in the same
   change.
4. Traceability notes A-07 plan progress without marking install/offline AI ready.

## Follow-ups required for Local-AI gate exit

1. Compatibility / fallback policy (supported-device matrix + honest copy).
2. Local-AI benchmark plan (privacy-safe fixtures + quality/perf metrics).
3. Separately approved implementation slices for disclosure UI, download, verify,
   and capability engine adapters.
