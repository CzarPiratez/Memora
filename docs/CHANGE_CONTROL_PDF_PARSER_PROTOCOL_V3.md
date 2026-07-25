# Change Control: PDF Parser Binder Protocol v3 Streaming

**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrail:** ADR-017.

## Delivered

- AIDL `begin` / `nextChunk` / `cancel` (protocol version 3).
- `IsolatedPdfParserSessionMessageCodec` and service chunk queue streaming.
- Ordinary `IsolatedPdfParserClient` pulls through `IsolatedPdfParserSessionAssembler`
  and returns status only.

## Not delivered

- Visible recovery UI, real SAF open, Room/UI wiring, WorkManager, AI, network.

## Verification

- Medium Phone emulator: **25** focused tests (service, client, end-to-end, binding,
  broker handoff, live process-death) passed on 2026-07-25.
