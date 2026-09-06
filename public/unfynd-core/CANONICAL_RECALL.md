# Canonical Recall — request/response sketch

**Status:** Class A planning sketch — **not** a frozen wire format, not a
semver API, not marketing AVAILABLE  
**License:** Apache License 2.0  
**Authority:** `SPEC.md` retrieval-first principles; ADR-049 naming in the
private program. Implementations are held to this shape; this file does not
open App source.

---

## Role

**Canonical Recall** is the sole product-facing Find boundary on a Core
substrate. Keyword, meaning, evidence, and anchor retrieval are **candidate
generation** inside that boundary. They are not independent product search
systems.

Ranking belongs inside Canonical Recall. A result that cannot cite stored
evidence is not a Find hit.

---

## Request (sketch)

| Field | Meaning |
|---|---|
| `query` | Natural-language or literal cue. Required. |
| `mode` | `KEYWORD` (literal evidence) or `MEANING` (semantic candidates). |
| `limit` | Positive upper bound on returned hits. |
| `assetType` | Optional scope (photo, screenshot, document, note). Omit to search the whole store. |

No request may instruct Core to reopen originals or to invent unsupported facts.

---

## Response (sketch)

| Field | Meaning |
|---|---|
| `memoryId` | Stable Memory identity |
| `revisionId` | The revision that was ranked |
| `excerpt` | Stored evidence text (or evidence-cited summary), not generated prose |
| `evidenceId` | Cited evidence when the path carries it; omit rather than invent |
| `locator` | Optional page / span / timecode / segment / message id |
| `retrievalPath` | `KEYWORD` or `MEANING` |
| `rankScore` | Optional finite score; never a calibrated confidence claim |

**Why** (when shown) must name the stored evidence that matched. It must not
invent words the evidence does not have.

`RETRIEVAL_SIGNAL` may help form the candidate pool. It must not appear as the
sole justification for a hit.

---

## Honesty

- Empty store, missing model, or no matching evidence → honest empty or
  blocked outcome, not a fabricated hit.
- This sketch is the contract target. The first implementing surface is
  UNFYND App. Freezing a public SDK / semver API waits for a later Core
  library publish (Class B / module split), not this file.

See [`INTEGRATION.md`](INTEGRATION.md) for how channels (local API, MCP
adapter) sit above this boundary.
