# Meaning Find — Product & Scenario Bar (v1)

**Status:** Binding acceptance bar for Find by meaning work  
**Date:** 2026-09-04  
**Process:** `docs/ENGINEERING_CHARTER.md` holistic scenario planning  
**Architecture:** Canonical Recall only (Live/Dual N = 0). No second Find path.  
**Does not authorize:** marketing AVAILABLE, ADR-052 UI, Grounded Answers, Act.

## North star (user value)

A person asks the way they remember — a word, a phrase, or a short question —
and gets a **short, trustworthy list** of the right originals, with a **plain Why**
they can understand in seconds. Prefer silence or “no solid match” over bluffing
with ten weak neighbors.

**Wow bar:** Feels like a careful friend who read your files — not “top-10 cosine
demo,” not a chatbot essay.

## Innovative stance (within architecture)

| Commodity default | UNFYND choice |
|---|---|
| Embed raw NL; show top 10 vectors | **Recall cue object**: normalize → content tokens → embed content cue; precision gate; short trusted list |
| Separate “smart search” product | Same **Canonical Recall**; keyword remains exact evidence path (later: one box, two generators) |
| Engineer Why / hash titles | **Consumer Why dialect**; friendly labels; no duplicate walls |
| Soft floor only | Lexical contain for **≥1** content token + relative score band |

## User scenarios (must plan + test)

| ID | User does | Pass | Fail (anti-case) |
|---|---|---|---|
| U1 | Types `silky` | Strong hits **contain** `silky` in saved text; list short | Urdu / bus / schedule PDFs with no `silky` |
| U2 | Asks `which file has silky in it?` | Same class of strong hit as U1 — **not empty** | Empty while bare `silky` works |
| U3 | Types two content words `scan silky` | Only evidence with **both** | Partial token-only hits |
| U4 | Mixed EN + Urdu library | Latin cue does not surface script-only noise without token match | Top-10 multilingual junk |
| U5 | Opens Why | ≤ few short lines; clear “why this file”; no hash wall; no cue-best jargon | Duplicate excerpt + engineering Open essay |
| U6 | No solid match | Honest empty / weak message | Ten irrelevant “closest” cards |
| U7 | Meaning pack missing | Clear unavailable; keyword Find still offered | Crash or fake meaning |
| U8 | Index incomplete | Corpus honesty; still no junk padding | Pretend complete |

## Technical scenarios (must plan + test)

| ID | Condition | Pass |
|---|---|---|
| T1 | Content tokens ≥ 1 | Lexical contain applies |
| T2 | NL stopwords only / no content tokens | Pure meaning path; relative cutoff; no boost on `which`/`file`/`has` |
| T3 | Embed input | Uses **content cue** when tokens exist; else normalized raw |
| T4 | Query normalize | Trim, collapse whitespace, length cap (align keyword ~120) |
| T5 | Token boost | Same stopword list as `RecallQueryContentTokens` |
| T6 | CE present / absent | Reorder only on **already precise** pool; identity fallback safe |
| T7 | Card label | Friendly filename (strip storage hash prefix) |
| T8 | Why dialect | Shared contract; meaning presentation consumer-grade |

## Acceptance examples (founder library)

From 2026-09-04 SM-A156E screenshots:

- **Must keep:** `Spelling list 4 (1)-1.pdf` for `silky` (snippet contains silky).
- **Must drop:** Urdu weekly vocab / mock papers / bus rules / PTM schedule /
  worksheets **without** `silky` when cue reduces to content token `silky`.
- **Must work:** `which file has silky in it?` returns the spelling-list class hit.

## Why consumer dialect (v1)

Why must answer in plain language:

1. What you asked (normalized display).  
2. Which file (friendly name).  
3. The saved line that justifies it (short; highlight cue when possible).  
4. How it was found (“by meaning” / “your word appears in the saved text”).

**Not in Why:** storage hashes, “cue-best”, “page 1 fallback”, score jargon,
duplicate full card excerpt when the card already shows it.

## Out of scope for this bar’s first code slices

- One combined Find UI (hybrid) — later; keep keyword capability.  
- ADR-052 pack onboarding.  
- Stage B evidence-native ranker.  
- Cross-lingual semantic match without shared tokens (future innovation ADR).

## Delivery slices against this bar

| Slice | Delivers | Bar IDs |
|---|---|---|
| **MF-1** | Cue object + embed content cue; lexical ≥1; boost stopwords; trusted short list | U1–U4, U6, T1–T6 |
| **MF-2** | Why + card + Open copy cleanup | U5, T7–T8 |
| **MF-3** | Device verify on founder mixed library | All U* |

Do not open ADR-052 UI until MF-1 + MF-2 meet this bar on device (MF-3).

## Explicit non-claims

Not marketing AVAILABLE. Not Grounded Answers. Not a second Find path.
