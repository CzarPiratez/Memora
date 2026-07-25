# PDF Parser Session/Chunk Streaming Plan

**Status:** Binder protocol v3 session streaming verified for repository-owned
synthetic descriptors; ordinary client remains status-only. Real-source parsing
remains disabled.
**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Governing guardrail:** ADR-017 and `docs/PDF_PARSER_ISOLATION_REVIEW.md`.

## Why this exists

Protocol version 2 returned one bounded Binder envelope that already chunked page text
inside a **single** transaction. ADR-017 and the isolation review require a true
**session/chunk** protocol so page output is never one unbounded Binder payload for
user PDFs. Protocol v3 implements that Binder shape for synthetic fixtures.

This plan alone does **not** authorize:

- returning searchable text from the ordinary-process client to Room/UI;
- opening a user PDF / real SAF descriptor;
- Room persistence wiring, UI, WorkManager, AI, or network.

## Boundary

| Layer | Responsibility |
|---|---|
| Isolated service (v3) | Parse the supplied descriptor only; emit one header, then bounded chunks; honor cancel; close its descriptor side. |
| Ordinary-process transport (v3) | Pull/validate each Binder message through the session assembler; close the caller descriptor; map death/timeout/cancel to retryable failure; discard text for status-only summary. |
| Pure session assembler | Accumulate validated header + chunks; reject out-of-order, duplicate, oversized, or incomplete sessions without exposing partial text. |
| Existing result contract | Final EXTRACTED / NO_TEXT / terminal outcomes still pass `IsolatedPdfParserResultContract` with injected limits. |

## Binder session shape (protocol v3)

Replaces the single `parse(pfd, version)` round-trip with:

1. **`begin(source, protocolVersion)`** → content-free **header** Bundle:
   - `schemaVersion`, `outcome`, `retryable`, optional `pageCount`;
   - no page text;
   - for terminal outcomes (`PASSWORD_PROTECTED`, `FAILURE`, and validated
     `NO_EXTRACTABLE_TEXT`), the session ends after `begin` and both descriptor sides
     close;
   - for `EXTRACTED`, the service retains a short-lived chunk queue for this caller only.
2. **`nextChunk()`** → exactly one page-text chunk Bundle **or** an explicit
   `SESSION_COMPLETE` sentinel with no text.
3. **`cancel()`** → cooperative cancel; service drops retained state; caller treats
   the attempt as retryable and content-free.

## Pure assembler contract

`IsolatedPdfParserSessionAssembler` accepts ordered events and remains the ordinary-
process validation gate before any later governed text handoff.

## Acceptance criteria for the v3 transport slice

1. Emulator service/client/end-to-end suites prove header + one-chunk-per-pull +
   complete, plus failure/cancel/death paths, without searchable UI exposure.
2. No real SAF open, Room content write, WorkManager, AI, or network.
3. Visible recovery UI and real-source path remain separate gates.

## Next slices (ordered)

1. Visible foreground progress / pause / retry recovery copy and state (gate 8).
2. Real-source descriptor path only after recovery passes.

## Change-control record

### Pre-work

- **Requirement IDs:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
- **Sources read:** product registry, Local AI Technical Specification, governance,
  CONTINUE, decisions (ADR-017/020), isolation review, write-path budget, broker plan,
  session streaming plan.
- **Code inspected:** prior v2 `parse` AIDL/service/client; session assembler.
- **Privacy / offline / retention:** synthetic descriptors only; no persistence; no network.
- **Smallest safe change:** replace Binder contract with begin/nextChunk/cancel and
  keep client status-only.

### Delivery

- **Files:** AIDL v3, session message codec, service streaming, client assembler wiring,
  updated instrumentation tests, this plan, CONTINUE / CHANGELOG / ADR notes.
- **Verification:** Medium Phone emulator **25** focused tests on 2026-07-25.
- **Limitation:** visible recovery UI and real sources remain blocked.
