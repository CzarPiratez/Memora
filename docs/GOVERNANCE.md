# Delivery Governance

**Product identity:** UNFYND (formerly Memora). Direction: Personal Knowledge Infrastructure. This document’s freeze is the technical invariants below. Naming is not an architectural invariant (ADR-040).

## Purpose

This file is the mandatory operating contract for all work on UNFYND. It applies to
product decisions, design, code, dependencies, tests, data handling, documentation,
and release preparation.

## Governing order

When guidance conflicts, use this order:

1. The user's latest explicit instruction.
2. The immutable product baseline and accepted amendments registered in
   `docs/PRODUCT_SOURCE_REGISTRY.md`.
3. `docs/LOCAL_AI_TECHNICAL_SPEC.md` for local-AI implementation details.
4. Accepted decisions in `docs/DECISIONS.md`.
5. `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, and
   `docs/PRD_TRACEABILITY.md`.
6. `docs/ROADMAP.md` and `CONTINUE.md`.
7. Engineering implementation details.

Nothing lower in this list may silently override anything above it.

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
