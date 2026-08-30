# Citations (maintainer map)

This file is for **private-monorepo maintainers**. External readers of the Class A
pack do not need it. Paths below are **sources of distillation** inside the
private tree; they are not opened by Class A and must not be copied verbatim into
the public pack as hashed freeze blobs.

| Public pack file | Distilled from (private paths) |
|---|---|
| `README.md` / Core noun / vision | `docs/DECISIONS.md` (ADR-046, ADR-047); https://www.unfynd.com/core; `docs/OPEN_SOURCE_COMMERCIAL_STRATEGY.md` §23 |
| `APPLICATIONS.md` | https://www.unfynd.com/core (capability primitives + industry application classes); builder model |
| `SPEC.md` §1 definition, builder model | Core site vision; `docs/PRODUCT_CONTRACT.md` (privacy / integrity - high level); ADR-046 |
| `SPEC.md` local-first, seams, Find principles | `docs/LOCAL_AI_TECHNICAL_SPEC.md` §4 capability list; §9 ordinary Find / no reopen; local-first / §12 prohibitions |
| `SPEC.md` staged model + evidence classes | `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` §2 staged model; §5 evidence classes |
| `SPEC.md` principles | `docs/ARCHITECTURE_FREEZE_v1.0.md` §3 (plain language) |
| `SPEC.md` low-power | `docs/DECISIONS.md` ADR-044; Spec §7 / §9 |
| `SPEC.md` pack scope / Act as phase | `docs/DECISIONS.md` ADR-043, ADR-047; Core site direction |
| `SPEC.md` MemoryBuilder seam | Spec §4 MemoryBuilder; private MIG-04 delivery (App progress stays private) |
| `examples/` | Synthetic only; authorized by ADR-048; **not** copied from App fixtures |
| `ROADMAP-OPEN.md` Class A vs B | Strategy §23–§24; ADR-047; ADR-048 for samples |
| `CONTRIBUTING.md` / `GOVERNANCE.md` / `PUBLIC_CHANGELOG.md` | Pack hygiene; Class A scope; Core site vision bar |
| License choice | ADR-047; Apache-2.0 text in `LICENSE` |

**App progress / MVP status** is not published in the Class A pack. Maintainers
use `CONTINUE.md` and private change-control docs for App honesty. Do not
reintroduce an App shipped/partial matrix into the public pack.

Hashed Product Contract, Local AI Spec, Experience Memory Amendment, Architecture
Freeze, and Grounding blobs remain private authority and are **not** republished
as the Class A pack. Update this map when distillation sources change.
