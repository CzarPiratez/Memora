# Change control: Meaning PDF cue-best page open (E5b2e)

## Why this exists (durable reminder)

E5b2d opens the **Memory summary’s cited** `pdf:page:N` (often page 1 —
first usable DOCUMENT_TEXT). Keyword/`mira` can live on a later page. Full
**index-time page/chunk embeddings** (rank pages in Find by meaning) remains
the long-term path. This slice is the smallest interim wow: at **Open
original**, re-rank **already-saved** PDF page texts against the user’s cue
with the on-device embedder and open the best matching page when it clearly
beats the cite.

**Remembered in:** this file, `CONTINUE.md` (embedding track next),
`docs/CHANGE_CONTROL_MEANING_PDF_CITED_PAGE_OPEN.md` follow-up,
`docs/CHANGE_CONTROL_LOCAL_AI_EMBEDDING_FIRST_TRACK.md`.

## Pre-work record

- **Requirement IDs:** Recall + evidence-backed open; Local AI Spec embedding
  path; ADR-024/025 honesty (no AVAILABLE / SLA).
- **Source documents read:** GOVERNANCE, CONTINUE, PRODUCT registry, Local AI
  Spec, ADR-024/025/031, E5b2d change control, embedding-first track.
- **Current-code evidence inspected:** `OpenMeaningSearchOriginal` uses
  `citedPdfPageNumber`; PDF page text already in Room
  (`AssetMemoryFactDao.findPdfPages` / keyword search rows); MediaPipe
  `EmbeddingEngine` Available after model install; Meaning ViewModel has query
  at open time.
- **Open ADRs / platform limitations:** Compact embedder quality is candidate
  only. Cap pages embedded per open (latency/battery). No marketing AVAILABLE.
- **Privacy / offline / retention:** Uses already-saved page text + on-device
  embedder; no network; no new permissions; no original-file rewrite.
- **Smallest safe change:** Pass cue into PDF open; load current saved pages;
  cosine-rank truncated page texts; open best page if score margin clears a
  floor; else keep cited/fallback page; honest copy.
- **Acceptance criteria:**
  1. Cue that matches a later saved page (e.g. `mira` on page 2/3) opens that
     page when that PDF is the meaning hit (or when re-rank applies to the
     opened asset).
  2. If no page clearly beats the cite, keep E5b2d cited/fallback behavior.
  3. Copy distinguishes cue-best open vs Memory cite / vs keyword Find.
  4. Unit tests for page picker; no AVAILABLE claims.
  5. Emulator smoke on `memora-open-2page` / `3page` if they rank in meaning.
- **Not in this slice:** Index-time per-page/chunk embedding rows (track as
  **E5c** after E5b2e); changing Memory assembly primary page; measured
  AVAILABLE.
- **Test plan:** Unit tests for pure resolver; ViewModel passes query; install
  + smoke.

## Delivery record

- **Status:** Code complete; emulator smoke pending.
- **Files/layers changed:** `SavedPdfPageTextSource` + Room impl; 
  `ResolveMeaningPdfOpenPage`; `OpenMeaningSearchOriginal` cue scoring;
  Meaning ViewModel passes query; copy; PersistenceModule bind; unit tests;
  CONTINUE/CHANGELOG/E5b2d follow-up pointer.
- **Automated verification and result:** ResolveMeaningPdfOpenPage + meaning
  open/copy/VM/search unit tests green.
- **Emulator/manual verification and result:** pending (`mira` on 2page/3page
  PDF if ranked, or Open on a PDF that contains `mira` on a later page).
- **Failure/recovery paths verified:** blank query / engine down / no pages →
  cited or page-1 fallback.
- **Known limitation or follow-up:** Find by meaning still ranks Memory
  summaries (often page-1 text). **Enterprise completion checklist:**
  `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` (**E5c** index-time
  page/chunk embeddings + measured ADR-024/025 + E4b if needed). Do not rely on
  chat memory.
- **Documentation/traceability/ADR updates:** CONTINUE, CHANGELOG, this record,
  E5b2d follow-up pointer.
- **Git commit:** (pending)
