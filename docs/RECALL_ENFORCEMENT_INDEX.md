# Recall enforcement — operator entry

**If you only read one enforcement file besides `LEGACY_RECALL_SURFACE`, read this
index.**

## Purpose

Operational map for Canonical Recall convergence: keep interim Find paths on the
allowlist measurable and shrinking. Authority remains Freeze §3 + ADR-049 +
Migration Spec — this page does not redefine architecture. Current allowlist:
Live/Dual **N = 0** (L1–L8 Retired). Ranking upgrades (FC-02 / ADR-051) stay
inside Canonical Recall and need their own change control.

## Heartbeat

**Live/Dual N = 0** — source of truth: [`docs/LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md)

**Rule:** N may **only shrink**, or grow only with an **ADR** (+
[`LEGACY_EXTENSION_EXCEPTION.md`](LEGACY_EXTENSION_EXCEPTION.md) when required).
Mirror N in `CONTINUE.md` whenever status changes.

Canonical Recall is the sole product Find boundary (keyword + meaning). Escape-hatch
today: **NO** (L1–L8 Retired). Program exit
[`RECALL_CONVERGENCE_DONE.md`](RECALL_CONVERGENCE_DONE.md) is **COMPLETE**.
Honest: program DONE ≠ marketing **AVAILABLE**; FC-02 Stage A/B still open under
ADR-051.

## 60-second checklist (any Find / Recall / embedding-search change)

1. Open [`LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md) — note Live/Dual **N** and which **L#** your change touches.
2. Do **not** extend a non-Retired Live/Dual row (new ranking, hit types, asset Finds, or Why pipelines) unless ADR + exception with sunset.
3. On any cutover / status change: recompute **N** in the LEGACY header and mirror it in `CONTINUE.md`.
4. If opening Find/Recall change-control: fill the architectural convergence block ([`CHANGE_CONTROL_TEMPLATE.md`](CHANGE_CONTROL_TEMPLATE.md)).
5. Do **not** claim marketing **AVAILABLE** from this index. Program
   `RECALL_CONVERGENCE_DONE` is already COMPLETE; ranking upgrades (FC-02 /
   ADR-051) stay inside Canonical Recall and need their own change control.

## Machine check

**Script:** [`scripts/check-legacy-recall-surface.sh`](../scripts/check-legacy-recall-surface.sh)
(run from repo root via Git Bash, WSL, or CI). Optional Windows forwarder:
[`scripts/check-legacy-recall-surface.ps1`](../scripts/check-legacy-recall-surface.ps1).

**CI job:** `Legacy recall surface guard` in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
(fast; no JDK). Fails the workflow on violation.

**Guards:** retired page-embedding tokens outside migration history; no new
`*KeywordSearch*` / `SearchPersisted*` under `ui/**` or `application/**` beyond
the allowlist in the script (L1–L4 Retired MemoryEvidence-backed helpers;
`SearchPersistedPdfPageText`, `SearchPersistedScreenshotOcrText`,
`SearchPersistedPhotoOcrText`, and `SearchPersistedNotePageText` deleted);
`CanonicalRecall` **ALLOWED** as thin KEYWORD façade
(`application/memory/CanonicalRecall.kt`) + four keyword ViewModels;
`SearchMemoryEvidence` **ALLOWED** as candidate gen (not under `ui/**`).
Does **not** ban MemoryBuilder / AssetMemoryFactSource.

## Deeper docs (links only)

| Need | Link |
|------|------|
| Allowlist + statuses | [`LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md) |
| Naming / Option C | ADR-049 in [`DECISIONS.md`](DECISIONS.md) · [`CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md`](CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md) |
| Evidence-native RecallRanker | ADR-051 in [`DECISIONS.md`](DECISIONS.md) · [`CHANGE_CONTROL_ADR051_EVIDENCE_NATIVE_RECALL_RANKER.md`](CHANGE_CONTROL_ADR051_EVIDENCE_NATIVE_RECALL_RANKER.md) · FC-02 [`CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md`](CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK.md) · Stage A model brief [`dependency-review/fc02-stage-a-recall-ranker-model-brief.md`](dependency-review/fc02-stage-a-recall-ranker-model-brief.md) |
| Cursor invariants | [`.cursor/rules/unfynd-architecture-invariants.mdc`](../.cursor/rules/unfynd-architecture-invariants.mdc) |
| Change-control + exception | [`CHANGE_CONTROL_TEMPLATE.md`](CHANGE_CONTROL_TEMPLATE.md) · [`LEGACY_EXTENSION_EXCEPTION.md`](LEGACY_EXTENSION_EXCEPTION.md) |
| Program / MIG-05 DONE | [`RECALL_CONVERGENCE_DONE.md`](RECALL_CONVERGENCE_DONE.md) · MIG-05 FULL DONE in [`CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`](CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md) (**ADR-050:** A=PDF slice complete; B=Spec full open) |
| MIG-05 claim levels | ADR-050 in [`DECISIONS.md`](DECISIONS.md) · [`CHANGE_CONTROL_ADR050_MIG05_CLAIM_LEVELS.md`](CHANGE_CONTROL_ADR050_MIG05_CLAIM_LEVELS.md) |
| MIG-06 step 1 (additive) | [`CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE.md`](CHANGE_CONTROL_MIG06_SEARCH_MEMORY_EVIDENCE.md) |
| MIG-07 PDF cutover | [`CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER.md`](CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER.md) |
| MIG-07 screenshot cutover | [`CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER.md`](CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER.md) |
| MIG-07 photo cutover | [`CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER.md`](CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER.md) |
| MIG-07 note cutover | [`CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER.md`](CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER.md) |
| Shared hit/Why (DRAFT) | [`CANONICAL_RECALL_RESULT_CONTRACT.md`](CANONICAL_RECALL_RESULT_CONTRACT.md) |
| Meaning Find intent / scenario bar | [`MEANING_FIND_PRODUCT_SCENARIO_BAR.md`](MEANING_FIND_PRODUCT_SCENARIO_BAR.md) |
| D-20 meaning ranking lexical veto | [`CHANGE_CONTROL_MEANING_RANKING_LEXICAL_VETO.md`](CHANGE_CONTROL_MEANING_RANKING_LEXICAL_VETO.md) — ranking sequence **stopped** at Phase 1 gate; probe / Phase 1 / page freeze / Phase 0 traces delivered; 1b–1d quotas removed; residual is D-21 |
| D-21 meaning Partial families | [`CHANGE_CONTROL_MEANING_PARTIAL_FAMILIES.md`](CHANGE_CONTROL_MEANING_PARTIAL_FAMILIES.md) — **landed**; keep any named-word hit already in the 60; not I3 / encoder / FTS |
| D-22 meaning gold-in-pool measure | [`CHANGE_CONTROL_MEANING_GOLD_IN_POOL.md`](CHANGE_CONTROL_MEANING_GOLD_IN_POOL.md) — **landed measure**; founder `diagnosis=in_pool_off_page`; seating not the leak |
| D-23 meaning page depth mix | [`CHANGE_CONTROL_MEANING_PAGE_DEPTH_MIX.md`](CHANGE_CONTROL_MEANING_PAGE_DEPTH_MIX.md) — **landed**; token-count mix; same-depth families still leaked (D-24) |
| D-24 meaning page family mix | [`CHANGE_CONTROL_MEANING_PAGE_FAMILY_MIX.md`](CHANGE_CONTROL_MEANING_PAGE_FAMILY_MIX.md) — **landed**; token sets share the 20; timetables still late (D-25) |
| D-25 meaning starved-family page | [`CHANGE_CONTROL_MEANING_STARVED_FAMILY_PAGE.md`](CHANGE_CONTROL_MEANING_STARVED_FAMILY_PAGE.md) — **landed**; mixed Partial starved family first; Exact interleave corrected by D-26 |
| D-26 meaning Exact-first page | [`CHANGE_CONTROL_MEANING_EXACT_FIRST_PAGE.md`](CHANGE_CONTROL_MEANING_EXACT_FIRST_PAGE.md) — **landed**; Exact leads; neighbours leftover only; wifi/silky mix unchanged |
| Human recall language (ceiling) | [`HUMAN_RECALL_ASK_MODEL.md`](HUMAN_RECALL_ASK_MODEL.md) — required before further NL Find code |

Program Steps 1–7 landed as docs (2026-08-29). **MIG-07 keyword L1–L4 + MIG-07B
L7/L8 cutovers delivered** (Live/Dual **N = 0**; escape-hatch **NO**).
`RECALL_CONVERGENCE_DONE` is **COMPLETE**. This index does not authorize
marketing AVAILABLE, FC-02 product wire, Stage B, or new Live/Dual rows.
