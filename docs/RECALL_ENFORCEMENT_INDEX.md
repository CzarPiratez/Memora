# Recall enforcement — operator entry

**If you only read one enforcement file besides `LEGACY_RECALL_SURFACE`, read this
index.**

## Purpose

Operational map for Canonical Recall convergence: keep interim Find paths on the
allowlist measurable and shrinking. Authority remains Freeze §3 + ADR-049 +
Migration Spec — this page does not redefine architecture and does **not**
authorize MIG-06+.

## Heartbeat

**Live/Dual N = 6** — source of truth: [`docs/LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md)

**Rule:** N may **only shrink**, or grow only with an **ADR** (+
[`LEGACY_EXTENSION_EXCEPTION.md`](LEGACY_EXTENSION_EXCEPTION.md) when required).
Mirror N in `CONTINUE.md` whenever status changes.

Canonical Recall is **not** a live single App API yet (ADR-049). Escape-hatch
today: **YES** (enabling Live: L1–L4, L7, L8).

## 60-second checklist (any Find / Recall / embedding-search change)

1. Open [`LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md) — note Live/Dual **N** and which **L#** your change touches.
2. Do **not** extend a non-Retired Live/Dual row (new ranking, hit types, asset Finds, or Why pipelines) unless ADR + exception with sunset.
3. On any cutover / status change: recompute **N** in the LEGACY header and mirror it in `CONTINUE.md`.
4. If opening Find/Recall change-control: fill the architectural convergence block ([`CHANGE_CONTROL_TEMPLATE.md`](CHANGE_CONTROL_TEMPLATE.md)).
5. Do **not** claim Canonical Recall exists in code, or authorize MIG-06+ from this index alone.

## Deeper docs (links only)

| Need | Link |
|------|------|
| Allowlist + statuses | [`LEGACY_RECALL_SURFACE.md`](LEGACY_RECALL_SURFACE.md) |
| Naming / Option C | ADR-049 in [`DECISIONS.md`](DECISIONS.md) · [`CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md`](CHANGE_CONTROL_ADR049_CANONICAL_RECALL_NAMING.md) |
| Cursor invariants | [`.cursor/rules/unfynd-architecture-invariants.mdc`](../.cursor/rules/unfynd-architecture-invariants.mdc) |
| Change-control + exception | [`CHANGE_CONTROL_TEMPLATE.md`](CHANGE_CONTROL_TEMPLATE.md) · [`LEGACY_EXTENSION_EXCEPTION.md`](LEGACY_EXTENSION_EXCEPTION.md) |
| Program / MIG-05 DONE | [`RECALL_CONVERGENCE_DONE.md`](RECALL_CONVERGENCE_DONE.md) · MIG-05 FULL DONE in [`CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md`](CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md) |
| Escape-hatch audit | [`ESCAPE_HATCH_AUDIT.md`](ESCAPE_HATCH_AUDIT.md) |
| Shared hit/Why (DRAFT) | [`CANONICAL_RECALL_RESULT_CONTRACT.md`](CANONICAL_RECALL_RESULT_CONTRACT.md) |

Program Steps 1–7 landed as docs (2026-08-29). **MIG code still needs separate
authorization.**
