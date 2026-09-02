# Change control — Class A conformance validator

**Date:** 2026-09-01  
**Type:** Class A pack — runnable tooling (public `unfynd-core/`)  
**Status:** Delivered in private monorepo; public remote publish out of band  
**Decision guardrails:** Public pack only under `public/unfynd-core/`. No App
source, no secrets, no grant or third-party program language. Does not authorize
Class B, full Core source, MCP server, or marketing AVAILABLE. Does not edit
hashed Product Contract / Spec / Amendment / Freeze blobs in private `docs/`.

## Pre-work record

- **Requirement IDs:** `public/unfynd-core/ROADMAP-OPEN.md` — “Runnable tooling
  that exercises Class A contracts”; `docs/POST_MVP_PROGRAM_V1.md` §6 Step 1.
- **Source documents read:** `public/unfynd-core/SPEC.md` §3 evidence classes,
  `public/unfynd-core/examples/README.md`, ADR-048 (synthetic examples),
  `docs/POST_MVP_PROGRAM_V1.md`, `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.
- **Current-code evidence inspected:** Existing three synthetic JSON examples;
  no prior validator or CI job for Class A.
- **Open ADRs / platform limitations checked:** ADR-047 Class A boundary;
  publishing to public GitHub remains out of band per ROADMAP-OPEN.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Validator processes synthetic public examples only. Stdlib Python; no network.
- **Smallest safe change:** JSON Schema sketch, Python validator CLI,
  `BUILDING.md`, two additional synthetic examples, CI job, planning sketches
  `EXPORT_CONTRACT.md` + `INTEGRATION.md`.
- **Acceptance criteria:**
  - [x] `python tools/validate_class_a_examples.py --self-test` passes on all
        `examples/*.json`.
  - [x] `valid-*.json` pass; `invalid-*.json` fail valid Asset Memory rules.
  - [x] CI job `class-a-validator` added to `.github/workflows/ci.yml`.
  - [x] `BUILDING.md` documents quick start (3 commands or fewer).
  - [x] No grant or third-party program references in public pack.
- **Test and emulator verification plan:** Run validator locally and in CI;
  `--self-test` is the conformance suite for examples.
- **User-visible quality/accessibility review plan:** N/A — developer tooling only.

## Architectural convergence

N/A — not a Find/Recall change.

## Delivery record

- **Files/layers changed:** `public/unfynd-core/schema/`,
  `public/unfynd-core/tools/`, `public/unfynd-core/BUILDING.md`,
  `EXPORT_CONTRACT.md`, `INTEGRATION.md`, new examples, README/ROADMAP-OPEN/
  examples README updates, `.github/workflows/ci.yml`, `PUBLIC_CHANGELOG.md`,
  `docs/CHANGELOG.md`, `CONTINUE.md`.
- **Automated verification and result:** `python tools/validate_class_a_examples.py --self-test` — **PASS** (5 examples); full examples run **PASS**.
- **Emulator/manual verification and result:** N/A.
- **Failure/recovery paths verified:** Invalid examples must fail; malformed JSON
  reports decode error.
- **Known limitation or follow-up:** Schema is illustrative sketch lock, not Room
  API. Export/import runtime waits for P6 change control in private monorepo.
- **Documentation/traceability/ADR updates:** `PUBLIC_CHANGELOG.md`; private
  `docs/CHANGELOG.md`; `POST_MVP_PROGRAM` §6 Step 1 satisfied for validator slice.
- **Git commit:** Pending user request.
