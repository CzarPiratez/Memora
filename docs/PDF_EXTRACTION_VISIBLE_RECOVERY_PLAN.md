# PDF Local Reading Visible Recovery Plan

**Status:** Presentation flow + foreground status-only parse wired; text not persisted.
**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Governing guardrail:** ADR-017 (`docs/PDF_PARSER_ISOLATION_REVIEW.md`).

## Purpose

People see an explicit foreground flow that explains:

1. source scope (the approved folder only; read-only; originals unchanged);
2. that reading is local on this phone;
3. progress, pause, and retry; and
4. calm failure recovery without parser, Binder, or encryption jargon.

After Continue, **Start** runs one status-only parse of the first indexed PDF in the
connected folder through the approved broker and isolated parser. Page text is
discarded in the ordinary client. This slice does **not** write searchable Room
extraction rows, schedule WorkManager, invoke AI, or use the network.

## Boundary

| Layer | Responsibility |
|---|---|
| Pure copy | Stable plain-language strings; jargon-free. |
| Pure session state | Ordered transitions including Completed. |
| Application | `RunPdfLocalReadingStatusCheck` binds parser once per Start. |
| ViewModel | Session + cancellation; no Room extraction write. |
| Compose | Renders explanation and controls on the connected PDF-folder screen. |

## Next slice

Governed searchable PDF extraction persistence from the validated parse path.
