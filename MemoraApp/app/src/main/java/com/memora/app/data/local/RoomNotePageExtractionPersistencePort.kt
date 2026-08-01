package com.memora.app.data.local

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.extraction.NotePageExtractionPersistence
import com.memora.app.domain.extraction.NotePageExtractionRecord
import com.memora.app.domain.extraction.NotePageSchemaVersion

class RoomNotePageExtractionPersistencePort(
    private val database: () -> MemoraDatabase,
) : NotePageExtractionPersistence {
    override suspend fun findHeader(record: NotePageExtractionRecord): NotePageExtractionRecord? {
        val entity = database().notePageExtractionDao().findHeader(
            sourceId = record.asset.identity.sourceId.value,
            sourceAssetKey = record.asset.identity.sourceAssetKey.value,
            fingerprint = record.asset.fingerprint.value,
            schemaVersion = record.schemaVersion.value,
        ) ?: return null
        return entity.toDomain(record.asset)
    }

    override suspend fun insert(record: NotePageExtractionRecord) {
        val now = System.currentTimeMillis()
        database().notePageExtractionDao().insert(
            NotePageExtractionEntity(
                sourceId = record.asset.identity.sourceId.value,
                sourceAssetKey = record.asset.identity.sourceAssetKey.value,
                fingerprint = record.asset.fingerprint.value,
                schemaVersion = record.schemaVersion.value,
                fullText = record.fullText,
                textTruncated = record.textTruncated,
                engineId = record.engineId,
                engineVersion = record.engineVersion,
                extractedAtEpochMillis = record.extractedAtEpochMillis,
                createdAtEpochMillis = now,
                integrity = record.integrity,
            ),
        )
    }

    override suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int =
        database().notePageExtractionDao().countCurrentForSource(sourceId, schemaVersion)
}

private fun NotePageExtractionEntity.toDomain(asset: Asset): NotePageExtractionRecord =
    NotePageExtractionRecord(
        asset = asset,
        schemaVersion = NotePageSchemaVersion(schemaVersion),
        fullText = fullText,
        textTruncated = textTruncated,
        engineId = engineId,
        engineVersion = engineVersion,
        extractedAtEpochMillis = extractedAtEpochMillis,
        integrity = integrity,
    )
