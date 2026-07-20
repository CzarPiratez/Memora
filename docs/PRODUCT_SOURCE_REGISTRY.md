# Product Source Registry

## Purpose

This registry preserves the exact approved product-source documents that govern
Memora. The copies in `docs/product-source/` are immutable reference artifacts.
They are version-controlled so delivery never relies on conversational memory or
an untracked desktop file.

## Authority and interpretation

1. `Memora.docx` remains the product baseline: what Memora is, its MVP sources,
   and its user promise.
2. `Addendum 1.docx` is the accepted Local AI & Offline-First implementation
   amendment. It supersedes the original PRD only where an implementation detail
   conflicts with its local-first rule.
3. `Addendum 2 Engineering Reference.docx` is the approved blueprint for
   `LOCAL_AI_TECHNICAL_SPEC.md`. The Markdown specification is the testable,
   implementation-facing interpretation; it must not weaken Addendum 1.
4. When an ambiguity remains, stop, record an ADR, and ask for a product decision.

## Immutable source copies

| Source document | Repository copy | SHA-256 |
|---|---|---|
| Original Memora PRD | `docs/product-source/Memora.docx` | `ECD5104E01FBC3066299AFDF83F2CDD7BA7A565725C3277EBB29B66AFDD9E195` |
| Local AI & Offline-First Revision | `docs/product-source/Addendum 1.docx` | `CEE42CAFE99595F2DED89BA5DA97529CC9A04F048714F55D7896A39577EAC89D` |
| Engineering Reference | `docs/product-source/Addendum 2 Engineering Reference.docx` | `F3D974F9DF654ED1242B1FD7A6BED0032C1CA960CB36280AB2A88F962D1CDD19` |

If a source document changes, preserve the previous copy, add the new version with
its SHA-256, and record the effect in `docs/DECISIONS.md` before implementation.
