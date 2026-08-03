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
- [ ] **Smoke (quality):** Cue text only on a later page should rank that page
      first — **not yet** with compact MediaPipe average-word embedder (`mira`
      still prefers screenshot / FOXTROT page 1 over 2page/3page mira pages).
      Track under measured gate + **E4b** larger pack.

### B. Measured quality gate (ADR-024 / ADR-025)

- [ ] Fixed offline PDF + cue corpus (privacy-safe fixtures; no user content in
      repo if sensitive).
- [ ] Record pack/model id + version, device class, latency, storage, page
      hit-rate / ranking metrics per `docs/LOCAL_AI_BENCHMARK_PLAN.md`.
- [ ] Only then: decide whether UI may say measured AVAILABLE (still never invent
      SLAs).

### C. Product / pack quality (related, not PDF-only)

- [ ] **E4b** larger vendor embedder pack when compact MediaPipe is insufficient
      for measured targets.
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
