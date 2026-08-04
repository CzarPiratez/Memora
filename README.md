# Memora

[![CI](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml/badge.svg)](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml)

**Find anything on your phone.**

Simply describe what you remember.

People don't remember filenames, folders, or exact dates. They remember what it
was, what it looked like, or why it mattered. Memora is built around the way
people naturally remember.

Instead of relying on filenames, folders, exact dates, or endless scrolling,
simply describe what you're looking for:

- "the photo of my granddaughter wearing a red jacket"
- "the prescription after my heart surgery"
- "the screenshot about the apartment I wanted to rent"

With thousands of photos, screenshots, documents, notes, and other personal
information stored on our phones—and more being added every day—finding what
you're looking for has become increasingly difficult.

Memora retrieves photos, videos, screenshots, documents, notes, receipts,
medical records, and other supported personal content entirely on-device, so
your private information never leaves your phone.

Built from the ground up for privacy, Memora explains why each result matches
your search, helping you understand and trust what was found.

At its core, Memora is a privacy-first, on-device personal AI memory
infrastructure designed to help people retrieve their own digital information.

Your original files always stay exactly where they are. They remain private,
never leave your phone, and are never copied or taken over by Memora.
Everything happens entirely on-device.

## Current status

**Updated 2026-08-04.** Android app under active product-contract delivery:

- Permissioned discovery + deterministic extract for photos, screenshots, and PDFs
- Keyword Find with Why evidence; Asset Memories from saved facts
- Candidate Find-by-meaning via on-device Universal Sentence Encoder, PDF page
  embeddings, disclosed evidence-token assist, and bounded meaning-index rebuild
  (≤25 memories per tap with live progress)
- GitHub Actions unit-test CI green on `main`

**Not claimed yet:** marketing AVAILABLE / midrange SLA — awaits physical
midrange measurement (M4) and an explicit product decision. Notes providers and
broader source coverage remain later work. See [CONTINUE.md](CONTINUE.md) and
[enterprise meaning checklist](docs/ENTERPRISE_COMPLETION_MEANING_PDF_PAGE_RECALL.md).

## Open the app

1. Open `MemoraApp` in Android Studio.
2. Select the configured emulator.
3. Press the green Run button.

## Project guidance

- [Engineering guide](AGENTS.md)
- [Current handoff](CONTINUE.md)
- [Product contract](docs/PRODUCT_CONTRACT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Decisions](docs/DECISIONS.md)
- [Roadmap](docs/ROADMAP.md)

## Vision

Become the default interface between people and their own information.

- Google: world's information.
- Memora: your information.
