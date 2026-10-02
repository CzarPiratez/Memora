# UNFYND — Index Fabric Current-State Audit Report

This report provides a strict, read-only engineering audit of the current UNFYND Android codebase (`MemoraApp/`), evaluating the actual implementation status of the Index Fabric against the v3.1 architectural requirements.

---

## 1. Index State Inventory

| File | Class / Table | Purpose | Fields | Who Writes It | Who Reads It | Lifecycle | Persisted / In-Memory | Assessment |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `DiscoveryCheckpointEntity.kt` | `discovery_checkpoints` table | Resumes discovery scans | `source_id`, `cursor`, `updated_at` | Discovery sources / workers | Discovery workers | Permanent until reset | Persisted (Room) | **PARTIAL** (Source-level cursor only) |
| `AssetEntity.kt` | `assets` table | Tracks atomic assets & indexing status | `source_id`, `source_asset_key`, `asset_type`, `location`, `fingerprint`, `indexing_status`, `indexing_attempt_count`, `failure_code`, `failure_message` | Discovery & indexing workers / extractors | UI, workers, completion loaders | Atomic file lifetime | Persisted (Room) | **PARTIAL** (Basic status enum; lacks full v3.1 stage matrix) |
| `MemoryEntity.kt` | `memories` table | Stores memory revisions & readiness | `memory_id`, `revision_id`, `status` (READY, STALE_REINDEX_REQUIRED), `assembly_schema_version`, `model_id`, `model_version` | Assembly & migration use cases | Search & readiness loaders | Memory lifecycle | Persisted (Room) | **PARTIAL** (Ready/Stale states present; lacks stage-specific states) |
| `AiPackInstallLedgerEntity.kt` | `ai_pack_install_ledger` table | Tracks AI pack installation state | `track_id`, `state`, `model_id`, `version`, `installed_at_epoch_ms` | AI pack manager / ledger | Setup UI, download workers | App lifetime | Persisted (Room) | **IMPLEMENTED** |

---

## 2. Worker Pipeline

UNFYND utilizes WorkManager workers for asynchronous background execution:
* **Workers**: `MediaStoreDiscoveryWorker`, `SafPdfDiscoveryWorker`, `SafPdfExtractWorker`, `MediaStorePhotoOcrExtractWorkScheduler` (and OCR workers), `AssetMemoryAssemblyWorker`, `MeaningIndexWorker`.
* **Triggers**: Periodic or one-off WorkManager constraints (unmetered network, charging, or device idle depending on task).
* **Idempotency**: Handled primarily via content fingerprints (`Asset.fingerprint`), revision tracking, and UPSERT semantics in Room DAOs (ignoring unchanged assets).
* **Process Death**: WorkManager durable job queues ensure that if Android kills the process mid-worker, WorkManager automatically retries or resumes pending work based on backoff policies.

---

## 3. Asset Versioning
* **Identification**: Logical assets are identified by `AssetIdentity(sourceId, sourceAssetKey)`.
* **Content Versioning**: Captured via `AssetFingerprint` (combining source ID, media ID / document ID, generation counter, and size bytes).
* **Guarantees**: If `AssetFingerprint` is unchanged, extraction and indexing stages skip duplicate processing. If content changes (e.g., generation or size changes), a new fingerprint is emitted, marking the asset/memory as stale or triggering re-extraction.

---

## 4. Idempotency

| Stage | Idempotency Behavior | Key Used to Determine Completion |
| :--- | :--- | :--- |
| **EXIF Extraction** | **A. No work** (skips if extraction record exists for asset fingerprint) | `source_id` + `source_asset_key` |
| **OCR (Photo/Screenshot)** | **A. No work** (skips if OCR record exists for asset fingerprint) | `source_id` + `source_asset_key` |
| **PDF Extraction** | **A. No work** (skips if PDF page extraction exists for fingerprint) | `source_id` + `source_asset_key` |
| **Memory Assembly** | **D. New revision** (assembles new revision if extraction facts change) | `memory_id` + `assembly_schema_version` |
| **Embedding** | **A. No work** (skips if vector exists in `memory_embeddings` for `revision_id` + `model_id` + `model_version`) | `revision_id` + `model_id` + `model_version` |
| **Reranking / Indexing** | **A. No work** (stateless ranking over retrieved candidate pool) | Query string + candidate list |

---

## 5. Version / Derivation Identity Matrix

| Stage | Input Identity | Algorithm Identity | Model Identity | Config Identity | Output Identity |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Discovery** | ABSENT | ABSENT | ABSENT | ABSENT | IMPLEMENTED (`AssetIdentity`) |
| **EXIF Extraction** | IMPLEMENTED | ABSENT | ABSENT | ABSENT | IMPLEMENTED (`ImageExifExtractionRecord`) |
| **OCR** | IMPLEMENTED | IMPLEMENTED (`engine_id`) | IMPLEMENTED (`engine_version`) | ABSENT | IMPLEMENTED (`PhotoOcrExtractionRecord`) |
| **PDF Extraction** | IMPLEMENTED | IMPLEMENTED (`extraction_schema_version`) | ABSENT | ABSENT | IMPLEMENTED (`PdfExtractionRecord`) |
| **Memory Assembly** | IMPLEMENTED | IMPLEMENTED (`assembly_schema_version`) | ABSENT | ABSENT | IMPLEMENTED (`MemoryRevisionId`) |
| **Embedding** | IMPLEMENTED | ABSENT | IMPLEMENTED (`ModelVersionIdentity`) | ABSENT | IMPLEMENTED (`MemoryEmbeddingRecord`) |

---

## 6. Invalidation
* **Source File Changes**: Detected when `AssetFingerprint` changes during discovery.
* **Model Changes**: When embedding model changes (e.g., USE to BGE-small), `deleteForModel` purges vectors belonging to the retired model ID/version.
* **Extraction Algorithm Changes**: Triggered via `assembly_schema_version` mismatch, marking older memories as `STALE_REINDEX_REQUIRED`.
* **Source Disappearance / Permission Revocation**: Handled via `DiscoveryResult.AccessRevoked` or source availability observation states.

---

## 7. Failure Model
* **Retryable Failures**: `PhotoOcrReadResult.RetryableFailure`, `IsolatedPdfParserService` retryable failure wire outcomes.
* **Permanent / Access Failures**: `DiscoveryResult.AccessRevoked`, `DiscoveryResult.AccessRequired`.
* **Model Unavailable**: `CapabilityAvailability.Unavailable(reason)` returned by embedding and intelligence engines.
* **Unrepresented Failures**: Certain transient memory assembly and JSON parsing failures currently log warnings (`Log.w`) rather than maintaining persistent error state per asset.

---

## 8. Process-Death & Reboot Recovery
* **Process Death**: WorkManager durable task persistence ensures pending background workers resume automatically upon process restart.
* **Reboot**: WorkManager broadcast receivers re-register scheduled workers with system AlarmManager/JobScheduler upon device boot.

---

## 9. Deletion & Source Disappearance
* **Code Paths**: `ClearMemoraDerivedData` and source availability stores handle revoked permissions and data clearing. When a source file disappears from MediaStore/SAF, discovery source cursors omit it on subsequent scans.

---

## 10. Index Health
* **Corpus Completeness**: Loaded via `LoadCorpusCompleteness.kt`, which computes `CorpusCompletenessSnapshot` containing ready memories, pending assembly counts, meaning summary/evidence vector counts, and blocked status.

---

## 11. Resource Governance
* **WorkManager Constraints**: Workers declare strict constraints via `WorkManagerModule` (unmetered network, battery not low, charging) to respect device battery and thermal state.

---

## 12. Final Gap Matrix

| Capability | IMPLEMENTED | PARTIAL | ABSENT | Evidence | Risk | Recommended action |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Stage state** | | **X** | | `AssetEntity.indexingStatus` (READY/FAILED) | Lacks fine-grained stage states (QUEUED, RUNNING, STALE) | Add explicit stage state machine per pipeline stage |
| **Asset version** | **X** | | | `AssetFingerprint` | None | Keep as is |
| **Idempotency** | **X** | | | DAO UPSERTs and fingerprint checks | None | Keep as is |
| **Checkpoints** | | **X** | | `DiscoveryCheckpointEntity` | Source-level only; lacks stage checkpoints | Add per-stage indexing checkpoints |
| **Retry** | | **X** | | WorkManager backoff + retry enums | Limited manual retry triggers | Expose retry controls in setup UI |
| **Failure taxonomy** | | **X** | | `IndexingFailure`, retryable enums | Lacks granular taxonomy across all workers | Standardize failure enums |
| **Derivation identity** | | **X** | | `assembly_schema_version`, engine versions | Partial config tracking | Full pipeline dependency graph |
| **Model identity** | **X** | | | `ModelVersionIdentity` in embedding tables | None | Keep as is |
| **Dependency invalidation** | | **X** | | `deleteForModel`, `STALE_REINDEX_REQUIRED` | Manual trigger required | Automated dependency propagation |
| **Deletion handling** | | **X** | | Source availability stores | Orphaned derived rows possible | Implement cascade cleanup on source deletion |
| **Permission handling** | **X** | | | `DocumentTreeAccessValidator`, `accessState()` | None | Keep as is |
| **Process recovery** | **X** | | | WorkManager durability | None | Keep as is |
| **Reboot recovery** | **X** | | | WorkManager boot broadcast receivers | None | Keep as is |
| **Index health** | **X** | | | `LoadCorpusCompleteness` | None | Keep as is |
| **Resource governance** | **X** | | | WorkManager constraints | None | Keep as is |
| **Incremental reconciliation** | | **X** | | Fingerprint diffing | Relies on periodic discovery scans | Event-driven incremental watcher |

---

## 13. Strategic Recommendations

* **A. What we should KEEP**: WorkManager durable job scheduling, `AssetFingerprint` content hashing, `deleteForModel` model version isolation, and `LoadCorpusCompleteness` health honesty.
* **B. What needs only a small modification**: Expand WorkManager failure classification to persist granular error reasons directly in `AssetEntity`.
* **C. What genuinely needs a new implementation**: A generalized dependency-aware invalidation graph for automatic cascading reindexing when upstream extraction models or schemas change.
* **D. What should NOT be built yet**: Complex distributed streaming pipelines or cloud-sync invalidation protocols.
* **E. Smallest P0 production-safe implementation**: Ensure persistent recording of failed asset extraction attempts with user-visible retry actions in the setup UI.
