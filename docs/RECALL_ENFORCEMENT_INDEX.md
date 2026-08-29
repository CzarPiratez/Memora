# Recall enforcement — operator entry

**If you only read one enforcement file besides `LEGACY_RECALL_SURFACE`, read this
index.**

## Purpose

Operational map for Canonical Recall convergence: keep interim Find paths on the
allowlist measurable and shrinking. Authority remains Freeze §3 + ADR-049 +
Migration Spec — this page does not redefine architecture and does **not**
authorize MIG-07B from this index alone. MIG-07 PDF + screenshot + photo +
note cutovers are separately authorized (see GOVERNANCE /
`CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER` /
`CHANGE_CONTROL_MIG07_SCREENSHOT_KEYWORD_CUTOVER` /
`CHANGE_CONTROL_MIG07_PHOTO_KEYWORD_CUTOVER` /
`CHANGE_CONTROL_MIG07_NOTE_KEYWORD_CUTOVER`); L1–L4 Retired.

## Heartbeat

**Live/Dual N = 2** — source of truth: [`docs/LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md)

**Rule:** N may **only shrink**, or grow only with an **ADR** (+
[`LEGACY_EXTENSION_EXCEPTION.md`](LEGACY_EXTENSION_EXCEPTION.md) when required).
Mirror N in `CONTINUE.md` whenever status changes.

Canonical Recall is **not** a live single App API yet (ADR-049). Escape-hatch
today: **YES** (enabling Live: L7, L8). L1–L4 Retired (MIG-07 keyword
cutovers complete). Honest: keyword L1–L4 cutovers ≠ Recall program DONE.

## 60-second checklist (any Find / Recall / embedding-search change)

1. Open [`LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md) — note Live/Dual **N** and which **L#** your change touches.
2. Do **not** extend a non-Retired Live/Dual row (new ranking, hit types, asset Finds, or Why pipelines) unless ADR + exception with sunset.
3. On any cutover / status change: recompute **N** in the LEGACY header and mirror it in `CONTINUE.md`.
4. If opening Find/Recall change-control: fill the architectural convergence block ([`CHANGE_CONTROL_TEMPLATE.md`](CHANGE_CONTROL_TEMPLATE.md)).
5. Do **not** claim Canonical Recall exists in code, or authorize MIG-07B
   from this index alone.

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
`SearchPersistedPhotoOcrText`, and `SearchPersistedNotePageText` deleted); no
`CanonicalRecall` type in main; `SearchMemoryEvidence` **ALLOWED** as MIG-06
application use case files + MIG-07 PDF + screenshot + photo + note
ViewModel/adapter only (still **FORBIDDEN** under other `ui/**` / as a new
Find clone). Does **not** ban MemoryBuilder / AssetMemoryFactSource.

## Deeper docs (links only)

| Need | Link |
|------|------|
| Allowlist + statuses | [`LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md) |
| Naming / Option C | ADR-049 in [`DECISIONS.md`](DECISIONS.md) · [`CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md`](CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md) |
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

Program Steps 1–7 landed as docs (2026-08-29). **MIG-07 PDF + screenshot +
photo + note keyword cutovers authorized separately** (L1–L4 Retired).
**MIG-07B still needs separate authorization.** This index does not authorize
those from the heartbeat alone. Keyword L1–L4 cutovers complete ≠ Recall
program DONE (`RECALL_CONVERGENCE_DONE` still open).
