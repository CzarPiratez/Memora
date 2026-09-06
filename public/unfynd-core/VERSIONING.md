# Versioning (Class A pack)

**License:** Apache License 2.0

This repository is a **documentation and conformance pack**, not a runtime
library. Versioning is honest about that.

## Pack revision

- Human-facing stamp: the **Pack revision** date on `README.md`
  (currently 2026-09-07).
- History: [`PUBLIC_CHANGELOG.md`](PUBLIC_CHANGELOG.md).
- Git tag on this public remote: `vMAJOR.MINOR.PATCH` (first tag: `v0.1.0`).

`0.y.z` means the contracts are inspectable and the validator is runnable;
they are not a frozen OEM API.

## Schema version

`schema/memory-evidence-sketch.schema.json` declares `x-unfyndSchemaVersion`
(currently `1.1.0`). The validator **loads that file** and refuses to invent
`sketchKind`, `evidenceClass`, or locator enums.

- **Patch:** examples, copy, CI, hygiene.
- **Minor:** new optional sketch fields or new negative fixtures; old valid
  examples still pass.
- **Major:** breaking enum or required-field change.

## What a version is not

- Not a claim that UNFYND App or a Core JAR shipped.
- Not marketing AVAILABLE.
- Not permission to treat this pack as Class B redistributable technology.

See [`ROADMAP-OPEN.md`](ROADMAP-OPEN.md) for later openness phases.
