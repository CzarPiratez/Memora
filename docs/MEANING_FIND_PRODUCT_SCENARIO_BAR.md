# Meaning Find — Product, Intent Register & Scenario Bar (v2)

**Status:** Binding execution bar for Find by meaning (I-* IDs)  
**Natural recall language:** `docs/HUMAN_RECALL_ASK_MODEL.md` is the **ceiling**.
I-* IDs **map** to that model. Do not implement NL Find from screenshots or from
this bar alone if the Ask Model is silent on the class.  
**Date:** 2026-09-04  
**Process:** `docs/ENGINEERING_CHARTER.md` holistic scenario planning  
**Architecture:** Canonical Recall only (Live/Dual N = 0). No second Find path.  
**Does not authorize:** marketing AVAILABLE, ADR-052 UI, Grounded Answers runtime,
Act, synonym nets, cross-lingual-without-tokens, or new Live/Dual rows.

**Honesty of claims (read first):**

| Claim people might hear | Truth |
|---|---|
| Canonical Recall / Live/Dual N = 0 | Architecture **DONE** — sole Find boundary |
| MIG-07B Slices 1–4 | Structured TIME/TOPIC **filter stage** DONE — not “last week works” |
| MF-1 | One-cue precision **landed** (U1–U8 / I1 partial); wrappers still leak |
| Product Contract “recall by time / person / place” | TIME/TOPIC **populated**; person/place/object **not**; relative time **not resolved** |
| Grounded Answers / “what happened last week?” as prose | **Blocked** until readiness gates; Find must not fake answers |
| Marketing AVAILABLE | **NO** |

v1 of this bar was screenshot-narrow (`silky`). v2 is the **execution register**.
The **ceiling** for how people ask is `HUMAN_RECALL_ASK_MODEL.md`.

---

## North star (user value)

A person asks the way they remember — a word, a phrase, several things in one
breath, a time window, or a short question — and gets a **short, trustworthy
list of the right originals**, with a **plain Why** they can understand in
seconds. Prefer silence or “no solid match” over bluffing.

**Wow bar:** Feels like a careful friend who read your files — not “top-10 cosine
demo,” not a chatbot essay, not a power-user query language the user must learn.

**Language for this bar:** English recall cues. Not “any sentence on the globe.”

---

## Innovative stance (within architecture)

| Commodity default | UNFYND choice |
|---|---|
| Embed raw NL; show top 10 vectors | **Recall cue object**: normalize → content tokens → embed content cue; precision gate; short trusted list |
| User learns AND/OR syntax | **Intent classes** inferred from ordinary English; list glue ≠ default same-file AND |
| Relative time as string contains | **Resolve** `yesterday` / `last week` to a date window vs parsed TIME anchors |
| Hide Find behind Ask | Find stays the safety rail; answer-seeking **abstains or hands off** — never fake an answer |
| Separate “smart search” product | Same **Canonical Recall**; keyword remains exact evidence path (later: one box, two generators) |
| Synonym explosion (silk → smooth) | **Forbidden** without ADR — brings junk back |
| Engineer Why / hash titles | **Consumer Why dialect**; matching cues including time when used |

---

## Surface ownership (do not mix constitutions)

| Surface | Returns | May do | Must not do |
|---|---|---|---|
| **Find** (this bar) | Memory cards + Why | Retrieve, filter, group, honest empty | Invent facts, passport numbers, summaries |
| **Grounded Answers** | StructuredAnswer + citations | Extract / summarize / timeline **after** readiness | Silent Find replacement; unsupported prose |
| **Act** | Do something | Open / share / later remind | Pretend search “did” the action |

A query may **sound** like Ask while the user is on Find. That is **I-MISROUTE**
(Find-owned honesty), not permission to start Grounded Answers code.

---

## Intent Register

Every material Find/NL slice must map to **I-*** IDs below. Silence in an ADR
does not retire an ID. New journeys found on device **add rows**; they do not
replace this register.

**Status:** `OPEN` = not meeting bar · `PARTIAL` = some paraphrases/fixtures ·
`MF-1` = covered by precision slice · `DEFERRED` = owned elsewhere or blocked
on missing substrate · `OUT` = explicit non-goal for English Find v1.

### Priority freeze (distinguished rule)

Do **not** grow this register as a substitute for shipping P0. New IDs only when
a **real device query** has no home, or a founder/product decision adds a class.

| Layer | Meaning | Blocks AVAILABLE talk? | Code now |
|---|---|---|---|
| **P0** | English Find v1 — how people ask for files | **Yes** | MF-1.1 → MF-1.2 / 1.3 |
| **P1** | Find hardening on existing substrate (TIME, honesty copy) | Yes before AVAILABLE | After P0 green on device |
| **P2** | Registered so we do not “never think of it” | No | Not until P0 (+ agreed P1) |

| IDs | Layer |
|---|---|
| I1 wrappers, I2 same-file AND, I3 grouped lists, I4 hedge, I5 filler, I6 keyword still offered, I13 empty, I14 pack, I17 Why (ungrouped), I30 CE | **P0** |
| I7 remainder, I8 relative time, I9 browse, I10 type, I15 not-indexed-yet, I16 open-failed, I20 meta, I21 misroute, I24 v1 copy, T10–T11 | **P1** |
| I11 person/place, I12 homonym polish, I18 refine, I19 duplicates UX, I22 typos, I23–I34 remainder, I35–I46, G*, ACT1 | **P2** or other constitution |

### P0 phase exit (binding — stop the reactive loop)

Chasing each device miss (`show me`, then `doc`, then `scan`, then `e.g.`) is **not**
how this phase ends. English has no finite list of sentences. The workstream is
done when the **contract** below is green — not when the founder runs out of
new phrasings.

**What we freeze for I1 (one thing, many phrasings)**

1. **Mechanism, not anecdotes.** Ask-shape = closed **classes** in
   `RecallQueryContentTokens` (ask verbs, type nouns, function words, discourse,
   `e.g.`-style abbrev). A new miss is classified into a class (or P2). It is
   **forbidden** to treat “add this one word the founder typed” as the plan.
2. **Generator contract (JVM, must stay green).** For a domain token `T`
   (`silky`, `scan`, `timetable`), every prefix/suffix in the P0 generator
   (see `MeaningFindP0AskShapeContractTest`) must yield **exactly** the same
   content tokens as bare `T` (plus any extra domain words we did not strip).
   Filler-only strings (no `T`) must yield **no** content tokens.
3. **Precision contract (JVM).** Lexical `T` matches if `T` appears in **any**
   stored excerpt/summary for that Asset — not only the cosine-winning snippet.
   Anti-case: Asset with no `T` anywhere stored → drop.
4. **One device gate, then leave.** Run the **fixed** device checklist once on
   the mixed library. Pass/fail that list. **Do not** open a new I1 code slice
   for the next paraphrase the same week. Log extras → catalog class or P2.

**P0 device checklist (I1 + I5 + precision) — this is the whole I1 device bar**

| # | Query | Expect |
|---|---|---|
| 1 | `silky` | Spelling-list class; no Urdu/bus junk |
| 2 | `which file has silky in it?` | Same class as 1 |
| 3 | `Show me the files with swimming timetables` | Same class as `swimming timetable` |
| 4 | `show me the files` | Honest empty — no neighbor dump |
| 5 | `doc with silky in it` | Same class as 1 |
| 6 | `scan` | File whose **saved text** contains scan |
| 7 | `get me some egs from the pdf related to the training project` | Same as `e.g.s` — how people actually type it |
| 8 | `scan silky` | Only assets whose stored text has **both** (I2) |

**Explicitly not I1-exit:** I3 grouped lists, I4 hedge, I8 last week, typos,
quoted phrases, “all files”, CE polish, ADR-052. Those are **next phases**.

**After checklist 1–8:** I1 is **closed for this phase**. Next code is **MF-1.2
(I3)**. Further English paraphrases are **maintenance** (catalog PR), never a
reason to stay in MF-1.x.

### A. Query-structure intents (how the ask is shaped)

| ID | Intent | Example | Owner | Pass | Fail | Status |
|---|---|---|---|---|---|---|
| **I1** | One thing, many phrasings | `silky` · `which file has silky` · `show me files with silky` · `I need the silky spelling list` | Find | Generator + 8-row device checklist in **P0 phase exit** | Empty for wrappers while bare token works | **P0** — JVM generator green; close after checklist 1–8, then **stop MF-1.x** |
| **I2** | Same-file multi-attribute | `CV with Škoda` · `scan silky` | Find | Hits whose **same** evidence/asset supports **all** content cues | Split into two unrelated files; partial-token junk | **PARTIAL** (token AND exists; `with` not distinguished from list glue) |
| **I3** | Several things → several files | `passport and id` · `cv…, lamborghini… or beetle…` | Find | **Grouped** short piles per cue; not default AND-in-one-file | Empty because AND required one file to contain all; one undifferentiated mash | **OPEN** |
| **I4** | Hedge / or-family (same thing) | `silk or silky` | Find | Either spelling is enough for that one pile | Treat as two unrelated products; or require both | **OPEN** |
| **I5** | Weak / filler / no cue | `show me files` · `that important thing` | Find | Honest empty / ask to narrow | Soft top-N bluff | **PARTIAL** (U6 empty; filler-only still under-specified) |
| **I6** | Exact / keyword expectation | invoice number, exact ID string | Find (keyword path until one box) | Keyword Find still offered; meaning must not pretend exact | Silent keyword-as-meaning | **PARTIAL** (separate UIs; copy must stay honest) |

**List vs same-file (binding for I2 vs I3):**

- Commas, `or`, and list-shaped `and` between **distinct recall targets** → **I3** (grouped files).
- `with` / stacked attributes of **one** remembered original → **I2** (same-file AND).
- If both interpretations have hits: prefer **grouped I3** when list glue is present; do not make the user learn a query language.
- `or` as hedge of **one family** (`silk or silky`) → **I4**, not I3.

### B. Constraint and memory-cue intents (what they remember besides words)

| ID | Intent | Example | Owner | Pass | Fail | Status |
|---|---|---|---|---|---|---|
| **I7** | Calendar / titled constraint (absolute) | `notes in 2024` · `titled "March Invoice"` | Find (MIG-07B) | Explicit year/ISO/month-day and quoted title use TIME/TOPIC correctly; missing anchor **neutral** | Exclude files that lack TIME; substring-match nonsense | **PARTIAL** (stage exists; Why rarely cites the constraint) |
| **I8** | Relative-time Find | `yesterday` · `last week` · `last month` · `recent screenshots` | Find (**MIG-07B hardening**, not Grounded Answers) | Resolve to a **date window vs now**; compare to **parsed** TIME (EXIF); advisory unless clearly exclusive; Why can say the window | Store cue as the words `last week` and `contains()` against ISO dates; omit `yesterday`; pretend timeline prose | **OPEN** |
| **I9** | Browse / freshness | `recent screenshots` · `what's new` | Find | Honest short recency list **or** “not enough date signal”; no junk pad | Ten unrelated meaning neighbors | **OPEN** |
| **I10** | Type / corpus scope | `PDF only` · `screenshot of Wi-Fi` · `not screenshots` | Find | Honor type when clearly said; ignore when wrapper noise (`on my phone` today) | Filter on every noun; silent no-op forever | **OPEN** (negation: see I24) |
| **I11** | Place / person / object as **structured** filter | `photo with Mira` · `notes from school` | Find **after** those anchors are populated | Neutral until populated; lexical/meaning may still hit the **words** in evidence | Fabricate PERSON/PLACE anchors; claim structured filter | **DEFERRED** (MIG-03 out of scope; Vision / future MemoryBuilder) |
| **I12** | Homonym / sense | `beetle` car vs insect | Find | Short list + honest Why; no synonym expansion | Invent the other sense | **OPEN** (precision + Why; no WordNet) |

**I8 is Find.** “Photos from last week” is retrieval. “What did I do last week?” as
an essay is **G3** (Grounded Answers, `TIMELINE` reserved). Do not slip I8 into Ask.

**MIG-07B honesty:** advisory patterns already detect `last week` / `last month` /
`last year` / `recent` in `RecallQueryConstraintClassifier`. Matching is still
`anchorText.contains(cue)`. EXIF looks like dates, not the phrase `last week`.
`yesterday` / `today` / `this morning` are **not classified**. Closing I8 requires
**resolve → interval → parsed TIME**, not more regex-contains.

### C. Trust, repair, and honesty intents

| ID | Intent | Example | Owner | Pass | Fail | Status |
|---|---|---|---|---|---|---|
| **I13** | No solid match | unknown word in a large mixed library | Find | Honest empty (U6) | Pad-to-10 | **MF-1** |
| **I14** | Pack / engine missing | meaning pack absent | Find | Clear unavailable; keyword still offered (U7) | Crash or fake meaning | **MF-1** |
| **I15** | Index incomplete / not yet searchable | new PDF still assembling | Find | Corpus honesty; “not indexed yet” ≠ “no match” when we can tell | Pretend complete (U8) | **PARTIAL** (FC-04 corpus UI; per-query distinction OPEN) |
| **I16** | Open failed / moved / revoked | Find hit, Open fails | Find | Search still valid; clear cannot-open | Hide failure; delete the Memory silently | **OPEN** (product copy) |
| **I17** | Why / trust | open Why on any hit | Find | Consumer dialect (U5); if grouped (I3), Why names **which cue** matched | Hash wall; “matched by meaning” with no cue | **PARTIAL** (MF-2 copy; grouped Why OPEN) |
| **I18** | Refine after wrong hit | “not that one — the other silky” | Find | Clear new query state; no ghost Why | Session bleed | **OPEN** |
| **I19** | Identify / disambiguate | several near-duplicate spelling lists | Find | Short list, distinct labels, Why contrast | One random file claimed as the only hit | **OPEN** |
| **I20** | Capability / meta | “can you search WhatsApp?” | Find | Honest product limit — **not** a search result list | Empty meaning results as if they asked for a file | **OPEN** |
| **I21** | Answer-seeking misroute | “what’s my passport number?” · “what does this PDF say?” | Find (handoff later) | Do **not** invent; point to Find cards or future Ask; never unsupported answer | Generative bluff on Find | **OPEN** (Find honesty); GA still blocked |
| **I24** | Negation / except | `not the Urdu one` · `except bus rules` | Find | v1: **do not pretend** to exclude; later explicit exclude with tests | Silently drop or silently ignore while claiming smart | **OPEN** (v1 = honest non-support) |

### D. Input / corpus / system intents

| ID | Intent | Example | Owner | Pass | Fail | Status |
|---|---|---|---|---|---|---|
| **I22** | Typos / near-miss | `silkky` · `passprot` | Find | v1: honest empty or exact-token miss; optional later light tolerance **without** inventing files | Fuzzy match to unrelated PDFs | **OUT** for v1 code; **registered** so we do not “never thought of it” |
| **I23** | Long / story query | 40-word paragraph | Find | Cap + extract content cues; do not embed the novel as one blob without a plan | Dilute to junk neighbors | **PARTIAL** (120-char cap; cue extract still thin) |
| **I25** | Messy input | ALL CAPS, emoji, `???` | Find | Normalize; same I1 class when tokens remain | Crash / weird tokens | **PARTIAL** (normalize exists) |
| **I26** | Mixed asset types in one ask | PDF + screenshot + note | Find | One Canonical Recall list; labels show type | Per-asset secret rankers | **PARTIAL** (architecture); UX labels OPEN |
| **I27** | Duplicates / versions | same PDF twice | Find | One card per Asset identity | Duplicate walls | **PARTIAL** (verify on device) |
| **I28** | Wrong OCR / garbage excerpt | false “contains word” | Find | Prefer silence when evidence is garbage if we can tell; never over-claim OCR | Trust OCR as ground truth | **OPEN** |
| **I29** | Huge library / latency | 10k memories | Find | Short trusted list; no pad; stay within measured budgets | Top-10 always | **PARTIAL** (trusted cap; 10k unmeasured) |
| **I30** | CE on vs off | pack present / IDENTITY_ONLY | Find | **Same precision rules**; CE reorders only | Precision changes when CE missing | **MF-1** / T6 |
| **I31** | Concurrent index + search | search mid-build | Find | Stable empty or partial honesty | Crash / torn results | **OPEN** |
| **I32** | Privacy-sensitive cue | medical, passwords in the query | Find | On-device; no cloud; careful empty; no durable prompt logs | Upload query; cute empty copy that leaks | **PARTIAL** (local-first; copy not reviewed) |
| **I33** | Non-English cue | Urdu query for Urdu PDF | Find | English-first in this bar; do not silent-fail **forever** without a later ADR | Claim multilingual AVAILABLE | **DEFERRED** (cross-lingual ADR) |
| **I34** | Desktop / “on my computer” | before desktop corpus exists | Find | Wrapper noise today; real source later | Treat as a Place filter now | **DEFERRED** |

### E. Registered P2 holes (named, not v1 code)

These were missing from chat-driven A–E. They **exist on the register** so a later
slice can claim them. They do **not** expand MF-1.1.

| ID | Intent | Example | Owner | v1 Find |
|---|---|---|---|---|
| **I35** | Quoted exact phrase | `"swimming timetable"` | Find | Later; unquoted tokens remain I1 |
| **I36** | Completeness / “all” | `all invoices` · `every spelling list` | Find | Must not invent a count; short list + optional “there may be more” later |
| **I37** | Superlative / recency of **use** | `the latest CV` · `the one I opened yesterday` | Find | Capture TIME ≠ opened-at; needs history |
| **I38** | Named days / festivals / ranges | `on Monday` · `before Eid` · `March to May` | Find (extends I8) | Not MF-TIME v1 (`yesterday` / `last week` first) |
| **I39** | Page / length | `page 12` · `the 3-page one` | Find | Later |
| **I40** | Existence | `do I have a passport scan` | Find / Why | Cards or honest empty; no fake yes/no chip |
| **I41** | Anaphora / more-like | `that one` · `similar to this` | Find | Needs session or an example asset |
| **I42** | Folder / album cue | `in Documents` · `Camera` | Find | Later |
| **I43** | Visual-only | `the red car photo` with no OCR | Find after vision | Do not fabricate OBJECT |
| **I44** | Mixed languages in one query | English + Urdu in the same ask | Find | I33 is single-language cue; mixed is later ADR |
| **I45** | Count | `how many PDFs about school` | Grounded Answers-ish | Find must not invent a number |
| **I46** | Permission lost mid-search | revoke while querying | Find | Stable failure; related to I16 |

### F. Not Find (must stay registered so Find does not steal them)

| ID | Intent | Example | Owner | Find must | Status |
|---|---|---|---|---|---|
| **G1** | Extract / look up | “what’s the Wi-Fi password on that page?” | Grounded Answers (`EXTRACT` reserved) | Show the file if recall works; **do not** print the password as an “answer” | Blocked |
| **G2** | Summarize / compare | “summarize this PDF” · “compare two CVs” | Grounded Answers | Cards only | Blocked |
| **G3** | Timeline / “what happened last week?” | prose about a week | Grounded Answers (`TIMELINE` reserved) | May still do **I8** file retrieval if they asked for files | Blocked |
| **G4** | Verify yes/no | “is silky in the spelling list?” | Borderline: Why on Find vs early GA | Honest Why citing the line; no fake yes/no chip | OPEN |
| **ACT1** | Do | open / share / remind | Act | Open original is already Find-adjacent; not a new search path | Deferred |

---

## User scenarios carried from v1 (precision core)

These remain binding. They are **I1 / I2 / I13–I17 / I30** fixtures, not the whole product.

| ID | User does | Pass | Fail (anti-case) |
|---|---|---|---|
| U1 | Types `silky` | Strong hits **contain** `silky` in saved text; list short | Urdu / bus / schedule PDFs with no `silky` |
| U2 | Asks `which file has silky in it?` | Same class of strong hit as U1 — **not empty** | Empty while bare `silky` works |
| U3 | Types two content words `scan silky` | Only evidence with **both** (I2) | Partial token-only hits |
| U4 | Mixed EN + Urdu library | Latin cue does not surface script-only noise without token match | Top-10 multilingual junk |
| U5 | Opens Why | ≤ few short lines; clear “why this file”; no hash wall; no cue-best jargon | Duplicate excerpt + engineering Open essay |
| U6 | No solid match | Honest empty / weak message | Ten irrelevant “closest” cards |
| U7 | Meaning pack missing | Clear unavailable; keyword Find still offered | Crash or fake meaning |
| U8 | Index incomplete | Corpus honesty; still no junk padding | Pretend complete |

### Golden wrappers (I1) — must not regress

Treat as **one cue** after stripping. Empty while the bare content token works is a fail.

Ask-shape is a **catalog of classes** in `RecallQueryContentTokens` (ask verbs,
type nouns like `pdf`/`doc`, discourse like `related`/`from`, abbreviations
like `e.g.`/`egs`, spoken quantities like `two files`) — not a growing list of
founder screenshots. Domain words stay content (`scan`, `silky`, `training`).

| Keep as wrappers (not content) | Keep as content |
|---|---|
| Ask verbs (`show`, `get`, `find`, `need`, …) | Domain words: `silky`, `timetable`, `passport`, `scan`, `training` |
| Type nouns used as ask shape (`file`, `doc`, `pdf`, `photo`, `screenshot`) | Type as a **filter** is I10 later |
| Discourse (`related`, `from`, `about`, `e.g.` / `examples`) | Do **not** synonym `silky` → `smooth` |
| `has` / `with` as glue in I1 questions | `with` as **I2** when it joins two content attributes of one original |
| Light plural `timetables` ≈ `timetable` | |

**MF-1.1 unit:** `Show me the files with swimming timetables` reduces to content
tokens `swimming` + `timetables` and matches evidence containing `timetable`.
Device MF-3 still required.

### Golden list / same-file examples (I2 vs I3 vs I4)

| Query | Class | Output shape |
|---|---|---|
| `scan silky` | I2 | One pile; both tokens in the same evidence |
| `cv with skoda` | I2 | Same document |
| `passport and id` | I3 | Group: passport files; id files |
| `lamborghini, beetle or skoda` | I3 | Three groups (or two if beetle/skoda are clearly cars-in-CV — still groups, not one AND) |
| `silk or silky` | I4 | One pile; either token |

---

## Technical scenarios

| ID | Condition | Pass |
|---|---|---|
| T1 | Content tokens ≥ 1 | Lexical contain applies (unless I8/I7 explicit time-only browse is designed otherwise **with tests**) |
| T2 | NL stopwords only / no content tokens | **No product hits** (I5); do not embed neighbors. Pack missing still EngineUnavailable |
| T3 | Embed input | Uses **content cue** when tokens exist; else normalized raw |
| T4 | Query normalize | Trim, collapse whitespace, length cap (align keyword ~120) |
| T5 | Token boost | Same stopword / wrapper catalog as cue extraction |
| T6 | CE present / absent | Reorder only on **already precise** pool; identity fallback safe (I30) |
| T7 | Card label | Friendly filename (strip storage hash prefix) |
| T8 | Why dialect | Shared contract; consumer-grade; grouped Why names the cue (I17) |
| T9 | Relative TIME | Classifier + **calendar resolve** + parsed TIME compare; missing TIME never excludes (Freeze §3) |
| T10 | Explicit TIME + content tokens | Must **not** skip lexical on `notes` merely because `in 2024` was explicit — content and constraint compose |
| T11 | Topic advisory | Must **not** treat every query ≥ 4 chars as TOPIC advisory with the **full raw question** as title cue |
| T12 | Multi-cue split | Deterministic, tested; no general NLU cloud; English list glue only |
| T13 | Unicode / injection | Normalize without crash |
| T14 | IDENTITY_ONLY / low RAM | Same precision contract as T6 |

---

## Why consumer dialect (v1 + constraints)

Why must answer in plain language:

1. What you asked (normalized display).  
2. Which file (friendly name).  
3. The saved line that justifies it (short; highlight cue when possible).  
4. How it was found (“by meaning” / “your word appears in the saved text”).  
5. **When I7/I8 applied:** the time or title constraint in words (“taken last week”,
   “from 2024”) — never fake a date that is not on the Memory.

**Not in Why:** storage hashes, “cue-best”, “page 1 fallback”, score jargon,
duplicate full card excerpt when the card already shows it, “matched by meaning”
with no cue word.

---

## Acceptance examples (founder library)

From 2026-09-04 SM-A156E screenshots (still binding):

- **Must keep:** `Spelling list 4 (1)-1.pdf` for `silky` (snippet contains silky).
- **Must drop:** Urdu weekly vocab / mock papers / bus rules / PTM schedule /
  worksheets **without** `silky` when cue reduces to content token `silky`.
- **Must work:** `which file has silky in it?` returns the spelling-list class hit.
- **Must work (I1 / MF-1.1):** `Show me the files with swimming timetables` same
  class as `which file has swimming timetable` (wrappers + light English plural).

Relative-time fixtures (I8) need **dated EXIF / TIME anchors** plus a frozen
“now” in tests — do not claim I8 on undated PDFs.

---

## Out of scope vs deferred (registered, not forgotten)

| Item | Bucket |
|---|---|
| One combined Find UI (hybrid) | Later; keep keyword capability (I6) |
| ADR-052 pack onboarding | After I1+I17 meet bar on device; not a substitute for intent coverage |
| Stage B evidence-native ranker | FC-03; does not close I3/I8 |
| Cross-lingual without shared tokens | Future ADR (I33) |
| Synonym / embedding expansion nets | Forbidden without ADR |
| PERSON/PLACE/OBJECT structured filter | After those anchors exist (I11) |
| Grounded Answers / Act | Separate constitutions (G*, ACT1) |
| Typo tolerance | Registered I22; not first code |
| Full negation NLU | I24 v1 = honest non-support |

---

## Delivery slices (code only after mapping to IDs)

Do **not** open ADR-052 UI until I1 (including golden wrappers) + I17 meet this
bar on device. Do **not** start Grounded Answers to paper over I8 or I21.

| Slice | Delivers | Bar IDs | Gate |
|---|---|---|---|
| **MF-1** | Cue object; lexical ≥1; boost stopwords; trusted short list | U1–U4, U6, T1–T6, I13, I30 | Landed JVM; device MF-3 still open |
| **MF-2** | Why + card + Open copy | U5, T7–T8, I17 (ungrouped) | Landed copy; grouped Why later |
| **MF-1.1.x** | Ask-shape catalog + whole-Memory lexical + P0 generator test | I1, I5, T2, T5 | **JVM done.** Device = 8-row checklist then **stop** |
| **MF-1.2** | Multi-cue split + grouped results + grouped Why | I3, I4, I17 grouped, T12 | **Next after checklist** — do not wait for more paraphrases |
| **MF-1.3** | `with` / list-glue disambiguation | I2 vs I3 | With or immediately after MF-1.2 |
| **MF-TIME** | Relative + compose with content (resolve → interval → parsed TIME); fix T10/T11 | I7 remainder, I8, T9–T11 | Find hardening; **not** Ask |
| **MF-HONEST** | Misroute, meta, not-indexed-yet, open-failed copy | I15, I16, I20, I21, I24 v1 | Copy + routing; no generation |
| **MF-3** | Device verify on founder mixed library | All in-scope I* marked for that build | Required before AVAILABLE discussion |

Each code change control must list **I-*** IDs**. Architectural DONE language
must not be used for I8 or I3.

---

## Explicit non-claims

Not marketing AVAILABLE. Not Grounded Answers. Not a second Find path.
Not “understands anything you say.” Not Product Contract person/place/object
filter until those anchors exist. Not relative-time product-ready until MF-TIME
passes dated fixtures.
