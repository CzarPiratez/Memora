# Change control — Meaning retrieval stack (ADR-055)

**Date:** 2026-09-19
**Type:** Authorized program plan (implementation slices below)
**Status:** **Authorized. Occupancy path (D-23–D-28) CLOSED.** Keyword Find
frozen. Live/Dual **N = 0**.
**Governing ADR:** ADR-055 in `docs/DECISIONS.md`
**Ask Model:** I1 (same job, many phrasings) · I2 (same-file qualifier) ·
P-AND vs P-LIST. Ceiling remains `HUMAN_RECALL_ASK_MODEL.md`.
**Does not authorize:** synonym nets (`classes` ≠ `timetable`), cloud ranker,
a second Find product, one-box UI this slice, keyword ranking/FTS5 replace,
marketing AVAILABLE, occupancy D-29

## Why this exists

Founder (2026-09-19): adding a word or changing wording forced another
occupancy ticket (D-23–D-28). That is not “any wording of the same job.”
USE is a 2018 MediaPipe pack, not the product. D-20’s “swap encoder only if
gold is at thousands” is **superseded**.

Keyword Find already finds the swimming PDFs. Meaning must **use** that
lexical generator, not compete as a second box the user has to learn.

## Closed path (do not resume)

D-23–D-28 landed as **history**. They stay in the tree until a later slice
replaces occupancy with one score. **No new occupancy change control.**
Do not “fix the next cue” by mixing families, Exact-first, or starved share.

D-14 / D-27 self-capture demotion stays: a picture of UNFYND is not the
original.

## The machine (every cue, any word count)

1. **Roles** — wrappers off; **head** / **qualifier** / **list** / constraint.
   `when are the swimming classes for grade 2` is the same head as
   `swimming classes`, plus qualifier `grade 2`. A qualifier may promote a
   file that has it. It must not open a `{grade}`-only Find.
2. **Two generators, one boundary** — lexical (`SearchMemoryEvidence`) +
   meaning (`SearchAssetMemoriesByMeaning`). Fuse inside
   `CanonicalRecall.searchByMeaning`. Keyword UI is unchanged.
3. **One score** — more head (and qualifier) overlap beats less; then fused
   rank; then cosine / rerank. Self-captures never count as overlap.
4. **Embedder is a pack** — `EmbeddingEngine` stays. USE is the current
   file, not a religion. Bake-off vs BGE-small or E5-small on founder gold
   cues; swap only if measured. Reindex once, disclosed.
5. **Rerank** — ADR-051 Stage A on the fused top ~40 when the pack is
   installed; identity fallback otherwise.
6. **Filters** — time/type only when we can honor them. Otherwise ignore.

Adding a word may help a file that has that word, or leave the old page.
It must not invent a parallel product.

## Slices (implement in order; one verified checkpoint each)

| Slice | Work | Keyword Find |
|---|---|---|
| **1** | Query roles + one monotonic score on the meaning page | Untouched |
| **2** | Fuse `SearchMemoryEvidence` candidates into `searchByMeaning` (RRF or equivalent inside Canonical Recall) | Untouched (read-only use of the generator) |
| **3** | Encoder bake-off on gold cues (USE vs one modern small model) | Untouched |
| **4** | Swap default meaning pack if bake-off wins; disclosed reindex | Untouched |
| **5** | Wire Stage A cross-encoder on the fused shortlist | Untouched |

**Later (own change control, not this week):** FTS5 under the lexical
generator; one-box UI; relative-time resolve.

## Frozen

- Keyword Find screens and `CanonicalRecall.invoke` ranking.
- Synonym nets. Cloud inference. New Live/Dual rows. Occupancy tickets.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning retrieval (roles, fusion,
  one score, measured embedder). Keyword remains a generator, not a second
  product.
CURRENT LEGACY PATH: none — Live/Dual N = 0
TARGET PATH: CanonicalRecall.searchByMeaning = roles → lexical+meaning
  fuse → one score → optional Stage A rerank
WHY THIS CONVERGES: two generators already exist; occupancy was compensating
  for not fusing them; USE stays a pack behind EmbeddingEngine
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: MeaningNamedWordDepthPage as the
  ranking authority (code may remain until slice 1 replaces it)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** Ask Model I1/I2; Local AI Spec §4 EmbeddingEngine +
  RecallRanker; ADR-049; ADR-051; FD-01; founder 2026-09-19 reliability ask.
- **Holistic scenarios:** same job many phrasings (`swimming classes` /
  `when are…` / `… for grade 2`); list asks (`scan silky`, `wifi password`);
  Exact (`passport`, `swimming timetable`); self-captures last; keyword
  screens unchanged; empty/offline/pack-missing stay honest.
- **Acceptance (plan):** this record + ADR-055 + CONTINUE current
  checkpoint point here; occupancy “next ticket” language removed.
- **Acceptance (slice 1, later):** unit tests for roles + one score;
  hold wifi / silky / passport / timetable; grade-2 is the same head.
- **Emulator/manual:** no `connectedDebugAndroidTest`.

## Delivery record (2026-09-19) — plan only

- **Files/layers:** ADR-055; this file; CONTINUE / CHANGELOG /
  PROGRAM_STATE / RECALL_ENFORCEMENT_INDEX / registry / AGENTS pointer.
  **No ranking code in this commit.**
- **Git commit:** local; do not push.
