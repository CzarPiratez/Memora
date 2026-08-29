# Memora Change-Control Record Template

Use this template in the relevant ADR, changelog entry, or delivery handoff before
and after every meaningful product, UX, dependency, data-access, AI, or release
change. It is evidence that the team worked from governing documents and current
code, not from conversational memory.

## Pre-work record

- **Requirement IDs:**
- **Source documents read:** `PRODUCT_SOURCE_REGISTRY`, `GOVERNANCE`, `CONTINUE`,
  product contract, architecture, decisions, roadmap, traceability, and any
  capability-specific specification:
- **Current-code evidence inspected:**
- **Open ADRs / platform limitations checked:**
- **Privacy, source-access, dependency, offline, and data-retention impact:**
- **Smallest safe change:**
- **Acceptance criteria:**
- **Test and emulator verification plan:**
- **User-visible quality/accessibility review plan:**

## Architectural convergence (required if touching Find / Recall / embeddings used for search / ranking / Why)

Fill when changing: `ui/search/**`, `application/**/Search*`, meaning
index/search, ranking, or product Why/evidence presentation for Find.

If the change does **not** touch Find / Recall / search embeddings / ranking /
Why, write `N/A — not a Find/Recall change` and skip the block.

```
ARCHITECTURAL BOUNDARY:
CURRENT LEGACY PATH (L# from LEGACY_RECALL_SURFACE, or none):
TARGET PATH:
WHY THIS CHANGE CONVERGES:
WHAT OLD PATH WILL EVENTUALLY BE RETIRED:
EXTENDS LEGACY? yes / no
IF YES — exception ID / ADR + sunset (MIG/step/date):
LEGACY SURFACE DELTA: unchanged | L# → Dual/Cutover/Retired | new row (ADR-…)
ESCAPE-HATCH AFTER CHANGE: can UI still produce a search hit without Canonical Recall? which L#?
```

Authority: `docs/LEGACY_RECALL_SURFACE.md`, ADR-049, Cursor rule
`unfynd-architecture-invariants.mdc`. Extending a Live/Dual row requires
`docs/LEGACY_EXTENSION_EXCEPTION.md` with a mandatory sunset. Defect fixes that
preserve the existing contract do not need that form.

## Delivery record

- **Files/layers changed:**
- **Automated verification and result:**
- **Emulator/manual verification and result:**
- **Failure/recovery paths verified:**
- **Known limitation or follow-up:**
- **Documentation/traceability/ADR updates:**
- **Git commit:**
