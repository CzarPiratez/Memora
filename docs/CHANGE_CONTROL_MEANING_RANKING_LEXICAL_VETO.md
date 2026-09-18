# Change control — Meaning ranking lexical veto (D-20)

**Date:** 2026-09-18
**Type:** Decision plan + authorized next slices (docs first; code not in this
record)
**Status:** **Authorized plan.** Probe and Phase 0/1 are not delivered until
their own delivery records.
**Ask Model:** **P-AND vs P-LIST**; **P-EVIDENCE**; **P-MEANING-ONLY** (honesty
banners may remain; they must not delete neighbours)
**Does not authorize:** marketing AVAILABLE, synonym nets, FTS5, encoder swap,
hybrid/RRF product wire, ANN, one-box Find, keyword Find changes, schema
migration, `fallbackToDestructiveMigration`

## Why this exists

Find by meaning already cosine-scans the **full** USE index
(`memory_embeddings` + `memory_evidence_embeddings` for the installed model),
then collapses to one hit per asset. On device, cues such as `kids water
lessons` and `swimming schedule` still surface files that contain the typed
words (or both words) and hide files the person meant (swimming timetables).

That is **not** “demo kNN of 30 vectors.” Retrieval has already run. Two
**lexical admission/veto** stages then throw neighbours away:

1. **`selectCandidatePool`** (when more than 30 assets survive the 0.05 floor):
   seats by named-token **count** first, cosine inside a depth (D-15). A
   timetable that only has `swimming` loses to 30 files that have
   `swimming` **and** `schedule`.
2. **`applyLexicalPrecisionTier`:** if any remaining hit is Exact (every named
   word in `lexicalHaystack()`), that set **replaces the list**. Combined with
   `MeaningEvidenceTokenBoost` (`+0.35`) and `MeaningTrustedHitPolicy`
   (0.22 of top **boosted** score, max 5), Exact competitors bury paraphrase.

`MeaningOnly` only runs when **zero** named words hit **any** remaining
candidate. On a mixed library that is almost never true.

Keyword Find (PDF / photo / screenshot / note) is a **different call**
(`CanonicalRecall.invoke` → `SearchMemoryEvidence` LIKE, multi-token AND). It
is working as designed for “find by saved text.” This plan must not change it.

The goal is **reliable, evidence-preserving Recall over persistent Memory
on-device** — not resemblance to a cloud enterprise search engine. Search is
how that capability is exposed.

## Scope

### In this record (docs)

- Defect **D-20**, sequence, freezes, gates, and what is **not** this slice.
- Honesty: D-12/D-15 did not finish paraphrase when an Exact competitor exists.

### Authorized next code (separate Agent tickets; do not combine)

| Step | What | Must not |
|---|---|---|
| **Probe** | On-device diagnostic: ~20 gold cues; three ranks (best gold **chunk** full cosine; gold **asset** after collapse; in vs out of token-seated 30 vs cosine-seated 30). Reuse `MeaningRecallCue.embedText` + existing stores. | Production ranking change; JVM script against a PC copy of SQLCipher without MediaPipe |
| **Phase 0** | Debug / opt-in `MeaningSearchTrace` on the **live** path (raw query, content tokens, vectors scanned, survived floor, assets after collapse, admitted, tier, `droppedByTier`, shown, top raw cosine, latency). Land **before** deleting the tier so `droppedByTier` has a baseline. | Ranking policy change in the same commit |
| **Phase 1** | Seat the pool by **cosine**, not token count. Stop Exact/Partial/MeaningOnly from **deleting** rows. Coverage remains a **boost**, not a subset. Invert `apply_prefers_the_exact_tier_and_drops_partial_hits`. Survival test must pass **after** trusted trim. Banners may still describe mix. | IDF, `RankFeatures`, FTS, encoder, hybrid, ANN, schema, keyword ViewModels, `SearchMemoryEvidence`, raising the pool in the same change, retuning `+0.35` / `0.22` unless the survival test cannot pass (then band on **raw cosine** only) |

### Out (not authorized here)

- FTS5 / SQLCipher virtual tables / triggers
- Replacing keyword LIKE/AND with OR fusion
- Encoder swap (USE → BGE/E5) until the probe says gold chunks sit at rank
  thousands
- Off-heap vector cache / HNSW
- In-memory BM25 on dense top-K (earned **after** measure, still no schema)
- Carrying `evidenceId` on `MeaningSearchHit` (worthwhile; **not** this week)
- Unifying Find screens (Phase 7)
- Marketing AVAILABLE

## Decision gates (after Phase 1, both cue classes)

Measure on the founder library. Keep **paraphrase** and **exact-token** cues
in the set (`swimming schedule` / `when are the swimming classes` **and**
`passport` / `Aadhaar`). Do not tune coverage until exact-token recall
regresses.

| Probe / device result | Next |
|---|---|
| Gold chunk in dense top ~50, still missing on screen before Phase 1 | Phase 1 should matter |
| Gold chunk at rank thousands | Encoder next; do not celebrate Phase 1 |
| After Phase 1, paraphrase appears and exact-token still works | Stop. Do not start FTS. |
| After Phase 1, paraphrase still fails and probe was top-50 | Inspect trusted band / `+0.35` on **raw** cosine; still no FTS |
| Exact-token (`Aadhaar`) regresses | Revert or retune boost; do not ship |

Hybrid retrieval is **not** decided yet. Dense already scans the full indexed
corpus. The first question is whether USE found the right chunk and ranking
discarded it.

## Frozen (keyword Find)

Do not modify:

- `SearchMemoryEvidence` / `MemoryEvidenceExcerptSearch` / LIKE AND
- `PdfKeywordSearchViewModel`, `PhotoOcrKeywordSearchViewModel`,
  `ScreenshotOcrKeywordSearchViewModel`, `NotePageKeywordSearchViewModel`
- `CanonicalRecall.invoke` (keyword façade)
- Room schema / migrations / `fallbackToDestructiveMigration`
- MemoryBuilder / extraction tables as a Find path

`CanonicalRecall.searchByMeaning` and meaning ranking objects only.

## Convergence block

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning ranking + meaning candidate
  admission (pool seating). Keyword candidate gen unchanged.
CURRENT LEGACY PATH (L# or none): none — Live/Dual N = 0
TARGET PATH: SearchAssetMemoriesByMeaning (full-index cosine, cosine-seated
  pool) → AnchorAwareMeaningRecallRanking (coverage as boost, not subset) →
  MeaningTrustedHitPolicy → Meaning Find UI
WHY THIS CONVERGES: removes a ranking/admission veto inside the sole Find
  boundary; no new generator, no new product Find, no Live/Dual growth
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none (keyword Find stays)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged — Live/Dual N = 0
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** Ask Model P-AND vs P-LIST; P-EVIDENCE; scenario bar
  north star (short trustworthy list); Local AI Spec §9 (embeddings propose
  candidates, cannot be truth). D-20.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, LEGACY_RECALL_SURFACE (N = 0),
  HUMAN_RECALL_ASK_MODEL, MEANING_FIND_PRODUCT_SCENARIO_BAR,
  PROGRAM_STATE D-11/D-12/D-15, CHANGE_CONTROL_TEMPLATE,
  CHANGE_CONTROL_MEANING_ONLY_TIER, CHANGE_CONTROL_MEANING_FIND_MF1_PRECISION.
- **Current-code evidence inspected:**
  `CanonicalRecall.searchByMeaning` (pool `min(limit*3, 30)`; no
  `SearchMemoryEvidence`; `searchHybrid` unwired);
  `SearchAssetMemoriesByMeaning` (full `listForModel`, `MIN_CANDIDATE_SCORE`
  0.05, asset collapse, `selectCandidatePool` token-count seating);
  `applyLexicalPrecisionTier` Exact replaces list;
  `MeaningEvidenceTokenBoost` +0.35; `MeaningTrustedHitPolicy` 0.22 / max 5;
  keyword ViewModels call `canonicalRecall(...)`.
- **Open ADRs:** none new. Not Act. Not AVAILABLE. Not FC-02 product-complete.
- **Privacy:** on-device only; probe and trace must not upload corpus text.
  Trace is debug / opt-in.
- **Smallest safe change (this commit):** documentation of D-20 and the
  sequence. Next code: probe **or** Phase 0, not mixed with ranking.
- **Acceptance criteria (this docs slice):**
  - [x] Plan recorded; keyword Find named frozen
  - [x] Probe / Phase 0 / Phase 1 sequenced; encoder and FTS not authorized
  - [ ] Probe delivered (later ticket)
  - [ ] Phase 0 delivered (later ticket)
  - [ ] Phase 1 delivered (later ticket)
  - [ ] Device measure on paraphrase **and** exact-token cues
- **Holistic scenarios (before implement):**
  - User: `swimming schedule` — timetable PDF still listed when a screenshot
    contains both words
  - User: `when are the swimming classes` — same class of job; may share
    fewer tokens; probe decides if USE retrieved it
  - User: `Aadhaar` / `passport` — exact-token Find by meaning must not
    regress to junk neighbours
  - User: keyword `swimming timetable` on PDF Find by text — **unchanged**
  - User: honest miss when nothing is close — relative band must not pad
    five irrelevant files (Phase 1 does not retune the floor unless tests
    force banding on raw cosine)
  - Anti: synonym net (`schedule` means `timetable`)
  - Anti: FTS on encrypted DB this slice
  - Technical: Live/Dual N = 0; no second Find path
  - Edge: pool size ≤ 30 → `selectCandidatePool` is already a no-op; cosine
    seating still required for larger libraries
  - Edge: trusted band can re-drop a neighbour after the tier is gone;
    survival test is after trim

## Alternatives considered

- **Claude 7-phase spec as the build ticket** — rejected for this week.
  Destination (no lexical veto, keep evidence identity, measure, scale later)
  is useful; FTS5 + feature ranker + encoder as one implementation is not.
- **Phase 1 as RankFeatures + IDF + 0.30 abs floor + pool 200** — rejected.
  `retrievalScore` does not exist until fusion; 0.30 is uncalibrated; IDF
  needs corpus DF.
- **Skip the probe** — rejected. One on-device rank of gold chunks answers
  Phase 1 vs encoder before writing ranking code.
- **Raise pool to 200 instead of cosine seating** — rejected as the *fix*.
  It masks D-15 seating; seating by cosine is the explicit change.
- **In-memory BM25 on dense top-K this week** — deferred. Right way to
  *earn* hybrid without SQLCipher FTS; not before measure.
- **Change keyword AND to OR** — forbidden. Literal Find is a different job.

## Delivery record (this commit)

- **Files/layers:** this change control; `CONTINUE.md`; `PROGRAM_STATE`
  D-20; `CHANGELOG`; scenario-bar honesty row.
- **Automated verification:** docs only.
- **Emulator/manual:** n/a this slice.
- **Known limitation:** live meaning path still has both vetoes until Phase 1.
  USE may still fail pure paraphrase; the probe decides.
- **Git commit:** local checkpoint when the founder asks; do not push.
