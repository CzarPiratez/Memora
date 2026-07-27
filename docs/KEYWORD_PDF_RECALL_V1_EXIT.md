# Keyword PDF Recall v1 exit

Bounded polish exit for interim keyword PDF recall (not Local-AI / Memory).

## Items

| # | Item | Status |
|---|------|--------|
| 1 | **Open original PDF** — in-app read-only preview of the cited page | Done (2026-07-28) |
| 2 | Clear-query control + clear typed text on index clear | Done (2026-07-28) |
| 3 | Cancel in-flight search | Pending |
| 4 | Accessibility baseline | Pending |

## Open original truthfulness

- Search answers still come only from stored Room page text.
- Open is user-initiated, read-only viewing of the original via the approved SAF
  grant and `PdfRenderer`. Memora does not edit or own the file.
- Primary path is **in-app preview at the cited page** so page honesty can be
  verified with multi-page fixtures (see `MemoraApp/fixtures/`).
- Copy must not promise an external viewer page jump.
