# Legacy recall extension exception

**Purpose:** Controlled, rare exceptions when someone believes they must
temporarily **extend** a Live/Dual row on `docs/LEGACY_RECALL_SURFACE.md`
(new ranking behavior, hit types, asset Find surface, or Why pipeline).

**Default is STOP.** Prefer feature work on the Canonical Recall path (or the
MIG that delivers it). This form is for temporary exceptions only — not routine
delivery.

**Defect fixes** that preserve the existing Live/Dual contract do **not** need
this form.

**Authority:** ADR-049; `LEGACY_RECALL_SURFACE` Rules (no extend by default;
exceptions need a sunset); Cursor rule `unfynd-architecture-invariants.mdc`.
Does **not** authorize MIG-06+, reopen the Architecture Freeze, or add a new
L# without a separate ADR.

---

## Required fields

- **Date:**
- **Owner:**
- **L# affected:** (from `LEGACY_RECALL_SURFACE`; Live or Dual only)
- **Why Canonical Recall / listed MIG cannot be used yet:**
- **Exact extension scope:** (what is added — ranking / hit type / Find surface /
  Why; keep narrow)
- **Sunset (mandatory):** Retire by MIG-0X **or** explicit date / step
- **ADR id:** (if new allowlist row or policy exception; else N/A with reason)
- **How `LEGACY_RECALL_SURFACE` will be updated:** (status note, Retire-by,
  Live/Dual count, escape-hatch list — no silent growth)
- **Change-control / convergence block reference:**

---

## Rules

1. **No exception without a sunset.** Missing Retire-by = do not ship.
2. Feature work belongs on Canonical Recall (or the authorizing MIG), not on
   growing a Live/Dual path.
3. Defect fixes that preserve the contract → no form; still fill the
   architectural convergence block on `CHANGE_CONTROL_TEMPLATE` when the change
   touches Find / Recall / search embeddings / ranking / Why.
4. New allowlist rows still require an ADR; this form does not replace that.
5. After approval, update `LEGACY_RECALL_SURFACE` in the same delivery and keep
   Live/Dual count honest.
