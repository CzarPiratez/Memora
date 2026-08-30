# UNFYND

[![CI](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml/badge.svg)](https://github.com/CzarPiratez/Memora/actions/workflows/ci.yml)

**[https://www.unfynd.com/](https://www.unfynd.com/)** · **[Core](https://www.unfynd.com/core)**

> **UNFYND turns your digital world into intelligence you can act on.**

**UNFYND App** is privacy-first, on-device AI that transforms the information in
your digital world into intelligence. It works across multimodal information
(files, photos, documents, conversations, audio, video, and connected sources),
bringing context and relationships together to produce decisions, outcomes, and
action.

This private monorepo holds UNFYND App engineering and the curated Class A
source for **UNFYND Core®** (on-device memory and intelligence infrastructure).

---

## Pillars

| Pillar | Meaning |
|---|---|
| **On-device intelligence** | Intelligence runs with your information, on the device |
| **Memory** | Durable representation of what matters, so context lasts |
| **Your information** | Originals stay with you / the OS; UNFYND does not take them over |
| **Privacy-first** | Foundation, not an option; the core path does not ship the corpus away to think |
| **UNFYND Core** | The infrastructure underneath that makes App intelligence possible |
| **Behavioral progression** | Sees → Remembers → Connects → Understands → Converses → Acts |

---

## Vision

UNFYND App is an **intelligence** product for your digital world. It is not a
file browser, upload tool, generic chatbot, or search-only app.

What matters is not only where information lives or what format it takes, but
what intelligence can be derived from it: context, knowledge, reasoning,
decisions, outcomes, and action.

### The evolution

```text
Information
    → Intelligence
    → Context
    → Knowledge
    → Reasoning
    → Decisions
    → Outcomes
    → Action
```

Product behavioral progression (direction we are building toward):

**Sees → Remembers → Connects → Understands → Converses → Acts**

Information can come from the device or, where supported and explicitly
permitted, from connected and cloud sources. Privacy-first and consent remain
binding.

Compelling everyday and high-stakes use cases (from “what’s on my phone?” to
work, care, and field settings) are part of how the product creates wow: not
by listing features, but by turning scattered information into intelligence
that hits a real pain point.

---

## What UNFYND App works with

UNFYND is designed for **multimodal information**, not a fixed short list of
types. The long-term surface includes:

| Domain | Examples |
|---|---|
| **Documents & text** | PDFs, Word, spreadsheets, presentations, text, Markdown, CSV, ebooks, webpages, scans, handwriting, receipts, invoices, bills, contracts, certificates, forms |
| **Images** | Photos, screenshots, scans, camera images, messaging images, maps, infographics, whiteboards, labels, product images |
| **Audio** | Voice recordings, voice notes, meetings, interviews, lectures, podcasts |
| **Video** | Camera video, screen recordings, meetings, lectures, video messages |
| **Notes & writing** | Notes, lists, journals, drafts, saved snippets |
| **Conversations** | Messages, chats, conversation history (where supported and permitted) |
| **Email** | Mail, threads, attachments (where supported) |
| **Calendar & events** | Meetings, appointments, reminders, invitations |
| **People & places** | Contacts, people, locations, dates, related context |
| **Connected sources** | Cloud storage, productivity, communication, and other services where supported and explicitly permitted |

Together, different forms of information contribute to the **same**
intelligence: a photo, a conversation, a document, an email, a calendar event,
and a voice note can become context and reasoning that drive what happens next.

---

## Progress

We are building toward that vision step by step. Foundations for on-device
memory, evidence-backed intelligence, and multimodal understanding advance
through product phases. Current engineering in this monorepo continues that
path across supported surfaces (including documents and images such as PDFs,
photos, screenshots, and notes as part of the broader multimodal roadmap).

Engineering handoff lives in [`CONTINUE.md`](CONTINUE.md). This README is
vision and orientation, not a feature checklist.

---

## UNFYND App and UNFYND Core

**UNFYND Core** is the underlying AI infrastructure: multimodal processing,
on-device execution, semantic representation, retrieval, context,
relationships, knowledge, reasoning, intelligence generation, and the path
toward automation and action as capabilities mature.

**UNFYND App** is the product built on that infrastructure.

> UNFYND Core is the intelligence infrastructure. UNFYND App puts that
> intelligence to work across your digital world.

| | **UNFYND App** | **UNFYND Core** |
|---|---|---|
| What it is | The product people use | On-device memory and intelligence infrastructure |
| Surfaces | Android, Windows, iOS, and Mac | Runs where you set the boundary: device, site, or air gap |
| Role | Puts intelligence to work in the digital world | Substrate others (and the App) build on |

Core also unlocks many trust boundaries and industries (health, defence,
government, enterprise, research, industrial, accessibility, emergency, aging /
companion care, and more). See [Core](https://www.unfynd.com/core) and
[`public/unfynd-core/APPLICATIONS.md`](public/unfynd-core/APPLICATIONS.md).

Assistants and companions are applications that can sit **on** Core. Core itself
is infrastructure, not a personal AI or assistant product definition.

---

## Openness

| What | Today |
|---|---|
| Class A Core foundations | [`public/unfynd-core/`](public/unfynd-core/) (Apache-2.0) and https://github.com/CzarPiratez/unfynd-core |
| This monorepo / UNFYND App | Private until a later decision |

Selected Core foundations are open so builders can inspect and build on the
model. Further foundations arrive as pack updates as Core matures.

---

## Open the UNFYND App (Android surface today)

1. Open the Android project folder in Android Studio. The folder is still named
   `MemoraApp/` (deferred technical path, ADR-040; product name is **UNFYND App**).
2. Select a configured emulator or device.
3. Press Run.

---

## Identity note (ADR-040)

The product name is **UNFYND**. GitHub path, folder names, and some technical
IDs may still say Memora; that rename is deferred.

---

## Project guidance

- [Engineering guide](AGENTS.md)
- [Current handoff](CONTINUE.md)
- [Docs](docs/)
- [UNFYND Core Class A pack](public/unfynd-core/)
