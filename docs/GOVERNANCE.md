# Delivery Governance

**Product identity:** UNFYND (formerly Memora). Direction: Personal Knowledge Infrastructure. This document’s freeze is the technical invariants below. Naming is not an architectural invariant (ADR-040).

## Purpose

This file is the mandatory operating contract for all work on UNFYND. It applies to
product decisions, design, code, dependencies, tests, data handling, documentation,
and release preparation.

## Governing order

When guidance conflicts, use this order (ADR-042; aligned with Freeze §2):

1. The user's latest explicit instruction.
2. The immutable product baseline and accepted amendments registered in
   `docs/PRODUCT_SOURCE_REGISTRY.md`.
3. `docs/PRODUCT_CONTRACT.md` — foundational product and privacy commitments.
   Nothing below may contradict it.
4. `docs/LOCAL_AI_TECHNICAL_SPEC.md` — accepted local-AI engineering authority.
   The spec wins only where an implementation detail would otherwise require
   cloud AI or repeated original-asset analysis at recall. Subordinate to the
   Product Contract.
5. `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` — staged Asset → Link → Event →
   Knowledge model, evidence classes, and the Explain/Trust contract.
   Subordinate to the Product Contract and Local AI Technical Spec; where it
   defines behavior not otherwise specified by either, it is authoritative for
   that core.
6. `docs/ARCHITECTURE_FREEZE_v1.0.md` — freeze and change-control declaration
   for the Asset-Memory / PKI core named in Freeze §2 items 1–3. Freeze §1
   exclusivity is that core only (ADR-042 option (a)); it does not unseat
   Grounded Answers constitutions.
7. `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` — sequencing and
   implementation-planning authority only. It does not define architecture.
   Where it appears to imply a rule not stated in items 3–5, those documents
   govern.
8. `docs/GROUNDING_ARCHITECTURE.md` and `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`
   — sole Grounded Answers constitution and product-direction amendment.
   They govern Grounded Answers only.
9. Other accepted decisions in `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`,
   `docs/PRD_TRACEABILITY.md`, `docs/ROADMAP.md`, and `CONTINUE.md`.
10. Engineering implementation details.

Nothing lower in this list may silently override anything above it.

MIG-05 step 3 (Find-by-meaning SEARCH cutover onto
`MemoryEvidenceEmbeddingStore` + `MemoryEvidence`; readiness counts exclude
`PdfPageEmbeddingStore`; cutover STALE for gap revisions only) is authorized
and in this delivery; do not claim full MIG-05 complete until step 4
(`PdfPageEmbedding*` retirement) and remaining Migration Spec acceptance.
Do not start MIG-06–MIG-11 or MIG-07B. `docs/PHASE_A_IMPLEMENTATION_PLAN_V1.md`
is not architectural authority and is not permission to implement later MIGs.
ADR-043 confirms the freeze is suitable for the PKI north star through
See/Remember, staged Connect, retrieve-by-meaning, and Understand /
converse-as-Q&A; Act remains out of current architecture. That confirmation
does not authorize later MIG-* beyond step 3 or reopen the Architecture Freeze.

## Mandatory pre-work gate

Before every implementation, product, UX, dependency, data-access, or release step:

1. Read `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY.md`, `LOCAL_AI_TECHNICAL_SPEC.md`,
   this file, `CONTINUE.md`, the product contract, architecture, decisions, roadmap,
   and traceability matrix.
2. Inspect the current code and tests that own the proposed boundary; do not rely on
   a previous conversation or a remembered repository state.
3. Identify the exact PRD and Local-AI requirement(s) being served.
4. Check for open ADRs, privacy implications, platform limitations, device support,
   offline behaviour, data retention, dependency, and performance impacts.
5. Define the smallest testable change and its explicit acceptance criteria.
6. If a requirement conflicts with the platform or another requirement, stop and
   record the conflict. Do not conceal it with a shortcut or unapproved assumption.
7. Find / Recall / search-embedding / ranking / Why changes must fill the
   **Architectural convergence** block in `docs/CHANGE_CONTROL_TEMPLATE.md`.
8. Consult `docs/LEGACY_RECALL_SURFACE.md` before any product Find change; do
   not extend a Live/Dual row without `docs/LEGACY_EXTENSION_EXCEPTION.md` and a
   mandatory sunset (defect fixes that preserve the contract are OK).
9. Cursor rule `.cursor/rules/unfynd-architecture-invariants.mdc` applies to
   agents and reviewers for Canonical Recall / Memory substrate boundaries.

## Enterprise-quality bar

Every delivered artifact must be deliberately designed, reliable, secure by default,
maintainable, tested in proportion to risk, accessible where applicable, and polished
enough to be credible in a production review.

"It compiles" is not a completion criterion. Completion requires:

- correct behavior against the relevant PRD traceability IDs;
- clear errors, recovery behavior, and state handling;
- no silent data loss, duplicate indexing, privacy overreach, or unbounded work;
- readable code with clear ownership boundaries;
- automated tests for business/domain logic and critical error paths;
- emulator verification for Android-facing behavior;
- documentation and decision records updated in the same change;
- a clean local Git checkpoint after verification.

For user-facing work, completion also requires a recognition-first, accessible,
polished experience with plain-language privacy, progress, error, and recovery copy.
“Wow factor” must come from trustworthy speed, clarity, and calm control—not from
misleading animation, hidden work, or unsupported claims.

## Scope and honesty

- Do not promise a capability until the platform and implementation support it.
- Do not trade away security, privacy, performance, accessibility, or data integrity
  merely to make a demo appear complete.
- Do not add third-party services, SDKs, permissions, or network access without
  documenting the need, alternatives, privacy impact, and verification plan.
- Treat every source as read-only unless the user explicitly approves otherwise.

## Change-control rule

For each meaningful change, record:

- requirement IDs from `docs/PRD_TRACEABILITY.md`;
- affected architecture layer(s);
- tests performed and result;
- known limitation or follow-up work;
- Git commit after the change is verified.

Use `docs/CHANGE_CONTROL_TEMPLATE.md` as the record format. A missing record is a
failed pre-work gate, not permission to proceed.

No implementation step may proceed merely because it seems plausible. It must be
traceable, reviewable, and reversible.
