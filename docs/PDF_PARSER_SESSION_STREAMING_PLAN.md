# PDF Parser Session/Chunk Streaming Plan

**Status:** Pure ordinary-process session assembler verified; Binder protocol v3 and
service streaming remain unimplemented. Real-source parsing remains disabled.
**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Governing guardrail:** ADR-017 and `docs/PDF_PARSER_ISOLATION_REVIEW.md`.

## Why this exists

Protocol version 2 returns one bounded Binder envelope that already chunks page text
inside a **single** transaction. ADR-017 and the isolation review require a true
**session/chunk** protocol so page output is never one unbounded Binder payload for
user PDFs. This plan defines that protocol and the first pure validation step.

This plan alone does **not** authorize:

- changing `IIsolatedPdfParser` / service protocol version;
- returning searchable text from the ordinary-process client;
- opening a user PDF / real SAF descriptor;
- Room persistence wiring, UI, WorkManager, AI, or network.

## Boundary

| Layer | Responsibility |
|---|---|
| Isolated service (future v3) | Parse the supplied descriptor only; emit one header, then bounded chunks; honor cancel; close its descriptor side. |
| Ordinary-process transport (future) | Pull/validate each Binder message; close the caller descriptor; map death/timeout/cancel to retryable failure. |
| Pure session assembler (this step) | Accumulate validated header + chunks; reject out-of-order, duplicate, oversized, or incomplete sessions without exposing partial text. |
| Existing result contract | Final EXTRACTED / NO_TEXT / terminal outcomes still pass `IsolatedPdfParserResultContract` with injected limits. |

## Future Binder session shape (protocol v3 — not implemented)

Replace the single `parse(pfd, version)` round-trip with:

1. **`begin(source, protocolVersion)`** → content-free **header** Bundle:
   - `schemaVersion`, `outcome`, `retryable`, optional `pageCount`;
   - no page text;
   - for terminal outcomes (`PASSWORD_PROTECTED`, `FAILURE`, and validated
     `NO_EXTRACTABLE_TEXT`), the session ends after `begin` and both descriptor sides
     close;
   - for `EXTRACTED`, the service retains short-lived parse state for this caller only.
2. **`nextChunk()`** → exactly one page-text chunk Bundle **or** an explicit
   `SESSION_COMPLETE` sentinel with no text. Each chunk reuses the existing keys
   (`page_number`, `chunk_index`, `is_final_chunk`, `text`) and must respect injected
   per-chunk / per-page / total limits.
3. **`cancel()`** → cooperative cancel; service drops retained state; caller treats
   the attempt as retryable and content-free.

Rejected Binder designs:

- returning all pages in one transaction (v2 remains synthetic-only proof);
- pushing unbounded callbacks without ordinary-process backpressure;
- passing URI/path/source identity into the service;
- retaining partial searchable Room rows mid-session.

## Pure assembler contract (this delivery)

`IsolatedPdfParserSessionAssembler` accepts ordered events:

1. `Opened(header)` exactly once;
2. zero or more `ChunkReceived(chunk)` only while collecting an `EXTRACTED` session;
3. `Completed` to finalize an `EXTRACTED` session;
4. `Cancelled` at any time after open (or before open as a no-op cancel).

Rules:

- Unknown schema, illegal header shape, chunk before open, second open, chunk after
  terminal/complete/cancel/reject, out-of-order or duplicate chunk indexes, missing
  final chunk markers, missing pages at complete, and any mid-stream size overflow
  transition to **`Rejected`** and discard all retained chunk text.
- `Cancelled` discards retained text and never becomes a valid extraction.
- Only **`Completed`** (or a terminal header that already validates) can become
  `IsolatedPdfParserWireResultValidation.Valid`.
- Rejection and cancellation deliberately carry **no** candidate text.

Limits are injected (`IsolatedPdfParserSessionLimits`), including a per-chunk UTF-16
code-unit ceiling, because production budgets require separately measured policy.

## Acceptance criteria for this slice

1. Focused unit tests prove successful multi-chunk assembly, terminal headers,
   cancellation, and every listed rejection path without exposing partial text.
2. No AIDL, service, client, SAF, Room, UI, WorkManager, AI, or network change.
3. Docs record that Binder v3 streaming and visible recovery remain separate gates.

## Next slices (ordered)

1. Implement protocol v3 AIDL + synthetic isolated-service streaming against
   repository-owned descriptors only; keep the ordinary client status-only until a
   later governed text handoff.
2. Visible foreground progress / pause / retry recovery copy and state (gate 8).
3. Real-source descriptor path only after streaming + recovery pass.

## Change-control record

### Pre-work

- **Requirement IDs:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Sources read:** product registry, Local AI Technical Specification, governance,
  CONTINUE, decisions (ADR-017/020), isolation review, write-path budget, broker plan.
- **Code inspected:** `IIsolatedPdfParser.aidl` (single `parse`), protocol v2 Bundle
  codec/contract, ordinary `IsolatedPdfParserClient` (discards chunks after validate).
- **Privacy / offline / retention:** no source opened; no persistence; no network.
- **Smallest safe change:** plan + pure assembler + unit tests only.

### Delivery

- **Files:** this plan, `IsolatedPdfParserSessionAssembler`, unit tests, CONTINUE /
  CHANGELOG / ADR-017 progress note / traceability touch.
- **Verification:** local `IsolatedPdfParserSessionAssemblerTest` (see CONTINUE).
- **Limitation:** Binder v3, service streaming, visible recovery, and real sources
  remain blocked.
