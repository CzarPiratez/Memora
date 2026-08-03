# Change Control — Local-AI embedding-first track

**Date opened:** 2026-08-02  
**Status:** E0–E5b2b **accepted** (engineering lead). MediaPipe average-word
embedder can become Available after user-approved model download (ADR-031).
Candidate Find-by-meaning UI ranks indexed Asset Memories and can open originals;
marketing AVAILABLE still gated by ADR-024/025 measurements.  
**Requirements:** A-01, A-02, A-03, A-07, E-06; Local AI Technical Spec §4, §6,
§10, §11, §13.  
**Decision guardrails:** ADR-012, ADR-023, ADR-024, ADR-025, **ADR-029**,
**ADR-030**, **ADR-031**.  
**Prior closed track:** measured pack baselines L0–L2
(`docs/CHANGE_CONTROL_LOCAL_AI_MEASURED_PACK_BASELINES.md`) — synthetic integrity
only; does not authorize AVAILABLE.

## Pre-work record

- **Sources read:** Product registry, Local AI Technical Specification, GOVERNANCE,
  CONTINUE, ROADMAP, AI Pack delivery/security plan, compatibility/fallback policy,
  benchmark plan, ADR-023/024/025, measured-baselines close.
- **Current-code evidence inspected:** `domain/intelligence` contracts; Unavailable
  stubs; synthetic L1/L2 harness; Room v9 (no pack install table yet); product
  `UnavailableAiPackManager` still bound.
- **Open limitations:** A-01 offline end-to-end intelligence remains Planned. No
  vendor model selected; no INTERNET for packs; keyword recall ≠ semantic AVAILABLE.
- **Privacy / network / offline / retention:**
  - No user content in pack install metadata or logs.
  - Core create/retrieve/rank/explain remain local after a verified pack is active.
  - Disclosure + affirmative action required before any future download.
  - Failed verify must not invent ACTIVE / AVAILABLE.
- **Smallest safe change for E0:** documentation + ADR-029 only.
- **Acceptance for opening E0:** Engineering lead accepts embeddings-first plan;
  CONTINUE points here; E1 may proceed without user smoke (no UI).

## Lead product framing (must stay true)

Memora’s wow is **trustworthy recall**: find by meaning, explain with evidence,
never fake “AI ready.” Keyword paths stay labeled. Embeddings are the first
capability because they unlock natural-language recall over facts Memora already
stores (PDF/OCR/note text + Asset Memory), without waiting on Vision packs.

## Phased delivery

| Phase | Goal | Ships code? | Hard gate |
|-------|------|-------------|-----------|
| **E0** (this) | ADR-029 + phase plan | Docs | Lead accepts plan |
| **E1** | Install ledger + disclosure ack rules; ledger-backed `AiPackManager` | Domain + unit tests | Never ACTIVE without verified evidence; product engines stay Unavailable |
| **E2** | Persist ledger in Room + migration; still no download | Data adapter | Schema honesty; clear-data clears pack state |
| **E3** | Disclosure UI (size/storage/license) for planned embedding pack | UI + copy | Affirmative action; no silent install; no AVAILABLE claim |
| **E4a** | Offline verify/store pack container after disclosure | Platform + use case; no INTERNET | Ledger ACTIVE ≠ AVAILABLE; clear index clears pack files |
| **E4b** | Network download + verify for chosen vendor pack | Platform + INTERNET scoped | Pack-only network; retain prior known-good |
| **E5a** | Embedding encode contract + Room index + index use case | Domain/data/app | Unavailable engine writes nothing; no meaning-search UI |
| **E5b1** | MediaPipe EmbeddingEngine + model download + index CTA | Platform + UI | Model not in APK; Available only after load; DEGRADED compact model disclosed |
| **E5b2** | Find-by-meaning candidate recall UI + measured claims | UI + recall | ADR-024/025 before marketing AVAILABLE |

**Recommended next code after E0:** **E1 only**.

## Explicit non-goals until later phases

- Model vendor selection or committing model binaries (E4 gate).
- Vision / OCR AI Pack adapters (follow-on tracks).
- Claiming AVAILABLE, latency SLAs, or midrange device support without measurements.
- Replacing keyword UI before measured embedding recall exists.

## Acceptance record — E0 (**accepted** 2026-08-02 — engineering lead)

- **Delivered:** This change-control; ADR-029; CONTINUE / CHANGELOG / ROADMAP
  pointers.
- **Not delivered at E0 alone:** Install ledger (landed in E1), Room, UI,
  download, inference, AVAILABLE.

## E1 continuation — Install ledger rules (domain)

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Disclosure → verify → ACTIVE/fail rules; ledger-backed manager | Domain + unit tests | Never ACTIVE from blank; disclosure before verify; retain prior known-good; product engines stay Unavailable |

### Acceptance record — E1 (**accepted** 2026-08-02 — engineering lead)

- **Delivered:** `AiPackDisclosureSnapshot`, `AiPackInstallLedgerEntry`,
  `AiPackInstallLedger`, `InMemoryAiPackInstallLedger`,
  `LedgerBackedAiPackManager`, `EmbeddingFirstAiPackTrack` planned pack id.
- **Automated:** `AiPackInstallLedgerTest` (JVM).
- **Honesty:** No Room persistence yet (E2); no UI; no download; Hilt/product
  still binds Unavailable stubs; no AVAILABLE claim.
- **Not delivered at E1 close:** Room migration (E2), disclosure UI, pack bytes,
  EmbeddingEngine bind, semantic recall UI.

## E2 continuation — Persist install ledger in Room

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Durable ledger + migration; DI bind | Room entity/DAO/migration v9→v10; `RoomAiPackInstallLedger`; Hilt | Empty DB never ACTIVE; clear Memora data clears pack state; no download/AVAILABLE |

### Acceptance record — E2 (**accepted** 2026-08-02 — engineering lead)

- **Delivered:** `ai_pack_install_ledger` table (Room v10); `MIGRATION_9_10`;
  `RoomAiPackInstallLedger`; Hilt provides `AiPackInstallLedger` +
  `LedgerBackedAiPackManager` as `AiPackManager`.
- **Automated:** domain ledger tests; `RoomAiPackInstallLedgerIntegrationTest`;
  migration test expects schema v10 + empty ledger.
- **Honesty:** No disclosure UI, INTERNET, pack bytes, EmbeddingEngine bind, or
  AVAILABLE claim.
- **Not delivered at E2 close:** E3 disclosure UI, E4 download/verify, E5 embedding
  recall.

## E3 continuation — Disclosure UI (no download)

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Affirmative disclosure of planned size/storage/license | Welcome entry + honesty screen + ViewModel → ledger ack | No silent install; no INTERNET; no AVAILABLE; keyword paths unchanged |

### Acceptance record — E3 (**accepted** 2026-08-02 — engineering lead)

- **Delivered:** Welcome → **About on-device meaning search**;
  `AiPackDisclosureScreen` / Copy / ViewModel; planned estimate fields on
  `EmbeddingFirstAiPackTrack`; acknowledge persists via Room ledger without
  ACTIVE/download.
- **Automated:** `AiPackDisclosureCopyTest`, `AiPackDisclosureViewModelTest`.
- **User smoke (optional):** open disclosure, tap acknowledge, reopen — status
  shows disclosure recorded; meaning search still off.
- **Not delivered at E3 close:** E4 activate/download, E5 EmbeddingEngine bind.

## E4a continuation — Offline pack-container activate (ADR-030)

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Disclosure → verify → store → ACTIVE offline | Fixture + `ActivateOfflineEmbeddingPackContainer` + no-backup store + UI CTA | No INTERNET; EmbeddingEngine Unavailable; clear deletes payloads |

### Acceptance record — E4a (**accepted** 2026-08-03 — engineering lead)

- **Delivered:** `EmbeddingFirstOfflinePackFixture`; `AiPackPayloadStore` /
  `NoBackupAiPackPayloadStore`; activate use case; disclosure UI
  **Verify and store pack container**; clear-index deletes pack files; ADR-030.
- **Automated:** activate use case + disclosure ViewModel/copy tests.
- **Honesty:** ACTIVE container ≠ meaning search AVAILABLE.
- **Not delivered at E4a close:** E4b network download, E5 embedding index/recall.

## E5a continuation — Embedding index foundation

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Encode contract + Room embeddings + index use case | `embedText`; `memory_embeddings` v11; `IndexMemoryEmbeddings` | Unavailable engine writes nothing; no meaning-search UI/AVAILABLE |

### Acceptance record — E5a (**accepted** 2026-08-03 — engineering lead)

- **Delivered:** `EmbeddingVector` / encode results / cosine helper;
  `EmbeddingEngine.embedText`; Room `memory_embeddings` + `MIGRATION_10_11`;
  `MemoryEmbeddingStore`; `IndexMemoryEmbeddings` (short-circuits when Unavailable);
  Hilt binds `UnavailableEmbeddingEngine`.
- **Automated:** embedding contract + index use case unit tests; schema export v11.
- **Honesty:** Product engine Unavailable; no recall UI; no AVAILABLE claim.
- **Not delivered at E5a close:** real model adapter, meaning-search UI.

## E5b1 continuation — MediaPipe embedder + model download (ADR-031)

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Real on-device embedder after affirmative download | MediaPipe engine; private model store; download + build-index CTAs | No APK model; Available only when model loads; no Find-by-meaning UI yet |

### Acceptance record — E5b1 (**accepted** 2026-08-03 — engineering lead)

- **Delivered:** ADR-031; `tasks-text:0.10.14` dependency review;
  `MediaPipeEmbeddingEngine`; model download use case; disclosure UI download +
  build-index; clear-index deletes model files.
- **Automated:** updated honesty copy tests; compile with MediaPipe.
- **User smoke (recommended):** disclose → download model → build index after
  Asset Memories exist.
- **Not delivered:** E5b2 Find-by-meaning results UI; larger vendor pack (E4b);
  midrange measured AVAILABLE marketing.

## E5b2 continuation — Find-by-meaning candidate recall UI

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Natural-language candidate recall over indexed memories | Search use case; Welcome Find by meaning; Why-on-hit | Candidate/DEGRADED honesty; no marketing AVAILABLE; no silent keyword fallback |

### Acceptance record — E5b2 (**accepted** 2026-08-03 — engineering lead)

- **Delivered:** `listForModel` + Memory→Asset meaning lookups; `SearchAssetMemoriesByMeaning`;
  `LoadMeaningSearchReadiness`; Welcome → Find by meaning screen with Why citations;
  unit tests for ranking/copy/ViewModel.
- **Automated:** full `:app:testDebugUnitTest` (engineering gate).
- **User smoke (recommended):** model → build index → Find by meaning with a recall cue.
- **Not delivered:** Open-original from meaning hits; measured AVAILABLE marketing;
  larger vendor pack (E4b); product `RecallRanker` Available binding.

## E5b2b continuation — Open original from meaning hits

| Goal | Ships | Hard gate |
|------|-------|-----------|
| Reopen cited original from a meaning hit | Route by AssetType to existing openers; PDF page-1 honesty | Search still uses index; no invented page cites |

### Acceptance record — E5b2b (**accepted** 2026-08-03 — engineering lead)

- **Delivered:** `OpenMeaningSearchOriginal`; meaning UI Open original + previews /
  OneNote launch; PDF page-1 disclosed; unit tests for open path.
- **Automated:** meaning ViewModel/copy unit tests; full debug unit suite.
- **User smoke (recommended):** Find by meaning → Open original on screenshot hit.
- **Not delivered:** PDF page-accurate open from meaning; measured AVAILABLE;
  E4b larger pack.

## E5b2c continuation — Meaning-quality Memory summaries

See `docs/CHANGE_CONTROL_MEANING_MEMORY_SUMMARY_QUALITY.md` (**accepted**
2026-08-03). Assembly schema v2; no dimension-only EXIF memories; OCR/text-first
summaries for meaning index quality.

## E5b2d continuation — Meaning PDF open uses cited page

See `docs/CHANGE_CONTROL_MEANING_PDF_CITED_PAGE_OPEN.md`. Meaning open resolves
`pdf:page:N` from Memory summary evidence; fallback page 1 when missing.
Query-best page ranking remains deferred (needs page/chunk embeddings).
