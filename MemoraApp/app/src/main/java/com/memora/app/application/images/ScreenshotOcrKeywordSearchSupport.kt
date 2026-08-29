package com.memora.app.application.images

import com.memora.app.application.documents.PdfKeywordSearchSupport
import com.memora.app.domain.extraction.ScreenshotOcrSchemaVersion

/**
 * Pure helpers shared with L3–L4 / highlight paths (normalize, LIKE, excerpt).
 *
 * Product screenshot Find no longer reads extraction DAOs (MIG-07 L2 Retired).
 * Keep this object so photo/note interim paths and shared excerpt rules stay
 * consistent with PDF keyword helpers.
 */
internal object ScreenshotOcrKeywordSearchSupport {
    const val MAX_QUERY_LENGTH = PdfKeywordSearchSupport.MAX_QUERY_LENGTH
    const val MAX_RESULTS = PdfKeywordSearchSupport.MAX_RESULTS
    const val EXCERPT_RADIUS = PdfKeywordSearchSupport.EXCERPT_RADIUS
    val SCHEMA_VERSION: String = ScreenshotOcrSchemaVersion.V1.value

    fun normalizeQuery(raw: String): String? = PdfKeywordSearchSupport.normalizeQuery(raw)

    fun escapeForLike(needle: String): String = PdfKeywordSearchSupport.escapeForLike(needle)

    fun excerptAroundMatch(fullText: String, needle: String): String =
        PdfKeywordSearchSupport.excerptAroundMatch(fullText, needle, EXCERPT_RADIUS)

    fun firstMatchSpan(haystack: String, needle: String): IntRange? =
        PdfKeywordSearchSupport.firstMatchSpan(haystack, needle)
}
