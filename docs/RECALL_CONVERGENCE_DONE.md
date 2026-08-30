# Recall convergence DONE (Canonical Recall program exit)

**Status: COMPLETE** — Canonical path **and** Retirement sections satisfied
(2026-08-31). Shared result model + shared Why contract implemented. Does
**not** claim marketing AVAILABLE, MIG-05 claim B (notes evidence embeddings),
or Grounded Answers / Act.

**Authority:** Architecture Freeze §3 (one evidence substrate; one canonical
recall pipeline); ADR-049 (Canonical Recall naming); Migration Spec MIG-06 /
MIG-07 / MIG-07B; `docs/LEGACY_RECALL_SURFACE.md` (living Live/Dual allowlist).

**Rule:** MIG-06 additive `SearchMemoryEvidence` existing is **NOT** program
complete. Program complete only when the **Retirement (mandatory)** section is
satisfied. “New path works” is insufficient; exit means old paths retired.

This file does **not** authorize starting MIG-06+ now. It is the program-exit
definition only.

---

## DONE only when

### Canonical path

- [x] Canonical Recall exists as the App product-facing retrieval API
      ViewModels call (ADR-049) — five Find ViewModels (four keyword + meaning)
- [x] Keyword/literal candidates enter through it (`SearchMemoryEvidence` or
      successor as candidate gen — MIG-06/07)
- [x] Semantic/meaning candidates enter through it (L8 folded in)
- [x] Evidence-level embedding candidates enter through it (candidate gen inside
      `CanonicalRecall`; index drain separate)
- [x] Anchor-aware structured stage exists as designed (MIG-07B) OR
      explicitly deferred with ADR (do not pretend anchors are recalled if
      unread)
- [x] Shared ranking stage exists inside Canonical Recall (L7 retired)
- [x] Shared result model exists
      `CanonicalRecallResult` + mappers (`MemoryEvidenceSearchHit`,
      `MeaningSearchHit` → `toCanonicalRecallResult()`); keyword UI hits carry
      `recall`
- [x] Shared Why / evidence presentation exists (path-labeled: keyword vs
      meaning per ADR-024 honesty — no silent relabel)
      `CanonicalRecallWhyCopy`; per-asset Why copy delegates

### Retirement (mandatory)

- [x] L1–L4 keyword Find paths no longer produce final results;
      `SearchPersisted*` / per-type support classes deleted (MIG-07)
- [x] L5 `PdfPageEmbedding*` retired (MIG-05 step 4)
- [x] L6 remains Retired (regression)
- [x] L7 meaning-local ranking removed from product path
- [x] L8 meaning ViewModel calls Canonical Recall (not a parallel product
      system)
- [x] `LEGACY_RECALL_SURFACE` Live/Dual count = 0 (or only Dual rows with
      documented zero product readers and immediate Retire-by — prefer 0)
- [x] Escape-hatch test answer: **NO** — user-visible search results cannot
      be produced without Canonical Recall

---

## Non-goals / do not claim from this file alone

- Does **not** authorize starting MIG-06+ now
- Does **not** implement Grounded Answers / Act / VisionEngine
- Does **not** require consolidating four Find screens into one UI (MIG-07
  allows asset-scoped UI over shared API)
- Does **not** close MIG-05 from Canonical path boxes alone (see MIG-05
  retirement checklist in
  `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`)
- Does **not** claim marketing AVAILABLE / ranking quality bars

---

## How to use

1. At each MIG-05 step 4 / MIG-06 / MIG-07 / MIG-07B checkpoint, re-read this
   file and `LEGACY_RECALL_SURFACE`.
2. Check a box only when current code + allowlist status prove it.
3. Record closure in CHANGELOG + CONTINUE only when **both** Canonical path
   and Retirement sections are fully satisfied.
4. Prefer Live/Dual count → **0**; Dual leftovers need zero product readers
   and an immediate Retire-by.
