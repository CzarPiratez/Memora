# Change control: Legacy recall surface CI grep guard

**Date:** 2026-08-29  
**Type:** Soft machine enforcement (script + CI)  
**Decision guardrails:** No MIG-06. No Canonical Recall implementation. No
broad `Search*` bans. N=6 unchanged. ADR-049/050 untouched except index
pointer. No push unless asked.

## Pre-work record

- **Requirement IDs:** `LEGACY_RECALL_SURFACE`; Freeze §3; ADR-049; MIG-05
  step 4 (page store retired); RECALL_ENFORCEMENT_INDEX operator entry.
- **Source documents read:** Lead-locked Step 5 watch-item; LEGACY allowlist;
  RECALL_ENFORCEMENT_INDEX; existing `.github/workflows/ci.yml`.
- **Current-code evidence inspected:** Enumerated L1–L4 `*KeywordSearch*` /
  `SearchPersisted*` under `ui/**` and `application/**`; confirmed
  `PdfPageEmbeddingStore` / `pdf_page_embeddings` only in migrations (+ one
  KDoc reworded to avoid false fail); no `CanonicalRecall` /
  `SearchMemoryEvidence` in main.
- **Open ADRs / platform limitations checked:** MIG-06 not authorized — guard
  fails if `SearchMemoryEvidence` appears early. Canonical Recall target-only.
- **Smallest safe change:** One bash script + CI job + docs pointers; micro
  KDoc reword so current tree PASSes.
- **Acceptance criteria:** Script exits 0 on current tree; CI job present;
  allowlist enumerates existing L1–L4 files; no false positive on
  MemoryBuilder / AssetMemoryFactSource.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: machine guard for legacy Find allowlist (not new Find)
CURRENT LEGACY PATH (L# or none): L1–L4 allowlisted; L5/L6 retired tokens banned
TARGET PATH: Canonical Recall after MIG-07 (not implemented here)
WHY THIS CONVERGES: CI fails resurrection of page store / premature CR / MIG-06
  types / new keyword Find clones outside allowlist
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L1–L4 Live rows (unchanged by this)
EXTENDS LEGACY? no
ESCAPE-HATCH AFTER CHANGE: yes — N=6 unchanged; Canonical Recall not in App
```

## How to test failure (optional dry note)

From repo root (do **not** commit):

```bash
# A: echo a forbidden token into a throwaway main .kt → script exits 1
# B: touch MemoraApp/app/src/main/java/com/memora/app/ui/search/FakeKeywordSearch.kt
#    → script exits 1; delete the file
# C/D: add class CanonicalRecall or SearchMemoryEvidence in main → exit 1
bash scripts/check-legacy-recall-surface.sh
```

## Delivery record

- **Files/layers changed:**
  - `scripts/check-legacy-recall-surface.sh` (new; rule source)
  - `scripts/check-legacy-recall-surface.ps1` (optional Windows forwarder)
  - `.github/workflows/ci.yml` (job `legacy-recall-surface` /
    display name `Legacy recall surface guard`)
  - `ApplyMig05EvidenceSearchCutover.kt` (KDoc: remove literal
    `PdfPageEmbeddingStore` so guard PASSes)
  - `docs/RECALL_ENFORCEMENT_INDEX.md` (Machine check section)
  - `CONTINUE.md` (one line)
  - `docs/LEGACY_RECALL_SURFACE.md` / `docs/ESCAPE_HATCH_AUDIT.md` (CI honesty)
  - `docs/CHANGELOG.md` (Unreleased)
  - this change-control
- **Verification:** Local
  `bash scripts/check-legacy-recall-surface.sh` → **PASS** (exit 0).
- **Non-claims:** Does not implement MIG-06 / Canonical Recall; does not
  change Live/Dual N; does not ban MemoryBuilder / AssetMemoryFactSource.
