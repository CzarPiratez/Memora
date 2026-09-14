# Change Log

## Unreleased

### Open the whole original in another app (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Open showed the page that justified the hit. That is the
  right evidence, but it is not the file — people want to scroll their PDF in
  their reader, use it normally, and come back.
- **Fixed.** **Open full file** on the PDF / photo / screenshot preview starts
  `ACTION_VIEW` with a read-only grant of the same stored URI Share hands out
  (ADR-054, extending ADR-053). No chooser is forced, so the person's default
  reader opens. The viewer starts inside UNFYND's task, so Back returns to the
  preview. The in-app cited-page preview is unchanged and still the default.
- **Honest failure.** "No app on this phone opens this kind of file yet" is a
  separate message from source-unreachable and could-not-open, because the
  person can fix it and their file is not at fault.
- **Not Act.** Read grant only, one intent, no `ACTION_EDIT`, no write grant,
  no copy into UNFYND storage, no upload. Notes have no local file and still
  open in OneNote. Not AVAILABLE.
- **Naming.** Share and Open need the same resolved URI, so the resolver is
  now `application/handoff/PrepareOriginalHandoff` rather than a class called
  `PrepareShareOriginal`. Rename and package move only; share behaviour
  unchanged.
- **Verification:** `OpenOriginalInAnotherAppTest`, `PrepareOriginalHandoffTest`,
  `OriginalHandoffCopyTest`. `:app:testDebugUnitTest` **828 tests, 0 failures**;
  `:app:assembleDebug` succeeded. Device gate open.

### OneNote opens the app, not the browser (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Open original note always landed in the browser on the
  founder's phone even with OneNote installed. The launcher asked
  `resolveActivity` whether anything could handle the `onenote:` link, and
  Android 11+ package-visibility filtering answers null for another app's
  scheme unless the caller declares it in `<queries>`. The app declares none,
  so the deep link was skipped on every modern device.
- **Fixed.** `OneNoteOpenTargetPolicy` puts the OneNote app link first and the
  launcher attempts it rather than querying for it. Package visibility does
  not restrict starting an implicit intent, so an installed OneNote opens; a
  phone without it throws, is caught, and the web URL runs as before.
  `CATEGORY_BROWSABLE` is now added only to `http`/`https`.
- **Unchanged.** Graph `links` only, never `contentUrl` or `Asset.location`.
  Search stays offline.
- **Verification:** `OneNoteOpenTargetPolicyTest`. `:app:testDebugUnitTest`
  **822 tests, 0 failures**. Device gate open — the emulator has no OneNote.

### Zoom hardening — no recycled bitmap, no OOM crash, honest failure (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Review of the zoom slice found three real defects. The
  preview recycled its bitmap when a sharper read replaced it, which Compose
  can still be drawing; a 2048 px decode can raise `OutOfMemoryError`, which
  is an `Error` and was caught nowhere in the app; and a failed sharper read
  cleared the spinner without telling anyone.
- **Fixed.** The preview never recycles — it holds an `ImageBitmap` built off
  the composition and lets the last one be collected. `ReloadOriginalPreview`
  treats `OutOfMemoryError` as Unavailable, so a phone that cannot spare the
  memory keeps the preview it already has. A failed sharper read now says so
  in a polite live region and states the picture has not changed.
- **No behavior added.** Same zoom, same decode budget, same Open path.
- **Verification:** `ReloadOriginalPreviewTest` out-of-memory case and
  `OriginalPreviewZoomCopyTest` honesty case. `:app:testDebugUnitTest`
  **818 tests, 0 failures**.

### Open-original share sheet — stored URI, not Act (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Open could show the original but could not hand it to
  another app the person already uses. Stretching “Act” to mean that tap
  would have been a constitution lie; skipping share would have been a
  demo lie.
- **Fixed.** Share on the PDF / photo / screenshot preview opens the
  Android chooser with a **read-only** grant of the stored content URI
  (ADR-053). PDFs use the canonical tree-document URI Open would read.
  Photos/screenshots walk Open’s URI candidates. Notes still open in
  OneNote. Ranking unchanged.
- **Not Act.** No reminders, no mutation, no upload, no FileProvider cache
  of user pixels. Not AVAILABLE.
- **Verification:** prepare-share and copy unit tests.
  `:app:testDebugUnitTest` **816 tests, 0 failures**.

### Open-original pinch-zoom — re-render from the file (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Open showed a 960 px (photo/screenshot) or 1440 px
  (PDF) buffer. Pinch would only have stretched those pixels, so a cited
  PDF line went blurry the moment someone tried to read it.
- **Fixed.** One preview chrome for PDF, photo, and screenshot (keyword and
  meaning). Pinch, double-tap, and Zoom in / Zoom out / Fit. After ~1.2×,
  UNFYND re-opens the original on this phone at a stepped edge up to
  2048 px. Failed re-render keeps the last image. Notes still open in
  OneNote. Ranking unchanged.
- **Not share, not a PDF reader.** No annotations, no page swipe, no Act.
  Not AVAILABLE.
- **Verification:** zoom policy, reload mapping, copy, sample-size, and
  Open identity unit tests. `:app:testDebugUnitTest` **809 tests, 0 failures**.

### Find result thumbnails — matched page, memory-only (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Find cards were filename + excerpt + Why + Open. On a
  phone that is a recognition defect: the person cannot see the photo or the
  PDF page that matched until they tap Open.
- **Fixed.** One 128 px read-only thumbnail on every product Find card.
  PDFs render the cited/matched page. Photos/screenshots use
  `loadThumbnail` (API 29+) or sampled decode. Notes show an honest glyph.
  Process-lifetime LRU only; clear-index evicts it. Ranking unchanged.
- **Not zoom, not share.** Open still uses the 960 px buffer. Not AVAILABLE.
- **Verification:** policy, cache, loader, scale, and copy unit tests.
  `:app:testDebugUnitTest` **800 tests, 0 failures**.

### I3 — meaning-index Build runs in WorkManager with Stop (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** `RunPendingMeaningIndex` already returned `hasMore`
  after I1/I1b, but About still ran one 25-memory batch on the ViewModel
  with no Stop and no auto-continue. A from-scratch rebuild before the
  investor demo would have been dozens of taps.
- **Fixed.** One global unique work (`meaning-index-drain`) calls only that
  use case, looping batches until empty or a 4-minute wall-clock budget,
  then re-enqueues. `EngineUnavailable` / `SelectionDisagreed` stop and
  report; they never Continue or retry. About shows progress and Stop.
  Cancelled / stopped work is not Failed. Clear-index cancels the tag.
- **Not I4.** The 4-minute budget is a safety cap under WorkManager’s
  ~10 minute ceiling, not a measured per-item SLA. Count cap stays 25.
  Not AVAILABLE. Find quality unchanged.
- **Verification:** drain budget/session, decision mapper, work observation,
  and copy unit tests. `:app:testDebugUnitTest` **786 tests, 0 failures**.

### Uniform Why — one dialect, one panel (2026-09-14)

- **Date:** 2026-09-14
- **Why it existed.** Keyword Why repeated the query, type, filename, page,
  and the excerpt the card already highlighted. Meaning Why was a different
  sentence shape, sometimes with a different visual treatment. On a phone
  that is a trust defect: the same question produced four different answers.
- **Fixed.** One `WhyPresentation` (relevance, optional cited line,
  how-found) assembled by `CanonicalRecallWhyCopy`, rendered by one
  `WhyDisclosure` on every Find card. Keyword leaves the cited line empty
  (U5 / D-17). Meaning quotes the stored justifying span. Path labelled in
  consumer words (ADR-024 / F-03). Contract §3.1 / §3.4 updated.
- **Not I3, not thumbnails, not zoom, not share.** Find quality unchanged.
- **Verification:** `WhyPresentationShapeContractTest` (every `AssetType` ×
  both paths); `CanonicalRecallWhyCopyTest`; per-screen copy tests updated.
  `:app:testDebugUnitTest` **761 tests, 0 failures**.

### Class A pack revision 2026-09-07 — public Core repo hygiene

- **Date:** 2026-09-07
- **Why.** Public `unfynd-core` was in sync with the 2026-09-05 pack, but the
  pack still said "Core provides" the runtime, the validator ignored its own
  schema, and the published repo had no CI.
- **Fixed.** Honesty copy; schema-driven validator (`x-unfyndSchemaVersion`
  1.1.0); four negative fixtures; public Actions workflow; CoC, versioning,
  issue/PR templates; Canonical Recall sketch. `CITATIONS.md` stays a
  maintainer map.
- **Not Class B.** No App source. No AVAILABLE.
- **Verification:** validator `--self-test`. Pack-only publish **`615629b`**,
  tag **`v0.1.0`**. Public CI `Class A validator` succeeded.

### I2 — Asset Memory assembly runs in WorkManager with Stop (2026-09-07)

- **Date:** 2026-09-07
- **Why it existed.** `RunPendingAssetMemoryAssembly` already returned `hasMore`
  after D-9, but Welcome still ran one 25-memory batch on the ViewModel with
  no Stop and no auto-continue. Discovery, OCR, PDF, and OneNote already use
  the worker trio.
- **Fixed.** One global unique work (`asset-memory-assembly-drain`) calls only
  that use case. `Completed(hasMore)` continues; `FailedSafely` retries and
  never continues (D-9). The Welcome card shows progress and Stop. Cancelled
  work is Ready, not Failed. Clear-index cancels the tag.
- **Not I3/I4.** Meaning-index still hand-cranks. Batch cap stays 25.
- **Verification:** decision mapper, work observation, ViewModel, and copy
  unit tests. `:app:testDebugUnitTest` **756 tests, 0 failures**. A15
  Welcome → Build: worker SUCCESS in ~40 ms, `reschedule = false`, card
  still 1001 READY, no crash. Empty assembly queue — Stop not visible.

### I1b — leftover evidence stays in the meaning-index Build queue (2026-09-07)

- **Date:** 2026-09-07
- **Why it existed.** After I1 on the A15, 1001 summaries were indexed and
  Build said the queue was empty while only 467 evidence vectors existed.
  Pending/select only saw missing summaries or `STALE_REINDEX_REQUIRED`.
  MIG-05 cutover marks STALE only for PDF pages with **zero** evidence
  vectors, and restores READY on the first one. OCR, notes, and remaining
  PDF pages never came back.
- **Fixed.** The shared pending/select WHERE now includes READY memories
  that still have embeddable PDF / OCR / note evidence without a vector.
  Count and select stay in agreement. No mass-STALE. No second indexer.
- **Not I2/I3.** Workers stay off until this cursor is proven on device.
- **Verification:** instrumented selection test on emulator PASS. A15
  re-tap: pending still 0, 467 unchanged — this library has no remaining
  embeddable evidence rows without a vector.

### I1 device tap-through — empty queue no longer contradicts the corpus line (2026-09-06)

- **Date:** 2026-09-06
- **Device.** Samsung SM-A156E. Debug install of I1. Welcome → About on-device
  meaning search → Build. 1001 memories, 1001 summary vectors, 467 evidence
  vectors. No crash.
- **Found.** The summary queue was already empty, so Build correctly did
  nothing — then said “No READY memories… Build Asset Memory first” while the
  corpus line always added “Tap Build to index the next batch.”
- **Fixed.** “Tap Build…” only when `meaningIndexPending > 0`. Empty-queue
  copy says no batch is waiting, not that the library is missing. Re-tapped
  on the same phone; corpus and result agree.
- **Not fixed.** Leftover evidence is still not a selectable drain. I2/I3
  workers still open.

### I1 — meaning-index Build is an application drain, not a ViewModel script (2026-09-06)

- **Date:** 2026-09-06
- **Why it existed.** `AiPackDisclosureViewModel` selected the queue, indexed
  summaries and evidence, applied MIG-05 cutover, and guessed remaining work as
  `pendingTotal - thisBatchSize` before the batch ran. A failed summary would
  have been reported as finished. A future I3 worker would have had to copy
  or bypass the UI.
- **Fixed.** `RunPendingMeaningIndex` owns one bounded drain and returns
  `hasMore` from a **post-batch** pending count (`hasMore == remainingPending > 0`).
  Count without a selectable row is a queue mismatch, not “no memories.” Empty
  and mismatch outcomes skip cutover. The setup screen only maps the result to
  copy.
- **Not I2/I3/I4.** No WorkManager. Batch cap stays 25 memories per tap.
- **Verification:** `RunPendingMeaningIndexTest` (hasMore, disagreement, failed
  still remaining, cutover skip, PDF/OCR/note type gates);
  `AiPackDisclosureCopyTest` for honest remaining and mismatch copy.
  `:app:testDebugUnitTest` **741 tests, 0 failures**.

### D-9 — a bad asset no longer pins the memory-assembly drain (2026-09-06)

- **Date:** 2026-09-06
- **Why it existed.** `RunPendingAssetMemoryAssembly` aborted the whole batch
  on `NoUsableEvidence`, `AssetMissing`, or `RevisionConflict`. Nothing was
  persisted, so the same Asset was first on the next tap. An auto-continuing
  worker would have looped forever on one file.
- **Fixed.** Terminal per-asset outcomes are recorded in
  `memory_assembly_skips` (Room 15→16). Pending select and count exclude an
  active skip. New PDF / OCR / EXIF / note facts clear the skip so the Asset
  can assemble when the fact set changes. Only infrastructure `FailedSafely`
  still aborts the drain. The setup card names how many files were skipped.
- **Not I2/I3.** Workers stay off until this cursor is safe. Batch I may now
  proceed.
- **Verification:** `RunPendingAssetMemoryAssemblyTest` (skip-and-continue,
  second drain does not retry the skip, infrastructure abort does not skip);
  `AssemblyFactsDigestTest`; setup copy test. JVM unit tests for this slice.

### D-14 — pictures of UNFYND no longer outrank the original (2026-09-06)

- **Date:** 2026-09-06
- **Found on device.** After the human Why rewrite, the first card for
  `swimming schedule` was still `Screenshot_…_UNFYND.png`, and Why said
  "This file is What are you trying to remember? files have swimming
  timetable" — the app's own chrome, treated as the file.
- **Fixed.** `UnfyndSelfCapture` detects two stored UI phrases, or one plus
  an UNFYND filename. Those hits are demoted below originals before the
  trusted-hit trim. Why names them "a screenshot of UNFYND, not the original
  file" and does not quote the chrome. A real timetable is not affected.
- The list banner is now "Closest files mention swimming. Nothing saved
  says schedule." — same fact, not a word inventory.
- **Verification:** `UnfyndSelfCaptureTest`, ranking demote test, Why
  self-capture test. `ui.search` + ranking unit tests green.

### Why explains relevance, not a word inventory (2026-09-06)

- **Date:** 2026-09-06
- **Founder feedback.** "Has swimming. Does not have schedule." is an audit of
  the lexical gate, not an answer to "why is this file here?" The card also
  dumped type chips, OCR, page labels, and Open hints — and UNFYND screenshots
  then repeated the search chrome inside the result.
- **Fixed.** A result card is now the filename, Why / Hide why, and Open.
  Why says `You asked about a swimming schedule. This file is Grade 2 Swimming
  TT 2026.` plus the stored line that supports that. It uses the file's own
  name or opening words — it never claims schedule means timetable. Missing-word
  honesty stays on the list banner, not on every card.
- Filename stays visible when Why is closed so a list of five files is still
  scannable. The search field at the top of the screen stays; that is how you
  ask, not a result.
- **Verification:** `MeaningWhyTest` pins the relevance sentence, the
  no-synonym claim, screenshot-name fallback, and wrapper stripping.
  `ui.search` unit tests green.

### D-17 — Why names the words this file has, and stops claiming all of them (2026-09-06)

- **Date:** 2026-09-06
- **Found on device.** After D-12 the list banner was honest (`Nothing saved
  has "schedule"`) and every Why contradicted it: *"Your cue words appear in
  that saved text."* That sentence fired on any token boost. It was written
  for exact-AND and was never updated when precision became a tier.
- **Also too much to read.** Why repeated the query, filename, type, page, and
  the snippet already on the card — the U5 anti-pattern. The founder asked
  whether all of that was necessary. It was not.
- **Fixed.** Why answers one question: which of this file's named words are
  present. `Has "swimming". Does not have "schedule".` Rendered as a
  primary-container panel with a colour bar, bold matched words, and
  error-coloured missing words (labels still say Has / Does not have, so
  colour is not the only signal). A cited line appears only when the matching
  word is *not* already on the card — the start of D-7, not a duplicate
  excerpt.
- **Not D-14.** Self-screenshots still rank. Not a synonym net.
- **Verification:** `MeaningWhyTest` pins exact / partial / no-duplicate-snippet
  / hidden-span citation. `CanonicalRecallWhyCopyTest` and
  `MeaningSearchCopyTest` forbid the old "cue words appear" / "You asked
  about" dialect. `:app:testDebugUnitTest` green for `ui.search`.

### D-15 — admission reserves seats by coverage depth, not only the exact AND (2026-09-06)

- **Date:** 2026-09-06
- **Why it existed.** D-11 reserved a pool seat for Memories whose stored text
  satisfied every named word. D-12 then made precision a tier, so
  `swimming schedule` is a legitimate partial answer. Admission still treated
  "no exact AND" as "no lexical claim", so the swimming-timetable PDF had to
  win a cosine seat or the new tier had nothing to show. Same reachability
  hole as D-11, one level down.
- **Fixed** by admitting on coverage depth — how many named words the Memory
  carries — using the same `PreparedCue.matchingTokens` that D-12 already
  ranks with. Deeper coverage claims seats first; remaining seats stay the
  best cosine neighbours; the pool is still returned in cosine order because
  this class generates candidates and does not rank them.
- **No new type, no second derivation.** `PreparedCue` already compiled one
  cue per token. It now reports which of those tokens matched, and
  `applyLexicalPrecisionTier` reads that instead of preparing one cue per
  word. Admission and the precision tier cannot drift apart.
- **Architecture.** Candidate admission inside Canonical Recall. No new Find
  path, no new ranker, Live/Dual **N = 0**. Zero-overlap paraphrase remains
  an honest empty (meaning-only tier, not this defect).
- **Verification:** `a_partial_literal_match_outside_the_cosine_pool_is_still_reachable`
  (one of two words, far cosine, survives a pool of 2);
  `an_exact_match_claims_a_seat_before_a_partial` (depth 2 beats depth 1 when
  seats are scarce); `matching_tokens_are_the_named_words_that_satisfy_one_at_a_time`.
  Existing D-11 / D-12 tests unchanged. Full `:app:testDebugUnitTest` green.

### D-12 — lexical precision becomes a tier, so meaning can answer a paraphrase (2026-09-06)

- **Date:** 2026-09-06
- **Found on device, after D-11 landed.** `swimming timetable` returned the right
  PDFs. `swimming schedule` returned nothing — against those same PDFs. The
  meaning model ranked them correctly and the lexical AND gate then removed every
  one, because none of them contains the literal word `schedule`.
- **Why this was the deepest defect of the session.** Embeddings were being used
  only to *order* results they were never allowed to *find*. `silky` worked
  solely because the PDF happens to say `silky`. Anyone who remembers the idea
  but not the wording — the premise of the product — got a blank screen. Meaning
  Find was keyword AND search wearing a meaning label.
- **A second trust defect, fixed with it.** The empty state read "No indexed
  Asset Memory was close enough … with the on-device meaning model", blaming the
  meaning model for a decision the keyword gate had made.
- **Fixed** by introducing `RecallPrecision` and making the gate a **tier**:
  - `Exact` — every named word present. Byte-for-byte today's behaviour; an
    exact tier always wins outright and is never diluted by partial hits.
  - `Partial(matched, missing)` — no hit carried all the words, so the list falls
    to the deepest tier available and the screen leads with what is **missing**:
    *"Nothing saved on this phone has "schedule". These match "swimming"."*
  - No named word found anywhere is still an honest empty, and `noMatchesBody`
    now names the words it looked for instead of blaming the model.
- **Not a synonym net.** UNFYND still never asserts that `schedule` means
  `timetable`; `RecallQueryContentTokens` remains explicit that `silky` ≠
  `smooth`. It reports what it matched and what it did not. A side effect worth
  keeping: this is how the product tells a person what their own corpus says.
- **Architecture.** Precision stays inside Canonical Recall; no new Find path, no
  new per-asset ranker, Live/Dual **N = 0** unchanged. Ask Model **P-AND vs
  P-LIST** and **P-ANSWER / P-EVIDENCE**.
- **Verification:** `AnchorAwareMeaningRecallRankingTest` gains four tests — the
  device case, exact-wins-outright, deepest-tier-preferred, and honest-empty.
  `MeaningSearchCopyTest` pins that the empty state names its words and blames
  nothing, and that a partial banner leads with the missing word. An outcome
  invariant makes an empty list unable to advertise a partial match. Full
  `:app:testDebugUnitTest` green.

### D-11 — cosine decided reachability, not just rank (2026-09-06)

- **Date:** 2026-09-06
- **Found on device.** `swimming timetable` and `get me some e.g.s from the pdf
  related to the training project` both answered nothing against ~1000 indexed
  memories, while single-word cues still worked.
- **Root cause.** `CanonicalRecall.searchByMeaning` asks
  `SearchAssetMemoriesByMeaning` for a pool of `min(limit * 3, 30)` candidates,
  and that pool was selected by cosine alone: `deduped.take(limit)`. Every stage
  after it — token boost, lexical precision, Stage A rerank, anchor filter,
  trusted-hit band — can only remove. So a Memory containing the exact words the
  person named was unreachable unless the embedding had already ranked it in the
  top 30. At 25 memories the pool was the whole library and this was invisible;
  at ~1000 the pool is roughly 3% of it. **The device failures were not a
  regression from the precision work — they are the corpus outgrowing the pool.**
- **Fixed** by making admission lexically aware: candidates whose stored text
  satisfies every named word claim seats first, then remaining seats keep the
  best cosine neighbours. A cue with no literal match anywhere still degrades to
  meaning rather than to empty, so candidate generation does not grow a second,
  hidden precision gate.
- **Admission only, not ranking.** The pool is handed back in cosine order.
  `SearchAssetMemoriesByMeaning` generates candidates; ranking stays inside
  Canonical Recall. A first attempt returned literal matches first and correctly
  broke `candidate_gen_ranks_by_cosine_without_token_boost`, which is the test
  that owns that contract.
- **Cost.** `EnglishRecallInflection.occursAsWholeWord` compiles a fresh `Regex`
  per variant per call, which is fine for one ranked hit and not fine across a
  corpus. `MeaningEvidenceLexicalFilter.prepare` now compiles a cue once per
  query and evaluates it many times.
- **Architecture.** No new Find path and no new product ranker: this is candidate
  admission inside the existing Canonical Recall meaning boundary
  (`unfynd-architecture-invariants` §4). Live/Dual **N = 0** unchanged.
- **Verification:** `SearchAssetMemoriesByMeaningTest` gains
  `a_literal_match_outside_the_cosine_pool_is_still_reachable` (a literal match
  with far worse cosine survives a pool of 2) and
  `a_cue_with_no_literal_match_still_offers_meaning_neighbours` (the degrade
  path). `MeaningEvidenceLexicalFilterTest` gains
  `a_prepared_cue_agrees_with_the_single_shot_check`. Full
  `:app:testDebugUnitTest` green (691 tests). Device re-test pending.

### D-10 — a time word is a constraint, not text to retrieve on (2026-09-06)

- **Date:** 2026-09-06
- **Found on device.** `recent files with silky` answered nothing while bare
  `silky` returned the right PDF. The T10 fix was working: `recent` is classified
  as an advisory TIME constraint and correctly dropped from the lexical
  requirement, so precision asked only for `silky`. Candidate generation never
  learned about that decision. `MeaningRecallCue.embedText` derived its own
  tokens straight from `RecallQueryContentTokens`, `recent` is not an ask-shape
  wrapper, and so the engine embedded **`recent silky`**. The query vector drifted
  toward recency language, the single Memory containing `silky` fell out of the
  cosine candidate pool, and the precision gate was left with nothing to keep.
- **The defect is the disagreement, not either half.** Two layers each derived
  "what the person named" independently, so the product could filter on a word it
  had never retrieved on.
- **Fixed** by making `MeaningRecallCue.contentTokens` the single derivation:
  ask-shape wrappers removed, then the words a structured constraint already owns
  removed. `embedText` builds from it and `AnchorAwareMeaningRecallRanking`
  filters on it, so the two cannot drift apart again. The now-orphaned aliases
  `MeaningEvidenceLexicalFilter.requiredContentTokens` / `evidenceSatisfies` are
  deleted; the filter keeps `satisfies(tokens, text)` and is told what to require.
- **Consequence, recorded honestly:** a cue made only of time words
  (`recent files`, `screenshots from last week`) now names no content and returns
  an honest empty instead of listing cosine neighbours of the word `recent`.
  Retrieval by TIME alone remains an unbuilt job (I5).
- **Verification:** `MeaningRecallCueTest` gains
  `embed_text_drops_the_words_a_time_constraint_owns`,
  `a_time_only_cue_names_no_content`, and an invariant test
  `embed_text_is_exactly_the_content_tokens_it_will_be_filtered_on` that fails if
  the two derivations ever separate again. `MeaningEvidenceLexicalFilterTest` now
  asserts the real production composition rather than a shortcut alias.
  `:app:testDebugUnitTest` green. Device re-test pending.

### Product surface — launcher mark and honest launch state (2026-09-06)

- **Date:** 2026-09-06
- **Launcher icon.** Replaced the generated placeholder with the UNFYND mark as
  an adaptive icon. The mark is drawn as a **vector**, not a scaled bitmap, so it
  stays sharp on every launcher density and on the Android 12+ splash screen; the
  old `mipmap-*/ic_launcher*.webp` bitmaps are removed. A `monochrome` layer is
  supplied so themed-icon launchers do not fall back to a flat silhouette, and
  `values-v31/themes.xml` points the splash at the same vector.
- **Launch state was telling the truth badly.** `DatabaseAvailabilityPhase.Checking`
  rendered `UnlockRequiredScreen`, so an already-unlocked phone was told to
  "Unlock your phone" during a check that normally lasts a few frames. The copy
  was accurate about the encrypted index but wrong about the user's situation,
  which reads as a demand for an action they have already taken.
- **Fixed** by separating the two states: `Checking` now shows a quiet
  `OpeningIndexScreen` (a progress indicator with an "Opening UNFYND" content
  description for screen readers), and `UnlockRequiredScreen` appears only for
  `WaitingForUnlock` — when the device genuinely is locked. No behaviour change
  to the deferred-open path itself.
- **Verification:** `ClearMemoraDerivedDataCopyTest` gains
  `opening_index_copy_does_not_ask_to_unlock`, asserting the new copy neither
  says "unlock" nor leaks encryption jargon, so the two states cannot quietly
  merge again. `:app:testDebugUnitTest` green; debug APK resource merge and
  install verified on Medium_Phone(AVD) API 36. Confirmed on the founder's
  physical device.

### A7 / D-8 — meaning index selects unindexed work, not the newest page (2026-09-06)

- **Date:** 2026-09-06
- **Found on device.** With 25 memories indexed and 970 pending, tapping
  **Build meaning index** reported `indexed 0, skipped 25` every time and the
  count never moved. Both components were behaving as written:
  `MemoryDao.listMeaningIndexSummaries` ordered by
  `updated_at_epoch_millis DESC LIMIT :limit` with no reference to
  `memory_embeddings`, so it re-offered the same newest page on every tap, and
  `IndexMemoryEmbeddings` correctly skipped each one as unchanged. The other 970
  memories were unreachable no matter how many times the user tapped. Because
  the same batch also drives PDF page, OCR, and note evidence indexing, OCR
  evidence for every untouched screenshot was unreachable too — which would have
  silently invalidated the P0 device checklist.
- **Fixed** by selecting against the index rather than the clock: a `LEFT JOIN`
  on `memory_embeddings` scoped to the active `ModelVersionIdentity`, admitting a
  revision when it has **no summary embedding** or is
  `STALE_REINDEX_REQUIRED`. Never-indexed rows sort first so a drain covers the
  corpus before revisiting evidence gaps.
- **The STALE arm is load-bearing, not defensive.**
  `ApplyMig05EvidenceSearchCutover` sets `STALE_REINDEX_REQUIRED` precisely when
  a summary embedding exists but evidence embeddings are still owed. A plain
  "not embedded" anti-join — the obvious fix — would have permanently excluded
  exactly those rows and deadlocked the MIG-05 evidence drain. Termination still
  holds because the cutover restores them to READY once evidence lands.
- **Count and selection can no longer disagree.**
  `countMeaningIndexPending(model)` shares the WHERE clause and replaces the
  `candidates - summaryIndexed` subtraction in `LoadCorpusCompleteness`. That
  subtraction could over- or under-state pending whenever embeddings existed for
  non-current revisions. A non-zero pending count now means a non-empty batch is
  genuinely selectable.
- **Verification:** `RoomMemoryRepositoryMeaningIndexSelectionIntegrationTest`
  — 7 tests against real SQLite covering disjoint consecutive batches, drain
  convergence, STALE staying selectable, fresh-before-evidence-gap ordering, a
  model-identity change re-owing the corpus, count/select agreement, and blank
  summaries excluded. Full `data.local` instrumented suite **38/38** on
  Medium_Phone(AVD) API 36, including `MemoraDatabaseMigrationTest` —
  queries only, no schema change. `:app:testDebugUnitTest` green.
- **Scope, stated plainly.** This makes the corpus reachable; it does **not**
  reduce the tap count. 995 memories at 25 per tap is still ~40 taps. That is a
  missing WorkManager drain driver, registered as Batch I, and it is blocked on
  **D-9** — `RunPendingAssetMemoryAssembly` currently aborts an entire drain on
  one unusable asset without advancing the cursor, which under an
  auto-continuing worker becomes a hot loop.
- **Architecture:** meaning-index construction, not a Find path. Canonical
  Recall remains the sole Find boundary; Live/Dual **N = 0** unchanged.

### A4 — doc truth drift cleared (2026-09-05)

- **Date:** 2026-09-05
- **Why:** the audit found the same fact stated three ways. A reader could open
  three governance docs and get Live/Dual **0**, **2**, or **5**; two docs said
  `RECALL_CONVERGENCE_DONE` was still open while the gate itself has read
  **COMPLETE** since 2026-08-31; `ROADMAP` still listed A-01 open after it passed
  on a physical device. Drift like this is not cosmetic — `CONTINUE.md`
  "Next eng default" item 1 was still pointing engineers at a closed program exit.
- **Fixed (all nine §4 rows):** Live/Dual corrected to **0** in `CONTINUE.md`,
  `CORE_APP_SEPARATION_PLAN.md`, and `PRODUCT_SOURCE_REGISTRY.md`; convergence
  status corrected in three `CONTINUE.md` locations; `MVP_EXIT_AUDIT.md` meaning
  plumbing row now separates the delivered **engineering slice** from
  **ADR-050 claim B (Spec-full), which stays OPEN**; `ROADMAP.md` records A-01
  closed 2026-09-02; `PHASE_A_IMPLEMENTATION_PLAN_V1.md` carries a **SUPERSEDED**
  banner; six `PRD_TRACEABILITY.md` rows refreshed.
- **Two judgement calls, recorded in `PROGRAM_STATE_AND_SEQUENCE_V1.md` §4.**
  Dated snapshots (e.g. "Status truth check 2026-08-30") were **left intact** and
  the surrounding standing claims date-stamped instead — a build diary is history,
  and rewriting it destroys the record. `PRD_TRACEABILITY` rows were set to their
  *actual* state rather than flipped to a flat "Delivered": **P-01** and **P-12**
  remain honestly partial because Ask Model NL coverage is still **FAIL**, and
  **P-10** records that understanding is deterministic only. Overclaiming here
  would have violated truth-before-intelligence to close a docs task.
- **Also corrected beyond the nine:** three further `CONTINUE.md` claims that
  contradicted the same facts — the MIG-07B docs note, "Next eng default" item 1,
  and "Shared result/Why UI contract still open" (both boxes are checked in
  `RECALL_CONVERGENCE_DONE.md`; what remains is the *optional* UI redesign, which
  convergence explicitly does not require). Leaving them would have defeated the
  purpose of the task.
- **Knowingly left for A5:** `CONTINUE.md` line ~1243 still reads "A-01 offline
  end-to-end intelligence remain open" inside the deep diary. It is undated
  history sitting 1,200 lines below the current checkpoint; fixing every such
  line is the restructure, not this task.
- **Verification:** repo-wide grep for `N=2` / `N=5` now returns only dated
  change-control records, `docs/CHANGELOG.md` history, and the §4 drift table
  itself — all correct as history. No code touched; no test run required.

### Public pack revision 2026-09-05 — scope-aware retrieval direction (2026-09-05)

- **Date:** 2026-09-05
- **Why:** `public/unfynd-core/APPLICATIONS.md` leads with health, defence, and
  government, all of which assume role-scoped access, while both the pack README
  and APPLICATIONS assigned **permissions** wholly to the builder. That division
  is not implementable: a builder handed a ranked list cannot scope it safely,
  because result counts and Why copy leak the existence of what was withheld
  (CR-04 bar). The fix moves *enforcement* into Core's stated direction and keeps
  *policy authorship* with the builder — an increase in Core's scope, not a
  caveat.
- **Delivered:** APPLICATIONS primitive 6 "Scope inside the boundary"; division
  of labour retuned in APPLICATIONS and README ("the where, and increasingly the
  who"); new README section **Where Core is on this path** (what Core carries
  today, what the current build deepens, what comes next, and why the published
  contracts deliberately run ahead of any implementation).
- **Contracts unchanged.** Evidence classes, capability seams, and frozen
  principles are identical to pack revision 2026-09-01, and the changelog entry
  says so, so anyone building against the pack knows nothing shifted underneath.
- **Recorded against CR-04** (`docs/CORE_CAPABILITY_REGISTER_V1.md`): the
  candidate-generation bar is now **public**, so a later post-filter shortcut
  would contradict a published contract rather than only an internal register.
- **No code change.** Nothing in the App or Core implementation moved; this is
  public framing plus governance. Not AVAILABLE.

### Stage 0 precision fixes — D-2 / T10 and D-4 / T11 (2026-09-04)

- **Date:** 2026-09-04
- **D-2 / T10 — a time cue no longer disables lexical precision.** The gate
  previously returned early whenever the query carried any TIME cue, so
  `recent files with silky` returned cosine neighbours containing no `silky` —
  the junk reported from device. Time words are a **constraint**, not content, so
  `RecallQueryConstraints` now also carries `timeSpanText` (the literal matched
  expression) and the ranker subtracts those words from the required content
  tokens instead of abandoning the gate. `notes in 2024` requires `notes` and
  never the literal `2024`, which stays with the TIME anchor stage.
- **D-4 / T11 — TOPIC is EXPLICIT-only.** Every query of four or more characters
  was marked TOPIC advisory with the whole raw question as the title cue, which no
  anchor can contain, so it never boosted anything while still costing a
  `findSignatureAnchors` round-trip and a re-sort on every search. A plain cue now
  skips the anchor stage; a test asserts the lookup count is zero.
- **Tests:** three T10 anti-cases (explicit, advisory, and time words not required
  in evidence) plus a T11 round-trip guard. `apply_skips_lexical_filter_for_explicit_time_queries`
  encoded the defect as intended behaviour and was **replaced**, not relaxed; the
  Stage A CE order guard was retargeted to a query that still reaches the anchor
  stage so it keeps its meaning. Fixtures for two time tests now carry the content
  word, since they previously passed only because the gate was off.
- **Cleanup:** `MeaningEvidenceLexicalFilter.shouldApply` removed — orphaned by the
  change and a second source of truth for when precision applies.
- **Verification:** 682 tests, 0 failures. No new Find path, no new ranker; ranking
  stays inside Canonical Recall (ADR-049). Not AVAILABLE.

### Core capability register v1 + D-1 fix (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** `docs/CORE_CAPABILITY_REGISTER_V1.md` — **CR-01…CR-09**, the
  capabilities the founder's Core definition ("persistent, multimodal,
  inspectable intelligence layer … inside the data's trust boundary") requires
  that are *unmodelled* in Freeze, Grounding Architecture, Ask Model, and code:
  fact validity/supersession, conflict reconciliation, audit seam,
  permission-scoped retrieval, re-derivation policy, reproducibility contract,
  aggregation trust, selective deletion with proof, knowledge stages. Each entry
  carries a retrofit cost class (Substrate / Path / Surface) and a build trigger.
- **Also:** `PROGRAM_STATE_AND_SEQUENCE_V1.md` §8b numbered execution backlog
  (A1…H5) so the sequence survives any single session.
- **Code:** defect **D-1** fixed — `CanonicalRecallMeaningTest` trimming fixtures
  now contain the query word, so the test exercises limit trimming instead of
  colliding with the MF-1 lexical precision gate. The gate was **not** weakened.
  Unit suite green: 679 tests, 0 failures.
- **Recovery note:** the uncommitted MF-1.1 and audit work was discarded from the
  working tree outside this repo's tooling and was rebuilt from the agent
  transcript (committed blob as base, post-commit edits replayed in order), then
  verified by compiling and running the full suite. Small uncommitted deltas to
  `MeaningTrustedHitPolicy.kt` and `MeaningSearchCopy.kt` had no recorded edit
  and remain at their committed state.
- **Truthfulness:** Register only — authorizes no code, no Find path, no
  Grounded Answers runtime, and no marketing AVAILABLE.

### Program state + sequence audit v1 (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** `PROGRAM_STATE_AND_SEQUENCE_V1.md` — verified architecture and
  Find state, 7 ranked defects (red unit test; lexical gate disabled by any TIME
  cue; relative time as string `contains`; TOPIC advisory on every query;
  `and`/`or`/`not` deleted as noise; meaning hits lack `evidenceId`; Why may cite
  a non-matching page), documentation truth drift (Live/Dual N, MIG-05 claim B,
  RECALL_CONVERGENCE_DONE), stale-code register, UNFYND Core Class A gaps and
  per-wave additions, and dependency-ordered stages 0–5.
- **Truthfulness:** Docs only. No code changed. Not AVAILABLE. Does not close any
  MVP-exit row; proposes an Ask Model v1.1 amendment (D16 device scope, result /
  system classes R1–R8, one-box ADR gate) for founder acceptance.

### Human Recall Ask Model v1.1 amendment (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** D16 device / corpus scope (“on my phone”, “on my computer”);
  §5b result and system classes R1–R8 (open failure, permission loss,
  near-duplicates, duplicate originals, garbage OCR, scale, search-during-index,
  sensitive cue); one-box decision registered at §2 and wave **W1.5**;
  §11 now requires every claimed anti-case to be a test.
- **Why:** eight scenario-bar rows and two decisions mapped to no class in v1,
  which the model’s own process rule calls a **model defect**
  (`PROGRAM_STATE_AND_SEQUENCE_V1.md` §3).
- **Truthfulness:** Docs only. Accept together with v1. No Find code. Not
  AVAILABLE. Does not unlock the daughter/pool/Saturday fixture.

### Human Recall Ask Model v1 (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** `HUMAN_RECALL_ASK_MODEL.md` — natural recall dimensions, shape
  classes, jobs, typo/time/visual/kinship policies, daughter/pool/Saturday
  fixture, Grounded Answers impact, waves W0–W5. Docs only; not AVAILABLE.

### Meaning Find P0 phase exit (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** Binding phase-exit in `MEANING_FIND_PRODUCT_SCENARIO_BAR.md`
  (8-row device checklist then MF-1.2). Generator test
  `MeaningFindP0AskShapeContractTest` so I1 is a contract, not anecdote patches.

### Meaning Find MF-1.1.2 ask-shape catalog + whole-Memory lexical (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** Closed English ask-shape catalog (verbs, type nouns, `e.g.`,
  `related`); lexical precision over all stored excerpts for the Asset, not
  only the cosine-winning snippet. Not AVAILABLE.

### Meaning Find MF-1.1.1 filler honesty + doc/scan (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** Filler queries (`show me the files`) skip embed and return no
  hits; `doc`/`docs` wrappers; lexical match on filename as well as excerpt.
  Device follow-up after MF-1.1. Not AVAILABLE.

### Meaning Find MF-1.1 wrappers + light plural (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** P0/P1/P2 freeze on the scenario bar (I35–I46 registered P2);
  ask-shape wrappers and `timetable`/`timetables` whole-word match. Change
  control `CHANGE_CONTROL_MEANING_FIND_MF1_1_WRAPPERS.md`. Not AVAILABLE;
  device MF-3 open.

### Meaning Find Intent Register v2 (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** `MEANING_FIND_PRODUCT_SCENARIO_BAR.md` expanded with surface
  ownership, intent IDs (I1–I34, G*, ACT1), golden wrappers, list-vs-same-file
  rules, and MIG-07B vs relative-time honesty. Change control
  `CHANGE_CONTROL_MEANING_FIND_INTENT_REGISTER_V2.md`. Docs only; not AVAILABLE.

### Meaning Find MF-1 precision + consumer Why (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** Scenario bar `MEANING_FIND_PRODUCT_SCENARIO_BAR.md`;
  `MeaningRecallCue` (NL → content-cue embed); lexical ≥1 token; boost stopwords;
  trusted short list; meaning Why dialect + friendly labels + short Open hints.
  Change control `CHANGE_CONTROL_MEANING_FIND_MF1_PRECISION.md`. Not AVAILABLE.

### FC-02 Stage A S4 wire smoke PASS (2026-09-04)

- **Date:** 2026-09-04
- **Delivered:** SM-A156E product meaning Find — CE present (`silky` top hit
  spelling-list PDF); identity when model absent (no crash); model restored.
  Runbook `FC02_STAGE_A_S4_WIRE_SMOKE_RUNBOOK.md`. Not AVAILABLE; ADR-052 UI
  still next for launch install.

### FC-02 Stage A wire (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** ORT promoted to `implementation`; `OnnxCrossEncoderRecallRanker` +
  `StageARecallRanker`; wired after lexical in `AnchorAwareMeaningRecallRanking` /
  `CanonicalRecall`; S3 DEGRADED_EXPLICIT pool 20; identity when pack absent;
  `CHANGE_CONTROL_FC02_STAGE_A_WIRE.md`.
- **Truthfulness:** CE only when model staged in no-backup; ADR-052 pack UI not
  shipped; not AVAILABLE.

### FC-02 Stage A S3 device PASS (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** SM-A156E S3 PASS — full40=1517 ms (over 800); reduced20@96=584 ms;
  disposition **DEGRADED_EXPLICIT** effectivePool=20. Binding for Stage A wire.
  See `CHANGE_CONTROL_FC02_STAGE_A_S3_LATENCY.md`.
- **Truthfulness:** Measurement + policy only; Find wire not yet; not AVAILABLE.

### FC-02 Stage A S3 latency harness (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** `RecallRankLatencyPolicy` (WITHIN_BUDGET / DEGRADED_EXPLICIT /
  IDENTITY_FALLBACK), unit tests, `RecallRankStageALatencyIntegrationTest`,
  `CHANGE_CONTROL_FC02_STAGE_A_S3_LATENCY.md`, S3 runbook. Device disposition
  pending founder Logcat.
- **Truthfulness:** No Find wire; not AVAILABLE; provisional DEGRADED from S1 only
  until S3 confirms.

### FC-02 Stage A S2 device PASS (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** Samsung SM-A156E S2 PASS — CE hit@1 3/3 on
  `stageALabeledCases()` after documenting raw-corpus 2/3 mira ambiguity.
  See `CHANGE_CONTROL_FC02_STAGE_A_S2_FIXTURE.md`.
- **Truthfulness:** Measurement only; no Find wire; not AVAILABLE; S3 open.

### FC-02 Stage A S2 fixture harness (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** `BertWordPieceTokenizer` (HF golden match for fixture pairs),
  `ScoreMeaningPdfPageRecallWithCrossEncoder`, JVM unit tests, androidTest
  `RecallRankStageAFixtureIntegrationTest` (Logcat `MemoraRecallRankS2`), vocab
  assets, `CHANGE_CONTROL_FC02_STAGE_A_S2_FIXTURE.md`.
- **Truthfulness:** Device S2 hit@1 result pending; no Find wire; not AVAILABLE.

### FC-02 Stage A S1 device PASS (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** Samsung SM-A156E S1 spike PASS — FULL/40; QInt8 ONNX download
  23180880 bytes; 40 pairs in 1642 ms (avg 41.05 ms); inputs
  attention_mask/input_ids/token_type_ids. Spike URL path fix + full BERT inputs.
  Recorded in FC-02 change control, model brief, S1 runbook, CONTINUE.
- **Truthfulness:** S1 load/latency smoke only; not Stage A wire; not AVAILABLE;
  S3 ≤800 ms bar still open (1642 ms observed).

### Docs track check — Recall N=0 + Stage A gate (2026-09-03)

- **Date:** 2026-09-03
- **Delivered:** Re-synced operator docs to Live/Dual **N = 0** / escape-hatch NO /
  `RECALL_CONVERGENCE_DONE` COMPLETE; aligned GOVERNANCE + registry with delivered
  MIG-07/MIG-07B; ADR-052 language in FC-02 / model brief / ORT review; S1 runbook
  `FC02_STAGE_A_S1_SPIKE_RUNBOOK.md`; MVP audit next actions point at S1 device proof.
- **Truthfulness:** Policy/docs only for this note; S1 device result still pending;
  not AVAILABLE; ADR-052 UI not shipped.

### ADR-052 Smart automatic AI pack onboarding (docs-only)

- **Date:** 2026-09-03
- **Delivered:** Accepted ADR-052 — launch-ready UX: one unified onboarding
  Continue after combined disclosure; auto meaning pack + auto rerank on eligible
  tiers; interim AiPackDisclosure multi-tap path is engineering-only.
  See `CHANGE_CONTROL_ADR052_SMART_AUTOMATIC_AI_PACK_ONBOARDING.md`.
- **Truthfulness:** Policy only; unified onboarding UI not shipped; not AVAILABLE.

### FC-02 Stage A S1 — device tiers + ONNX spike harness (2026-09-02)

- **Date:** 2026-09-02
- **Delivered:** `RecallRankDevicePolicy` (FULL/REDUCED/IDENTITY_ONLY pools),
  optional `RecallRankAiPackTrack`, `OnnxMsMarcoMiniLmCrossEncoderSpec`,
  `RecallRankCapabilitySupportPolicy`, `AndroidRecallRankDeviceSignals`,
  `RecallRankStageASpikeIntegrationTest` + `OnnxCrossEncoderSpikeSupport`
  (androidTest). `onnxruntime-android` 1.28.0 on **androidTest** classpath only.
  Dependency review: `onnxruntime-android-1.28.0-review.md`. Broader-coverage
  section added to Stage A model brief.
- **Truthfulness:** Not wired into Canonical Recall; not AVAILABLE; S1 ONNX test
  skips without staged model; promote ONNX to implementation after spike green.

### FC-02 Stage A recall-ranker model brief (docs-only)

- **Date:** 2026-09-02
- **Delivered:** `docs/dependency-review/fc02-stage-a-recall-ranker-model-brief.md`
  — five candidates evaluated; **recommended default:** ONNX Runtime Mobile +
  MS MARCO MiniLM-L6 cross-encoder QInt8 (~23 MiB); fallbacks documented. FC-02
  change control updated with selection table. Pending founder approval + S1 spike.
- **Truthfulness:** No App code; no new dependency merged; not AVAILABLE; Stage A
  not wired.

### ADR-051 Evidence-native on-device RecallRanker (docs-only)

- **Date:** 2026-09-02
- **Delivered:** Accepted ADR-051 — Spec `RecallRanker` inside Canonical Recall
  is an Evidence-native on-device ranker: Stage A (measured semantic head /
  FC-02) + Stage B (structured evidence signals; FC-03 before Stage B complete).
  Type-agnostic over `MemoryEvidence`. FC-02 change control reinterpreted under
  ADR-051. Registry / CONTINUE / P2 / backlog pointers.
  See `CHANGE_CONTROL_ADR051_EVIDENCE_NATIVE_RECALL_RANKER.md`.
- **Truthfulness:** Docs only; no App wiring; not AVAILABLE; Stage A model pack
  not selected; public “evidence-native ranker” requires Stage B measured green.

### Post-MVP program, Class A validator, domain foundations (2026-09-02)

- **Date:** 2026-09-02
- **Delivered:** `POST_MVP_PROGRAM_V1.md`, `MVP_EXIT_AUDIT.md`, Class A validator +
  CI, `EXPORT_CONTRACT.md` / `INTEGRATION.md`, `A01_OFFLINE_DEVICE_PROOF_RUNBOOK.md`,
  `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md` (authorized), domain foundations:
  `AudioSegmentEvidenceLocator`, `domain.grounding` contracts, `RecallRanker.rank` +
  `IdentityRecallRanker`, `EmbeddingModelSignature` alias.
- **Truthfulness:** Not AVAILABLE; FC-02 slice 2 (cross-encoder wiring) not shipped;
  A-01 requires physical device proof per runbook.

### Post-MVP program + Class A conformance validator (2026-09-01)

- **Date:** 2026-09-01
- **Delivered:** `docs/POST_MVP_PROGRAM_V1.md` — gate-driven post-MVP phases,
  foundation checklist, Core/App matrix, open-source ladder. `docs/MVP_EXIT_AUDIT.md`
  — honest PASS/FAIL/PARTIAL inventory for MVP exit. Class A pack: JSON Schema,
  Python validator CLI, `BUILDING.md`, CI job, `EXPORT_CONTRACT.md` +
  `INTEGRATION.md` planning sketches, two new synthetic examples.
- **Truthfulness:** Program doc does not authorize AVAILABLE, Act, FC-02, or
  Grounded Answers implementation. Audit records open blockers (A-01, AVAILABLE
  decision, battery/latency budgets).

### Fix: Find by meaning — anchor load crash + hardening (2026-09-01)

- **Date:** 2026-09-01
- **Delivered:** `findSignatureAnchors` loads cited evidence from
  `memory_anchor_evidence` (fixes crash on typical ≥4-char queries during anchor
  ranking). MediaPipe embedder serialized for index+search safety. Candidate drop
  diagnostics + failure-path tests (ViewModel, Canonical Recall, Room integration).
  See `CHANGE_CONTROL_MEANING_SEARCH_ANCHOR_FIX.md`.
- **Truthfulness:** Defect fix on Canonical Recall MEANING path; Live/Dual N
  unchanged (0); not marketing AVAILABLE.

### Product voice refresh + build fix (2026-08-31)

- **Date:** 2026-08-31
- **Delivered:** Welcome/Privacy onboarding, Find screens, setup summaries, and
  Why copy reframed to capability-forward product voice (no leading “not yet” /
  interim framing). Direct `androidx.appcompat` dependency fixes AGP 9 resource
  linking for MSAL (Compose + OneNote auth).
- **Truthfulness:** Internal governance / AVAILABLE boundaries unchanged in code
  comments and change control; user-facing copy describes what UNFYND does.

### Fix: Recall reach — STALE / page evidence index deadlock (2026-08-31)

- **Date:** 2026-08-31
- **Delivered:** Narrow MIG-05 STALE gaps to summary-indexed PDFs missing evidence
  embeddings; meaning-index lookups include STALE for Build + meaning hits;
  keyword MemoryEvidence search includes STALE (excerpts stay searchable).
  See `CHANGE_CONTROL_RECALL_REACH_STALE_EVIDENCE_DEADLOCK.md`.
- **Truthfulness:** Defect fix on Canonical Recall path; Live/Dual N unchanged
  (0); not marketing AVAILABLE.

### MIG-05 claim B slice 2 — note text evidence embeddings (2026-08-31)

- **Date:** 2026-08-31
- **Delivered:** `findNoteTextEvidenceForEmbedding` + meaning-index drain for
  NOTE assets; reuses `IndexOcrEvidenceEmbeddings`. ADR-050 claim B closed for
  all MVP asset types.
- **Truthfulness:** Not marketing AVAILABLE; MIG-05 Spec full close still needs
  founder checklist per ADR-050.

### Recall program exit — COMPLETE (2026-08-31)

- **Date:** 2026-08-31
- **Delivered:** `CanonicalRecallResult` + `CanonicalRecallWhyCopy`; keyword
  hits carry `recall`; all five Find ViewModels use unified Why dialect.
  `RECALL_CONVERGENCE_DONE` 15/15 boxes checked.
- **Truthfulness:** Not marketing AVAILABLE; MIG-05 claim B (notes evidence
  embeddings) still open; not Grounded Answers / Act.

### Recall program exit — partial closure (2026-08-31)

- **Date:** 2026-08-31
- **Delivered:** `RECALL_CONVERGENCE_DONE` retirement + canonical retrieval
  boxes checked (13/15). Live/Dual N=0; escape-hatch NO.
- **Still OPEN:** shared result model + shared Why contract (2 boxes).

### FC-04 — Corpus completeness honesty UI

- **Date:** 2026-08-31
- **Delivered:** `LoadCorpusCompleteness` + indexed/pending/blocked counts on
  Find-by-meaning readiness, AI Pack disclosure, and Asset Memory setup.
- **Truthfulness:** On-device Room counts only; not AVAILABLE; not Recall DONE.

### MIG-07B Slice 4 — L7 retirement (shared meaning ranker)

- **Date:** 2026-08-31
- **Delivered:** `MeaningEvidenceTokenBoost` moved from
  `SearchAssetMemoriesByMeaning` to `AnchorAwareMeaningRecallRanking` inside
  `CanonicalRecall`. Candidate gen returns cosine scores only; over-fetch pool
  before final limit. Legacy guard section F. L7 → Retired; Live/Dual N=0.
- **Truthfulness:** Not `RECALL_CONVERGENCE_DONE` (shared result/Why + MIG-05
  claim B remain open).

### MIG-07B Slice 3 — L8 retirement (meaning Find via CanonicalRecall)

- **Date:** 2026-08-31
- **Delivered:** `MeaningSearchViewModel` injects `CanonicalRecall` only;
  calls `searchByMeaning`. Legacy guard section E (`SearchAssetMemoriesByMeaning`
  allowlist; no `ui/**` binding). L8 → Retired; Live/Dual N=1 (L7 only).
- **Truthfulness:** L7 local ranking still Live inside
  `SearchAssetMemoriesByMeaning`. Not Recall DONE.

### MIG-07B Slice 2 — CanonicalRecall meaning + hybrid RRF

- **Date:** 2026-08-31
- **Delivered:** `CanonicalRecall.searchByMeaning` (anchor filter on meaning
  path), `searchHybrid` (keyword + meaning RRF), `findSignatureAnchors` port,
  `ReciprocalRankFusion`, `AnchorAwareMeaningRecallRanking`.
- **Truthfulness:** Slice 2 only; L8 cutover landed in Slice 3.

### Dual-track Checkpoint 1 — MIG-05 B slice 1 + MIG-07B Slice 1

- **Date:** 2026-08-31
- **Delivered:** Photo/screenshot OCR evidence embedding index drain
  (`IndexOcrEvidenceEmbeddings`); meaning-index Build tap wires OCR candidates.
  MIG-07B domain contracts: `RecallQueryConstraintClassifier`,
  `AnchorStructuredRecallFilter`. Change controls authorized.
- **Truthfulness:** Checkpoint 1 only. ADR-050 claim **B** OPEN (notes deferred).
  L7/L8 unchanged. Not Recall DONE, not AVAILABLE, not Gradle Phase 2.

### Core / App Phase 1 — exit complete

- **Date:** 2026-08-30
- **Delivered:** Phase 1 boundary hygiene closed. Application→data allowlist
  3/3 (OneNote App bucket). Domain purity guard
  `scripts/check-domain-layer-purity.sh` + CI job `Core / App layer boundary
  guards`. Change control marked complete.
- **Truthfulness:** Phase 1 only; not Gradle modules or MIG-07B implementation.

### MIG-07B change control (docs-only)

- **Date:** 2026-08-30
- **Delivered:** `docs/CHANGE_CONTROL_MIG07B_ANCHOR_AWARE_RECALL.md` — sliced
  plan to fold L7/L8 into Canonical Recall with anchor-aware structured filter.
- **Truthfulness:** Awaiting authorization; no code.

### Core / App Phase 1 — boundary hygiene slice 4 (Core exit)

- **Date:** 2026-08-30
- **Delivered:** `UserConfirmedDerivedDataClearer`, `OnDeviceEmbeddingModelDownloader`
  domain ports; `ClearMemoraDerivedData` and `DownloadOnDeviceEmbeddingModel` clean.
  Allowlist 5→3 (OneNote App bucket only). Phase 1 Core exit met.
- **Truthfulness:** Phase 1 hygiene only; not Gradle modules or MIG-07B.

### Core / App Phase 1 — boundary hygiene slice 3 (PDF ports)

- **Date:** 2026-08-30
- **Delivered:** `PdfReadOnlyDescriptorAccess`, `PdfIsolatedLocalReadingSession`,
  `ValidatedPdfLocalReadingPersister`; refactored PDF open/view/extract use cases;
  `PersistValidatedPdfLocalReading` moved to data layer. Hilt `PdfExtractionBindingsModule`.
  Boundary allowlist 10→5.
- **Truthfulness:** Phase 1 hygiene only; not MIG-07B or Gradle modules.

### Core / App Phase 1 — boundary hygiene slice 2

- **Date:** 2026-08-30
- **Delivered:** `PhotoOcrReader`, `ScreenshotOcrReader`, `ImageExifReader` moved
  to `domain/extraction/ImageExtractionReaders.kt`. `RunPendingPhotoOcrExtract`,
  `RunPendingScreenshotOcrExtract`, `RunPendingImageExifExtract` inject domain
  persistence ports only (no `data` imports). Boundary allowlist 13→10.
- **Truthfulness:** Phase 1 hygiene only; not Gradle modules or MIG-07B.

### Future capability backlog (docs-only)

- **Date:** 2026-08-30
- **Delivered:** `docs/FUTURE_CAPABILITY_BACKLOG.md` — Tier A candidates (RRF,
  cross-encoder rerank, chunking, corpus honesty, zero-egress pack, evidence
  lineage), Tier B deferred options (embedding/SLM/runtime/ANN/CRAG/ColBERT/graph
  UI), Tier C explicit out-of-scope (routing, GraphRAG, multi-agent, neuromorphic,
  etc.). Adjacent landscape pointers (StratoSort, enterprise hybrid search).
  `CONTINUE.md` pointer.
- **Truthfulness:** Planning backlog only; does not authorize MIG-*, Grounded
  Answers code, Freeze reopen, or new Find paths. Items already in MIG/Grounding
  docs are cross-referenced, not duplicated.

### Core / App Phase 1 — boundary hygiene slice 1

- **Date:** 2026-08-30
- **Delivered:** Image-library access in five application use cases routed through
  `ImageLibraryDiscoverySource` (domain port). CI guard
  `scripts/check-application-layer-boundaries.sh` with shrink-only allowlist (13
  remaining `application`→`data` files). Inventory table in separation plan §6.1.
  Change control: `CHANGE_CONTROL_CORE_APP_PHASE1_BOUNDARY_HYGIENE.md`.
- **Truthfulness:** Phase 1 hygiene only; not Gradle modules, Class B, or public
  Core source.

### Core / App separation — execution plan (docs-only)

- **Date:** 2026-08-30
- **Delivered:** `docs/CORE_APP_SEPARATION_PLAN.md` — phased path from monorepo
  hygiene → Gradle modules → private Maven → optional public Core source
  (Class B ADR). Package allowlists, coupling hotspots, recall/MIG alignment,
  verification gates. `CONTINUE.md` pointer.
- **Truthfulness:** Planning only; does not authorize implementation, Class B,
  repository split, or “fully open Core” claims.

### Canonical Recall thin façade (KEYWORD path)

- **Date:** 2026-08-30
- **Delivered:** `CanonicalRecall` application API; PDF/screenshot/photo/note
  ViewModels inject it; `SearchMemoryEvidence` is candidate gen only (no
  `ui/**` binding). CI legacy guard updated. LEGACY N=2 unchanged (L7, L8).
  Not MIG-07B / Recall DONE / AVAILABLE.
- **Truthfulness:** Thin KEYWORD façade only; meaning/ranking still Live
  outside CanonicalRecall.
- **Change control:** `docs/CHANGE_CONTROL_CANONICAL_RECALL_THIN_FACADE.md`.

### Vision alignment — MVP vs Act / multimodal surface (docs-only)

- **Date:** 2026-08-30
- **Delivered:** `docs/UNFYND_VISION_ALIGNMENT.md` records planning rule:
  README multimodal maps are long-term vision (not silent MVP scope); do not
  freeze Act/mutation now; Connect and Grounded Answers stay out of MVP exit;
  MVP keeps permission/disclosure, evidence + Explain, read-only Assets.
  `CONTINUE.md` pointer. ADR-043 Act-out unchanged; no Freeze reopen.
- **Truthfulness:** Docs only; not architectural authority (ADR-043 still
  governs). Does not authorize MIG-*, Connect, Grounded Answers code, or Act.

### Public framing: App intelligence README + Class A cleanup

- **Date:** 2026-08-30
- **Delivered:** Root `README.md` rewritten as UNFYND App intelligence vision
  (pillars, multimodal map, information→action evolution, progress without MVP
  gap lists). Class A pack: drop Search/Find definition lines; remove em dashes
  from public pack; strategy §1 no “personal memory.” Sync pack-only to public
  unfynd-core after commit. Website unchanged.
- **Truthfulness:** Docs only; vision + progress framing.

### Class A pack — vision alignment with unfynd.com/core (+ root README)

- **Date:** 2026-08-30
- **Delivered:** `public/unfynd-core/` rewrite: vision-first README/SPEC;
  `APPLICATIONS.md` (primitives + industries from Core site); ROADMAP-OPEN /
  examples / GOVERNANCE / CONTRIBUTING phased openness; removed public
  `SPEC-STATUS.md` (App MVP audit stays private). Root `README.md` aligned
  (Core vision; public GitHub link; not personal AI). PUBLIC_CHANGELOG /
  CITATIONS / CONTINUE updated. **Synced pack-only** to public unfynd-core
  (`da457b9`). Private living-canon light-align: `AGENTS.md`,
  `docs/GOVERNANCE.md` / `GROUNDING_ARCHITECTURE.md` headers,
  `OPEN_SOURCE_COMMERCIAL_STRATEGY.md` Class A publish status. Private Memora
  not pushed.
- **Truthfulness:** Docs only; no App MIG code. Does not claim vertical
  products shipped or full stack open. Site Open “AGI / run / fork” marketing
  wording is a website follow-up — Class A pack stays selected foundations.

### Class A pack — status alignment (Memory-evidence Find + midrange)

- **Date:** 2026-08-30
- **Delivered:** Prior pack refresh (App status column + EmbeddingEngine
  wording). Superseded for public pack structure by vision-alignment entry
  above (`SPEC-STATUS` removed from public pack).
- **Truthfulness:** Docs / Class A pack only; no App MIG code in that checkpoint.

### MIG-07 note — keyword Find cutover to SearchMemoryEvidence

- **Date:** 2026-08-30
- **Delivered:** Note keyword Find ViewModel binds `SearchMemoryEvidence`
  (`AssetType.NOTE`); adapter maps hits to existing UI models; readiness
  counts READY note Memory evidence; `SearchPersistedNotePageText` and
  `NotePageKeywordSearchSupport` deleted. L4 Retired; Live/Dual **N=2**
  (L7, L8). Guard updated (no L4 SearchPersisted allowlist; note UI
  allowlisted for SearchMemoryEvidence). **MIG-07 keyword L1–L4 cutovers
  complete.** Canonical Recall not a live API; full MIG-07 / Recall DONE
  not claimed.
- **Truthfulness:** Escape-hatch still YES via L7, L8. Marketing AVAILABLE
  not claimed. ADR-050 claim A done / B open.
- **Change control:** `docs/CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER.md`.

### MIG-07 photo — keyword Find cutover to SearchMemoryEvidence

- **Date:** 2026-08-30
- **Delivered:** Photo keyword Find ViewModel binds `SearchMemoryEvidence`
  (`AssetType.PHOTO`); adapter maps hits to existing UI models; readiness
  counts READY photo Memory evidence; `SearchPersistedPhotoOcrText` deleted.
  L3 Retired; Live/Dual **N=3** (superseded for N by note cutover above;
  L3 remains Retired). Guard updated (no L3 SearchPersisted
  allowlist; photo UI allowlisted for SearchMemoryEvidence). L4 unchanged
  at landing. Canonical Recall not a live API; full MIG-07 / Recall DONE
  not claimed.
- **Truthfulness:** Escape-hatch still YES via L4, L7, L8 at landing.
  Marketing AVAILABLE not claimed. ADR-050 claim A done / B open.
- **Change control:** `docs/CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER.md`.

### MIG-07 screenshot — keyword Find cutover to SearchMemoryEvidence

- **Date:** 2026-08-30
- **Delivered:** Screenshot keyword Find ViewModel binds `SearchMemoryEvidence`
  (`AssetType.SCREENSHOT`); adapter maps hits to existing UI models; readiness
  counts READY screenshot Memory evidence; `SearchPersistedScreenshotOcrText`
  deleted. L2 Retired; Live/Dual **N=4** (superseded for N by photo cutover
  above; L2 remains Retired). Guard updated (no L2 SearchPersisted
  allowlist; screenshot UI allowlisted for SearchMemoryEvidence). L3–L4
  unchanged at landing. Canonical Recall not a live API; full MIG-07 / Recall
  DONE not claimed.
- **Truthfulness:** Escape-hatch still YES via L3–L4, L7, L8 at landing.
  Marketing AVAILABLE not claimed. ADR-050 claim A done / B open.
- **Change control:** `docs/CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER.md`.

### MIG-07 PDF — keyword Find cutover to SearchMemoryEvidence

- **Date:** 2026-08-29
- **Delivered:** PDF keyword Find ViewModel binds `SearchMemoryEvidence`
  (`AssetType.PDF`); adapter maps hits to existing UI models; readiness counts
  READY PDF Memory evidence; `SearchPersistedPdfPageText` deleted. L1 Retired;
  Live/Dual **N=5** (superseded for N by screenshot cutover above; L1 remains
  Retired). Guard updated (no L1 SearchPersisted allowlist; PDF UI
  allowlisted for SearchMemoryEvidence). L2–L4 unchanged at landing. Canonical
  Recall not a live API; full MIG-07 / Recall DONE not claimed.
- **Truthfulness:** Escape-hatch still YES via L2–L4, L7, L8 at landing.
  Marketing AVAILABLE not claimed. ADR-050 claim A done / B open.
- **Change control:** `docs/CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER.md`.

### MIG-06 step 1 — additive SearchMemoryEvidence

- **Date:** 2026-08-29
- **Delivered:** Additive `SearchMemoryEvidence` application use case querying
  `MemoryEvidence` excerpts (Room LIKE; no schema bump), Hilt-bound excerpt
  search port, unit tests for all current evidence kinds + empty-query honesty.
  CI guard allowlists MIG-06 application files; still forbids UI Find clones
  and `CanonicalRecall`. **No** ViewModel/screen/`SearchPersisted*` cutover
  (superseded for PDF by MIG-07 PDF cutover above; L2–L4 still Live).
- **Truthfulness:** At landing: L1–L4 still Live; Live/Dual **N=6**; Canonical
  Recall not a live API (ADR-049); MIG-07 not started; ADR-050 claim A done /
  B open; marketing AVAILABLE not claimed.
- **Change control:** `docs/CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE.md`.

### M4 midrange USE page-recall execute

- **Date:** 2026-08-29
- **Delivered:** Ran
  `MeaningPdfPageRecallUseMidrangeBaselineIntegrationTest` on physical Galaxy
  A15 (`SM-A156E` / `RZCX12KZ6EN` / `midrange_arm64`) only — **PASS**. Log
  `MemoraMeaningPdfM4`: USE cosine-only **2/3**, E5d-boosted **3/3**, embeds
  14, wallMs 448 (not an SLA). Compatibility midrange EMBEDDING/RECALL_RANKER
  → `DEGRADED_EXPLICIT`. Enterprise M4 execute checked; AVAILABLE decision
  checklist still open.
- **Truthfulness:** Marketing AVAILABLE remains **NO**. Live/Dual **N=6**
  unchanged. No push. Keep E5d disclosed assist.
- **Change control:** `docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md`
  (execute delivery). Artifact:
  `docs/artifacts/MemoraMeaningPdfM4-RZCX12KZ6EN.logcat.txt`.

### Legacy recall surface CI grep guard

- **Date:** 2026-08-29
- **Delivered:** Added `scripts/check-legacy-recall-surface.sh` and CI job
  `Legacy recall surface guard` (no JDK). Fails on retired page-embedding
  tokens outside migration history, new unallowlisted `*KeywordSearch*` /
  `SearchPersisted*` under ui/application, or premature `CanonicalRecall` /
  `SearchMemoryEvidence` in main. Does not authorize MIG-06+. N=6 unchanged.
- **Change control:** `docs/CHANGE_CONTROL_LEGACY_RECALL_SURFACE_CI.md`.

### ADR-050 MIG-05 claim levels (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-050 — claim **A** (PDF evidence-embedding
  delivery slice steps 1–4) COMPLETE for eng checkpoints; claim **B**
  (Migration Spec MIG-05 full) STILL OPEN until non-PDF evidence indexing.
  Grants/public must not say “MIG-05 complete” without stating B open.
  CONTINUE + FULL DONE checklist + registry + enforcement index updated.
  No app code; hashed Spec/Freeze untouched; non-PDF indexer not authorized.
- **Change control:** `docs/CHANGE_CONTROL_ADR050_MIG05_CLAIM_LEVELS.md`.

### Recall enforcement 60s operator entry (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Tightened `docs/RECALL_ENFORCEMENT_INDEX.md` into the single
  operator entry (heartbeat N=6, 60s checklist, deep links only). CONTINUE +
  AGENTS short pointers. No MIG code; no Canonical Recall fiction.
- **Change control:** append on `docs/CHANGE_CONTROL_ESCAPE_HATCH_AUDIT.md`.

### Canonical Recall fiction scan (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Living docs/rules scan PASS — Canonical Recall remains
  target-only (ADR-049); not claimed as a live ViewModel/API. Recorded in
  `ESCAPE_HATCH_AUDIT.md`. No app code.

### Status truth check — recall / MIG-05 (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Recorded that CONTINUE + `LEGACY_RECALL_SURFACE` + code agree
  after MIG-05 step 4; Live/Dual **N=6**. No app behavior change.

### MIG-05 step 4 — retire PdfPageEmbedding*

- **Date:** 2026-08-29
- **Delivered:** Deleted `PdfPageEmbedding*` (entity/DAO/Room store/domain
  interface). Room **14→15** `DROP TABLE pdf_page_embeddings`.
  `IndexPdfPageEmbeddings` is evidence-only (fingerprint-skip against
  `MemoryEvidenceEmbeddingStore`). Cutover STALE uses READY ∩ `pdf:page:N`
  evidence ∩ zero evidence embeddings. L5 Retired; Live/Dual **6**.
- **Truthfulness:** Full MIG-05 Spec acceptance remains **open** — non-PDF
  evidence indexer **deferred** (CHANGE_CONTROL step 4). Search cutover from
  step 3 unchanged. MIG-06+, ranking redesign, AVAILABLE, Act, Grounded
  Answers code, Links, Event/Knowledge, VisionEngine, and package/db rename
  are not started. Hashed freeze/spec/amendment files unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (step 4 section).

### CONTINUE status table honesty (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Find-by-meaning status row now reflects MIG-05 step 3
  evidence-level embedding ranking (not PdfPageEmbedding* for search).

### Escape-hatch audit + Live/Dual metric habit (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added `docs/ESCAPE_HATCH_AUDIT.md` (cadence, checklist,
  record template) and `docs/RECALL_ENFORCEMENT_INDEX.md` (Steps 1–7).
  Allowlist Audit cadence + CONTINUE Live/Dual = 7 one-liner. Enforcement
  program complete (docs); MIG code not started.
- **Change control:** `docs/CHANGE_CONTROL_ESCAPE_HATCH_AUDIT.md`.

### Canonical Recall shared result + Why contract (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added `docs/CANONICAL_RECALL_RESULT_CONTRACT.md` — DRAFT
  logical shared Find hit + Why fields (path labels KEYWORD|MEANING; no
  Kotlin). CONTINUE + `RECALL_CONVERGENCE_DONE` pointers. No app code; does
  not authorize MIG-06+.
- **Change control:** `docs/CHANGE_CONTROL_CANONICAL_RECALL_RESULT_CONTRACT.md`.

### MIG-05 + Recall convergence DONE checklists (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added MIG-05 FULL DONE retirement checklist (unchecked) to
  `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE`; new
  `docs/RECALL_CONVERGENCE_DONE.md` (Canonical Recall program exit; retirement
  mandatory). CONTINUE pointers only. No app code; checklists remain open;
  does not authorize MIG-05 step 4 / MIG-06+.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_RECALL_CONVERGENCE_DONE_CHECKLISTS.md`.

### Change-control architectural convergence (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Wired Find/Recall enforcement into change-control —
  architectural convergence block on `CHANGE_CONTROL_TEMPLATE`; new
  `LEGACY_EXTENSION_EXCEPTION.md` (mandatory sunset); GOVERNANCE pre-work
  pointers. No app code; does not authorize MIG-06+ or reopen Freeze.
- **Change control:** `docs/CHANGE_CONTROL_ARCHITECTURAL_CONVERGENCE_TEMPLATE.md`.

### Cursor architecture invariants rule (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Added alwaysApply Cursor rule
  `.cursor/rules/unfynd-architecture-invariants.mdc` — Canonical Recall /
  Memory substrate invariants, false-positive guard, pre-code gate; links
  Freeze §3, ADR-049, `LEGACY_RECALL_SURFACE`. No app code; does not authorize
  MIG-05 step 4 / MIG-06+.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_ARCHITECTURE_INVARIANTS_RULE.md`.

### Legacy recall surface allowlist (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Introduced `docs/LEGACY_RECALL_SURFACE.md` — living
  shrink-only allowlist (L1–L8) for interim product Find paths; Live/Dual
  count = 7. Does not authorize MIG-05 step 4 / MIG-06+ or Cursor rules.
- **Change control:** `docs/CHANGE_CONTROL_LEGACY_RECALL_SURFACE.md`.

### ADR-049 Canonical Recall naming (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-049: App Find boundary name **Canonical Recall**;
  MIG-06 `SearchMemoryEvidence` = candidate generation into it; Grounding
  Retriever Option C (same pipeline); Spec `RecallRanker` / MIG-07B stages
  inside Canonical Recall. No MIG-06+ authorization; no app search code.
- **Change control:** `docs/CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md`.

### MIG-05 step 3 — meaning search cutover to evidence embeddings

- **Date:** 2026-08-29
- **Delivered:** `SearchAssetMemoriesByMeaning` ranks page/evidence hits from
  `MemoryEvidenceEmbeddingStore` + `MemoryEvidence` excerpt (page via
  `PdfPageEvidenceLocator`); no `SavedPdfPageTextSource` / no
  `PdfPageEmbeddingStore` on the search ranking path. Readiness
  `indexedCount` = summary + evidence stores. Cutover STALE targets only
  READY gap revisions (page embeddings without evidence embeddings for the
  active model); meaning-index drain includes STALE and restores READY after
  dual-write fill.
- **Truthfulness:** `IndexPdfPageEmbeddings` still dual-writes
  `PdfPageEmbedding*`. Full MIG-05 acceptance remains open (step 4 retirement
  + remaining Spec criteria). Room stays 14. MIG-06+, ranking redesign,
  AVAILABLE, Act, Grounded Answers code, Links, Event/Knowledge, VisionEngine,
  and package/db rename are not started. Hashed freeze/spec/amendment files
  unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (step 3 section).

### MIG-05 pre-step-3 — e2e dual-write device proof

- **Date:** 2026-08-29
- **Delivered:** `IndexPdfPageEmbeddingsDualWriteInstrumentedTest` proves on
  Medium Phone that dual-write persists both `pdf_page_embeddings` and
  `memory_evidence_embeddings` with real `e{n}` evidence ids (Room v14);
  unresolved path writes page store only. Search constructor assert confirms
  no cutover.
- **Truthfulness:** MIG-05 step 3 (Search cutover) not started. Room stays 14.
  Full MIG-05 acceptance remains open.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (pre-step-3 e2e dual-write section).

### MIG-05 step 2 — PDF page embedding dual-write to evidence store

- **Date:** 2026-08-29
- **Delivered:** `IndexPdfPageEmbeddings` dual-writes successful PDF page
  vectors into `MemoryEvidenceEmbeddingStore` keyed by
  `(revisionId, evidenceId, model)` when `MemoryEvidence.id` resolves via
  locator `pdf:page:N` (never the locator as id). Fingerprint-skip backfills
  the evidence store without mass `STALE_REINDEX`. Callers thread resolved
  evidence ids through `PdfPageEmbeddingCandidate`.
- **Truthfulness:** Search still reads `PdfPageEmbedding*` +
  `SavedPdfPageTextSource`. Full MIG-05 acceptance remains open. Room stays
  14. MIG-06+, ranking, AVAILABLE, Act, Grounded Answers code, Links,
  Event/Knowledge, VisionEngine, and package/db rename are not started.
  Hashed freeze/spec/amendment files unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`
  (step 2 section).

### MIG-05 step 1 residual closed (Room 13→14 device-verified)

- **Date:** 2026-08-29
- **Delivered (docs only):** `MemoraDatabaseMigrationTest` passed **3/3** on
  the Medium Phone emulator (Android Studio Run of class). Room 13→14
  evidence-embedding store foundation is device-verified. No application
  code change in this residual close. MIG-05 step 2 not started.
- **Truthfulness:** Full MIG-05 acceptance remains open. Hashed
  freeze/spec/amendment files unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`.

### MIG-05 step 1 — evidence-level embedding store foundation

- **Date:** 2026-08-29
- **Delivered:** Additive `MemoryEvidenceEmbedding` domain + Room table/store
  (`memory_evidence_embeddings`, Room 13→14) keyed by
  `(revisionId, evidenceId, model)`. Hilt-bound; unused by Search/Index.
  **Dual-store interim:** `PdfPageEmbedding*` remains the live PDF meaning
  path. No `STALE_REINDEX_REQUIRED` marking in this step. No SQL remap of old
  page vectors onto evidenceIds. No search/index cutover.
- **Truthfulness:** Full MIG-05 acceptance remains open. MIG-06+, ranking,
  AVAILABLE, Act, Grounded Answers code, Links/Event/Knowledge, VisionEngine,
  and package/db rename are not started. Hashed freeze/spec/amendment files
  unchanged. `ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION` remains 5
  (conversion journal, not Room). Device residual later closed separately
  (see Unreleased residual entry above).
- **Change control:** `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`.

### Class A pack enrichment — status, examples, open-foundation docs (docs-only)

- **Date:** 2026-08-29
- **Delivered:** `public/unfynd-core/` enrichment: `SPEC-STATUS.md` (honest
  contract vs App table); synthetic `examples/` (valid Memory+evidence + reject
  cases; ADR-048); `PUBLIC_CHANGELOG.md`, `CONTRIBUTING.md`, `GOVERNANCE.md`;
  README pack revision + inspect/build-on/run section; ROADMAP-OPEN / SPEC /
  NOTICE / CITATIONS pointers. LICENSE remains Apache-2.0. Synced pack files
  only to https://github.com/CzarPiratez/unfynd-core. Private Memora not pushed.
  No App code, AI Packs, or personal fixtures.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_ENRICHMENT.md`; ADR-048.

### ADR-048 synthetic Class A reference samples (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-048: synthetic non-personal Class A examples under
  the same Apache-2.0 license as the pack; not a schema lock; no App open.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_ENRICHMENT.md`.

### Class A pack copy fix — App vs Core / no Android-only framing (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Public pack under `public/unfynd-core/` rewritten for honest
  App vs Core nouns (multiplatform App; Core = infrastructure). Removed
  Android-as-product / “reference application” framing from README, SPEC,
  SECURITY, ROADMAP-OPEN, and NOTICE. Optional line: commercial / enterprise
  licensing is separate from this Apache-2.0 contracts pack. LICENSE unchanged
  (Apache-2.0). Synced pack files only to https://github.com/CzarPiratez/unfynd-core.
  Private Memora monorepo not pushed.
- **Change control:** ADR-047 Class A pack (docs copy).

### Class A pack published to public GitHub (docs-only)

- **Date:** 2026-08-29
- **Delivered:** ADR-047 follow-up (c): Class A Apache-2.0 pack published to
  https://github.com/CzarPiratez/unfynd-core root only (`LICENSE`, `NOTICE`,
  `README.md`, `SPEC.md`, `ROADMAP-OPEN.md`, `SECURITY.md`, `CITATIONS.md`).
  Private Memora monorepo was not pushed.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.

### Root README / identity alignment — App vs Core (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Root `README.md` rewritten to site-aligned UNFYND App vs
  UNFYND Core voice (privacy-first, vision ladder as direction with Act/agents
  and AVAILABLE non-claims, Class A honesty, multiplatform App surfaces). Light
  App vs Core table fix in `public/unfynd-core/README.md`; light identity touch
  in `CONTINUE.md` / `AGENTS.md`. No MIG-*; no technical-ID renames; no private
  Memora push. Public Core GitHub publish tracked as Class A follow-up (c).
- **Change control:** `docs/CHANGE_CONTROL_README_APP_VS_CORE.md`.

### Class A NOTICE copyright attribution fix (docs-only)

- **Date:** 2026-08-29
- **Delivered:** `public/unfynd-core/NOTICE` copyright holder set to
  `Copyright 2026 UNFYND <czar.piratez@gmail.com>`. Pack scan: no other
  Memora / mir.m.hameedi copyright lines. Trademark and Apache NOTICE body
  unchanged.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.

### Class A pack files under `public/unfynd-core/` (docs-only)

- **Date:** 2026-08-29
- **Delivered:** ADR-047 follow-up (a): curated Class A pack landed at
  `public/unfynd-core/` under Apache-2.0 (`LICENSE`, `NOTICE`, `README.md`,
  `SPEC.md`, `ROADMAP-OPEN.md`, `SECURITY.md`, `CITATIONS.md`). Plain-language
  distillation of Core contracts (Memory/Evidence, evidence classes, capability
  seams including MemoryBuilder, local-first / truth-before-intelligence /
  retrieval-first). No hashed Freeze/Spec/Amendment/Grounding/Product Contract
  blob edits.
- **Truthfulness:** Root README App vs Core rewrite and public GitHub publish of
  **only** this pack were deferred at pack landing; publish completed as
  follow-up (c). No MIG-*, no private Memora push, no app source, no secrets.
  Act still out; no AVAILABLE / Act shipped claims.
- **Change control:** `docs/CHANGE_CONTROL_CLASS_A_PACK_V1.md`.

### ADR-047 Class A UNFYND Core contracts under Apache-2.0 (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-047: Release Class A authorizes a curated
  **Public Specification / Contract** pack for UNFYND Core under **Apache
  License 2.0** (Memory/Evidence model, evidence classes, capability seams
  including MemoryBuilder, local-first / truth-before-intelligence /
  retrieval-first principles in plain public markdown). Cites ADR-046 Core
  noun; strategy §23 Class A; ADR-040 technical IDs unchanged. Product owner
  accepts Apache-2.0 residual legal risk without external counsel for this
  docs/contracts pack. Class A does not open the Android app or proprietary
  assets. Openness is product strategy (no grant-program framing).
- **Truthfulness:** Pack files (`public/unfynd-core/` or equivalent), root
  README Class A rewrite, and public GitHub repo publish are **not** created
  in this step — deferred follow-ups. No MIG-*, Grounded Answers code,
  Act/agents, Class B commercial licensing completion, or Architecture Freeze
  reopen. Hashed freeze/spec/amendment/grounding/contract/GA files unchanged.
  No `MemoraApp/` edits.
- **Change control:**
  `docs/CHANGE_CONTROL_ADR047_CLASS_A_CORE_CONTRACTS.md`.

### ADR-046 Core noun on-device memory and intelligence infrastructure (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-046: living-canon and public product language is
  **UNFYND Core — on-device memory and intelligence infrastructure**. Supersedes
  ADR-045 public wording (“on-device intelligence infrastructure”) only; ADR-045
  still stands for retired PKI/PII/grant metaphors, hashed PKI untouched,
  technical IDs unchanged, and non-authorization of Class A / app open / MIG /
  Act. Prefer “memory and intelligence” so Core is not read as a generic
  local-LLM stack without a Memory substrate.
- **Truthfulness:** No Class A pack, public GitHub, MIG-*, Grounded Answers
  code, Act/agents, or Architecture Freeze reopen. Hashed freeze/spec/amendment/
  grounding/contract/GA files unchanged. No `MemoraApp/` edits.
- **Change control:**
  `docs/CHANGE_CONTROL_ADR046_ON_DEVICE_MEMORY_AND_INTELLIGENCE_INFRASTRUCTURE.md`.

### ADR-045 UNFYND Core as on-device intelligence infrastructure (docs-only)

- **Date:** 2026-08-29
- **Delivered:** Accepted ADR-045: living-canon and public product language for
  the substrate is **UNFYND Core — on-device intelligence infrastructure**
  (durable multimodal memory next to data that cannot leave; local-first;
  evidence-backed). Retires “Personal Knowledge Infrastructure,” “Personal
  Intelligence Infrastructure,” and third-party grant metaphors from new
  living-canon / CONTINUE / AGENTS identity / registry interpretation lines.
  PKI remains a prior internal noun where hashed docs or ADR-043 still use it;
  ADR-043 substance stands. Technical IDs unchanged (`com.memora.app`,
  `memora.db`, Keystore/MSAL hosts, GitHub/`Memora` folder).
- **Truthfulness:** No Class A publish, app open, MIG-*, Grounded Answers code,
  Act/agents, or Architecture Freeze reopen. Hashed freeze/spec/amendment/
  grounding/contract/GA files unchanged. No `MemoraApp/` edits.
- **Change control:**
  `docs/CHANGE_CONTROL_ADR045_ON_DEVICE_INTELLIGENCE_INFRASTRUCTURE.md`.

### MIG-04 MemoryBuilder contract boundary

- **Date:** 2026-08-28
- **Delivered:** Spec §4 `MemoryBuilder` gains `assemble` returning a
  schema-validated Memory (or pure build outcome). Deterministic assembly is
  `DeterministicMemoryBuilder` (Available; no AI Pack). Production drain path is
  `RunPendingAssetMemoryAssembly` → `AssembleAssetMemoryFromExtractionFacts` →
  `MemoryBuilder` with empty local observations. Non-empty observations fail
  clearly (not silently dropped). Assembly schema remains
  `asset-memory-facts-v4`. No Room migration. No user-visible Find change.
- **Truthfulness:** VisionEngine / observation processing, MIG-05+ embeddings
  store, RecallRanker operate, ranking/Find/Why UI, Links, Grounded Answers
  code, and package rename are not started. Hashed freeze/spec/amendment files
  unchanged. ADR-044 unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG04_MEMORY_BUILDER_CONTRACT.md`.

### ADR-044 low-power equals event-driven Memory lifecycle (docs-only)

- **Date:** 2026-08-28
- **Delivered:** Accepted ADR-044: on-device power posture is the event-driven
  Memory lifecycle (sparse index → cheap recall → rare deep thought), not
  neuromorphic silicon or always-on sensing. Cites Spec §7 / §9 and Freeze
  retrieval-first / truth-before-intelligence. Paused indexing under
  battery/storage/thermal/WorkManager constraints is correct product behavior
  with honest UI. Interpretation only.
- **Truthfulness:** No application, database, Room, retrieval, UI, or MIG-*
  code. Does not reopen Architecture Freeze, authorize Grounded Answers
  implementation, always-on camera/mic, or cloud AI on the core path. Hashed
  freeze/spec/amendment/grounding/contract files unchanged. ADR-043 Act-out
  unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ADR044_LOW_POWER_MEMORY_LIFECYCLE.md`.

### MIG-03 TIME and TOPIC Memory Anchors

- **Date:** 2026-08-28
- **Delivered:** `AssembleAssetMemoryFromExtractionFacts` emits TIME anchors
  from surviving EXIF `Date taken:` facts (`exif:fields`) and TOPIC anchors
  from `pdf:title` / `note:title` SOURCE_METADATA, in addition to TEXT.
  PERSON / PLACE / OBJECT / ACTIVITY / PURPOSE are not fabricated. Evidence
  class remains DIRECT. Assembly schema bumped to `asset-memory-facts-v4`
  (non-forced reindex). No Room schema migration.
- **Truthfulness:** Ranking/Find/Why do not filter by anchor kind. Links,
  Grounded Answers code, package rename, and MIG-04+ are not started. Hashed
  freeze/spec/amendment files unchanged. MIG-01 residual already closed
  (Room 12→13 device-verified).
- **Change control:** `docs/CHANGE_CONTROL_MIG03_TIME_TOPIC_ANCHORS.md`.

### MIG-01 residual closed (Room 12→13 device-verified)

- **Date:** 2026-08-28
- **Delivered (docs only):** `MemoraDatabaseMigrationTest` passed **2/2** on
  the Medium Phone emulator. Room 12→13 evidence-class backfill is
  device-verified. No application code change in this residual close.
- **Truthfulness:** Hashed freeze/spec/amendment files unchanged. MIG-03+ not
  claimed here.
- **Change control:** `docs/CHANGE_CONTROL_MIG01_RESIDUAL_CLOSED.md`.

### MIG-02 Remove Artificial Evidence Item/Length Caps

- **Date:** 2026-08-28
- **Delivered:** Asset Memory assembly no longer truncates to 8 evidence items.
  Every usable deterministic fact becomes `MemoryEvidence` (`DIRECT`). Per-item
  character bound is a pathological guard at 8192 chars (aligned with the
  provisional PDF page write budget), documented as not a completeness policy.
  `MAX_SUMMARY_CHARS` ≤ 240 remains display-only. Assembly schema bumped to
  `asset-memory-facts-v3` so the next legitimate reassembly gets richer
  evidence; no forced mass reindex of existing memories.
- **Truthfulness:** Search paths, embeddings, typed anchors, Grounded Answers,
  ranking/Find/Why UI, Links, package rename, and MIG-03+ are not started in
  this entry. Hashed freeze/spec/amendment files unchanged. MIG-01 Room 12→13
  instrumentation residual later closed separately (see Unreleased residual
  entry above).
- **Change control:** `docs/CHANGE_CONTROL_MIG02_EVIDENCE_CAPS.md`.

### MIG-01 Evidence Class Taxonomy (Room 12→13)

- **Date:** 2026-08-24
- **Delivered:** Additive `MemoryEvidenceClass` on Asset Memory evidence
  (`DIRECT`, `VALIDATED_OBSERVATION`, `RETRIEVAL_SIGNAL`, `HYPOTHESIS`). All
  evidence currently assembled from deterministic extraction facts is tagged
  `DIRECT`. Existing `memory_evidence` rows backfill `DIRECT` via Room
  migration 12→13. Construction without an explicit class does not compile.
- **Truthfulness:** No ranking, Find, Why, or UI change. Class is not used
  for display or link eligibility. Links remain unimplemented. Hashed
  freeze/spec/amendment files unchanged. ADR-043 Act-out unchanged.
- **Change control:** `docs/CHANGE_CONTROL_MIG01_EVIDENCE_CLASS.md`.

### ADR-043 PKI vision vs freeze; Act remains out (docs-only)

- **Date:** 2026-08-24
- **Delivered:** Accepted ADR-043: PKI is the north star; search is one
  capability; Android is the first reference implementation. Frozen architecture
  is suitable through See/Remember, staged Connect, retrieve-by-meaning, and
  Understand / converse-as-Q&A. Act remains out of current architecture. MIG-*
  not started. Hashed freeze/spec/amendment files unchanged.
- **Truthfulness:** No application, database, migration, retrieval, or package
  rename. Historical changelog entries unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ADR043_PKI_VISION_ALIGNMENT.md`.

### ADR-042 Freeze vs Grounding and GOVERNANCE order (docs-only)

- **Date:** 2026-08-24
- **Delivered:** Accepted ADR-042: Freeze §1 exclusivity is the Asset-Memory /
  PKI core; Grounding Architecture remains the sole Grounded Answers
  constitution; GOVERNANCE numbered order aligned with Freeze §2; Freeze §7
  §15/§7 deferral recorded as errata without rewriting hashed specs. MIG-*
  not started.
- **Truthfulness:** No application, database, migration, retrieval, or package
  rename. Historical changelog entries unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ADR042_FREEZE_GROUNDING.md`.

### Architecture Freeze v1.0 and Migration Spec V1 registration (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Registered `ARCHITECTURE_FREEZE_v1.0.md` and
  `ARCHITECTURAL_MIGRATION_SPEC_V1.md` with SHA-256 hashes; ADR-041 records
  registration and open authority conflicts. No MIG-* implementation.
- **Truthfulness:** No application, database, migration, retrieval, or package
  rename. Historical changelog Memora wording unchanged.
- **Change control:** `docs/CHANGE_CONTROL_ARCHITECTURE_FREEZE_REGISTRATION.md`.

### UNFYND identity Step 5 — presentation/entry identifier rename

- **Date:** 2026-08-23
- **Delivered:** `UnfyndApplication`, `UnfyndTheme` / `Theme.Unfynd`, and
  Compose entry `UnfyndApp` / `UnfyndAppReady` / `UnfyndWelcomeScreen`. PascalCase
  Unfynd*, not UNFYND*.
- **Truthfulness:** No `applicationId`/namespace, database, MSAL host, Gradle folder,
  or user-visible string changes in this checkpoint. Emulator not claimed here.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP5_PRESENTATION_NAMES.md`.

### UNFYND identity Step 4 — user-visible Android product-noun copy

- **Date:** 2026-08-23
- **Delivered:** Launcher label and user-visible product-as-subject copy now UNFYND
  (strings, Compose/copy objects, notices header, debug SAF fixture title, matching
  unit copy tests). Resource IDs such as `clear_memora_index`, classes, packages,
  and `applicationId` unchanged.
- **Truthfulness:** No Gradle identity, database, MSAL host, Theme/Application
  rename (Step 5), or emulator claim in this checkpoint.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP4_ANDROID_COPY.md`.

### UNFYND identity Step 3 — living-canon product-noun overlay (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Overlay of product-as-subject Memora → UNFYND on living constitutions
  and amendments, with identity headers. Domain language (Memory, Find, Evidence
  Package) unchanged. Historical ADR-001–039 bodies and `.docx` hashes unchanged.
- **Truthfulness:** No Kotlin, XML strings, Gradle, package, database, or GitHub
  rename; no Grounded Answers or Event/Knowledge implementation.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP3_CANON_OVERLAY.md`.

### UNFYND identity Step 2 — architecture registry (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Inventoried and registered frozen architecture Markdown (hashes of
  Git blobs). Grounded Answers files now tracked; Experience Memory and Local-AI Spec
  hashes recorded; no competing Grounded Answers constitution.
- **Truthfulness:** No product-noun overlay (playbook Step 3); no package, database,
  Gradle, Kotlin, or UI-string rename; historical `.docx` hashes unchanged.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_STEP2_ARCHITECTURE_REGISTRY.md`.

### UNFYND identity ADR-040 (docs-only)

- **Date:** 2026-08-23
- **Delivered:** Accepted ADR-040: product/brand name is UNFYND (formerly Memora);
  PKI north star; Android is a milestone; domain language and technical IDs
  unchanged. Registry interpretation updated; historical `.docx` hashes unchanged.
- **Truthfulness:** No constitution overlay; no package, database, Gradle, Kotlin,
  or UI-string rename; no new architecture-file hashes.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_ADR040.md`.

### UNFYND identity transition playbook (docs-only)

- **Date:** 2026-08-22
- **Delivered:** Operating playbook for UNFYND product identity (formerly Memora),
  Personal Knowledge Infrastructure direction, Android as a milestone, branding
  overlay on frozen architecture, and one-step-at-a-time execution. No identity ADR.
- **Truthfulness:** No package, database, Gradle, Kotlin, or UI-string rename.
- **Change control:** `docs/CHANGE_CONTROL_UNFYND_IDENTITY_PLAYBOOK.md`.

### Grounded Answers architecture constitution (docs-only)

- **Date:** 2026-08-05
- **Delivered:** Canonical `GROUNDING_ARCHITECTURE.md`; `GROUNDED_ANSWERS_AMENDMENT_V1.md`;
  PDF slice acceptance spec; ADR-033–039; G-01–G-08 traceability; Local-AI Spec §9
  carve-out; Experience Memory §10 status refresh; registry/CONTINUE/ROADMAP updates.
- **Truthfulness:** No ReasoningEngine, generative pack, or Ask UI code. Find remains
  non-generative. Implementation blocked on readiness gates in grounding architecture.
- **Change control:** `docs/CHANGE_CONTROL_GROUNDED_ANSWERS_ARCHITECTURE.md`.

### PDF local-reading progress counts

- **Date:** 2026-08-04
- **Delivered:** Pending PDF count at drain start; live “N of M” progress from
  WorkManager unit successes; completed copy includes drained count.
- **Truthfulness:** Aggregate integers only; no AVAILABLE flip.
- **Change control:** `docs/CHANGE_CONTROL_PDF_LOCAL_READING_PROGRESS.md`.

### Progress status refresh (README / CONTINUE / ROADMAP)

- **Date:** 2026-08-04
- **Delivered:** Honest current-status snapshot aligned to shipped meaning path,
  index progress/cap, M4 pending, and green CI — no AVAILABLE claim change.

### Fix: E5d boost unit-test fixtures (CI)

- **Date:** 2026-08-04
- **Delivered:** Corrected FOXTROT/mira page-vector fixtures so bounded token
  boost can flip ranking; GitHub Actions on `main` green (`970fcf9`).

### Meaning index progress + per-tap cap UI

- **Date:** 2026-08-04
- **Delivered:** Named ≤25 memories/tap batch; live summary/page progress copy;
  remaining READY → tap Build again; bottom Back on disclosure screen; unit tests.
- **Truthfulness:** Cap is per tap, not a library ceiling; no AVAILABLE flip.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_INDEX_PROGRESS_CAP.md`.

### Midrange meaning measurement gate (M4 plan)

- **Date:** 2026-08-04
- **Delivered:** M4 runbook + product AVAILABLE decision checklist; compatibility
  policy honesty for USE on emulator vs pending midrange.
- **Truthfulness:** No AVAILABLE flip; midrange row still ⬜ until device run.
- **Change control:** `docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md`.

### M3 USE meaning PDF page-recall baseline

- **Date:** 2026-08-04
- **Delivered:** Live USE scoring of `meaning-pdf-page-recall-v1` on
  `emulator_medium_phone` (prefer product install).
- **Outcome:** Cosine-only 2/3 (up from compact 0/3); boosted 3/3; keep E5d
  assist. No AVAILABLE UI flip.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_USE.md`.

### E4b Universal Sentence Encoder product embedder

- **Date:** 2026-08-04
- **Delivered:** Product model spec/download/store/disclosure switched to
  MediaPipe USE (ADR-032); legacy average-word cleanup; honesty copy; unit tests.
- **Smoke:** Emulator `mira` → Matched page 5 → Open Page 5 of 5 (accepted).
- **Truthfulness:** Candidate meaning path; not measured AVAILABLE; rebuild index
  after upgrade; E5d assist retained pending M3.
- **Change control:** `docs/CHANGE_CONTROL_E4B_UNIVERSAL_SENTENCE_ENCODER.md`.

### Fix: Done after local PDF reading returns to Welcome

- **Date:** 2026-08-04
- **Delivered:** Completed local reading Done navigates to Welcome instead of
  resetting to Start local reading; optional Read folder again.

### M2 on-device meaning PDF page-recall baseline

- **Date:** 2026-08-04
- **Delivered:** Live MediaPipe scoring of `meaning-pdf-page-recall-v1` on
  `emulator_medium_phone`; disposable model staging; unit + instrumentation.
- **Outcome:** Cosine-only 0/3; E5d-boosted 3/3; recommends E4b. No AVAILABLE
  UI flip (emulator tier ≠ midrange marketing).
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_ON_DEVICE.md`.

### M1 meaning PDF page-recall baseline (JVM harness)

- **Date:** 2026-08-04
- **Delivered:** Labeled corpus `meaning-pdf-page-recall-v1`;
  `MeasureMeaningPdfPageRecallBaseline` (cosine-only vs E5d boost page hit@1);
  `MeaningEvidenceTokenBoost` in domain; unit tests green.
- **Outcome:** Cosine-only below interim bar → E4b recommended for
  semantic-only; boosted path clears labeled @1. No product AVAILABLE flip.
- **Follow-up:** M2 on-device MediaPipe measurement.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_BASELINE.md`.

### E5d evidence-token boost for candidate meaning ranking

- **Date:** 2026-08-04
- **Delivered:** Disclosed bounded boost when a significant cue token appears in
  ranked evidence text; Why copy discloses assist; unit tests.
- **Truthfulness:** Hybrid candidate assist — not keyword Find alone, not
  measured AVAILABLE; E4b still the path for stronger semantic-only ranking.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_EVIDENCE_TOKEN_BOOST.md`.

### E5c index-time PDF page embeddings

- **Date:** 2026-08-04
- **Delivered:** Room v12 `pdf_page_embeddings`; Build meaning index also embeds
  capped PDF pages; Find by meaning ranks summary+page with per-asset dedup;
  hit shows Matched page N; Open uses ranked page; candidate honesty copy.
- **Verification:** Search page-prefer unit test + meaning copy tests (pending
  full suite / smoke).
- **Truthfulness:** Rebuild meaning index required; not measured AVAILABLE.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_EMBEDDINGS.md`.

### Enterprise completion — meaning PDF page recall (backlog)

- **Date:** 2026-08-04
- **Recorded:** Checklist for finishing enterprise-grade multi-page PDF meaning
  recall: E5c index-time page/chunk embeddings; measured ADR-024/025 gate;
  E4b if needed. E5b2e documented as interim only.
- **Change control:** `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md`.

### Meaning PDF cue-best page open (E5b2e)

- **Date:** 2026-08-04
- **Delivered:** On meaning Open original for PDFs, re-rank saved page texts
  against the cue with the on-device embedder; open cue-best page when it
  clearly beats the Memory cite; else keep E5b2d cite/fallback. Durable
  backlog for index-time page/chunk embeddings (E5c).
- **Verification:** ResolveMeaningPdfOpenPage unit tests; meaning copy/VM tests.
- **Truthfulness:** Open-time cue-best ≠ Find-by-meaning page ranking; not
  measured AVAILABLE.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_PDF_CUE_BEST_PAGE_OPEN.md`.

### Meaning PDF open uses cited page (E5b2d)

- **Date:** 2026-08-03
- **Delivered:** Meaning-search Open original for PDFs opens the page cited by
  the Memory summary evidence locator (`pdf:page:N`); honest page-1 fallback
  when no cite; Why / hit copy reflect the cited page.
- **Verification:** locator parser, open page resolve, meaning search cite
  flow, and copy unit tests.
- **Truthfulness:** Cited summary page ≠ query-best page ranking.

### Meaning-quality Asset Memory summaries (E5b2c)

- **Requirements:** A-05; ADR-029, ADR-031.
- **Delivered:** No dimension-only EXIF memories; OCR/text-first summaries
  (assembly schema v2); meaning index/lookups scoped to current schema.
- **Verification:** assembly unit tests; full debug unit suite.
- **Truthfulness:** Rebuild memories + meaning index required for existing
  devices; not a measured AVAILABLE claim.
- **Change control:** `docs/CHANGE_CONTROL_MEANING_MEMORY_SUMMARY_QUALITY.md`.

### Local-AI embedding-first track E5b2b open original from meaning hits

- **Requirements:** A-05; ADR-031.
- **Delivered:** Open original on meaning hits (screenshot/photo in-app preview,
  PDF page 1 with honesty, note OneNote/browser launch).
- **Verification:** meaning open ViewModel + copy unit tests; full debug unit suite.
- **Truthfulness:** Ranking still on indexed memories; PDF page cite not invented.

### Local-AI MediaPipe 16 KB page-size fix

- **Requirements:** A-01; ADR-031.
- **Delivered:** Bump `tasks-text` 0.10.14 → 0.10.29 for 16 KB ELF alignment;
  load availability off the main thread after download.
- **Verification:** debug install on 16 KB emulator; unit suite still green.
- **Truthfulness:** Prior crash was native MediaPipe on 16 KB AVD after a
  successful model download — not a failed download.

### Local-AI embedding-first track E5b2 Find-by-meaning candidate recall

- **Requirements:** A-01, A-05; ADR-024, ADR-025, ADR-029, ADR-031.
- **Delivered:** Welcome → Find by meaning; cosine candidate ranking over indexed
  Asset Memories; Why-this-result cites stored summary + cue; readiness honesty.
- **Verification:** ranking + ViewModel + copy unit tests; full debug unit suite.
- **Truthfulness:** Compact candidate path only — not measured AVAILABLE; open
  original from meaning hits deferred.

### Local-AI embedding-first track E5b1 MediaPipe embedder + model download

- **Requirements:** A-01, A-02, A-07; ADR-029, ADR-031.
- **Delivered:** MediaPipe Text Embedder adapter; private model download after
  disclosure; build meaning index CTA; dependency review for tasks-text 0.10.14.
- **Verification:** full `:app:testDebugUnitTest` green; debug compile with
  MediaPipe (`createFromFile` private model path).
- **Truthfulness:** Compact model disclosed; Find-by-meaning UI not shipped yet;
  model not bundled in APK.

### Local-AI embedding-first track E5a embedding index foundation

- **Requirements:** A-01, A-02; ADR-029, ADR-030.
- **Delivered:** `EmbeddingEngine.embedText` contract; `memory_embeddings` Room
  v11; `IndexMemoryEmbeddings` refuses writes while Unavailable; cosine helper;
  Hilt still binds Unavailable embedding engine.
- **Verification:** embedding + index unit tests (engineering gate).
- **Truthfulness:** No meaning-search UI or AVAILABLE claim.

### Local-AI embedding-first track E4a offline pack-container activate

- **Requirements:** A-01, A-07; ADR-023, ADR-029, ADR-030.
- **Delivered:** Offline embedding pack-container fixture; private payload store;
  activate use case; Welcome disclosure CTA to verify/store; clear-index removes
  pack files. EmbeddingEngine stays Unavailable.
- **Verification:** activate + disclosure ViewModel unit tests (engineering gate).
- **Truthfulness:** ACTIVE install record ≠ meaning search AVAILABLE; no INTERNET.

### Local-AI embedding-first track E3 disclosure UI

- **Requirements:** A-01, A-03, A-07; ADR-023, ADR-029.
- **Delivered:** Welcome → About on-device meaning search; honesty screen with
  planned size/storage/license; affirmative acknowledge into Room ledger; no
  download or AVAILABLE claim.
- **Verification:** disclosure copy + ViewModel unit tests (engineering gate);
  optional user smoke on Welcome disclosure.
- **Truthfulness:** Meaning search stays off; keyword recall unchanged.

### Local-AI embedding-first track E2 Room install ledger

- **Requirements:** A-01, A-07; ADR-023, ADR-029.
- **Delivered:** Room v10 `ai_pack_install_ledger`; `MIGRATION_9_10`;
  `RoomAiPackInstallLedger`; Hilt `AiPackManager` = ledger-backed (empty ⇒
  NOT_INSTALLED).
- **Verification:** ledger unit tests; Room integration + migration on emulator.
- **Truthfulness:** No disclosure UI, download, AVAILABLE, or model vendor.

### Local-AI embedding-first track E0–E1 (ADR-029)

- **Requirements:** A-01, A-02, A-03, A-07; ADR-023, ADR-024, ADR-025, ADR-029.
- **Delivered:** Embeddings-first phase plan; domain AI Pack install ledger
  (disclosure → verify → ACTIVE/fail, retain prior known-good);
  `LedgerBackedAiPackManager`; planned `memora-embedding-pack-v1` id.
- **Verification:** `AiPackInstallLedgerTest` JVM (engineering gate).
- **Truthfulness:** No download, Room bind, AVAILABLE UI, or model vendor yet.

### Local-AI measured pack baselines L2 offline integrity + matrix honesty

- **Requirements:** A-01, A-03, A-05, A-07; ADR-023, ADR-024, ADR-025.
- **Delivered:** L2 harness — truncated-payload reject, prior known-good after
  corrupt update, `OFFLINE_CORE_PATH_OK`, emulator support-matrix rows remain
  UNSUPPORTED; compatibility policy draft notes. Product engines stay Unavailable.
- **Verification:** L2 unit + `SyntheticAiPackL2BaselineIntegrationTest` on
  emulator 2026-08-02 (engineering gate).
- **Truthfulness:** No AVAILABLE claim; midrange SUPPORTED rows deferred.

### Local-AI measured pack baselines L1 synthetic integrity harness

- **Requirements:** A-01, A-03, A-07; ADR-023, ADR-025.
- **Delivered:** SHA-256 pack payload verifier; synthetic fixture corpus;
  aggregate baseline measurement (size + integrity failure retained); emulator
  instrumentation staging under no-backup private storage. Product pack manager
  stays Unavailable.
- **Verification:** `:app:testDebugUnitTest` intelligence tests passed;
  `SyntheticAiPackBaselineIntegrationTest` passed on emulator 2026-08-02.
- **Truthfulness:** No AVAILABLE claim; no real model; Vision remains Unavailable.

### Local-AI measured pack baselines L0 opened

- **Requirements:** A-01, A-03, A-05, A-07, E-06; ADR-023, ADR-024, ADR-025.
- **Delivered:** Change-control opened with L0/L1/L2 phase plan; honesty gates;
  no pack/harness code.
- **Verification:** Docs-only; **accepted** 2026-08-02 (proceed to L1).
- **Truthfulness:** No AVAILABLE intelligence claim; Notes/keyword recall ≠ Local AI.

### Notes connector N7 Open original OneNote page

- **Requirements:** P-03, P-08, P-19; ADR-002, ADR-003.
- **Delivered:** Find saved note text → Open original note via Graph page
  `links`; prefers OneNote app when installed, else browser; MSAL WebView Connect;
  silent token refresh; IO timeout against ANR; search remains offline Room-only.
- **Verification:** Unit tests passed; emulator Open → OneDrive web **accepted**
  by user 2026-08-02 (no OneNote app on AVD).
- **Truthfulness:** Open may need network and Microsoft session; not in-app preview.

### Notes connector N6 Build-memories NOTE facts

- **Requirements:** P-03, P-08, P-19; ADR-003, ADR-007, ADR-019.
- **Delivered:** Build memories from saved OneNote page text (`NOTE_TEXT` /
  `note:page`, optional title metadata); pending drain includes notes with
  non-blank extracts; setup honesty updated.
- **Verification:** Unit tests passed; emulator **28** Asset Memories + OneNote
  copy **accepted** by user 2026-08-02.
- **Truthfulness:** Pre-AI cited facts only — not meaning-based ranking.

### Notes connector N5 Find saved note text

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Welcome **Find saved note text**; Room keyword search over
  saved OneNote page text; Why evidence; offline after extract; no Graph in search.
- **Verification:** Unit tests passed; emulator readiness 28 pages + `pass` →
  1 match + Why **accepted** by user 2026-08-02.
- **Truthfulness:** Keyword matching ≠ meaning-based Memory recall.

### Notes connector N4 page text extract

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Room `note_page_extractions` (v9); Graph HTML → plain text;
  user-started Extract drain; honesty “not searchable yet.”
- **Verification:** Unit tests passed; emulator Extract 29/29 for
  `mir.m@outlook.com` **accepted** by user 2026-08-01.
- **Truthfulness:** Extracted text becomes keyword-searchable in N5; still not
  meaning-based Memory recall.

### Notes connector N3 page discovery placeholders

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** User-started Discover OneNote pages via Graph (sections →
  per-section pages) into `AssetType.NOTE` placeholders; bounded pages; vaulted
  session + `ensureSession`; honest “not searchable yet.”
- **Verification:** Unit tests passed; emulator Connect + Discover (25 then 28
  placeholders) **accepted** by user 2026-08-01.
- **Truthfulness:** Placeholders ≠ extracted text ≠ searchable notes.

### Notes connector N2b MSAL Connect / Disconnect

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** MSAL 8.4.1; INTERNET for Microsoft source access; Connect /
  Disconnect OneNote; Keystore vault + MSAL cache clear; scopes Notes.Read,
  User.Read, offline_access. No Graph discovery yet (N3).
- **Verification:** Unit tests passed; emulator Connect (`mir.m@outlook.com`) +
  Disconnect accepted by user 2026-08-01.
- **Truthfulness:** Connected ≠ notes indexed/searchable.

### Notes connector N2a vault + registration gate

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Keystore-backed OneNote session vault; `BuildConfig` client ID /
  signature hash from `local.properties`; Notes status copy for registration
  required / disconnected / session-present; Disconnect + clear-index clears
  vault. Azure runbook + planned MSAL review. **No** MSAL, INTERNET, or Graph.
- **Verification:** Unit tests for copy, config, in-memory vault, secret absence.
  Emulator smoke pending user.
- **Truthfulness:** Does not claim notes are searchable; Connect not offered until
  N2b after Azure registration.

### Notes connector N1 honesty UI

- **Requirements:** P-03, P-08, P-19; ADR-003.
- **Delivered:** Welcome → About Notes indexing honesty screen; OneNote + network
  framing; explicit “not indexed yet”; no Connect / MSAL / Graph.
- **Verification:** `NotesConnectorHonestyCopyTest` passed; emulator UI + Back
  accepted by user 2026-08-01.
- **Truthfulness:** Does not claim notes are searchable or connected.

### Notes connector N0 opened (OneNote-class)

- **Requirements:** P-03, P-08, P-15, P-19; ADR-001, ADR-003, ADR-004.
- **Delivered:** `docs/CHANGE_CONTROL_NOTES_ONENOTE_CONNECTOR.md` — phased plan
  N0–N5, network/token honesty, Azure app-registration as N2 external gate.
  Traceability P-08/P-19 and ADR-003 rule updated; CONTINUE points next code at N1.
- **Verification:** Docs-only; no Notes implementation code.
- **Truthfulness:** Do not claim phone-wide notes indexing; OneNote connector only.

### ADR-003 Notes strategy accepted

- **Requirements:** Notes MVP honesty; ADR-001; ADR-003.
- **Delivered:** ADR-003 accepted — OneNote-class read-only provider connector as
  the Notes path; Share-as-indexing and arbitrary note-app scanning rejected.
- **Verification:** Decision recorded; no Notes code in this slice.
- **Truthfulness:** Do not claim all phone notes are indexed until the connector
  ships under change control.

### Screenshot OCR open-original acceptance

- **Requirements:** P-01, P-06, P-11, P-13, P-17; A-01, A-02, A-05.
- **Delivered:** Documentation gate closed for capped read-only Open preview from
  screenshot OCR hits (feature already on `main`).
- **Verification:** Unit tests re-verified 2026-07-31; interactive emulator UI
  accepted same day (SwiftShader; user pass).
- **Truthfulness:** Search still uses saved OCR only; originals read-only.

### Evidence-backed Asset Memory persistence

- **Requirements:** P-02, P-09, P-11, P-14, P-17; A-02, A-04; E-04, E-05.
- **Delivered:** Room v8 persists immutable current-fingerprint Asset Memory revisions
  with normalized evidence, extraction schemas, TEXT anchors, and citation joins.
  An explicit bounded setup action assembles them only from saved PDF text/metadata,
  screenshot/photo OCR, and useful EXIF fields.
- **Verification:** `:app:testDebugUnitTest` passed on 2026-07-31, including
  deterministic assembler, revision-history, and normalized Room mapping tests.
  The v1→v8 migration test is updated; emulator execution remains pending.
- **Truthfulness:** No source reopen, network, Local-AI pack, embeddings, semantic
  ranking, natural-language recall, or confidence claim. Existing keyword search
  remains the interim recall UI.

### Photo OCR extract and keyword search

- **Requirements:** P-01, P-04, P-05, P-06, P-11, P-13, P-14, P-15, P-17;
  A-01, A-02, A-05, A-06; ADR-028.
- **Delivered:** Room v7 adds separate `photo_ocr_extractions`; explicit Read text
  from photos drains PHOTO assets through bundled on-device Latin OCR. Welcome →
  Find saved photo text provides current-fingerprint keyword search, readiness,
  cancel, clear, Why citation, and capped read-only Open original preview.
- **Verification:** `:app:testDebugUnitTest`, Android-test compilation, and debug
  assembly passed on 2026-07-31. Targeted migration execution was blocked before
  tests by emulator package-service `Broken pipe (32)`; visible verification pending.
- **Truthfulness:** PHOTO and SCREENSHOT tables/flows remain separate. Keyword
  matching is not natural-language or semantic Memory recall; no network or AI Pack.

### Public positioning and CI maturity

- **Requirements:** Docs / delivery hygiene; ADR-027.
- **Delivered:** README and GitHub About use the approved on-device personal AI
  memory positioning copy. Lightweight GitHub Actions CI runs
  `testDebugUnitTest` on `main` pushes and PRs.
- **Verification:** Workflow file present; first green run pending after push.
- **Truthfulness:** Public category examples do not expand MVP delivery scope
  (ADR-027).

### Screenshot OCR open-original preview

- **Requirements:** P-01, P-06, P-11, P-13, P-17; A-01, A-02, A-05.
- **Delivered:** From Find saved screenshot text hits, Open original shows a
  hard-capped (960px long edge) read-only in-app preview (or honest
  SourceUnavailable / CouldNotOpen). Search still uses saved OCR text only.
- **Verification:** Unit tests + install pending; emulator confirmation pending.
- **Truthfulness:** Read-only; no edit/upload; keyword search does not reopen
  images for matching; PHOTO out of scope.

### Screenshot OCR keyword search

- **Requirements:** P-01, P-06, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Welcome → Find saved screenshot text searches current-fingerprint
  `screenshot_ocr_extractions.full_text` (LIKE, ADR-022). Readiness, cancel, clear,
  Why citation, highlight. Open-original preview is a follow-on slice.
- **Verification:** Unit tests green; emulator confirmation 2026-07-29 (query
  `note` → 1 match with excerpt + Why on Medium Phone). Reconfirmed 2026-07-30
  after emulator recovery.
- **Truthfulness:** Keyword matching only; ordinary photos excluded.

### Screenshot OCR extract

- **Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** After photo catalogue + EXIF facts, Read text from screenshots
  drains `SCREENSHOT` assets via WorkManager, opens permitted URIs read-only for
  bundled ML Kit Latin OCR, persists `screenshot_ocr_extractions` (Room v6). Honest
  copy — no keyword/Memory claims; PHOTO OCR out of scope.
- **Verification:** Unit tests green; `installDebug` succeeded; emulator
  confirmation 2026-07-28 (3 catalogued; EXIF for 3; OCR text for 1 screenshot;
  honest OCR-only copy).
- **Truthfulness:** Discovery remains metadata-only (ADR-009); OCR is a separate
  explicit step (ADR-026). Keyword search over OCR remains later.

### MediaStore image EXIF extract

- **Requirements:** P-04, P-05, P-06, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** After photo catalogue completes, Read photo facts drains PHOTO/
  SCREENSHOT assets via WorkManager, opens permitted URIs read-only for ExifInterface,
  persists `image_exif_extractions` (Room v5). Honest copy — no OCR/keyword/Memory claims.
- **Verification:** Unit tests green; `installDebug` succeeded; emulator confirmation
  2026-07-28 (3 fixture photos catalogued; Read photo facts saved basic facts;
  honest EXIF-only copy).
- **Truthfulness:** Discovery remains metadata-only (ADR-009); extract is a separate
  explicit step. OCR remains a later governed capability.

### Keyword search accessibility baseline

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05; Local-AI §11.
- **Delivered:** Headings, polite live regions for search/open statuses, merged
  progress announcements, richer preview image content description on keyword
  search + cited-page preview screens.
- **Verification:** Unit tests green (`PdfKeywordSearch*`); `installDebug` succeeded;
  emulator confirmation 2026-07-28 (user: all pass on sighted flow).
- **Truthfulness:** Keyword search + preview only; not a full-app TalkBack audit.

### Cancel in-flight keyword search

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Dedicated Cancel search while Searching (separate from Search);
  returns Idle keeping typed query; late completions ignored; search Job
  cancelled; short minimum Searching visibility (~700ms) on tiny indexes.
- **Verification:** Unit tests green (`PdfKeywordSearch*`); `installDebug` succeeded;
  emulator confirmation 2026-07-28 (user: pass) — dedicated Cancel search stops
  Searching; query kept; no results.
- **Truthfulness:** Interim keyword path only; does not cancel PDF preview Opening.

### Keyword search clear query

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Clear control on the query field when text is present; clears
  results/Why/open state; typed query also clears after Clear Memora index.
- **Verification:** Unit tests green (`PdfKeywordSearch*`); `installDebug` succeeded;
  emulator confirmation 2026-07-28 (user: all pass).
- **Truthfulness:** Interim keyword path only; does not cancel in-flight search.

### Open original PDF from keyword result (in-app cited-page preview)

- **Requirements:** P-01, P-11, P-13, P-17; A-02, A-05.
- **Delivered:** Per-hit Open original PDF opens a read-only in-app preview of the
  cited page via SAF + `PdfRenderer`; Opening / SourceUnavailable / CouldNotOpen
  feedback; Back returns to results. Search still uses stored text only.
- **Verification:** On 2026-07-28, unit tests passed; debug APK installed. User
  confirmed on Medium Phone (Documents/MemoraFixtures): keyword hit page labels,
  Open original in-app cited-page preview, Back to results, Why unchanged.
- **Truthfulness:** No external viewer page-jump promise; originals remain
  read-only; no AI/network.

### Keyword search IME gate + Searching progress

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Shared `canSubmitSearch` gate for button and keyboard Search;
  field read-only while searching; calm Searching progress copy with spinner.
- **Verification:** On 2026-07-27, search unit tests; debug APK installed. User
  confirmed on Medium Phone: blank keyboard Search blocked; Searching progress
  copy + locked field; results/Why unchanged.
- **Truthfulness:** Interim keyword path only; no AI/network.

### Keyword blank-query guidance + Search button gating

- **Requirements:** P-01, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Find saved PDF text disables Search while the query is blank and
  shows inline empty-query guidance in Idle; non-blank query unchanged.
- **Verification:** On 2026-07-27, search unit tests; debug APK installed. User
  confirmed on Medium Phone: blank → disabled Search + guidance; typed query →
  Search/Why still work.
- **Truthfulness:** Interim keyword path only; no AI/network.

### Keyword search corpus readiness

- **Requirements:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Find saved PDF text shows current-fingerprint page/document
  counts ready for keyword search; refreshes on open and after clear.
- **Verification:** On 2026-07-27, search readiness/copy/ViewModel unit tests;
  debug APK installed. User confirmed on Medium Phone: empty/searchable/clear
  readiness states all display honestly, and search/Why still work.
- **Truthfulness:** Interim keyword inventory only; not Memory recall; no AI/network.

### Keyword search clear invalidation

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Clear Memora index ack resets Find saved PDF text to Idle
  (invalidates in-flight search) so Results/Why cannot cite deleted excerpts.
- **Verification:** On 2026-07-27, ViewModel clear unit tests; debug APK
  installed. User confirmed on Medium Phone: clear drops stale Results/Why.
- **Truthfulness:** Interim keyword path; typed query may remain; no AI/network.

### Keyword excerpt match highlight

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** First case-insensitive query span bold/primary in each result
  excerpt; Why this result? unchanged.
- **Verification:** On 2026-07-27, support/highlight unit tests; debug APK
  installed. User confirmed on Medium Phone: meet highlighted in fixture excerpts.
- **Truthfulness:** Interim keyword path; first occurrence only; no AI/network.

### Clear-index UI recovery (PDF Index + Local reading + live DB)

- **Requirements:** P-04, P-05, P-14, P-15, P-17; A-02, A-06.
- **Delivered:** Ignore stale finished SAF discovery work after clear/reconnect so
  Index this folder returns; cancel Memora WorkManager tags on clear; Completed
  local reading shows Done; reset local-reading session on clear; extract/search
  use live MemoraDatabaseHandle (no closed-DB hang).
- **Verification:** On 2026-07-27, DocumentTree/local-reading/search unit tests;
  debug APK installed. User confirmed on Medium Phone: Index returns after
  clear+reconnect; Done after reading; search works.
- **Truthfulness:** Grants still survive clear; no AI/network.

### Keyword empty-corpus honesty + search hang recovery

- **Requirements:** P-01, P-11, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Distinct Nothing saved for search yet vs true no-match; live DB
  resolution; SearchCouldNotFinish instead of endless spinner.
- **Verification:** Unit tests; user confirmed search after clear/rebuild on
  Medium Phone (2026-07-27).
- **Truthfulness:** Interim keyword path only.

### Keyword search submitted-query coherence

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Editing the query clears stale Results/Why; results summary and
  no-match copy name the submitted query; superseded in-flight searches ignored.
- **Verification:** On 2026-07-26, search ViewModel/copy unit tests; debug APK
  installed. User confirmed on Medium Phone: Results/Why for "meet mira", then for
  "meet" without citing the prior query.
- **Truthfulness:** Interim keyword path only; no AI/network.

### Keyword Why document-label provenance

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Why this result? cites the same saved document label shown on the
  result card, plus query/page/excerpt; still keyword-not-meaning.
- **Verification:** On 2026-07-26, unit copy tests; debug APK installed. User
  confirmed on Medium Phone: Why cites each fixture PDF name for meet mira.
- **Truthfulness:** Interim keyword path; label from Room metadata only; no PDF reopen.

### Keyword recall match-count / cap honesty

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-02, A-05.
- **Delivered:** Results summary with listed match count; honest at-most-20
  disclosure when the search cap is reached; Why this result? unchanged.
- **Verification:** On 2026-07-26, unit copy tests; debug APK installed. User
  confirmed on Medium Phone: meet mira → Showing 2 matches for two fixture PDFs.
- **Truthfulness:** Interim keyword path only; no total-corpus count query; no AI.

### Local-AI benchmark plan (architecture gate)

- **Requirements:** A-01, A-02, A-05, A-06, A-07, E-06; Local AI Technical Spec §11.
- **Delivered:** `docs/LOCAL_AI_BENCHMARK_PLAN.md` (ADR-025); domain benchmark
  metric/claim contracts forbidding unmeasured release promises; unit tests.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed. No new
  AI/OCR/network Gradle deps.
- **Truthfulness:** No latency/battery/storage SLA accepted; measured pack
  baselines still required before AVAILABLE claims.

### Local-AI compatibility/fallback policy (architecture gate)

- **Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §11/§12/§13.4.
- **Delivered:** `docs/LOCAL_AI_COMPATIBILITY_FALLBACK_POLICY.md` (ADR-024); domain
  support tiers/default unsupported resolver; forbidden silent semantic fallbacks;
  unit tests.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed. No new
  AI/OCR/network Gradle deps.
- **Truthfulness:** Defaults all intelligence capabilities to UNSUPPORTED; no
  AVAILABLE claim from discovery/keyword paths alone.

### AI Pack delivery/security plan (architecture gate)

- **Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §6/§13.3.
- **Delivered:** `docs/AI_PACK_DELIVERY_SECURITY_PLAN.md` (ADR-023); domain
  AiPackManifest / install state / verification contracts; UnavailableAiPackManager
  stub; unit validation tests. No download, INTERNET, models, or install UI.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed (including
  AiPackContractsTest). No new AI/OCR/network Gradle deps.
- **Truthfulness:** Does not claim packs can install or that understanding is ready;
  compatibility/fallback policy and Local-AI benchmarks still open for gate exit.

### Local-AI capability interfaces (architecture gate)

- **Requirements:** A-01, A-02, A-03; Local AI Technical Spec §4.
- **Delivered:** Domain CapabilityAvailability/Id types; Spec §4 VisionEngine,
  OcrEngine, DocumentEngine, EmbeddingEngine, MemoryBuilder, RecallRanker;
  truthful unavailable stubs; unit availability tests. No models, OCR SDKs,
  embeddings, network, or AI Pack downloads.
- **Verification:** On 2026-07-25, domain intelligence unit tests passed. No new
  AI/OCR/network Gradle deps.
- **Truthfulness:** Does not claim on-device understanding is ready; A-01/A-07 and
  full Local-AI gate exit remain open (packs, fallback matrix, benchmarks).

### Keyword search Why this result? (stored evidence)

- **Requirements:** P-01, P-11, P-13, P-14, P-15, P-17; A-01, A-02, A-05.
- **Delivered:** Per-hit Why this result? on Find saved PDF text; cites query,
  page, and stored excerpt; Results phase retains normalized query; honest
  keyword-not-meaning copy.
- **Verification:** On 2026-07-25, unit copy tests; debug APK installed. User
  confirmed Why this result? cites page + excerpt for meet mira on Medium Phone.
- **Truthfulness:** Interim keyword path only; no AI, confidence, Memory Explain,
  or PDF reopen.

### WorkManager MediaStore discovery drain

- **Requirements:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** MediaStoreDiscoveryWorker drains IndexMediaStoreImages pages under
  unique work; ViewModel enqueues on Start indexing; honest full/selected metadata
  copy; battery-not-low constraint.
- **Verification:** On 2026-07-25, unit mapper/summary/ViewModel tests; Medium Phone
  MediaStoreDiscoveryWorkerAndroidTest **2 of 2**; debug APK installed. User
  confirmed on Medium Phone: metadata-only completed copy (0 items / up to date).
- **Truthfulness:** Metadata catalogue only; no image bytes, OCR, AI, or network.

### WorkManager SAF PDF extract drain

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** SafPdfExtractWorker drains pending PDFs (no current extraction)
  via broker + isolated parser + persist; Local PDF reading Start enqueues unique
  work; honest multi-PDF copy; password skip / access-stop outcomes.
- **Verification:** On 2026-07-25, unit mapper/copy tests; Medium Phone extract
  androidTest + pending-asset Room test; debug APK installed. User confirmed on
  Medium Phone: Completed multi-PDF saved-for-search copy.
- **Truthfulness:** Explicit consent only; ADR-017 isolation; no AI/network.

### WorkManager SAF PDF discovery drain

- **Requirements:** P-04, P-05, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Hilt SafPdfDiscoveryWorker drains bounded IndexSafPdfFolder
  pages under unique work per source; ViewModel enqueues on Index; honest metadata
  progress copy; battery-not-low constraint.
- **Verification:** On 2026-07-25, unit mapper/summary/ViewModel tests; Medium Phone
  WorkManager androidTest **3 of 3**; debug APK installed. User confirmed on
  Medium Phone: indexed 2 PDF items + up-to-date list copy (Local PDF reading still
  required for text search).
- **Truthfulness:** Discovery placeholders/checkpoints only; no PDF bytes, extract
  WM, AI, or network.

### On-device keyword search over saved PDF page text

- **Requirements:** P-01, P-07, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** `SearchPersistedPdfPageText`; DAO join on current Asset fingerprint;
  Find saved PDF text screen with page + excerpt hits; honest keyword copy.
- **Verification:** On 2026-07-25, unit support/copy tests; Medium Phone keyword
  search androidTest **2 of 2**; debug APK installed. User confirmed query → page
  + excerpt hit on Medium Phone.
- **Truthfulness:** Keyword/substring only; no PDF reopen, WorkManager, AI, or network.

### Persist searchable PDF text from Local PDF reading Start

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Client retains validated wire results; `PersistValidatedPdfLocalReading`
  maps → prepare → Room; Start path persists complete/no-text extractions; Completed
  copy states text was saved for search on this phone.
- **Verification:** On 2026-07-25, unit local-reading tests passed; Medium Phone
  persist integration **1 of 1** and client isolation **10 of 10**; debug APK
  installed. User confirmed Start → Completed saved-for-search copy on Medium Phone.
- **Truthfulness:** No WorkManager, AI, network, or search-results UI.

### Wired Local PDF reading status UI (foreground, status-only)

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** `RunPdfLocalReadingStatusCheck`; ViewModel Start/Retry/Resume bind
  the isolated parser for one indexed PDF; session Completed state; honest copy;
  first-PDF asset lookup on `AssetRepository`.
- **Verification:** On 2026-07-25, unit local-reading **9**; Room asset lookup
  androidTest **3 of 3**; debug APK installed on Medium Phone. User confirmed
  Start → Completed on an indexed folder (1 PDF).
- **Truthfulness:** Page text still discarded; no searchable Room persist,
  WorkManager, AI, or network.

### Verified real-source descriptor path (fingerprint + live SAF open)

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** ApprovedPdfFingerprintRevalidationContract; SafPdfDocumentFingerprint;
  broker observeFingerprint before open; StaleSource through parse/assemble/eligibility;
  ParseApprovedPdfWithIsolatedParserRealSourceIntegrationTest (user-approved tree,
  fixture PDF, live open, status-only, delete fixture).
- **Verification:** On 2026-07-25, Medium Phone emulator 11 focused broker/synthetic/
  real-source tests; unit fingerprint revalidation 3 of 3.
- **Truthfulness:** Does not wire production UI to real parse, persist searchable
  extraction text, or add WorkManager/AI/network.

### Verified visible PDF local-reading recovery UI (presentation-only)

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** PdfLocalReadingCopy/session/ViewModel and Local PDF reading card on
  the connected folder screen (explain, progress, pause, retry, unavailable).
- **Verification:** On 2026-07-25, unit copy 2 of 2 and session 5 of 5.
- **Truthfulness:** Does not open PDFs, call the parser for user documents, write
  Room extraction text, or add WorkManager/AI/network.

### Verified Binder protocol v3 session/chunk streaming

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** IIsolatedPdfParser begin/nextChunk/cancel; session message codec;
  isolated service streaming; ordinary client assembly via session assembler with
  status-only result.
- **Verification:** On 2026-07-25, Medium Phone emulator 25 focused isolation/handoff/
  process-death tests passed.
- **Truthfulness:** Synthetic descriptors only; no real PDF, Room/UI wiring,
  WorkManager, AI, or network.

### Verified pure PDF parser session/chunk assembler

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** Session streaming plan plus IsolatedPdfParserSessionAssembler; unit tests cover ordered assembly, terminal headers, cancel, and rejection without partial text. Binder protocol v3 not implemented.
- **Verification:** On 2026-07-25, local IsolatedPdfParserSessionAssemblerTest 8 of 8.
- **Truthfulness:** No AIDL/service change, real PDF, Room/UI wiring, WorkManager, AI, or network.

### Verified PDF extraction write-path resource budgets

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** PdfExtractionWriteBudgets provisional hard limits enforced on Room writes; content-free write-path benchmark buckets; over-budget page count fails safely with zero rows.
- **Verification:** On 2026-07-25, unit 3 of 3 and Medium Phone emulator 5 of 5.
- **Truthfulness:** Synthetic fixtures only; no real PDF / WorkManager / AI / network / production UI wiring. ADR-017 remains.

### Accepted ADR-022 and verified synthetic PDF extraction Room persistence

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** ADR-022 accepted (retain superseded extrated as non-current provenance). Room schema v4 adds pdf extraction tables with additive migration 3 to 4. RoomPdfExtractionPersistencePort proves atomic write, idempotency, dual-fingerprint retention, conflict fail-safe, and delete-all. Not wired into production discovery/UI.
- **Verification:** On 2026-07-25, Medium Phone emulator persistence + migration 5 of 5; opener 3 of 3 on schema v4.
- **Truthfulness:** No real PDF parse, WorkManager, AI, network, or searchable UI. Measured write limits and ADR-017 real-source gates remain.

### Proposed ADR-022 superseded PDF extraction retention

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** ADR-020 gate progress updated (ADR-021 encryption prerequisite
  satisfied). ADR-022 proposed: retain superseded PDF extractions as non-current
  provenance (recommended) versus delete-on-supersede. No Room content write enabled.
- **Verification:** Documentation and decision records only.
- **Truthfulness:** Historical proposal; ADR-022 later accepted. See entry above.

### Verified encrypted conversion performance budget

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Content-free conversion timing buckets and provisional ceilings for
  synthetic schema-v3 sizes (`ConversionElapsedBuckets`,
  `ConversionPerformanceBenchmarkIntegrationTest`,
  `docs/ENCRYPTED_DATABASE_CONVERSION_BENCHMARK_PLAN.md`).
- **Verification:** On 2026-07-25, unit **1 of 1** and Medium Phone emulator **4 of
  4** under ceilings (largest measured ~7.3s for 5_000 assets).
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Emulator battery deltas are informational only.

### Verified device-unlock deferred open and recovery copy guards

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Credential-unlock gate defers encrypted open without creating a
  second database or mutating files; handle exposes waiting state; calm unlock UI;
  rebuild + unlock copy jargon guards.
- **Verification:** On 2026-07-25, unit **2 of 2** and Medium Phone emulator
  `DeviceUnlockDeferredOpenIntegrationTest` **2 of 2**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Performance/battery budget remains open.

### Verified physical-device arm64 encrypted conversion

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Rollout proof #6 on Samsung Galaxy A15 5G (`SM-A156E`,
  `arm64-v8a`): native SQLCipher load, encrypted create/reopen, wrong-passphrase
  denial, production-named conversion, and production opener conversion.
  `StandardSqliteDatabaseProbe` copies before probing so OEM SQLite cleanup cannot
  delete live encrypted DB files.
- **Verification:** On 2026-07-25, device instrumentation **13 of 13 passed**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.

### Verified low-storage / interruption conversion denial

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `ConversionStorageGuard` preflight before encrypted candidate create;
  denial and mid-conversion IO failure mark `FAILED_SAFE` /
  `CONVERSION_VALIDATION_FAILED`, keep plaintext intact, and open plaintext for the
  session until storage allows a successful retry (`MemoraEncryptedDatabaseOpener`).
- **Verification:** On 2026-07-24, Medium Phone emulator
  `ConversionLowStorageDenialIntegrationTest` **2 of 2 passed**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Physical-device arm64 proof later verified separately.

### Verified live conversion process-death resume

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Debug secondary process `:conv_live_death` arms conversion at
  `ROWS_COPIED` / `SWITCH_PENDING`; instrumentation induces real `am crash`/kill;
  ordinary process cold-opens and completes with fixture rows
  (`ConversionLiveProcessDeathIntegrationTest`).
- **Verification:** On 2026-07-24, Medium Phone emulator **2 of 2 passed**.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Live-death helper is debug-source only (not in release).

### Corrected process-death proof status (honesty)

- **Requirements:** P-05, P-14, P-15, P-17; governance truthfulness.
- **Delivered:** Documentation and ADR-021 status corrected so simulated prepare-stop
  resume is not claimed as the rollout **live crash/kill** gate. Pre-work record for
  the live-kill slice added in
  `docs/CHANGE_CONTROL_LIVE_CONVERSION_PROCESS_DEATH.md`.
- **Verification:** Documentation cross-check only; no production behavior change.
- **Truthfulness:** Superseded for the live-kill gate by the verified live suite above.
  Low-storage denial and physical-device arm64 proofs later verified separately.
  PDF content persistence remains blocked.

### Verified simulated conversion process-death resume

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `MemoraEncryptedDatabaseOpener` resume for interrupted `ROWS_COPIED`
  and `SWITCH_PENDING`, including mid-finalize layouts (plaintext retained; candidate
  already promoted). Instrumentation simulates death via prepare-stop hooks then
  resumes with `open()`.
- **Verification:** On 2026-07-24, Medium Phone emulator
  `ConversionProcessDeathResumeIntegrationTest`: **4 of 4 passed**.
- **Limitation:** This is **simulated** interrupt/resume only. The rollout **live
  crash/kill** process-death gate remains open.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.

### Verified Clear Memora derived data and rebuild recovery UX

- **Requirements:** P-05, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `MemoraDatabaseHandle` + `ClearMemoraDerivedData` close, delete only
  Memora-owned DB/wrapper/journal/Keystore wrap state, and reopen a fresh encrypted
  empty index. Welcome **Clear Memora index** confirm flow uses ADR-021 rebuild copy
  only. Repositories resolve DAOs through the live handle after recreate.
- **Verification:** On 2026-07-24, Medium Phone emulator
  `ClearMemoraDerivedDataIntegrationTest` **1 of 1 passed**; unit
  `ClearMemoraDerivedDataCopyTest` passed. Persistable URI grants unchanged by clear.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was added.
  Clear does not revoke Android folder permissions.

### Wired live encrypted PersistenceModule open and Open-source notices

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `PersistenceModule` opens `memora.db` via
  `MemoraEncryptedDatabaseOpener` (SQLCipher `SupportOpenHelperFactory`, Keystore
  passphrase wrap, conversion journal). Fresh encrypted create and plaintext
  copy-and-validate rename finalize. Welcome → **Open-source licenses** ships
  SQLCipher Community BSD notice text plus SQLite/LibTomCrypt notices.
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `MemoraEncryptedDatabaseOpenerIntegrationTest`: **3 of 3 passed**. TearDown
  clears production identity files.
- **Truthfulness:** No PDF content write, WorkManager, AI, or network path was
  added. Recovery UX still uses approved rebuild wording only.

### Verified production-named disposable conversion

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Instrumentation conversion using disposable `memora.db` /
  `memora.db.encrypted_candidate` names, production Keystore alias/journal files,
  and rename-finalize onto `memora.db` after validated encrypted reopen. TearDown
  deletes disposable files so live PersistenceModule identity is not stranded.
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `ProductionNamedConversionIntegrationTest`: **3 of 3 passed**.
- **Truthfulness (at delivery):** Live `PersistenceModule` still opened plaintext
  `memora.db` at that gate; later superseded by the encrypted-open checkpoint above.

### Promoted SQLCipher to production classpath (Slice 1)

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Re-verified `net.zetetic:sqlcipher-android:4.17.0` AAR SHA-256 and
  OSV (empty advisories), then moved SQLCipher and `androidx.sqlite:2.6.2` to
  `implementation`. Provenance review updated for classpath promotion.
- **Verification:** On 2026-07-24, `:app:assembleDebug` succeeded; emulator
  `EncryptedDatabasePocIntegrationTest` **7/7** and
  `PlaintextToEncryptedConversionIntegrationTest` **5/5**.
- **Truthfulness:** `PersistenceModule` still opens plaintext `memora.db`. No
  encrypted open switch, PDF content write, notices UI, WorkManager, AI, or network
  path was added. BSD notices remain required before any release that ships the
  library.

### Accepted encrypted-database production conversion rollout plan

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** `docs/ENCRYPTED_DATABASE_CONVERSION_ROLLOUT.md` defines
  PersistenceModule switch acceptance criteria, crash-resume rules, BSD attribution
  gate, rollback, staged release checklist, and remaining device/process-death
  proofs.
- **Status:** Design gate only. Production `memora.db` remains plaintext; no
  SQLCipher promotion, Room conversion switch, PDF content write, UI, worker, AI, or
  network behavior changed at the documentation gate.
- **Verification:** Documentation cross-check against verified synthetic PoC and
  conversion harness results on 2026-07-24.

### Verified synthetic plaintext-to-encrypted conversion harness

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Conversion journal/phases in `data/security`, DAO `findAll`/`count`
  helpers, and androidTest-only `PlaintextToEncryptedConversionHarness` that
  copy-and-validates schema-v3 fixture rows between separately named PoC databases,
  retains plaintext until explicit finalize, and fails safe without destructive
  overwrite.
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `PlaintextToEncryptedConversionIntegrationTest`: **5 of 5 passed**.
- **Truthfulness:** Production `PersistenceModule` remains plaintext `memora.db`. No
  PDF content persistence, UI, WorkManager, AI, or network path was added.

### Verified synthetic encrypted-database PoC

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Keystore AES-GCM passphrase wrapper in `data/security`, androidTest-
  only SQLCipher Room opener for `memora_encrypted_poc.db`, and
  `EncryptedDatabasePocIntegrationTest` covering native load, create/reopen round-
  trip, wrong passphrase, tampered wrapper, read-only plaintext probe, clear-text
  marker absence, no-INTERNET permission, and unchanged production `memora.db` name.
- **Dependencies:** `androidTestImplementation` only —
  `net.zetetic:sqlcipher-android:4.17.0` and `androidx.sqlite:sqlite:2.6.2` (resolves
  cleanly with Room `2.8.4`).
- **Verification:** On 2026-07-24, Medium Phone emulator ran
  `EncryptedDatabasePocIntegrationTest`: **7 of 7 passed**.
- **Truthfulness:** Production `PersistenceModule` remains plaintext. No PDF content
  persistence, UI, WorkManager, AI, or network path was added.

### Accepted ADR-021 encrypted-database direction with provenance and PoC plan

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** Product owner accepted ADR-021. Frictionless UX and recovery-copy
  rules are recorded. `docs/SQLCIPHER_DEPENDENCY_PROVENANCE_REVIEW.md` accepts
  Maven Central `net.zetetic:sqlcipher-android:4.17.0` for PoC use only, with
  recorded AAR hashes and a point-in-time empty OSV result.
  `docs/ENCRYPTED_DATABASE_POC_PLAN.md` defines the synthetic-only next code gate.
- **Status:** Direction accepted; production `memora.db` remains plaintext; no
  production SQLCipher binding, Room conversion, PDF content write, UI, worker, AI,
  or network behavior changed at the documentation gate.
- **Verification:** Documentation and supply-chain inspection only on 2026-07-24.

### Proposed encrypted-database decision record

- **Delivered:** Added the SQLCipher-for-Android plus Android-Keystore recommendation,
  option comparison, licensing/provenance gate, key lifecycle, recovery,
  non-destructive conversion, diagnostics, and test/release requirements.
- **Status:** Superseded by ADR-021 acceptance above. Historical proposal retained for
  traceability.

### Accepted local-data backup and transfer exclusion

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Delivered:** The accepted ADR-020 privacy posture now excludes all Memora-private
  databases, preferences, files, external app data, and app-root data from legacy
  Android backup plus Android 12+ cloud backup and device-to-device transfer.
  `allowBackup=false` is included as defence in depth. The design record remains the
  gate for future PDF extraction persistence.
- **Reason:** Android documents that `allowBackup=false` alone cannot reliably block
  device-to-device transfer on every manufacturer, so explicit exclusions are used
  for both transfer paths.
- **Verification:** On 2026-07-24, `:app:assembleDebug :app:installDebug` succeeded
  on the Medium Phone emulator. Package flags confirmed backup is disabled; packaged
  resources confirmed exclusions for all five private-data domains on both legacy and
  Android 12+ cloud/device-transfer paths.
- **Truthfulness:** This adds no Room schema/migration/write, source access, parser
  transport, UI, worker, AI, network, search, or real-source PDF capability.

### Verified content-free PDF extraction persistence eligibility

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure application policy now permits only an identity/fingerprint/
  schema-matching complete or explicit no-text extraction record to become eligible
  for a future atomic persistence transaction. Eligible facts are content-free:
  identity, fingerprint, schema, page count, coverage, integrity, lifecycle, and
  retry directive. Partial, inconsistent, failed, access-blocked, source-unavailable,
  and retryable outcomes stay explicitly ineligible.
- **Verification:** On 2026-07-23, `PrepareApprovedPdfExtractionPersistenceTest`
  passed **8 of 8** local unit tests.
- **Truthfulness:** This adds no Room entity, migration, or write; no source access,
  descriptor or parser-text transport, UI, WorkManager, semantic understanding/AI,
  or network path. It does not enable a real user PDF.

### Verified atomic PDF extraction persistence port

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A domain-layer repository port now accepts only a typed request
  built from an `EligibleForAtomicWrite` decision and its matching extraction record.
  The factory rejects mismatched identity, fingerprint, schema, page count, and
  partial/wrong coverage before a repository can receive a request. The port has
  explicit persisted, retryable, stale-reindex, and safe-failure outcomes.
- **Verification:** On 2026-07-23, `PdfExtractionPersistencePortContractTest` passed
  **5 of 5** local unit tests; the prerequisite eligibility suite remained **8 of 8**.
- **Truthfulness:** No repository implementation, Room schema/migration/write, source
  access, descriptor or parser transport, UI, WorkManager, AI, or network behavior
  was added.

### Verified PDF extraction persistence coordinator

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure application coordinator invokes the future persistence port
  only for an eligible, matching record. It preserves all four explicit port outcomes
  and prevents ineligible, missing, or mismatched record input from reaching the port.
- **Verification:** On 2026-07-23, `PersistApprovedPdfExtractionTest` passed
  **7 of 7** local unit tests.
- **Truthfulness:** The test uses only a fake port. No repository implementation,
  Room schema/migration/write, source access, parser/text transport, UI, WorkManager,
  semantic understanding/AI, or network behavior was added.

### Verified approved-parser and in-memory extraction assembly

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A small application consistency gate now joins an approved parser
  status with an already-created in-memory extraction outcome. It calls the synthetic
  extraction provider only after a successful parser status, requires exact Asset
  identity/fingerprint/schema binding plus matching page count and coverage, and
  rejects an inconsistent record. Parser transport failures remain distinct from
  post-parser extraction failures.
- **Verification:** On 2026-07-23, `AssembleApprovedPdfExtractionTest` passed
  **6 of 6** local unit tests. `ParseApprovedPdfWithIsolatedParserIntegrationTest`
  also passed **3 of 3** on the Medium Phone emulator.
- **Truthfulness:** The assembly has no descriptor/source/text transport API and
  writes no Room data. Its provider is synthetic and in-memory; this does not prove
  that page text has crossed the private service boundary or enable real-source
  parsing, search, UI, WorkManager, AI, or network.

### Verified validated-parser-result to domain-extraction handoff

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A small, synthetic-only data-to-domain mapper now accepts only an
  already validated isolated-parser result. It deterministically rejoins ordered
  chunks into complete page records, preserves explicit no-extractable-text, and maps
  protected or failed parser outcomes to truthful domain failures. It creates only an
  in-memory `PdfExtractionRecord` bound to the supplied Asset identity/fingerprint.
- **Verification:** On 2026-07-23, `ValidatedIsolatedPdfResultToExtractionMapperTest`
  passed **4 of 4** local unit tests. The existing private-parser emulator regression
  also passed **20 of 20** focused Android tests on the Medium Phone.
- **Truthfulness:** This mapper does not decode Binder data, open a descriptor or
  source, write Room, start WorkManager, invoke understanding/AI, expose UI, search,
  or use network. It has no partial-result transport and cannot enable real-source
  parsing or persistence.

### Verified bounded synthetic isolated-parser result transport

- **Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** The existing private isolated parser now uses protocol version 2 to
  return a strict, bounded page/chunk envelope for repository-owned synthetic
  descriptors. The ordinary-process client validates the exact envelope and then
  deliberately discards chunks, retaining its content-free status summary. The
  temporary synthetic limits are 32 pages, four chunks per page, 8,192 UTF-16
  code-units per page, 65,536 total, and 2,048 per chunk.
- **Verification:** On 2026-07-23, the Medium Phone emulator passed **20 focused
  tests** across the service, client, end-to-end transport, and approved-broker
  handoff integration suites.
- **Truthfulness:** This does not enable a user source, extraction persistence,
  search, UI, WorkManager, understanding/AI, or network. The single bounded
  synthetic response is not the future real-source session/chunk streaming protocol;
  its limits are not production budgets.

### Verified synthetic approved-PDF parser handoff

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** An unbound application coordinator now connects the approved SAF
  custody broker to the private isolated parser only through a dedicated descriptor
  ownership adapter. The broker retains and closes its borrowed duplicate; the adapter
  duplicates it before transferring ownership to the existing parser client, which
  closes its own handle. The debug-only fixture was upgraded to a valid one-page,
  repository-owned selectable-text PDF.
- **Verification:** On 2026-07-23,
  `ParseApprovedPdfWithIsolatedParserIntegrationTest` completed on the Medium Phone
  emulator: **3 tests passed**. It proves the synthetic approved path reaches the
  private parser, a grant revoked immediately before opening prevents parser
  submission, and a retryable parser status is not presented as extraction.
- **Truthfulness:** The result is content-free and not persisted or searchable. This
  does not open a real user PDF or add UI, Hilt, WorkManager, Room extraction,
  semantic understanding/AI, or network behavior.

### Verified synthetic SAF PDF descriptor broker

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** An unbound data/platform broker, Android tree-membership/read-only
  adapter, and debug-only synthetic DocumentsProvider fixture, excluded from release
  builds. The broker owns original/duplicate descriptor closure and has no UI, Hilt,
  Room, worker, parser, AI, or network binding. API 26-28 returns an explicit safe
  unsupported-platform result and opens nothing.
- **Verification:** On 2026-07-23,
  `SafPdfDescriptorBrokerIntegrationTest` completed on the Medium Phone emulator:
  **6 tests passed**.
- **Truthfulness:** The test uses only a pipe-backed repository fixture. It does not
  request or use a real persisted user grant, and it does not make a user PDF
  eligible for opening or parsing.

### Canonical SAF PDF target boundary

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A SAF data/platform factory now derives an internal future opening
  target from the approved tree plus opaque source document ID. It does not trust a
  stored location as opening authority and rejects non-PDF, source-mismatch, invalid
  tree, and foreign-provider cases before any platform I/O.
- **Verification:** On 2026-07-23, the user ran
  `SafPdfCanonicalDocumentTargetFactoryIntegrationTest` on the Medium Phone emulator:
  **5 tests passed**.
- **Truthfulness:** The test uses five synthetic URI-only cases. This component
  performs no grant validation, provider query, descriptor open, parser call, source
  read, persistence, UI, WorkManager, AI, or network operation.

### Approved Android PDF descriptor-broker design

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** The platform custody plan defines exact SAF approval/grant ordering,
  opaque document-ID handling, Android subtree membership checks, read-only descriptor
  ownership, duplicate transfer to the isolated parser, and a synthetic-fixture test
  matrix.
- **Truthfulness:** Documentation and review only. No descriptor-opening code, source
  read, PDF parse, UI, persistence, WorkManager job, AI capability, permission,
  dependency, or network path was added. Real user-source parsing remains disabled
  under ADR-017.

### Verified approved PDF descriptor-custody gate

- **Requirements:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Delivered:** A pure domain contract now binds a future PDF extraction request to
  the exact approved source ID and a freshly observed source-access state. Its only
  successful output contains the immutable Asset identity and fingerprint; it cannot
  carry a URI, descriptor, stream, source content, or parser handle inward.
- **Verification:** On 2026-07-23, focused Gradle and the user's Android Studio run
  both reported `ApprovedPdfDescriptorCustodyContractTest`: **5 tests passed**.
  The tests cover authorization plus mismatch, access-required, access-revoked, and
  source-unavailable denials.
- **Truthfulness:** No Android source, document, descriptor, parser, Room state, UI,
  worker, AI capability, network, or dependency was accessed or changed. This does
  not enable real-source PDF parsing; ADR-017's platform descriptor-broker gates
  remain required.

### Frozen trust, identity, and quality rules

- **Requirements:** P-09–P-13, A-01–A-05, E-04–E-06.
- **Delivered:** ADR-019 and Experience Memory Amendment v1.1 establish stable
  Memory identity/revisions, “truth before intelligence,” explicit integrity states,
  user-facing `Why this result?`, evidence-support classes, and calibration/
  overconfident-error evaluation requirements.
- **Truthfulness:** Documentation only. The current one-Asset `Memory` code does not
  yet implement Memory IDs, revisions, persistence, state presentation, confidence,
  evaluation, or `Why this result?` UI.
- **Verification:** Cross-reference review completed against the governing product
  documents, current Memory contract, architecture, ADRs, roadmap, and traceability.

### Governed Experience Memory direction and behavioral boundary

- **Requirements:** P-01, P-02, P-09 through P-13, P-18; A-01 through A-05; future
  architecture IDs E-01 through E-03.
- **Delivered:** User-approved `EXPERIENCE_MEMORY_AMENDMENT_V1`, ADR-018, an updated
  Local-AI specification, product capability map, roadmap, and traceability entries.
  They establish Personal Knowledge Infrastructure as Memora's long-term direction:
  Asset Memories remain the MVP foundation; future Event and Knowledge Memories use
  evidence-backed links rather than destructive grouping.
- **Truthfulness:** This is governance and behavioral specification only. It adds no
  source access, AI dependency, model, event detection, timeline, WhatsApp/audio
  access, storage schema, UI, or background work. P-18 exclusions remain in force.
- **Verification:** Documentation cross-reference review against the product source
  registry, Local-AI specification, product contract, architecture, ADRs, roadmap,
  traceability matrix, and current one-Asset `Memory` domain contract.

### Verified fresh SAF read-grant validation boundary

- **Requirements:** P-03, P-05, P-07, P-14, P-15, P-17; Local AI principles A-01,
  A-02, A-06.
- **Delivered:** A source-neutral `DocumentTreeAccessValidator` now checks
  Android's persisted permission list for the exact user-approved document-tree URI
  and a retained read permission. The existing metadata-only SAF discovery adapter
  depends on this new boundary; the metadata catalog no longer owns authorization.
- **Verification:** Kotlin, unit-test, and Android-test compilation passed. The
  focused matcher result contains **3 tests passed** for exact-tree acceptance and
  different-tree/readless-grant rejection. On 2026-07-23, after explicitly
  reconnecting an emulator Documents folder, the user ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator:
  **1 test passed**.
- **Truthfulness:** The Android adapter reads only the retained grant list. It does
  not query a provider, open a user document or descriptor, parse a PDF, persist an
  extraction, call the isolated service, invoke AI, or access a network. This does
  not enable real-source parsing.

### Verified live isolated PDF parser-process death recovery

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A test-only harness binds the existing private isolated parser service,
  holds a synthetic pipe request open, identifies exactly one package-matching process
  with an isolated UID, then induces an Android `am crash <pid>` for that PID alone.
  It adds no production service behavior, permission, Binder method, or debug kill
  switch.
- **Verification:** On 2026-07-23, the user ran
  `LiveIsolatedPdfParserProcessDeathIntegrationTest` on the Medium Phone emulator:
  **1 test passed**. The ordinary process returned a retryable content-free failure,
  closed its supplied descriptor, and marked the Binder connection unavailable.
- **Truthfulness:** This proves a narrow synthetic live-death recovery path only. It
  does not authorize real-source access or prove grant validation, result transport,
  resource limits, persistence, reconnection scheduling, or recovery UI.

### Verified offline synthetic PDF parser runtime

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A synthetic-only Android test now confirms that the release app
  requests no `INTERNET` permission, then permits the existing local PDF parser to
  run only after Android reports the emulator has no Internet-capable or validated
  network. It obtains network-state visibility through a temporary test-shell identity
  and drops that identity before parsing; neither app manifest gains a network
  permission or a network client.
- **Verification:** On 2026-07-22, the user ran
  `PdfParserOfflineRuntimeIntegrationTest` on the offline Medium Phone emulator:
  **2 tests passed**. It parsed only a repository-owned fixture and accessed no user
  source, descriptor, URI, SAF tree, Room data, service, UI, WorkManager, AI, or
  network.
- **Truthfulness:** This closes only the deterministic parser's narrow offline runtime
  check. It does not authorize real-source parsing or prove future source access,
  isolated-service behavior, persistence, or semantic understanding offline.

### Verified many-page synthetic PDF parser baseline

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A test-only benchmark plan and Android harness measure aggregate
  input bytes, page count, extracted-text UTF-16 code units, future-result Bundle
  size, and parser elapsed-time range after a warm-up and five measured runs. The
  expanded corpus generates small, medium, larger, and 32-page repository-owned PDFs
  entirely in memory, then materializes input bytes before timing begins. It contains
  no user source access, source text logging, service binding, Room, UI, WorkManager,
  or AI.
- **Verification:** On 2026-07-22, the connected Medium Phone emulator ran
  `PdfParserSyntheticBenchmarkIntegrationTest`: 3 of 3 tests passed. The new
  32-page fixture produced 65,536 text code units; all measurements are recorded in
  `docs/PDF_PARSER_BENCHMARK_PLAN.md` and remain harness evidence only, not a
  production policy.

### Verified strict parser-result Bundle codec

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A future-only Android `Bundle` decoder accepts exactly the approved
  version-one keys and field types, rejects unexpected or missing fields, then feeds
  the existing bounded-result validator. It returns no candidate text when decoding
  or validation fails.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserResultBundleCodecIntegrationTest` on the Medium Phone emulator:
  10 of 10 tests passed. The live isolated service still returns status only and this
  change opens no descriptor or source.

### Verified bounded parser-result contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A pure validator now defines the future versioned page-text result
  shape. It requires complete page/chunk coverage and an injected, explicit limit for
  page count, chunks per page, page-text UTF-16 code units, and total-text UTF-16
  code units. Rejection exposes no candidate content; a later transport must map it
  to a retryable content-free outcome.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserResultContractTest` in Android Studio: 10 of 10 tests passed.
  The current Binder service still returns status only; this change opens no
  descriptor or source and enables no PDF content return.

### Verified isolated parser end-to-end transport

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** One Android test connects the existing private Binder adapter to the
  existing ordinary-process client, then passes only a repository-owned pipe
  descriptor through the isolated service. It asserts a validated status-only result
  and ordinary-process descriptor closure.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserEndToEndIntegrationTest` on the Medium Phone emulator: 1 of 1
  test passed. This does not enable SAF access, real-source parsing, text chunks,
  Room persistence, UI, or AI.

### Verified isolated-parser client cancellation contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic ordinary-process client accepts an Android
  `CancellationSignal`. Cancellation before submission prevents a parser call;
  cancellation while waiting cancels the client-side future and returns a
  content-free retryable result after descriptor closure.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 10 of 10
  tests passed. This does not claim to terminate a live isolated process or enable
  real PDF access.

### Verified private parser-service binding contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A private Android binding adapter can expose only the existing
  isolated parser Binder. It reports connecting, available, or retryable-unavailable
  status and accepts no descriptor, URI, path, source identity, or parser request.
- **Verification:** On 2026-07-22, the user ran
  `AndroidIsolatedPdfParserConnectionIntegrationTest` on the Medium Phone emulator:
  3 of 3 tests passed. It proves a live private binding, explicit bind failure, and
  explicit disconnection callback without parser work.

### Verified malformed parser-response handling

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic ordinary-process client test now covers unknown
  outcomes, a false isolated-process claim, missing page-count data, and an invalid
  page count. Each must become a content-free retryable failure and close its
  descriptor.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 8 of 8
  tests passed. This remains a transport-validation step only; it does not enable
  real PDF access, Binder page/text chunks, or UI.

### Verified ordinary-process parser recovery contract

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** A private, synthetic-only client contract maps bind failure,
  simulated Binder death, bounded timeout, interruption, and malformed transport
  responses to content-free retryable results. It returns only a validated outcome,
  retryability, and optional page count, and closes every supplied descriptor.
- **Verification:** On 2026-07-22, the user ran
  `IsolatedPdfParserClientIntegrationTest` on the Medium Phone emulator: 4 of 4
  tests passed. This does not enable real PDF access, a live service connection,
  real process-death testing, cancellation, chunking, offline checks, persistence,
  or visible recovery UI.

### PDF parser isolation guardrail

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Decision:** ADR-017 requires all future real-PDF parsing to run in a private
  Android isolated service. The normal app verifies one persisted SAF read grant and
  passes one read-only descriptor; the parser service receives neither URI/path nor
  broad source access.
- **Delivered:** The documented threat model is now represented by a private,
  non-exported `android:isolatedProcess` service and a fixed-version descriptor-only
  Binder interface. The service receives no URI, path, source identity, metadata,
  extracted text, Room access, Hilt graph, UI, or network permission. It currently
  accepts only a repository-owned synthetic descriptor and returns a small parser
  status summary. No SAF source access, UI, Room change, worker, AI capability, or
  real user document path has been added.
- **Verification:** Android-test APK compilation passed on 2026-07-21. The user ran
  `IsolatedPdfParserServiceIntegrationTest` on the Medium Phone emulator: 2 of 2
  tests passed. They prove the private/isolated manifest configuration and a two-page
  synthetic descriptor parse. Offline verification, no-text/password/malformed inputs,
  descriptor cleanup under every failure, service death, cancellation, timeout
  reporting, chunk validation, source access, persistence, and visible
  progress/recovery are still required before real-source enablement.

### Expanded isolated PDF parser safety cases

- **Requirements:** P-07, P-14, P-15, P-17; Local AI principles A-01, A-02, A-06.
- **Delivered:** The synthetic-only service test now covers no extractable text,
  password protection, malformed input, an unsupported protocol version, and
  client-side descriptor cleanup after every Binder call. The worker now also closes
  its received descriptor if submission or execution fails. Its result is still a
  tiny status summary only: no source text, title, metadata, URI, path, or identity.
- **Verification:** Android-test APK compilation passed on 2026-07-21. On
  2026-07-22, the user ran the expanded class on the Medium Phone emulator: 6 of 6
  tests passed. This does not verify service death, cancellation, timeout, output
  chunking, offline operation, actual source access, persistence, or UI recovery.

### Local PDF parser and fixture decision

- **Requirements:** P-05, P-07, P-14, P-15, P-17; scanned-PDF OCR remains a future
  Local AI capability.
- **Decision:** ADR-016 selects PDFBox-Android 2.0.27.0 as the local parser behind
  the existing domain port, subject to explicit licensing, dependency, supply-chain,
  fixture, isolated-process, resource-measurement, offline, and emulator gates. Its
  vulnerable declared Bouncy Castle 1.72 dependencies are explicitly overridden with
  the reviewed 1.84 set.
- **Delivered:** The data-layer mapper and generated synthetic-only Android test
  fixtures exist. No SAF URI, connected folder, Room record, UI, worker, or real user
  document is wired to the mapper.
- **Verification:** Dependency graph, SBOM, OSV review, third-party notices, debug
  APK build-size baseline (`12,423,988` to `18,734,630` bytes), and Android-test APK
  compilation are complete. On 2026-07-21, a failed initial emulator run exposed
  accidental leading patch markers in the repository-owned Base64 fixture payloads;
  all four corrected payloads then passed independent Base64 validation. The user
  reran `PdfBoxPdfDocumentMapperIntegrationTest` on the Medium Phone emulator: 4 of
  4 tests passed. Command-line Android tools still could not see that emulator.
- **Truthfulness:** Text-layer PDFs can become complete page-level extraction records;
  scanned/image-only PDFs remain `NoExtractableText` until a separately governed local
  OCR capability is delivered. This records the P-07 gap rather than hiding it.
- **Verification:** Governing-document/current-code review, official Android API and
  compatibility review, and Git diff check. No source content, app behavior, or test
  suite changed in this documentation-only decision.

### Deterministic PDF extraction contract

- **Requirements:** P-05, P-07, P-14, P-15; this step does not implement Local AI.
- **Decision:** ADR-015 defines a versioned record that ties deterministic PDF facts
  to the exact source identity, fingerprint, and extraction schema. It distinguishes
  complete, partial, and no-text-layer coverage rather than silently treating a PDF as
  fully extracted.
- **Delivered:** Pure Kotlin PDF request, record, coverage, and recoverable outcome
  contracts, plus an inward-facing platform-extractor boundary. No data/platform
  adapter exists yet.
- **Privacy:** No document is opened, copied, uploaded, persisted, changed, or
  deleted. No parser, model, cloud path, background work, or UI behavior is added.
- **Verification:** On 2026-07-21, local Gradle passed `PdfExtractionTest`: 6 of 6
  tests cover PDF-only input, source-version binding, complete-page coverage, partial
  coverage, no-text truthfulness, and recoverable failure construction.
  Android-facing verification is not required for this pure domain contract because
  the app's runtime behavior is unchanged.
- **Known limitation:** The local PDF parser, privacy-safe fixtures, Room persistence,
  platform adapter, and emulator test remain separate steps.

### Verified explicit PDF-folder indexing control

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-16, P-17. This step does not
  implement Local AI, extraction, or a cloud path.
- **Decision:** ADR-014 restores the most recently approved private folder reference
  into the setup screen but never scans it automatically. The user explicitly starts
  one bounded metadata page and explicitly chooses any later page.
- **Delivered:** A Hilt ViewModel owns restored connection, indexing, completed,
  retryable-failure, and access-recovery state. Compose only renders that immutable
  state and forwards actions; it does not access Room, SAF, or PDF content.
- **Verification:** Local ViewModel and presentation-copy tests passed on 2026-07-21.
  Android Hilt/test compilation passed. The user then launched the app on the Medium
  Phone emulator, restored the connected folder, explicitly selected `Index this
  folder`, and observed the truthful completed `0 PDF items` state.
- **Known limitation:** This interim screen activates the most recently connected
  folder. A future source-management experience must let people view/select all
  independently connected folders. PDF extraction and background scheduling remain
  intentionally out of scope.

### Verified SAF descendant traversal

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17; no Local-AI requirement is
  implemented by this step.
- **Decision:** ADR-013 replaces root-only traversal with a resumable depth-first
  checkpoint strategy. One invocation reads children from exactly one pending folder;
  it never recurses unboundedly in a single call.
- **Delivered:** The SAF adapter's v2 checkpoint stores pending folder frames and
  their source-owned cursors, discovers declared PDF metadata in descendant folders,
  and reads a prior v1 root checkpoint safely. An empty provider page that claims more
  data becomes an explicit retryable failure.
- **Privacy:** The implementation reads metadata only and changes no source content.
  It does not open a PDF, read bytes/text, copy a document, request broader access,
  start automatically, or schedule background work.
- **Verification:** Local `SafPdfDiscoverySourceTest` passed on 2026-07-20, including
  deterministic nested traversal and v1-resume coverage. Android-test compilation
  passed. The user then reran `SafPdfDiscoverySourceIntegrationTest` on the Medium
  Phone emulator: 1 of 1 test passed after reconnecting the approved folder.
- **Known limitation:** The emulator is not assumed to contain a nested PDF fixture,
  so its live test confirms platform access/regression while the nested logic remains
  deterministically covered locally. Background scheduling and PDF extraction remain
  future work.

### Verified SAF PDF metadata page persistence

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17; A-01 and A-02 remain
  unaffected because this has no model, cloud, or network path.
- **Delivered:** `IndexSafPdfFolder` accepts one exact private approved-folder
  source ID, constructs a source-neutral adapter through an injected factory, and
  delegates one bounded result to the existing atomic discovery-page persistence
  boundary. Its immutable outcome distinguishes unconnected source, required/revoked
  access, retryable failure, and success.
- **Architecture:** The application layer depends only on domain contracts. The
  Android SAF catalog/factory is Hilt-bound in the data layer; no composable is
  changed and no layer opens a PDF.
- **Verification:** Local `IndexSafPdfFolderTest` and Android-test compilation passed
  on 2026-07-20. The user then ran `IndexSafPdfFolderIntegrationTest` on the Medium
  Phone emulator: 1 of 1 test passed against the already approved folder. It read one
  bounded metadata page and wrote only its placeholders and checkpoint to an isolated
  in-memory Room database.
- **Known limitation:** This is one explicit bounded metadata page per invocation.
  Descendant traversal is now handled through its source-owned checkpoint (ADR-013),
  but PDF bytes/text extraction, background scheduling, and UI initiation remain
  deliberately out of scope.

### Local-first engineering governance checkpoint

- **Delivered:** Preserved immutable, versioned repository copies of the original
  PRD and both accepted addenda in `docs/product-source/`, with SHA-256 values in
  `docs/PRODUCT_SOURCE_REGISTRY.md`.
- **Decision:** Added ADR-012 and `docs/LOCAL_AI_TECHNICAL_SPEC.md`. Normal memory
  creation, retrieval, ranking, and explanation are now governed as local-first and
  offline after required on-device capability installation. Cloud AI is optional and
  cannot become a core dependency.
- **Process:** Strengthened the mandatory pre-work gate. Every meaningful delivery
  must use the product registry, local-AI specification, current-code inspection,
  traceability IDs, and `docs/CHANGE_CONTROL_TEMPLATE.md`; conversational memory is
  not an authority.
- **Scope:** Documentation and source-artifact checkpoint only. No Android code,
  dependencies, permissions, source access, model, network client, or user-visible
  behaviour changed.

### Verified bounded SAF PDF metadata discovery

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17.
- **Decision:** ADR-011 records the bounded immediate-child implementation boundary,
  the explicit persisted-grant check, and the required future descendant-traversal
  work. It is not a claim that all nested PDFs are already discoverable.
- **Delivered:** A read-only SAF platform catalog and source adapter now verify the
  exact retained Android read grant for each connected source, query one bounded page
  of immediate-child metadata, emit declared PDFs as source-neutral Asset
  placeholders, and produce a private source-owned checkpoint. Revocation and
  provider errors are explicit outcomes rather than an empty folder.
- **Privacy:** The adapter does not request a permission, open a document, read PDF
  bytes or text, copy source data, persist a discovery page, start automatically, or
  schedule background work.
- **Verification:** On 2026-07-20, local `SafPdfDiscoverySourceTest` passed: 5 of 5
  tests. Android-test compilation passed. The user then ran
  `SafPdfDiscoverySourceIntegrationTest` on the Medium Phone emulator: 1 of 1 test
  passed against the already approved folder.
- **Known limitation:** It currently discovers immediate children only; resumable
  nested-folder traversal, persistence, extraction, and background scheduling remain
  future work.

### Verified user-approved SAF PDF-folder connection

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-16, P-17.
- **Decision:** ADR-010 establishes each approved SAF document tree as an
  independently resumable source with a SHA-256-derived source ID. Room database
  version 3 stores the private URI reference and approval time required by a later
  platform adapter.
- **Delivered:** The privacy-explained Compose setup screen launches Android's
  `ACTION_OPEN_DOCUMENT_TREE` picker. It persists only the chosen tree's read grant,
  then asks a Hilt ViewModel and application use case to save the private reference.
  The UI never accesses Room directly.
- **Privacy:** This flow does not enumerate a tree, open a PDF, copy content, request
  broad storage access, or start background work. The raw tree URI remains private
  database data and does not appear in source IDs.
- **Verification:** On 2026-07-20, `SafDocumentTreeSourceTest` passed in Android
  Studio: 3 of 3 tests. `RoomDocumentTreeApprovalRepositoryTest` then passed on the
  Medium Phone emulator: 2 of 2 tests. `MemoraDatabaseMigrationTest` passed: 1 of 1
  test verified the original Asset fixture survives the version-3 migration.
  `DocumentTreeSetupViewModelTest` passed: 4 of 4 tests, and
  `ApproveDocumentTreeTest` passed: 1 of 1. The user then selected an emulator folder
  through Android's live picker and observed the truthful connected state.
- **Known limitation:** No document enumeration or PDF discovery exists yet. The
  connected folder remains inert until the later, bounded read-only SAF adapter.

### Verified Compose setup and explicit MediaStore indexing control

- **Requirements:** P-04, P-05, P-14, P-15, P-16, P-17.
- **Delivered:** The Compose setup screen now reports Android permission results to
  the Hilt ViewModel and exposes `Start indexing` only after confirmed access. One
  user-initiated request indexes one bounded metadata-only page, then renders a
  truthful full-library or selected-photo completion message, or a recoverable error.
- **Privacy:** The Activity owns Android's permission launcher. The UI has no direct
  source, Room, or AI calls. The workflow does not open image bytes, modify originals,
  upload content, or schedule background work.
- **Verification:** On 2026-07-20, `IndexingSummaryTest` passed in Android Studio:
  3 of 3 tests passed. The Kotlin, Hilt, Android-test compilation, and local unit-test
  graphs compiled successfully. The user then verified the visible app flow on the
  Medium Phone emulator: it completed with 0 permitted items, a valid empty result.
- **Known limitation:** Android 17 (API 37.1) currently fails Compose
  instrumentation tests before test assertions due to the emulator/test bridge
  expecting the unavailable `InputManager.getInstance` method. This is recorded in
  `CONTINUE.md`; presentation copy is locally tested and the visible flow is manually
  verified until compatible tooling is available.

### Verified setup/indexing ViewModel state boundary

- **Requirements:** P-05, P-14, P-16, P-17.
- **Delivered:** A Hilt ViewModel with immutable state for photo access and one
  explicit indexing request. The ViewModel receives a permission result from the UI;
  it does not request Android permission. It blocks indexing without confirmed access,
  prevents concurrent duplicate requests, preserves selected-photo scope, and exposes
  recovery-safe access or failure outcomes.
- **Dependency note:** Added the official AndroidX Lifecycle ViewModel KTX runtime for
  `viewModelScope`, plus Kotlin coroutines test support for deterministic local state
  tests. Neither dependency adds a permission, network access, source scan, or user
  data collection.
- **Verification:** On 2026-07-20, `MediaStoreSetupViewModelTest` passed in Android
  Studio: 4 of 4 tests passed. The Hilt and Android-test compilation graph passed.
- **Known limitation:** This ViewModel starts one foreground, user-initiated page
  only. Background scheduling and extraction remain future work.

### Verified live MediaStore-to-Room indexing path

- **Requirements:** P-04, P-05, P-14, P-15, P-17.
- **Delivered:** An Android emulator integration test that executes one explicit,
  bounded `MediaStore -> discovery use case -> Room` path using a temporary in-memory
  database. It verifies the returned page's Asset placeholders and opaque checkpoint
  are saved together and that the reported access scope agrees with Android's live
  grant.
- **Privacy:** The test reads only the emulator's existing MediaStore metadata. It
  opens no image bytes, inserts no media, creates no thumbnail, changes no original,
  and leaves no derived records in the normal Memora database.
- **Verification:** On 2026-07-20,
  `IndexMediaStoreImagesIntegrationTest` passed in Android Studio on the Medium Phone
  emulator: 1 of 1 test passed after photo access was granted.
- **Known limitation:** The verified UI starts a foreground page only; background
  work remains intentionally unimplemented.

### Verified controlled MediaStore indexing boundary

- **Requirements:** P-04, P-05, P-14, P-15, P-17.
- **Delivered:** A Hilt-bound application use case that invokes the existing
  checkpoint-driven discovery flow for exactly one explicit, bounded MediaStore page.
  It returns an immutable outcome for a later ViewModel, including the truthful
  distinction between full-library and selected-photo access.
- **Privacy:** This boundary neither requests nor caches a permission, starts itself,
  schedules background work, opens image bytes, creates thumbnails, modifies original
  media, or changes the prototype UI. Without a future explicit caller it is inert.
- **Verification:** On 2026-07-19, `IndexMediaStoreImagesTest` passed in Android
  Studio: 5 of 5 tests passed. The Hilt application and Android-test graphs also
  compiled successfully.
- **Known limitation:** This use case has no user-facing ViewModel or UI caller yet.

### Verified checkpoint-driven discovery invocation

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application use case that checks a source's access state, loads
  only that source's saved opaque checkpoint, requests exactly one bounded page, then
  passes the outcome through the verified result coordinator. It rejects pages that
  claim a different source identity.
- **Privacy:** The use case is inactive until called explicitly, requests no Android
  permission, starts no background work, opens no original content, and has no UI.
- **Verification:** On 2026-07-19, `DiscoverSourcePageTest` passed in Android Studio:
  6 of 6 tests passed.
- **Known limitation:** The real MediaStore adapter is still not bound to this use
  case, so no actual source result is persisted by the running app.

### Verified discovery-result coordination

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application coordinator that sends only a successful discovery
  page to the atomic page-store use case. Missing access, revoked access, and source
  failures remain unchanged and cause no write. A storage exception becomes a
  recoverable, non-sensitive failure outcome.
- **Privacy:** The coordinator opens no source, requests no permission, starts no
  scan, and changes no UI. It does not expose internal storage errors to a user.
- **Verification:** On 2026-07-19, `ProcessDiscoveryResultTest` passed in Android
  Studio: 3 of 3 tests passed.
- **Known limitation:** No source calls this coordinator yet; no actual MediaStore
  discovery page is persisted by the running app.

### Verified atomic discovery-page persistence

- **Requirements:** P-04, P-05, P-14, P-17.
- **Delivered:** An application `PersistDiscoveryPage` use case and a Room-backed
  atomic store. For every successful source-neutral page, it writes all Asset
  placeholders and that page's opaque source checkpoint in one database transaction.
  Unchanged versions retain their current indexing state; changed fingerprints are
  safely returned to `DISCOVERED`.
- **Privacy:** This component receives only source-neutral Asset metadata and opaque
  cursors. It opens no original content, requests no permission, performs no scan,
  starts no background work, and changes no UI.
- **Verification:** On 2026-07-19, local Gradle passed
  `PersistDiscoveryPageTest`: 1 of 1. `RoomDiscoveryPageStoreTest` then passed in
  Android Studio on the Medium Phone emulator: 3 of 3 tests passed.
- **Known limitation:** No platform source invokes this boundary yet; therefore no
  actual MediaStore result is persisted by the app, and restart behavior has not yet
  been verified end-to-end.

### Verified durable discovery checkpoints

- **Requirements:** P-04, P-14, P-17.
- **Delivered:** Room database version 2, a source-owned opaque checkpoint table,
  repository, Hilt binding, exported schema, and explicit migration from version 1.
- **Privacy:** A checkpoint contains only its source ID, opaque source cursor, and
  save time. It contains no original media, thumbnail, text, or semantic memory.
- **Verification:** On 2026-07-19, `RoomDiscoveryCheckpointRepositoryTest` passed in
  Android Studio on the Medium Phone emulator: 2 of 2 tests passed. The real
  `MemoraDatabaseMigrationTest` then passed: 1 of 1 test preserved a version-1 Asset
  across the migration.
- **Known limitation:** This repository is intentionally not yet connected to
  MediaStore discovery. The later atomic page-store boundary is tested, but no
  platform source invokes it yet.

### Verified MediaStore image/screenshot discovery adapter

- **Requirements:** P-03, P-04, P-05, P-14, P-15, P-17.
- **Delivered:** A bounded, read-only `ContentResolver` query adapter for image and
  screenshot metadata; an opaque MediaStore version/watermark/ID checkpoint; and
  runtime access-state handling for full, selected, missing, and revoked access.
- **Privacy:** The adapter never opens image bytes, creates thumbnails, writes to
  MediaStore, requests location, persists scan results, or starts automatically.
- **Verification:** On 2026-07-19, local Gradle compiled the app and passed 5 of 5
  selected mapper/checkpoint unit tests. `MediaStoreImageDiscoverySourceTest` then
  passed in Android Studio on the Medium Phone emulator: 1 of 1 test passed after
  photo access was granted, proving a real read-only catalogue query.
- **Known limitation:** The adapter is intentionally not injected into a worker or
  UI and no cursor/Asset page is stored in Room yet.

### MediaStore adapter design approved

- **Requirements:** P-03, P-04, P-05, P-15, P-17.
- **Decision:** Query only Android-granted image metadata, distinguish full and
  selected-photo access, and use version/generation checkpoints (ADR-009).
- **Privacy:** No media location, image bytes, thumbnail, write operation, automatic
  scan, or user-content test fixture is permitted in this step.

### Verified incremental checkpoint correction

- **Requirements:** P-04, P-14.
- **Decision:** Completed and empty discovery pages retain a durable source checkpoint
  for the next incremental pass (ADR-008).
- **Scope:** Contract correction and unit tests only. No device media or permission is
  accessed.
- **Verification:** On 2026-07-18, `AssetDiscoverySourceTest` passed in Android
  Studio: 3 of 3 tests passed after the correction.

### Verified source discovery contract

- **Requirements:** P-03, P-04, P-05, P-14, P-15.
- **Decision:** Discovery returns bounded, source-specific Asset pages and explicit
  access/failure outcomes (ADR-008).
- **Scope:** Pure Kotlin contract and unit tests only. No device media, document,
  provider, permission, worker, or UI is accessed or changed.
- **Verification:** On 2026-07-18, `AssetDiscoverySourceTest` passed in Android
  Studio: 3 of 3 tests passed. The emulator remained healthy during the run.

### Verified Memory evidence contract

- **Requirements:** P-09, P-10, P-11, P-12, P-13.
- **Decision:** A Memory is versioned by its Asset and every summary/anchor cites
  stored evidence (ADR-007).
- **Scope:** Pure Kotlin domain model and unit tests only. No source is opened, no AI
  is called, no derived content is persisted, and no visible behavior is changed.
- **Verification:** On 2026-07-18, `:app:testDebugUnitTest` passed, including the
  Memory contract tests. The unchanged welcome screen then launched successfully on
  the Medium Phone emulator.

### Verified dependency-injection foundation

- **Requirement:** P-17.
- **Decision:** Hilt 2.60.1 owns application composition (ADR-006), using the existing
  Android legacy KAPT compatibility bridge. Java 17 is configured as required by the
  current official Hilt/Compose setup guidance.
- **Scope:** Application, Room database/DAO/repository bindings, and the Android
  entry point only. No source access, indexing work, permission, network behavior,
  or visible UI behavior is added.
- **Verification:** On 2026-07-18, `:app:testDebugUnitTest` completed successfully;
  Hilt's generated tasks compiled the application graph. The existing automated test
  task was current. The unchanged welcome screen then launched successfully on the
  Medium Phone emulator.

### Verified persistence boundary

- **Requirements:** P-04, P-05, P-09, P-14, P-17.
- **Decision:** Room 2.8.4 with exported schemas; Android legacy KAPT is used for
  Room generation under the current AGP built-in Kotlin toolchain (ADR-005).
- **Verification status:** Initial KSP configuration was rejected by AGP before
  compilation. The documented compatibility path uses legacy KAPT. The Room schema
  path is supplied explicitly to legacy KAPT because the Room Gradle plugin does not
  propagate it through this compatibility bridge.
- **Verification:** `:app:testDebugUnitTest` passed with the generated Room schema.
  On 2026-07-18, `RoomAssetRepositoryTest` passed on the Medium Phone emulator:
  2 of 2 tests passed. The tests verify record round-trip persistence and idempotent
  upsert behavior for a stable source identity.

### Source-neutral domain foundation

- **Requirements:** P-03, P-04, P-05, P-09, P-14.
- **Layers:** Domain only; no UI, Android platform API, data persistence, network, or
  AI dependency was added.
- **Delivered:** Asset types, immutable source identity/location/fingerprint values,
  read-only source capability contract, and validated recoverable indexing lifecycle.
- **Verification:** `:app:testDebugUnitTest` passed on 2026-07-17. Tests cover identity
  validation, source capability safety, valid indexing flow, retry behavior, and
  rejected invalid transitions.
- **Known limitation:** This does not discover, open, persist, or search real assets.
  The next phase is a Room persistence boundary; automatic existing-note access remains
  blocked by ADR-003.
