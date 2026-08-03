# Open / keyword recall PDF fixtures

Small selectable-text PDFs for verifying keyword search page numbers and
Open-original page honesty. Not product content.

| File | Pages | Useful searches |
|------|------:|-----------------|
| `memora-open-2page.pdf` | 2 | `ALPHA` → page 1; `meet mira` / `BRAVO` → page 2 |
| `memora-open-3page.pdf` | 3 | `boarding pass` → page 2; `meet mira` → page 3 |
| `memora-open-5page.pdf` | 5 | `invoice` → page 2; `meet mira` → page 5 |

Regenerate with:

`python generate_open_fixtures.py`

## Emulator use

1. Copy these into a folder Memora can connect via SAF (prefer **Documents** or a
   custom folder — Android blocks tree grants for **Download**).
2. Connect that folder in Memora → Index → Local PDF reading.
3. Search the phrases above; confirm the hit page label.
4. When Open original exists: open a non–page-1 hit and record whether the viewer
   lands on the cited page or page 1 (either outcome is OK if copy is honest).

## Meaning page-recall baseline (M1 / M2)

Labeled cue→page cases for JVM (M1) and on-device MediaPipe (M2) harnesses mirror
these fixtures (corpus id `meaning-pdf-page-recall-v1`). See
`docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_BASELINE.md` and
`docs/CHANGE_CONTROL_MEANING_PDF_PAGE_RECALL_ON_DEVICE.md`. Those harnesses do
**not** authorize product AVAILABLE claims.
