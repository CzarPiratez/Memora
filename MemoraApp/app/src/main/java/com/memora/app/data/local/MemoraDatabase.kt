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
    ],
    version = 6,
    exportSchema = true,
)
abstract class MemoraDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    abstract fun discoveryCheckpointDao(): DiscoveryCheckpointDao

    abstract fun documentTreeApprovalDao(): DocumentTreeApprovalDao

    abstract fun pdfExtractionDao(): PdfExtractionDao

    abstract fun imageExifExtractionDao(): ImageExifExtractionDao

    abstract fun screenshotOcrExtractionDao(): ScreenshotOcrExtractionDao
}
