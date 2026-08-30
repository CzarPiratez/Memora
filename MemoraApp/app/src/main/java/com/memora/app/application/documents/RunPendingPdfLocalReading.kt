package com.memora.app.application.documents

import android.os.CancellationSignal
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs one bounded local PDF reading unit for the next pending Asset on a source.
 *
 * Uses the approved descriptor broker and isolated parser only (ADR-017). Persists
 * searchable text through [ValidatedPdfLocalReadingPersister]. Never schedules WorkManager
 * itself, invokes AI, or uses the network.
 */
interface PendingPdfLocalReader {
    suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingPdfLocalReadingOutcome
}

@Singleton
class RunPendingPdfLocalReading @Inject constructor(
    private val assetRepository: AssetRepository,
    private val isolatedSession: PdfIsolatedLocalReadingSession,
    private val persister: ValidatedPdfLocalReadingPersister,
) : PendingPdfLocalReader {
    override suspend operator fun invoke(
        sourceId: SourceId,
        afterSourceAssetKey: String?,
        cancellationSignal: CancellationSignal,
    ): PendingPdfLocalReadingOutcome = withContext(Dispatchers.IO) {
        if (cancellationSignal.isCanceled) {
            return@withContext PendingPdfLocalReadingOutcome.Cancelled
        }

        val asset = assetRepository.findNextPdfPendingLocalReading(
            sourceId = sourceId,
            schemaVersion = EXTRACTION_SCHEMA.value,
            afterSourceAssetKey = afterSourceAssetKey,
        ) ?: return@withContext PendingPdfLocalReadingOutcome.NoPending

        val request = PdfExtractionRequest(
            asset = asset,
            schemaVersion = EXTRACTION_SCHEMA,
        )
        val processedKey = asset.identity.sourceAssetKey.value

        when (val parse = isolatedSession.parseApprovedRequest(request, cancellationSignal)) {
            is PdfIsolatedLocalReadingParseOutcome.Parsed -> {
                when (val status = persister.persist(request, parse.persistenceHandle)) {
                    PdfLocalReadingStatusCheckResult.Completed ->
                        advance(sourceId, processedKey, persisted = true)
                    PdfLocalReadingStatusCheckResult.PasswordProtected ->
                        advance(sourceId, processedKey, persisted = false, password = true)
                    PdfLocalReadingStatusCheckResult.AccessRecoveryNeeded ->
                        PendingPdfLocalReadingOutcome.AccessStopped
                    PdfLocalReadingStatusCheckResult.RetryableProblem,
                    PdfLocalReadingStatusCheckResult.Cancelled,
                    -> PendingPdfLocalReadingOutcome.RetryableFailure
                }
            }
            PdfIsolatedLocalReadingParseOutcome.ServiceUnavailable,
            PdfIsolatedLocalReadingParseOutcome.RetryableFailure,
            -> PendingPdfLocalReadingOutcome.RetryableFailure
            PdfIsolatedLocalReadingParseOutcome.AccessStopped ->
                PendingPdfLocalReadingOutcome.AccessStopped
            PdfIsolatedLocalReadingParseOutcome.Cancelled ->
                PendingPdfLocalReadingOutcome.Cancelled
        }
    }

    private suspend fun advance(
        sourceId: SourceId,
        processedKey: String,
        persisted: Boolean,
        password: Boolean = false,
    ): PendingPdfLocalReadingOutcome {
        val hasMore = assetRepository.findNextPdfPendingLocalReading(
            sourceId = sourceId,
            schemaVersion = EXTRACTION_SCHEMA.value,
            afterSourceAssetKey = processedKey,
        ) != null
        return when {
            password -> PendingPdfLocalReadingOutcome.SkippedPassword(
                processedSourceAssetKey = processedKey,
                hasMorePending = hasMore,
            )
            persisted -> PendingPdfLocalReadingOutcome.Persisted(
                processedSourceAssetKey = processedKey,
                hasMorePending = hasMore,
            )
            else -> PendingPdfLocalReadingOutcome.RetryableFailure
        }
    }

    companion object {
        val EXTRACTION_SCHEMA = ExtractionSchemaVersion("pdf-extraction-v1")
    }
}

/** Content-free outcome for one pending PDF reading unit. */
sealed interface PendingPdfLocalReadingOutcome {
    data object NoPending : PendingPdfLocalReadingOutcome

    data class Persisted(
        val processedSourceAssetKey: String,
        val hasMorePending: Boolean,
    ) : PendingPdfLocalReadingOutcome {
        init {
            require(processedSourceAssetKey.isNotBlank())
        }
    }

    data class SkippedPassword(
        val processedSourceAssetKey: String,
        val hasMorePending: Boolean,
    ) : PendingPdfLocalReadingOutcome {
        init {
            require(processedSourceAssetKey.isNotBlank())
        }
    }

    data object AccessStopped : PendingPdfLocalReadingOutcome

    data object RetryableFailure : PendingPdfLocalReadingOutcome

    data object Cancelled : PendingPdfLocalReadingOutcome
}
