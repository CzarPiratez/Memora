# Change-control: Propose ADR-022 PDF extraction retention

## Pre-work record

- **Requirement IDs:** P-05, P-07, P-14, P-15, P-17; A-01, A-02, A-06; E-04, E-05.
- **Source documents read:** `AGENTS.md`, `GOVERNANCE.md`, `CONTINUE.md`, ADR-020,
  ADR-021, `PDF_EXTRACTION_DATA_PERSISTENCE_DESIGN.md`, product contract /
  architecture / traceability (persistence path only).
- **Current-code evidence:** Encrypted DB path verified; PDF persistence port is
  content-free eligibility only; no PDF Room content entities. ADR-020 still blocks
  writes; retention policy was the next explicit unresolved decision.
- **Open ADRs:** ADR-022 proposed; ADR-017 real-source gates; ADR-003 notes.
- **Privacy impact:** Documentation only. No new storage of PDF text.
- **Smallest safe change:** Record ADR-022 proposal + refresh ADR-020 gate progress;
  ask product owner to accept recommended or alternative retention.
- **Acceptance criteria:** Decision recorded; CONTINUE names next implementation gate
  only after acceptance. No PDF Room write in this slice.
- **Test plan:** Documentation cross-check only.

## Delivery record

- **Status:** Delivered as proposal; awaiting product-owner acceptance of ADR-022.
