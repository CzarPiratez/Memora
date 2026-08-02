# Change Control — Local-AI embedding-first track

**Date opened:** 2026-08-02  
**Status:** E0–E3 **accepted** (engineering lead). Engines remain Unavailable;
no AVAILABLE UI; no pack download. Product `AiPackManager` is ledger-backed
(empty Room table ⇒ NOT_INSTALLED). E3 disclosure UI records affirmative
acknowledgment only.  
**Requirements:** A-01, A-02, A-03, A-07, E-06; Local AI Technical Spec §4, §6,
§10, §11, §13.  
**Decision guardrails:** ADR-012, ADR-023, ADR-024, ADR-025, **ADR-029**.  
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
| **E4** | Download + atomic verify for chosen pack bytes | Platform + INTERNET scoped | Pack-only network; retain prior known-good |
| **E5** | Bind `EmbeddingEngine`; index + recall candidate path; measure | Intelligence + recall | ADR-024 matrix + benchmark claims before AVAILABLE marketing |

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
- **Not delivered:** E4 download/verify, E5 EmbeddingEngine bind / meaning recall
  UI, AVAILABLE claims.
