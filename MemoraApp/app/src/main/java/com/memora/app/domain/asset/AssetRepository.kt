package com.memora.app.domain.asset

/** The atomic local record that keeps a source Asset and its recoverable state together. */
data class AssetIndexRecord(
    val asset: Asset,
    val indexingState: IndexingState,
)

/** Persistence boundary for source-neutral assets. Implementations must be idempotent. */
interface AssetRepository {
    suspend fun save(record: AssetIndexRecord)

    suspend fun find(identity: AssetIdentity): AssetIndexRecord?

    /** Returns the first stored asset for [sourceId] with [type], or null when none exist. */
    suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset?

    /** Returns how many stored assets exist for [sourceId] with [type]. */
    suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int

    /**
     * Returns the next PDF that still needs local reading for [schemaVersion], or null.
     *
     * [afterSourceAssetKey] is exclusive; pass null/blank to start from the first pending PDF.
     */
    suspend fun findNextPdfPendingLocalReading(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String? = null,
    ): Asset?

    /**
     * Returns the next PHOTO/SCREENSHOT that still needs EXIF extract for [schemaVersion].
     *
     * [afterSourceAssetKey] is exclusive; pass null/blank to start from the first pending.
     */
    suspend fun findNextImagePendingExifExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String? = null,
    ): Asset?

    /**
     * Returns the next SCREENSHOT that still needs OCR extract for [schemaVersion].
     *
     * [afterSourceAssetKey] is exclusive; pass null/blank to start from the first pending.
     */
    suspend fun findNextScreenshotPendingOcrExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String? = null,
    ): Asset?
}
