# Recall convergence DONE (Canonical Recall program exit)

**Status: OPEN** — do not claim the Recall / Canonical Recall program complete
until every box in this file is verified. Boxes below remain unchecked until
truly done.

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

- [ ] Canonical Recall exists as the App product-facing retrieval API
      ViewModels call (ADR-049)
- [ ] Keyword/literal candidates enter through it (`SearchMemoryEvidence` or
      successor as candidate gen — MIG-06/07)
- [ ] Semantic/meaning candidates enter through it (L8 folded in)
- [ ] Evidence-level embedding candidates enter through it
- [ ] Anchor-aware structured stage exists as designed (MIG-07B) OR
      explicitly deferred with ADR (do not pretend anchors are recalled if
      unread)
- [ ] Shared ranking stage exists inside Canonical Recall (L7 retired)
- [ ] Shared result model exists
- [ ] Shared Why / evidence presentation exists (path-labeled: keyword vs
      meaning per ADR-024 honesty — no silent relabel)

### Retirement (mandatory)

- [ ] L1–L4 keyword Find paths no longer produce final results;
      `SearchPersisted*` / per-type support classes deleted (MIG-07)
- [ ] L5 `PdfPageEmbedding*` retired (MIG-05 step 4)
- [ ] L6 remains Retired (regression)
- [ ] L7 meaning-local ranking removed from product path
- [ ] L8 meaning ViewModel calls Canonical Recall (not a parallel product
      system)
- [ ] `LEGACY_RECALL_SURFACE` Live/Dual count = 0 (or only Dual rows with
      documented zero product readers and immediate Retire-by — prefer 0)
- [ ] Escape-hatch test answer: **NO** — user-visible search results cannot
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
