# Change control: Class A pack revision 2026-09-07

**Date:** 2026-09-07  
**Type:** Class A public pack (contracts, validator, hygiene)  
**Closes:** PROGRAM_STATE Batch G / G1–G7 (G7 as sketch)  
**Does not authorize:** Class B, `:core-domain`, App source, AVAILABLE,
Grounded Answers runtime, a frozen public SDK

## Pre-work record

- **Requirement IDs:** PROGRAM_STATE §7 C-1…C-6 and Batch G; ROADMAP-OPEN
  publish mechanics; ADR-047 Class A boundary.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  PUBLISH_CLASS_A_PACK, CORE_APP_SEPARATION_PLAN Phase 4 gates,
  CORE_CAPABILITY_REGISTER (public seams sanitized, register stays private),
  public pack README / SPEC / APPLICATIONS / validator / schema.
- **Current-code evidence inspected:** Public HEAD `4d99a42` matched monorepo
  `16e8270`. Pack files unchanged since 2026-09-05. Validator re-implemented
  enums in Python and never loaded the schema. Public repo had zero workflows.
- **Open ADRs / platform:** no new ADR. Class A only.
- **Privacy:** synthetic examples only. No App fixtures, secrets, or freeze
  blobs. `CITATIONS.md` kept as a maintainer map without new private paths.
- **Smallest safe change:** honesty copy; schema-driven stdlib validator;
  four negative fixtures; public CI workflow in the pack; OSS hygiene;
  Canonical Recall **sketch**.
- **Acceptance criteria:**
  - [x] README / SPEC / APPLICATIONS no longer say this pack *provides* the
        runtime
  - [x] Validator loads `x-unfyndSchemaVersion` and schema enums
  - [x] `--self-test` includes the four new rejects
  - [x] `.github/workflows/validate.yml` in the pack
  - [x] `CANONICAL_RECALL.md` is a sketch, not a frozen API
- **Holistic scenarios:**
  - External reader quotes README in a grant deck → must see "specifies /
    App implements / this repo is not the runtime"
  - Contributor clones public repo only → `python tools/... --self-test` and
    Actions both work
  - Invalid export example is a reject, not an accepted valid bundle
- **Alternatives considered:**
  - Publish App Find internals / I2 workers — rejected; Class A boundary
  - Full JSON Schema engine via pip — rejected; BUILDING is stdlib-only
  - Remove `CITATIONS.md` — rejected; keep as maintainer map (G6)
  - Freeze Canonical Recall as semver — rejected until Phase 3

## Architectural convergence

N/A — not a Find/Recall App change. Public sketch only.

## Delivery record

- **Files/layers:** `public/unfynd-core/**` only.
- **Automated verification:** `python public/unfynd-core/tools/validate_class_a_examples.py --self-test`
- **Public publish:** pack-only copy to `CzarPiratez/unfynd-core`; tag `v0.1.0`.
- **Known limitation:** website revision date on unfynd.com/core is still
  optional. Class B / `:core-domain` remain closed.
