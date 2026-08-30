# UNFYND

[![CI](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml/badge.svg)](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml)

**[https://www.unfynd.com/](https://www.unfynd.com/)** · **[Core](https://www.unfynd.com/core)**

## Vision

**UNFYND Core®** is on-device **memory and intelligence infrastructure** — how
memory and intelligence become something you can ship, audit, and extend in
places where the data has to stay put.

Core turns multimodal data into lasting, inspectable intelligence on phones,
laptops, workstations, and controlled networks. It is infrastructure you build
on when the corpus cannot be treated as someone else’s training set.

**Core is the system underneath. You build the product.**

| | |
|---|---|
| **What Core provides** | Ingest and retention for multimodal data. Recall and ranking. Answers tied to evidence you can open. Runs at the boundary you set: device, site, or air gap. |
| **What you build** | The app, workflow, domain rules, sources, and permissions — for clinics, field teams, enterprises, research, consumer devices, and more. |

UNFYND Core is **not** a personal AI or assistant. Assistants, companions, and
vertical tools are applications that can be built **on** Core. Industry
application classes (Health, Defence, Government, Enterprise, Research,
Industrial, Accessibility, Emergency, Aging / companion care) are described on
[Core](https://www.unfynd.com/core) and in the open pack
[`public/unfynd-core/APPLICATIONS.md`](public/unfynd-core/APPLICATIONS.md).

**UNFYND App** is the multiplatform product people use (Android today; Windows,
iOS, and Mac in direction). This private monorepo holds App engineering and the
curated Class A Core pack source.

Privacy-first and local-first are foundations of Core: intelligence next to the
data; originals stay with the user / OS; the core path does not send that corpus
away so a system elsewhere can think.

Search and Find are capabilities of products on Core. They are not the whole
definition of UNFYND.

---

## UNFYND App vs UNFYND Core

| | **UNFYND App** | **UNFYND Core** |
|---|---|---|
| What it is | The product people use | On-device **memory and intelligence infrastructure** |
| Surfaces | Android, Windows, iOS, and Mac | Durable multimodal memory next to data that cannot leave; local-first; evidence-backed |
| Role | One application experience on Core | The substrate: Asset → Memory → evidence, capability seams, retrieval and explain |

---

## Direction (building toward)

Product direction on the App surface:

**Sees → Remembers → Connects → Understands → Converses → Acts**

That ladder is where we are building. Stages mature through product phases.
**Act** (agentic action on the user’s behalf) remains out of current architecture
until an explicit later product-contract change.

Core’s broader capability picture — including industries and trust boundaries —
is on [unfynd.com/core](https://www.unfynd.com/core).

---

## Openness

| What | Status |
|---|---|
| Class A contracts pack | Curated Public Specification / Contract under [`public/unfynd-core/`](public/unfynd-core/) (Apache-2.0) |
| Public UNFYND Core GitHub | https://github.com/CzarPiratez/unfynd-core |
| This monorepo / UNFYND App | **Private** until a later decision |

Class A means contracts, capability map, and specs are inspectable. It does
**not** mean the whole stack is open source. Further foundations (including
runnable tooling) arrive as pack updates.

---

## Open the UNFYND App (Android project today)

To run the Android surface of UNFYND App from this private monorepo:

1. Open the Android project folder in Android Studio. The folder is still named
   `MemoraApp/` (deferred technical path — ADR-040; product name is **UNFYND App**).
2. Select a configured emulator or device.
3. Press Run.

Engineering handoff and delivery status live only in [`CONTINUE.md`](CONTINUE.md)
— not in this README.

---

## Identity note (ADR-040)

The product name is **UNFYND**. GitHub path, folder names, and some technical
IDs may still say Memora; that rename is deferred. Do not treat those paths as
the product name.

---

## Project guidance

- [Engineering guide](AGENTS.md)
- [Current handoff](CONTINUE.md)
- [Docs](docs/)
- [UNFYND Core Class A pack](public/unfynd-core/)
