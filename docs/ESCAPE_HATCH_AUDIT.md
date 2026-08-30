# Escape-hatch audit (Find bypass check)

**Purpose:** Prove whether a user-visible Find / search hit can still be produced
**without** Canonical Recall. Makes Live/Dual convergence measurable on a
cadence.

**Authority:** `docs/LEGACY_RECALL_SURFACE.md`; ADR-049; Cursor rule
`unfynd-architecture-invariants.mdc`; `docs/RECALL_CONVERGENCE_DONE.md`.

**Does not:** Authorize MIG-06+; claim full cutover CI (post–MIG-07) is done.
Soft resurrection guard: `scripts/check-legacy-recall-surface.sh` + CI job
`Legacy recall surface guard` (see `RECALL_ENFORCEMENT_INDEX`).


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
| L1–L4 | `ui/search` ViewModels formerly calling `SearchPersisted*` (PDF / screenshot / photo / note keyword) — **Retired**; now MemoryEvidence-backed |
| L8 | **Retired** — `MeaningSearchViewModel` → `CanonicalRecall.searchByMeaning` (MIG-07B Slice 3) |
| L7 | **Retired** — `MeaningEvidenceTokenBoost` in `AnchorAwareMeaningRecallRanking` only (MIG-07B Slice 4) |
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

## Status truth check — 2026-08-29

| Field | Value |
|-------|-------|
| Date | 2026-08-29 |
| Live/Dual N | **6** |
| Enabling L#s | L1, L2, L3, L4, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049) |
| Delta | none — CONTINUE + LEGACY + code agree (re-verify after MIG-05 step 4) |
| Auditor | enterprise status truth check (read-only) |

**Canonical Recall fiction scan 2026-08-29: PASS — target-only**

---

## Checkpoint — MIG-06 step 1 (additive SearchMemoryEvidence)

| Field | Value |
|-------|-------|
| Date | 2026-08-29 |
| Live/Dual N | **6** |
| Enabling L#s | L1, L2, L3, L4, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049; UI still on L1–L4) |
| Delta | none — MIG-06 candidate generator exists; product escape-hatch unchanged |
| Auditor | MIG-06 step 1 delivery |

---

## Checkpoint — MIG-07 PDF keyword cutover

| Field | Value |
|-------|-------|
| Date | 2026-08-29 |
| Live/Dual N | **5** |
| Enabling L#s | L2, L3, L4, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049; L1 no longer enables) |
| Delta | L1 Live → Retired; N 6 → 5; PDF Find → `SearchMemoryEvidence` (PDF filter); `SearchPersistedPdfPageText` deleted |
| Auditor | MIG-07 PDF cutover delivery |

---

## Checkpoint — MIG-07 screenshot keyword cutover

| Field | Value |
|-------|-------|
| Date | 2026-08-30 |
| Live/Dual N | **4** |
| Enabling L#s | L3, L4, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049; L1/L2 no longer enable) |
| Delta | L2 Live → Retired; N 5 → 4; screenshot Find → `SearchMemoryEvidence` (SCREENSHOT filter); `SearchPersistedScreenshotOcrText` deleted |
| Auditor | MIG-07 screenshot cutover delivery |

---

## Checkpoint — MIG-07 photo keyword cutover

| Field | Value |
|-------|-------|
| Date | 2026-08-30 |
| Live/Dual N | **3** |
| Enabling L#s | L4, L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049; L1–L3 no longer enable) |
| Delta | L3 Live → Retired; N 4 → 3; photo Find → `SearchMemoryEvidence` (PHOTO filter); `SearchPersistedPhotoOcrText` deleted |
| Auditor | MIG-07 photo cutover delivery |

---

## Checkpoint — MIG-07 note keyword cutover

| Field | Value |
|-------|-------|
| Date | 2026-08-30 |
| Live/Dual N | **2** |
| Enabling L#s | L7, L8 |
| Escape-hatch | YES (Canonical Recall not yet one App API — ADR-049; L1–L4 no longer enable) |
| Delta | L4 Live → Retired; N 3 → 2; note Find → `SearchMemoryEvidence` (NOTE filter); `SearchPersistedNotePageText` + `NotePageKeywordSearchSupport` deleted |
| Auditor | MIG-07 note cutover delivery |

---

## Checkpoint — MIG-07B Slice 3 (L8 retired)

| Field | Value |
|-------|-------|
| Date | 2026-08-31 |
| Live/Dual N | **1** |
| Enabling L#s | L7 |
| Escape-hatch | YES (L7 local ranking still outside shared Canonical Recall ranker) |
| Delta | L8 Live → Retired; N 2 → 1; meaning Find → `CanonicalRecall.searchByMeaning`; `MeaningSearchViewModel` no longer binds `SearchAssetMemoriesByMeaning` |
| Auditor | MIG-07B Slice 3 delivery |

---

## Checkpoint — MIG-07B Slice 4 (L7 retired; N=0)

| Field | Value |
|-------|-------|
| Date | 2026-08-31 |
| Live/Dual N | **0** |
| Enabling L#s | (none) |
| Escape-hatch | **NO** — product Find paths route through `CanonicalRecall` |
| Delta | L7 Live → Retired; N 1 → 0; token boost moved from `SearchAssetMemoriesByMeaning` to `AnchorAwareMeaningRecallRanking` |
| Auditor | MIG-07B Slice 4 delivery |

---

## Related

- Allowlist + metric: `docs/LEGACY_RECALL_SURFACE.md`
- Program exit: `docs/RECALL_CONVERGENCE_DONE.md`
- Enforcement index: `docs/RECALL_ENFORCEMENT_INDEX.md`
- Extension if N grows: `docs/LEGACY_EXTENSION_EXCEPTION.md`
- MIG-06 step 1: `docs/CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE.md`
- MIG-07 PDF: `docs/CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER.md`
- MIG-07 screenshot: `docs/CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER.md`
- MIG-07 photo: `docs/CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER.md`
- MIG-07 note: `docs/CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER.md`
