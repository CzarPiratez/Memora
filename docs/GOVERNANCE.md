# Delivery Governance

## Purpose

This file is the mandatory operating contract for all work on Memora. It applies to
product decisions, design, code, dependencies, tests, data handling, documentation,
and release preparation.

## Governing order

When guidance conflicts, use this order:

1. The user's latest explicit instruction.
2. The approved product requirements in `Memora.docx`.
3. Accepted decisions in `docs/DECISIONS.md`.
4. `docs/PRODUCT_CONTRACT.md`, `docs/ARCHITECTURE.md`, and
   `docs/PRD_TRACEABILITY.md`.
5. `docs/ROADMAP.md` and `CONTINUE.md`.
6. Engineering implementation details.

Nothing lower in this list may silently override anything above it.

## Mandatory pre-work gate

Before every implementation, product, UX, dependency, data-access, or release step:

1. Read `AGENTS.md`, this file, `CONTINUE.md`, the product contract, architecture,
   decisions, roadmap, and traceability matrix.
2. Identify the exact PRD requirement(s) being served.
3. Check for open ADRs, privacy implications, platform limitations, and dependency
   impacts.
4. Define the smallest testable change and its explicit acceptance criteria.
5. If a requirement conflicts with the platform or another requirement, stop and
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

No implementation step may proceed merely because it seems plausible. It must be
traceable, reviewable, and reversible.
