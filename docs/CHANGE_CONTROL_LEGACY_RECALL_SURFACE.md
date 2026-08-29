# Change control: Legacy recall surface allowlist

**Date:** 2026-08-29  
**Type:** Documentation / operational enforcement (docs-only)  
**Decision guardrails:** Docs only. Do not create Cursor rules. Do not
authorize or implement MIG-05 step 4 / MIG-06+. Do not modify `MemoraApp/**`.
Do not delete keyword Find or `PdfPageEmbedding*` tables. Do not redefine
Freeze §3 or ADR-049. Do not claim Canonical Recall exists in code. No push.

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; Migration Spec MIG-05 / MIG-06 /
  MIG-07 / MIG-07B; MIG-05 step 3 done / step 4 = retire PdfPage.
- **Source documents read:** `CONTINUE` (MIG-05 step 3; ADR-049 note),
  `DECISIONS` ADR-049, `ARCHITECTURE_FREEZE_v1.0` §3,
  `ARCHITECTURAL_MIGRATION_SPEC_V1` (MIG-05–07B),
  `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE` (step 3), CHANGELOG.
- **Current-code evidence inspected (read-only):**
  `SearchAssetMemoriesByMeaning` injects `MemoryEvidenceEmbeddingStore`; does
  **not** take `SavedPdfPageTextSource` or `PdfPageEmbeddingStore` for
  ranking (L6 Retired confirmed). `IndexPdfPageEmbeddings` still dual-writes
  `PdfPageEmbedding*` (L5 Dual).
- **Open ADRs / platform limitations checked:** ADR-049 accepted; Canonical
  Recall not yet one API. Prefer ADR-049 committed separately; this step does
  not reopen it.
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  Documentation only.
- **Smallest safe change:** New living allowlist + CONTINUE pointer +
  CHANGELOG line + this record.
- **Acceptance criteria:** `LEGACY_RECALL_SURFACE.md` with L1–L8, sunsets,
  rules, Live/Dual count; L6 Retired only if code confirms; CONTINUE points
  with current N; zero application code changes; stop before Cursor-rule step.
- **Test and emulator verification plan:** Docs + read-only code inspection.
- **User-visible quality/accessibility review plan:** N/A (no UI).

## Delivery record

- **Files/layers changed:**
  - `docs/LEGACY_RECALL_SURFACE.md` (new)
  - `CONTINUE.md` (Legacy recall surface pointer; Live/Dual = 7)
  - `docs/CHANGELOG.md` (Unreleased one-liner)
  - `docs/CHANGE_CONTROL_LEGACY_RECALL_SURFACE.md` (this file)
- **Automated verification and result:** N/A (docs-only; no app compile).
- **Emulator/manual verification and result:** N/A.
- **Failure/recovery paths verified:** N/A.
- **Known limitation or follow-up:** Cursor rule (Step 3) not started.
  MIG-05 step 4 / MIG-06+ not authorized. Live/Dual count shrinks only as rows
  advance to Retired.
- **Documentation/traceability/ADR updates:** CONTINUE + CHANGELOG; no new ADR
  (seed rows from ADR-049 / MIG sequencing).
- **Git commit:** Local only when requested; no push.
