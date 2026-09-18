# Change control — Meaning ranking lexical veto (D-20)

**Date:** 2026-09-18
**Type:** Decision plan + authorized slices
**Status:** **Probe delivered** (founder A15). **Phase 1 Exact list-replace
delivered.** **Phase 1b mixed trusted seating delivered.** **Phase 1c
modifier-first Partial seats delivered.** **Phase 1d document seats
delivered** (device: swimming screenshots filled the five; keyword Find
already has the PDFs). Token-count pool seating **kept**. Phase 0 live
trace not delivered.
**Ask Model:** **P-AND vs P-LIST**; **P-EVIDENCE**; **P-MEANING-ONLY** (honesty
banners may remain; they must not delete neighbours)
**Does not authorize:** marketing AVAILABLE, synonym nets, FTS5, encoder swap,
hybrid/RRF product wire, ANN, one-box Find, keyword Find changes, schema
migration, `fallbackToDestructiveMigration`, cosine-only pool seating

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
2. **`applyLexicalPrecisionTier` (Phase 1 — delivered):** if any remaining hit
   was Exact, that set **used to replace the list**. That gate is gone: Exact
   and Partial sit together; MeaningOnly still only runs when deepest is 0.
   Combined with `MeaningEvidenceTokenBoost` (`+0.35`) and
   `MeaningTrustedHitPolicy` (0.22 of top **boosted** score, max 5), a distant
   neighbour still missed the shown list. **Phase 1b:** when Exact and Partial
   share a list, reserve up to two Partial seats and band Exact among Exact
   only. One-word exact-token cues (`Aadhaar`) still use the score band.

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
| **Phase 1** | Stop Exact from **deleting** Partial neighbours. Coverage remains a boost and a banner, not a subset when any Exact hit exists. `apply_keeps_a_partial_hit_when_an_exact_hit_exists`. Survival test must pass **after** trusted trim. Mixed banner must not claim nothing saved says a word an Exact hit has. **Keep** D-15 token-count pool seating — probe showed cosine-30 would drop the timetable. | Cosine-only `selectCandidatePool`; IDF, `RankFeatures`, FTS, encoder, hybrid, ANN, schema, keyword ViewModels, `SearchMemoryEvidence`, raising the pool, retuning `+0.35` / `0.22` unless the survival test cannot pass (then band on **raw cosine** only) |

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
ARCHITECTURAL BOUNDARY: Canonical Recall meaning ranking (lexical precision
  tier). Keyword candidate gen unchanged. Pool seating unchanged (D-15).
CURRENT LEGACY PATH (L# or none): none — Live/Dual N = 0
TARGET PATH: SearchAssetMemoriesByMeaning (full-index cosine, token-count
  pool seating) → AnchorAwareMeaningRecallRanking (Exact no longer replaces
  the list; Partial-only still uses deepest shared set; MeaningOnly only
  when deepest==0) → MeaningTrustedHitPolicy → Meaning Find UI (honest mixed
  banner)
WHY THIS CONVERGES: removes a ranking veto inside the sole Find boundary;
  no new generator, no new product Find, no Live/Dual growth
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
- **Smallest safe change (this commit):** stop Exact list-replace; keep
  token-count seating; honest mixed banner; survival after trusted trim.
- **Acceptance criteria (this docs slice):**
  - [x] Plan recorded; keyword Find named frozen
  - [x] Probe / Phase 0 / Phase 1 sequenced; encoder and FTS not authorized
  - [x] Probe delivered (code + founder A15 ranks)
  - [ ] Phase 0 delivered (later ticket)
  - [x] Phase 1 Exact list-replace delivered (this ticket)
  - [ ] Device re-test on paraphrase **and** exact-token cues after Phase 1
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
  - Edge: pool size ≤ 30 → `selectCandidatePool` is already a no-op. Cosine
    seating is **not** this slice: founder probe showed it would drop gold.
  - Edge: trusted band can re-drop a neighbour after the tier is gone;
    survival test is after trim (`+0.35` / `0.22` unchanged unless that test
    cannot pass)

## Alternatives considered

- **Claude 7-phase spec as the build ticket** — rejected for this week.
  Destination (no lexical veto, keep evidence identity, measure, scale later)
  is useful; FTS5 + feature ranker + encoder as one implementation is not.
- **Phase 1 as RankFeatures + IDF + 0.30 abs floor + pool 200** — rejected.
  `retrievalScore` does not exist until fusion; 0.30 is uncalibrated; IDF
  needs corpus DF.
- **Skip the probe** — rejected. One on-device rank of gold chunks answers
  Phase 1 vs encoder before writing ranking code.
- **Raise pool to 200 instead of cosine seating** — rejected as the *fix* in
  the original plan. After the probe, cosine seating itself is rejected for
  this library: gold was in token-30 and out of cosine-30.
- **Seat Phase 1 by cosine (original plan)** — rejected after founder probe
  (`swimming schedule` assetRank=380, token30=true, cosine30=false). Keep
  D-15 token-count seating.
- **In-memory BM25 on dense top-K this week** — deferred. Right way to
  *earn* hybrid without SQLCipher FTS; not before measure.
- **Change keyword AND to OR** — forbidden. Literal Find is a different job.

## Delivery record — probe (2026-09-18)

- **Files/layers:** `ProbeMeaningEncoderRanks` (read-only; no MIG-05 cutover);
  `MeaningEncoderProbeCues`; debug-only button on About on-device meaning
  search (`BuildConfig.DEBUG`); Logcat tag `MeaningEncoderProbe`. Does **not**
  change `searchByMeaning`, keyword Find, or schema.
- **Automated verification:** `:app:testDebugUnitTest` for
  `ProbeMeaningEncoderRanksTest` + copy tests (run with this slice).
- **Emulator/manual:** founder A15 — About → Run encoder probe; filter
  Logcat `MeaningEncoderProbe`. Do **not** run `connectedDebugAndroidTest`
  that clears the live DB.
- **Device result:** scanned=3030 (1603 summaries + 1427 evidence);
  floor/collapse=1603 assets. `swimming schedule` gold assetRank=380,
  token30=true, cosine30=false. `kids water lessons` rank 841, in neither
  pool. Top cosine hits were UNFYND screenshots, logos, photos.
- **Known limitation:** gold is a filename/key substring (`timetable`,
  `passport`, …). If the real file uses another name, `matched=false` and
  `top=` still shows what USE retrieved.
- **Git commit:** local when asked; do not push.

## Delivery record — Phase 1 Exact list-replace (2026-09-18)

- **Files/layers:** `AnchorAwareMeaningRecallRanking.applyLexicalPrecisionTier`
  (Exact + Partial together; Partial-only deepest set unchanged; MeaningOnly
  only when deepest==0); `refineAfterTrustedTrim` after
  `MeaningTrustedHitPolicy`; `RecallPrecision.Partial.exactHitsPresent`;
  `MeaningSearchCopy.partialMatchBody` mixed banner. `selectCandidatePool`
  **unchanged**. Keyword Find **unchanged**.
- **Automated verification:**
  `AnchorAwareMeaningRecallRankingTest.apply_keeps_a_partial_hit_when_an_exact_hit_exists`;
  `apply_keeps_a_partial_scan_hit_beside_an_exact_scan_and_silky_hit`;
  `apply_still_drops_a_zero_overlap_neighbour_when_an_exact_hit_exists`;
  `apply_prefers_the_tier_that_matches_more_of_the_named_words` (unchanged
  contract); `CanonicalRecallMeaningTest.searchByMeaning_keeps_a_timetable_neighbour_after_trusted_trim_when_an_exact_hit_exists`;
  `MeaningSearchCopyTest.mixed_exact_and_partial_copy_does_not_claim_nothing_saved_says_a_present_word`.
- **Emulator/manual:** founder re-test `swimming schedule` vs timetable PDF;
  also `Aadhaar` / `passport` so exact-token recall does not regress. Do
  **not** run `connectedDebugAndroidTest` that clears the live DB.
- **Known limitation:** `kids water lessons` is still an encoder problem
  (rank 841). Trusted 0.22 on boosted score hid a distant neighbour on
  device — Phase 1b.
- **Git commit:** `12076e5`.

## Delivery record — Phase 1b mixed trusted seating (2026-09-18)

- **Files/layers:** `MeaningTrustedHitPolicy.apply(hits, limit, rawQuery)`
  reserves up to two Partial seats when Exact competitors exist; Exact still
  uses 0.22 among Exact only. `CanonicalRecall.trimMeaningMatches` passes the
  query. One-word cues unchanged. Keyword Find unchanged.
- **Automated verification:**
  `MeaningTrustedHitPolicyTest.mixed_query_keeps_a_distant_partial_when_exact_hits_fill_the_band`;
  `one_word_query_still_uses_the_score_band`;
  `CanonicalRecallMeaningTest.searchByMeaning_keeps_a_distant_timetable_when_exact_hits_fill_the_trusted_band`.
- **Emulator/manual:** founder re-test `swimming schedule`; confirm
  `Aadhaar` / `passport` still look like themselves. Do **not** run
  `connectedDebugAndroidTest` that clears the live DB.
- **Known limitation:** reserved Partial seats were score-only. Device then
  showed a `schedule`-only JPG first. Keyword `swimming timetable` returns
  four correct PDFs. Phase 1c.
- **Git commit:** `79d9613`.

## Delivery record — Phase 1c modifier-first Partial seats (2026-09-18)

- **Files/layers:** `MeaningTrustedHitPolicy.orderPartialsByModifierThenScore`
  — English compound: last content token is the head; earlier tokens are
  modifiers. Reserved Partial seats take modifier matches first. Head-only
  still used if no modifier Partial exists. Not a synonym net. Keyword Find
  unchanged.
- **Automated verification:**
  `mixed_query_prefers_modifier_partials_over_a_head_only_image`;
  `mixed_query_keeps_a_head_only_partial_when_no_modifier_partial_exists`.
- **Emulator/manual:** founder `swimming schedule` again. The
  `schedule`-only JPG should not take a reserved seat if any `swimming`
  Partial exists. The timetable PDF may still lose to other swimming
  screenshots on cosine. Do **not** run `connectedDebugAndroidTest`.
- **Known limitation:** does not rank among swimming Partials; USE still
  preferred screenshots over the PDF. Phase 1d.
- **Git commit:** `bc2c7b3`.

## Delivery record — Phase 1d document seats (2026-09-18)

- **Files/layers:** `MeaningTrustedHitPolicy.ensureDocumentSeats` swaps up to
  two PHOTO/SCREENSHOT seats for PDF/NOTE hits still in the ranked pool.
  Does not expand the 0.22 band. Applies after mixed/Exact seating, so
  `swimming timetable` (Exact-only) is covered. Keyword Find unchanged.
- **Automated verification:**
  `exact_query_swaps_image_seats_for_a_pdf_still_in_the_pool`;
  `mixed_query_swaps_an_image_seat_for_the_timetable_pdf`.
- **Emulator/manual:** founder meaning `swimming schedule` and `swimming
  timetable`. Expect a PDF among the five if it was in the ranked 30.
  Keyword Find already returns those PDFs. Do **not** run
  `connectedDebugAndroidTest`.
- **Known limitation:** if token-count pool 30 contains no PDF, there is
  nothing to swap. Encoder still later.
- **Git commit:** local; do not push.

## Delivery record (plan docs, 2026-09-18)

- **Files/layers:** this change control; `CONTINUE.md`; `PROGRAM_STATE`
  D-20; `CHANGELOG`; scenario-bar honesty row.
- **Automated verification:** docs only.
- **Emulator/manual:** n/a that slice.
- **Known limitation:** live meaning path still has both vetoes until Phase 1.
  USE may still fail pure paraphrase; the probe decides.
- **Git commit:** `661e919`.
