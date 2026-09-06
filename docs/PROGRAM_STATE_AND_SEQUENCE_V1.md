# Program state and sequence (v1)

**Status:** Verified audit + binding build sequence proposal — awaiting founder acceptance
**Date:** 2026-09-04
**Method:** Code inspection + doc inspection + `testDebugUnitTest` execution. Not
conversational memory. Every claim below has a file, a line, or a test result.
**Process:** `docs/ENGINEERING_CHARTER.md` holistic planning; `docs/GOVERNANCE.md` pre-work gate.

**Authority:** This document does **not** create product authority. It records
**what is actually true today** and proposes a **dependency-ordered sequence**.
Product authority stays with `PRODUCT_SOURCE_REGISTRY`, `PRODUCT_CONTRACT`,
`ARCHITECTURE_FREEZE_v1.0`, `HUMAN_RECALL_ASK_MODEL`, and `DECISIONS.md`.

**Does not authorize:** marketing AVAILABLE, new Find paths, Grounded Answers
runtime, Act, VisionEngine, ADR-052 UI, opening private code.

---

## 0. Executive summary

Three things are true at once, and confusing them is what caused the last round
of churn:

1. **The architecture is right and largely done.** One Memory/evidence substrate,
   one `MemoryBuilder`, one `CanonicalRecall` Find boundary, Live/Dual **N = 0**,
   MIG-01 through MIG-07B landed. Verified in code, not just docs.
2. **The natural-recall product is early.** Of the Ask Model's 15 dimensions,
   18 shapes and 17 jobs, roughly a third are implemented. That is expected at
   this stage — but the codebase currently contains **four defects that silently
   return junk**, which is why the device kept feeling broken.
3. **The truth record has drifted.** Several docs state a status that other docs
   and the code contradict. That drift is how "architecture DONE" got read as
   "recall done."

**Immediate blocker:** the working tree is **red**. `testDebugUnitTest` →
`679 tests completed, 1 failed`.

---

## 1. Verified state (code-checked, 2026-09-04)

| Claim | Verified? | Evidence |
|---|---|---|
| One product Find boundary | **YES** | All 6 UI entry points route through `application/memory/CanonicalRecall.kt`; no `PhotoKeywordSearchV2` / `VideoSearchService` exists |
| Live/Dual N = 0 | **YES** | `LEGACY_RECALL_SURFACE.md`; extraction-DAO search methods now have **no** production callers |
| Domain layer is Android-free | **YES** | 74 files under `domain/**`, no `import android.*` |
| MIG-01…MIG-07B landed | **YES** | Change controls + code; MIG-08…MIG-11 not started |
| No destructive Room migration | **YES** | 14 migrations 1→15; `fallbackToDestructiveMigration` absent; chain tested in androidTest |
| No secrets in repo | **YES** | OneNote client id via `local.properties` → `BuildConfig`; no keystore |
| Meaning Find is honest about vision | **PARTIAL** | `CanonicalRecallWhyCopy` never claims scene/person recognition, but there is **no copy** telling the user we cannot see photos |

**Scale:** 338 main Kotlin files (~31k LOC); 131 unit test files (525 tests);
56 instrumented test files (145 tests); Room schema v15.

---

## 2. Defects found (ranked)

### D-1 — Working tree is red (**P0, blocks any commit**)

```
CanonicalRecallMeaningTest > searchByMeaning_trims_results_to_requested_limit FAILED
    java.lang.AssertionError at CanonicalRecallMeaningTest.kt:60
679 tests completed, 1 failed
```

**Cause, not symptom:** the MF-1.1 lexical precision gate is working correctly.
The test fixture predates it — the query is `"invoice"` but the fixture Memory's
only text is `"first.pdf summary for meaning search"`
(`CanonicalRecallMeaningTest.kt:179`). No `invoice` anywhere, so the lexical AND
filter correctly drops it and the trim-to-limit assertion sees zero hits.

**Correct fix:** put `invoice` in the fixture text so the test still measures
*trimming* and not *filtering*. Do **not** weaken the filter or delete the test.

### D-2 — Lexical precision is skipped whenever a time word appears (**P0, user-visible junk**)

```103:104:MemoraApp/app/src/main/java/com/memora/app/application/memory/AnchorAwareMeaningRecallRanking.kt
        if (constraints.time != RecallConstraintStrength.NONE) return outcome
        if (!MeaningEvidenceLexicalFilter.shouldApply(rawQuery)) return outcome
```

Any query containing `recent`, `recently`, `old`, `older`, `last week`,
`last month`, `last year`, a year, an ISO date, or a month-day **turns the
precision gate off entirely**. `recent files with silky` returns cosine
neighbours with no `silky` in them — exactly the junk the founder reported.
This is registered as anti-case T10 in the scenario bar and is currently violated.

**FIXED 2026-09-04 (A2).** The classifier now also reports `timeSpanText`, the
literal expression it matched. The ranker subtracts those words from the required
content tokens instead of abandoning the gate: `notes in 2024` requires `notes`
and never the literal `2024` (which lives on a TIME anchor), while
`recent files with silky` requires `silky` again. Three anti-cases added, and the
test that encoded the old behaviour as intended was replaced rather than relaxed.

### D-3 — Relative time is a string `contains`, so it can never match (**P1, silent no-op**)

```188:191:MemoraApp/app/src/main/java/com/memora/app/domain/memory/AnchorStructuredRecall.kt
    private fun anchorSatisfiesCue(anchorText: String, cue: String?): Boolean {
        if (cue.isNullOrBlank()) return true
        return anchorText.lowercase().contains(cue)
    }
```

The advisory time cue is the literal phrase `"last week"`; a TIME anchor from
EXIF looks like `2024-03-01`. The boost can never fire. `yesterday`, `today`,
`this morning` and weekdays are not classified at all
(`AnchorStructuredRecall.kt:50-51`). So relative time today costs us D-2's
precision loss and buys nothing.

### D-4 — TOPIC is "advisory" on every query ≥ 4 characters (**P1, waste + landmine**)

```100:103:MemoraApp/app/src/main/java/com/memora/app/domain/memory/AnchorStructuredRecall.kt
            explicitTopicPatterns.any { it.containsMatchIn(query) } ->
                RecallConstraintStrength.EXPLICIT
            query.length >= 4 -> RecallConstraintStrength.ADVISORY
```

…and the topic cue becomes the **entire raw query**
(`AnchorStructuredRecall.kt:114`). A TOPIC anchor would have to contain the whole
sentence, so this never boosts. But because `topic != NONE`, **every** meaning
search pays an extra `findSignatureAnchors` database round-trip and a re-sort.
Registered as anti-case T11; currently violated.

**FIXED 2026-09-04 (A3).** TOPIC is now EXPLICIT-only — it is a constraint when
the person names a title (`titled "March Invoice"`), not whenever a query is four
characters long. A plain cue skips the anchor stage entirely; a test asserts the
lookup count is zero. A topic boost that actually works needs its own cue
extraction and belongs to a later slice, not to a length check.

### D-5 — `and` / `or` / `not` / `all` are deleted as noise (**P1, whole jobs collapse**)

`RecallQueryContentTokens.kt` lists `and` (:103), `or` (:152), `not` (:147),
`all` (:100), `every` (:121) as function words. Consequences:

- `passport and id` → requires **one** file containing both words (J3 becomes J2)
- `silk or silky` → requires **both** spellings in one file (J4 inverted)
- `not the Urdu one` → the `not` vanishes, the Urdu file can still rank

The J4 inversion is the worst: an OR-hedge behaves as an AND, so the user gets
*fewer* results for being helpful about spelling.

### D-6 — Meaning results carry no `evidenceId` (**P2, contract gap**)

`CanonicalRecallResult.kt:76` sets `evidenceId = null` on the meaning path, so
a meaning hit cannot be traced to the specific stored evidence row. This will
block Grounded Answers citations later (`GROUNDING_ARCHITECTURE.md`).

### D-7 — Why can cite a different page than the one that matched (**P2, trust — copy half landed**)

The card and Why used to show the cosine-winning excerpt, while the lexical gate
matches against `precisionText` — **all** stored excerpts for the asset. When
the matching word is on page 7 and the best-cosine page is page 2, Why quoted
page 2.

**Copy half FIXED 2026-09-06 (D-17):** Why no longer repeats that cosine
snippet when the card already shows the word, and when the word lives only in
another stored span it quotes a window around *that* word. The structured
`evidenceId` / page-accurate citation is still open — Why can name the word
and a nearby line, not yet “page 7”.

### D-8 — Meaning index selected a page, not a queue (**P0, corpus reachability**)

**FIXED 2026-09-06.** `MemoryDao.listMeaningIndexSummaries` ordered candidates
by `updated_at_epoch_millis DESC LIMIT :limit` with no reference to
`memory_embeddings`, so every Build tap re-offered the same newest 25 rows.
`IndexMemoryEmbeddings` then correctly skipped them all as unchanged, the
indexed count never moved, and the rest of the corpus was unreachable no matter
how many times the user tapped. Found on device with 25 indexed and 970
pending. Because the same batch drives PDF page, OCR, and note evidence
indexing, OCR evidence for the untouched assets could never be indexed either.

Fixed by selecting against the index instead of the clock: a `LEFT JOIN` on
`memory_embeddings` for the active model identity, admitting a revision when it
has no summary embedding **or** is `STALE_REINDEX_REQUIRED`. That second arm is
load-bearing — `ApplyMig05EvidenceSearchCutover` sets STALE precisely when a
summary embedding exists but evidence embeddings are still owed, so a plain
"not embedded" anti-join would have permanently deadlocked the MIG-05 evidence
drain. Termination holds because the cutover restores those rows to READY once
their evidence lands.

`countMeaningIndexPending(model)` shares the same WHERE clause, replacing the
old `candidates - summaryIndexed` subtraction in `LoadCorpusCompleteness`, so a
non-zero pending count now always means a non-empty batch is actually
selectable.

Verified by `RoomMemoryRepositoryMeaningIndexSelectionIntegrationTest` (7 tests
against real SQLite: disjoint consecutive batches, drain convergence, STALE
stays selectable, fresh-before-evidence-gap ordering, model change re-owes the
corpus, count/select agreement, blank summaries excluded). No schema change;
the migration test still passes.

**Does not fix leftover evidence after summaries are done.** A READY memory
with a summary vector and unindexed OCR / note / remaining PDF pages stayed
invisible: cutover STALE is PDF-and-zero-vectors only, and restore fires on
the first evidence vector. **I1b** admits those rows in the same pending/select
WHERE without mass-STALE. I3 must not start before I1b.

**Does not fix the tap count.** 995 memories at 25 per tap is still ~40 taps.
That is the missing drain driver, tracked as E1/E2 below, and it must not be
automated before **D-9**.

### D-9 — One unusable asset aborts the whole assembly drain (**P0, fixed**)

**FIXED 2026-09-06.** `RunPendingAssetMemoryAssembly` used to return
`FailedSafely` for the entire drain on `NoUsableEvidence`, `AssetMissing`,
`RevisionConflict`, or `FailedSafely`. Nothing was persisted, so
`findNextPendingAsset` returned the same asset on the next run. A later
auto-continuing worker would have been a hot loop on one bad asset.

**Fixed** by recording a durable skip (`memory_assembly_skips`, Room 15→16)
keyed by identity + fingerprint + assembly schema and the digest of the fact
set that failed. Pending select and pending count both exclude active skips.
New OCR / PDF / EXIF / note facts clear the skip so the Asset is pending
again. Only `FailedSafely` (infrastructure) still aborts the drain.

Does not start I2/I3 workers. **A8 closed; Batch I may proceed.**

### D-10 — A time word was a constraint for precision and content for retrieval (**P0, fixed**)

**FIXED 2026-09-06.** `recent files with silky` answered nothing while bare
`silky` returned the right PDF. T10 was working: `recent` classifies as an
advisory TIME constraint and was correctly dropped from the required tokens.
Candidate generation never learned about that decision — `MeaningRecallCue.embedText`
derived tokens straight from `RecallQueryContentTokens`, `recent` is not an
ask-shape wrapper, so the engine embedded `recent silky`. The vector drifted
toward recency language, the one Memory containing `silky` fell out of the
candidate pool, and the precision gate had nothing left to keep.

**The defect was the disagreement, not either half.** Two layers each derived
"what the person named" independently, so the product could filter on a word it
had never retrieved on. Fixed by making `MeaningRecallCue.contentTokens` the
single derivation that both read, guarded by an invariant test that fails if
embed text and required tokens ever separate again.

Recorded consequence: a cue made only of time words (`recent files`,
`screenshots from last week`) now names no content and returns an honest empty.
Retrieval by TIME alone remains an unbuilt job (I5).

### D-11 — Cosine decided reachability, not just rank (**P0, fixed**)

**FIXED 2026-09-06.** `CanonicalRecall.searchByMeaning` requests a pool of
`min(limit * 3, 30)` candidates and that pool was chosen by cosine alone
(`deduped.take(limit)`). Every stage after it can only subtract, so a Memory
holding the exact words the person named was unreachable unless the embedding
had already ranked it in the top 30.

**This is why device queries that passed at 25 memories failed at ~1000**: the
pool was the whole library, then became roughly 3% of it. Not a regression from
the precision work — the corpus outgrew the pool.

Fixed by lexically-aware admission: candidates whose stored text satisfies every
named word claim seats first, remaining seats keep the best cosine neighbours,
and the pool is still returned in cosine order because ranking belongs to
Canonical Recall, not to candidate generation. `MeaningEvidenceLexicalFilter.prepare`
compiles the cue once per query so corpus-wide admission does not pay a `Regex`
build per candidate.

**Completed by D-15 (2026-09-06):** the original fix reserved seats only for the
exact AND. Once D-12 made precision a tier, a two-word cue with no exact match
fell back to cosine-only admission — D-11 one level down.

### D-12 — The lexical AND was a veto, so meaning could not answer a paraphrase (**P0, fixed**)

**FIXED 2026-09-06.** Found on device *after* D-11 landed. `swimming timetable`
worked; `swimming schedule` returned nothing — against the same PDFs. The
meaning model ranked those timetables correctly, and the lexical AND gate then
removed every one of them because none contains the literal word `schedule`.

**This was the deepest defect of the session.** Embeddings were being used only
to *order* results they were never allowed to *find*, which makes Meaning Find
behave as keyword AND search with a meaning label. `silky` worked solely because
the PDF happens to say `silky`. A person who remembers the idea but not the
wording — the entire premise of the product — got a blank screen.

The empty state then said nothing was "close enough with the on-device meaning
model", blaming the meaning model for a decision the gate had made. That is a
second, independent trust defect and is fixed with it.

**Fixed** by making precision a **tier** rather than a veto (`RecallPrecision`):
hits carrying every named word win outright — the exact path is byte-for-byte
today's behaviour — and when no hit carries all of them the list falls to the
deepest tier available and *states what it could not match*. `swimming schedule`
now returns the timetables under "Nothing saved on this phone has "schedule".
These match "swimming"."

**This is not a synonym net.** UNFYND still never asserts that `schedule` means
`timetable` (`RecallQueryContentTokens` is explicit that `silky` ≠ `smooth`). It
reports what it matched and what it did not, and lets the reader judge — which
is also how it teaches a person what their own corpus actually says.

Ask Model: satisfies **P-AND vs P-LIST** (a descriptive phrase is not a
conjunction the person intends) and **P-ANSWER / P-EVIDENCE** (a partial answer
presented as a whole one is worse than an empty one). Live/Dual **N = 0**
unchanged; no new Find path, no new ranker.

### D-15 — Admission reserved seats only for the exact AND (**P0, fixed**)

**FIXED 2026-09-06.** D-11 reserved a pool seat when stored text satisfied
*every* named word. D-12 then made precision a tier, so `swimming schedule`
became a legitimate partial answer — but admission still treated "no exact AND"
as "no lexical claim", and the swimming-timetable PDF had to win a cosine seat
or the new tier had nothing to show. At ~1000 memories that is the same
reachability hole as D-11, one level down.

**Fixed** by admitting on *coverage depth*: how many named words the Memory
carries, using the same `PreparedCue.matchingTokens` that D-12 ranks with.
Deeper coverage claims seats first; remaining seats stay cosine neighbours;
the pool is still returned in cosine order. No second lexical derivation, no
new Find path, no ranking in candidate generation. Live/Dual **N = 0**.

Zero-overlap paraphrase (`kids water lessons` against a file that says
neither word) is still an honest empty. That is the meaning-only tier, not
this defect.

### D-13 — `notes` is only ever a content word (**P1, open — J7 / P-TYPE**)

`notes in 2026` returned screenshots containing the string "Note:" rather than
Note assets. The lexical gate is behaving correctly; the product model is
missing. **J7 / P-TYPE** requires that a type noun can act as a filter over
`AssetType` as well as content, chosen by context, without losing the ability to
find the literal word when that is what was meant.

### D-17 — Why claimed every cue word appeared (**P0, fixed**)

**FIXED 2026-09-06.** Found on device after D-12. The list banner told the
truth (`Nothing saved has "schedule". These match "swimming".`) and every
opened Why contradicted it: *"Your cue words appear in that saved text."*
That line fired whenever *any* cue word had been token-boosted. It was written
for the old exact-AND world and was never updated when precision became a
tier.

The same Why also repeated the query, filename, type, page, and snippet the
card already showed — the U5 anti-pattern ("duplicate full card excerpt").

**Fixed** by making Why answer one question: which of *this file's* named
words are present. `Has "swimming". Does not have "schedule".` A coloured
panel with a primary bar, bold matched words, and error-coloured missing
words so it is not another muted paragraph. A cited line appears only when
the matching word is not already on the card.

Ask Model **P-EVIDENCE** / **P-ANSWER**; scenario bar U5. Live/Dual **N = 0**.

### D-14 — UNFYND's own screenshots compete as corpus (**P0, fixed — D16**)

**FIXED 2026-09-06.** Found on device: `Screenshot_20260904_124145_UNFYND.png`
ranked above `Grade-2-Swimming-TT-2026.pdf` for `swimming schedule`, and Why
said "This file is What are you trying to remember? files have swimming
timetable" — the product chrome, treated as the file's identity.

**Fixed** by detecting self-captures from stored chrome (two UI phrases, or
one plus an UNFYND filename) and demoting them *after* ranking but *before*
the trusted-hit trim, so a high-cosine picture of the app cannot set the
trust band and drop the original. Why names them "a screenshot of UNFYND,
not the original file" and does not quote the chrome. A real timetable is
untouched. Live/Dual **N = 0**; no new Find path.

---

## 3. The earlier "holes" — are they in the Ask Model?

**Short answer: the language holes yes; the system holes no.**

`HUMAN_RECALL_ASK_MODEL.md` is a model of **how people ask**. Section E of
`MEANING_FIND_PRODUCT_SCENARIO_BAR.md` ("Registered P2 holes", I35–I46) plus the
OPEN rows in sections C–D contain two different kinds of item, and only one kind
has a home in the model.

### 3.1 Covered — every phrasing hole maps to a class

| Hole | Ask Model class |
|---|---|
| I35 quoted exact phrase | S12 |
| I36 `all` / `every` | S18 |
| I37 "the one I opened yesterday" | D5 |
| I38 named days, ranges | D4 + P-TIME (weekdays named; festivals/ranges thin) |
| I40 existence | J12 |
| I41 "that one" / more-like | S17 |
| I42 folder / album | D3 |
| I43 visual-only | D11 / J9 |
| I44 mixed languages | D13 |
| I22 typos | S11 + P-TYPO |
| I3 / I4 lists and hedges | S13 / S14, J3 / J4 |
| I34 "on my computer" | *(see 3.3 — gap)* |

### 3.2 Not covered — the machine-and-corpus holes have no class

These rows in the scenario bar map to **nothing** in the Ask Model. By the
model's own process rule ("a device phrasing that has no class in this model is a
**model defect**"), that is a defect to close before W1 — otherwise a wave can be
declared done while these stay unowned forever:

| Hole | Why it matters |
|---|---|
| **I16** open failed / file moved / permission revoked | Find succeeded, Open failed — different failure, different copy |
| **I19** near-duplicate disambiguation | Five almost-identical spelling lists; picking one at random destroys trust |
| **I27** duplicates / versions of the same original | Duplicate walls |
| **I28** garbage OCR producing a false "contains word" | We would cite evidence that is nonsense |
| **I29** huge library latency | 10k memories is unmeasured; `listForModel` loads the full index per query |
| **I31** searching while indexing | Torn or unstable results |
| **I32** privacy-sensitive cue (medical, passwords) | Query content is itself sensitive |
| **I46** permission lost mid-search | Stable failure, not a crash |

### 3.3 Not covered — one dimension and one decision are genuinely missing

- **Device / corpus scope.** "where is silky **on my phone**", "**on my computer**"
  (I10 / I34). There is no dimension for *which corpus the user thinks they are
  searching*. D14 is source/channel (WhatsApp, Camera), which is not the same
  thing. This matters the moment desktop exists, and it matters **now** because
  we must not treat "phone" as a content word.
- **One box vs two boxes.** The founder asked directly why "Find by saved text"
  exists alongside "Find by meaning". The Ask Model says keyword Find stays
  "until one box" (§2, §10) but **no ADR decides the merge**, and no acceptance
  bar exists for a unified box. This is a live product decision, not a detail.

### 3.4 Proposal — Ask Model v1.1, not a second document

Keep one ceiling. Amend `HUMAN_RECALL_ASK_MODEL.md` with:

1. **§3 add D16 — Device / corpus scope.** Today: wrapper noise, never a content
   token, never a place filter. Later: a real corpus selector.
2. **New §5b — Result and system classes R1–R8** covering I16, I19, I27, I28,
   I29, I31, I32, I46. Each with: what the user experiences, what we must say,
   and the anti-case. These are **not** jobs (the user did not ask for them);
   they are conditions the answer must survive.
3. **§10 add W1.5 — one-box decision** as an ADR gate, sequenced after W1 and
   before W2, so keyword and meaning stop being two products in the user's head.

Do this as one amendment accepted together with v1. It is the difference between
"we did not think of it" and "it is on the register with an owner."

---

## 4. Truth drift in the documentation (exact fixes)

The audit found the same fact stated three different ways. Each line below is a
one-line correction, not a rewrite.

> **APPLIED 2026-09-05 (A4).** All nine landed. Three deviations from the table,
> each deliberate: (a) `CONTINUE.md` line numbers had drifted, so the fixes were
> made by content — and two further standing claims contradicting the same facts
> were corrected with them (the MIG-07B docs note, and "Next eng default" item 1,
> which still pointed engineers at a program exit closed on 2026-08-31);
> (b) dated snapshots such as **Status truth check 2026-08-30** were left intact
> and the surrounding claims date-stamped instead — the diary is history, not
> current truth, and A5 is where that separation gets made structural;
> (c) `PHASE_A_IMPLEMENTATION_PLAN_V1.md` carries a **SUPERSEDED** banner rather
> than being moved to `docs/archive/`, so inbound links keep working; the physical
> move belongs to A5. `PRD_TRACEABILITY.md` rows were set to their *actual* state
> with evidence pointers, not flipped to a flat "Delivered" — P-01 and P-12 remain
> honestly partial because Ask Model coverage is still **FAIL**.

| File:line | Says | Truth | Action |
|---|---|---|---|
| `CONTINUE.md:277` | Live/Dual = **2** | **0** | Fix |
| `CONTINUE.md:351` | `RECALL_CONVERGENCE_DONE` "still open" | **COMPLETE** (`RECALL_CONVERGENCE_DONE.md:3`) | Fix |
| `CONTINUE.md:204` | "**Not** RECALL_CONVERGENCE_DONE" | Same as above | Fix |
| `CORE_APP_SEPARATION_PLAN.md:46` | Live/Dual **N = 2**; L7/L8 outside | N = 0; MIG-07B landed | Fix |
| `PRODUCT_SOURCE_REGISTRY.md:76` | N = 5 | 0 | Fix |
| `MVP_EXIT_AUDIT.md:31` | "MIG-05 claim B all MVP types" | Engineering delivered; **ADR-050 claim B is still OPEN** for Spec-full | Re-word or close B by ADR |
| `PHASE_A_IMPLEMENTATION_PLAN_V1.md:22` | migrations "not yet implemented" | MIG-01–07B landed | Archive as superseded |
| `ROADMAP.md:18` | A-01 open | **PASS** 2026-09-02 | Fix |
| `PRD_TRACEABILITY.md:9-21` | P-01/02/08/10/12/16 "Planned" | Delivered | Refresh |

**Root cause, worth fixing once:** `CONTINUE.md` is ~1,400 lines of build diary
with the current truth scattered through it. Recommendation: a single **"Current
truth"** table at the top (one row per fact, one evidence pointer each), and move
everything older than the current phase to `docs/archive/`.

**MIG-05 claim B must be settled, not repeated.** Either write the ADR that
closes B against MVP scope, or stop writing "claim B closed" in the changelog and
audit. Right now both statements exist in the repo.

---

## 5. Stale and dead code register

Nothing here is urgent, and none of it should be swept into a feature slice.
One dedicated cleanup commit, with tests green before and after.

| Item | Location | Action |
|---|---|---|
| `searchCurrentPages` | `PdfExtractionDao.kt:103` | Delete — no production caller since MIG-07 |
| `searchCurrentOcrText` | `ScreenshotOcrExtractionDao.kt:72`, `PhotoOcrExtractionDao.kt:62` | Delete |
| `searchCurrentNoteText` | `NotePageExtractionDao.kt:72` | Delete |
| `countCurrentSearchable*` | extraction DAOs | Keep only what `MemoraDatabaseMigrationTest` uses; delete the rest |
| `rebuild_index_message` | `res/values/strings.xml:14` | Unused; text is duplicated at `ClearMemoraDerivedData.kt:57` — keep **one** |
| `CanonicalRecall.searchHybrid()` | `CanonicalRecall.kt:97` | Keep, but it is unwired — it belongs to the one-box decision (§3.3) |
| `MainActivity.kt` | ~2,304 LOC incl. the whole PDF search UI | Extract `PdfKeywordSearchScreen` to `ui/search/` — matches the other four surfaces |

Positive findings worth keeping: **zero** `TODO`/`FIXME`/`HACK` markers, **zero**
`@Deprecated`, **zero** `GlobalScope`, no commented-out code blocks, no
forbidden parallel Find classes.

---

## 6. Enterprise-grade gaps outside Find

These are what stands between "works on the founder's phone" and "shippable
product". None are Find-quality work; all are real.

### 6.1 Quality gates

| Gap | Detail |
|---|---|
| No static analysis | No ktlint, detekt, or spotless anywhere |
| Release build not minified | `optimization { enable = false }`; no ProGuard/R8 rules file |
| CI runs unit tests only | No `lint`, no instrumented tests, no release-build check |
| No JVM migration tests | Room migrations tested only on a device/emulator |
| `data.intelligence` has no unit tests | Includes `StageARecallRanker` — the CE path |

### 6.2 Product surfaces

| Gap | Detail |
|---|---|
| Accessibility | `contentDescription` appears in 4 files; **absent** from `MeaningSearchScreen` and all setup screens |
| Localization | All Find and setup copy is hardcoded in `*Copy.kt` objects, not string resources — blocks D13 permanently |
| Error taxonomy | No user-facing error model; failures are per-screen strings |
| Onboarding UX | ADR-052 accepted as policy; UI not shipped |

### 6.3 Missing documents (checked; genuinely absent)

Threat model · privacy policy (user-facing) · data retention & deletion policy ·
accessibility specification · **performance budgets** (this one blocks AVAILABLE
per `ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` §C) · release/QA plan ·
support & versioning policy · backup/restore policy.

---

## 7. UNFYND Core — what to add, and when

**Today:** Class A is docs + 5 synthetic examples + 1 JSON schema + 1 Python
validator (validator self-test passes). It is honest about being Class A. It does
**not** let an outside engineer implement or run anything.

### 7.1 Two honest problems now

1. **Present-tense capability copy.** `README.md:44`, `SPEC.md:23-27`,
   `APPLICATIONS.md:41-43` all say "Core provides ingest… recall and ranking…
   answers tied to evidence". True of the **private App**; not of the **open
   pack**. Quoted out of context in a grant deck, that is an overclaim. Fix is
   one clause, not a rewrite: say which layer provides it today.
2. **The validator does not use the schema.** `tools/validate_class_a_examples.py`
   re-implements a subset of the rules in Python and never loads
   `schema/memory-evidence-sketch.schema.json`. Two sources of truth that can
   drift. Also: the schema allows any string for anchor `type`, while the private
   model has a closed `MemoryAnchorKind`.

### 7.2 Sequenced Core additions

**Now (no private code opened, no product cost):**

- C-1 Fix the present-tense copy in README / SPEC / APPLICATIONS
- C-2 Make the validator schema-driven; add a `version` field to the schema
- C-3 Expand negative fixtures: `RETRIEVAL_SIGNAL`-only justification,
  `HYPOTHESIS`-only anchor, orphan export slice ref, malformed locator
- C-4 Public CI workflow in the published repo (today CI is private-only, while
  `ROADMAP-OPEN.md:39` says "Delivered")
- C-5 OSS hygiene: `CODE_OF_CONDUCT`, issue/PR templates, a versioning policy,
  first semver tag
- C-6 Decide whether `CITATIONS.md` (which lists private doc paths) should ship
  publicly at all

**Per wave, as the App earns it:**

| After | Add to Core |
|---|---|
| W1 (lists/hedge) | Publish the **cue → jobs** contract: how one sentence becomes several retrieves |
| W2 (type + time) | Publish the **TIME anchor + resolution** contract (this is genuinely novel and worth being public) |
| `:core-domain` module lands | Class B candidate: portable Kotlin `Memory` / evidence / anchor types + `DeterministicMemoryBuilder` + tests |
| Recall API stable | Public **Canonical Recall** request/response spec — currently the single biggest spec gap |
| Grounded Answers slice | Evidence package + citation contract |

**Extraction map (already good news):** `domain/**` is 74 files with **no Android
imports**, so the Core boundary is real, not aspirational. The `:core-domain`
Gradle module (Phase 2, currently **FAIL** in the MVP audit) is mostly mechanical.

---

## 8. The sequence

Ordered by dependency. Each step ends with a gate; nothing starts before its gate.

### Stage 0 — Make the tree honest (hours, not days)

| # | Task | Gate |
|---|---|---|
| 0.1 | Fix `CanonicalRecallMeaningTest` fixture (D-1) | `testDebugUnitTest` green |
| 0.2 | Fix D-2 (lexical gate must survive TIME) + D-4 (no advisory TOPIC on every query) with anti-case tests T10/T11 | Tests green; junk-on-`recent` gone |
| 0.3 | Correct the doc drift in §4 (7 one-line fixes) | One truth per fact |
| 0.4 | Founder accepts **Ask Model v1 + v1.1 amendment** (§3.4) | Change control checkbox |

**Why 0.2 is here and not in a wave:** it is a defect against an already-declared
contract, not new capability. Shipping W1 on top of a precision gate that
switches itself off would repeat exactly the last failure.

### Stage 1 — MVP exit (the FAIL rows)

MVP exit has **4 FAIL** rows. Two are Find, two are measurement.

| # | Task | Closes | Gate |
|---|---|---|---|
| 1.1 | **W1** — J3 grouped lists + J4 hedge; stop deleting `and`/`or`; Why names which cue matched | NL intent (part) | JVM class tests + P0 device checklist |
| 1.2 | **W2** — type as filter (J7) + real relative-time resolution incl. weekdays (J8, D-3) | NL intent (part) | Frozen-clock tests; EXIF fixtures |
| 1.3 | **W3** — bounded typo repair with anti-cases | NL intent (part) | `silkky`→`silky` passes, junk stays out |
| 1.4 | Record **battery / latency budgets** | AVAILABLE blocker | Enterprise completion §C |
| 1.5 | Settle **ADR-050 claim B** (close it or stop claiming it) | Truth | ADR |
| 1.6 | `:core-domain` Gradle module | MVP audit §E FAIL | Module builds; domain purity CI stays green |
| 1.7 | Founder **AVAILABLE** review | Marketing FAIL | Recorded decision |

**Note on 1.1–1.3:** NL intent coverage stops being FAIL when the **in-scope**
waves pass, not when every I-ID closes. Vision and kinship are out of MVP by
design and must stay out of the exit bar.

### Stage 2 — Trust hardening (before anyone else's phone)

| # | Task |
|---|---|
| 2.1 | R-classes from §3.4: duplicates, garbage OCR, open-failure, permission loss, search-during-index |
| 2.2 | Honest copy for the §7 fixture: *we can search words and dates we have; we cannot yet recognise people or scenes in photos* |
| 2.3 | Accessibility pass + move copy into string resources |
| 2.4 | Static analysis + minified release + lint in CI |
| 2.5 | Latency at 10k memories (I29) — `listForModel` currently loads the whole index per query |
| 2.6 | Threat model, privacy policy, retention policy |
| 2.7 | Stale-code cleanup (§5) |

### Stage 3 — One box (product decision, then build)

ADR: does keyword Find remain a separate screen? Today the user must know which
box to use — that is a query language by another name, in UI form.
`CanonicalRecall.searchHybrid()` already exists and is unwired. This is the
cheapest large trust win available.

### Stage 4 — New substrate (this is where the photo fixture unlocks)

| # | Task |
|---|---|
| 4.1 | **W4** — VisionEngine + PERSON/PLACE anchors through `MemoryBuilder` (ADR required) |
| 4.2 | Kinship linkage (D7) — a person the user names, not OCR of the word "daughter" |
| 4.3 | `show me the photo of my daughter in swimming pool last saturday` finally means what the user means |

### Stage 5 — Answers and reach

W5 Grounded Answers after retrieval quality (needs D-6 fixed first) · MIG-08…11 ·
audio/video · cloud connectors · Act · Core Class B.

---

## 8b. Execution backlog (numbered, durable)

§8 is the reasoning. This is the **work list**, numbered so any future session can
pick it up by ID. Batch letters map to the stages above.

**Done 2026-09-04:** this document; Ask Model **v1.1** (D16, §5b R1–R8, one-box at
§2 + wave W1.5, anti-case-as-test at §11); change-control boxes;
`CORE_CAPABILITY_REGISTER_V1.md` (CR-01…CR-09); **A1** red-test fix;
**A2** (D-2, T10) and **A3** (D-4, T11) — Stage 0 code complete, 682 tests green.

### Batch A — Stage 0: make the tree honest

| # | Task | Kind | Status |
|---|---|---|---|
| A1 | Red test `CanonicalRecallMeaningTest` fixture (**D-1**) | Code | **done** |
| A2 | Lexical precision gate must survive TIME cues (**D-2**) + T10 anti-case | Code | **done** |
| A3 | Drop blanket advisory TOPIC on every query ≥ 4 chars (**D-4**) + T11 | Code | **done** |
| A4 | Nine doc-drift corrections (§4) | Docs | **done** |
| A5 | `CONTINUE.md` "Current truth" table; archive diary to `docs/archive/` | Docs | open |
| A6 | **Founder:** accept Ask Model v1 + v1.1 | Decision | open |
| A7 | Meaning index must select unindexed work, not the newest page (**D-8**) | Code | **done** |
| A8 | Terminal per-asset outcomes must advance the assembly cursor (**D-9**) | Code | **done** |

> **A2 note:** `CanonicalRecallMeaningTest.searchByMeaning_wires_anchor_ranking_for_explicit_time_query`
> passes today *because of* D-2 — its fixtures do not contain "notes". Fixing D-2
> will turn it red. Fix the fixture with it; do not weaken the gate.

### Batch I — Indexing drains (runs after A7/A8, before Batch C)

Assembly and meaning indexing are the only two pipelines with no WorkManager
driver, so they are the only two the user has to hand-crank. Discovery, EXIF,
photo OCR, screenshot OCR, PDF discovery, PDF extract, and OneNote already run
the `Worker` + `Scheduler` + `DecisionMapper` trio that re-enqueues itself while
work remains. `RunPendingAssetMemoryAssembly` already returns `hasMore`; nothing
listens to it.

| # | Task | Kind | Status |
|---|---|---|---|
| I1 | Lift meaning-index orchestration out of `AiPackDisclosureViewModel` into an application use case returning `hasMore` (also clears a standing UI→application boundary violation) | Code | **done** |
| I1b | Meaning-index pending/select includes leftover embeddable PDF / OCR / note evidence (not only missing summaries / STALE) | Code | **done** |
| I2 | `AssetMemoryAssembly` worker trio + in-app progress and Stop | Code | **done** |
| I3 | `MeaningIndex` worker trio + in-app progress and Stop | Code | open |
| I4 | Replace count-only batch caps with a wall-clock budget plus a count backstop; derive both from measured per-item cost on device, and record the basis | Code + Measurement | open |

> **I4 rationale:** a count cap cannot bound work whose per-item cost varies by
> two orders of magnitude. One observed tap indexed 25 memories but 49 PDF
> pages; a 300-page PDF and a one-line screenshot are both "1". Time budgets are
> already an idiom here — see `PdfExtractionWriteBudgets.MAX_PERSIST_ELAPSED_MS`.

> **Hard ordering:** I2 and I3 must not land before **D-9 (A8)** — delivered;
> I2 is done. An auto-continuing worker over the old abort-on-bad-asset drain
> would have been a hot loop. I3 must also not land before **I1b** — delivered;
> the meaning-index worker would otherwise see `NothingPending` while leftover
> evidence remains.

### Batch B — Register the unmodelled

| # | Task | Kind | Status |
|---|---|---|---|
| B1 | `CORE_CAPABILITY_REGISTER_V1.md` — CR-01…CR-09 | Docs | **done** |

### Batch C — Find→Answers join, then W1

| # | Task | Kind |
|---|---|---|
| C1 | `evidenceId` on the meaning path (**D-6**) — the seam between Find and every answer | Code |
| C2 | **W1** — J3 grouped lists + J4 hedge; stop deleting `and`/`or`; Why names the cue; R1–R5 on cards | Code |
| C3 | MF-1.3 — `with` (same file) vs list glue (several files) | Code |
| C4 | Why must cite the excerpt that actually matched (**D-7**) | Code |

### Batch D — W2, W3, one box

| # | Task | Kind |
|---|---|---|
| D1 | **W2** — type as filter (J7) + real relative-time resolution incl. weekdays (J8 / **D-3**) | Code |
| D2 | **W3** — bounded typo repair with anti-cases | Code |
| D3 | **W1.5** one-box ADR (`searchHybrid()` exists, unwired) | Decision + Docs |

### Batch E — Close MVP exit

| # | Task | Kind |
|---|---|---|
| E1 | Record battery / latency budgets | Measurement |
| E2 | Settle ADR-050 claim B — close it or stop claiming it | Decision + Docs |
| E3 | `:core-domain` Gradle module | Code |
| E4 | **Founder:** AVAILABLE review | Decision |

### Batch F — Enterprise grade

| # | Task | Kind |
|---|---|---|
| F1 | Implement R-classes R1–R8 | Code |
| F2 | Vision-honesty copy (we cannot yet recognise people or scenes) | Code |
| F3 | Accessibility pass; move `*Copy.kt` strings to resources | Code |
| F4 | CI: static analysis, minified release + R8 rules, lint, JVM migration tests, recall-quality gate | Build |
| F5 | Latency at 10k memories (`listForModel` full-index scan) | Code |
| F6 | Missing docs: threat model, privacy policy, retention/deletion, performance budgets, release/QA, error taxonomy | Docs |
| F7 | Stale-code cleanup (§5) | Code |

### Batch G — UNFYND Core public pack

| # | Task | Kind |
|---|---|---|
| G1 | Fix present-tense "Core provides" copy (README / SPEC / APPLICATIONS) | Docs |
| G2 | Make the validator schema-driven; version the schema | Code |
| G3 | More negative fixtures | Code |
| G4 | Public CI in the published repo | Build |
| G5 | OSS hygiene: code of conduct, templates, versioning policy, semver tag | Docs |
| G6 | Decide whether `CITATIONS.md` ships publicly | Decision |
| G7 | **Public Canonical Recall API spec** — biggest external spec gap | Docs |

### Batch H — Vision scale

| # | Task | Depends on |
|---|---|---|
| H1 | **W4** — VisionEngine + PERSON/PLACE anchors (ADR) | Substrate |
| H2 | Knowledge stages Link → Event → Knowledge (**CR-09**) | W1–W3, CR-01 |
| H3 | Conflict reconciliation + evidence-class-aware answers (**CR-01, CR-02**) | H2 |
| H4 | Grounded Answers runtime (**W5**) | C1, §14 gates |
| H5 | MIG-08…MIG-11 incl. ANN for facility-scale corpora | — |

### Decisions only the founder can make

Ask Model acceptance (A6) · one box vs two (D3) · AVAILABLE (E4) · ADR-050
claim B (E2) · `CITATIONS.md` public or not (G6) · the CR-07 aggregation stance.

---

## 9. What this changes about how we work

The last cycle failed in a specific, fixable way: **we discovered the product
during implementation.** The fix is already written down — the Ask Model's
process rule and the charter's holistic-scenarios requirement. Two additions:

1. **Anti-cases are tests, not prose.** T10 and T11 were written in the scenario
   bar and violated in code at the same time. A registered anti-case with no test
   is a wish.
2. **A wave ends on its checklist, not on the next phrasing.** When a new sentence
   appears mid-wave, classify it and log it. It joins the next wave. It does not
   open a slice.

---

## Document history

| Date | Change |
|---|---|
| 2026-09-04 | v1: verified state, 7 defects, holes analysis, doc drift, Core sequence, stages 0–5 |
