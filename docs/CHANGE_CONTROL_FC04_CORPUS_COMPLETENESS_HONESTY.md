# Change control — FC-04 corpus completeness honesty UI

**Status:** Delivered  
**Opened:** 2026-08-31  
**Authority:** `docs/FUTURE_CAPABILITY_BACKLOG.md` FC-04; Dual-track Checkpoint 1
sequencing.  
**Does not authorize:** new Find paths, Recall DONE, marketing AVAILABLE,
or MIG-05 claim B (notes).

## Intent

Show honest **indexed / pending / blocked** counts for the meaning-search corpus
on device — extending the per-screen readiness pattern to a unified snapshot.

## Architectural boundary

| Field | Value |
|---|---|
| **ARCHITECTURAL BOUNDARY** | App UX honesty (no new retrieval path) |
| **CURRENT LEGACY PATH** | none |
| **TARGET PATH** | `LoadCorpusCompleteness` → setup + meaning Find screens |
| **EXTENDS LEGACY?** | No |

## Delivered

- `CorpusCompletenessCounts` / `CorpusCompletenessSnapshot` (domain)
- `LoadCorpusCompleteness` use case
- `AssetMemoryFactDao.countPendingAssembly` (+ port method)
- `CorpusHonestyCopy` shared UI formatter
- Meaning Find readiness line (`MeaningSearchCopy.readinessBody`)
- AI Pack disclosure "Corpus on this phone" section when model ready
- Asset Memory setup pending-assembly line

## Verification

- `LoadCorpusCompletenessTest`, `CorpusHonestyCopyTest`, `MeaningSearchCopyTest`
- `./gradlew :app:testDebugUnitTest` (affected packages)

## Truthfulness

Indexed/pending/blocked counts reflect Memora-owned Room state on this phone
only. Not a measured AVAILABLE claim. Does not close `RECALL_CONVERGENCE_DONE`.
