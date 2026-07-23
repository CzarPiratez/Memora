package com.memora.app.application.documents

import com.memora.app.domain.extraction.PdfExtractionPersistenceDecision
import com.memora.app.domain.extraction.PdfExtractionPersistenceLifecycle
import com.memora.app.domain.extraction.PdfExtractionPersistencePort
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteOutcome
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteRequest
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRetryDirective

/**
 * Coordinates a future atomic extraction write without deciding storage details.
 *
 * The port is called only after [PdfExtractionPersistenceDecision.EligibleForAtomicWrite] has
 * produced a valid [PdfExtractionPersistenceWriteRequest]. An ineligible decision and an
 * internally inconsistent/missing record both return a truthful outcome before any port call.
 * This class has no Room, source, UI, WorkManager, parser, AI, or network dependency.
 */
internal class PersistApprovedPdfExtraction(
    private val persistencePort: PdfExtractionPersistencePort,
) {
    suspend fun execute(
        decision: PdfExtractionPersistenceDecision,
        record: PdfExtractionRecord?,
    ): PersistApprovedPdfExtractionOutcome = when (decision) {
        is PdfExtractionPersistenceDecision.EligibleForAtomicWrite -> {
            persistEligible(decision, record)
        }

        is PdfExtractionPersistenceDecision.NotEligible -> {
            PersistApprovedPdfExtractionOutcome.Ineligible(
                lifecycle = decision.lifecycle,
                retry = decision.retry,
            )
        }
    }

    private suspend fun persistEligible(
        decision: PdfExtractionPersistenceDecision.EligibleForAtomicWrite,
        record: PdfExtractionRecord?,
    ): PersistApprovedPdfExtractionOutcome {
        val writeRequest = try {
            record?.let { PdfExtractionPersistenceWriteRequest.from(decision, it) }
        } catch (_: IllegalArgumentException) {
            return PersistApprovedPdfExtractionOutcome.InconsistentInput
        } ?: return PersistApprovedPdfExtractionOutcome.InconsistentInput

        return when (persistencePort.persist(writeRequest)) {
            PdfExtractionPersistenceWriteOutcome.Persisted -> {
                PersistApprovedPdfExtractionOutcome.Persisted
            }

            PdfExtractionPersistenceWriteOutcome.RetryableFailure -> {
                PersistApprovedPdfExtractionOutcome.RetryableFailure
            }

            PdfExtractionPersistenceWriteOutcome.StaleReindexRequired -> {
                PersistApprovedPdfExtractionOutcome.StaleReindexRequired
            }

            PdfExtractionPersistenceWriteOutcome.FailedSafely -> {
                PersistApprovedPdfExtractionOutcome.FailedSafely
            }
        }
    }
}

/** Every application outcome is explicit; none asserts that a write happened without port proof. */
internal sealed interface PersistApprovedPdfExtractionOutcome {
    data object Persisted : PersistApprovedPdfExtractionOutcome

    data object RetryableFailure : PersistApprovedPdfExtractionOutcome

    data object StaleReindexRequired : PersistApprovedPdfExtractionOutcome

    data object FailedSafely : PersistApprovedPdfExtractionOutcome

    data object InconsistentInput : PersistApprovedPdfExtractionOutcome

    data class Ineligible(
        val lifecycle: PdfExtractionPersistenceLifecycle,
        val retry: PdfExtractionRetryDirective,
    ) : PersistApprovedPdfExtractionOutcome
}
