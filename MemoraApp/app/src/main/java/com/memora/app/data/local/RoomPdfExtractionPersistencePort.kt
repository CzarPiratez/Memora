package com.memora.app.data.local

import com.memora.app.domain.extraction.PdfExtractionIntegrity
import com.memora.app.domain.extraction.PdfExtractionPersistencePort
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteOutcome
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteRequest
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfTextCoverage
import androidx.room.withTransaction

/**
 * Room adapter for eligible PDF extraction writes.
 * Keeps superseded fingerprint/schema rows (ADR-022). Wired from the foreground
 * Local PDF reading Start path only; no WorkManager, AI, or network.
 */
internal class RoomPdfExtractionPersistencePort(
    private val database: () -> MemoraDatabase,
) : PdfExtractionPersistencePort {
    constructor(database: MemoraDatabase) : this(database = { database })

    override suspend fun persist(
        request: PdfExtractionPersistenceWriteRequest,
    ): PdfExtractionPersistenceWriteOutcome {
        val database = database()
        val dao = database.pdfExtractionDao()
        val record = request.record
        val key = request.facts.key
        val sourceId = key.assetIdentity.sourceId.value
        val sourceAssetKey = key.assetIdentity.sourceAssetKey.value
        val fingerprint = key.assetFingerprint.value
        val schemaVersion = key.schemaVersion.value

        return try {
            database.withTransaction {
                val existing = dao.findHeader(sourceId, sourceAssetKey, fingerprint, schemaVersion)
                if (existing != null) {
                    return@withTransaction if (matchesExisting(
                            dao,
                            existing,
                            record,
                            sourceId,
                            sourceAssetKey,
                            fingerprint,
                            schemaVersion,
                        )
                    ) {
                        PdfExtractionPersistenceWriteOutcome.Persisted
                    } else {
                        PdfExtractionPersistenceWriteOutcome.FailedSafely
                    }
                }

                val coverage = when (record.textCoverage) {
                    PdfTextCoverage.Complete -> COVERAGE_COMPLETE
                    PdfTextCoverage.NoExtractableText -> COVERAGE_NO_TEXT
                    is PdfTextCoverage.Partial -> {
                        return@withTransaction PdfExtractionPersistenceWriteOutcome.FailedSafely
                    }
                }
                val pageCount = record.pageCount
                    ?: return@withTransaction PdfExtractionPersistenceWriteOutcome.FailedSafely

                when (PdfExtractionWriteBudgets.evaluate(inspect(record, pageCount))) {
                    PdfExtractionWriteBudgets.WriteBudgetVerdict.Accepted -> Unit
                    is PdfExtractionWriteBudgets.WriteBudgetVerdict.Rejected -> {
                        return@withTransaction PdfExtractionPersistenceWriteOutcome.FailedSafely
                    }
                }

                val header = PdfExtractionEntity(
                    sourceId = sourceId,
                    sourceAssetKey = sourceAssetKey,
                    fingerprint = fingerprint,
                    schemaVersion = schemaVersion,
                    pageCount = pageCount,
                    textCoverage = coverage,
                    title = record.title,
                    extractedAtEpochMillis = record.extractedAt.toEpochMilli(),
                    createdAtEpochMillis = System.currentTimeMillis(),
                    integrity = PdfExtractionIntegrity.VERIFIED.name,
                )
                val pages = record.pages.map { page ->
                    PdfExtractionPageEntity(
                        sourceId = sourceId,
                        sourceAssetKey = sourceAssetKey,
                        fingerprint = fingerprint,
                        schemaVersion = schemaVersion,
                        pageNumber = page.pageNumber,
                        pageText = page.text,
                    )
                }
                val metadata = record.metadata.map { (name, value) ->
                    PdfExtractionMetadataEntity(
                        sourceId = sourceId,
                        sourceAssetKey = sourceAssetKey,
                        fingerprint = fingerprint,
                        schemaVersion = schemaVersion,
                        metadataName = name,
                        metadataValue = value,
                    )
                }
                dao.insertAtomic(header, pages, metadata)
                PdfExtractionPersistenceWriteOutcome.Persisted
            }
        } catch (_: Exception) {
            PdfExtractionPersistenceWriteOutcome.RetryableFailure
        }
    }

    private suspend fun matchesExisting(
        dao: PdfExtractionDao,
        existing: PdfExtractionEntity,
        record: PdfExtractionRecord,
        sourceId: String,
        sourceAssetKey: String,
        fingerprint: String,
        schemaVersion: String,
    ): Boolean {
        val expectedCoverage = when (record.textCoverage) {
            PdfTextCoverage.Complete -> COVERAGE_COMPLETE
            PdfTextCoverage.NoExtractableText -> COVERAGE_NO_TEXT
            is PdfTextCoverage.Partial -> return false
        }
        if (existing.pageCount != record.pageCount) return false
        if (existing.textCoverage != expectedCoverage) return false
        if (existing.title != record.title) return false
        if (existing.integrity != PdfExtractionIntegrity.VERIFIED.name) return false

        val pages = dao.findPages(sourceId, sourceAssetKey, fingerprint, schemaVersion)
        if (pages.size != record.pages.size) return false
        if (pages.zip(record.pages.sortedBy { it.pageNumber }).any { (stored, expected) ->
                stored.pageNumber != expected.pageNumber || stored.pageText != expected.text
            }
        ) {
            return false
        }

        val metadata = dao.findMetadata(sourceId, sourceAssetKey, fingerprint, schemaVersion)
            .associate { it.metadataName to it.metadataValue }
        return metadata == record.metadata
    }

    private fun inspect(
        record: PdfExtractionRecord,
        pageCount: Int,
    ): PdfExtractionWriteBudgets.WriteBudgetInspection {
        val pageCharCounts = record.pages.map { it.text.length }
        return PdfExtractionWriteBudgets.WriteBudgetInspection(
            pageCount = pageCount,
            totalChars = pageCharCounts.sum(),
            maxCharsOnAnyPage = pageCharCounts.maxOrNull() ?: 0,
            metadataEntryCount = record.metadata.size,
            maxMetadataNameChars = record.metadata.keys.maxOfOrNull { it.length } ?: 0,
            maxMetadataValueChars = record.metadata.values.maxOfOrNull { it.length } ?: 0,
            titleChars = record.title?.length ?: 0,
        )
    }

    companion object {
        const val COVERAGE_COMPLETE = "COMPLETE"
        const val COVERAGE_NO_TEXT = "NO_EXTRACTABLE_TEXT"
    }
}
