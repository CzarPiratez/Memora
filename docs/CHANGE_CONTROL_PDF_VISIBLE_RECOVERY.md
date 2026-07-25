# Change Control: PDF Local Reading Visible Recovery

**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Decision guardrail:** ADR-017 gate 8.

## Delivered

- Plan, jargon-free copy, pure session state, ViewModel, and Local PDF reading card
  on the connected folder screen.

## Not delivered

- Opening user PDFs, parser calls for real sources, Room extraction writes,
  WorkManager, AI, network.

## Verification

- Unit: `PdfLocalReadingCopyTest` 2/2, `PdfLocalReadingSessionTest` 5/5.
- Manual emulator confirmation requested in CONTINUE.
