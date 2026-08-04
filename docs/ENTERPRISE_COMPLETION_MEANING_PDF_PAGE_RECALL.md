# Enterprise completion — meaning PDF page recall

**Status:** Open backlog (durable; do not rely on chat memory)  
**Date opened:** 2026-08-04  
**Owner path:** Local-AI embedding-first track after E5b2e  
**Authority:** `AGENTS.md`, `docs/GOVERNANCE.md`, ADR-024/025/031,
`docs/LOCAL_AI_TECHNICAL_SPEC.md`, `docs/LOCAL_AI_BENCHMARK_PLAN.md`

## Current state (honest)

| Slice | What it delivers | Enterprise-complete? |
|-------|------------------|----------------------|
| **E5b2d** | Open the Memory summary’s cited `pdf:page:N` | Partial — cite can be page 1 while the cue matches a later page |
| **E5b2e** | On Open, re-rank **saved** page texts vs cue; open cue-best when clear | Interim wow — not search-time page ranking; not measured quality |
| **E5c** (below) | Index-time page/chunk embeddings; Find by meaning ranks pages | Required for enterprise page recall |
| **Measured gate** | ADR-024/025 baselines + honest AVAILABLE | Required before marketing AVAILABLE / SLA |

E5b2e is a **sound interim**. It is **not** the enterprise end-state for
multi-page PDF meaning recall.

## What “enterprise-grade” means here

1. **Search ranks the right page**, not only a whole-document Memory summary.
2. **Open lands on the ranked page** with evidence the user can trust (Why cites
   that page’s text / locator).
3. **Quality is measured** on a fixed offline corpus (precision@k, page hit rate,
   latency, storage) — no AVAILABLE claim from planning targets alone.
4. **Privacy / offline:** no upload of page text; originals read-only; disclosure
   before any pack download.
5. **Degraded devices** get truthful Unavailable / candidate copy (ADR-024) — never
   silent fallback to keyword labeled as meaning.

## Required work to complete (checklist)

### A. E5c — Index-time PDF page / chunk embeddings

Change-control this as its own approved slice before coding.

- [x] **Design:** One embedding row per PDF page, keyed to Memory revision +
      page + model (Room v12 `pdf_page_embeddings`).
- [x] **Index drain:** Build meaning index embeds capped page texts after
      summaries (MAX_PAGES=12, MAX_CHARS=480).
- [x] **Recall:** Find by meaning ranks summary + page; per-asset dedup prefers
      stronger page hit.
- [x] **Why / Explain:** Page hit Why cites matched page; excerpt from page text.
- [x] **Open original:** Ranked page opens directly (`RANKED_HIT`); E5b2e remains
      fallback for summary-only hits.
- [x] **Honesty copy:** Candidate / not measured AVAILABLE.
- [x] **Tests:** Unit tests for page-prefer search + copy (migration registered
      11→12).
- [x] **Smoke (plumbing):** 2026-08-04 — Build index reported Pages indexed 10;
      Find by meaning shows **Matched page N**; Open opens that ranked page
      (`mira` → `memora-open-5page.pdf` Matched page 1 → Page 1 of 5).
- [x] **Smoke (quality):** 2026-08-04 — after E5d evidence-token boost, cue
      `mira` ranks `memora-open-5page.pdf` **Matched page 5** (“JULIET meet mira
      closing”) above FOXTROT/screenshot; Why discloses assist; Open → **Page 5
      of 5**. E4b still required for stronger semantic-only quality / measured
      AVAILABLE.

### B. Measured quality gate (ADR-024 / ADR-025)

- [x] **M1 JVM harness** (2026-08-04): Fixed offline cue→page corpus
      `meaning-pdf-page-recall-v1` + page hit@1 cosine-only vs E5d boost.
      Change control: `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_BASELINE.md`.
      Does **not** authorize marketing AVAILABLE.
- [x] **M2 on-device** (2026-08-04): Compact MediaPipe on
      `emulator_medium_phone` — cosine-only **0/3**, boosted **3/3**, recommends
      E4b. Change control:
      `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_ON_DEVICE.md`.
      Emulator tier only — not midrange marketing AVAILABLE.
- [x] **M3 on-device USE** (2026-08-04): Universal Sentence Encoder on
      `emulator_medium_phone` — cosine-only **2/3**, boosted **3/3**; keep E5d
      assist. Change control:
      `docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_USE.md`.
- [x] **M4 midrange plan** (2026-08-04): Runbook + honesty matrix —
      `docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md`.
- [ ] **M4 midrange execute:** Run page-recall on physical `midrange_arm64`;
      then product decision for measured AVAILABLE (still never invent SLAs).

### C. Product / pack quality (related, not PDF-only)

- [x] **E4b** Universal Sentence Encoder product embedder (2026-08-04) —
      ADR-032; change control
      `docs/CHANGE_CONTROL_E4B_UNIVERSAL_SENTENCE_ENCODER.md`. Rebuild index
      after upgrade. M3 USE page-recall measurement closed (keep E5d).
- [ ] Optional later: EmbeddingGemma / larger pack if midrange semantic-only
      bar still fails without assist.
- [ ] Battery / latency budgets for index rebuild and query (Spec §11).
- [ ] Cap / progress UI for large PDF libraries (unbounded work forbidden by
      governance).

### D. Explicitly out of “done” until checked

- Open-time-only re-rank (E5b2e) without E5c search ranking
- Keyword Find PDF text labeled as meaning
- AVAILABLE / midrange marketing without measured rows

## Pointers

- Interim open path: `docs/CHANGE_CONTROL_MEANING_PDF_CUE_BEST_PAGE_OPEN.md` (E5b2e)
- Cited-page open: `docs/CHANGE_CONTROL_MEANING_PDF_CITED_PAGE_OPEN.md` (E5b2d)
- Embedding track: `docs/CHANGE_CONTROL_LOCAL_AI_EMBEDDING_FIRST_TRACK.md`
- Continue checkpoint: `CONTINUE.md`
- Benchmark plan: `docs/LOCAL_AI_BENCHMARK_PLAN.md`
