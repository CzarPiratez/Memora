# Product Source Registry

## Purpose

This registry preserves the exact approved product-source documents that govern
UNFYND (formerly Memora). The copies in `docs/product-source/` are immutable
reference artifacts. They are version-controlled so delivery never relies on
conversational memory or an untracked desktop file.

## Authority and interpretation

1. **Current product identity (ADR-040 / ADR-045 / ADR-046):** the product/brand
   name is **UNFYND**. Living-canon product language for the substrate is
   **UNFYND Core — on-device memory and intelligence infrastructure** (ADR-046
   refining ADR-045 wording). The Android app is one milestone/reference
   implementation, not the whole system. Search/retrieval is one capability, not
   the product definition. Operating procedure:
   `docs/UNFYND_IDENTITY_TRANSITION_PLAYBOOK.md`.
2. `Memora.docx` remains the **historical** product baseline: MVP sources and
   user promise as originally written. Identity is superseded by ADR-040, not by
   editing the `.docx`. Living canon may say UNFYND after later overlay steps.
3. `Addendum 1.docx` is the accepted Local AI & Offline-First implementation
   amendment. It supersedes the original PRD only where an implementation detail
   conflicts with its local-first rule.
4. `Addendum 2 Engineering Reference.docx` is the approved blueprint for
   `LOCAL_AI_TECHNICAL_SPEC.md`. The Markdown specification is the testable,
   implementation-facing interpretation; it must not weaken Addendum 1.
5. When an ambiguity remains, stop, record an ADR, and ask for a product decision.
6. **Vision vs freeze (ADR-043; noun updated by ADR-045 / ADR-046):** the frozen
   architecture (Product Contract + Local AI Spec + Experience Memory Amendment
   + Freeze change-control; Grounding Architecture for Grounded Answers only,
   per ADR-042) is accepted as suitable for UNFYND Core / on-device memory and
   intelligence infrastructure through See/Remember, staged Connect,
   retrieve-by-meaning, and Understand / converse-as-Q&A. Act / agentic Personal
   AI remains out of current architecture until a later ADR and product-contract
   change. ADR-043’s body and hashed blobs that still say “Personal Knowledge
   Infrastructure” / PKI are left unchanged; PKI is a prior internal noun
   (ADR-045). This does not rewrite hashed blobs, reopen the Architecture
   Freeze, or start MIG-*.
7. **Low-power posture (ADR-044):** interpretation only — UNFYND’s on-device
   power behavior is the event-driven Memory lifecycle already in Local AI Spec
   §7 / §9 and Freeze retrieval-first / truth-before-intelligence (“hippocampus,
   not GPU cluster”), not neuromorphic silicon or always-on sensing. ADR-044 is
   not hashed and does not rewrite Spec, Freeze, or authorize MIG-* / Grounded
   Answers code.
8. **Product noun (ADR-045; public wording refined by ADR-046):** living-canon
   and public product language is **UNFYND Core — on-device memory and
   intelligence infrastructure**. Do not use “Personal Knowledge Infrastructure,”
   “Personal Intelligence Infrastructure,” or third-party grant metaphors in new
   living-canon, CONTINUE, AGENTS identity blurb, or registry interpretation
   lines. Technical IDs remain deferred (`com.memora.app`, `memora.db`,
   Keystore/MSAL hosts, GitHub/`Memora`). ADR-045 / ADR-046 do not by themselves
   open the Android app, MIG-*, Grounded Answers code, Act/agents, or Freeze
   reopen.
9. **Class A Core contracts (ADR-047 / ADR-048):** Release Class A authorizes a
   curated UNFYND Core Public Specification / Contract pack under Apache-2.0.
   Pack files live under `public/unfynd-core/` and are published at
   https://github.com/CzarPiratez/unfynd-core. Public pack leads with Core
   vision and capability/industry map (`APPLICATIONS.md`) aligned to
   https://www.unfynd.com/core; App MVP status is not published in Class A.
   ADR-048 authorizes synthetic non-personal reference samples under the same
   Apache-2.0 license. UNFYND App and proprietary assets stay private until a
   later ADR. Openness is product strategy; do not frame the product by
   external grant programs. Core is infrastructure — not a personal AI /
   assistant product.
10. **Canonical Recall naming (ADR-049):** the sole App product-facing
    retrieval boundary after MIG-07 cutover is **Canonical Recall** (not yet a
    single application API). MIG-06 `SearchMemoryEvidence` is keyword/literal
    candidate generation into that boundary, not a second Find system.
    Grounding’s Retriever is the same converged pipeline under Option C (App
    Find near term; Ask later) — not a competing search architecture. Spec
    `RecallRanker` and MIG-07B structured/anchor filter are stages inside
    Canonical Recall. ADR-049 naming alone does not authorize L2–L4 / MIG-07B
    or rewrite hashed Freeze / Spec / Grounding / Experience Memory blobs.
    MIG-07 PDF cutover is authorized separately in GOVERNANCE /
    `CHANGE_CONTROL_MIG07_PDF_KEYWORD_CUTOVER` (L1 Retired; Live/Dual **N = 0**
    after MIG-07B — see `docs/LEGACY_RECALL_SURFACE.md`).
11. **MIG-05 claim levels (ADR-050):** **A** = PDF evidence-embedding delivery
    slice (steps 1–4) COMPLETE for engineering checkpoint language.
    **B** = Migration Spec MIG-05 full acceptance STILL OPEN until non-PDF
    MemoryEvidence can be embedded at evidence granularity (or a future ADR
    reinterprets MVP scope — not a hashed Spec rewrite). Grants / public /
    marketing must not say “MIG-05 complete” or “Spec MIG-05 done” without
    stating non-PDF evidence indexing remains open (or wait until B closes).
    ADR-050 does not authorize the non-PDF indexer.
12. **Evidence-native RecallRanker (ADR-051):** Spec `RecallRanker` inside
    Canonical Recall is an **Evidence-native on-device RecallRanker** — Stage A
    measured semantic head (FC-02) plus Stage B structured evidence signals.
    FC-03 is a hard dependency before Stage B complete. Type-agnostic ranking
    over `MemoryEvidence`; new Asset types still need their own access /
    extraction ADRs. Does not authorize AVAILABLE, cloud rerank, App wiring by
    itself, or hashed Freeze/Spec rewrites. Implementation remains under
    `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK` (and FC-03 when opened).
13. **Smart automatic AI pack onboarding (ADR-052):** At launch-ready /
    marketing AVAILABLE, meaning + eligible rerank packs install via **one unified
    onboarding Continue** after combined disclosure — not interim multi-tab
    `AiPackDisclosure` engineering flow. Device tier (`RecallRankDevicePolicy`)
    decides rerank auto-install. Not silent; not bundled in APK. Does not ship UI
    from this ADR alone.
14. **Share sheet of a stored original URI (ADR-053):** A user tap on Share that
    opens the Android chooser with a read-only grant of the stored content URI
    is Open-adjacent, not Act. ADR-043 still bars agentic Act, reminders, and
    mutation. Notes have no local file share in this slice. Implementation:
    `CHANGE_CONTROL_OPEN_ORIGINAL_SHARE.md`.
15. **Open the whole original in another app (ADR-054):** A user tap on Open
    full file that starts `ACTION_VIEW` with a read-only grant of the same
    stored URI is the same Open-adjacent handoff as ADR-053, not Act. ADR-053
    point 4 bars `ACTION_VIEW` **as a hidden editor**, not a user-tapped
    read-only viewer. No write grant, no `ACTION_EDIT`, no copy, no upload.
    Notes have no local file. Implementation:
    `CHANGE_CONTROL_OPEN_ORIGINAL_IN_ANOTHER_APP.md`.

## Governed internal amendments

The following repository-owned amendments are accepted product-direction decisions.
They clarify UNFYND's long-term architecture without rewriting the immutable source
documents or expanding the current MVP source scope unless an amendment explicitly
says so:

| Amendment | Authority | Scope |
|---|---|---|
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | User-approved product direction; ADR-018 and ADR-019 | Evidence-first Asset Memories are the MVP foundation. Event, Knowledge, and relationship memories are a future, staged evolution governed by truth before intelligence. |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | User-approved product direction; ADR-033–ADR-039 | Grounded Answers over retrieved stored evidence (or abstain). No chatbot. Find remains source of truth. Canonical engineering map: `docs/GROUNDING_ARCHITECTURE.md`. First slice: PDF saved text (`docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md`). Generative implementation blocked on readiness gates. |

An internal amendment may not silently enable a source, permission, cloud path, or
feature excluded by the immutable PRD. Such a change still requires an explicit
product decision and a recorded ADR.

Product-noun overlay of living constitutions is playbook Step 3; ADR-040 already
binds identity. User-visible Android copy is Step 4. Presentation/entry identifiers
are Step 5. Technical IDs (`applicationId`, `memora.db`, MSAL host, GitHub) remain
deferred.

`docs/ARCHITECTURE_FREEZE_v1.0.md` and `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md`
are registered hashed artifacts (ADR-041). ADR-042 resolves the open authority
conflicts: Freeze §1 exclusivity is the Asset-Memory / PKI core (Freeze §2
items 1–3, Migration Spec sequencing only); Freeze §1 text is not rewritten.
`docs/GROUNDING_ARCHITECTURE.md` and `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md`
remain the sole Grounded Answers constitution and amendment; Grounding is not
retired. Delivery governing order is aligned with Freeze §2 in
`docs/GOVERNANCE.md`. Freeze §7’s “§15 defers to Experience Memory §7” claim is
errata relative to the current hashed Spec and Amendment; that correction is
deferred and those hashes are unchanged. The freeze is the change-control
declaration for the architecture it names. The migration spec is sequencing
authority only. MIG-05 step 4 (`PdfPageEmbedding*` retired; Room 15;
evidence-only index writer; L5 Retired). Do **not** claim Spec MIG-05 / claim
**B** complete while non-PDF evidence indexer remains deferred.
**MIG-07 keyword L1–L4 + MIG-07B L7/L8** are delivered (PDF / screenshot / photo /
note keyword + meaning fold-in). Live/Dual **N = 0** — see
`docs/LEGACY_RECALL_SURFACE.md`. `RECALL_CONVERGENCE_DONE` is **COMPLETE**.
Do **not** claim Spec MIG-05 / claim **B** complete while non-PDF evidence
indexing remains deferred. Do **not** claim marketing **AVAILABLE**. Do **not**
start MIG-08–MIG-11, Grounded Answers runtime, or Act from this registry alone.
FC-02 Stage A/B remains under `CHANGE_CONTROL_FC02_CROSS_ENCODER_RERANK` /
ADR-051 (and ADR-052 for launch pack onboarding). Phase A plan is still not
permission for later MIGs. `docs/PHASE_A_IMPLEMENTATION_PLAN_V1.md` is not
hashed and is not permission to implement.

## Governed architecture artifacts

SHA-256 values are of the Git blob content (playbook Step 3 overlay for the files
whose identity bytes changed; PDF slice hash unchanged because that file was not
overlayed). Change-control and changelog files are not product source and are not
hashed here.

| Artifact | Classification | SHA-256 |
|---|---|---|
| `docs/ARCHITECTURE_FREEZE_v1.0.md` | Canon (architecture freeze declaration) | `5CB1D4D4FA4BC9762A58E13839EBF479ECDECAE03CEE793D76D887844F801758` |
| `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` | Canon (implementation sequencing; Freeze §2 item 4) | `D10AD7C3742529148EB6A14B2DCF106B889CCAA81F0A1B483835800A11EF526E` |
| `docs/LOCAL_AI_TECHNICAL_SPEC.md` | Canon (engineering constitution) | `0338C2EC7397FF0B71A183890CEE71394A837F94F7436661E86D506CB649BBB8` |
| `docs/GROUNDING_ARCHITECTURE.md` | Canon (engineering constitution; sole Grounded Answers constitution) | `DF9A53D7116EC1B464A61AB8C0FBFC4B5C7CD28A9D5EB63193F9ABB1C397CEAA` |
| `docs/EXPERIENCE_MEMORY_AMENDMENT_V1.md` | Amendment (product-direction) | `CAFA233D03C162A26C95C83B4C0279F6D813042C887CD29782C2C8004461FBB3` |
| `docs/GROUNDED_ANSWERS_AMENDMENT_V1.md` | Amendment (product-direction) | `3C6D753B26E4027D6F886F863FA3B626CC15B31B45B81FC6985E305244EFC1B9` |
| `docs/GROUNDED_ANSWER_PDF_SLICE_ACCEPTANCE.md` | Acceptance / slice spec | `E39CAC711ACA0C571E24A4C91777B879065DECE565DAF4DAC592F74A8DDD4E2F` |

## Immutable source copies

| Source document | Repository copy | SHA-256 |
|---|---|---|
| Original Memora PRD | `docs/product-source/Memora.docx` | `ECD5104E01FBC3066299AFDF83F2CDD7BA7A565725C3277EBB29B66AFDD9E195` |
| Local AI & Offline-First Revision | `docs/product-source/Addendum 1.docx` | `CEE42CAFE99595F2DED89BA5DA97529CC9A04F048714F55D7896A39577EAC89D` |
| Engineering Reference | `docs/product-source/Addendum 2 Engineering Reference.docx` | `F3D974F9DF654ED1242B1FD7A6BED0032C1CA960CB36280AB2A88F962D1CDD19` |

If a source document changes, preserve the previous copy, add the new version with
its SHA-256, and record the effect in `docs/DECISIONS.md` before implementation.
