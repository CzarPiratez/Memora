# Evidence Package export contract (planning sketch)

**Status:** Class A planning document — **not** a shipping wire format or hashed
spec  
**License:** Apache License 2.0  
**Authority:** Informs post-MVP platform phase (P6) in private
`docs/POST_MVP_PROGRAM_V1.md`. Does not authorize implementation by itself.

---

## Purpose

Define what **portable Memory** means for UNFYND Core: cited **evidence slices**
with locators and revision references — **not** raw Assets, embeddings as truth,
or unverifiable chat facts.

Industry memory portability efforts (vendor-neutral protocols, agent memory
exports) typically move **facts and preferences**. UNFYND exports **evidence with
provenance** so importers can treat imported material as **candidate evidence**,
not automatic truth.

---

## Export bundle (conceptual)

| Field | Required | Meaning |
|---|---|---|
| `bundleId` | Yes | Stable id for this export artifact |
| `exportedAt` | Yes | ISO-8601 export timestamp |
| `coreContractRevision` | Yes | Class A pack revision string |
| `memories` | Yes | Memory id + `revisionHint` + list of cited `evidenceSliceIds` |
| `evidenceSlices` | Yes | Bounded evidence records (see below) |
| `signatureHint` | Optional | Planning placeholder for future signed exports |

### Evidence slice (exported)

Each slice includes at minimum:

- `evidenceId`
- `evidenceClass` (`DIRECT`, `VALIDATED_OBSERVATION`, `RETRIEVAL_SIGNAL`, `HYPOTHESIS`)
- `kind`, `excerpt`, `provenance`
- Optional `locator` (`page`, `span`, `timecode`, `segment`, `message_id`)

**Never included by default:**

- Original file bytes (photo, PDF, audio, video)
- Full embedding vectors (may be separate optional retrieval artifact with
  `RETRIEVAL_SIGNAL` class only)
- User secrets, tokens, URIs that identify people without consent

---

## Import rules (conceptual)

1. Imported bundles land as **candidate evidence** — importer runs validation and
   MemoryBuilder rules before promoting to durable Memory.
2. Conflicts resolve via **revision lineage**, not silent overwrite.
3. `RETRIEVAL_SIGNAL` and `HYPOTHESIS` slices do not alone justify new durable
   claims on import.

---

## Illustrative example

See [`examples/valid-evidence-package-export.json`](examples/valid-evidence-package-export.json).

Runnable check:

```bash
python tools/validate_class_a_examples.py --file examples/valid-evidence-package-export.json
```

---

## Relationship to other documents

| Document | Role |
|---|---|
| [`SPEC.md`](SPEC.md) | Evidence classes and truth rules |
| [`INTEGRATION.md`](INTEGRATION.md) | How exporters/importers fit external tools |
| Private `docs/POST_MVP_PROGRAM_V1.md` | When export implementation is authorized |

Implementation requires private change control and ADR where governance demands.
