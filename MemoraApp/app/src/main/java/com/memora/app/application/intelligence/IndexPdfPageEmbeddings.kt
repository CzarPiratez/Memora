package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.PdfPageEmbeddingRecord
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Indexes capped PDF page texts for E5c Find-by-meaning page ranking.
 *
 * Writes nothing when the engine is Unavailable. Fingerprint-skips unchanged
 * pages. Does not claim measured AVAILABLE.
 */
class IndexPdfPageEmbeddings @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val pageEmbeddingStore: PdfPageEmbeddingStore,
) {
    operator fun invoke(
        candidates: List<PdfPageEmbeddingCandidate>,
        nowEpochMs: Long,
    ): IndexPdfPageEmbeddingsResult {
        require(nowEpochMs >= 0)
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return IndexPdfPageEmbeddingsResult.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }

        var indexed = 0
        var skippedUnchanged = 0
        var failed = 0
        for (candidate in candidates) {
            val text = ResolveMeaningPdfOpenPage.truncateForEmbed(candidate.pageText)
            if (text.isBlank()) {
                failed += 1
                continue
            }
            val fingerprint = sha256Hex(text)
            val model = when (val availability = embeddingEngine.availability()) {
                is CapabilityAvailability.Available -> availability.model
                is CapabilityAvailability.Unavailable ->
                    return IndexPdfPageEmbeddingsResult.EngineUnavailable(availability.reason)
            }
            val existing = pageEmbeddingStore.find(
                candidate.revisionId,
                candidate.pageNumber,
                model,
            )
            if (existing != null && existing.sourceTextFingerprint == fingerprint) {
                skippedUnchanged += 1
                continue
            }
            when (val encoded = embeddingEngine.embedText(text)) {
                is EmbeddingEncodeResult.Unavailable ->
                    return IndexPdfPageEmbeddingsResult.EngineUnavailable(encoded.reason)
                is EmbeddingEncodeResult.Failed -> failed += 1
                is EmbeddingEncodeResult.Success -> {
                    pageEmbeddingStore.upsert(
                        PdfPageEmbeddingRecord(
                            revisionId = candidate.revisionId,
                            memoryId = candidate.memoryId,
                            pageNumber = candidate.pageNumber,
                            model = encoded.model,
                            vector = encoded.vector,
                            sourceTextFingerprint = fingerprint,
                            createdAtEpochMs = nowEpochMs,
                        ),
                    )
                    indexed += 1
                }
            }
        }
        return IndexPdfPageEmbeddingsResult.Completed(
            indexed = indexed,
            skippedUnchanged = skippedUnchanged,
            failed = failed,
        )
    }

    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class PdfPageEmbeddingCandidate(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val pageNumber: Int,
    val pageText: String,
) {
    init {
        require(pageNumber > 0)
    }
}

sealed interface IndexPdfPageEmbeddingsResult {
    data class EngineUnavailable(val reason: String) : IndexPdfPageEmbeddingsResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Completed(
        val indexed: Int,
        val skippedUnchanged: Int,
        val failed: Int,
    ) : IndexPdfPageEmbeddingsResult {
        init {
            require(indexed >= 0)
            require(skippedUnchanged >= 0)
            require(failed >= 0)
        }
    }
}
