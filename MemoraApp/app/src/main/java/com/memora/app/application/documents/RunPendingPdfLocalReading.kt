package com.memora.app.application.documents

import android.content.Context
import android.os.CancellationSignal
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.pdfbox.isolation.AndroidIsolatedPdfParserConnection
import com.memora.app.data.pdfbox.isolation.ContextIsolatedPdfParserServiceBinder
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserBindingStatus
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserClient
import com.memora.app.data.pdfbox.isolation.IsolatedPdfParserDescriptorHandoff
import com.memora.app.data.saf.ContentResolverSafPdfDescriptorPlatform
import com.memora.app.data.saf.SafPdfDescriptorBroker
import com.memora.app.data.saf.SafPdfDescriptorBrokerResult
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs one bounded local PDF reading unit for the next pending Asset on a source.
 *
 * Uses the approved descriptor broker and isolated parser only (ADR-017). Persists
 * searchable text through [PersistValidatedPdfLocalReading]. Never schedules WorkManager
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
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val accessValidator: DocumentTreeAccessValidator,
    private val database: MemoraDatabase,
) : PendingPdfLocalReader {
    private val persistValidated = PersistValidatedPdfLocalReading(database)

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

        val connection = AndroidIsolatedPdfParserConnection(
            ContextIsolatedPdfParserServiceBinder(context),
        )
        val client = IsolatedPdfParserClient(connection)
        try {
            if (connection.connect() == IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE) {
                return@withContext PendingPdfLocalReadingOutcome.RetryableFailure
            }
            if (connection.awaitAvailability(BIND_TIMEOUT_MILLIS) !=
                IsolatedPdfParserBindingStatus.AVAILABLE
            ) {
                return@withContext PendingPdfLocalReadingOutcome.RetryableFailure
            }
            if (cancellationSignal.isCanceled) {
                return@withContext PendingPdfLocalReadingOutcome.Cancelled
            }

            val broker = SafPdfDescriptorBroker(
                approvalRepository = approvalRepository,
                accessValidator = accessValidator,
                platform = ContentResolverSafPdfDescriptorPlatform(context),
            )
            val handoff = IsolatedPdfParserDescriptorHandoff(client)
            when (
                val brokerResult = broker.withReadOnlyDescriptor(request, cancellationSignal) {
                    handoff.parseBorrowed(it, cancellationSignal)
                }
            ) {
                is SafPdfDescriptorBrokerResult.Consumed -> {
                    when (val status = persistValidated.execute(request, brokerResult.value)) {
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

                SafPdfDescriptorBrokerResult.AccessRequired,
                SafPdfDescriptorBrokerResult.AccessRevoked,
                -> PendingPdfLocalReadingOutcome.AccessStopped

                SafPdfDescriptorBrokerResult.Cancelled ->
                    PendingPdfLocalReadingOutcome.Cancelled

                SafPdfDescriptorBrokerResult.SourceUnavailable,
                SafPdfDescriptorBrokerResult.SourceMismatch,
                SafPdfDescriptorBrokerResult.StaleSource,
                SafPdfDescriptorBrokerResult.InvalidTarget,
                SafPdfDescriptorBrokerResult.TreeMembershipDenied,
                SafPdfDescriptorBrokerResult.UnsupportedPlatform,
                SafPdfDescriptorBrokerResult.RetryableFailure,
                -> PendingPdfLocalReadingOutcome.RetryableFailure
            }
        } finally {
            client.close()
            connection.close()
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
        private const val BIND_TIMEOUT_MILLIS = 10_000L
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
