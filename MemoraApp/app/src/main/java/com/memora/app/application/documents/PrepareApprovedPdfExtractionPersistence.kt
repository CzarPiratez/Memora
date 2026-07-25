package com.memora.app.application.documents

import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
import com.memora.app.domain.extraction.PdfExtractionPersistenceDecision
import com.memora.app.domain.extraction.PdfExtractionPersistenceFacts
import com.memora.app.domain.extraction.PdfExtractionPersistenceKey
import com.memora.app.domain.extraction.PdfExtractionPersistenceLifecycle
import com.memora.app.domain.extraction.PdfExtractionIntegrity
import com.memora.app.domain.extraction.PdfExtractionRetryDirective
import com.memora.app.domain.extraction.PdfTextCoverage
import com.memora.app.domain.extraction.PersistablePdfTextCoverage

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

        ApprovedPdfExtractionAssemblyOutcome.StaleSource -> notEligible(
            request,
            lifecycle = PdfExtractionPersistenceLifecycle.STALE_REINDEX_REQUIRED,
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
