# Examples (synthetic, illustrative)

**License:** Apache License 2.0 (same as this Class A pack)  
**Authorized by:** Private monorepo ADR-048 (synthetic non-personal Class A
reference samples)

These files help humans **inspect** Core Memory / evidence contracts. They are
**not**:

- Production schema locks or API guarantees
- Personal, private, or evaluation fixtures from any App repository
- Proof that Links, Events, Knowledge, Act, or AVAILABLE recall are shipped
- Runnable validators (a public validator may come later)

Treat field names and nesting as **sketch** shape aligned with [`SPEC.md`](../SPEC.md)
evidence rules. Implementers should validate against their own schemas and tests.

| File | Role |
|---|---|
| [`valid-memory-evidence.json`](valid-memory-evidence.json) | One valid Memory + DIRECT evidence sketch |
| [`invalid-invented-fact.json`](invalid-invented-fact.json) | Rejected: claim without supporting evidence |
| [`invalid-empty-evidence.json`](invalid-empty-evidence.json) | Rejected: empty required evidence for a durable claim |

All content is **fake** (made-up cafe / receipt text). Do not treat it as real
user data.
