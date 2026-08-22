# Product Source Registry

## Purpose

This registry preserves the exact approved product-source documents that govern
UNFYND (formerly Memora). The copies in `docs/product-source/` are immutable
reference artifacts. They are version-controlled so delivery never relies on
conversational memory or an untracked desktop file.

## Authority and interpretation

1. **Current product identity (ADR-040):** the product/brand name is **UNFYND**.
   Personal Knowledge Infrastructure is the north star (already recorded in
   `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` and ADR-018). The Android app is one
   milestone/reference implementation, not the whole system. Search/retrieval is
   one capability, not the product definition. Operating procedure:
   `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md`.
2. `Memora.docx` remains the **historical** product baseline: MVP sources and
   user promise as originally written. Identity is superseded by ADR-040, not by
   editing the `.docx`. Living canon may say UNFYND after later overlay steps.
3. `Addendum 1.docx` is the accepted Local AI & Offline-First implementation
   amendment. It supersedes the original PRD only where an implementation detail
   conflicts with its local-first rule.
4. `Addendum 2 Engineering Reference.docx` is the approved blueprint for
   `LOCAL_AI_TECHNICAL_SPEC.md`. The Markdown specification is the testable,
   implementation-facing interpretation; it must not weaken Addendum 1.
5. When an ambiguity remains, stop, record an ADR, and ask for a product decision.

## Governed internal amendments

The following repository-owned amendments are accepted product-direction decisions.
They clarify Memora's long-term architecture without rewriting the immutable source
documents or expanding the current MVP source scope unless an amendment explicitly
says so:

| Amendment | Authority | Scope |
|---|---|---|
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | User-approved product direction; ADR-018 and ADR-019 | Evidence-first Asset Memories are the MVP foundation. Event, Knowledge, and relationship memories are a future, staged evolution governed by truth before intelligence. |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | User-approved product direction; ADR-033–ADR-039 | Grounded Answers over retrieved stored evidence (or abstain). No chatbot. Find remains source of truth. Canonical engineering map: `docs/GROUNDING_ARCHITECTURE.md`. First slice: PDF saved text (`docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`). Generative implementation blocked on readiness gates. |

An internal amendment may not silently enable a source, permission, cloud path, or
feature excluded by the immutable PRD. Such a change still requires an explicit
product decision and a recorded ADR.

Product-noun overlay of living constitutions is playbook Step 3; ADR-040 already
binds identity.

## Governed architecture artifacts

SHA-256 values are of the Git blob as committed (playbook Step 2). Change-control
and changelog files are not product source and are not hashed here.

| Artifact | Classification | SHA-256 |
|---|---|---|
| `docs/LOCAL_AI_TECHNICAL_SPEC.md` | Canon (engineering constitution) | `7EF9F38F0C33708A7E1E4CC7E21A2D062F037F3471CC4C68A782BB1CEC621DDD` |
| `docs/GROUNDING_ARCHITECTURE.md` | Canon (engineering constitution; sole Grounded Answers constitution) | `5D667ADAA3505044FC541AEDA38AE10262B832D1023ECA228406A62DC994EDB0` |
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | Amendment (product-direction) | `ED25CDE8FA1DBE2B7EFC422276C8D1A0E5F7A8B9A5339062A6AF2F701796E06A` |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | Amendment (product-direction) | `0B1BA90F8E8943991C2B6640CB67150763CEF51BFF557CFE785A6E3003A1C75A` |
| `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` | Acceptance / slice spec | `E39CAC711ACA0C571E24A4C91777B879065DECE565DAF4DAC592F74A8DDD4E2F` |

## Immutable source copies

| Source document | Repository copy | SHA-256 |
|---|---|---|
| Original Memora PRD | `docs/product-source/Memora.docx` | `ECD5104E01FBC3066299AFDF83F2CDD7BA7A565725C3277EBB29B66AFDD9E195` |
| Local AI & Offline-First Revision | `docs/product-source/Addendum 1.docx` | `CEE42CAFE99595F2DED89BA5DA97529CC9A04F048714F55D7896A39577EAC89D` |
| Engineering Reference | `docs/product-source/Addendum 2 Engineering Reference.docx` | `F3D974F9DF654ED1242B1FD7A6BED0032C1CA960CB36280AB2A88F962D1CDD19` |

If a source document changes, preserve the previous copy, add the new version with
its SHA-256, and record the effect in `docs/DECISIONS.md` before implementation.
