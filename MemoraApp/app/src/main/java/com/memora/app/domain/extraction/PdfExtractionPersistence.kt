package com.memora.app.domain.extraction

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity

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

/** This decision contains no extracted page text, metadata, title, source location, or URI. */
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
 * The record's content is deliberately absent. A future persistence implementation receives that
 * content only through [PdfExtractionPersistenceWriteRequest], after this contract validates both
 * values together.
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

/**
 * The only input accepted by a future atomic PDF-extraction persistence implementation.
 *
 * Its private constructor and eligible-decision factory prevent a generic or ineligible decision
 * from being handed to the repository. The record and content-free facts are revalidated together
 * so a stale fingerprint, schema, page count, or coverage cannot be written as a verified result.
 */
internal class PdfExtractionPersistenceWriteRequest private constructor(
    val record: PdfExtractionRecord,
    val facts: PdfExtractionPersistenceFacts,
) {
    init {
        require(record.assetIdentity == facts.key.assetIdentity) {
            "PDF persistence record identity must match its facts."
        }
        require(record.assetFingerprint == facts.key.assetFingerprint) {
            "PDF persistence record fingerprint must match its facts."
        }
        require(record.schemaVersion == facts.key.schemaVersion) {
            "PDF persistence record schema must match its facts."
        }
        require(record.pageCount == facts.pageCount) {
            "PDF persistence record page count must match its facts."
        }
        require(
            (facts.coverage == PersistablePdfTextCoverage.COMPLETE &&
                record.textCoverage == PdfTextCoverage.Complete) ||
                (facts.coverage == PersistablePdfTextCoverage.NO_EXTRACTABLE_TEXT &&
                    record.textCoverage == PdfTextCoverage.NoExtractableText),
        ) {
            "Only complete or explicit no-text PDF coverage may be persisted."
        }
    }

    companion object {
        fun from(
            decision: PdfExtractionPersistenceDecision.EligibleForAtomicWrite,
            record: PdfExtractionRecord,
        ): PdfExtractionPersistenceWriteRequest = PdfExtractionPersistenceWriteRequest(
            record = record,
            facts = decision.facts,
        )
    }
}

/**
 * Inward-facing port for a future data repository's one-transaction extraction write.
 *
 * The implementation must save [PdfExtractionPersistenceWriteRequest.record] and its matching
 * [PdfExtractionPersistenceWriteRequest.facts] atomically, or return an explicit outcome. No Room
 * implementation exists at this checkpoint.
 */
internal interface PdfExtractionPersistencePort {
    suspend fun persist(
        request: PdfExtractionPersistenceWriteRequest,
    ): PdfExtractionPersistenceWriteOutcome
}

/** A future repository may never report an ambiguous persistence result. */
internal sealed interface PdfExtractionPersistenceWriteOutcome {
    data object Persisted : PdfExtractionPersistenceWriteOutcome

    data object RetryableFailure : PdfExtractionPersistenceWriteOutcome

    data object StaleReindexRequired : PdfExtractionPersistenceWriteOutcome

    data object FailedSafely : PdfExtractionPersistenceWriteOutcome
}
