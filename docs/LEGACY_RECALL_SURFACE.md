# Legacy recall surface (living allowlist)

**Authority:** Architecture Freeze §3 (one evidence substrate; one canonical
recall pipeline) + ADR-049 (Canonical Recall naming) + Migration Spec
sequencing (MIG-05 / MIG-06 / MIG-07 / MIG-07B). This file does **not** redefine
architecture; it is operational enforcement so temporary product Find paths
cannot quietly become permanent.

**Updated:** 2026-08-30  
**Live/Dual count:** **4** (rows with Status Live or Dual; Retired excluded)

| Metric | Value |
|--------|-------|
| Live | 4 (L3–L4, L7, L8) |
| Dual | 0 |
| Retired | 4 (L1, L2, L5, L6) |
| **Live/Dual count (N)** | **4** |

**Operator entry (60s):** `docs/RECALL_ENFORCEMENT_INDEX.md`.  
**Escape-hatch audit:** `docs/ESCAPE_HATCH_AUDIT.md` (cadence + record template).

**Metric habit:** Whenever any row’s Status changes, recompute **Live/Dual
count N** in this header and mirror N in `CONTINUE.md`.

---

## Purpose

Track every interim / legacy product path that can still produce a
user-visible search result outside (or beside) Canonical Recall after
MIG-07 cutover. The allowlist must **shrink only**. Extending a non-Retired
row with new ranking, hit types, asset Finds, or Why pipelines is forbidden
by default.

This is not a new constitution. Cursor invariants and the full enforcement
map live in `.cursor/rules/unfynd-architecture-invariants.mdc` and
`docs/RECALL_ENFORCEMENT_INDEX.md`.

---

## Rules

1. **Status values only:** `Live` | `Dual` | `Cutover` | `Retired`.
2. **Forward-only transitions:** Live → Dual → Cutover → Retired. No reverse.
3. **New row requires an ADR** (and an explicit Retire-by MIG / step).
4. **No extend by default:** For any non-Retired row, do not add new ranking
   behavior, new hit types, new asset-type Find surfaces, or new Why
   pipelines. Defect fixes that preserve the existing contract are OK.
   Feature work waits for Canonical Recall or the listed MIG.
5. **Exceptions need a sunset:** Any temporary exception must name Retire by
   (MIG-0X or explicit step) before it ships.
6. **False-positive guard (ADR-049):** Extraction tables →
   `AssetMemoryFactSource` → MemoryBuilder is **ALLOWED** and must **not**
   appear as a “legacy Find” row. Forbidden (target after cutover): extraction
   DAO → user-visible search result outside Canonical Recall (or outside this
   allowlist while rows remain Live/Dual).
7. **Every non-Retired row MUST have Retire by:** MIG-0X or an explicit step.

---

## Escape-hatch test

Ask at every search / MIG checkpoint:

> Can a user-visible search result be produced **without** Canonical Recall?
> If yes, which **L#** rows still enable that?

Today the answer is **yes**. Enabling rows (Live/Dual): **L3, L4,
L7, L8**. Canonical Recall does not yet exist as a single application API
(ADR-049). Keyword Finds for photo/note (L3–L4) and product-facing
meaning Find (L8, with local ranking L7) remain live paths. **L1 is Retired**
(MIG-07 PDF cutover) — PDF keyword Find uses `SearchMemoryEvidence`
(PDF-filtered); do not resurrect `SearchPersistedPdfPageText`. **L2 is Retired**
(MIG-07 screenshot cutover) — screenshot keyword Find uses
`SearchMemoryEvidence` (SCREENSHOT-filtered); do not resurrect
`SearchPersistedScreenshotOcrText`. **L5 is Retired**
(MIG-05 step 4) — `PdfPageEmbedding*` types/table deleted; do not resurrect.

Full procedure, grep hints, and copy-paste record template:
`docs/ESCAPE_HATCH_AUDIT.md`.

---

## Audit cadence

Run the escape-hatch audit per `docs/ESCAPE_HATCH_AUDIT.md`:

- At every search / MIG / Find checkpoint
- At minimum before claiming MIG-05 full / MIG-07 / Recall convergence DONE
- Recommended when opening Find / Recall change-control

After each audit: update **Live/Dual N** in this header if status changed;
mirror N in `CONTINUE.md`; if N increased, require ADR +
`docs/LEGACY_EXTENSION_EXCEPTION.md`.

Soft machine guard (not full cutover CI): `scripts/check-legacy-recall-surface.sh`
+ CI job `Legacy recall surface guard` — see `RECALL_ENFORCEMENT_INDEX`.
Broader post–MIG-07 cutover gates remain future work.

---

## Allowlist table

| ID | Legacy product path | Replacement | Status | Retire by |
|----|---------------------|-------------|--------|-----------|
| L1 | PDF keyword Find (`SearchPersistedPdfPageText` → ViewModel) | Canonical Recall via `SearchMemoryEvidence` (MIG-06/07) | Retired | MIG-07 PDF (done) |
| L2 | Screenshot OCR keyword Find (`SearchPersistedScreenshotOcrText` → ViewModel) | Canonical Recall via `SearchMemoryEvidence` (MIG-06/07) | Retired | MIG-07 screenshot (done) |
| L3 | Photo OCR keyword Find | same | Live | MIG-07 |
| L4 | Note keyword Find | same | Live | MIG-07 |
| L5 | `PdfPageEmbedding*` dual-write / page vector table | Evidence-only index path (`MemoryEvidenceEmbeddingStore`); table DROP Room 15 | Retired | MIG-05 step 4 (done) |
| L6 | `SavedPdfPageTextSource` used for product meaning ranking | `MemoryEvidence` excerpts | Retired | MIG-05 step 3 (done) |
| L7 | Meaning-local ranking/boost inside `SearchAssetMemoriesByMeaning` | Shared ranking stage inside Canonical Recall | Live | MIG-07 / RecallRanker stage (not a separate Find) |
| L8 | `SearchAssetMemoriesByMeaning` as product-facing meaning Find (not yet behind Canonical Recall facade) | Canonical Recall (MIG-07 wiring; meaning remains a candidate generator) | Live | MIG-07 |

### Row notes

- **L1 Retired** (MIG-07 PDF): `SearchPersistedPdfPageText` deleted. PDF
  keyword Find ViewModel binds `SearchMemoryEvidence` with `AssetType.PDF`.
  Readiness counts READY PDF Memory evidence (not `PdfExtractionDao` search
  corpus). `PdfKeywordSearchSupport` remains as shared helpers for L3–L4 +
  highlight. If `SearchPersistedPdfPageText` or extraction-DAO PDF Find
  reappears, **STOP** — do not keep L1 Retired.
- **L2 Retired** (MIG-07 screenshot): `SearchPersistedScreenshotOcrText`
  deleted. Screenshot keyword Find ViewModel binds `SearchMemoryEvidence`
  with `AssetType.SCREENSHOT`. Readiness counts READY screenshot Memory
  evidence (distinct documents; not `ScreenshotOcrExtractionDao` search
  corpus). `ScreenshotOcrKeywordSearchSupport` remains for shared helpers.
  If `SearchPersistedScreenshotOcrText` or extraction-DAO screenshot Find
  reappears, **STOP** — do not keep L2 Retired.
- **L6 Retired** means out of **product recall ranking**. Verified 2026-08-29
  against `SearchAssetMemoriesByMeaning`: constructor injects
  `MemoryEvidenceEmbeddingStore`; KDoc and constructor params exclude
  `SavedPdfPageTextSource` for ranking. If SavedPdfPageTextSource reappears
  on the ranking path, **STOP** — do not keep L6 Retired.
  (`SavedPdfPageTextSource` may still exist for open-original / meaning-
  **index** candidate build; that is not L6.)
- **L5 Retired** (MIG-05 step 4): `PdfPageEmbedding*` entity/DAO/store/
  domain types deleted; Room 15 drops `pdf_page_embeddings`.
  `IndexPdfPageEmbeddings` is evidence-only. If those types or the page
  table reappear, **STOP** — do not keep L5 Retired.
- **L8** tracks meaning Find as an **interim product path**. Do not mistake
  `SearchAssetMemoriesByMeaning` for Canonical Recall already existing
  (ADR-049: naming only; no single API in code yet).
- **L7** is ranking/boost local to the meaning use case, not a separate Find
  surface; it still counts toward Live/Dual until shared ranking lives inside
  Canonical Recall.
- **MIG-07 screenshot note (N=4):** L1+L2 Retired. L3–L4 still Live. Canonical
  Recall still **not** a live App API. Do **not** claim full MIG-07 / Recall
  DONE.

---

## How to update

At every search or MIG checkpoint (and when accepting a related ADR):

1. Re-run the escape-hatch audit (`ESCAPE_HATCH_AUDIT.md`); list enabling L#
   rows (after step 4: **without L5**; after screenshot: **without L1/L2**).
2. Confirm statuses against **current code** (do not invent).
3. Advance status only forward when the replacement is verified.
4. Recompute **Live/Dual count N** in the header (mandatory on every status
   change).
5. Point `CONTINUE.md` Current checkpoint at this file with the current N and
   the audit pointer.
6. Record the checkpoint in CHANGELOG / change-control when governance requires
   it.
7. Do **not** add rows without an ADR; do **not** extend non-Retired rows
   with new Find features.

**Out of scope for this file:** Claiming Canonical Recall exists in code, or
claiming full MIG-05 Spec close while non-PDF evidence indexer remains deferred,
or authorizing L3–L4 / MIG-07B from this file alone. Soft resurrection guard
lives in `scripts/check-legacy-recall-surface.sh` (see
`RECALL_ENFORCEMENT_INDEX`; MIG-07 PDF + screenshot allowlist). Broader
post–MIG-07 full cutover gates remain future work.
