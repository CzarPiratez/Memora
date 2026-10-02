# UNFYND Codebase Factual Inventory

This document provides an exhaustive, factual inventory of what is actually implemented in the UNFYND Android codebase (`MemoraApp/`), inspected directly from source code without inference from filenames or documentation.

---

## 1. Project Structure
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/`
* **Main Class/Interface**: Single-module Android application (`:app`) organized into Clean Architecture layers (`domain`, `application`, `data`, `ui`).
* **What it Actually Does**: Houses the entire UNFYND Android app, including dependency injection (Hilt), Room database, WorkManager background indexing, ONNX/ML Kit AI inference, and Jetpack Compose presentation.
* **Storage Used**: App-local SQLite / Room database (`memora.db`), no-backup files directory (`noBackupFilesDir/memora_embedding_models/`).
* **Current Status**: **IMPLEMENTED**

## 2. AndroidManifest and Permissions
* **File Path**: `MemoraApp/app/src/main/AndroidManifest.xml`
* **Main Class/Interface**: Android Manifest declaration (`AndroidManifest.xml`)
* **What it Actually Does**: Declares app metadata, entry points (`MainActivity`), isolated sandboxed PDF parser service (`IsolatedPdfParserService`), MSAL browser redirect activity (`BrowserTabActivity`), and runtime permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE`, `INTERNET`, `ACCESS_NETWORK_STATE`).
* **Storage Used**: N/A
* **Current Status**: **IMPLEMENTED**

## 3. Gradle / Dependencies
* **File Path**: `MemoraApp/app/build.gradle.kts`
* **Main Class/Interface**: Gradle build script (`build.gradle.kts`)
* **What it Actually Does**: Configures compileSdk 36, targetSdk 36, minSdk 26, Jetpack Compose BOM, Room, Hilt, WorkManager, PDFBox-Android, BouncyCastle, OnnxRuntime Android, SQLCipher, ML Kit Text Recognition, MediaPipe Tasks Text, and MSAL Android.
* **Storage Used**: N/A
* **Current Status**: **IMPLEMENTED**

## 4. Room Database / Entities / DAOs / Migrations
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/local/`
* **Main Class/Interface**: `MemoraDatabase`, `AssetEntity`, `MemoryEntities`, `MemoryEmbeddingEntity`, `AssetDao`, `MemoryDao`, `MemoraDatabaseMigrations`
* **What it Actually Does**: Defines relational Room tables for assets, memory facts, evidence, summary embeddings, evidence embeddings, and migration scripts (Mig01 through Mig05).
* **Storage Used**: SQLite / Room (`memora.db`)
* **Current Status**: **IMPLEMENTED**

## 5. MediaStore / File Discovery
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/mediastore/` and `saf/`
* **Main Class/Interface**: `MediaStoreImageDiscoverySource`, `MediaStoreImageMapper`, `SafPdfDiscoverySource`
* **What it Actually Does**: Queries Android's shared image catalogue (`MediaStore.Images.Media`) for photos and screenshots, and traverses user-approved Storage Access Framework (SAF) folder trees for PDFs.
* **Storage Used**: Read-only MediaStore cursors and SAF document trees.
* **Current Status**: **IMPLEMENTED** (Photos, Screenshots, and SAF PDFs; Video and Audio discovery not implemented).

## 6. Indexing and WorkManager
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/work/` and `UnfyndApplication.kt`
* **Main Class/Interface**: `UnfyndApplication`, `*Worker.kt`, `*Scheduler.kt`
* **What it Actually Does**: Wires Hilt WorkManager initialization (`HiltWorkerFactory`) and schedules background discovery, OCR extraction, PDF parsing, memory assembly, and meaning index generation workers.
* **Storage Used**: WorkManager internal SQLite database.
* **Current Status**: **IMPLEMENTED**

## 7. Metadata Extraction
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/mediastore/ContentResolverImageExifReader.kt`
* **Main Class/Interface**: `ContentResolverImageExifReader`
* **What it Actually Does**: Extracts EXIF metadata (camera model, capture date, orientation, location headers) from image Uris using `ExifInterface`.
* **Storage Used**: Room (`image_exif_extractions`)
* **Current Status**: **IMPLEMENTED**

## 8. OCR
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/mediastore/MlKitPhotoOcrReader.kt` & `MlKitScreenshotOcrReader.kt`
* **Main Class/Interface**: `MlKitPhotoOcrReader`, `MlKitScreenshotOcrReader`
* **What it Actually Does**: Performs on-device text recognition on photos and screenshots using Google ML Kit Text Recognition (Latin bundled).
* **Storage Used**: Room (`photo_ocr_extractions`, `screenshot_ocr_extractions`)
* **Current Status**: **IMPLEMENTED**

## 9. PDF Extraction
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/pdfbox/isolation/`
* **Main Class/Interface**: `IsolatedPdfParserService`, `IIsolatedPdfParser.aidl`, `PdfBoxPdfDocumentParser`
* **What it Actually Does**: Parses PDF text securely inside an isolated Android service process (`android:isolatedProcess="true"`) using PDFBox-Android and IPC streaming via AIDL.
* **Storage Used**: Room (`saved_pdf_page_texts`)
* **Current Status**: **IMPLEMENTED**

## 10. Image / Video / Audio Processing
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/mediastore/`
* **Main Class/Interface**: MediaStore image mappers and thumbnail loaders.
* **What it Actually Does**: Processes images and screenshots. Video and audio file discovery, transcription, or processing pipelines are absent.
* **Storage Used**: MediaStore
* **Current Status**: **PARTIAL** (Images/Photos/Screenshots fully implemented; Video/Audio processing not found).

## 11. Embedding / Meaning Models
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/intelligence/OnnxBgeSmallEmbeddingEngine.kt`
* **Main Class/Interface**: `OnnxBgeSmallEmbeddingEngine`, `EmbeddingEngine`
* **What it Actually Does**: Encodes memory text and queries into 384-dimensional semantic vectors using BGE-small-en-v1.5 ONNX runtime. Enforces asymmetric query prefix (`QUERY_PREFIX`) for search cues and raw text for documents.
* **Storage Used**: ONNX Runtime memory session / asset vocab (`bert_vocab.txt`)
* **Current Status**: **IMPLEMENTED**

## 12. Model Download and Installation
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/intelligence/HttpOnDeviceEmbeddingModelDownloader.kt`
* **Main Class/Interface**: `HttpOnDeviceEmbeddingModelDownloader`, `NoBackupOnDeviceEmbeddingModelStore`
* **What it Actually Does**: Downloads BGE-small ONNX model weights from HuggingFace, performs stream-wise SHA-256 integrity verification against pinned hash, stores in no-backup directory, and purges legacy USE model files.
* **Storage Used**: `noBackupFilesDir/memora_embedding_models/`
* **Current Status**: **IMPLEMENTED**

## 13. Vector Storage
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/local/RoomMemoryEmbeddingStore.kt` & `RoomMemoryEvidenceEmbeddingStore.kt`
* **Main Class/Interface**: `RoomMemoryEmbeddingStore`, `RoomMemoryEvidenceEmbeddingStore`, `deleteForModel`
* **Storage Used**: Room (`memory_embeddings`, `memory_evidence_embeddings`)
* **What it Actually Does**: Persists summary and evidence-level vectors tied strictly to `ModelVersionIdentity`, supporting upsert, list, count, and model-specific deletion (`deleteForModel`).
* **Current Status**: **IMPLEMENTED**

## 14. Keyword Search
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/application/memory/SearchMemoryEvidence.kt`
* **Main Class/Interface**: `SearchMemoryEvidence`, `MemoryEvidenceLiteralSearchSupport`
* **What it Actually Does**: Executes literal/FTS keyword search over stored memory evidence excerpts, OCR text, PDF pages, and note pages.
* **Storage Used**: Room FTS / text search queries.
* **Current Status**: **IMPLEMENTED**

## 15. Meaning / Semantic Search
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/application/intelligence/SearchAssetMemoriesByMeaning.kt`
* **Main Class/Interface**: `SearchAssetMemoriesByMeaning`, `MeaningSearchScreen`
* **What it Actually Does**: Computes cosine similarity between query vectors and stored summary/evidence vectors to retrieve semantic candidates.
* **Storage Used**: Room vector stores.
* **Current Status**: **IMPLEMENTED**

## 16. Ranking / Retrieval
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/application/memory/AnchorAwareMeaningRecallRanking.kt`
* **Main Class/Interface**: `AnchorAwareMeaningRecallRanking`, `ReciprocalRankFusion`, `OnnxCrossEncoderRecallRanker`
* **What it Actually Does**: Fuses lexical and meaning candidate lists (RRF), applies anchor-aware time/topic structured filters, role scoring, and optional Stage-A cross-encoder re-ranking.
* **Storage Used**: In-memory ranking pipeline.
* **Current Status**: **IMPLEMENTED**

## 17. CanonicalRecall
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/application/memory/CanonicalRecall.kt`
* **Main Class/Interface**: `CanonicalRecall`
* **What it Actually Does**: Unified application retrieval boundary routing keyword queries to `SearchMemoryEvidence` and meaning queries to `searchByMeaning` (fused retrieval + ranking).
* **Storage Used**: N/A (orchestrator)
* **Current Status**: **IMPLEMENTED**

## 18. Evidence
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/domain/memory/` and extraction ports.
* **Main Class/Interface**: `MemoryEvidence`, `MemoryEvidenceSearchRow`
* **What it Actually Does**: Represents citable source text snippets (PDF page excerpts, OCR blocks, note pages) bound to asset memories.
* **Storage Used**: Room (`memory_evidence`)
* **Current Status**: **IMPLEMENTED**

## 19. Memory / AssetMemory
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/domain/intelligence/DeterministicMemoryBuilder.kt`
* **Main Class/Interface**: `DeterministicMemoryBuilder`, `AssetMemoryFact`
* **What it Actually Does**: Assembles validated structured memory summaries from extracted facts without inventing source data.
* **Storage Used**: Room (`assets`, `memory_facts`, `memories`)
* **Current Status**: **IMPLEMENTED**

## 20. UI and Search
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/MainActivity.kt` and `ui/search/`
* **Main Class/Interface**: `MainActivity`, `MeaningSearchScreen`, `PdfKeywordSearchScreen`, `PhotoOcrKeywordSearchScreen`
* **What it Actually Does**: Composes the single-activity Jetpack Compose application UI, handling setup disclosures, model downloads, search tabs, and original file previews.
* **Storage Used**: UI StateFlows
* **Current Status**: **IMPLEMENTED**

## 21. Security / Encryption
* **File Path**: `MemoraApp/app/src/main/java/com/memora/app/data/security/` and `PersistenceModule.kt`
* **Main Class/Interface**: `MemoraDatabaseHandle`, `MemoraEncryptedDatabaseOpener`
* **What it Actually Does**: SQLCipher dependencies and encrypted opener classes are present in the dependency graph and source base, but production persistence (`PersistenceModule.kt`) currently opens `memora.db` as plaintext SQLite (`MemoraDatabaseHandle`).
* **Storage Used**: SQLite (`memora.db`)
* **Current Status**: **PARTIAL** (SQLCipher infrastructure present; database currently opens plaintext).

## 22. Tests
* **File Path**: `MemoraApp/app/src/test/` and `MemoraApp/app/src/androidTest/`
* **Main Class/Interface**: JUnit, Kotlin Coroutines Test, Room Testing, 220+ unit test files, 50+ instrumentation/integration test files.
* **What it Actually Does**: Tests domain rules, Room migrations, isolated parser IPC contracts, vector math, and recall ranking convergence.
* **Storage Used**: In-memory databases and test fixtures.
* **Current Status**: **IMPLEMENTED**

---

## Factual Inventory Summary Table

| Area | Status | Main Files | What is Implemented |
| :--- | :--- | :--- | :--- |
| **1. Project structure** | IMPLEMENTED | `app/build.gradle.kts`, `com/memora/app/` | Single-module clean architecture app layout |
| **2. Manifest & permissions** | IMPLEMENTED | `AndroidManifest.xml` | Media permissions, isolated PDF service, MSAL activity |
| **3. Gradle dependencies** | IMPLEMENTED | `app/build.gradle.kts` | Compose, Room, Hilt, WorkManager, PDFBox, ONNX, SQLCipher |
| **4. Room database & migration** | IMPLEMENTED | `MemoraDatabase.kt`, `MemoraDatabaseMigrations.kt` | Relational tables and migration scripts (Mig01-05) |
| **5. MediaStore / discovery** | IMPLEMENTED | `MediaStoreImageDiscoverySource.kt`, `SafPdfDiscoverySource.kt` | Scans shared images/screenshots and user-selected SAF PDFs |
| **6. Indexing & WorkManager** | IMPLEMENTED | `UnfyndApplication.kt`, `*Worker.kt`, `*Scheduler.kt` | Background sync and extraction worker pipeline |
| **7. Metadata extraction** | IMPLEMENTED | `ContentResolverImageExifReader.kt` | EXIF metadata reader for photos |
| **8. OCR** | IMPLEMENTED | `MlKitPhotoOcrReader.kt`, `MlKitScreenshotOcrReader.kt` | Google ML Kit text recognition for photos and screenshots |
| **9. PDF extraction** | IMPLEMENTED | `IsolatedPdfParserService.kt`, `IIsolatedPdfParser.aidl` | Secure sandboxed PDF text parsing via AIDL |
| **10. Image/Video/Audio** | PARTIAL | `MediaStoreImageDiscoverySource.kt` | Image processing implemented; video/audio discovery absent |
| **11. Embedding models** | IMPLEMENTED | `OnnxBgeSmallEmbeddingEngine.kt` | BGE-small ONNX embedder with asymmetric query prefix |
| **12. Model download** | IMPLEMENTED | `HttpOnDeviceEmbeddingModelDownloader.kt` | SHA-256 pinned model downloader and legacy clearer |
| **13. Vector storage** | IMPLEMENTED | `RoomMemoryEmbeddingStore.kt`, `RoomMemoryEvidenceEmbeddingStore.kt` | Summary and evidence vector storage with model isolation |
| **14. Keyword search** | IMPLEMENTED | `SearchMemoryEvidence.kt` | FTS/literal search over extracted evidence excerpts |
| **15. Meaning search** | IMPLEMENTED | `SearchAssetMemoriesByMeaning.kt` | Vector cosine similarity search over memories and evidence |
| **16. Ranking / retrieval** | IMPLEMENTED | `AnchorAwareMeaningRecallRanking.kt`, `ReciprocalRankFusion.kt` | RRF fusion, anchor filters, and cross-encoder re-ranking |
| **17. CanonicalRecall** | IMPLEMENTED | `CanonicalRecall.kt` | Unified keyword and meaning search boundary |
| **18. Evidence** | IMPLEMENTED | `MemoryEvidence.kt`, `MemoryEvidenceSearchRow.kt` | Citable text excerpts bound to asset memories |
| **19. Memory / AssetMemory** | IMPLEMENTED | `DeterministicMemoryBuilder.kt` | Schema-validated memory fact builder |
| **20. UI and search** | IMPLEMENTED | `MainActivity.kt`, `ui/search/`, `ui/setup/` | Compose UI, setup screens, and search tabs |
| **21. Security / encryption** | PARTIAL | `PersistenceModule.kt`, `MemoraDatabaseHandle.kt` | SQLCipher classes present; database currently plaintext |
| **22. Tests** | IMPLEMENTED | `src/test/`, `src/androidTest/` | 220+ unit tests and 50+ integration tests |

---

## Top 10 Architecture Inspection Files

To understand the core architecture of UNFYND, inspect these 10 key classes/files:

1. [CanonicalRecall.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/application/memory/CanonicalRecall.kt) — Unified application retrieval boundary coordinating keyword and meaning search.
2. [SearchAssetMemoriesByMeaning.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/application/intelligence/SearchAssetMemoriesByMeaning.kt) — Semantic vector search engine over memory summaries and evidence.
3. [SearchMemoryEvidence.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/application/memory/SearchMemoryEvidence.kt) — Lexical/keyword search engine over text evidence.
4. [AnchorAwareMeaningRecallRanking.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/application/memory/AnchorAwareMeaningRecallRanking.kt) — RRF fusion, role scoring, and cross-encoder re-ranking pipeline.
5. [OnnxBgeSmallEmbeddingEngine.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/data/intelligence/OnnxBgeSmallEmbeddingEngine.kt) — Production BGE-small ONNX embedding engine with asymmetric query/text prefixing.
6. [HttpOnDeviceEmbeddingModelDownloader.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/data/intelligence/HttpOnDeviceEmbeddingModelDownloader.kt) — SHA-256 pinned model downloader and legacy pack cleaner.
7. [IsolatedPdfParserService.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/data/pdfbox/isolation/IsolatedPdfParserService.kt) — Secure sandboxed PDF parser service running in an isolated process via AIDL.
8. [MemoraDatabase.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/data/local/MemoraDatabase.kt) — Central Room database defining tables, DAOs, and persistence schemas.
9. [LocalIntelligenceModule.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/data/di/LocalIntelligenceModule.kt) — Hilt dependency injection module wiring intelligence engines, stores, and downloaders.
10. [MainActivity.kt](file:///C:/Users/DELL/Documents/Memora/MemoraApp/app/src/main/java/com/memora/app/MainActivity.kt) — Single-activity Jetpack Compose host managing navigation, setup, and search UIs.
