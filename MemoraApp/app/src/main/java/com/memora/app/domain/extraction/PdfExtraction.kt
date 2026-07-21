package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetType
import java.time.Instant

/** A versioned shape for deterministic source facts stored by a later repository. */
@JvmInline
value class ExtractionSchemaVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "An extraction schema version cannot be blank." }
    }
}

/**
 * A source-neutral request for deterministic PDF facts.
 *
 * The owning platform adapter is responsible for reopening [asset.location] only after
 * it has freshly verified the user's existing source grant. The domain deliberately
 * contains no Android URI or PDF-library type.
 */
data class PdfExtractionRequest(
    val asset: Asset,
    val schemaVersion: ExtractionSchemaVersion,
) {
    init {
        require(asset.type == AssetType.PDF) {
            "PDF extraction can only be requested for a PDF asset."
        }
    }
}

/** Text observed on one numbered PDF page. Empty text is meaningful for a blank page. */
data class PdfPageText(
    val pageNumber: Int,
    val text: String,
) {
    init {
        require(pageNumber > 0) { "A PDF page number must be positive." }
    }
}

/**
 * Declares how much of the source's text layer was deterministically read.
 *
 * A partial result is explicit so later understanding and recall cannot mistake an
 * interrupted read for a complete PDF. A PDF whose pages have no selectable text is
 * also explicit; a future OCR capability may address that case without inventing text.
 */
sealed interface PdfTextCoverage {
    data object Complete : PdfTextCoverage

    data class Partial(val reason: String) : PdfTextCoverage {
        init {
            require(reason.isNotBlank()) { "A partial PDF extraction needs a reason." }
        }
    }

    data object NoExtractableText : PdfTextCoverage
}

/**
 * Deterministic PDF metadata and page text tied to one immutable Asset fingerprint.
 *
 * This is derived data, not a copy of or owner of the original document. [metadata]
 * contains only source-provided facts that a local platform adapter can name safely.
 */
class PdfExtractionRecord private constructor(
    val assetIdentity: AssetIdentity,
    val assetFingerprint: AssetFingerprint,
    val schemaVersion: ExtractionSchemaVersion,
    val title: String?,
    val pageCount: Int?,
    val metadata: Map<String, String>,
    val pages: List<PdfPageText>,
    val textCoverage: PdfTextCoverage,
    val extractedAt: Instant,
) {
    init {
        require(title == null || title.isNotBlank()) {
            "A PDF title must be meaningful when provided."
        }
        require(pageCount == null || pageCount > 0) {
            "A PDF page count must be positive when provided."
        }
        require(metadata.keys.all(String::isNotBlank) && metadata.values.all(String::isNotBlank)) {
            "PDF metadata keys and values must be meaningful."
        }
        require(pages.map(PdfPageText::pageNumber).distinct().size == pages.size) {
            "A PDF extraction cannot contain duplicate page records."
        }
        require(pageCount == null || pages.all { it.pageNumber <= pageCount }) {
            "A PDF extraction cannot contain a page beyond its known page count."
        }
        when (textCoverage) {
            PdfTextCoverage.Complete -> {
                require(pageCount != null) {
                    "Complete PDF text coverage requires a known page count."
                }
                require(pages.map(PdfPageText::pageNumber).toSet() == (1..pageCount).toSet()) {
                    "Complete PDF text coverage must account for every page."
                }
            }

            is PdfTextCoverage.Partial -> require(pages.isNotEmpty()) {
                "Partial PDF text coverage must identify at least one extracted page."
            }

            PdfTextCoverage.NoExtractableText -> require(pages.isEmpty()) {
                "A PDF with no extractable text cannot contain page text records."
            }
        }
    }

    companion object {
        /** Creates a record that is permanently bound to the requested source version. */
        fun forRequest(
            request: PdfExtractionRequest,
            title: String? = null,
            pageCount: Int? = null,
            metadata: Map<String, String> = emptyMap(),
            pages: List<PdfPageText> = emptyList(),
            textCoverage: PdfTextCoverage,
            extractedAt: Instant,
        ): PdfExtractionRecord = PdfExtractionRecord(
            assetIdentity = request.asset.identity,
            assetFingerprint = request.asset.fingerprint,
            schemaVersion = request.schemaVersion,
            title = title,
            pageCount = pageCount,
            metadata = metadata.toMap(),
            pages = pages.toList(),
            textCoverage = textCoverage,
            extractedAt = extractedAt,
        )
    }
}

/**
 * Inward-facing boundary for a future local, read-only PDF platform adapter.
 *
 * The adapter must return an explicit access or recoverable failure result rather than
 * silently treating it as an empty document. It must never mutate, copy, or upload the
 * original PDF.
 */
interface PdfDeterministicExtractor {
    suspend fun extract(request: PdfExtractionRequest): PdfExtractionOutcome
}

sealed interface PdfExtractionOutcome {
    data class Extracted(val record: PdfExtractionRecord) : PdfExtractionOutcome

    data object AccessRequired : PdfExtractionOutcome

    data object AccessRevoked : PdfExtractionOutcome

    data class Failed(
        val message: String,
        val retryable: Boolean,
    ) : PdfExtractionOutcome {
        init {
            require(message.isNotBlank()) { "An extraction failure needs a message." }
        }
    }
}
