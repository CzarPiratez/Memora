# Change Control: On-Device Keyword Search of Saved PDF Text

**Date:** 2026-07-25
**Requirements:** P-01, P-07, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrails:** ADR-020, ADR-022 (current fingerprint only); no AI/network.

## Pre-work record

- **Requirement IDs:** P-01, P-07, P-11, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** `AGENTS.md`, product registry, Local AI spec,
  governance, CONTINUE, product contract, architecture, decisions, roadmap,
  traceability, persist change-control, change-control template.
- **Current-code evidence inspected:** `pdf_extraction_pages` stored; no search
  DAO/use case/UI before this slice; Local PDF reading persists text.
- **Open ADRs:** ADR-020 gate for search UI; ADR-022 supersedes must not surface as
  current; no WorkManager/AI/network in this slice.
- **Privacy:** Searches only Memora-owned encrypted Room rows; does not reopen PDFs
  or leave the device.
- **Smallest safe change:** Keyword/substring search over current-fingerprint page
  text; page number + excerpt hits; honest keyword copy; unit + Room androidTest.

## Delivery record

- **Files/layers changed:** `PdfKeywordSearchSupport`, `SearchPersistedPdfPageText`,
  `PdfExtractionDao.searchCurrentPages`, search UI/ViewModel/copy, welcome entry,
  unit + androidTest, CONTINUE/CHANGELOG/this record.
- **Automated verification and result:** unit support/copy passed; Medium Phone
  keyword search androidTest **2 of 2**; `:app:installDebug` OK.
- **Emulator/manual verification and result:** APK installed; user confirmation
  pending.
- **Failure/recovery paths verified:** blank query; no matches; superseded
  fingerprint ignored via Asset join.
- **Known limitation or follow-up:** Not semantic Memory recall; WorkManager
  deferred; only saved PDF page text is searched.
- **Documentation/traceability/ADR updates:** CONTINUE next step → WorkManager or
  richer recall after confirmation.
- **Git commit:** f08e894
