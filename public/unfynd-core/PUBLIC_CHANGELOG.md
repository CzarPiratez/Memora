# Public pack changelog

All notable changes to this **Class A** Public Specification / Contract pack.
The pack is documentation, synthetic examples, and conformance tooling
(Apache-2.0).

## 2026-09-01 - Class A conformance validator + export/integration sketches

- Added runnable **example validator** (`tools/validate_class_a_examples.py`),
  JSON Schema sketch (`schema/memory-evidence-sketch.schema.json`), and
  [`BUILDING.md`](BUILDING.md) quick start.
- Added synthetic examples: audio timecode locator, evidence package export sketch.
- Added planning documents [`EXPORT_CONTRACT.md`](EXPORT_CONTRACT.md) and
  [`INTEGRATION.md`](INTEGRATION.md).
- Private monorepo CI runs validator via `class-a-validator` job (publish this
  pack to the public remote separately per `ROADMAP-OPEN.md`).

## 2026-08-30 - App/Core public framing: intelligence vision, no em dashes

- Root monorepo README rewritten as UNFYND App intelligence vision (pillars,
  multimodal map, evolution to action, progress without MVP gap lists).
- Class A README: removed Search/Find definition line; em dashes removed across
  public pack text. SPEC Search/Find line removed.
- Pack still vision-first with APPLICATIONS industry map.

## 2026-08-30 - Vision alignment with unfynd.com/core

- README and SPEC rewritten: vision first (infrastructure you ship, audit, and
  extend; Core underneath / you build the product); Core is not a personal AI or
  assistant.
- Added [`APPLICATIONS.md`](APPLICATIONS.md): how Core works (primitives) and
  industry application classes aligned with https://www.unfynd.com/core.
- Removed App MVP status table (`SPEC-STATUS.md`) from the public pack - App
  progress stays in the private monorepo; open pack showcases Core capability
  and contracts.
- ROADMAP-OPEN, examples README, GOVERNANCE, CONTRIBUTING: phased openness
  language; pack scope without deficit App checklists.
- Contracts (evidence classes, seams, local-first) retained; SPEC §1 no longer
  defines Core as everyday personal recall alone.

## 2026-08-30 - Status alignment after Memory-evidence Find convergence + midrange measurement (contracts unchanged)

- Historical: App status column refresh and AVAILABLE honesty (superseded for
  public pack structure by vision-alignment entry above).
- SPEC EmbeddingEngine wording clarified Memory and evidence / query vectors as
  retrieval signals.

## 2026-08-29 - Enrichment (status, examples, open-foundation hygiene)

- Added status table (later removed from public pack in vision alignment),
  synthetic `examples/`, CONTRIBUTING / GOVERNANCE, README revision stamp.
- SECURITY issues URL remains https://github.com/CzarPiratez/unfynd-core/issues

## 2026-08-29 - Initial Class A pack

- Initial Apache-2.0 pack: `LICENSE`, `NOTICE`, `README.md`, `SPEC.md`,
  `ROADMAP-OPEN.md`, `SECURITY.md`, `CITATIONS.md`.
- Contracts distillation only; UNFYND App and proprietary assets stay private.
