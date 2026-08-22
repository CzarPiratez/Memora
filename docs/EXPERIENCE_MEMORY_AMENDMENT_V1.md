# Experience Memory Amendment v1.1

**Product identity:** UNFYND (formerly Memora). Direction: Personal Knowledge Infrastructure. This document’s freeze is the technical invariants below. Naming is not an architectural invariant (ADR-040).

**Status:** Accepted product-direction amendment; staged beyond the current MVP  
**Decisions:** ADR-018 and ADR-019
**Governing baseline:** `docs/product-source/Memora.docx` and accepted local-AI
addenda registered in `docs/PRODUCT_SOURCE_REGISTRY.md`

## 1. Purpose

UNFYND is becoming a private **Personal Knowledge Infrastructure**: a local-first
memory system that helps a person retrieve meaningful evidence about their life and
work. It is not a file browser, import inbox, generic chatbot, or embedding store.

This amendment does not rewrite the immutable PRD or claim new MVP capability. It
sets the frozen direction for future work and makes the boundary between present
implementation, planned MVP, and long-term architecture explicit.

## 2. Staged memory model

```text
Original read-only Asset
        -> Asset Memory
        -> evidence-backed Link
        -> Event Memory
        -> Knowledge Memory
```

### 2.1 Asset Memory — current foundation

An Asset Memory is the validated semantic representation of one version of one
permitted Asset: a photo, screenshot, PDF, or later approved note. It contains
source-derived evidence, searchable anchors, a concise evidence-cited summary, and
version/provenance data. It never owns or replaces the original Asset.

An Asset Memory has a stable **Memory ID**. Its source fingerprint, evidence,
summary, embedding, confidence, and explanation belong to immutable, traceable
revisions. A valid improvement creates a new revision with a supersession reason; it
does not silently rewrite the Memory's history or break references to it.

### 2.2 Link — future correlation primitive

A Link says only that two or more existing memories **appear related**. It is not an
assertion that they are identical or belong to one event. Each Link must retain:

- a relationship type and state (`candidate`, `confirmed`, `rejected`, `superseded`);
- the member-memory identities and versions it relates;
- the evidence supporting every relationship claim;
- origin/version of the rule or local capability that proposed it;
- calibrated confidence and an explicit uncertainty representation;
- timestamps, a reversible lifecycle, and a correction path.

A Link cannot be created solely from embedding similarity. It needs independently
inspectable supporting evidence, such as consistent permitted time, place, person,
text, or deterministic source facts. Conflicting or insufficient evidence produces
no link or a clearly tentative candidate—never a hidden merge.

### 2.3 Event Memory — future, additive context

An Event Memory is a cautious representation of an occurrence, for example a dinner,
trip, meeting, or purchase. It references links and member Asset Memories. It never
deletes the member memories, absorbs their evidence, or asserts an event title as
fact when the evidence only suggests one.

For example, a restaurant photo, bill, map screenshot, and permitted reservation may
be proposed as related. UNFYND must show why: their evidence, source locations,
matching factors, and uncertainty. The user can inspect or correct that relationship.

### 2.4 Knowledge Memory — future, evolving context

A Knowledge Memory is an evidence-backed, evolving object about a subject such as a
project, idea, decision, person, place, or relationship. It may reference many Asset
and Event Memories but is never an untraceable mega-summary. It preserves the links
and evidence that justify each material statement.

## 3. Memory Builder and Context/Correlation boundaries

**Memory Builder** is a first-class subsystem. It validates deterministic extraction
and permitted local observations into a schema-valid Asset Memory. It cannot invent
facts, omit evidence, or use an embedding as a substitute for evidence.

A future **Context and Correlation Engine** proposes links from stored memory/evidence
only. It must be isolated from acquisition and extraction, bounded, local-first,
versioned, retryable, and testable. It cannot reopen an original source during normal
recall. The engine does not make Android private data available: each future source
still needs its own approval and provider-access contract.

Timeline, automatic experience detection, audio/voice notes, and WhatsApp are not
current MVP capabilities. The architecture may be ready for them without pretending
they are available.

## 4. Explain and Trust contract

**Why this result?** is the user-facing trust surface (Explain Mode is the internal
term). It is a first-class product surface, not decorative copy. For every search
result and every future relationship, it must provide the truthful subset that is
available:

1. **Why it matched:** specific stored cues or matching factors.
2. **Evidence:** the bounded facts/excerpts used.
3. **Where it came from:** source type plus safe provenance pointer, respecting
   source access and privacy.
4. **Confidence and uncertainty:** calibrated, non-misleading language; absence of
   evidence is not confidence.
5. **State and freshness:** whether a source is unavailable, a link is tentative, or
   a memory has been superseded.

It must never fabricate support, overstate a proposed relationship, reopen a source
for ordinary recall, present hidden model reasoning as factual explanation, or call a
retrieval result an unsupported chatbot “answer.”

## 5. Truth before intelligence and evidence classes

**Truth before intelligence** is a binding rule: UNFYND must never fabricate
certainty to appear intelligent. No evidence means no assertion; uncalibrated
confidence means no precise confidence claim.

Evidence classes describe support and provenance, not automatic truth:

| Class | Meaning | Can it independently justify a durable claim? |
|---|---|---|
| Direct | A bounded source-derived fact: permitted metadata, OCR, PDF/note text, or EXIF | Yes, subject to provenance and validation |
| Validated observation | A bounded, provenance-cited local model observation | Only after its validator accepts it |
| Retrieval signal | Similarity, embedding, or ranking information | No; candidate generation only |
| Hypothesis | A tentative possible relationship | No; needs further evidence or user confirmation |

## 6. Memory integrity states

The internal lifecycle is precise and durable:

```text
AWAITING_PERMISSION -> QUEUED -> INDEXING -> READY
                         |            |
                         v            v
                SOURCE_UNAVAILABLE  FAILED_SAFELY

READY -> STALE_REINDEX_REQUIRED -> QUEUED
READY -> REMOVED
```

The UI may use calm language such as “Needs attention,” but it must show the actual
cause and safe recovery action. Incomplete, stale, unavailable, or failed derived
content must never appear as a fully ready Memory.

## 7. Embeddings and learning

Embeddings are a versioned retrieval mechanism. They can help discover candidates but
do not define meaning, create truth, or justify a durable memory/link by themselves.

Future personalization may learn from explicit local feedback such as opening,
selecting, correcting, or dismissing a result. It must be opt-in, inspectable,
reversible, retained separately from source content and semantic evidence, and unable
to silently alter a Memory's facts. It is not part of the current MVP.

## 8. Quality and delivery sequence

Before a capability displays confidence or produces a future link, the team must use
a representative private evaluation corpus and measure recall quality,
unsupported-claim rate, explanation coverage, calibration, false-link rate where
applicable, and overconfident-error rate. A confidently wrong output is a more severe
failure than a plainly uncertain wrong candidate.

1. Finish the reliable Asset-Memory MVP: permissioned discovery, deterministic
   extraction, validated local understanding, persistence, natural-language recall,
   and Explain Mode.
2. Specify pure behavioral contracts for Links, confidence/uncertainty, memory
   versions, corrections, and explanation before storage or model work.
3. Build a local correlation capability with fixture-based quality and false-link
   tests; maintain a strict no-evidence/no-link rule.
4. Add Event Memory only after link provenance, correction, and Explain Mode are
   proven.
5. Add Knowledge Memory and optional local personalization only after their privacy,
   retention, quality, and user-control contracts are accepted.

## 9. Non-goals and guardrails

- This amendment does not add WhatsApp, Gmail, Calendar, audio, video, automatic
  timelines, cloud sync, accounts, collaboration, phone-wide chat, or any other
  P-18 exclusion to the MVP.
- It does not replace Android's source access controls or permit reading another
  application's private data.
- It does not add cloud AI, remote embedding, or network dependence to the core path.
- It does not authorize manual per-asset sharing/import as the primary indexing model.
- It does not claim that a correlation, Event Memory, Knowledge Memory, or feedback
  loop exists in current code.

## 10. Current implementation status

**Updated:** 2026-08-05. This section describes shipped Asset-Memory foundation
behavior; it does not claim Event/Knowledge Memories, marketing AVAILABLE, or a
grounded-answer capability.

| Area | Status |
|---|---|
| Asset Memory domain + Room persistence | Shipped (evidence-cited summaries/anchors; immutable revisions) |
| Deterministic extract (PDF text, screenshot/photo OCR, EXIF) | Shipped where built; scanned-PDF OCR remains follow-up |
| Keyword Find + user-facing Why copy | Shipped for PDF / OCR / note paths where built |
| On-device embedding (USE) + Find by meaning | Candidate / DEGRADED_EXPLICIT; midrange M4 and AVAILABLE still open |
| Spec `MemoryBuilder` / `RecallRanker` operate APIs | Contracts exist; production operate paths still outside those interfaces |
| Links, Event Memories, Knowledge Memories, timelines, personalization | Not implemented (future, governed by this amendment) |
| Grounded Answers / query-time ReasoningEngine | **Not implemented**; governed by `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` |

This amendment remains a directional and behavioral contract for Experience Memory
evolution. It is not a completion claim for AVAILABLE semantic recall or Ask.

## 11. Change-control record

- **Requirement IDs:** P-01, P-02, P-09–P-13, P-18, A-01–A-05; E-01–E-06; G-01–G-08.
- **Source documents read:** `AGENTS.md`, product-source registry, Local-AI
  specification, governance, `CONTINUE.md`, product contract, architecture, ADRs,
  roadmap, traceability, `Memory.kt`, and grounded-answers architecture docs.
- **Current-code evidence:** Asset Memories persist; keyword and candidate meaning
  recall exist; Why copy is UI-assembled; no generative ReasoningEngine; no links/
  Event/Knowledge Memories.
- **Open limitations checked:** ADR-003 note-source conflict; P-18 exclusions;
  Android sandbox; meaning AVAILABLE not claimed; Experience status must stay
  aligned with `CONTINUE.md`.
- **Privacy/offline impact:** Documentation only. Future grounded answers remain
  local-first and evidence-bounded per `GROUNDED_ANSWERS_AMENDMENT_V1.md`.
- **Smallest safe change:** Status refresh + cross-links; no code/schema/UI.
- **Acceptance criteria:** Present vs future vs grounded-answer scope are explicit;
  P-18 boundary unchanged.
- **Verification:** Cross-reference with `CONTINUE.md` (2026-08-04/05 checkpoint);
  documentation-only.
