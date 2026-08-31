# Change control: Recall reach — MIG-05 STALE / page evidence index deadlock

**Date:** 2026-08-31  
**Type:** Defect fix (Canonical Recall reach; MIG-05 cutover + meaning-index drain)  
**Decision guardrails:** No new Find path. No Live/Dual growth (N stays 0). No
AVAILABLE claim. No Room schema bump. No legacy extension.

```
ARCHITECTURAL BOUNDARY: Canonical Recall (KEYWORD MemoryEvidence + MEANING
  evidence index drain / MIG-05 cutover integrity)
CURRENT LEGACY PATH (L# or none): none (Live/Dual N = 0)
TARGET PATH: Canonical Recall → SearchMemoryEvidence; Build meaning index →
  MemoryEvidenceEmbeddingStore; MIG-05 STALE only for incomplete evidence reindex
WHY THIS CONVERGES: restores evidence-level reach so keyword and meaning Find
  use stored MemoryEvidence (not summary-only); aligns STALE drain with MIG-05
  step 3 intent (Build fills evidence while/after gap)
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: n/a — defect on canonical path
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset: n/a
LEGACY SURFACE DELTA: unchanged (N = 0)
ESCAPE-HATCH AFTER CHANGE: no — UI still only via Canonical Recall
```

## Pre-work record

- **Requirement IDs:** Freeze §3; ADR-049; MIG-05 cutover integrity; Local AI
  Spec §8 recoverable STALE; P-01 evidence-backed recall.
- **Sources read:** `RECALL_ENFORCEMENT_INDEX`, `LEGACY_RECALL_SURFACE` (N=0),
  `CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE` step 3 (STALE + drain intent),
  CONTINUE, GOVERNANCE, PRODUCT_SOURCE_REGISTRY.
- **Code inspected:** `ApplyMig05EvidenceSearchCutover`,
  `Mig05EvidenceSearchCutoverSelection`, `AiPackDisclosureViewModel` page
  candidate build, `findCurrentReadyMeaningLookups` vs `listMeaningIndexSummaries`,
  keyword `MemoryDao` READY filters, emulator sequence (4 summaries / Pages 0 /
  keyword empty).
- **Root cause:** Opening About/readiness marked first-time PDFs STALE (page
  evidence, zero evidence embeddings) before Build; evidence candidate lookups
  were READY-only → Pages indexed 0 → restore never fired; keyword Find was
  READY-only → empty despite stored excerpts.
- **Smallest safe change:** (A) meaning-index lookups READY+STALE for Build +
  meaning hit display; (B) keyword corpus includes STALE; (C) gap selection
  requires summary embedding already present (do not STALE fresh memories).
- **Acceptance:** Unit tests for selection + cutover; keyword/search compiles;
  assembleDebug; emulator: Build indexes pages > 0; keyword `mira`/`invoice`
  hits; meaning corpus shows evidence vectors after Build.
- **Privacy:** No new network/permissions; integrity on derived data only.

## Delivery record

- **Files:** selection + apply cutover; `MemoryRepository.findMeaningIndexLookups`;
  `MemoryDao` / `RoomMemoryRepository`; `AiPackDisclosureViewModel`;
  `SearchAssetMemoriesByMeaning`; keyword evidence SQL; unit tests; CONTINUE /
  CHANGELOG; this record.
- **Not done:** Welcome product-description copy (separate); mass reindex UI;
  AVAILABLE marketing.
