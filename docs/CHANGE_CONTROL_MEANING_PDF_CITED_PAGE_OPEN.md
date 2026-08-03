# Change control: Meaning search opens cited PDF page

## Pre-work record

- **Requirement IDs:** Recall with evidence-backed open of the original Asset;
  PDF page locators already persisted on Memory evidence (`pdf:page:N`).
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `LOCAL_AI_TECHNICAL_SPEC`, `GOVERNANCE`, `CONTINUE`, product contract,
  architecture, ADR-024/025/031, embedding-first + meaning-summary change
  controls, this template.
- **Current-code evidence inspected:** `OpenMeaningSearchOriginal` hardcodes
  page 1; `RoomAssetMemoryFactSource` writes `pdf:page:N`;
  `AssembleAssetMemoryFromExtractionFacts` cites primary evidence locator on
  the summary; keyword PDF open already passes `hit.pageNumber`;
  `MeaningSearchHit` / `MemoryMeaningLookup` lack cited page.
- **Open ADRs / platform limitations checked:** ADR-024/025 — no AVAILABLE /
  SLA claim. Open is the **cited Memory summary evidence page**, not
  query→best-page ranking (needs per-page embeddings later).
- **Privacy, source-access, dependency, offline, and data-retention impact:**
  None new. Same read-only PDF preview path; no network; no schema migration.
- **Smallest safe change:** Load summary-evidence locators with meaning lookups;
  parse `pdf:page:N`; pass cited page into open + copy; fallback page 1 when
  missing/invalid; unit tests.
- **Acceptance criteria:**
  1. PDF meaning hit whose summary cites `pdf:page:N` opens page N.
  2. Missing/invalid locator → page 1 fallback; copy does not claim accuracy.
  3. UI/Why reflect cited page when known; drop “always page 1” wording.
  4. No AVAILABLE / midrange marketing copy.
  5. Unit tests for parser, open page selection, copy honesty.
- **Test and emulator verification plan:** Unit suite; install debug APK;
  multi-page PDF whose primary Memory text is not page 1 → Find by meaning →
  Open original shows that page.
- **User-visible quality/accessibility review plan:** Plain-language hint;
  preview caption already shows page N of count from renderer.

## Delivery record

- **Files/layers changed:** `PdfPageEvidenceLocator`; `MemoryDao` summary
  locator query; `RoomMemoryRepository` / `MemoryMeaningLookup` /
  `MeaningSearchHit` cited page; `OpenMeaningSearchOriginal` page resolve;
  MeaningSearch copy + PDF hit UI; unit tests; CONTINUE/CHANGELOG/embedding
  track pointer.
- **Automated verification and result:** Targeted meaning/PDF cite unit tests
  green (`PdfPageEvidenceLocatorTest`, `OpenMeaningSearchOriginalTest`,
  `SearchAssetMemoriesByMeaningTest`, `MeaningSearchCopyTest`,
  `MeaningSearchViewModelTest`).
- **Emulator/manual verification and result:** **Accepted** 2026-08-04 —
  Find by meaning `mira` → PDF memory `memora-open-5page.pdf` shows
  **Cites page 1** → Open original preview **Page 1 of 5** with matching
  "Page 1 FOXTROT cover sheet" text.
- **Failure/recovery paths verified:** missing/invalid locator → page 1
  fallback with honest copy; out-of-range page still handled by PDF renderer
  (`CouldNotOpen`).
- **Known limitation or follow-up:** Summary still cites first usable
  DOCUMENT_TEXT page — not query-best page (keyword `mira` can be on later
  pages). Page/chunk embeddings needed for true cue→page ranking.
- **Documentation/traceability/ADR updates:** CONTINUE, CHANGELOG, embedding
  track E5b2d pointer, this record.
- **Git commit:** `7e7f237` (feat); docs acceptance follow-up below.
