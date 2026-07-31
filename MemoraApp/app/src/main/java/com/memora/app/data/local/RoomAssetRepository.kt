package com.memora.app.data.local

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceId

/** Room-backed implementation of the Asset persistence boundary. */
class RoomAssetRepository(
    private val assetDao: () -> AssetDao,
) : AssetRepository {
    override suspend fun save(record: AssetIndexRecord) {
        assetDao().upsert(record.toEntity())
    }

    override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = assetDao()
        .find(identity.sourceId.value, identity.sourceAssetKey.value)
        ?.toDomain()

    override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? =
        assetDao()
            .findFirstBySourceAndType(sourceId.value, type.name)
            ?.toDomain()
            ?.asset

    override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int =
        assetDao().countBySourceAndType(sourceId.value, type.name)

    override suspend fun findNextPdfPendingLocalReading(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? =
        assetDao()
            .findNextPdfPendingLocalReading(
                sourceId = sourceId.value,
                assetType = AssetType.PDF.name,
                schemaVersion = schemaVersion,
                afterSourceAssetKey = afterSourceAssetKey.orEmpty(),
            )
            ?.toDomain()
            ?.asset

    override suspend fun findNextImagePendingExifExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? =
        assetDao()
            .findNextImagePendingExifExtract(
                sourceId = sourceId.value,
                schemaVersion = schemaVersion,
                afterSourceAssetKey = afterSourceAssetKey.orEmpty(),
            )
            ?.toDomain()
            ?.asset

    override suspend fun findNextScreenshotPendingOcrExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? =
        assetDao()
            .findNextScreenshotPendingOcrExtract(
                sourceId = sourceId.value,
                schemaVersion = schemaVersion,
                afterSourceAssetKey = afterSourceAssetKey.orEmpty(),
            )
            ?.toDomain()
            ?.asset

    override suspend fun findNextPhotoPendingOcrExtract(
        sourceId: SourceId,
        schemaVersion: String,
        afterSourceAssetKey: String?,
    ): Asset? =
        assetDao()
            .findNextPhotoPendingOcrExtract(
                sourceId = sourceId.value,
                schemaVersion = schemaVersion,
                afterSourceAssetKey = afterSourceAssetKey.orEmpty(),
            )
            ?.toDomain()
            ?.asset
}
