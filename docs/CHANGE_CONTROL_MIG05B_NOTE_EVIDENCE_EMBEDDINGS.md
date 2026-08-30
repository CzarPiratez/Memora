# Change control — MIG-05 claim B slice 2 (note text evidence embeddings)

**Status:** Delivered (2026-08-31)  
**Authority:** `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` MIG-05; ADR-050 claim **B**;
`CHANGE_CONTROL_MIG05B_PHOTO_SCREENSHOT_EVIDENCE_EMBEDDINGS.md` (slice 1).  
**Does not authorize:** MIG-05 Spec full close without founder checklist,
marketing AVAILABLE, new Find paths, or MIG-06/07 changes.

## Intent

Index `MemoryEvidence` rows with kind `NOTE_TEXT` for **NOTE** Assets into
`MemoryEvidenceEmbeddingStore`, mirroring photo/screenshot OCR slice 1.

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Meaning candidate generation (SearchAssetMemoriesByMeaning)
CURRENT LEGACY PATH: none (Canonical Recall; Live/Dual N=0)
TARGET PATH: Evidence-level vectors for note body text feed existing meaning search
WHY THIS CONVERGES: Closes ADR-050 claim B for notes without new Find surface
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: none
EXTENDS LEGACY? no
```

## Scope

- `MemoryRepository.findNoteTextEvidenceForEmbedding` + Room query
- Meaning-index build tap wires NOTE candidates after OCR slice
- Reuses `IndexOcrEvidenceEmbeddings` (evidence-only writer; fingerprint skip)
- Progress / feedback copy for note evidence indexing

## Acceptance criteria

- READY note Memories with NOTE_TEXT evidence get embeddable rows on Build index tap
- Unresolved / blank note evidence counts as failed; never invents evidence ids
- Fingerprint skip when note text unchanged
- `SearchAssetMemoriesByMeaning` can rank note evidence hits when indexed (existing path)
- ADR-050 claim **B** may be marked **COMPLETE** after verification + registry update

## Verification

- `./gradlew :app:testDebugUnitTest`
- Manual: Build meaning index with READY note Memory; confirm note evidence line in feedback
