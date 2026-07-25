package com.memora.app.data.pdfbox.isolation

import android.os.Bundle

/**
 * Strict encoder/decoder for protocol v3 session messages (header, one chunk, or complete).
 *
 * It never packs an entire multi-chunk EXTRACTED payload into one Binder transaction.
 */
@Suppress("DEPRECATION") // API 26-compatible exact runtime type checks are required at this boundary.
internal object IsolatedPdfParserSessionMessageCodec {
    const val KEY_MESSAGE_KIND = "message_kind"
    const val KEY_SCHEMA_VERSION = IsolatedPdfParserResultBundleCodec.KEY_SCHEMA_VERSION
    const val KEY_OUTCOME = IsolatedPdfParserResultBundleCodec.KEY_OUTCOME
    const val KEY_RETRYABLE = IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE
    const val KEY_PAGE_COUNT = IsolatedPdfParserResultBundleCodec.KEY_PAGE_COUNT
    const val KEY_CHUNK = "result_page_chunk"

    const val KIND_HEADER = "header"
    const val KIND_CHUNK = "chunk"
    const val KIND_SESSION_COMPLETE = "session_complete"

    fun encodeHeader(
        header: IsolatedPdfParserSessionHeader,
        isIsolated: Boolean,
    ): Bundle = Bundle().apply {
        putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, isIsolated)
        putString(KEY_MESSAGE_KIND, KIND_HEADER)
        putInt(KEY_SCHEMA_VERSION, header.schemaVersion)
        putString(KEY_OUTCOME, header.outcome.toWireValue())
        putBoolean(KEY_RETRYABLE, header.retryable)
        header.pageCount?.let { putInt(KEY_PAGE_COUNT, it) }
    }

    fun encodeChunk(
        chunk: IsolatedPdfParserPageTextChunk,
        isIsolated: Boolean,
    ): Bundle = Bundle().apply {
        putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, isIsolated)
        putString(KEY_MESSAGE_KIND, KIND_CHUNK)
        putBundle(
            KEY_CHUNK,
            Bundle().apply {
                putInt(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_PAGE_NUMBER, chunk.pageNumber)
                putInt(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_INDEX, chunk.chunkIndex)
                putBoolean(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_IS_FINAL, chunk.isFinalChunk)
                putString(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_TEXT, chunk.text)
            },
        )
    }

    fun encodeSessionComplete(isIsolated: Boolean): Bundle = Bundle().apply {
        putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, isIsolated)
        putString(KEY_MESSAGE_KIND, KIND_SESSION_COMPLETE)
    }

    fun decode(bundle: Bundle): IsolatedPdfParserSessionMessage? {
        return try {
            if (!bundle.getBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED)) {
                return null
            }
            when (bundle.get(KEY_MESSAGE_KIND) as? String) {
                KIND_HEADER -> decodeHeader(bundle)
                KIND_CHUNK -> decodeChunk(bundle)
                KIND_SESSION_COMPLETE -> {
                    if (bundle.keySet() != setOf(IsolatedPdfParserService.KEY_IS_ISOLATED, KEY_MESSAGE_KIND)) {
                        null
                    } else {
                        IsolatedPdfParserSessionMessage.SessionComplete
                    }
                }
                else -> null
            }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun decodeHeader(bundle: Bundle): IsolatedPdfParserSessionMessage.Header? {
        val allowed = setOf(
            IsolatedPdfParserService.KEY_IS_ISOLATED,
            KEY_MESSAGE_KIND,
            KEY_SCHEMA_VERSION,
            KEY_OUTCOME,
            KEY_RETRYABLE,
            KEY_PAGE_COUNT,
        )
        if (!bundle.keySet().all(allowed::contains) ||
            !bundle.keySet().containsAll(
                setOf(
                    IsolatedPdfParserService.KEY_IS_ISOLATED,
                    KEY_MESSAGE_KIND,
                    KEY_SCHEMA_VERSION,
                    KEY_OUTCOME,
                    KEY_RETRYABLE,
                ),
            )
        ) {
            return null
        }
        val schemaVersion = bundle.get(KEY_SCHEMA_VERSION) as? Int ?: return null
        val outcome = (bundle.get(KEY_OUTCOME) as? String)?.toWireOutcomeOrNull() ?: return null
        val retryable = bundle.get(KEY_RETRYABLE) as? Boolean ?: return null
        val pageCount = if (bundle.containsKey(KEY_PAGE_COUNT)) {
            bundle.get(KEY_PAGE_COUNT) as? Int ?: return null
        } else {
            null
        }
        return IsolatedPdfParserSessionMessage.Header(
            IsolatedPdfParserSessionHeader(
                schemaVersion = schemaVersion,
                outcome = outcome,
                retryable = retryable,
                pageCount = pageCount,
            ),
        )
    }

    private fun decodeChunk(bundle: Bundle): IsolatedPdfParserSessionMessage.Chunk? {
        if (bundle.keySet() != setOf(
                IsolatedPdfParserService.KEY_IS_ISOLATED,
                KEY_MESSAGE_KIND,
                KEY_CHUNK,
            )
        ) {
            return null
        }
        val chunkBundle = bundle.get(KEY_CHUNK) as? Bundle ?: return null
        val required = setOf(
            IsolatedPdfParserResultBundleCodec.KEY_CHUNK_PAGE_NUMBER,
            IsolatedPdfParserResultBundleCodec.KEY_CHUNK_INDEX,
            IsolatedPdfParserResultBundleCodec.KEY_CHUNK_IS_FINAL,
            IsolatedPdfParserResultBundleCodec.KEY_CHUNK_TEXT,
        )
        if (chunkBundle.keySet() != required) {
            return null
        }
        return IsolatedPdfParserSessionMessage.Chunk(
            IsolatedPdfParserPageTextChunk(
                pageNumber = chunkBundle.get(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_PAGE_NUMBER) as? Int
                    ?: return null,
                chunkIndex = chunkBundle.get(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_INDEX) as? Int
                    ?: return null,
                isFinalChunk = chunkBundle.get(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_IS_FINAL) as? Boolean
                    ?: return null,
                text = chunkBundle.get(IsolatedPdfParserResultBundleCodec.KEY_CHUNK_TEXT) as? String
                    ?: return null,
            ),
        )
    }

    private fun String.toWireOutcomeOrNull(): IsolatedPdfParserWireOutcome? = when (this) {
        "extracted" -> IsolatedPdfParserWireOutcome.EXTRACTED
        "no_extractable_text" -> IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT
        "password_protected" -> IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED
        "failure" -> IsolatedPdfParserWireOutcome.FAILURE
        else -> null
    }

    private fun IsolatedPdfParserWireOutcome.toWireValue(): String = when (this) {
        IsolatedPdfParserWireOutcome.EXTRACTED -> "extracted"
        IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT -> "no_extractable_text"
        IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED -> "password_protected"
        IsolatedPdfParserWireOutcome.FAILURE -> "failure"
    }
}

internal sealed interface IsolatedPdfParserSessionMessage {
    data class Header(val header: IsolatedPdfParserSessionHeader) : IsolatedPdfParserSessionMessage

    data class Chunk(val chunk: IsolatedPdfParserPageTextChunk) : IsolatedPdfParserSessionMessage

    data object SessionComplete : IsolatedPdfParserSessionMessage
}
