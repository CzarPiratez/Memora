# Change control — Meaning retrieval stack (ADR-055)

**Date:** 2026-09-19
**Type:** Authorized program plan (implementation slices below)
**Status:** **Slice 4 landed in tree 2026-09-21 (BGE product pack). Founder USB reindex re-test next.**
Occupancy path (D-23–D-28) CLOSED. Keyword Find frozen. Live/Dual **N = 0**.
Fuse is on the product path. USE gold@60 = 0 (median 352). BGE-small probe
card: gold@60 = 11 (median 8). Product meaning pack is BGE-small ONNX;
USE vectors are purged on install; rebuild meaning index required.
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

The product bar is **correct and most relevant** for any natural wording
against the whole library — not a table of specimen phrases. Device cues
only measure. A new phrasing is a model/stack defect, not a new occupancy
cell (`HUMAN_RECALL_ASK_MODEL.md`).

1. **Roles** — wrappers off; **head** / **qualifier** / **list** / constraint.
   Many wordings of one job share one head. A trailing `for` is one
   constraint pattern when content remains on both sides. A qualifier may
   promote a file that has it. It must not open a constraint-only Find.
2. **Two generators, one boundary** — lexical (`SearchMemoryEvidence`) +
   meaning (`SearchAssetMemoriesByMeaning`). Fuse inside
   `CanonicalRecall.searchByMeaning`. Keyword UI is unchanged.
3. **One score** — with a constraint, the topic (first job word) plus the
   constraint lead; leftover job words cannot take the first seats. Without
   a constraint, more head overlap then family fair share (list cues). Then
   fused rank; then cosine / rerank. Self-captures never count as overlap.
4. **Embedder is a pack** — `EmbeddingEngine` stays. USE is the current
   file, not a religion. Bake-off vs BGE-small or E5-small on founder gold
   cues; swap only if measured. Reindex once, disclosed.
5. **Rerank** — ADR-051 Stage A on the fused top ~40 when the pack is
   installed; identity fallback otherwise.
6. **Filters** — time/type only when we can honor them. Otherwise ignore.

Adding a word may help a file that has that word, or leave the old page.
It must not invent a parallel product. Slice 1 is job-vs-constraint, not
yet paraphrase or fused lexical+meaning relevance (slices 2–5).

## Slices (implement in order; one verified checkpoint each)

| Slice | Work | Keyword Find |
|---|---|---|
| **1** | Query roles + one monotonic score on the meaning page (**landed**; head-first correction 2026-09-20) | Untouched |
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
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: occupancy case table (slice 1
  replaced it with one score; slice 2 fused the generators); USE as the
  default pack if a challenger wins gold@60 (slice 4)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: no
```

## Pre-work record

- **Requirement IDs:** Ask Model I1/I2; Local AI Spec §4 EmbeddingEngine +
  RecallRanker; ADR-049; ADR-051; FD-01; founder 2026-09-19 reliability ask.
- **Holistic scenarios:** same job many phrasings; job + constraint; list
  asks; Exact named thing; self-captures last; keyword screens unchanged;
  empty/offline/pack-missing stay honest. Named device phrases are
  specimens of those classes, not the product.
- **Acceptance (plan):** this record + ADR-055 + CONTINUE current
  checkpoint point here; occupancy “next ticket” language removed.
- **Acceptance (slice 1):**
  - [x] `for grade 2` keeps head `swimming`+`classes`; `looking for silky`
        is not a qualifier split
  - [x] Qualifier-only files do not lead when a head hit exists
  - [x] Exact head still leads; wifi / silky same-tier fair share kept
  - [x] D-10: embed text still equals all content tokens
  - [x] Head overlap outranks a leftover job word plus a constraint
        (vocabulary-free; device 2026-09-20 was one specimen)
  - [x] Founder re-test on the **debug** install (2026-09-21): constrained
        job + Exact / one-word hold. Specimens only.
- **Acceptance (slice 2):**
  - [x] A lexical-only Memory that has the topic (+ constraint) can appear
        on Meaning Find even when cosine never admitted it
  - [x] Leftover job words do not AND-veto the lexical probe
  - [x] List cues still AND every named word
  - [x] Engine-unavailable stays Engine-unavailable (no silent keyword page)
  - [x] Keyword Find screens and `CanonicalRecall.invoke` unchanged
  - [x] Slice 1 role score still ranks the fused list
  - [x] Founder USB re-test 2026-09-21; no `connectedDebugAndroidTest`
- **Acceptance (slice 3):**
  - [x] USE baseline scorecard (gold@1 / @10 / @60 + median) from the
        existing debug probe; no excerpts
  - [x] Challenger pack installed and scored on the same cue set
        (device 2026-09-21: gold@1=1 gold@10=6 gold@60=11 median=8;
        `verdict=challenger_wins`)
  - [x] Win bar recorded before any swap (slice 4): gold@60 strictly
        greater on the same cue count; gold@1/@10 must not regress;
        median-only is not a win; unmatched gold is locator/index
  - [x] Keyword Find and product ranking unchanged this slice
- **Acceptance (slice 4):**
  - [x] Product meaning pack defaults to the winning BGE-small ONNX
  - [x] Disclosed rebuild of the meaning index (USE vectors not mixed)
  - [x] Keyword Find screens and ranking laws unchanged
  - [x] Engine-unavailable / pack-missing stay honest
  - [ ] Founder USB re-test after reindex; no `connectedDebugAndroidTest`

## Delivery record (2026-09-21) — slice 4 BGE product swap (in tree)

- **Why.** Slice 3 `challenger_wins` (BGE gold@60=11 vs USE 0). Product
  still ran MediaPipe USE until this disclosed swap.
- **Law.** Product `EmbeddingEngine` is `OnnxBgeSmallEmbeddingEngine`
  (cached ONNX session). Find cues use `embedQuery` + BGE query prefix;
  Memory / evidence text uses `embedText` without a prefix. Product
  download/store is sha256-pinned BGE. Successful install purges USE /
  average-word Room vectors and deletes legacy MediaPipe files. About
  disclosure names BGE and requires rebuild. Keyword Find and ranking
  laws unchanged. Pack-missing stays Unavailable.
- **Holistic scenarios:** USE still on phone → download shows until BGE
  installed; install clears USE vectors so Find cannot mix packs; no BGE
  file → honest Unavailable; rebuild pending keyed by BGE model identity;
  keyword screens untouched.
- **Files/layers:** `OnnxBgeSmallEmbeddingEngine`; product store /
  downloader; `PurgeRetiredMeaningEmbeddings`; `EmbeddingEngine.embedQuery`;
  `SearchAssetMemoriesByMeaning` / probe query path; About disclosure.
  Keyword Find untouched.
- **Automated verification:** `PurgeRetiredMeaningEmbeddingsTest`,
  `OnnxBgeSmallEnV15SpecTest`, `AiPackDisclosureCopyTest`, plus existing
  meaning unit suites after `deleteForModel` stubs.
- **Emulator/manual:** founder USB — disclose / download BGE / Build
  meaning index / Find by meaning. No `connectedDebugAndroidTest`.
- **Git commit:** local when asked; do not push.

## Delivery record (2026-09-21) — slice 3 BGE device card (held)

- **Why.** Founder ran the probe-only BGE bake-off on the live library.
- **Card.** `pack=onnx-bge-small-en-v1.5 cues=17 matched=12 gold@1=1
  gold@10=6 gold@60=11 medianAssetRank=8` vs USE gold@60=0.
  `verdict=challenger_wins`. Same five unmatched exact labels as USE
  (locator/index). Probe top-5 often includes UNFYND screenshots —
  product demotion stays on Find.
- **Law.** Frozen in `MeaningEncoderBgeChallengerBaseline`. Does not swap
  the product pack by itself. Slice 4 is the disclosed swap + reindex.
- **Emulator/manual:** device card recorded. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-21) — slice 3 BGE challenger pack (probe-only)

- **Why.** USE gold@60=0. A second pack must be measured on the same cue
  set before any product swap. Vectors are incompatible with USE Room.
- **Law.** Debug About downloads Xenova BGE-small quantized ONNX (~34 MiB,
  sha256 pinned). Probe re-embeds the USE-indexed corpus in memory with
  one ONNX session, scores the 17 cues, writes bakeoff + verdict. Product
  `EmbeddingEngine` stays MediaPipe USE. Find / keyword untouched. No Room
  challenger index. Swap remains slice 4.
- **Holistic scenarios:** pack missing → honest; USE index empty → honest;
  download integrity fail → no install; long run with progress; clear
  derived data removes challenger bytes; meaning Find unchanged.
- **Files/layers:** `OnnxBgeSmallEnV15Spec`;
  `NoBackupMeaningEncoderChallengerStore`;
  `HttpMeaningEncoderChallengerDownloader`;
  `ProbeMeaningEncoderChallenger`; About debug CTAs. Keyword Find untouched.
- **Automated verification:** `DownloadMeaningEncoderChallengerPackTest`,
  `OnnxBiEncoderRuntimeTest`, `BertWordPieceTokenizerTest` (encodeSingle),
  bake-off scorecard/verdict + `AiPackDisclosureCopyTest` green.
- **Emulator/manual:** founder USB — Download BGE challenger, Run probe,
  paste Logcat. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-21) — slice 3 win bar (frozen)

- **Why.** Founder probe: gold@1/@10/@60 = 0, median 352, cosine30=false
  on every cue. Installing a second pack before a swap law would let a
  closer median look like a win while gold still sits outside the product
  pool of 60.
- **Law.** `MeaningEncoderBakeOffVerdict`: gold@60 must be strictly
  greater on the same cue count; gold@1/@10 must not regress; dropping
  matched gold does not win; median-only does not win; unmatched labels
  are locator/index. Product Find / keyword untouched. Swap remains
  slice 4.
- **Holistic scenarios:** any natural cue (specimens measure); paraphrase
  the lexical generator cannot seat; exact unmatched labels stay out of
  the encoder card; challenger missing → no swap; better median at rank
  61+ → no swap.
- **Files/layers:** `MeaningEncoderUseBaseline`;
  `MeaningEncoderBakeOffVerdict`. Keyword Find untouched.
- **Automated verification:** `MeaningEncoderBakeOffVerdictTest`,
  `MeaningEncoderBakeOffScorecardTest`, `AiPackDisclosureCopyTest` green.
- **Emulator/manual:** no USB needed this increment (measurement law).
  No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-21) — slice 3 USE scorecard (started)

- **Why.** Slice 2 held. Slice 3 is measure, not swap. A second pack is
  not on the phone. Land the baseline card so a later challenger is
  compared on the same cues, not a new occupancy table.
- **Law.** Read-only probe. `MeaningEncoderBakeOffScorecard` over collapsed
  asset ranks. Challenger = `not_installed`. Product Find / keyword
  untouched. Swap remains slice 4.
- **Files/layers:** `MeaningEncoderBakeOffScorecard`; About probe copy +
  Logcat `bakeoff` line. Keyword Find untouched.
- **Automated verification:** `MeaningEncoderBakeOffScorecardTest`,
  `ProbeMeaningEncoderRanksTest`, `AiPackDisclosureCopyTest` green.
- **Emulator/manual:** About → Run encoder probe; paste `bakeoff` line.
  No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-21) — slice 2 fuse (held)

- **Why.** Slice 1 held on device. Cosine can still omit a Memory that
  keyword already finds. Occupancy was compensating for that hole.
- **Law.** `searchByMeaning` reads `SearchMemoryEvidence` (keyword screens
  frozen). Job / ask-shape probes topic + constraint; list cues AND every
  named word. Lexical-only cosine is the meaning floor so it cannot set
  the trusted band. Role score still ranks. Engine-unavailable is not
  replaced by a keyword page.
- **Files/layers:** `MeaningLexicalProbeQuery`; `MeaningCandidateFusion`;
  `CanonicalRecall.searchByMeaning`. Keyword `invoke` untouched.
- **Automated verification:** `:app:testDebugUnitTest` for
  `MeaningLexicalProbeQueryTest`, `MeaningCandidateFusionTest`,
  `CanonicalRecallMeaningTest`, `CanonicalRecallTest` — BUILD SUCCESSFUL
  (2026-09-21).
- **Emulator/manual:** founder USB re-test of a constrained job, a list
  cue, and one Exact hold. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-21) — slice 1 constraint number + self-capture

- **Why.** Valid debug: a constrained job led with a looser same-topic
  file and UNFYND screenshots of Find. Device phrasing is a specimen.
  The laws were: a named number beside other content was dropped; leftover
  extra heads outranked a tighter constraint; Android `Screenshot_*_UNFYND`
  OCR of only the document skipped D-14.
- **Law.** Keep a digit when non-numeric content remains; quantity-only
  digits still drop. TIME years stay D-10 consumed. Inside a topic band,
  qualifier depth first. Android app-screenshot filenames demote without
  chrome phrases. Not fusion. Not a synonym. Not occupancy.
- **Files/layers:** `RecallQueryContentTokens`; `EnglishRecallInflection`;
  `MeaningNamedWordDepthPage.orderConstrainedTier`; `UnfyndSelfCapture`.
  Keyword Find untouched.
- **Automated verification:** `:app:testDebugUnitTest` meaning role /
  page / cue / self-capture suite — BUILD SUCCESSFUL (2026-09-21).
- **Emulator/manual:** founder USB re-test of any constrained job + one
  Exact hold. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-20) — slice 1 correction (head-first)

- **Why.** Device: `when are the swimming classes for grade 2` first
  screen was mostly school grade docs with no swimming. Slice 1 ranked
  qualifier before head, so `classes`+`grade` beat `swimming`. Pool
  admission still used raw named-token count (D-15), so the 60 filled
  with those school docs.
- **Law.** Same [MeaningRoleScorer] for pool + eligibility + page: more
  head overlap first; same head depth fair-shares families; qualifier
  ranks inside a family only. Vocabulary-free. Specimens measure; they
  are not a synonym table. Device 2026-09-20 later: longer constrained
  cues put leftover-word files in the first seats; topic + constraint
  now lead. Not fusion (slice 2).
- **Files/layers:** `MeaningRoleScorer` / `MeaningRoleAdmission`;
  `SearchAssetMemoriesByMeaning.selectCandidatePool`;
  `MeaningNamedWordDepthPage`; `MeaningTrustedHitPolicy.eligibleHits`.
  Keyword Find untouched.
- **Automated verification:** `:app:testDebugUnitTest` for
  `MeaningRoleAdmissionTest`, `MeaningNamedWordDepthPageTest`,
  `MeaningTrustedHitPolicyTest`, `SearchAssetMemoriesByMeaningTest`,
  `CanonicalRecallMeaningTest`, `MeaningRecallRolesTest`,
  `MeaningRecallCueTest`, `AnchorAwareMeaningRecallRankingTest` —
  BUILD SUCCESSFUL (2026-09-20 night, after Canonical Recall trim
  uses the raw typed cue).
- **Emulator/manual:** founder USB re-test of the six Meaning cues. No
  `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-19) — slice 1

- **Files/layers:** `MeaningRecallRoles`; `MeaningNamedWordDepthPage` one
  score (head / qualifier / same-tier fair share). Keyword Find untouched.
- **Automated verification:** `:app:testDebugUnitTest` for
  `MeaningRecallRolesTest`, `MeaningRecallCueTest`,
  `MeaningNamedWordDepthPageTest`, `MeaningTrustedHitPolicyTest`,
  `CanonicalRecallMeaningTest` — BUILD SUCCESSFUL (2026-09-19).
- **Emulator/manual:** `when are the swimming classes for grade 2`; confirm
  holds. No `connectedDebugAndroidTest`.
- **Git commit:** local; do not push.

## Delivery record (2026-09-19) — plan only

- **Files/layers:** ADR-055; this file; CONTINUE / CHANGELOG /
  PROGRAM_STATE / RECALL_ENFORCEMENT_INDEX / registry / AGENTS pointer.
  Plan commit landed before slice 1.
