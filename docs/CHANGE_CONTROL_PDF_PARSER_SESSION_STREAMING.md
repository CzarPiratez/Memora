# Change Control: PDF Parser Session Streaming Plan + Pure Assembler

**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrail:** ADR-017.

## Pre-work

- Write-path budgets closed; remaining ADR-017 gates are streaming, visible recovery,
  then real-source.
- Protocol v2 single-envelope chunking is not the required session/chunk stream.

## Delivered

- `docs/PDF_PARSER_SESSION_STREAMING_PLAN.md` defines future Binder protocol v3.
- `IsolatedPdfParserSessionAssembler` pure state machine with injected limits.
- Unit tests for assembly, terminal headers, cancel, and rejection paths.

## Not delivered

- AIDL / service protocol v3, client streaming, real SAF open, Room/UI wiring,
  WorkManager, AI, network, visible recovery UI.

## Verification

- Local: `IsolatedPdfParserSessionAssemblerTest` **8 of 8** passed on 2026-07-25.
