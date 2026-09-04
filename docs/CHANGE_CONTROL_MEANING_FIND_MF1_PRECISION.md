# Change control: Meaning Find MF-1 — cue precision + trusted list

**Date:** 2026-09-04  
**Type:** Domain + application + UI copy (Canonical Recall meaning path)  
**Bar:** `docs/MEANING_FIND_PRODUCT_SCENARIO_BAR.md` (v2 Intent Register
supersedes v1 as ceiling; MF-1 still maps to U1–U8 / I1 partial)  
**Decision guardrails:** Live/Dual N = 0. No second Find path. No AVAILABLE.
No ADR-052 UI. Innovation within Canonical Recall.

## Pre-work record

- **Requirement IDs:** Meaning Find product bar U1–U4, U6, T1–T6; charter
  holistic scenario planning.
- **Source documents read:** ENGINEERING_CHARTER, GOVERNANCE, CONTINUE,
  RECALL_ENFORCEMENT_INDEX, MEANING_FIND_PRODUCT_SCENARIO_BAR, FC-02 Stage A
  wire (CE stays after precision).
- **Current-code evidence inspected:** SearchAssetMemoriesByMeaning embeds raw
  trim only; lexical shouldApply ≥2; token boost ignores stopwords;
  MeaningSearchCopy forces top-10 framing.
- **Privacy:** On-device only; no new network.
- **Smallest safe change:** `MeaningRecallCue`; lexical ≥1; boost uses content
  tokens; trusted short list; embed content cue; tests; copy aligned to bar.
- **Acceptance criteria:**
  - [x] `silky` drops hits whose evidence lacks `silky`
  - [x] `which file has silky in it?` embeds content cue `silky` (not empty class)
  - [x] Token boost does not treat `which`/`file`/`has` as cue words
  - [x] Result list ≤ trusted band (not forced noise to 10)
  - [x] Unit tests encode U1/U2/T1/T5
  - [ ] Device MF-3 on SM-A156E (`silky` / NL question)

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Canonical Recall meaning path (ADR-049)
CURRENT LEGACY PATH (L# or none): none (N = 0)
TARGET PATH: MeaningRecallCue → SearchAssetMemoriesByMeaning →
  AnchorAwareMeaningRecallRanking (precision → CE) → consumer UI
WHY THIS CONVERGES: Product-grade cue + precision inside sole Find boundary
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: raw-embed + top-10 soft neighbors UX
EXTENDS LEGACY? no
```

## Delivery record

- **Files:** `MeaningRecallCue`; lexical ≥1; token boost stopwords;
  `MeaningTrustedHitPolicy` + `CanonicalRecall` trim; meaning Why dialect;
  friendly card labels; short Open hints; unit tests (cue, lexical, boost,
  trusted list, ranking silky/NL, Why/copy).
- **Bar IDs:** U1–U4, U5 (Why), U6, T1–T8 (partial device pending MF-3).
- **Automated verification:** Founder runs JVM unit tests in Android Studio for
  `MeaningRecallCue*`, `MeaningEvidenceLexical*`, `MeaningEvidenceTokenBoost*`,
  `MeaningTrustedHitPolicy*`, `AnchorAwareMeaningRecallRanking*`,
  `CanonicalRecallWhyCopy*`, `MeaningSearchCopy*`.
- **Device verification:** MF-3 on SM-A156E after sync (`silky`,
  `which file has silky in it?`).
- **Known limitation:** Pure semantic cues with zero content tokens still use
  soft meaning + relative band only; cross-lingual without shared tokens deferred.
- **Git commit:** When requested
