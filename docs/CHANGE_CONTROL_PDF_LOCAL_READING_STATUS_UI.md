# Change Control: PDF Local Reading Status in Product UI

**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrail:** ADR-017 (status-only ordinary client; no searchable persist).

## Pre-work record

- **Requirement IDs:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `LOCAL_AI_TECHNICAL_SPEC`, `GOVERNANCE`, `CONTINUE`, product contract,
  architecture, decisions (ADR-017/020), roadmap, traceability,
  `PDF_EXTRACTION_VISIBLE_RECOVERY_PLAN`,
  `CHANGE_CONTROL_PDF_REAL_SOURCE_DESCRIPTOR`, change-control template.
- **Current-code evidence inspected:** `PdfLocalReadingViewModel` (presentation
  only), `ParseApprovedPdfWithIsolatedParser` (unbound status path),
  `SafPdfDescriptorBroker`, `IsolatedPdfParserClient` (discards text),
  `AssetRepository` / `AssetDao`, PDF folder setup UI in `MainActivity`.
- **Open ADRs / platform limitations checked:** ADR-017 ordinary client remains
  status-only; ADR-003 notes connector still open; no WorkManager/AI/network.
- **Privacy / source-access impact:** Opens one already-indexed PDF from the
  user-approved SAF tree only, read-only, after grant/fingerprint checks in the
  existing broker. No text leaves the device; no searchable Room write.
- **Smallest safe change:** Wire Local PDF reading **Start / Retry / Resume** to
  one foreground status-only parse of the first indexed PDF for the connected
  folder; map outcomes into existing recovery states; update honest copy; remove
  the obsolete “not enabled yet” demo control.
- **Acceptance criteria:**
  1. After Continue, Start runs a real broker + isolated-parser status check.
  2. Success → Completed; password / access / retryable failures map to existing
     states with jargon-free copy.
  3. Pause/Stop cancel in-flight work; no Room extraction persist; client still
     discards page text; no WorkManager/AI/network.
  4. Unit tests for session/outcome mapping/copy; focused emulator proof for
     asset lookup; manual Start confirmation on connected indexed folder.

## Delivery record

- **Files/layers changed:** application `RunPdfLocalReadingStatusCheck`; domain
  `AssetRepository.findFirstBySourceAndType`; Room DAO/repository; UI session,
  copy, ViewModel, `MainActivity` recovery control; unit + androidTest coverage;
  CONTINUE/CHANGELOG/this record.
- **Automated verification and result:** unit local-reading **9** passed; Room
  asset lookup androidTest **3 of 3** on Medium Phone; `:app:installDebug` OK.
- **Emulator/manual verification and result:** APK installed; user Start →
  Completed (or honest failure) confirmation pending.
- **Failure/recovery paths verified:** unit mapping for password/access/retryable/
  cancel; Pause/Stop cancel signal in ViewModel.
- **Known limitation or follow-up:** Searchable Room persist and WorkManager remain
  separately governed. Only the first indexed PDF is checked per Start.
- **Documentation/traceability/ADR updates:** CONTINUE next step → searchable
  persist; CHANGELOG unreleased entry; ADR-020 gate note updated.
- **Git commit:** b655890
