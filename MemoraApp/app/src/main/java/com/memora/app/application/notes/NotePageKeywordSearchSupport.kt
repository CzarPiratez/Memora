package com.memora.app.application.notes

import com.memora.app.application.documents.PdfKeywordSearchSupport
import com.memora.app.domain.extraction.NotePageSchemaVersion

/**
 * Pure helpers for interim on-device keyword search over saved OneNote page text.
 *
 * Reuses PDF keyword normalize / LIKE / excerpt rules so interim paths stay
 * consistent. This is not semantic recall.
 */
internal object NotePageKeywordSearchSupport {
    const val MAX_QUERY_LENGTH = PdfKeywordSearchSupport.MAX_QUERY_LENGTH
    const val MAX_RESULTS = PdfKeywordSearchSupport.MAX_RESULTS
    const val EXCERPT_RADIUS = PdfKeywordSearchSupport.EXCERPT_RADIUS
    val SCHEMA_VERSION: String = NotePageSchemaVersion.V1.value

    fun normalizeQuery(raw: String): String? = PdfKeywordSearchSupport.normalizeQuery(raw)

    fun escapeForLike(needle: String): String = PdfKeywordSearchSupport.escapeForLike(needle)

    fun excerptAroundMatch(fullText: String, needle: String): String =
        PdfKeywordSearchSupport.excerptAroundMatch(fullText, needle, EXCERPT_RADIUS)

    fun firstMatchSpan(haystack: String, needle: String): IntRange? =
        PdfKeywordSearchSupport.firstMatchSpan(haystack, needle)
}
