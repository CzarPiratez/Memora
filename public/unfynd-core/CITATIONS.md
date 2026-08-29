# Citations (maintainer map)

This file is for **private-monorepo maintainers**. External readers of the Class A
pack do not need it. Paths below are **sources of distillation** inside the
private tree; they are not opened by Class A and must not be copied verbatim into
the public pack as hashed freeze blobs.

| Public pack file | Distilled from (private paths) |
|---|---|
| `README.md` / Core noun | `docs/DECISIONS.md` (ADR-046, ADR-047); `docs/OPEN_SOURCE_COMMERCIAL_STRATEGY.md` §23 |
| `SPEC.md` §1–2 definition, privacy, integrity | `docs/PRODUCT_CONTRACT.md` (definition, privacy, memory integrity — high level) |
| `SPEC.md` local-first, seams, Find | `docs/LOCAL_AI_TECHNICAL_SPEC.md` §4 capability list; §9 ordinary Find / no reopen; local-first / §12 prohibitions |
| `SPEC.md` staged model + evidence classes | `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` §2 staged model; §5 evidence classes |
| `SPEC.md` principles | `docs/ARCHITECTURE_FREEZE_v1.0.md` §3 (plain language) |
| `SPEC.md` low-power | `docs/DECISIONS.md` ADR-044; Spec §7 / §9 |
| `SPEC.md` Act out / honesty | `docs/DECISIONS.md` ADR-043, ADR-047 |
| `SPEC.md` MemoryBuilder seam | `CONTINUE.md` MIG-04 summary; Spec §4 MemoryBuilder |
| `SPEC-STATUS.md` App column | `CONTINUE.md` honest status themes (Memory-evidence keyword Find; candidate Find-by-meaning / evidence embeddings; midrange measured ≠ AVAILABLE; Canonical Recall not a live single API; Vision / Links / Event / Knowledge / Act / GA / Class B non-claims); not a dump of App source |
| `examples/` | Synthetic only; authorized by ADR-048; **not** copied from App fixtures |
| `ROADMAP-OPEN.md` Class A vs B | Strategy §23–§24; ADR-047; ADR-048 for samples |
| `CONTRIBUTING.md` / `GOVERNANCE.md` / `PUBLIC_CHANGELOG.md` | Pack hygiene; Class A scope |
| License choice | ADR-047; Apache-2.0 text in `LICENSE` |

Hashed Product Contract, Local AI Spec, Experience Memory Amendment, Architecture
Freeze, and Grounding blobs remain private authority and are **not** republished
as the Class A pack. Update this map when distillation sources change.
