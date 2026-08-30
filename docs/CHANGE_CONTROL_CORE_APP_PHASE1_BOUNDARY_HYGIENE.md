# Change control — Core / App Phase 1 boundary hygiene

**Status:** Complete (Phase 1 exit)  
**Opened:** 2026-08-30  
**Authority:** `docs/CORE_APP_SEPARATION_PLAN.md` Phase 1  
**Does not authorize:** Gradle module split, Class B, public Core source, MIG-*,
Freeze reopen, or repository surgery.

## Intent

Enforce `application` → `domain` (ports) instead of `application` → `data`
(concrete adapters) so Core use cases can compile without the App module.

## Delivered in this change control

1. Phase 1.1 import inventory recorded in `CORE_APP_SEPARATION_PLAN.md` §6.1.
2. CI guard `scripts/check-application-layer-boundaries.sh` (baseline allowlist;
   shrink-only).
3. **Slice 1:** Image-library access checks in five application use cases now
   use `ImageLibraryDiscoverySource.accessScope()` (domain port) instead of
   `data.mediastore.mediaStoreImageAccess`.
4. **Slice 2:** `domain/extraction/ImageExtractionReaders.kt`; three `RunPending*`
   extract use cases inject domain `*ExtractionPersistence` only (allowlist 13→10).
5. **Slice 3:** PDF ports (`PdfReadOnlyDescriptorAccess`, isolated session, persister);
   five PDF application paths clean (allowlist 10→5).
6. **Slice 4:** `UserConfirmedDerivedDataClearer`, `OnDeviceEmbeddingModelDownloader`;
   privacy + model download clean (allowlist 5→3, OneNote App bucket only).
7. **Step 1.4:** Domain purity CI guard `scripts/check-domain-layer-purity.sh`;
   wired in CI job `Core / App layer boundary guards`.

## Verification

- `./gradlew :app:testDebugUnitTest` (Phase 1 affected unit tests) — PASS
- `bash scripts/check-application-layer-boundaries.sh` — PASS (3/3 OneNote)
- `bash scripts/check-domain-layer-purity.sh` — PASS
- `bash scripts/check-legacy-recall-surface.sh` (unchanged recall surface)

## Remaining Phase 1 debt (allowlisted)

See inventory table in separation plan. OneNote (3 files) remains intentional App-only allowlist.
