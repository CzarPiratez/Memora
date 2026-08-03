# Change control: E5c index-time PDF page embeddings

## Pre-work record

- **Requirement IDs:** Recall by meaning with evidence; enterprise completion
  `docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md` §A; ADR-024/025/031.
- **Source documents read:** GOVERNANCE, CONTINUE, PRODUCT registry, Local AI
  Spec, enterprise completion checklist, embedding-first track, E5b2d/E5b2e
  change controls.
- **Current-code evidence inspected:** `memory_embeddings` one row per Memory
  summary; Build meaning index via `IndexMemoryEmbeddings`;
  `SearchAssetMemoriesByMeaning` summary-only; `SavedPdfPageTextSource` has
  page texts; E5b2e open-time re-rank only.
- **Open ADRs / limitations:** Compact MediaPipe candidate only; caps
  MAX_PAGES=12 / MAX_CHARS=480; no AVAILABLE/SLA; sync index drain may be
  slow on large libraries (progress UI later).
- **Privacy / offline:** On-device embed of already-saved page text; no upload;
  originals unchanged.
- **Smallest safe change:** Room v12 `pdf_page_embeddings`; index drain after
  summaries; search ranks summary+page with per-asset dedup; hit shows page N
  + excerpt; Open uses ranked page; honest candidate copy; unit tests +
  migration registration.
- **Acceptance criteria:** See enterprise checklist §A smoke: cue only on later
  page ranks that page in Find by meaning → Open that page. No AVAILABLE.
- **Not in this slice:** Measured baselines; E4b; WorkManager progress;
  intra-page chunks; changing Memory assembly primary.

## Delivery record

- **Status:** Code complete; emulator smoke pending (rebuild meaning index).
- **Files/layers changed:** Room v12 `pdf_page_embeddings`; store/index/search/
  open/UI; disclosure Build index pages; schema export 12.json; docs.
- **Automated verification and result:** Search page-prefer + meaning copy/
  resolve/index unit tests green.
- **Emulator/manual verification and result:** pending user smoke after rebuild
  meaning index (`mira` → Matched page N → Open page N).
- **Known limitation or follow-up:** Measured ADR-024/025; E4b; WorkManager
  progress for large libraries; intra-page chunks.
- **Git commit:** (pending)
