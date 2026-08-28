# UNFYND

[![CI](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml/badge.svg)](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml)

**[https://www.unfynd.com/](https://www.unfynd.com/)**

Your privacy-first, on-device AI.

UNFYND helps people use intelligence grounded in what is already on their
devices and in storage they already use — without sending that information away
so a system elsewhere can think.

## UNFYND App vs UNFYND Core

| | **UNFYND App** | **UNFYND Core** |
|---|---|---|
| What it is | The product people use | On-device **memory and intelligence infrastructure** |
| Surfaces | Android, Windows, iOS, and Mac | Durable multimodal memory next to data that cannot leave; local-first; evidence-backed |
| Role | The application experience | The substrate: Asset → Memory → evidence, capability seams, retrieval and explain |

Search and Find are capabilities of the product. They are not the whole definition
of UNFYND.

## Privacy-first

Privacy-first is the foundation, not an option.

- Intelligence runs **on the device**, with your information.
- Your **original files stay with you**. They remain where the OS keeps them.
  UNFYND does not take them over, and they are not sent away as the core path.
- Photos, screenshots, documents, notes, and other supported personal content
  follow the same promise: the form of the information does not change the rule.

## Direction (not a shipping claim)

Product direction:

**Sees → Remembers → Connects → Understands → Converses → Acts**

That ladder is where we are building. It is **not** a checklist of what already
ships.

- **Act** (agentic action on the user’s behalf) and agents are **not shipped**
  and remain out of current architecture until an explicit later product-contract
  change.
- Do **not** read marketing **AVAILABLE** meaning-recall SLA claims from this
  repository; that bar is not claimed here.

## Openness (honest scope)

We are building toward open on-device memory and intelligence.

| What | Status today |
|---|---|
| Class A contracts pack | Curated Public Specification / Contract files under [`public/unfynd-core/`](public/unfynd-core/) (Apache-2.0) |
| This monorepo / UNFYND App | **Private** until a later decision |
| Public UNFYND Core GitHub repo | **Not published yet** (TBD) |

Class A means contracts and specs are inspectable. It does **not** mean the
whole stack is open source.

## Open the UNFYND App (Android project today)

To run the Android surface of UNFYND App from this private monorepo:

1. Open the Android project folder in Android Studio. The folder is still named
   `MemoraApp/` (deferred technical path — ADR-040; product name is **UNFYND App**).
2. Select a configured emulator or device.
3. Press Run.

Engineering handoff and delivery status live only in [`CONTINUE.md`](CONTINUE.md)
— not in this README.

## Identity note (ADR-040)

The product name is **UNFYND**. GitHub path, folder names, and some technical
IDs may still say Memora; that rename is deferred. Do not treat those paths as
the product name.

## Project guidance

- [Engineering guide](AGENTS.md)
- [Current handoff](CONTINUE.md)
- [Docs](docs/)
- [UNFYND Core Class A pack](public/unfynd-core/)
