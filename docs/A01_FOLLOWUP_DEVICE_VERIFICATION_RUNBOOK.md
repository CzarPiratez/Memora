# A-01 follow-up device verification runbook (F-01–F-04)

**Date:** 2026-09-02  
**Type:** Verification — physical device  
**Status:** Open until operator records PASS  
**Prerequisite proof:** `docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md` (A-01 PASS)  
**Code slices:** F-01 keyword AND, F-02 meaning lexical filter, F-03 Why copy, F-04 SAF rescan

Does **not** authorize marketing **AVAILABLE**.

## ARCHITECTURAL BOUNDARY

```
CURRENT LEGACY PATH: none (Live/Dual N = 0)
TARGET PATH: Canonical Recall keyword + meaning ranking stages
WHY THIS CONVERGES: F-01–F-03 tighten recall quality inside Canonical Recall only
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a
EXTENDS LEGACY? no
```

## Host and corpus

| Field | Value |
|---|---|
| Device | Samsung **SM-A156E** (or same midrange class used for A-01) |
| PDF folder | `Documents/unfynd-test` (small folder; avoid `Downloads/Adobe Acrobat` until F-05) |
| Starting corpus | 3 spelling-list PDFs indexed with memories + meaning index |
| Build | Fresh debug install from current `main` after F-01–F-04 slices |

## One-time setup (online)

1. Android Studio → **Run** debug build on device.
2. **Connect PDF folder** → point at `Documents/unfynd-test`.
3. If already connected from prior session, confirm folder still shows indexed state.
4. Confirm **3** Asset Memories and meaning index built (About on-device meaning search).

## F-04 — SAF PDF folder rescan

| Step | Action | Pass criterion |
|---|---|---|
| 1 | Copy a **4th spelling-list PDF** into `Documents/unfynd-test` on the phone | File visible in Files app |
| 2 | Open UNFYND → PDF folder setup for connected folder | Screen loads |
| 3 | Tap **Check for new PDFs** | Indexing runs (not instant empty complete) |
| 4 | Wait for completion summary | Copy mentions new PDFs found (e.g. “Found 1 new PDF…”) |
| 5 | Run **Local PDF reading** for the new file → **Build memories** | Corpus grows to **4** memories |
| 6 | Rebuild meaning index if prompted | Index counts include new memory |

**F-04 verdict:** PASS / FAIL

## F-01 — Keyword multi-word AND

On **Find saved PDF text** (keyword path):

| Query | Expected |
|---|---|
| `scan` | ≥1 hit (same as A-01) |
| `silky` | ≥1 hit |
| `scan silky` | Hits only PDFs where **both** words appear in saved evidence (not whole-phrase-only) |
| `scan silky wreck` | Only PDFs containing all three tokens across saved text |

Expand **Why this result?** on one `scan silky` hit — excerpt should cite saved text; path says exact words.

**F-01 verdict:** PASS / FAIL

## F-02 — Meaning lexical AND filter

On **Find by meaning**:

| Query | Expected |
|---|---|
| `learning spelling of English words` | Multiple hits (semantic recall still works) |
| `files with scan and silky` | Only PDFs whose ranked memory text contains **both** `scan` and `silky` |
| `mira` (or other single-token cue) | Semantic ranking unchanged (no lexical AND gate) |

**F-02 verdict:** PASS / FAIL

## F-03 — Consumer Why copy

On any meaning hit, expand **Why this result?**

| Check | Expected |
|---|---|
| Plain language | Mentions “meaning from your saved memories on this phone” |
| No engineering jargon | Does **not** say “on-device meaning similarity” or “Score reflects…” |
| Evidence | Still cites stored excerpt + PDF label |
| Token boost (if applicable) | Says cue “also appears in the saved text” (not “rank rose because…”) |

Repeat on one keyword hit — still says “exact words in saved evidence”.

**F-03 verdict:** PASS / FAIL

## A-01 regression (offline, after online checks pass)

1. Confirm AI Pack + meaning index built while online.
2. Enable **airplane mode** → force-stop app → cold start.
3. Keyword `scan` → hits + Why + **Open original**.
4. Meaning Find with prior cue → hits + Why + **Open original**.
5. No crash / ANR.

**A-01 regression:** PASS / FAIL

## Record results

Update `docs/CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md` §Follow-up verification with date, build SHA, and F-01–F-04 verdicts. Update `CONTINUE.md` checkpoint when all PASS.
