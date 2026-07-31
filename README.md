# Memora

[![CI](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml/badge.svg)](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml)

Memora is on-device personal AI memory infrastructure that helps people regain
access to their own digital information by simply describing what they remember.
It enables natural-language retrieval of photos, videos, screenshots, documents,
notes, medical records, receipts, and more—without relying on filenames, folders,
or cloud services. Designed for privacy from the ground up, Memora processes
information entirely on-device and provides clear evidence for why each result was
found.

Whether it's "the photo of my granddaughter wearing a red jacket" or "the
prescription after my heart surgery," people simply describe what they remember,
and Memora finds the right information without your private information ever
leaving your phone.

Memora does not take ownership of original user files. Source content stays where
the user already keeps it; Memora stores references and derived memory data only.

## Current status

The Android app is in active foundation build-out: permissioned discovery,
deterministic extract paths, local persistence, and early keyword recall slices
are landing under the product contract. Full natural-language memory recall and
broader source coverage remain in progress.

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
