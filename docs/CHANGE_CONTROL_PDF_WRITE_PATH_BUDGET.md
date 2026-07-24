# Change-control: PDF extraction write-path budgets

## Pre-work record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`, ADR-020,
  ADR-022, persistence design, conversion benchmark pattern.
- **Current-code evidence:** Synthetic Room port writes without size/time budgets.
- **Open ADRs:** ADR-017 real-source gates remain. No WorkManager/AI/network.
- **Privacy impact:** Synthetic fixtures; aggregate Logcat only; no production UI wire.
- **Smallest safe change:** `PdfExtractionWriteBudgets` + enforce in Room port +
  content-free benchmark/overflow tests.
- **Acceptance criteria:** In-budget buckets persist under elapsed ceiling; over-budget
  page count fails safely with zero rows; docs record limits.
- **Test plan:** Unit budget tests + Medium Phone write-path benchmark suite.

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Code:** `PdfExtractionWriteBudgets` enforced in `RoomPdfExtractionPersistencePort`;
  write-path benchmark + overflow instrumentation.
- **Verification:** Unit **3/3**; emulator benchmark **5/5**.
- **Truthfulness:** Synthetic only; no production UI wiring; ADR-017 remains.
