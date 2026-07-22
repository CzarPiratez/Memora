package com.memora.app.data.pdfbox.isolation

import android.os.Bundle

/**
 * Strict future decoder for isolated-parser page-text results.
 *
 * This codec is deliberately not connected to the current status-only Binder service. It accepts
 * only the exact version-one field set, converts it into the pure result contract, and returns no
 * candidate content for unknown, missing, mistyped, or over-limit data.
 */
@Suppress("DEPRECATION") // API 26-compatible exact runtime type checks are required at this boundary.
internal object IsolatedPdfParserResultBundleCodec {
    const val KEY_SCHEMA_VERSION = "result_schema_version"
    const val KEY_OUTCOME = "result_outcome"
    const val KEY_RETRYABLE = "result_retryable"
    const val KEY_PAGE_COUNT = "result_page_count"
    const val KEY_CHUNKS = "result_page_chunks"

    const val KEY_CHUNK_PAGE_NUMBER = "page_number"
    const val KEY_CHUNK_INDEX = "chunk_index"
    const val KEY_CHUNK_IS_FINAL = "is_final_chunk"
    const val KEY_CHUNK_TEXT = "text"

    fun decode(
        bundle: Bundle,
        limits: IsolatedPdfParserResultLimits,
    ): IsolatedPdfParserWireResultValidation = try {
        bundle.toCandidateOrNull()?.let { candidate ->
            IsolatedPdfParserResultContract.validate(candidate, limits)
        } ?: IsolatedPdfParserWireResultValidation.Rejected
    } catch (_: RuntimeException) {
        // An unparcelling or type failure must never expose unvalidated candidate text.
        IsolatedPdfParserWireResultValidation.Rejected
    }

    private fun Bundle.toCandidateOrNull(): IsolatedPdfParserWireResult? {
        if (!keySet().all(allowedTopLevelKeys::contains) || !keySet().containsAll(requiredTopLevelKeys)) {
            return null
        }

        val schemaVersion = get(KEY_SCHEMA_VERSION) as? Int ?: return null
        val outcome = (get(KEY_OUTCOME) as? String)?.toWireOutcomeOrNull() ?: return null
        val retryable = get(KEY_RETRYABLE) as? Boolean ?: return null
        val pageCount = if (containsKey(KEY_PAGE_COUNT)) get(KEY_PAGE_COUNT) as? Int ?: return null else null
        val chunks = chunksOrNull() ?: return null

        return IsolatedPdfParserWireResult(
            schemaVersion = schemaVersion,
            outcome = outcome,
            retryable = retryable,
            pageCount = pageCount,
            chunks = chunks,
        )
    }

    private fun Bundle.chunksOrNull(): List<IsolatedPdfParserPageTextChunk>? {
        val rawChunks = get(KEY_CHUNKS) as? ArrayList<*> ?: return null
        return rawChunks.map { it as? Bundle ?: return null }.map { chunk ->
            if (!chunk.keySet().all(allowedChunkKeys::contains) || !chunk.keySet().containsAll(requiredChunkKeys)) {
                return null
            }
            IsolatedPdfParserPageTextChunk(
                pageNumber = chunk.get(KEY_CHUNK_PAGE_NUMBER) as? Int ?: return null,
                chunkIndex = chunk.get(KEY_CHUNK_INDEX) as? Int ?: return null,
                isFinalChunk = chunk.get(KEY_CHUNK_IS_FINAL) as? Boolean ?: return null,
                text = chunk.get(KEY_CHUNK_TEXT) as? String ?: return null,
            )
        }
    }

    private fun String.toWireOutcomeOrNull(): IsolatedPdfParserWireOutcome? = when (this) {
        OUTCOME_EXTRACTED -> IsolatedPdfParserWireOutcome.EXTRACTED
        OUTCOME_NO_EXTRACTABLE_TEXT -> IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT
        OUTCOME_PASSWORD_PROTECTED -> IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED
        OUTCOME_FAILURE -> IsolatedPdfParserWireOutcome.FAILURE
        else -> null
    }

    private val requiredTopLevelKeys = setOf(
        KEY_SCHEMA_VERSION,
        KEY_OUTCOME,
        KEY_RETRYABLE,
        KEY_CHUNKS,
    )
    private val allowedTopLevelKeys = requiredTopLevelKeys + KEY_PAGE_COUNT
    private val requiredChunkKeys = setOf(
        KEY_CHUNK_PAGE_NUMBER,
        KEY_CHUNK_INDEX,
        KEY_CHUNK_IS_FINAL,
        KEY_CHUNK_TEXT,
    )
    private val allowedChunkKeys = requiredChunkKeys

    private const val OUTCOME_EXTRACTED = "extracted"
    private const val OUTCOME_NO_EXTRACTABLE_TEXT = "no_extractable_text"
    private const val OUTCOME_PASSWORD_PROTECTED = "password_protected"
    private const val OUTCOME_FAILURE = "failure"
}
