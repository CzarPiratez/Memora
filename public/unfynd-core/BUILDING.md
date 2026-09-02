# Building — Class A conformance validator

**License:** Apache License 2.0 (same as this pack)

This guide runs the **Class A example validator** — the first runnable tooling
that exercises synthetic Memory / evidence sketches against `SPEC.md` rules.

Requirements:

- Python **3.10+** (stdlib only; no pip install)

## Quick start

From this pack root (`public/unfynd-core/` in the private monorepo, or the root
of the public `unfynd-core` repository):

```bash
python tools/validate_class_a_examples.py --self-test
python tools/validate_class_a_examples.py
```

Expected output: `self-test passed` then `OK` for each file under `examples/`.

## Validate one file

```bash
python tools/validate_class_a_examples.py --file examples/valid-memory-evidence.json
```

## What the validator checks

Structural shape aligned with
[`schema/memory-evidence-sketch.schema.json`](schema/memory-evidence-sketch.schema.json):

- Required fields per `sketchKind`
- Evidence classes from `SPEC.md` §3
- **Truth before intelligence** for `valid_asset_memory`:
  - Non-empty evidence list
  - Anchors cite existing evidence IDs
  - Cited evidence for anchors must be `DIRECT` or `VALIDATED_OBSERVATION`
  - Summary requires at least one truth-justifying evidence slice
- Export bundle sketches reference evidence slice IDs present in the bundle

Files named `valid-*.json` must pass. Files named `invalid-*.json` must fail the
valid Asset Memory rules (while remaining well-formed illustrative rejects).

## CI

The private monorepo runs this validator in GitHub Actions (`class-a-validator`
job in `.github/workflows/ci.yml`). The same command should pass before
publishing an updated Class A pack to the public remote.

## Scope boundary

This validator exercises **synthetic Class A examples only**. It does not validate
the UNFYND App Room schema, AI packs, or private fixtures.
