package com.memora.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AssetEntity::class,
        DiscoveryCheckpointEntity::class,
        DocumentTreeApprovalEntity::class,
        PdfExtractionEntity::class,
        PdfExtractionPageEntity::class,
        PdfExtractionMetadataEntity::class,
        ImageExifExtractionEntity::class,
        ScreenshotOcrExtractionEntity::class,
        PhotoOcrExtractionEntity::class,
        NotePageExtractionEntity::class,
        MemoryEntity::class,
        MemoryExtractionSchemaEntity::class,
        MemoryEvidenceEntity::class,
        MemoryAnchorEntity::class,
        MemoryAnchorEvidenceEntity::class,
        MemorySummaryEvidenceEntity::class,
        AiPackInstallLedgerEntity::class,
        MemoryEmbeddingEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class MemoraDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    abstract fun discoveryCheckpointDao(): DiscoveryCheckpointDao

    abstract fun documentTreeApprovalDao(): DocumentTreeApprovalDao

    abstract fun pdfExtractionDao(): PdfExtractionDao

    abstract fun imageExifExtractionDao(): ImageExifExtractionDao

    abstract fun screenshotOcrExtractionDao(): ScreenshotOcrExtractionDao

    abstract fun photoOcrExtractionDao(): PhotoOcrExtractionDao

    abstract fun notePageExtractionDao(): NotePageExtractionDao

    abstract fun memoryDao(): MemoryDao

    abstract fun assetMemoryFactDao(): AssetMemoryFactDao

    abstract fun aiPackInstallLedgerDao(): AiPackInstallLedgerDao

    abstract fun memoryEmbeddingDao(): MemoryEmbeddingDao
}
