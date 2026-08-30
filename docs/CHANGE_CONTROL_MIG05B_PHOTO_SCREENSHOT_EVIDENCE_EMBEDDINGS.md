# Change control — MIG-05 claim B slice 1 (photo + screenshot OCR evidence embeddings)

**Status:** Authorized — Checkpoint 1  
**Opened:** 2026-08-31  
**Authority:** `docs/ARCHITECTURAL_MIGRATION_SPEC_V1.md` MIG-05; ADR-050 claim **B**
(open); `docs/CHANGE_CONTROL_MIG05_EVIDENCE_EMBEDDING_STORE.md` non-PDF deferral.  
**Does not authorize:** Note-body evidence embeddings, MIG-05 Spec full close,
MIG-06/07/07B cutover, new Find paths, or AVAILABLE marketing.

## Intent

Index `MemoryEvidence` rows with kind `OCR_TEXT` for **PHOTO** and **SCREENSHOT**
Assets into `MemoryEvidenceEmbeddingStore`, mirroring the PDF page evidence
index path (`IndexPdfPageEmbeddings`).

## Architectural convergence

```
ARCHITECTURAL BOUNDARY: Meaning candidate generation (SearchAssetMemoriesByMeaning)
CURRENT LEGACY PATH: L8 (unchanged this slice)
TARGET PATH: Evidence-level vectors for image OCR blocks feed existing meaning search
WHY THIS CONVERGES: Closes ADR-050 claim B gap for photos/screenshots without new Find surface
WHAT OLD PATH WILL EVENTUALLY BE RETIRED: None this slice (L8 remains until MIG-07B Slice 3)
EXTENDS LEGACY? no
LEGACY SURFACE DELTA: unchanged
ESCAPE-HATCH AFTER CHANGE: unchanged (L7, L8)
```

## Scope (Checkpoint 1)

- `IndexOcrEvidenceEmbeddings` use case (evidence-only writer; fingerprint skip)
- `MemoryRepository.findOcrTextEvidenceForEmbedding` port + Room query
- Meaning-index build tap wires photo/screenshot OCR candidates after PDF pages
- Unit tests for indexer + repository mapping
- Progress / feedback copy for OCR evidence indexing

## Out of scope

- Note `NOTE_TEXT` evidence embeddings (separate slice)
- Chunking policy (FC-03)
- UI changes to Find-by-meaning screen beyond index feedback
- Claim **B** / Spec MIG-05 full close (notes remain open)

## Acceptance criteria

- READY photo/screenshot Memories with OCR evidence get embeddable rows on Build index tap
- Unresolved / blank OCR evidence counts as failed; never invents evidence ids
- Fingerprint skip when OCR text unchanged
- `SearchAssetMemoriesByMeaning` can rank OCR evidence hits when indexed (existing path)
- Claim **B** stays **OPEN** in CONTINUE until notes indexer or explicit ADR

## Verification

- `IndexOcrEvidenceEmbeddingsTest`
- Affected `AiPackDisclosureViewModel` tests if present
- `./gradlew :app:testDebugUnitTest`
