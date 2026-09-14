# Human Recall Ask Model (v1.1)

**Status:** Binding product model for **how people ask**, **what they ask for**,
and **what the answer must survive**  
**Date:** 2026-09-04 (v1 model; v1.1 amendment — accept together)  
**Process:** `docs/ENGINEERING_CHARTER.md` holistic scenario planning  
**Does not authorize:** marketing AVAILABLE, new Find paths, Grounded Answers runtime,
Act, VisionEngine, fabricated PERSON/PLACE anchors, synonym nets.

**Authority relative to other Find docs**

| Document | Role |
|---|---|
| **This file** | Ceiling: natural recall language, jobs, policies, substrate honesty, Ask impact |
| `MEANING_FIND_PRODUCT_SCENARIO_BAR.md` | Execution IDs (I1–I46), P0/P1/P2, device checklists **must map here** |
| Product Contract retrieval | Person, place, object, time, purpose, topic — **promised**; this file says **when** each is honest |
| Local AI Spec §9 | Pipeline: cue → candidates → structured filters → rank → cards → Why |
| Grounding Architecture | Answers **after** retrieval; Find remains the rail |

**Process rule (non-negotiable):** A device phrasing that has **no class in this model** is a **model defect**. It is **not** permission to open an ad-hoc code slice. Add or refine a **class** here first; then one change control mapped to that class. Building must not be how we discover the product.

English is the language of this model. Mixed-script and non-English cues are D13 (deferred ADR), not “unthought.”

---

## 1. North star

A person remembers **an episode or an artefact** the way humans remember — fragments of who, what, where, when, how it looked, a word they saw, a kind of file, a mistake in typing — and UNFYND returns **the right originals** with a **plain Why**, or **honest silence**.

It must not feel like a query language. It must not invent people, dates, or captions.

**Wow:** Feels like a careful friend who actually had access to what is on the device.  
**Anti-wow:** Top-N vectors; “understood” a photo of a child that we cannot see; an essay instead of the file.

---

## 2. Surfaces (never mix constitutions)

| Surface | User wants | Returns | Must not |
|---|---|---|---|
| **Find** | The original(s) | Memory cards + Why | Invent facts, names, dates, captions |
| **Keyword Find** (until one box) | Exact string | Evidence hits | Pretend to be meaning |
| **Grounded Answers** | A claim from evidence | StructuredAnswer + citations | Run until Find can retrieve the right Memories; first GA slice is PDF text until a later spec |
| **Act** | Do something | Open / share sheet / open in another app, all stored URI (ADR-053, ADR-054); remind later | Pretend search “did” it |

“Show me the photo…” is **Find**. “What did we do last Saturday?” as prose is **Ask**. Find may still **retrieve files** for that window (time-bound Find) without writing the essay.

**Open decision — one box (v1.1).** Today the person must choose between “Find by
saved text” and “Find by meaning” **before** typing. That is a query language
wearing a UI costume: it asks the user to know which retrieval strategy will work,
which is exactly what §1 forbids. This model does not decide the merge — it
**registers** it as a decision that must be made, with an ADR, at **W1.5**. Until
then keyword Find stays the honest rail for J6 and must never be presented as
meaning. The decision must answer: one box with two candidate generators, or two
boxes with copy that makes the choice obvious? Bar row **I6** maps here.

**Ceiling rule for answers:** Retrieval quality is the ceiling. If Find cannot get the photo, Ask must **abstain**, not describe a daughter at a pool.

---

## 3. Cue dimensions (what they remember)

Every natural ask is a bundle of **dimensions**. Implementation maps each dimension to **stored evidence or an honest gap**. Missing a dimension in this table is how we “mess again.”

| ID | Dimension | How people say it | What must exist on Memory | Honest if missing | Layer |
|---|---|---|---|---|---|
| **D1** | Text in the artefact | silky, scan, timetable, password on a screenshot | OCR / PDF / note text | Empty or keyword | Find now |
| **D2** | Filename / title | “March invoice”, “that PDF called…” | display label, TOPIC | Weak / lexical on name | Find partial |
| **D3** | Kind of original | photo, screenshot, PDF, note, Camera | `AssetType`; later album/folder | Today often **padding**; must become **filter** (I10) | Find P1 |
| **D4** | Clock time | 2024, 12 March, yesterday, last week, **last Saturday**, this morning | Parsed TIME (EXIF, etc.) | Neutral if no TIME; never fake a date | Find P1 (I7/I8/I38) |
| **D5** | Use time | “the one I opened yesterday” | view/open history | Cannot claim | P2 (I37) |
| **D6** | Who (name) | Mira, Ahmed | PERSON or text of the name | Text hit only if the **name is stored** | Find text; structured **deferred** |
| **D7** | Who (kinship / role) | my daughter, the kids, Mum | PERSON + relationship; **not** OCR of “daughter” | **Cannot** mean “the child in the picture” | Vision + model; **deferred** |
| **D8** | Where | pool, Dubai, school, “at home” | PLACE / GPS / text | Text only if the **word** is stored | Structured **deferred** |
| **D9** | Activity / event | swimming, birthday, PTM | ACTIVITY or text | Text if written | Deferred structured |
| **D10** | Object | passport, beetle (car) | OBJECT or text | Text / homonym risk (I12) | Find text; structured deferred |
| **D11** | How it looked | “the photo”, in the water, red swimsuit | **Visual** evidence | A pool photo may have **zero** useful OCR | Vision **deferred** |
| **D12** | Purpose / topic | “for school”, “the invoice one” | PURPOSE / TOPIC | Title/topic partial | Partial |
| **D13** | Language | Urdu cue, mixed EN+Urdu | Policy + models | English-first now | Deferred ADR |
| **D14** | Source / channel | WhatsApp, Camera, Documents | not a third-party inbox | Honest limit (I20) | Meta honesty |
| **D15** | Episode vs word | “that day at the pool” vs “the word silky” | episode needs D4–D11; word needs D1 | Do not treat both as bag-of-tokens | Model must distinguish |
| **D16** | Device / corpus scope | “on my phone”, “on my computer”, “here”, “in Drive” | the corpus actually indexed **on this device** | Today **wrapper noise**: strip it. Never a content token; never a PLACE filter | Find now (strip) / P2 (real selector) |

**On D16 (v1.1).** People name the machine they think they are searching, and they
get it wrong — someone says “computer” while holding the phone. Two rules follow.
**Never** treat `phone` / `laptop` / `computer` as a word to find inside a file.
**Never** silently answer for a corpus we did not search: once a second corpus
exists (desktop, Drive), a scope word the user says and a scope we cannot search
must produce an honest limit, not an empty list. Bar rows **I10** (scope vs
padding) and **I34** (desktop) map here.

**Product Contract** lists person, place, object, time, purpose, topic. This table is when each is **honest**. MIG-03 populated **TIME** (EXIF) and **TOPIC** (titles) only. PERSON/PLACE/OBJECT/ACTIVITY/PURPOSE are **not fabricated**.

---

## 4. Ask shape (how they wrap it)

Shape is **orthogonal** to dimensions. Classes, not anecdotes.

| Class | Includes (illustrative, not exhaustive) | Policy |
|---|---|---|
| **S1 Imperative / request** | show, get, give, find, need, want, pull up, grab | Strip; not content |
| **S2 Interrogative** | which, what, where, who, how | Strip unless the rest is empty |
| **S3 Type-as-padding** | file(s), doc(s), pdf(s), photo(s), screenshot(s) when not a filter | Strip in I1; **I10** when clearly a constraint |
| **S4 Discourse / glue** | from, related, regarding, about, with (glue vs I2), in it | Strip when glue |
| **S5 Quantity of results** | two, three, a few, both, some examples / egs / e.g. | **Not** a word in the file |
| **S6 Abbreviation / informal** | egs, eg, pls, wanna | Map into S1–S5; not domain synonyms |
| **S7 Inflection** | timetable / timetables | Light English plural only; **not** silk→silky |
| **S8 Noise / filler** | please, just, really, emoji, ??? | Strip; if nothing left → J2 empty |
| **S9 Story length** | 40-word paragraph | Cap; extract dimensions; do not embed the novel (I23) |
| **S10 Orthography** | ALL CAPS, punctuation | Normalize |
| **S11 Typo / near-miss** | staurday, silkky, passprot | **P-TYPO** (below) — not silent fuzzy junk |
| **S12 Quote** | `"swimming timetable"` | Exact vs loose (I35) later |
| **S13 List glue** | commas, list-and, list-or | **J3** several retrieves |
| **S14 Hedge** | silk or silky | **J4** one family |
| **S15 Same-original “with”** | CV with Škoda | **J2** AND in one asset |
| **S16 Negation** | not, except | **P-NEG** — do not pretend in v1 |
| **S17 Deixis / refine** | that one, the other silky | Session (I18) later |
| **S18 Completeness** | all, every | Do not invent a count (I36) |

New informal English is **classified into S\*** or a new **S-class in this document**, then implemented. It is not a surprise code path.

---

## 5. Jobs (what they want back)

| ID | Job | Example | Surface | v1 Find |
|---|---|---|---|---|
| **J1** | One artefact, many phrasings | which file has silky | Find | In progress (shape + lexical) |
| **J2** | Same artefact, several attributes | scan silky; CV with Škoda | Find AND | Partial |
| **J3** | Several artefacts in one breath | passport and id | Find **groups** | **Not built** |
| **J4** | One thing, two spellings | silk or silky | Find OR-family | **Not built** |
| **J5** | No cue | show me the files | Honest empty | Landed (skip embed) |
| **J6** | Exact string | invoice 4412 | Keyword until one box | Separate UI |
| **J7** | Type-filtered retrieve | photos only; screenshot of Wi-Fi | Find + D3 | Open |
| **J8** | Time-filtered retrieve | files from last Saturday | Find + D4 | Not resolved |
| **J9** | Visual retrieve | photo of a pool (no text) | Find + D11 | **Blocked** on vision |
| **J10** | Person retrieve | photo of my daughter | Find + D6/D7 | **Blocked** on PERSON/vision |
| **J11** | Browse / recent | what’s new | Find + D4/D5 | Open |
| **J12** | Existence | do I have a passport scan | Cards or empty; no fake yes | Open |
| **J13** | Extract / lookup | what’s the Wi-Fi password | **Ask** | Find may show the screenshot |
| **J14** | Summarize / compare | summarize this PDF | **Ask** | Cards only |
| **J15** | Timeline prose | what did we do last Saturday | **Ask TIMELINE** | Find may still do J8 |
| **J16** | Capability | can you search WhatsApp | Honest meta | Open |
| **J17** | Act | open it, share | Act | Open original + share sheet (ADR-053) + open the whole file in another app (ADR-054); remind out |

---

## 5b. Result and system classes (R1–R8) — v1.1

Sections 3–5 model the **ask**. They do not model the **answer surviving contact
with a real device**. Eight conditions in `MEANING_FIND_PRODUCT_SCENARIO_BAR.md`
mapped to nothing in v1, which meant no wave could ever own them. Under this
model’s own process rule, that was a **model defect**. It is closed here.

An R-class is **not** a job: the user did not ask for it. It is a condition the
result must handle without lying. Section numbering is kept as **5b** so existing
references to §6, §7 and §10 in other documents stay valid.

| ID | Condition | What the person sees | We must | Anti-case (lie) | Bar |
|---|---|---|---|---|---|
| **R1** | Find worked, **Open** failed — moved, deleted, or access revoked | Right card, dead tap | Say the original cannot be opened **and** keep the Memory and the Why intact | Blame the search; silently delete the Memory; a generic crash toast | I16 |
| **R2** | Permission lost **mid-search** | Results half-arrive | Stable, explained failure; offer to re-grant | Empty list as if nothing matched | I46 |
| **R3** | Several **near-duplicate** originals match | Five almost-identical files | Show them as distinct, with what differs; a short list, not a pick | Return one at random as “the” answer | I19 |
| **R4** | The **same** original indexed twice | Duplicate cards | One card per Asset identity | A wall of the same file | I27 |
| **R5** | Stored evidence is **garbage** — bad OCR that happens to contain the token | A “match” that is noise | Prefer silence when we can tell the excerpt is unreadable; never present garbage as a citation | Treat OCR as ground truth because it is stored | I28 |
| **R6** | **Large library** — 10k+ Memories | Waiting | Stay inside a measured budget; short trusted list; never pad | Full-index scan per query with no budget; top-N to look busy | I29 |
| **R7** | Search **while indexing** | Partial corpus | Distinguish “not indexed yet” from “no match” whenever we can tell, per **P-INCOMPLETE INDEX** | Present a partial corpus as complete | I31, I15 |
| **R8** | **Sensitive cue** — medical, passwords, legal | A query that is itself private | On-device; no cloud; no durable prompt log; empty copy that does not echo the cue back into UI history | Log the query; cute empty copy that leaks what was asked | I32 |

**Done rule (binds every wave from W1 on):** a wave that returns results owns the
R-classes those results can hit. R1–R5 apply to any wave that returns a card.
R6–R8 apply to every wave. “We shipped J3” is not done if `passport and id`
returns four copies of the same passport with an unopenable original.

---

## 6. Policies (so we do not invent)

### P-TYPO (D/S11) — spelling and near-miss

| Option | When | Risk |
|---|---|---|
| **A. Exact (current)** | Token must occur in stored text (plus light plural) | `silkky` / `staurday` look like “Find is broken” |
| **B. Bounded repair** | Edit distance 1 on **content** tokens only; still must match **stored** text; never invent a file | `silkky`→`silky` OK; `scan`↛ random PDF |
| **C. Suggest, don’t retrieve** | “Did you mean silky?” no results until confirm | Extra UX |

**v1 freeze until a dedicated slice:** **A**, with **honest empty** (not junk neighbors).  
**Next typo slice (after J3 or with I8 — product choice):** **B** with anti-cases: misspelling must not surface Urdu/bus PDFs.  
**Forbidden:** unconstrained fuzzy / synonym nets (`silky` → `smooth`).

### P-NEG (S16)

v1: do **not** claim exclusion. `not` is not a smart filter. Later: explicit exclude with tests.

### P-TIME (D4)

- Absolute year / ISO / month-day: structured TIME when present; **missing TIME never excludes** (Freeze).  
- Relative (`yesterday`, last week, **last Saturday**, last weekend): **resolve vs “now”** to an interval; compare **parsed** TIME. String-contains `"last week"` is **not** the product.  
- Typo `staurday`: P-TYPO then P-TIME.

### P-TYPE (D3)

- Padding: “from the pdf” with a content cue → not a type filter.  
- Constraint: “photos of…” / “PDF only” → filter `AssetType` when we can tell.  
- Ambiguity: prefer **retrieve with type as advisory** over empty, until I10 tests exist — **except** when the user clearly scoped (“photos only”).

### P-AND vs P-LIST (J2 vs J3)

- List glue between **distinct targets** → **J3 groups**.  
- `with` / stacked attributes of **one** original → **J2**.  
- If both have hits and list glue is present → **J3**.  
- User must not learn AND.

### P-VISUAL (D11)

Do not treat embedding of **empty OCR** as “understood the photo.” If there is no visual evidence, **abstain** on visual claims. Why must not say we recognized a daughter or a pool.

### P-ANSWER

Find never answers. Ask never silently replaces Find. Same cue parse should feed both later.

### P-EVIDENCE

Lexical/precision is **any stored excerpt for that Asset**, not only the cosine-winning snippet. Filename may assist; it is not enough alone (Product Contract).

### P-INCOMPLETE INDEX

“No match” ≠ “not indexed yet” when we can tell (I15).

---

## 7. Binding fixture: photo + kinship + place + weekday + typo

**Canonical asks (same job, noise variants):**

1. `show me the photo of my daughter in swimming pool last saturday`  
2. `show me the photo of my daughter in swimming pool last staurday`  
3. `photo of my daughter at the pool last Saturday`

**This is Find (J1 + J7 + J8 + J9 + J10), not Ask.**

### Layered honesty (must stay in Why / empty copy as we gain layers)

| Layer | Needed | Pass when that layer exists | Fail / lie |
|---|---|---|---|
| Shape | S1–S5 | Query not empty-for-wrong-reason | Require `show`/`photo` as file text |
| Type | D3 / J7 | Prefer photos if we filter | Only PDFs that mention swimming |
| Text tokens | D1 | `swimming`/`pool` in **stored text** | Timetable PDF as if it were the photo |
| Time | D4 / J8 | EXIF in last Saturday’s window | Files with the word Saturday; fake dates |
| Visual | D11 / J9 | Model evidence of pool/swim scene | Caption invented from filename |
| Kinship | D7 / J10 | Linked PERSON “daughter” or equivalent | Any child photo; any “daughter” OCR hit from a form |
| Typo | P-TYPO | `staurday` → Saturday **or** honest miss | Junk list |

**Today (2026-09-04), honest product:** this query is **not** satisfiable as the user means it. Best accidental behaviour is **wrong** (text “swimming”). Required copy when we detect D7+D11 without substrate: **we can search words and dates we have; we cannot yet recognise people or scenes in photos.** Never a fake “here she is.”

**When Ask exists:** only after Find can retrieve that Memory class. First GA slice is **PDF text**; a **photo** answer needs a later acceptance spec. Abstain if retrieval incomplete.

---

## 8. Other fixtures (cover the model, not one library)

Text Find (can be true on today’s substrate if indexed):

- One word; question form; padded request; filler empty; quantity + content; abbreviation class; two tokens same file; **several files in one ask** (J3).

Must not pretend:

- Kinship without vision/PERSON  
- Visual scene without vision  
- last Saturday without date resolve  
- Typo without P-TYPO slice  
- WhatsApp  
- Invented passport numbers  

All four asset types: **same jobs and dimensions**; evidence quality differs (PDF text vs OCR vs note vs empty OCR photo).

---

## 9. Impact on Grounded Answers (do not paint into a corner)

| Ask-shaped user sentence | Find must have done | Ask may then |
|---|---|---|
| Show me the photo… | J9+J10+J8 retrieve | Optional caption **only** from packaged evidence |
| What’s the Wi-Fi on that screenshot? | Retrieve screenshot | EXTRACT from OCR |
| What did we do last Saturday? | J8 file set **or** abstain incomplete | TIMELINE over package; never invent |
| Compare two CVs | Two Memories retrieved | COMPARE reserved |

If Find remains bag-of-tokens, Ask will either **hallucinate** or **always abstain**. Building J3 (lists) does **not** unlock the daughter/pool query. Building CE does **not** unlock kinship.

**Shared parse:** Future one box routes J1–J12 to Find and J13–J15 to Ask using **this model’s jobs**, not a second NLU stack.

---

## 10. Build order (dependency, not the last test)

Do **not** discover the next class in implementation. Execute **waves**:

| Wave | Delivers | Unlocks | Does not unlock |
|---|---|---|---|
| **W0** | This model accepted | Honest sequencing | Any new behaviour |
| **W1** | J3 groups + J4 hedge + Why-which-cue; shape catalog **maintenance only**; R1–R5 for returned cards | Multi-document asks on **text** Memories | Daughter/pool photo |
| **W1.5** | **One-box ADR** (§2) — decide whether keyword stays a separate screen | A single ask surface; J6 stops being a place the user must find | Any new retrieval capability |
| **W2** | J7 type filter; P-TIME including **weekdays**; Why cites time | last Saturday **photos with EXIF** | Kinship, scenes |
| **W3** | P-TYPO B with anti-cases | staurday / silkky without junk | Vision |
| **W4** | Vision + PERSON/PLACE (ADR + MemoryBuilder) | Fixture §7 as **intended** | Act |
| **W5** | Grounded Answers after retrieval quality | J13–J15 | — |

ADR-052 pack UI is **launch**, not recall-language. Stage B CE is **ranking**, not dimensions D7/D11.

**Keyword Find** stays until one box; it is the rail for J6.

---

## 11. What “done” means for a wave

- Every job/dimension the wave claims has pass/fail + anti-case **in this file**.  
- Every anti-case the wave claims is a **test**, not a sentence. A registered
  anti-case with no test is a wish (v1.1).  
- The **R-classes** (§5b) that the wave’s results can hit are handled.  
- Tests encode the **class**, not one founder sentence.  
- Device proves the **wave checklist** on mixed types **when those types are indexed**.  
- A new phrasing → classify → if in-wave, it was already in the generator; if not, **amend this model** then code.

---

## 12. Explicit non-claims

Not AVAILABLE. Not “understands photos of family.” Not multilingual AVAILABLE. Not Grounded Answers. Not a second Find path. Not that I3 equals natural recall complete.

---

## Document history

| Date | Change |
|---|---|
| 2026-09-04 | **v1.1 amendment** (accept with v1): D16 device/corpus scope; §5b result and system classes R1–R8; one-box decision registered at §2 and wave W1.5; anti-case-as-test in §11. Source: `PROGRAM_STATE_AND_SEQUENCE_V1.md` §3 — eight scenario-bar rows mapped to no class, which the model’s own process rule calls a model defect |
| 2026-09-04 | v1: dimensions, shape classes, jobs, policies, §7 fixture, Ask impact, waves W0–W5 |
