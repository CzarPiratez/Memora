# PDF Local Reading Visible Recovery Plan

**Status:** Foreground Start reads one indexed PDF and may persist searchable text.
**Date:** 2026-07-25
**Requirements:** P-07, P-14, P-15, P-17; A-01, A-02, A-06.
**Governing guardrail:** ADR-017 / ADR-020 (`docs/PDF_PARSER_ISOLATION_REVIEW.md`).

## Purpose

People see an explicit foreground flow that explains folder scope, local reading,
progress/pause/retry, and calm failure recovery. After Continue, **Start** opens one
indexed PDF through the approved broker and isolated parser. When reading succeeds
within budgets, eligible complete/no-text extraction text is saved for search on this
phone. Originals stay unchanged. WorkManager, AI, and network are out of scope here.

## Next slice

Search/recall UI for saved PDF text, or WorkManager-backed incremental indexing —
each with its own change-control approval.
