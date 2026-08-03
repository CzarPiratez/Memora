# Change Control — Meaning-quality Asset Memory summaries (E5b2c)

**Date opened:** 2026-08-03  
**Status:** **accepted** (engineering lead)  
**Requirements:** A-05; ADR-029, ADR-031  
**Parent track:** `docs/CHANGE_CONTROL_LOCAL_AI_EMBEDDING_FIRST_TRACK.md`

## Problem

Emulator smoke for Find by meaning ranked EXIF-only photo memories whose
summaries were `Dimensions: W × H`. Those near-identical vectors drowned the
real OCR screenshot hit.

## Decision

1. Emit EXIF Asset Memory facts only when date-taken or camera make/model is
   present (dimensions/orientation alone are not facts).
2. Assembly schema **v2** (`asset-memory-facts-v2`) picks OCR / document / note
   text as the Memory summary when present.
3. Ready-memory counts, meaning index drains, and meaning lookups only use the
   current assembly schema — so v1 dimension junk is not re-indexed.

## Acceptance

- Unit tests: OCR preferred over EXIF; dimension-only → NoUsableEvidence.
- Rebuild path: Build memories from saved facts (v2) → Build meaning index.
- No AVAILABLE marketing claim.

## User rebuild (smoke)

1. Welcome → Build memories from saved facts (count should refresh for v2).
2. About on-device meaning search → Build meaning index.
3. Find by meaning with `note on my screenshot` — top hit should be the OCR
   screenshot, not dimension-only photos.
