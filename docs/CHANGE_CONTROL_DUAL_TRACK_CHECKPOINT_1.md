# Change control — Dual-track Checkpoint 1 (recall + multimodal meaning)

**Status:** Authorized — Checkpoint 1 only  
**Opened:** 2026-08-31  
**Authority:** Lead engineering sequencing after Phase 1 Core exit;
`docs/FUTURE_CAPABILITY_BACKLOG.md` suggested sequencing.  
**Does not authorize:** MIG-07B Slices 2–3, MIG-05 claim B full close (notes),
Phase 2 Gradle, Grounded Answers, marketing AVAILABLE, or Freeze reopen.

## Intent

Ship two parallel slices in one verified checkpoint:

| Track | Slice | Change control |
|---|---|---|
| **A — Product** | Photo + screenshot OCR evidence embedding index drain | `CHANGE_CONTROL_MIG05B_PHOTO_SCREENSHOT_EVIDENCE_EMBEDDINGS.md` |
| **B — Architecture** | Anchor classifier + structured filter (domain only) | `CHANGE_CONTROL_MIG07B_ANCHOR_AWARE_RECALL.md` Slice 1 |

## Sequencing after Checkpoint 1

1. **Checkpoint 2:** MIG-07B Slice 2 — fold meaning into `CanonicalRecall` (no L8 retirement yet)
2. **Checkpoint 3:** FC-04 corpus honesty UI (indexed / pending counts)
3. **Checkpoint 4:** MIG-07B Slice 3 — L8 retirement; Live/Dual N → 0
4. **Defer:** Phase 2 Gradle until recall convergence nearer DONE

## Verification (Checkpoint 1)

- `./gradlew :app:testDebugUnitTest` (new + affected tests)
- `scripts/check-application-layer-boundaries.sh` — PASS
- `scripts/check-domain-layer-purity.sh` — PASS
- `scripts/check-legacy-recall-surface.sh` — N unchanged (2)
- ADR-050 claim **B** remains **OPEN** (photo/screenshot only; notes deferred)

## Truthfulness

Checkpoint 1 improves multimodal meaning index coverage and lands recall filter
**contracts** only. Does not claim Recall DONE, MIG-05 Spec full, or hybrid Find UI.
