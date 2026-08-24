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
They clarify UNFYND's long-term architecture without rewriting the immutable source
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
binds identity. User-visible Android copy is Step 4. Presentation/entry identifiers
are Step 5. Technical IDs (`applicationId`, `memora.db`, MSAL host, GitHub) remain
deferred.

`docs/ARCHITECTURE_FREEZE_v1.0.md` and `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md`
are registered as supplied (ADR-041). The freeze is the change-control declaration
for the architecture it names. The migration spec is sequencing authority only.
Open authority conflicts recorded in ADR-041 are **not** resolved by this
registration. `docs/GROUNDING_ARCHITECTURE.md` remains listed as the sole Grounded
Answers constitution until a human decision says otherwise.

## Governed architecture artifacts

SHA-256 values are of the Git blob content (playbook Step 3 overlay for the files
whose identity bytes changed; PDF slice hash unchanged because that file was not
overlayed). Change-control and changelog files are not product source and are not
hashed here.

| Artifact | Classification | SHA-256 |
|---|---|---|
| `docs/ARCHITECTURE_FREEZE_v1.0.md` | Canon (architecture freeze declaration) | `5CB1D4D4FA4BC9762A58E13839EBF479ECDECAE03CEE793D76D887844F801758` |
| `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` | Canon (implementation sequencing; Freeze §2 item 4) | `D10AD7C3742529148EB6A14B2DCF106B889CCAA81F0A1B483835800A11EF526E` |
| `docs/LOCAL_AI_TECHNICAL_SPEC.md` | Canon (engineering constitution) | `0338C2EC7397FF0B71A183890CEE71394A837F94F7436661E86D506CB649BBB8` |
| `docs/GROUNDING_ARCHITECTURE.md` | Canon (engineering constitution; sole Grounded Answers constitution) | `DF9A53D7116EC1B464A61AB8C0FBFC4B5C7CD28A9D5EB63193F9ABB1C397CEAA` |
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | Amendment (product-direction) | `CAFA233D03C162A26C95C83B4C0279F6D813042C887CD29782C2C8004461FBB3` |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | Amendment (product-direction) | `3C6D753B26E4027D6F886F863FA3B626CC15B31B45B81FC6985E305244EFC1B9` |
| `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` | Acceptance / slice spec | `E39CAC711ACA0C571E24A4C91777B879065DECE565DAF4DAC592F74A8DDD4E2F` |

## Immutable source copies

| Source document | Repository copy | SHA-256 |
|---|---|---|
| Original Memora PRD | `docs/product-source/Memora.docx` | `ECD5104E01FBC3066299AFDF83F2CDD7BA7A565725C3277EBB29B66AFDD9E195` |
| Local AI & Offline-First Revision | `docs/product-source/Addendum 1.docx` | `CEE42CAFE99595F2DED89BA5DA97529CC9A04F048714F55D7896A39577EAC89D` |
| Engineering Reference | `docs/product-source/Addendum 2 Engineering Reference.docx` | `F3D974F9DF654ED1242B1FD7A6BED0032C1CA960CB36280AB2A88F962D1CDD19` |

If a source document changes, preserve the previous copy, add the new version with
its SHA-256, and record the effect in `docs/DECISIONS.md` before implementation.
