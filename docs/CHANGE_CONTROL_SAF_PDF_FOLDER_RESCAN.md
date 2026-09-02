# Change control — SAF PDF folder rescan after completed checkpoint

**Date:** 2026-09-02  
**Type:** Product fix — discovery / PDF setup  
**Status:** Delivered (pending device re-verify)  
**Requirement IDs:** P-04 (incremental discovery); PRODUCT_CONTRACT PDF rescan; A-01 follow-up F-04

## Pre-work record

- **Source documents read:** `PRODUCT_CONTRACT.md`, ADR-008, ADR-010,
  `CHANGE_CONTROL_A01_OFFLINE_DEVICE_PROOF.md` (F-04), `SafPdfDiscoverySource.kt`,
  `IndexSafPdfFolder.kt`, `DocumentTreeSetupViewModel.kt`.
- **Current-code evidence inspected:** Completed SAF checkpoint uses empty frame list;
  subsequent index returned empty discovery page without re-walking folder.
- **Smallest safe change:** Clear completed SAF checkpoint before explicit index;
  honest UI for new-vs-total PDF counts; **Check for new PDFs** button label.
- **Acceptance criteria:**
  - [x] Explicit index after a completed walk clears checkpoint and rescans.
  - [x] Mid-walk **Continue folder indexing** unchanged (non-empty checkpoint).
  - [x] UI reports newly listed PDFs vs folder total.
  - [x] Unit tests for checkpoint clear + summary copy.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY:
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none): none
TARGET PATH: discovery application layer (IndexSafPdfFolder) + setup UI
WHY THIS CONVERGES: fixes incremental PDF discovery without a parallel Find path
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: n/a
```

## Delivery record

- **Files/layers changed:**
  - `DiscoveryCheckpointRepository.delete`
  - `IndexSafPdfFolder.clearCompletedSafPdfCheckpointIfNeeded`
  - `DocumentTreeSetupViewModel` new-vs-total counts
  - `PdfFolderIndexingSummary` + `MainActivity` button labels
- **Automated verification:** `IndexSafPdfFolderTest`, `PdfFolderIndexingSummaryTest`,
  `DocumentTreeSetupViewModelTest` (unit).
- **Device verification (operator):** Add PDFs to `unfynd-test` → **Check for new PDFs**
  → count increases → Local PDF reading picks up new assets.
- **Known follow-up:** Auto-chain local reading after rescan; background periodic rescan;
  large Downloads folder SAF hang (F-05).

## Git commit

Pending operator request.
