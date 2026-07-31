package com.memora.app.application.images

import com.memora.app.application.documents.PdfKeywordSearchSupport
import com.memora.app.domain.extraction.PhotoOcrSchemaVersion

internal object PhotoOcrKeywordSearchSupport {
    const val MAX_RESULTS = PdfKeywordSearchSupport.MAX_RESULTS
    val SCHEMA_VERSION = PhotoOcrSchemaVersion.V1.value

    fun normalizeQuery(raw: String) = PdfKeywordSearchSupport.normalizeQuery(raw)
    fun escapeForLike(needle: String) = PdfKeywordSearchSupport.escapeForLike(needle)
    fun excerptAroundMatch(fullText: String, needle: String) =
        PdfKeywordSearchSupport.excerptAroundMatch(
            fullText,
            needle,
            PdfKeywordSearchSupport.EXCERPT_RADIUS,
        )
}
