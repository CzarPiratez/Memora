# Change Control: Open original PDF (in-app cited-page preview)

**Date:** 2026-07-27  
**Requirements:** P-01, P-11, P-13, P-17; A-02, A-05; E-05 (trust / source view).  
**Decision guardrails:** Originals read-only; search stays on stored text; no
AI/network; ADR-002; Keyword v1 exit flagship (`docs/KEYWORD_PDF_RECALL_V1_EXIT.md`).

## Pre-work record

- **Requirement IDs:** P-01, P-11, P-13, P-17; A-02, A-05.
- **Sources read:** AGENTS, governance, CONTINUE, architecture, product contract,
  decisions, fixtures README, current keyword search + SAF descriptor broker.
- **Code inspected:** `PdfKeywordSearchHit` already carries `sourceId`,
  `sourceAssetKey`, `pageNumber`; `SafPdfDescriptorBroker` opens read-only PFDs;
  no viewer after WIP wipe.
- **Platform note:** External `ACTION_VIEW` on emulator often black-screened /
  ANR’d Files. Primary path is in-app `PdfRenderer` preview at the cited page.
- **Smallest safe change:** Application open use case + renderer port + ViewModel
  feedback/preview + per-hit Open button + preview screen; unit tests; docs.
- **Acceptance:** From a keyword hit, Open shows Opening then an in-app page image
  for that hit’s page (or honest SourceUnavailable / CouldNotOpen). Why unchanged.
  Search still does not reopen PDFs. Fixtures in Documents/MemoraFixtures support
  page-honesty checks (`meet mira` → page 2+).

## Architecture layers

UI → ViewModel → `OpenPersistedPdfForViewing` → AssetRepository + SAF broker +
`PdfPagePreviewRenderer` (Android `PdfRenderer`).

## Known limitation

Preview is a rendered page image, not a full PDF reader (no pinch-zoom suite,
annotations, or external share in this slice). Folder grant must still be valid.

## Acceptance record

- **Unit:** `OpenPersistedPdfForViewingTest`, open ViewModel paths,
  `PdfKeywordSearchCopyTest` open/preview honesty — passed.
- **Emulator (2026-07-28):** User confirmed all pass on Medium Phone with
  MemoraFixtures — search page labels, Open original in-app preview, Why still
  works.
- **Git:** `d38a1c8`.
