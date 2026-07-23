package com.memora.app.application.documents

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfTextCoverage

/**
 * Produces a content-free decision for a future atomic extraction write.
 *
 * This is deliberately a pure application boundary. It does not persist [PdfExtractionRecord],
 * expose its page text or metadata, open a source, or schedule work. A later persistence use case
 * must use the returned [PdfExtractionPersistenceKey] and [PdfExtractionPersistenceFacts] in the
 * same transaction as the already validated record; this contract only decides whether that future
 * transaction is allowed and which non-content facts it must carry.
 */
internal class PrepareApprovedPdfExtractionPersistence {
    fun prepare(
        request: PdfExtractionRequest,
        outcome: ApprovedPdfExtractionAssemblyOutcome,
    ): PdfExtractionPersistenceDecision = when (outcome) {
        is ApprovedPdfExtractionAssemblyOutcome.Extracted -> prepareExtracted(request, outcome.record)
        ApprovedPdfExtractionAssemblyOutcome.PasswordProtected -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.FAILED_SAFELY,
            retry = PdfExtractionRetryDirective.USER_ACTION_REQUIRED,
        )

        ApprovedPdfExtractionAssemblyOutcome.AccessRequired,
        ApprovedPdfExtractionAssemblyOutcome.AccessRevoked -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.AWAITING_PERMISSION,
            retry = PdfExtractionRetryDirective.USER_ACTION_REQUIRED,
        )

        ApprovedPdfExtractionAssemblyOutcome.SourceUnavailable,
        ApprovedPdfExtractionAssemblyOutcome.SourceMismatch -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.SOURCE_UNAVAILABLE,
            retry = PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION,
        )

        ApprovedPdfExtractionAssemblyOutcome.Cancelled,
        ApprovedPdfExtractionAssemblyOutcome.RetryableParserFailure -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.QUEUED,
            retry = PdfExtractionRetryDirective.RETRY_WHEN_REQUEUED,
        )

        ApprovedPdfExtractionAssemblyOutcome.ParserFailure -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.FAILED_SAFELY,
            retry = PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION,
        )

        is ApprovedPdfExtractionAssemblyOutcome.ExtractionFailure -> notEligible(
            request,
            lifecycle = if (outcome.retryable) {
                PdfExtractionPersistenceLifecycle.QUEUED
            } else {
                PdfExtractionPersistenceLifecycle.FAILED_SAFELY
            },
            retry = if (outcome.retryable) {
                PdfExtractionRetryDirective.RETRY_WHEN_REQUEUED
            } else {
                PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION
            },
        )

        ApprovedPdfExtractionAssemblyOutcome.InconsistentExtraction -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED,
            retry = PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION,
        )
    }

    private fun prepareExtracted(
        request: PdfExtractionRequest,
        record: PdfExtractionRecord,
    ): PdfExtractionPersistenceDecision {
        val key = PdfExtractionPersistenceKey.from(request)
        if (!record.matches(key)) {
            return PdfExtractionPersistenceDecision.NotEligible(
                key = key,
                lifecycle = PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED,
                retry = PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION,
            )
        }

        val coverage = when (record.textCoverage) {
            PdfTextCoverage.Complete -> PersistablePdfTextCoverage.COMPLETE
            PdfTextCoverage.NoExtractableText -> PersistablePdfTextCoverage.NO_EXTRACTABLE_TEXT
            is PdfTextCoverage.Partial -> {
                return PdfExtractionPersistenceDecision.NotEligible(
                    key = key,
                    lifecycle = PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED,
                    retry = PdfExtractionRetryDirective.REQUIRES_FRESH_EXTRACTION,
                )
            }
        }

        return PdfExtractionPersistenceDecision.EligibleForAtomicWrite(
            PdfExtractionPersistenceFacts(
                key = key,
                pageCount = record.pageCount!!,
                coverage = coverage,
                integrity = PdfExtractionIntegrity.VERIFIED,
                retry = PdfExtractionRetryDirective.NO_RETRY_REQUIRED,
            ),
        )
    }

    private fun notEligible(
        request: PdfExtractionRequest,
        lifecycle: PdfExtractionPersistenceLifecycle,
        retry: PdfExtractionRetryDirective,
    ): PdfExtractionPersistenceDecision.NotEligible = PdfExtractionPersistenceDecision.NotEligible(
        key = PdfExtractionPersistenceKey.from(request),
        lifecycle = lifecycle,
        retry = retry,
    )

    private fun PdfExtractionRecord.matches(key: PdfExtractionPersistenceKey): Boolean =
        assetIdentity == key.assetIdentity &&
            assetFingerprint == key.assetFingerprint &&
            schemaVersion == key.schemaVersion &&
            pageCount != null
}

/** Immutable source-version binding that a future atomic write must retain. */
internal data class PdfExtractionPersistenceKey(
    val assetIdentity: AssetIdentity,
    val assetFingerprint: AssetFingerprint,
    val schemaVersion: ExtractionSchemaVersion,
) {
    companion object {
        fun from(request: PdfExtractionRequest): PdfExtractionPersistenceKey =
            PdfExtractionPersistenceKey(
                assetIdentity = request.asset.identity,
                assetFingerprint = request.asset.fingerprint,
                schemaVersion = request.schemaVersion,
            )
    }
}

/** The only text-coverage facts eligible for a later durable extraction record. */
internal enum class PersistablePdfTextCoverage {
    COMPLETE,
    NO_EXTRACTABLE_TEXT,
}

/** Integrity is verified before a record may be made durable; it is not a confidence score. */
internal enum class PdfExtractionIntegrity {
    VERIFIED,
}

/**
 * Content-free lifecycle state for a future persistence/indexing transaction.
 *
 * These values intentionally align with the product integrity vocabulary without claiming that a
 * Room schema or user-facing state machine exists yet.
 */
internal enum class PdfExtractionPersistenceLifecycle {
    AWAITING_PERMISSION,
    QUEUED,
    READY,
    STALE_REINDEX_REQUIRED,
    SOURCE_UNAVAILABLE,
    FAILED_SAFELY,
}

/** The future scheduler/recovery layer must follow this explicit directive, never infer one. */
internal enum class PdfExtractionRetryDirective {
    NO_RETRY_REQUIRED,
    RETRY_WHEN_REQUEUED,
    REQUIRES_FRESH_EXTRACTION,
    USER_ACTION_REQUIRED,
}

/** This contract contains no extracted page text, metadata, title, source location, or URI. */
internal sealed interface PdfExtractionPersistenceDecision {
    data class EligibleForAtomicWrite(
        val facts: PdfExtractionPersistenceFacts,
    ) : PdfExtractionPersistenceDecision

    data class NotEligible(
        val key: PdfExtractionPersistenceKey,
        val lifecycle: PdfExtractionPersistenceLifecycle,
        val retry: PdfExtractionRetryDirective,
    ) : PdfExtractionPersistenceDecision
}

/**
 * Metadata-only facts for the later atomic write of a validated extraction record.
 *
 * The record's content will be supplied only by that future, separately reviewed persistence
 * implementation; this value deliberately cannot carry text, metadata, title, URI, or source.
 */
internal data class PdfExtractionPersistenceFacts(
    val key: PdfExtractionPersistenceKey,
    val pageCount: Int,
    val coverage: PersistablePdfTextCoverage,
    val integrity: PdfExtractionIntegrity,
    val retry: PdfExtractionRetryDirective,
    val lifecycle: PdfExtractionPersistenceLifecycle = PdfExtractionPersistenceLifecycle.READY,
) {
    init {
        require(pageCount > 0) { "Persisted PDF page count must be positive." }
        require(integrity == PdfExtractionIntegrity.VERIFIED) {
            "Only verified PDF extraction facts may be eligible for an atomic write."
        }
        require(retry == PdfExtractionRetryDirective.NO_RETRY_REQUIRED) {
            "A PDF extraction eligible for an atomic write cannot require a retry."
        }
        require(lifecycle == PdfExtractionPersistenceLifecycle.READY) {
            "A PDF extraction eligible for an atomic write must be ready."
        }
    }
}
