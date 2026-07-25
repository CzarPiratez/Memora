# PDF Local Reading Visible Recovery Plan

**Status:** Presentation-only recovery flow implemented; does not open documents.
**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Governing guardrail:** ADR-017 gate 8 (`docs/PDF_PARSER_ISOLATION_REVIEW.md`).

## Purpose

Before Memora may open any user PDF for local text extraction, people must see an
explicit foreground flow that explains:

1. source scope (the approved folder only; read-only; originals unchanged);
2. that reading is local on this phone;
3. progress, pause, and retry; and
4. calm failure recovery without parser, Binder, or encryption jargon.

This slice delivers that **visible contract** and tested copy/state. It does **not**
open a descriptor, call the isolated parser for a user document, write Room
extraction rows, schedule WorkManager, invoke AI, or use the network.

## Boundary

| Layer | Responsibility |
|---|---|
| Pure copy | Stable plain-language strings; jargon-free. |
| Pure session state | Ordered transitions for explain → ready → progress → pause → retry/fail. |
| ViewModel | Holds UI state; never opens a PDF. |
| Compose | Renders explanation and controls on the connected PDF-folder screen. |

## User-visible states

1. **Needs explanation** — scope and local-reading facts; Continue acknowledges.
2. **Ready** — Start / not yet reading.
3. **In progress** — truthful “preparing / not opening a document yet” progress; Pause.
4. **Paused** — Resume or Stop.
5. **Retryable problem** — Retry or Stop.
6. **Reconnect folder** — access-style recovery copy (no document open).
7. **Unavailable** — honest stop when text reading is not enabled yet.

## Forbidden user copy

Do not mention SQLCipher, encryption, Keystore, passphrase, Binder, isolated process,
AIDL, PDFBox, or raw exception text.

## Acceptance

1. Unit tests cover copy jargon guards and session transitions.
2. Connected PDF-folder UI shows the flow; Start never opens a PDF.
3. Real-source descriptor opening remains a later ADR-017 gate.

## Next slice

Real-source descriptor path only after this recovery gate is verified.
