package com.memora.app.data.local

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.ImageExifExtractionPersistence
import com.memora.app.domain.extraction.ImageExifExtractionRecord
import com.memora.app.domain.extraction.ImageExifSchemaVersion

class RoomImageExifExtractionPersistencePort(
    private val database: () -> MemoraDatabase,
) : ImageExifExtractionPersistence {
    override suspend fun findHeader(record: ImageExifExtractionRecord): ImageExifExtractionRecord? {
        val entity = database().imageExifExtractionDao().findHeader(
            sourceId = record.asset.identity.sourceId.value,
            sourceAssetKey = record.asset.identity.sourceAssetKey.value,
            fingerprint = record.asset.fingerprint.value,
            schemaVersion = record.schemaVersion.value,
        ) ?: return null
        return entity.toDomain(record.asset)
    }

    override suspend fun insert(record: ImageExifExtractionRecord) {
        val now = System.currentTimeMillis()
        val db = database()
        db.imageExifExtractionDao().insert(
            ImageExifExtractionEntity(
                sourceId = record.asset.identity.sourceId.value,
                sourceAssetKey = record.asset.identity.sourceAssetKey.value,
                fingerprint = record.asset.fingerprint.value,
                schemaVersion = record.schemaVersion.value,
                assetKind = record.assetKind.name,
                datetimeOriginal = record.datetimeOriginal,
                imageWidth = record.imageWidth,
                imageHeight = record.imageHeight,
                orientation = record.orientation,
                make = record.make,
                model = record.model,
                extractedAtEpochMillis = record.extractedAtEpochMillis,
                createdAtEpochMillis = now,
                integrity = record.integrity,
            ),
        )
        db.clearMemoryAssemblySkips(
            sourceId = record.asset.identity.sourceId.value,
            sourceAssetKey = record.asset.identity.sourceAssetKey.value,
            fingerprint = record.asset.fingerprint.value,
        )
    }

    override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int =
        database().imageExifExtractionDao().countCurrentForSource(sourceId, schemaVersion)
}

private fun ImageExifExtractionEntity.toDomain(asset: Asset): ImageExifExtractionRecord =
    ImageExifExtractionRecord(
        asset = asset,
        schemaVersion = ImageExifSchemaVersion(schemaVersion),
        assetKind = AssetType.valueOf(assetKind),
        datetimeOriginal = datetimeOriginal,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        orientation = orientation,
        make = make,
        model = model,
        extractedAtEpochMillis = extractedAtEpochMillis,
        integrity = integrity,
    )
