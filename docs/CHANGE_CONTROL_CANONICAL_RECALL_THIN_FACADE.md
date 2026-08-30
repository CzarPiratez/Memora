# Change control: Canonical Recall thin façade (KEYWORD path)

**Date:** 2026-08-30  
**Type:** Application Find boundary (thin façade)  
**Decision guardrails:** Land a **thin** `CanonicalRecall` application API that
is the product-facing entry for keyword Finds already on Memory evidence.
Delegate KEYWORD candidate generation to `SearchMemoryEvidence`. Wire PDF /
screenshot / photo / note ViewModels through `CanonicalRecall` only. Do **not**
start MIG-07B, fold L7/L8, implement RecallRanker, Grounded Answers, Vision,
UI redesign, or claim `RECALL_CONVERGENCE_DONE`. Leave unrelated dirty files
unstaged. No push.

## Lead decisions (LOCKED)

1. **`CanonicalRecall`** = sole App product-facing retrieval entry for the
   four Memory-evidence keyword Find surfaces (ADR-049 naming made real for
   KEYWORD path).
2. **`SearchMemoryEvidence`** remains keyword/literal **candidate generation**
   inside that boundary (not a UI-facing Find system).
3. **Return shape:** reuse `MemoryEvidenceSearchOutcome` /
   `MemoryEvidenceSearchHit` (already aligned with DRAFT result contract fields
   for KEYWORD). No UI model redesign.
4. **Adapters** unchanged (still map MemoryEvidence outcomes → per-asset UI).
5. **CI guard:** allow `CanonicalRecall` in application + the four ViewModels;
   **forbid** `SearchMemoryEvidence` under `ui/**`.
6. **LEGACY:** N stays **2** (L7, L8). No new legacy row. Keyword paths are no
   longer escape hatches; L7/L8 still are.
7. **Not** MIG-07B / meaning merge / shared ranking / AVAILABLE.

## Pre-work record

- **Requirement IDs:** ADR-049; Architecture Freeze §3; Migration Spec MIG-07
  post-cutover boundary; Local AI Spec retrieval honesty.
- **Source documents read:** `AGENTS.md`, `PRODUCT_SOURCE_REGISTRY`,
  `GOVERNANCE`, `CONTINUE`, `LEGACY_RECALL_SURFACE`, `RECALL_ENFORCEMENT_INDEX`,
  `CANONICAL_RECALL_RESULT_CONTRACT`, ADR-049, CI guard script, four keyword
  ViewModels, `SearchMemoryEvidence`.
- **Current-code evidence inspected:** ViewModels inject `SearchMemoryEvidence`
  directly; CI section C forbids `CanonicalRecall` in main; section D allowlists
  UI binding to `SearchMemoryEvidence`.
- **Open ADRs / platform limitations checked:** ADR-049 naming; MIG-07B not
  authorized; ADR-050 B open; Act out.
- **Privacy / offline:** No new network, permissions, or cloud AI. Reads stored
  Memory evidence only.
- **Smallest safe change:** façade class + ViewModel inject swap + CI + docs +
  unit test.
- **Acceptance criteria:** ViewModels call `CanonicalRecall`; unit test proves
  delegation; legacy guard PASS; assembleDebug; N=2 unchanged; no MIG-07B claim.
- **Test plan:** `CanonicalRecallTest`; existing SearchMemoryEvidence tests
  remain; `:app:testDebugUnitTest` focused; `:app:assembleDebug`; bash legacy
  guard.
- **UX:** No UI redesign; same hit cards / Why adapters.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: App product Find entry (application + ui/search keyword VMs)
CURRENT LEGACY PATH (L# or none): none for L1–L4 (Retired); UI still called SearchMemoryEvidence directly (candidate gen without named Canonical Recall API)
TARGET PATH: CanonicalRecall (App Find) → SearchMemoryEvidence (KEYWORD candidate gen) → adapters → existing UI
WHY THIS CHANGE CONVERGES: makes ADR-049 Canonical Recall a live thin App API for keyword Memory-evidence Finds; UI no longer binds candidate-gen use case directly
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: L7/L8 (meaning + local ranking) when MIG-07B folds them into Canonical Recall
EXTENDS LEGACY? no
IF YES — exception ID / ADR + sunset: N/A
LEGACY SURFACE DELTA: unchanged (N=2; L7, L8 Live)
ESCAPE-HATCH AFTER CHANGE: YES — L7, L8 still produce hits without going through CanonicalRecall; keyword PDF/screenshot/photo/note Finds go through CanonicalRecall
```

## Delivery record

- **Files/layers:** `CanonicalRecall.kt` + test; four keyword ViewModels;
  `SearchMemoryEvidence` KDoc; CI `check-legacy-recall-surface.sh`;
  GOVERNANCE auth; LEGACY / RECALL_ENFORCEMENT_INDEX /
  CANONICAL_RECALL_RESULT_CONTRACT; CONTINUE / CHANGELOG; Cursor invariants.
- **Automated verification and result:** `scripts/check-legacy-recall-surface.ps1`
  PASS (A–D). `:app:testDebugUnitTest` CanonicalRecallTest +
  SearchMemoryEvidenceTest PASS. `:app:assembleDebug` SUCCESS.
- **Emulator/manual verification and result:** Not required for façade inject
  swap (same adapters/outcomes); unit + assemble + guard.
- **Failure/recovery paths verified:** Blank / nothing-saved still from
  SearchMemoryEvidence via façade (covered by SearchMemoryEvidenceTest +
  CanonicalRecallTest blank case).
- **Known limitation or follow-up:** L7/L8 outside façade until MIG-07B;
  MEANING path not in CanonicalRecall yet; no shared ranking.
- **Documentation/traceability/ADR updates:** this file; CONTINUE; CHANGELOG;
  enforcement docs; GOVERNANCE auth; Cursor invariants. ADR-049 substance
  unchanged (naming); delivery realizes thin KEYWORD API.
- **Git commit:** (see local commit after this delivery)
