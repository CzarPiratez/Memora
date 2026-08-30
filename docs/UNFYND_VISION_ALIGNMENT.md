# UNFYND vision alignment

This file is **not hashed**. It is **not architectural authority**. **ADR-043
governs.**

UNFYND is Personal Knowledge Infrastructure: a local-first, storage-agnostic
Memory Core. Search/retrieval is one capability, not the product. The Android
app is the first reference implementation. Product language: See → Remember →
Connect → Understand → Converse → Act.

Frozen authority remains Product Contract + Local AI Spec + Experience Memory
Amendment + Freeze change-control; Grounding Architecture for Grounded Answers
only (ADR-042). This map does not reopen the freeze or start MIG-*.

| Theme | Frozen mapping | Status |
|---|---|---|
| See | Source-neutral read-only Assets; discovery adapters; originals not owned or uploaded as the core model | Suitable (MVP sources) |
| Remember | Memory as derived intelligence of an Asset; Product Contract lifecycle; Experience Memory §2.1 | Suitable (Asset Memory) |
| Connect | Asset Memory → evidence-backed Link → Event Memory → Knowledge Memory (Experience Memory §2; Freeze §4 staged future) | Staged; not implemented; not claimed |
| Retrieve | One Memory / evidence substrate; embeddings are retrieval signals only (Freeze §3) | Suitable as architecture; meaning recall still candidate |
| Answer | Grounded Answers over retrieved stored evidence or abstain; Find remains a safety rail (Grounding Architecture; ADR-042) | Staged; architecture accepted; not implemented |
| Sources | MVP: photos, screenshots, PDFs, notes. Drive, iCloud, NAS, email, video, audio, and non-PDF office docs need adapters + an explicit contract/ADR | Not a new Memory Core |
| Life | Life-not-files via staged Connect, not a file-browser product | Staged |
| Converse | Converse-as-Q&A is Grounded Answers, not a generic chatbot (`GROUNDING_ARCHITECTURE.md` §16) | Staged; not chatbot |
| Act | Agentic Personal AI; tools that act on the user's behalf; mutating originals | Out of current architecture |

Do not skip Asset-Memory quality to jump to Links, Events, Grounded Answers,
or conversation UI. Do not treat PRD MVP exclusions as silently in-scope.

## MVP plan vs long-term vision (recorded 2026-08-30)

Planning guidance only. Does **not** reopen Architecture Freeze, rewrite
hashed Product Contract / Spec / Amendment, authorize MIG-*, Grounded Answers
code, Connect implementation, or Act. **ADR-043 still governs Act-out.**

Public / README multimodal maps (documents, audio, video, email, calendar,
conversations, connected sources, and similar) describe the **long-term
surface**. They are **not** silent MVP scope. MVP sources remain photos,
screenshots, PDFs, and notes until an explicit contract/ADR adds a type.

| Concern | In MVP plan? | Note |
|---|---|---|
| Permission / disclosure patterns (sources, AI pack, optional network) | Yes | Already MVP; reuse the same shape later for action consent — do not build an Act consent subsystem now |
| Inspectable evidence + Explain ("Why this result?") | Yes | Core MVP (Find + evidence-backed explain) |
| Asset originals remain read-only | Yes (invariant) | Enforce; do not add mutation scaffolding "for later Act" |
| Connect (Links / Event / Knowledge Memory) | No | Post-MVP per Roadmap / Experience Memory; not an MVP exit gate |
| Grounded Answers | No | Contracts exist; not required to exit PRD MVP Find/Explain |
| Freeze Act / mutation / agent mechanics now | No | Defer until a later ADR + Product Contract change can define action classes, consent, and audit |
| Act implementation | No | Out of current architecture (ADR-043) |

**Rule:** MVP = trustworthy Asset Memory + recall + explain on approved sources.
Post-MVP = Connect → Grounded Answers → (much later) Act. Do not pull Connect
or Grounded Answers into the MVP exit to prepare for Act.
