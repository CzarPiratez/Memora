# Local-AI Compatibility and Fallback Policy

**Status:** Accepted for Local-AI architecture gate planning (2026-07-25).  
**Requirements:** A-01, A-02, A-03, A-07; Local AI Technical Spec §6, §10–§13.  
**Governing decisions:** ADR-012, ADR-023, ADR-024.  
**Does not authorize:** model downloads, OCR/embedding SDKs, network permission,
or any claim that on-device understanding is ready on any device tier.

## Purpose

Define which device/capability combinations Memora may treat as supported, what
honest outcomes unsupported devices receive, and which fallbacks are forbidden.
This satisfies Spec §13.4 and the Spec §11 requirement for a supported-device /
capability matrix with honest fallback copy — before any AI implementation.

## Support tiers (planning matrix)

Support is evaluated **per Spec §4 capability**, not as a single “AI works” bit.
A device may support keyword PDF search while every intelligence capability remains
unavailable.

| Tier | Meaning | User-visible outcome |
|---|---|---|
| `SUPPORTED` | Verified pack (or approved system runtime) can run for this capability on this device class | Capability may report AVAILABLE only after pack/runtime verification |
| `DEGRADED_EXPLICIT` | A documented, narrower local path exists and is disclosed | UI must name the limitation; never imply full semantic understanding |
| `UNSUPPORTED` | No approved pack/runtime path | Capability reports UNAVAILABLE with plain reason; no hidden substitute |

Representative device classes for later measurement (not release claims):

| Device class | Planning role |
|---|---|
| `emulator_medium_phone` | Development / CI smoke host |
| `midrange_arm64` | Primary physical MVP target class |
| `low_ram` | Must not claim heavy packs; prefer UNAVAILABLE over thrash |
| `unsupported_abi` | Always UNAVAILABLE for packs that lack matching assets |

Exact ABI/API/RAM cut lines are filled when a pack is chosen and benchmarks exist.
Until then, **every intelligence capability defaults to UNSUPPORTED** in product
copy and domain stubs.

## Allowed fallbacks

1. **Unavailable stays unavailable.** If vision/OCR/document/embedding/memory-builder/
   recall-ranker is not installed or not compatible, report UNAVAILABLE and keep
   non-AI product paths that already work (for example keyword search of saved PDF
   text) clearly labeled as not meaning-based.
2. **Prior known-good pack.** After a failed update verification, retain the prior
   active pack when present (ADR-023). That is rollback, not a silent feature swap.
3. **System local runtime.** May satisfy a capability only when compatibility is
   documented for that capability and device class; otherwise UNAVAILABLE.
4. **Deterministic extraction without understanding.** PDF text / metadata extraction
   may succeed while Memory understanding remains unavailable. Do not rebrand
   extraction as semantic Memory.

## Forbidden fallbacks (Spec §12)

- Silent fallback from semantic understanding to filename-only search.
- Silent fallback from semantic recall to keyword search without labeling the path.
- Claiming AVAILABLE on an unsupported device or when pack verification failed.
- Inventing Explain Mode reasons when no stored evidence exists.
- Shipping a “works everywhere” claim without a measured matrix row.

## Honest copy principles

User-facing language must be recognition-first and must not expose SDK jargon.

| Situation | Direction for copy |
|---|---|
| Pack not installed | Say on-device intelligence is not installed yet; offer install only when a download slice exists |
| Device unsupported | Say this phone cannot run that on-device feature; do not imply a cloud substitute in core |
| Keyword search only | Say matches are based on saved text, not meaning |
| Capability paused / failed | Say paused or could not finish; keep retry honest |

Exact UI strings are owned by later UI slices; this policy constrains their meaning.

## Domain contract

`CapabilitySupportDecision` and related types in
`com.memora.app.domain.intelligence` encode one matrix row: capability + device
class + tier + reason. Resolvers must not upgrade UNSUPPORTED to SUPPORTED without
an explicit verified pack/runtime binding.

Default resolver: all Spec §4 capabilities are UNSUPPORTED with a stable reason
until a later adapter supplies measured rows.

## Relationship to other gate documents

| Document | Boundary |
|---|---|
| `AI_PACK_DELIVERY_SECURITY_PLAN.md` | How packs are disclosed, verified, rolled back |
| This policy | Whether a device/capability may be supported and how fallbacks behave |
| Local-AI benchmark plan (pending) | How support claims are evidenced with measurements |

## Acceptance for this policy slice

1. This document is recorded and linked from ROADMAP / CONTINUE / CHANGELOG / ADR.
2. Domain support-decision types and a default unsupported resolver are unit-tested.
3. No AI/OCR/network dependency lands in the same change.
4. Traceability notes progress without marking A-01 done or gate exit complete.

## Emulator draft notes (measured baselines L2 — 2026-08-02)

On `emulator_medium_phone`, synthetic pack **integrity/size** baselines exist
(`docs/CHANGE_CONTROL_LOCAL_AI_MEASURED_PACK_BASELINES.md` L1–L2). That evidence
does **not** change support tiers by itself:

| Capability (all Spec §4) | Device class | Tier | Reason (draft) |
|---|---|---|---|
| VISION / OCR / DOCUMENT / MEMORY_BUILDER | `EMULATOR_MEDIUM_PHONE` | `UNSUPPORTED` | Integrity harness only; no verified pack/runtime bound |
| EMBEDDING / RECALL_RANKER | `EMULATOR_MEDIUM_PHONE` | `DEGRADED_EXPLICIT` | USE + candidate Find-by-meaning measured on emulator (M3); **not** marketing AVAILABLE; E5d assist may apply |
| VISION / OCR / DOCUMENT / MEMORY_BUILDER | `MIDRANGE_ARM64` | `UNSUPPORTED` (pending) | No midrange measured pack/runtime row for these capabilities yet |
| EMBEDDING / RECALL_RANKER | `MIDRANGE_ARM64` | `DEGRADED_EXPLICIT` | USE page-recall measured on Galaxy A15 (M4): cosine **2/3** / boosted **3/3**; candidate copy only — **not** marketing AVAILABLE; E5d assist retained — see `docs/CHANGE_CONTROL_MIDRANGE_MEANING_MEASUREMENT_GATE.md` |

Keyword recall remains an allowed non-AI path and must stay labeled as not
meaning-based (`SemanticFallbackRules`).

## Follow-up for Local-AI gate exit

1. Local-AI benchmark plan (privacy-safe fixtures + quality/perf metrics) — accepted.
2. When a first pack is chosen: fill concrete ABI/API/RAM rows and DEGRADED paths.
3. UI copy review against this policy before any AVAILABLE claim ships.
4. M4 midrange meaning page-recall measurement executed (2026-08-29). Product
   AVAILABLE decision + UI copy review still required before midrange AVAILABLE.
