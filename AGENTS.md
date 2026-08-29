# UNFYND Engineering Guide

**Product identity:** **UNFYND App** (the product people use) and **UNFYND Core**
(on-device memory and intelligence infrastructure). Formerly Memora. The Android
project folder path `MemoraApp/` is a deferred technical ID (ADR-040), not the
product name. This document’s freeze is the technical invariants below. Naming is
not an architectural invariant (ADR-040 / ADR-045 / ADR-046).

## Product authority

The immutable product baseline and accepted amendments are registered in
`docs/PRODUCT_SOURCE_REGISTRY.md`. `Memora.docx` defines the product vision;
`LOCAL_AI_TECHNICAL_SPEC.md` defines the binding local-first implementation rules
where an AI architecture detail would otherwise conflict. UNFYND is a **memory
retrieval engine**, not a file browser, upload tool, or generic chatbot. It must help
a person recall content by meaning and explain why a result matched.

Before every step, read `docs/PRODUCT_SOURCE_REGISTRY.md`,
`docs/LOCAL_AI_TECHNICAL_SPEC.md`, `docs/GOVERNANCE.md`, `CONTINUE.md`, and the
applicable files in `docs/`. The mandatory pre-work gate in `docs/GOVERNANCE.md` is
not optional. Work from those sources and current-code inspection, never from
conversational memory alone. Update the relevant document whenever a requirement,
decision, architecture boundary, or delivery checkpoint changes.

## Non-negotiable product behavior

- The durable flow is: discover -> extract -> understand -> store -> recall -> explain.
- A source is indexed; users do not manually feed individual assets into UNFYND as
  the primary workflow.
- Photos, screenshots, PDFs, and notes are all MVP asset types.
- A `Memory` is the searchable semantic representation of an `Asset`; it is not a
  copy of, replacement for, or owner of the original file.
- Search must work from natural-language recall cues and return evidence-backed
  explanations. Filename-only search is insufficient.
- Original user data is read-only. Never alter or delete source content.
- Do not silently reduce scope when Android or an external provider imposes a
  limitation. Record the gap in `docs/DECISIONS.md` and present the choice clearly.

## Android reality and privacy

- Android apps cannot read another app's private data merely because a user grants
  storage or media permission. Third-party note sources need a documented provider
  integration or another explicit, user-approved access route.
- Do not treat Android sharing as the primary indexing model. It may be a future
  fallback, but it does not satisfy the product's automatic discovery requirement.
- Request the smallest permission at the moment it is needed. Explain what will be
  indexed, where it remains stored, and what will leave the device before asking.
- No AI key, provider secret, or long-lived access token may be committed to the
  repository or embedded in the APK.
- Core memory creation, retrieval, ranking, and explanation are local after the
  required on-device capability is installed. Cloud AI is optional and never a core
  dependency.

## Architecture boundaries

Keep dependencies flowing inward:

`UI -> application/use cases -> domain -> data/platform adapters`

- **Platform adapters** talk to MediaStore, Storage Access Framework, WorkManager,
  Android permissions, and later approved note providers.
- **Discovery** returns source-neutral Asset records.
- **Extraction** obtains deterministic source facts first: metadata, OCR, PDF text,
  or note text.
- **Understanding** creates structured semantic memory data. It must be isolated
  behind an interface so it can be tested, cached, replaced, and safely retried.
- **Repository** persists only normalized domain data and indexing state; Android
  owns the original content.
- **Recall** ranks stored evidence, then Explain Mode cites the actual matching
  evidence. It must not invent explanations.

## Implementation standards

- Use Kotlin, Jetpack Compose, unidirectional UI state, Room, WorkManager, and Hilt
  as laid out in the PRD. Add a dependency only when its purpose and test plan are
  documented.
- Prefer small, focused commits and isolated implementation steps. Do not combine a
  source adapter, AI integration, database migration, and UI redesign in one change.
- Add unit tests for pure domain logic and integration tests for repository/database
  behavior before declaring a data feature complete.
- Keep composables presentation-focused; no file access, database calls, or AI calls
  directly from composables.
- Compile and run on the Android emulator after each meaningful change. Report any
  unverified area plainly.
- Meet the enterprise-quality bar in `docs/GOVERNANCE.md`; compiling code alone is
  never sufficient to call a feature complete.

## Collaboration rule

The user is learning Android development. Work one verified step at a time: explain
the outcome in plain language, make the smallest safe change, ask the user to test,
and wait for feedback before moving on. After each verified checkpoint, create a
local git commit so work is not lost; do not push unless asked. See
`.cursor/rules/git-checkpoint-commits.mdc`. Do not make product decisions that
materially change the PRD without flagging them in `docs/DECISIONS.md`.

Before Find / Recall / embedding-search work: open
`docs/RECALL_ENFORCEMENT_INDEX.md` (60s) + `docs/LEGACY_RECALL_SURFACE.md` Live/Dual
**N** (may only shrink, or ADR to grow).
