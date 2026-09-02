# Integration surface (planning sketch)

**Status:** Class A planning document — **not** authorized implementation  
**License:** Apache License 2.0  
**Authority:** Informs post-MVP platform phase (P6) in private
`docs/POST_MVP_PROGRAM_V1.md`.

---

## Principle

UNFYND Core exposes **Memory and Evidence as a stable contract**. Integration
protocols (MCP, OS semantic APIs, vertical SDKs) are **channels** — not the
identity of Core.

---

## Layered exposure (target stack)

```text
┌─────────────────────────────────────────────┐
│  External tools (IDE, assistant, vertical)   │
└─────────────────────┬───────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   MCP adapter   Core local API   OS hooks (optional)
   (compat)      (primary OEM)    (AppSearch, etc.)
        │             │             │
        └─────────────┴─────────────┘
                      ▼
        Canonical Recall + Evidence Package
                      ▼
              Memory / evidence store
```

| Layer | Role | Notes |
|---|---|---|
| **Core local API** | Typed Find, Evidence Package, completeness signals | Primary for OEM, air-gap, vertical apps |
| **MCP adapter** | Read-only Find + evidence for Cursor / Claude-class tools | Thin; optional v1 after MVP exit |
| **Import bridges** | Ingest third-party memory exports | Candidate evidence only — see `EXPORT_CONTRACT.md` |
| **OS semantic hooks** | Donate indexed scope to platform search | Optional; does not replace Core store |

---

## MCP positioning

MCP is a **compatibility adapter**, not Core identity. Any MCP surface must:

- Expose **cited evidence**, not opaque context blobs
- Respect **abstain** and completeness honesty
- Stay **read-only** on originals (no file mutation tools in v1)
- Avoid sending source content to remote services on the Core path

---

## Connector pattern (App layer today)

Approved source connectors (e.g. read-only note provider):

- Network for **source read** at the provider boundary
- **No** Memora/Core cloud sync of the intelligence index
- Extract → MemoryBuilder → Canonical Recall on device

Future cloud storage connectors (Drive, iCloud, NAS) follow the same pattern:
index at source, intelligence stays local.

---

## Private program reference

Sequencing and gates: private monorepo `docs/POST_MVP_PROGRAM_V1.md` §8.

Implementation requires change control per slice; MCP server code is **not**
authorized by this sketch alone.
