# Change-control: ADR-022 accept + synthetic PDF extraction Room schema

## Pre-work record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`, ADR-020,
  ADR-022 proposal, persistence design/contract, architecture, traceability.
- **Current-code evidence:** Schema v3; persistence port was fake-only; encryption
  path expects schema version constant.
- **Open ADRs:** ADR-017 real-source gates; measured write budgets; ADR-003 notes.
- **Privacy impact:** Synthetic fixtures only in tests; production UI not wired to
  persist PDF text. Encrypted DB already protects Memora-owned Room files.
- **Smallest safe change:** Accept ADR-022; additive schema v4 + migration; Room
  port + instrumentation; bump expected schema version for conversion.
- **Acceptance criteria:** Migration preserves assets; atomic persist/idempotent/
  superseded retention/conflict/delete tests pass; no production discovery wiring.
- **Test plan:** Medium Phone migration + Room persistence instrumentation; opener
  smoke on v4.

## Delivery record

- **Status:** Verified complete on 2026-07-25.
- **Verification:** Persistence + migration instrumentation **5/5**; opener **3/3**
  on schema v4.
- **Truthfulness:** Synthetic fixtures only; no production UI wiring; no real PDF
  parse / WorkManager / AI / network.
