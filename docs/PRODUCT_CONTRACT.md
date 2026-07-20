# Product Contract

## Product definition

Memora is a personal memory retrieval engine. It helps users find what they remember
about an item, not merely what they remember about its filename or folder.

The PRD's required lifecycle is:

`Discover -> Extract -> Understand -> Store -> Recall -> Explain`

Every discovered item becomes an Asset. Every indexed Asset becomes a Memory with
durable semantic attributes that can be searched later without repeatedly re-reading
the original source.

## MVP asset types

| Asset type | Required outcome | Primary access model |
|---|---|---|
| Photos | Automatically discover and index permitted device images | Android MediaStore |
| Screenshots | Automatically discover and distinguish from other images | Android MediaStore + deterministic heuristics |
| PDFs | Automatically rescan user-approved document locations | Persisted Storage Access Framework folder access |
| Notes | Automatically index supported existing note sources | Explicit provider adapter; source-specific approval |

The user does not manually upload or share each item to make it searchable. A one-time
permission or source connection may be necessary; thereafter the discovery service is
responsible for incremental indexing.

## Deterministic extraction before AI

- Images: URI, timestamps, dimensions, EXIF/GPS when available, and OCR text.
- PDFs: URI, title, page count, metadata, and extractable full text.
- Notes: provider identity, stable note ID, timestamps, title, raw text, and allowed
  metadata.
- Screenshots: image metadata and OCR text.

## Retrieval contract

- Natural-language queries use remembered cues such as person, place, object, time,
  purpose, and topic.
- Results are ranked from stored semantic evidence, not filename matching alone.
- Explain Mode states the evidence that made a result match.
- Search never fabricates facts that are absent from the memory record.

## Privacy contract

- All original source content is read-only.
- The app asks for source access before discovery.
- Indexing state must be visible and recoverable.
- Source access must be revocable.
- Core memory creation, retrieval, ranking, and explanation execute locally after the
  required on-device capability is installed. They do not depend on cloud AI.
- The app explains the source scope, local processing, model-pack storage, and any
  optional network action before the user enables it.
- Cloud AI, backup, and sync are future optional enhancements only; they need their
  own explicit product decision, consent, and data-handling contract.

## MVP exclusions from the PRD

WhatsApp, Gmail, Calendar, videos, audio, experience detection, timeline, cloud sync,
user accounts, collaboration, manual tags, folders, saved searches, and phone-wide
chat are out of scope unless the user explicitly changes the product contract.

## Platform constraint requiring a decision

The PRD includes automatic indexing of notes while excluding user accounts and cloud
sync. Android prevents one application from reading arbitrary private data held by
another note application. We therefore need a narrow, approved note-source connector
or a revised MVP definition. This is tracked in ADR-003; it must not be hidden by a
per-note import workflow.
