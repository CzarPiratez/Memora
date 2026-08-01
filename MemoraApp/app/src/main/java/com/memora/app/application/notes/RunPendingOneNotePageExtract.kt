package com.memora.app.application.notes

import android.os.CancellationSignal
import com.memora.app.data.local.RoomNotePageExtractionPersistencePort
import com.memora.app.data.notes.OneNotePageContentReader
import com.memora.app.data.notes.OneNotePagesDiscoverySource
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.extraction.NotePageExtractionRecord
import com.memora.app.domain.extraction.NotePageReadResult
import com.memora.app.domain.extraction.NotePageSchemaVersion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs one bounded OneNote page text extract for the next pending NOTE. */
interface PendingOneNotePageExtractor {
    suspend operator fun invoke(
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingOneNotePageExtractOutcome
}

sealed interface PendingOneNotePageExtractOutcome {
    data object NoPending : PendingOneNotePageExtractOutcome

    data class Persisted(
        val processedSourceAssetKey: String,
        val hasMorePending: Boolean,
    ) : PendingOneNotePageExtractOutcome {
        init {
            require(processedSourceAssetKey.isNotBlank())
        }
    }

    data object AccessStopped : PendingOneNotePageExtractOutcome

    data object RetryableFailure : PendingOneNotePageExtractOutcome

    data object Cancelled : PendingOneNotePageExtractOutcome
}

@Singleton
class RunPendingOneNotePageExtract @Inject constructor(
    private val assetRepository: AssetRepository,
    private val pageContentReader: OneNotePageContentReader,
    databaseHandle: MemoraDatabaseHandle,
) : PendingOneNotePageExtractor {
    private val persistence = RoomNotePageExtractionPersistencePort { databaseHandle.database() }

    override suspend operator fun invoke(
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingOneNotePageExtractOutcome = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) {
            return@withContext PendingOneNotePageExtractOutcome.Cancelled
        }

        val asset = assetRepository.findNextNotePendingPageExtract(
            sourceId = OneNotePagesDiscoverySource.SOURCE_ID,
            schemaVersion = SCHEMA.value,
            afterSourceAssetKey = afterSourceAssetKey,
        ) ?: return@withContext PendingOneNotePageExtractOutcome.NoPending

        val processedKey = asset.identity.sourceAssetKey.value
        if (cancellationSignal.isCanceled) {
            return@withContext PendingOneNotePageExtractOutcome.Cancelled
        }

        when (val read = pageContentReader.read(asset)) {
            NotePageReadResult.AccessStopped -> PendingOneNotePageExtractOutcome.AccessStopped
            NotePageReadResult.RetryableFailure -> PendingOneNotePageExtractOutcome.RetryableFailure
            is NotePageReadResult.Text -> {
                val record = NotePageExtractionRecord(
                    asset = asset,
                    schemaVersion = SCHEMA,
                    fullText = read.fullText,
                    textTruncated = read.textTruncated,
                    engineId = read.engineId,
                    engineVersion = read.engineVersion,
                    extractedAtEpochMillis = System.currentTimeMillis(),
                )
                persistence.insert(record)
                val hasMore = assetRepository.findNextNotePendingPageExtract(
                    sourceId = OneNotePagesDiscoverySource.SOURCE_ID,
                    schemaVersion = SCHEMA.value,
                    afterSourceAssetKey = processedKey,
                ) != null
                PendingOneNotePageExtractOutcome.Persisted(
                    processedSourceAssetKey = processedKey,
                    hasMorePending = hasMore,
                )
            }
        }
    }

    companion object {
        val SCHEMA: NotePageSchemaVersion = NotePageSchemaVersion.V1
    }
}
