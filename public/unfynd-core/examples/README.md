# Examples (synthetic, illustrative)

**License:** Apache License 2.0 (same as this Class A pack)  
**Authorized by:** Private monorepo ADR-048 (synthetic non-personal Class A
reference samples)

These files help humans **inspect** Core Memory / evidence contracts. They are
illustrative sketches aligned with [`SPEC.md`](../SPEC.md) - not production
schema locks, not personal data, and not App fixtures.

Treat field names and nesting as **sketch** shape. Implementers should validate
against their own schemas and tests. Runnable tooling:

```bash
python tools/validate_class_a_examples.py --self-test
```

See [`BUILDING.md`](../BUILDING.md).

| File | Role |
|---|---|
| [`valid-memory-evidence.json`](valid-memory-evidence.json) | One valid Memory + DIRECT evidence sketch |
| [`valid-audio-timecode.json`](valid-audio-timecode.json) | Valid Memory with temporal locator on transcript evidence |
| [`valid-evidence-package-export.json`](valid-evidence-package-export.json) | Export bundle planning sketch (cited slices only) |
| [`invalid-invented-fact.json`](invalid-invented-fact.json) | Rejected: claim without supporting evidence |
| [`invalid-empty-evidence.json`](invalid-empty-evidence.json) | Rejected: empty required evidence for a durable claim |

All content is **fake** (made-up cafe / receipt text). Do not treat it as real
user data.
