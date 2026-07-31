# Change Control — Evidence-backed Asset Memory Persistence

## Pre-work record

- **Requirement IDs:** P-02, P-09, P-11, P-14, P-17; A-02, A-04; E-04, E-05.
- **Source documents read:** Product registry, Local AI Technical Specification,
  governance, product contract, architecture, decisions, roadmap, traceability,
  `CONTINUE.md`, ADR-007, ADR-019, ADR-022, ADR-026, and ADR-028.
- **Current-code evidence inspected:** `Memory.kt`, Asset repository and Room mapping,
  database v7 and migrations, PDF page/metadata persistence, screenshot/photo OCR,
  image EXIF persistence, Hilt composition, clear-index behavior, setup UI, and tests.
- **Open ADRs / limitations checked:** ADR-003 still blocks Notes. No Local-AI pack,
  embedding, semantic ranker, or natural-language recall is available.
- **Privacy / offline / retention impact:** Assembly reads only Memora's encrypted
  Room extraction rows for the Asset's current fingerprint. It does not reopen an
  original, request permission, use network, or invoke AI. Prior fingerprint
  revisions remain immutable provenance until user-confirmed Clear Memora index.
- **Smallest safe change:** Build bounded READY Asset Memory revisions from verified
  PDF text/metadata, screenshot OCR, photo OCR, and useful EXIF fields; persist
  normalized citations; expose an explicit bounded foreground setup action and count.
- **Acceptance criteria:** No blank evidence creates a Memory; every summary and TEXT
  anchor cites stored evidence; identity stays stable across fingerprints while
  revision identity changes; duplicate inserts are idempotent and conflicting payloads
  do not overwrite history; current-ready count excludes superseded fingerprints.
- **Verification plan:** Full debug unit suite; generated Room schema; migration and
  repository execution remain emulator gates.
- **User-visible review:** Copy says deterministic saved facts, on-device, and no
  meaning-based ranking. Existing keyword search remains the interim recall UI.

## Delivery record

- **Layers changed:** Domain revision/repository/fact-source contracts; application
  assembler and bounded drain; Room v8 normalized Memory/evidence/anchor/citation
  persistence and migration; Hilt; setup readiness/action; tests and governance docs.
- **Automated verification:** `:app:testDebugUnitTest` passed on 2026-07-31,
  including deterministic assembly, revision-history, and normalized Room mapping.
- **Emulator/manual verification (accepted 2026-07-31):** Welcome → **Build
  memories from saved facts** reported Built 3 in this step and 3 current
  evidence-backed Asset Memories, with interim keyword-search honesty retained.
  User confirmed pass. `MemoraDatabaseMigrationTest` remains a separate
  instrumentation gate if needed.
- **Failure/recovery:** Missing Asset and blank evidence skip safely; immutable-key
  conflicts and persistence exceptions fail safely; the foreground drain is capped;
  Clear Memora index deletes the entire Memora-owned encrypted database, including
  Memory tables, and resets the setup count.
- **Known limitations:** No Notes, AI Pack, embeddings, semantic ranking,
  natural-language recall, confidence, or Memory search UI. Existing PDF/screenshot/
  photo keyword search remains the truthful interim recall path.
- **Decision record:** No ADR-029 is needed. This implements accepted ADR-007,
  ADR-019, and ADR-022 without changing product direction.
- **Git commit:** Feature `9aaa5f2`; acceptance docs recorded after 2026-07-31
  user pass.
