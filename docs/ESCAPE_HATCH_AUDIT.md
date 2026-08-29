# Escape-hatch audit (Find bypass check)

**Purpose:** Prove whether a user-visible Find / search hit can still be produced
**without** Canonical Recall. Makes Live/Dual convergence measurable on a
cadence.

**Authority:** `docs/LEGACY_RECALL_SURFACE.md`; ADR-049; Cursor rule
`unfynd-architecture-invariants.mdc`; `docs/RECALL_CONVERGENCE_DONE.md`.

**Does not:** Authorize MIG-06+; run CI grep gates (future after
MIG-07 cutover); treat Memory construction as Find.

---

## Cadence

Run this audit:

1. At **every** search / MIG / Find checkpoint.
2. At **minimum** before claiming any of: MIG-05 full DONE, MIG-07 done, or
   Recall convergence DONE (`RECALL_CONVERGENCE_DONE`).
3. **Recommended** when opening a Find / Recall change-control (fill the
   Escape-hatch line in the architectural convergence block from a fresh audit).

---

## Procedure (checklist)

- [ ] Read `docs/LEGACY_RECALL_SURFACE.md`. Record **Live/Dual count N** and list
      every Live/Dual **L#**.
- [ ] For each Live/Dual L#: name the **production entrypoint**
      (ViewModel / use case) that can still emit a **user-visible** hit.
- [ ] Answer: Can UI produce a search hit **without** Canonical Recall?
      **YES** / **NO**
- [ ] If **YES**: list enabling L#s (expected until MIG-07 cutover).
- [ ] If claiming program / MIG DONE: **N must be 0** (or only Dual rows with
      documented zero product readers per `RECALL_CONVERGENCE_DONE`) **and**
      escape-hatch = **NO**.
- [ ] Update `CONTINUE.md` Current checkpoint one-liner with current N (see
      template pattern there).
- [ ] If N **increased** since last audit: STOP — require ADR +
      `docs/LEGACY_EXTENSION_EXCEPTION.md` (or new-row ADR). Link that record
      below.

### Grep / inspection hints (manual; not CI yet)

Non-CI until after MIG-07 cutover. Prefer current-code inspection over memory.

| Hint | What to look for |
|------|------------------|
| L1–L4 | `ui/search` ViewModels calling `SearchPersisted*` (PDF / screenshot / photo / note keyword) |
| L8 | `SearchAssetMemoriesByMeaning` called from `MeaningSearchViewModel` (product meaning Find) |
| L7 | Ranking/boost still local inside `SearchAssetMemoriesByMeaning` (counts Live until shared Canonical Recall ranking) |
| L5 | `PdfPageEmbeddingStore` / `pdf_page_embeddings` must be **absent** from main source after MIG-05 step 4 (L5 Retired); migrations may mention DROP |

### False-positive guard

Do **not** treat as Find escape hatches:

- Extraction tables → `AssetMemoryFactSource` → MemoryBuilder → Memory
  (construction path only)

---

## Record template

Copy into change-control or `CONTINUE.md` when auditing:

```
Date:
Live/Dual N:
Enabling L#s:
Escape-hatch YES/NO:
Delta since last audit:
Auditor:
```

If N increased: exception / ADR link: (see `LEGACY_EXTENSION_EXCEPTION.md`)

---

## Baseline (as of allowlist auth)

| Field | Value |
|-------|-------|
| Date | 2026-08-29 |
| Live/Dual N | 7 |
| Enabling L#s | L1, L2, L3, L4, L5, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049) |
| Delta | Initial enforcement-program baseline |
| Auditor | docs Step 7 (procedure landed; re-verify on next code checkpoint) |

---

## Checkpoint — MIG-05 step 4 (PdfPageEmbedding* retired)

| Field | Value |
|-------|-------|
| Date | 2026-08-29 |
| Live/Dual N | **6** |
| Enabling L#s | L1, L2, L3, L4, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049) |
| Delta | L5 Dual → Retired; N 7 → 6; page embedding substrate deleted (Room 15) |
| Auditor | MIG-05 step 4 delivery |

---

## Related

- Allowlist + metric: `docs/LEGACY_RECALL_SURFACE.md`
- Program exit: `docs/RECALL_CONVERGENCE_DONE.md`
- Enforcement index: `docs/RECALL_ENFORCEMENT_INDEX.md`
- Extension if N grows: `docs/LEGACY_EXTENSION_EXCEPTION.md`
